package org.eclipse.edc.gxfs.catalog.extension.converter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

//import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;

import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.configuration.Config;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.json.JsonObject;

public class ConverterService {
    private final GaiaXDto identity;
    private final Monitor monitor;
    final JSONParser parser = new JSONParser();
    final HttpClient httpclient = HttpClients.createDefault();
    private final Config config;
    ObjectMapper mapper = new ObjectMapper();

    private static final String SD_URL = "edc.converter.sd.url";

    private static final String SD_AUTH_USER = "edc.converter.sd.authorization.user";

    private static final String SD_AUTH_PW = "edc.converter.sd.authorization.pw";

    // paths to gaia-x files
    private static final String PATH_TO_GAIAX_FILES = "edc.contract.gaiaxressourcepath";
    private static final String NOTAR_URL = "edc.converter.notar.url";
    private static final String PATH_TO_IDENTITY_HOTSPRINGS = "identity/hotspring/";
    private static final String PATH_TO_IDENTITY_TEMPLATE = "identity/template/";
    private static final String TEMPLATE_FILES_DIR = "template-files/";
    private static final String SERVICE_OFFERING_DIR = "service-offering/";

    // gaia-x file-names
    private static final String NOTAR_DTO = "notarDto.json";
    private static final String TERMS_FILE = "terms.json";
    private static final String REG_NUMBER_FILE = "legalRegNumber.json";
    private static final String PARTICIPANT_FILE = "legalParcip.json";

    private static final String CARRIER_TEMPLATE = "carrier.json";
    private static final String DATA_RESOURCE_TEMPLATE = "dataResource.json";
    private static final String INSTANTIATED_DATA_RESOURCE_TEMPLATE = "instanzDataRessource.json";
    private static final String PHYSICAL_RESOURCE_TEMPLATE = "physicalRessource_template.json";
    private static final String SERVICE_ACCESSPOINT_TEMPLATE = "serviceAccessPoint.json";
    private static final String SERVICE_OFFERING_TEMPLATE = "serviceOffering.json";

    private static final String PHYSICAL_RESOURCE_FILE = "physicalRessource.json";

    private static final String CONTRACT_DEF_CONVERTER_GX_LOCATION = "edc.converter.location";
    private static final String CONTRACT_DEF_CONVERTER_COMPANYNAME = "edc.converter.company.name";
    private static final String CONTRACT_DEF_CONVERTER_COUNTRY_SUBDISVION_CODE = "edc.converter.country.subdivision.code";
    private static final String CONTRACT_DEF_CONVERTER_VATID = "edc.converter.vat.id";
    // Häufiger genutzte Strings
    private static final String CONVERTER_ID = "id";
    private static final String CONVERTER_ADD_ID = "@id";
    private static final String CONVERTER_EMPTY_STR = " ";
    private static final String CONVERTER_EMPTY_STR_REPLACEMENT = "_";
    private static final String CREDENTIALSUBJECT = "credentialSubject";
    private String gaiaXResourcePath;

    public ConverterService(Monitor monitor, Config config) {
        this.monitor = monitor;
        this.config = config;
        identity = new GaiaXDto();
        fillBasics();
        monitor.debug("Basic Identity: " + identity);
    }

    private void fillBasics() {
        gaiaXResourcePath = config.getString(PATH_TO_GAIAX_FILES);
        String pathToSD = gaiaXResourcePath + PATH_TO_IDENTITY_HOTSPRINGS;
        String pathToTemplateFiles = gaiaXResourcePath + TEMPLATE_FILES_DIR;
        String pathToServiceOffering = gaiaXResourcePath + SERVICE_OFFERING_DIR;

        companyIdentity(pathToSD, pathToTemplateFiles, pathToServiceOffering);

        try {
            identity.setCarrier(getJsonFromFile(pathToTemplateFiles + CARRIER_TEMPLATE));
        } catch (Exception e) {
            monitor.warning("carrier could not be read from file", e);
        }
        try {
            identity.setDataResource(getJsonFromFile(pathToTemplateFiles
                    + DATA_RESOURCE_TEMPLATE));
        } catch (Exception e) {
            monitor.warning("dataResource could not be read from file", e);
        }
        try {
            identity.setServiceOffering(getJsonFromFile(pathToTemplateFiles
                    + SERVICE_OFFERING_TEMPLATE));
        } catch (Exception e) {
            monitor.warning("serviceOffering could not be read from file", e);
        }
        try {
            identity.setPhysicalResource(getJsonFromFile(pathToServiceOffering
                    + PHYSICAL_RESOURCE_FILE));
        } catch (Exception e) {
            monitor.warning("physicalRessource could not be read from file", e);
        }
        try {
            identity.setServiceAccesPoint(getJsonFromFile(pathToTemplateFiles
                    + SERVICE_ACCESSPOINT_TEMPLATE));
        } catch (Exception e) {
            monitor.warning("serviceAccessPoint could not be read from file", e);
        }
        try {
            identity.setInstantiatedDataResource(getJsonFromFile(pathToTemplateFiles
                    + INSTANTIATED_DATA_RESOURCE_TEMPLATE));
        } catch (Exception e) {
            monitor.warning("instantiatedDataRessource could not be read from file", e);
        }
        monitor.info("defaults are applied");
        setDefaultIds();

        monitor.info("carrier is being filled");
        JSONArray verifiableCredential = (JSONArray) identity.getCarrier()
                .get("verifiableCredential");
        verifiableCredential.add(identity.getLegalParticipant());
        verifiableCredential.add(identity.getTermsAndConditions());
        verifiableCredential.add(identity.getLegalRegistrationNumber());
        verifiableCredential.add(identity.getPhysicalResource());
    }

    private void companyIdentity(String pathToSD, String pathToTemplateFiles, String pathToServiceOffering) {
        getCompanyName();
        boolean generateNew = false;
        try {
            identity.setLegalParticipant(getJsonFromFile(pathToSD
                    + PARTICIPANT_FILE));
        } catch (Exception e) {
            monitor.warning("legalPerson not defined, onboarding new Company");
            generateNew = true;
        }

        if (generateNew) {
            onBoarding(pathToSD, pathToTemplateFiles, pathToServiceOffering);
        } else {
            try {
                identity.setTermsAndConditions(getJsonFromFile(pathToSD
                        + TERMS_FILE));
            } catch (Exception e) {
                monitor.warning("terms and conditions could not be read from file", e);
            }
            try {
                identity.setLegalRegistrationNumber(getJsonFromFile(pathToSD
                        + REG_NUMBER_FILE));
            } catch (Exception e) {
                monitor.warning("legalRegNumber could not be read from file", e);
            }
        }
    }

    private void onBoarding(String pathToSD, String pathToTemplateFiles, String pathToServiceOffering) {
        String pathToTemplateSD = gaiaXResourcePath + PATH_TO_IDENTITY_TEMPLATE;
        try {
            JSONObject legalRegNotaryDTO = getJsonFromFile(pathToTemplateSD
                    + NOTAR_DTO);
            String notarParameter = "did:web:" + config.getString(SD_URL).split("/")[2] + ":"
                    + config.getString(CONTRACT_DEF_CONVERTER_COMPANYNAME) + "_regNum";
            legalRegNotaryDTO.put("id", notarParameter);
            legalRegNotaryDTO.put("gx:vatID", config.getString(CONTRACT_DEF_CONVERTER_VATID));
            legalRegNotaryDTO.put("gx:vatID-countryCode",
                    config.getString(CONTRACT_DEF_CONVERTER_VATID).substring(0, 2));
            try {
                identity.setLegalRegistrationNumber(getNotarCredential(legalRegNotaryDTO));
            } catch (Exception e) {
                monitor.severe("Credential could not be retrieved for the Legal Regrestration", e);
            }
            String relativePath = pathToSD + REG_NUMBER_FILE;
            writeToFile(identity.getLegalRegistrationNumber(), relativePath);
        } catch (Exception e) {
            monitor.severe("Could not onboard. Problems with the legal Regrestrationnumber: ", e);
            return;
        }

        try {
            JSONObject legalPap = getJsonFromFile(pathToTemplateSD
                    + PARTICIPANT_FILE);
            JSONObject credentialSubject = (JSONObject) legalPap.get("credentialSubject");
            ((JSONObject) credentialSubject.get("gx:legalRegistrationNumber")).put("id",
                    ((JSONObject) identity.getLegalRegistrationNumber()
                            .get("credentialSubject")).get("id"));
            ((JSONObject) credentialSubject.get("gx:headquarterAddress")).put("gx:countrySubdivisionCode",
                    config.getString(CONTRACT_DEF_CONVERTER_COUNTRY_SUBDISVION_CODE));
            ((JSONObject) credentialSubject.get("gx:legalAddress")).put("gx:countrySubdivisionCode",
                    config.getString(CONTRACT_DEF_CONVERTER_COUNTRY_SUBDISVION_CODE));

            ((JSONObject) credentialSubject).put("gx:legalName",
                    config.getString(CONTRACT_DEF_CONVERTER_COMPANYNAME).replaceAll(CONVERTER_EMPTY_STR_REPLACEMENT,
                            CONVERTER_EMPTY_STR));
            try {
                identity.setLegalParticipant(getCredential(legalPap, "participant", true));
            } catch (Exception e) {
                monitor.severe("Credential could not be retrieved for the Legal Participant", e);
            }
            String relativePath = pathToSD + PARTICIPANT_FILE;
            writeToFile(identity.getLegalParticipant(), relativePath);
        } catch (Exception e) {
            monitor.severe("No legalParticipant to onboard", e);
            return;
        }

        try {
            JSONObject terms = getJsonFromFile(pathToTemplateSD + TERMS_FILE);
            try {
                identity.setTermsAndConditions(getCredential(terms, "terms", true));
            } catch (Exception e) {
                monitor.severe("Credential could not be retrieved for the terms and condition", e);
            }
            String relativePath = pathToSD + TERMS_FILE;
            writeToFile(identity.getTermsAndConditions(), relativePath);
        } catch (Exception e) {
            monitor.severe("No termsAndCondition for onboarding");
            return;
        }

        try {
            JSONObject physical = getJsonFromFile(pathToTemplateFiles
                    + PHYSICAL_RESOURCE_TEMPLATE);

            String legalpart = (String) (((JSONObject) identity.getLegalParticipant()
                    .get("credentialSubject")).get("id"));

            JSONObject credentialSubject = (JSONObject) physical.get("credentialSubject");

            ((JSONObject) credentialSubject.get("gx:maintainedBy")).put("id", legalpart);
            ((JSONObject) credentialSubject.get("gx:ownedBy")).put("id", legalpart);
            ((JSONObject) credentialSubject.get("gx:manufacturedBy")).put("id", legalpart);

            String location = config.getString(CONTRACT_DEF_CONVERTER_GX_LOCATION);
            if (location != null && !location.equals("")) {
                location.replace("_", " ");
            } else {
                monitor.severe("No location for physical ressource under"
                        + CONTRACT_DEF_CONVERTER_GX_LOCATION);
            }
            credentialSubject.put("gx:location", location);

            String relativePath = pathToServiceOffering + PHYSICAL_RESOURCE_FILE;
            writeToFile(getCredential(physical, "physicalRessource", true), relativePath);

        } catch (Exception e) {
            monitor.severe("No physical Ressource created ", e);
            return;
        }

        monitor.info("Company onboarded");
    }

    private JSONObject getNotarCredential(JSONObject notartDto) throws IOException,
            ParseException {

        String url = config.getString(NOTAR_URL) + "?vcid=" + notartDto.get("id").toString().replaceAll(":", "%3A");
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader("Content-Type", "application/json");
        StringEntity body = new StringEntity(notartDto.toString(), StandardCharsets.UTF_8);
        httpPost.setEntity(body);
        HttpResponse response = httpclient.execute(httpPost);
        HttpEntity entity = response.getEntity();
        monitor.debug(notartDto.toString() + " " + url);
        if (entity != null) {
            try (InputStream inStream = entity.getContent()) {
                String text = new String(inStream.readAllBytes(), StandardCharsets.UTF_8);
                JSONObject regNumber = (JSONObject) parser.parse(text);
                getCredential(regNumber, "regNum", false);
                return regNumber;
            }
        }
        monitor.warning("It was not possible to get a notar credential!");
        return null;

    }

    private void getCompanyName() {
        String companyName = config.getString(CONTRACT_DEF_CONVERTER_COMPANYNAME);
        identity.setCompanyName(companyName);
    }

    private void writeToFile(JSONObject part, String relativePath) {
        String absolutePath = System.getProperty("user.dir") + relativePath;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(absolutePath))) {
            mapper.writeValue(writer, part);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void setDefaultIds() {
        String credentialSubjectKey = CREDENTIALSUBJECT;
        JSONObject credentialSubject = ((JSONObject) identity.getLegalParticipant()
                .get(credentialSubjectKey));

        JSONObject providedBy = (JSONObject) (((JSONObject) identity.getServiceOffering()
                .get(credentialSubjectKey)).get("gx:providedBy"));
        providedBy.put(CONVERTER_ID, credentialSubject.get(CONVERTER_ID));
        JSONObject producedBy = (JSONObject) (((JSONObject) identity.getDataResource()
                .get(credentialSubjectKey)).get("gx:producedBy"));
        producedBy.put(CONVERTER_ADD_ID, credentialSubject.get(CONVERTER_ID));

        /*
         * ToDo:
         */
        ((JSONObject) ((JSONObject) identity.getInstantiatedDataResource()
                .get(credentialSubjectKey)).get("gx:maintainedBy")).put(CONVERTER_ADD_ID,
                        credentialSubject.get(CONVERTER_ID));

        ((JSONObject) ((JSONObject) identity.getInstantiatedDataResource()
                .get(credentialSubjectKey)).get("gx:hostedOn")).put(CONVERTER_ADD_ID,
                        ((JSONObject) identity.getPhysicalResource()
                                .get(credentialSubjectKey)).get(CONVERTER_ID));

    }

    public JSONObject getJsonFromFile(String path) throws IOException, ParseException {
        String totalpath = System.getProperty("user.dir") + path;
        InputStream inputStream = new FileInputStream(totalpath);
        String string = new String(inputStream.readAllBytes());
        inputStream.close();
        return (JSONObject) parser.parse(string);
    }

    public JSONObject convertEdcToGaiaX(PolicyDefinition policy, Asset asset,
            ContractDefinition contract) {
        GaiaXDto dto = null;
        try {
            dto = (GaiaXDto) identity.clone();
        } catch (CloneNotSupportedException e) {
            // DEBUG
            e.printStackTrace();
            monitor.severe("An error occurred while trying to clone the identity GaiaXDto object!",
                    e);
            return null;
        }
        cleanForNewServiceOffering(dto,
                (String) asset.getProperty("https://w3id.org/edc/v0.0.1/ns/serviceOfferingName"));

        // ToDo: fill from EDC

        /*
         * 
         */
        fillServiceOffering(dto, asset);
        fillDataRessource(dto, asset);
        fillServiceAccessPoint(dto, asset);
        fillInstantiatedDataResource(dto, asset);

        JSONObject carrier = dto.getCarrier();
        JSONArray verifiableCredential = (JSONArray) carrier.get("verifiableCredential");
        verifiableCredential.add(dto.getDataResource());
        verifiableCredential.add(dto.getServiceAccesPoint());
        verifiableCredential.add(dto.getInstantiatedDataResource());

        return carrier;
    }

    private void cleanForNewServiceOffering(GaiaXDto dto, String ServiceOfferingName) {
        JSONArray vCs = ((JSONArray) dto.getCarrier().get("verifiableCredential"));
        // legalPap, legalRegNum, terms, serviceOffering have type instead of @type ?
        List<JSONObject> vCsList = (List<JSONObject>) vCs.stream()
                .filter(vc -> ((JSONObject) ((JSONObject) vc).get("credentialSubject")).get("@type") != null &&
                        ((String) ((JSONObject) ((JSONObject) vc).get("credentialSubject")).get("@type"))
                                .equals("gx:ServiceOffering"))
                .toList();
        if (vCsList.isEmpty()) {
            return;
        }
        // assumption for now that only one ServiceOffering is created at a time. If the
        // new serviceoffering has a different
        // name than the old one, the carrier will be cleaned for the new
        // serviceOffering.
        if (!((JSONObject) vCsList.get(0).get("credentialSubject")).get("gx:name").toString()
                .equals(ServiceOfferingName)) {
            monitor.info("New SO, clearing cache.");
            vCs.clear();
            vCs.add(identity.getLegalParticipant());
            vCs.add(identity.getTermsAndConditions());
            vCs.add(identity.getLegalRegistrationNumber());
            vCs.add(identity.getPhysicalResource());

        }
    }

    private void fillServiceOffering(GaiaXDto dto, Asset asset) {
        JSONObject serviceOfferingSubject = (JSONObject) dto.getServiceOffering()
                .get(CREDENTIALSUBJECT);
        // Wenn ServiceOffering so schon angelegt ist, nichts machen
        if (((String) serviceOfferingSubject.get("gx:name")) != null &&
                ((String) serviceOfferingSubject.get("gx:name"))
                        .equals(asset.getProperty("https://w3id.org/edc/v0.0.1/ns/serviceOfferingName"))
                && ((String) serviceOfferingSubject.get("gx:description")) != null
                && ((String) serviceOfferingSubject.get("gx:description"))
                        .equals(asset.getProperty("https://w3id.org/edc/v0.0.1/ns/serviceOfferingDescription"))) {
            monitor.debug("ServiceOffering clready present, no update nessesary");
            return;
        }

        JSONObject oldServiceOffering = checkForServiceOffering(
                (String) asset.getProperty("https://w3id.org/edc/v0.0.1/ns/serviceOfferingName"), dto, asset);

        // Pfad zum neuen Ordner
        String folderPath = System.getProperty("user.dir") + config.getString(PATH_TO_GAIAX_FILES)
                + SERVICE_OFFERING_DIR
                + ((String) asset.getProperty("https://w3id.org/edc/v0.0.1/ns/serviceOfferingName")).replaceAll(
                        CONVERTER_EMPTY_STR,
                        CONVERTER_EMPTY_STR_REPLACEMENT);

        // Erzeuge ein neues File-Objekt mit dem Ordnerpfad
        File folder = new File(folderPath);
        if (!folder.exists()) {
            try {
                folder.mkdirs();
            } catch (Exception e) {
                monitor.severe("error creating the folder for serviceOffering: "
                        + ((String) asset.getProperty("https://w3id.org/edc/v0.0.1/ns/serviceOfferingName")).replaceAll(
                                CONVERTER_EMPTY_STR,
                                CONVERTER_EMPTY_STR_REPLACEMENT));
            }
        }
        if (oldServiceOffering != null) {
            dto.setServiceOffering(oldServiceOffering);
            JSONObject carrier = dto.getCarrier();
            JSONArray verifiableCredential = (JSONArray) carrier.get("verifiableCredential");
            verifiableCredential.add(dto.getServiceOffering());
            return;
        }

        serviceOfferingSubject.put("gx:name",
                asset.getProperty("https://w3id.org/edc/v0.0.1/ns/serviceOfferingName"));
        serviceOfferingSubject.put("gx:description",
                asset.getProperty("https://w3id.org/edc/v0.0.1/ns/serviceOfferingDescription"));

        try {
            dto.setServiceOffering(getCredential(dto.getServiceOffering(),
                    (String) asset.getProperty("https://w3id.org/edc/v0.0.1/ns/serviceOfferingName")+"_serviceOffering", true));
            JSONObject carrier = dto.getCarrier();
            JSONArray verifiableCredential = (JSONArray) carrier.get("verifiableCredential");
            verifiableCredential.add(dto.getServiceOffering());
        } catch (Exception e) {
            monitor.severe("Credential could not be retrieved for the service offering", e);
        }
    }

    private JSONObject checkForServiceOffering(String credential, GaiaXDto dto, Asset asset) {

        JSONObject serviceOffering = getDid(credential);
        if (serviceOffering == null) {
            return null;
        }
        monitor.debug("serviceOffering: "+serviceOffering.toJSONString());
        if (serviceOfferingUnchanged(serviceOffering, dto, asset)) {
            return serviceOffering;
        }
        return null;
    }

    private JSONObject getDid(String credential) {
        credential = identity.getCompanyName() + "_" + credential;
        credential = credential.replaceAll(CONVERTER_EMPTY_STR,
                CONVERTER_EMPTY_STR_REPLACEMENT);
        String url = config.getString(SD_URL, "https://gaiax4roms.hotsprings.io/") + credential
                + "/did.json";
        try {
            HttpGet request = new HttpGet(url);
            HttpResponse response = httpclient.execute(request);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                try (InputStream inStream = entity.getContent()) {
                    String text = new String(inStream.readAllBytes(), StandardCharsets.UTF_8);
                    JSONObject json=(JSONObject) parser.parse(text);
                    if(json.get("error")!=null){
                        monitor.debug("did not found, can be fine, if it's currently being created: "+credential);
                        return null;
                    }
                    return json;
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private boolean serviceOfferingUnchanged(JSONObject serviceOffering, GaiaXDto dto, Asset asset) {
        JSONObject legalPar = getDid("participant");
        // Company identity was updated, so all credentials have to be updated
        if (legalPar==null || !((String) legalPar.get("issuanceDate")).equals((String) dto.getLegalParticipant().get("issuanceDate"))) {
            return false;
        }
        // the service offering description should be updated
        if (((JSONObject) serviceOffering.get("credentialSubject")).get("gx:description")
                .equals((String) asset.getProperty("https://w3id.org/edc/v0.0.1/ns/serviceOfferingDescription"))) {
            monitor.debug("No change, using old service offering");
            return true;
        }
        return false;

    }

    private void fillDataRessource(GaiaXDto dto, Asset asset) {
        String credentialSubjectKey = CREDENTIALSUBJECT;
        JSONObject credentialSubject = (JSONObject) dto.getDataResource()
                .get(credentialSubjectKey);
        credentialSubject.put("gx:name", asset.getName());
        credentialSubject.put("gx:description", asset.getDescription());

        credentialSubject = (JSONObject) dto.getDataResource().get(credentialSubjectKey);

        ((JSONObject) credentialSubject.get("gx:exposedThrough")).put(CONVERTER_ADD_ID,
                ((JSONObject) dto.getServiceOffering()
                        .get(credentialSubjectKey)).get(CONVERTER_ID));

        try {
            dto.setDataResource(getCredential(dto.getDataResource(), asset.getName()+"_dataresource", true));
        } catch (Exception e) {
            monitor.severe("Credential could not be retrieved for the data ressource", e);
        }
    }

    private void fillServiceAccessPoint(GaiaXDto dto, Asset asset) {
        JSONObject credentialSubject = (JSONObject) dto.getServiceAccesPoint()
                .get(CREDENTIALSUBJECT);
        credentialSubject.put("gx:host",
                asset.getProperty("https://w3id.org/edc/v0.0.1/ns/host"));
        credentialSubject.put("gx:port",
                asset.getProperty("https://w3id.org/edc/v0.0.1/ns/port"));
        credentialSubject.put("gx:protocol",
                asset.getProperty("https://w3id.org/edc/v0.0.1/ns/protocol"));
        credentialSubject.put("gx:version",
                asset.getProperty("https://w3id.org/edc/v0.0.1/ns/version"));
        credentialSubject.put("gx:openAPI",
                asset.getProperty("https://w3id.org/edc/v0.0.1/ns/endPoint"));
        try {
            dto.setServiceAccesPoint(getCredential(dto.getServiceAccesPoint(),
                    asset.getName() + "_ServiceAccesPoint", true));
        } catch (Exception e) {
            monitor.severe("Credential could not be retrieved for the data ressource", e);
        }
    }

    private void fillInstantiatedDataResource(GaiaXDto dto, Asset asset) {
        String credentialSubjectKey = CREDENTIALSUBJECT;
        JSONArray serviceAcces = ((JSONArray) ((JSONObject) dto.getInstantiatedDataResource()
                .get(credentialSubjectKey)).get("gx:serviceAccessPoint"));
        ((JSONObject) serviceAcces.get(0)).put(CONVERTER_ADD_ID,
                ((JSONObject) dto.getServiceAccesPoint()
                        .get(credentialSubjectKey)).get(CONVERTER_ID));
        ((JSONObject) ((JSONObject) dto.getInstantiatedDataResource()
                .get(credentialSubjectKey)).get("gx:instanceOf")).put(CONVERTER_ADD_ID,
                        ((JSONObject) dto.getDataResource()
                                .get(credentialSubjectKey)).get(CONVERTER_ID));

        try {
            dto.setInstantiatedDataResource(getCredential(dto.getInstantiatedDataResource(),
                    asset.getName() + "_Insantz", true));
        } catch (Exception e) {
            monitor.severe("Credential could not be retrieved for the data ressource", e);
        }
    }

    private JSONObject getCredential(JSONObject dto,
            String credential, Boolean newId) throws /* ClientProtocolException, */ IOException,
            ParseException {
        credential = identity.getCompanyName() + "_" + credential;
        credential = credential.replaceAll(CONVERTER_EMPTY_STR,
                CONVERTER_EMPTY_STR_REPLACEMENT);
        String url = config.getString(SD_URL, "https://gaiax4roms.hotsprings.io/") + credential
                + "/did.json";

        if (newId) {
            ((JSONObject) dto.get(CREDENTIALSUBJECT)).put(CONVERTER_ID, url);
        }
        HttpPost httpPost = new HttpPost(url);

        StringEntity body = new StringEntity(dto.toString(), StandardCharsets.UTF_8);
        httpPost.setEntity(body);
        httpPost.setHeader("Authorization",
                getBasicAuthenticationHeader(config.getString(SD_AUTH_USER, "api"),
                        config.getString(SD_AUTH_PW)));
        httpPost.setHeader("Content-Type", "application/json");
        httpPost.setHeader("X-Signature-Flavour", "Specification");
        HttpResponse response = httpclient.execute(httpPost);
        HttpEntity entity = response.getEntity();

        if (entity != null) {
            try (InputStream inStream = entity.getContent()) {
                String text = new String(inStream.readAllBytes(), StandardCharsets.UTF_8);
                JSONObject object=(JSONObject) parser.parse(text);
                if(object.get("credentialSubject")!=null){
                return object;
                }
            }
        }

        monitor.warning("It was not possible to create a not-null credential object!");
        return null;
    }

    private static String getBasicAuthenticationHeader(String username, String password) {
        String valueToEncode = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
    }

}
