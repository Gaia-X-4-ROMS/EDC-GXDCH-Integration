package org.eclipse.edc.gxfs.catalog.extension.listener;

public class TokenDto {

  String accessToken;
  String refreshToken;

  public TokenDto(String accessToken, String refreshToken) {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

}
