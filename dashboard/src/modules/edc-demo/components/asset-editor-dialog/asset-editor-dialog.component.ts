import { Component, Inject, OnInit } from '@angular/core';
import { AssetInput } from "@think-it-labs/edc-connector-client";
import { MatDialogRef } from "@angular/material/dialog";
import { StorageType } from "../../models/storage-type";


@Component({
  selector: 'edc-demo-asset-editor-dialog',
  templateUrl: './asset-editor-dialog.component.html',
  styleUrls: ['./asset-editor-dialog.component.scss']
})
export class AssetEditorDialog implements OnInit {

  id: string = '';
  version: string = '';
  name: string = '';
  contenttype: string = '';
  description: string = '';

  storageTypeId: string = '';
  selectedStorageTypeId: string = '';

  //Azure
  account: string = '';
  container: string = 'src-container';
  blobname: string = '';

  //Http
  baseUrl: string = '';

  //Kafka
  kafkaServer: string = '';
  kafkaTopic: string = '';

  //Gaia-X
  gaia_service_offering_name: string = '';
  gaia_service_offering_desc: string = '';
  gaia_service_offering_endpoint: string = '';
  gaia_service_offering_protocol: string = '';
  gaia_service_offering_host: string = '';
  gaia_service_offering_port: string = '';

  constructor(private dialogRef: MatDialogRef<AssetEditorDialog>,
      @Inject('STORAGE_TYPES') public storageTypes: StorageType[]) {
  }

  ngOnInit(): void {
  }

  onSave() {
    const assetInput: AssetInput = {
      "@id": this.id,
      properties: {
        "name": this.name,
        "version": this.version,
        "contenttype": this.contenttype,
        "serviceOfferingName": this.gaia_service_offering_name,
        "serviceOfferingDescription": this.gaia_service_offering_desc,
        "description": this.description,
        "endPoint": this.gaia_service_offering_endpoint,
        "protocol": this.gaia_service_offering_protocol,
        "host": this.gaia_service_offering_host,
        "port": this.gaia_service_offering_port
      },
      dataAddress: {
        "type": this.storageTypeId,
        //zusätzliche Daten müssten eigentlich in "properties":{} gewrappt werden, scheint aber auch so zu gehen
        //Der Kram wird in AssetInput gepackt aus der Lib: @think-it-labs/edc-connector-client
        ...(this.storageTypeId === 'azureStorage' && {
          "account": this.account,
          "container": this.container,
          "blobname": this.blobname,
          "keyName": `${this.account}-key1`
        }),
        ...(this.storageTypeId === 'HttpData' && {
          "name": this.name,
          "baseUrl": this.baseUrl
        }),
        ...(this.storageTypeId === 'Kafka' && {
          "name": this.name,
          "kafka.bootstrap.servers": this.kafkaServer,
          "type": "Kafka",
          "topic": this.kafkaTopic
        })
      }
    };

    this.dialogRef.close({ assetInput });
  }
}
