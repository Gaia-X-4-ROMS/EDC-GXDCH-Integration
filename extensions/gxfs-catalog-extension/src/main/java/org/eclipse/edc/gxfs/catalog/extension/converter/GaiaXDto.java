package org.eclipse.edc.gxfs.catalog.extension.converter;

import org.json.simple.JSONObject;

public class GaiaXDto implements Cloneable {

  private String companyName;
  private String start;
  private String end;
  private JSONObject carrier;
  private JSONObject termsAndConditions;
  private JSONObject legalParticipant;
  private JSONObject legalRegistrationNumber;
  private JSONObject serviceOffering;
  private JSONObject dataResource;
  private JSONObject physicalResource;
  private JSONObject instantiatedDataResource;
  private JSONObject serviceAccesPoint;

  public GaiaXDto() {
    start = "{ \"@context\": \"https://www.w3.org/2018/credentials/v1\","
              + "\"type\": \"VerifiablePresentation\"," + "\"verifiableCredential\": [";
    end = "]}";
  }

  @Override
  public String toString() {
    return start + " \n" + termsAndConditions.toJSONString() + ", \n"
              + legalParticipant.toJSONString() + ", \n"
              + legalRegistrationNumber.toJSONString() + ", \n"
              + serviceOffering.toJSONString() + ", \n" + dataResource.toJSONString()
              + " \n" + end;
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    GaiaXDto clone = new GaiaXDto();
    clone.setCompanyName(this.companyName);
    clone.setLegalParticipant(this.legalParticipant);
    clone.setLegalRegistrationNumber(this.legalRegistrationNumber);
    clone.setTermsAndConditions(this.termsAndConditions);
    
    clone.setCarrier((JSONObject) this.carrier.clone());
    clone.setDataResource((JSONObject) this.dataResource.clone());
    clone.setServiceOffering((JSONObject) this.serviceOffering.clone());
    clone.setPhysicalResource((JSONObject) this.physicalResource.clone());
    clone.setInstantiatedDataResource((JSONObject) this.instantiatedDataResource.clone());
    clone.setServiceAccesPoint((JSONObject) this.serviceAccesPoint.clone());
    return clone;
  }

  public JSONObject getTermsAndConditions() {
    return termsAndConditions;
  }

  public void setTermsAndConditions(JSONObject termsAndConditions) {
    this.termsAndConditions = termsAndConditions;
  }

  public JSONObject getLegalParticipant() {
    return legalParticipant;
  }

  public void setLegalParticipant(JSONObject legalParticipant) {
    this.legalParticipant = legalParticipant;
  }

  public JSONObject getLegalRegistrationNumber() {
    return legalRegistrationNumber;
  }

  public void setLegalRegistrationNumber(JSONObject legalRegristrationNumer) {
    this.legalRegistrationNumber = legalRegristrationNumer;
  }

  public JSONObject getServiceOffering() {
    return serviceOffering;
  }

  public void setServiceOffering(JSONObject serviceOffering) {
    this.serviceOffering = serviceOffering;
  }

  public JSONObject getDataResource() {
    return dataResource;
  }

  public void setDataResource(JSONObject dataResource) {
    this.dataResource = dataResource;
  }

  public JSONObject getCarrier() {
    return carrier;
  }

  public void setCarrier(JSONObject carrier) {
    this.carrier = carrier;
  }

  public JSONObject getPhysicalResource() {
    return physicalResource;
  }

  public void setPhysicalResource(JSONObject physicalResource) {
    this.physicalResource = physicalResource;
  }

  public JSONObject getInstantiatedDataResource() {
    return instantiatedDataResource;
  }

  public void setInstantiatedDataResource(JSONObject instantiatedDataResource) {
    this.instantiatedDataResource = instantiatedDataResource;
  }

  public JSONObject getServiceAccesPoint() {
    return serviceAccesPoint;
  }

  public void setServiceAccesPoint(JSONObject serviceAccesPoint) {
    this.serviceAccesPoint = serviceAccesPoint;
  }

  public String getCompanyName() {
    return companyName;
  }

  public void setCompanyName(String companyName) {
    this.companyName = companyName;
  }

}
