package com.devteam.aiauditserver.requests.Auth;

import javax.validation.constraints.NotBlank;

public class TokenRefreshRequest {
    @NotBlank
    private String refreshToken;
    @NotBlank
    private String token;
    @NotBlank
    private String deviceId;
    @NotBlank
    private String deviceType;
    @NotBlank
    private String ip;
   @NotBlank
   private String tokendevice;

    public String getTokendevice() {
        return tokendevice;
    }

    public void setTokendevice(String tokendevice) {
        this.tokendevice = tokendevice;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
