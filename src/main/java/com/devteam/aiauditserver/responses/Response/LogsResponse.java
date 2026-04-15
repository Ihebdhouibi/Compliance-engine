package com.devteam.aiauditserver.responses.Response;

import java.util.Date;


public class LogsResponse {
    private String logs;
    private Date timestamp;

    public LogsResponse() {
        this.timestamp = new Date();

    }

    public LogsResponse(String logs) {
        this.logs = logs;
        this.timestamp = new Date();
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getLogs() {
        return logs;
    }

    public void setLogs(String logs) {
        this.logs = logs;
    }
}
