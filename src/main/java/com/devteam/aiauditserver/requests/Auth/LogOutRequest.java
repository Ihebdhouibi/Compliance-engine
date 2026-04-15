package com.devteam.aiauditserver.requests.Auth;

import lombok.Data;

public class LogOutRequest {
    private String token;

    public LogOutRequest() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
