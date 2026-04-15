package com.devteam.aiauditserver.requests.Auth;

// Lombok annotations
public class DeviceInfo {

    // Payload Validators
    private String deviceId;
    private String deviceType;

    public DeviceInfo() {
    }

    public DeviceInfo(String deviceId, String deviceType) {
        this.deviceId = deviceId;
        this.deviceType = deviceType;
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
}
