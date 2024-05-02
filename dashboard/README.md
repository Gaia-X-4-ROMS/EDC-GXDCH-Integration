# EDC Data Dashboard

This Data Dashboard is based on the [Eclipse EDC DataDashboard](https://github.com/eclipse-edc/DataDashboard/tree/main).
Some adjustments have been made: 
- When creating assets, it is possible to specify additional fields to support the publication of ServiceOfferings in the GXFS catalog (if the "listener" extension is used in the connector).
- The creation of httpdata assets is supported by the GUI.

**Please note: This repository does not contain production-grade code and is only intended for demonstration purposes.**

EDC Data Dashboard is a dev frontend application for [EDC Management API](https://github.com/eclipse-edc/Connector).

## Running the frontend locally

### 1. Configure the necessary parameters.
The parameters are stored in  `edc-data-dashboard\src\assets\config\app.config.json`, and by default contain the following:

```json
{
  "managementApiUrl": "{{managementApiUrl}}",
  "catalogUrl": "{{catalogUrl}}",
  "storageAccount": "{{account}}",
  "storageExplorerLinkTemplate": "storageexplorer://v=1&accountid=/subscriptions/{{subscriptionId}}/resourceGroups/{{resourceGroup}}/providers/Microsoft.Storage/storageAccounts/{{account}}&subscriptionid={{subscriptionId}}&resourcetype=Azure.BlobContainer&resourcename={{container}}",
}
```
Substitute the values as necessary:
- `apiKey`: enter here what your EDC instance expects in th `x-api-key` header
- `catalogUrl`: prepend your connector URL, e.g. `http://localhost`, assuming your catalog endpoint is exposed at port 8181, which is the default
- `managementApiUrl`:  prepend your connector URL, e.g. `http://localhost`, assuming your IDS endpoint is exposed at port 9191
- `storageAccount`: insert the name of an Azure Blob Storage account to which the connector has access, otherwise Azure data transfers won't work.

### 2. Start the connector
If the connector and the dashboard run on different domains, additional CORS settings are required.
To start the consumer-with-listener from the ROMS-project:
```bash
cd /path/to/edc-event-listener
java -Dedc.keystore=consumer-with-listener/resources/certs/cert.pfx -Dedc.keystore.password=123456 -Dedc.vault=consumer-with-listener/resources/configuration/connector-vault.properties -Dedc.fs.config=consumer-with-listener/resources/configuration/connector-configuration.properties -Dedc.web.rest.cors.enabled="true" -Dedc.web.rest.cors.headers="origin,content-type,accept,authorization,x-api-key" -jar consumer-with-listener/build/libs/connector.jar
```
### 3. Build the dashboard
To build the dashboard locally, you must have node.js and npm installed on your computer.
```bash
cd /path/to/edc-data-dashboard
npm install
```
### 4. Start the dashboard
The dashboard will open in the default browser.
```bash
ng serve --open
```