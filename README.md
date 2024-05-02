# EDC Integration to Create and Distribute Gaia-X Compliance Credentials via GXDCH

## 0. Known Issues
- When no Credentials are presend in the /identity/hostspring folder the EDC will create new ones. If another EDC already created credentials they will all be marked as depricated by the new one
- The Katalog currently can only contain one VP at a time. When one changes to a new Service Offering a new VP will be created for that, depricating the previous one.

## 1. Prepare Information
### b&#41; Adjust the resources-files
- If you want to start the application through docker: In the Docker-Compose file, adjust line 48-51 with the information corrosponding to your company. Then go to step 2.
- If you don't want to use docker but start the gaia-x extention on it's own go to the connector-configuration.properties (/resources/connector/configuration/) and adjust line 38-41 accordingly. Then go to step 3.


## 2. Build and Run via Docker-Compose
- Open a terminal in this directory (`roms-edc`).
### a&#41; Build and Run 
start the edc through docker (install docker if nessesary):
```bash
docker-compose up 
```
### b&#41; Create an asset 
* Open the Dashboard at http://localhost:7080
* Open the (`Assets`)-subpage
  * Click on "Create asset".
  * Fill in the required fields for your data-asset.
    * The Id can be text and numbers, e.g. "UsersAsset"
    * The content type should be something like the [Media Types](https://www.iana.org/assignments/media-types/media-types.xhtml), e.g. "application/json"
    * Select your data source (currently supported: Kafka, Http and AzureStorage) and enter the additional fields, e.g. "HttpData" and the base url "https://jsonplaceholder.typicode.com/users"
  * Fill in the required fields for the Gaia-X service offering.
    * Provide a name an a description for your service offering.
    * Provide the endpoint for your service offering. 
      If an EDC-connector provides your service, enter the URL of your protocol-endpoint, e.g. "http://<ip>:19194/protocol". Enter "dataspace-protocol-http" as the protocol and the IP and port in the "host" and "port" fields.
  * Click on "Create". The asset should be displayed in the overview.
* Open the (`Policies`)-subpage
  * Click on "Create policy".
  * Fill in the required fields. Just an ID is needed, if your data is publicly accessible.
  * Click on "Save". The policy should be displayed in the overview.
* Open the (`Contract Definitions`)-subpage
  * Click on "Create contract definition".
  * Fill in an Id and select your previously created policy and the asset.
  * Choose your previously created asset in the asset field below the policies (IMPORTANT!).
  * Click on "Create". The creation of the contract offer takes some time, as the data is transferred to the CES and the catalog. The contract offer is only displayed in the overview once both have been completed. The service offering has been successfully entered in the catalog.

### c&#41; Check the service offering data
* Your service offering should be displayed in the catalog: [Accenture Catalog Viewer](https://federated-catalog-viewer.gaiax4roms.hotsprings.io/nodes) 
* There should be three new files in this directory (`roms-edc`) under \resources\connector\gaia-x\service-offering: catalog.json, compliant.json and VerifiablePresentation.json.
  The files "compliant.json" and "VerifiablePresentation.json" should reference your new service offering. The "catalog.json" file is simply the catalog response that documents the upload to the catalog and gives you the hash-code under which you can find your offering in the catalog.

## 3. Build and Run - Connector Standalone
### a&#41; Build and Run
- Open a terminal in this directory (`roms-edc`, where this readme is located)
- Run the following command to build the necessary .jar files:
(Sometimes you have to use just "gradlew" instead of "./gradlew")
```bash
./gradlew launchers:connector:build
```
- Run the following cmd-command (will not work in powershell! Easier to make do in wsl or Linux) to execute `connector.jar`:
```bash
java -Dedc.keystore=resources/connector/certs/cert.pfx -Dedc.keystore.password=123456 -Dedc.vault=resources/connector/configuration/connector-vault.properties -Dedc.fs.config=resources/connector/configuration/connector-configuration.properties -jar launchers/connector/build/libs/connector.jar
```

### b&#41; Create an asset
- Run the postman collection found in the main folder. Adjust the variables there for your Usecase.

- The extension should now have written a debug output confirming that the CES now containts Gaia-X conform credentials of this offering. You can find them under listener/src/main/ressources/Gaia-X-Identity
