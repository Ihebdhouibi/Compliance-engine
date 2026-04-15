package com.devteam.aiauditserver.Tools.exception;


import com.devteam.aiauditserver.Tools.error.ApiBaseException;
import org.springframework.http.HttpStatus;

public class ConflictException extends ApiBaseException {

    public  ConflictException(String message){
        super(message);
    }
    @Override
    public HttpStatus getStatusCode() {
        return HttpStatus.CONFLICT;
    }
}
