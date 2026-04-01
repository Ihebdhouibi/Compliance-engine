package com.devteam.aiauditserver.responses.Response;

import lombok.Data;

import java.util.Date;


public class TokenResponse {
    private String token;
    private Date expirationdate;

    public TokenResponse() {
    }

    public TokenResponse(String token, Date expirationdate) {
        this.token = token;
        this.expirationdate = expirationdate;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Date getExpirationdate() {
        return expirationdate;
    }

    public void setExpirationdate(Date expirationdate) {
        this.expirationdate = expirationdate;
    }
}
