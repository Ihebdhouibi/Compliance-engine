package com.devteam.aiauditserver.Tools.exception;


import com.devteam.aiauditserver.Tools.error.ApiBaseException;
import org.springframework.http.HttpStatus;

public class NotFoundException extends ApiBaseException {
    public NotFoundException(String message){
        super(message);
    }
    @Override
    public HttpStatus getStatusCode(){
        return HttpStatus.NOT_FOUND;
    }
}
