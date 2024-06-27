import {HttpClient, HttpErrorResponse, HttpHeaders, HttpParams} from '@angular/common/http';
import {Inject, Injectable} from '@angular/core';
import {CONNECTOR_MANAGEMENT_API} from "../../app/variables";
import {ContractDefinitionInput} from "../../mgmt-api-client/model";
import {EMPTY, Observable} from 'rxjs';

@Injectable({
    providedIn: 'root'
})
export class ContractDefinitionV062Service {

    constructor(
        private httpClient: HttpClient,
        @Inject(CONNECTOR_MANAGEMENT_API) private managementApiUrl: string) {}

    createContractDefinition(input: ContractDefinitionInput): Observable<any> {
        // URL zum Aufruf
        const url = this.managementApiUrl + '/v2/contractdefinitions';
        
        // JSON-Payload
        const payload = {
            "@context": {
                "edc": "https://w3id.org/edc/v0.0.1/ns/"
            },
            "@id": input['@id'], 
            "accessPolicyId": input.accessPolicyId,
            "contractPolicyId": input.contractPolicyId,
            "assetsSelector": {
                "@type" : "CriterionDto",
                "operandLeft": input.assetsSelector[0].operandLeft, 
                "operator": "=",
                "operandRight": input.assetsSelector[0].operandRight[0]
            }
        };

        const headers = new HttpHeaders()
            .set('Content-Type', 'application/json');

        //TODO catchError einbauen
        return this.httpClient.post(url, payload, { headers });
    }

}