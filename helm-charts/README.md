# Helm Charts for edc connector and edc dashboard

This directory contains the helm charts for deployment of a connector or a connector dashboard.
Additionally there are two example value files, which need to be modified to conform to the right company, as well as to configure the external access.

## Startup

The directory includes a Makefile which contains commands for starting and stopping the connectors and dashboards. With
```
make connector-start
```
a single instance of the connector will be started in the current kubectl context in a namespace __gaiax__. The connector will use the overriden values from the `connector-override-values.yaml` file.

The dashboard can be started in a similar way:
```
make dashboard-start
```
It will be deployed in the same namespace as the connector and uses the overriden values from ```connector-dashboard-override-values.yaml```.

## Deployment Shutdown

Both components can be shut down by the following commands:
```
make connector-stop
make dashboard-stop
```
