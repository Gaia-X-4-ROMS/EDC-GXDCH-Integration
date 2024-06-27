package org.eclipse.edc.gxfs.catalog.extension.listener;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractdefinition.ContractDefinitionCreated;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.gxfs.catalog.extension.converter.ConverterService;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.system.configuration.Config;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ContractDefinitionCreatedSubscriber implements EventSubscriber {

  private final Monitor monitor;
  private final ContractDefinitionStore store;
  private final AssetIndex assetIndex;
  private final PolicyDefinitionStore policyStore;
  private final ConverterService converterService;
  final HttpClient httpclient = HttpClients.createDefault();
  final JSONParser parser = new JSONParser();
  TokenDto tokenDto;
  ObjectMapper mapper = new ObjectMapper();
  private final Config config;
  private final DateTimeFormatter dtf;
  private String serviceOffering;

  // Autorisierungsparameter
  private static final String KEYCLOAK_USERNAME = "edc.converter.keycloak.username";
  private static final String KEYCLOAK_PASS = "edc.converter.keycloak.password";
  private static final String KEYCLOAK_CLIENT_ID = "edc.converter.keycloak.client.id";
  private static final String KEYCLOAK_SCOPE = "edc.converter.keycloak.scop";
  private static final String KEYCLOAK_CLIENT_SECRET = "edc.converter.keycloak.client.secret";

  // URLS
  private static final String CES_URL = "edc.converter.ces.url";
  private static final String COMPLIANT_URL = "edc.converter.compliant.url";
  private static final String FC_URL = "edc.converter.fc.url";
  private static final String KEYCLOAK_URL = "edc.converter.keycloak.url";

  // paths to gaia-x files
  private static final String PATH_TO_GAIAX_FILES = "edc.contract.gaiaxressourcepath";
  private static final String TEMPLATE_FILES_DIR = "template-files/";
  private static final String SERVICE_OFFERING_DIR = "service-offering/";

  // gaia-x file-names
  private static final String PRESENTATION_FILE = "VerifiablePresentation.json";
  private static final String COMPLIANCE_FILE = "compliant.json";
  private static final String CES_CARRIER_FILE = "carrierCes.json";
  private static final String CATALOG_RESPONSE_FILE = "catalog.json";
  private static final String CES_RESPONSE_FILE = "cesReply.json";

  // DEFAULT URLS
  private static final String DEFAULT_COMPLIANCE_URL = "https://compliance.lab.gaia-x.eu/main/api/credential-offers?vcid=https%3A%2F%2Fstorage.gaia-x.eu%2Fcredential-offers%2Fb3e0a068-4bf8-4796-932e-2fa83043e203";
  private static final String DEFAULT_CES_RELATIVE_PATH_PART = "https://ces-main.lab.gaia-x.eu/credentials-events";
  private static final String DEFAULT_GAIA_SELF_DESC_URL = "https://fc.gaiax4roms.hotsprings.io/self-descriptions";

  // Häufiger genutzte Strings
  private static final String CONTENT_TYPE_HEADER = "Content-Type";
  private String gaiaXResourcePath;

  public ContractDefinitionCreatedSubscriber(Monitor monitor, ContractDefinitionStore store,
      AssetIndex assetIndex, PolicyDefinitionStore policyStore, Config config) {
    this.monitor = monitor;
    this.store = store;
    this.assetIndex = assetIndex;
    this.policyStore = policyStore;
    this.config = config;
    this.dtf = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ssZ");
    this.converterService = new ConverterService(monitor, config);
    gaiaXResourcePath = config.getString(PATH_TO_GAIAX_FILES);
  }

  @Override
  public <E extends Event> void on(EventEnvelope<E> event) {
    monitor.debug("ContractListenerExtension: Asset created!");

    Event x = event.getPayload();
    if (x instanceof ContractDefinitionCreated y) {
      ContractDefinition contract = store.findById(y.getContractDefinitionId());
      List<Criterion> listCrit = contract.getAssetsSelector();
      monitor.debug("ContractDefinition: \n" + "ContractToString: " + contract + " \n");
      PolicyDefinition policy = policyStore.findById(contract.getContractPolicyId());

      monitor.debug("AssetDefinition: \n" + "Target?: " + policy.getPolicy().getTarget()
          + "\n");

      Asset asset = assetIndex.findById((String) (listCrit.get(0).getOperandRight()));
      monitor.debug("AssetDefinition: \n" + asset.getName() + "\n");
      /*
       * Aus EDC Informationen möglichst viel für Selfdiscription rausziehen.
       */
      JSONObject dto = converterService.convertEdcToGaiaX(policy, asset, contract);
      serviceOffering=((String)asset.getProperty("https://w3id.org/edc/v0.0.1/ns/serviceOfferingName")).replaceAll(" ",
      "_");
      if (dto == null) {
        monitor.severe("The converter service returns null!  "
            + "It is not possible to send the self-description to a catalog!");
        return;
      }
      try {
        JSONObject cert = (JSONObject) parser.parse(getCertificated(dto)) ;
        if (cert.get("statusCode")==null) {
          sendToCES(cert.toJSONString());
          sendToCatalog(dto);
        } else
          monitor.warning("The certificate is empty or not valid! The self-description cannot be sent to the catalog.");
      } catch (ClientProtocolException e) {
        // DEBUG
        e.printStackTrace();
        monitor.severe("An HTTP protocol error occurred during the certification!", e);
      } catch (IOException e) {
        monitor.severe("An error occurred while handling the ContractDefinitionCreated event!",
            e);
        // DEBUG
        e.printStackTrace();
      } catch (ParseException e) {
        // DEBUG
        e.printStackTrace();
        monitor.severe("A parsing error occurred while sending the self-description to a catalog!",
            e);
      }
    }
  }

  private String getCertificated(JSONObject dto) throws IOException, ParseException {
    ObjectMapper mapper = new ObjectMapper();
    String relativePath = System.getProperty("user.dir")
        + gaiaXResourcePath + SERVICE_OFFERING_DIR +serviceOffering+"/"+ PRESENTATION_FILE;

    monitor.debug("Verifiable Presentation available at: " + relativePath);
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(relativePath))) {
      mapper.writeValue(writer, dto);
    } catch (IOException ex) {
      // Handle IOException
      monitor.severe("An error occurred while writing verifiable presentation json file!",
          ex);
      // DEBUG
      ex.printStackTrace();
    }

    String uRL = config.getString(COMPLIANT_URL, DEFAULT_COMPLIANCE_URL);
    HttpPost httpPost = new HttpPost(uRL);
    StringEntity body = new StringEntity(dto.toString(), StandardCharsets.UTF_8);

    httpPost.setEntity(body);
    HttpResponse response = httpclient.execute(httpPost);
    HttpEntity entity = response.getEntity();

    if (entity != null) {
      try (InputStream inStream = entity.getContent()) {
        String text = new String(inStream.readAllBytes(), StandardCharsets.UTF_8);
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(text);
        relativePath = System.getProperty("user.dir")
            + gaiaXResourcePath + SERVICE_OFFERING_DIR +serviceOffering+"/"+ COMPLIANCE_FILE;
        monitor.debug("Compliant Credential available at: " + relativePath);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(relativePath))) {
          mapper.writeValue(writer, json);
        } catch (IOException ex) {
          // Handle IOException
          monitor.severe("An error occurred while writing compliant credential json file!",
              ex);
        }
        return text;
      }
    }
    return "";
  }

  private void sendToCES(String com) throws IOException, ParseException {
    String uRL = config.getString(CES_URL, DEFAULT_CES_RELATIVE_PATH_PART);

    JSONObject carrierCes = converterService.getJsonFromFile(gaiaXResourcePath + TEMPLATE_FILES_DIR
        + CES_CARRIER_FILE);
    JSONParser parser = new JSONParser();
    carrierCes.put("data", parser.parse(com));
    carrierCes.put("time", formatDate(LocalDateTime.now(), dtf));
    HttpPost httpPost = new HttpPost(uRL);
    StringEntity body = new StringEntity(carrierCes.toString(), StandardCharsets.UTF_8);

    httpPost.setEntity(body);
    httpPost.setHeader(CONTENT_TYPE_HEADER, "application/cloudevents+json");
    HttpResponse response = httpclient.execute(httpPost);
    Header[] header= response.getAllHeaders();
    for(int i=0;i<header.length;i++){
        if (header[i].getName().equals("Location")) {
            String relativePath = System.getProperty("user.dir") + gaiaXResourcePath + SERVICE_OFFERING_DIR
            +serviceOffering+"/"+ CES_RESPONSE_FILE;
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(relativePath))) {
              mapper.writeValue(writer, header[i].getValue());
            } catch (IOException ex) {
              ex.printStackTrace();
            }
            monitor.info("Ces reply can be found at: " + relativePath);
          }
    }
  }

  private void sendToCatalog(JSONObject dto) throws UnsupportedOperationException,
      IOException,
      ParseException {
    if (tokenDto == null) {
      getToken();
    }
    String uRL = config.getString(FC_URL, DEFAULT_GAIA_SELF_DESC_URL);
    HttpPost httpPost = new HttpPost(uRL);
    StringEntity body = new StringEntity(dto.toString(), StandardCharsets.UTF_8);
    httpPost.setHeader("Authorization", "Bearer " + tokenDto.getAccessToken());
    httpPost.setHeader(CONTENT_TYPE_HEADER, "application/json");
    httpPost.setEntity(body);
    HttpResponse response = null;
    try {
      response = httpclient.execute(httpPost);
    } catch (IOException e) {
      monitor.severe("An error occurred while sending to the catalog!", e);
    }
    HttpEntity entity = response.getEntity();
    if (entity != null) {
      String relativePath = System.getProperty("user.dir") + gaiaXResourcePath + SERVICE_OFFERING_DIR
      +serviceOffering+"/"+ CATALOG_RESPONSE_FILE;
      String text = new String(entity.getContent().readAllBytes(), StandardCharsets.UTF_8);
      JSONObject object = (JSONObject) parser.parse(text);
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(relativePath))) {
        mapper.writeValue(writer, object);
      } catch (IOException ex) {
        ex.printStackTrace();
      }
      monitor.info("Katalog antwort liegt in: " + relativePath);
    }
  }

  private void getToken() throws UnsupportedOperationException,
      IOException,
      ParseException {
    String url = "";
    try {
      url = config.getString(KEYCLOAK_URL);
    } catch (Exception e) {
      monitor.info("Kein Endpunkt für Token abuf, gehe davon aus das der Katalog ohne Keycloak arbeitet");
    }
    HashMap<String, String> parameters = new HashMap<>();
    parameters.put("grant_type", "password");
    parameters.put("username", config.getString(KEYCLOAK_USERNAME));
    parameters.put("password", config.getString(KEYCLOAK_PASS));
    parameters.put("client_id",
        config.getString(KEYCLOAK_CLIENT_ID, "federated-catalogue"));
    parameters.put("scope", config.getString(KEYCLOAK_SCOPE, "openid"));
    parameters.put("client_secret", config.getString(KEYCLOAK_CLIENT_SECRET));
    String form = parameters.keySet()
        .stream()
        .map(key -> key + "="
            + URLEncoder.encode(parameters.get(key),
                StandardCharsets.UTF_8))
        .collect(Collectors.joining("&"));

    HttpPost httpPost = new HttpPost(url);
    StringEntity body = new StringEntity(form, StandardCharsets.UTF_8);

    httpPost.setEntity(body);

    httpPost.setHeader(CONTENT_TYPE_HEADER, "application/x-www-form-urlencoded");

    HttpResponse response = null;
    try {
      response = httpclient.execute(httpPost);
    } catch (IOException e) {
      // DEBUG
      e.printStackTrace();
      monitor.severe("An error occurred while retrieving the access token!", e);
    }
    HttpEntity entity = response.getEntity();
    if (entity != null) {
      InputStream inStream = entity.getContent();
      String text = new String(inStream.readAllBytes(), StandardCharsets.UTF_8);
      JSONObject reply = (JSONObject) parser.parse(text);
      tokenDto = new TokenDto(reply.get("access_token").toString(),
          reply.get("refresh_token").toString());
    }
  }

  public static String formatDate(LocalDateTime date, DateTimeFormatter dtf) {
    return date.atOffset(ZoneOffset.UTC).format(dtf);
  }
}
