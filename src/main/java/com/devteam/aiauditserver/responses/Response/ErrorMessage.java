package com.devteam.aiauditserver.responses.Response;


import com.devteam.aiauditserver.enums.Tools.ErrorCodeEnum;
import com.devteam.aiauditserver.enums.Tools.ResponseMessage;
import org.springframework.http.HttpStatus;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ErrorMessage {
    private String message;
    private String title;
    private HttpStatus error;
    private int status;
    private String code;
    private String timestamp;
    private Object data;
    private String requestId;
    private String description;

    public ErrorMessage() {
    }

    public ErrorMessage(String message, HttpStatus error, ErrorCodeEnum code) {
        this.message = message;
        this.error = error;
        this.code = code.toString();
    }

    public ErrorMessage(String requestId, String message, HttpStatus error, ErrorCodeEnum code) {
        this(message, error, code);
        this.requestId = requestId;
    }

    public ErrorMessage(int statusCode, Date timestamp, String message, String description) {
        this.status = statusCode;
        this.timestamp = convertDateToString(timestamp);;
        this.message = message;
        this.description = description;
    }


    private String convertDateToString(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(date);
    }

    private Date convertStringToDate(String dateString) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            return dateFormat.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public ErrorMessage(ResponseMessage message, HttpStatus error) {
        this(message.toString(), error, message.getErrorCode());
    }

    public ErrorMessage(String requestId, ResponseMessage message, HttpStatus error) {
        this(message.toString(), error, message.getErrorCode());
        this.requestId = requestId;
    }

    public ErrorMessage(ResponseMessage message, HttpStatus error, Object data) {
        this(message.toString(), error, message.getErrorCode());
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public HttpStatus getError() {
        return error;
    }

    public void setError(HttpStatus httpStatus) {
        this.error = httpStatus;
        this.status = httpStatus.value();
    }

    public int getStatus() {
        return error.value();
    }

    public void setStatus(int status) {
        error = HttpStatus.valueOf(status);
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTimestamp() {
        return new Date().toString();
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "ErrorMessage{" +
                "message='" + message + '\'' +
                ", title='" + title + '\'' +
                ", error=" + error +
                ", status=" + status +
                ", code='" + code + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", data=" + data +
                ", requestId='" + requestId + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
