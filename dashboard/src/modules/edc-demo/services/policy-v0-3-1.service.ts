import {HttpClient, HttpErrorResponse, HttpHeaders, HttpParams} from '@angular/common/http';
import {Inject, Injectable} from '@angular/core';
import {CONNECTOR_CATALOG_API, CONNECTOR_MANAGEMENT_API} from "../../app/variables";
import {EMPTY, Observable} from 'rxjs';
import {catchError, map, reduce} from 'rxjs/operators';
import {PolicyDefinition, PolicyDefinitionInput, IdResponse} from "../../mgmt-api-client/model";


@Injectable({
  providedIn: 'root'
})
export class PolicyV031Service {

    constructor(
        private httpClient: HttpClient,
        @Inject(CONNECTOR_MANAGEMENT_API) private managementApiUrl: string,
        @Inject(CONNECTOR_CATALOG_API) private catalogApiUrl: string) {}
    
    createPolicy(policyDefinition: PolicyDefinitionInput): Observable<any> {
        // URL zum Aufruf
        const url = this.managementApiUrl + '/v2/policydefinitions';

        // JSON-Payload
        const payload = {
            "@context": {
                "edc": "https://w3id.org/edc/v0.0.1/ns/",
                "odrl": "http://www.w3.org/ns/odrl/2/"
            },
            "@id": policyDefinition['@id'], 
            "policy": {
                "@type": "set",
                "odrl:permission": policyDefinition.policy.permission,
                "odrl:prohibition": policyDefinition.policy.prohibition,
                "odrl:obligation": policyDefinition.policy.obligation
            }
        };

        const headers = new HttpHeaders()
            .set('Content-Type', 'application/json');

        //TODO catchError einbauen
        return this.httpClient.post(url, payload, { headers });
        //return this.catchError(this.httpClient.post<T>(url, payload, { headers })); //funzt so noch nicht
    }

    private catchError<T>(observable: Observable<T>): Observable<T> {
        return observable
        .pipe(
            catchError((httpErrorResponse: HttpErrorResponse) => {
                console.error(`Error creating policy, Method: 'POST', Error: '${httpErrorResponse.error.message}'`);
                return EMPTY;
            }));
    }

    
}