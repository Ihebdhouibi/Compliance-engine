package com.devteam.aiauditserver.Tools.exception;




import com.devteam.aiauditserver.enums.Tools.ErrorCodeEnum;
import com.devteam.aiauditserver.responses.Response.ErrorMessage;
import org.springframework.http.HttpStatus;

public class Exception extends RuntimeException {
    private ErrorMessage error;

    public Exception(ErrorMessage error) {
        super(error.getMessage());
        this.error = error;
    }

    public Exception(String message, HttpStatus httpStatus, ErrorCodeEnum errorCode) {
        this(new ErrorMessage(message, httpStatus, errorCode));
    }


}
