package com.devteam.aiauditserver.responses.Response.Notification;

import lombok.Data;

@Data
public class FirebaseMessagingResponse {
    private String response;
    private Exception exception;

    public FirebaseMessagingResponse(String response) {
        this.response = response;
    }

    public FirebaseMessagingResponse(Exception exception) {
        this.exception = exception;
    }

    public boolean isSuccessful() {
        return response != null && exception == null;
    }

    public Exception getException() {
        return exception;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }
}
