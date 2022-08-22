package io.mosip.opencrvs.dto;

import io.mosip.kernel.core.exception.BaseCheckedException;
import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
public class ErrorResponse extends SimpleMessageResponse{
    public static class Error{
        public String errorCode;
        public String errorMessage;
        public Error(String code, String message){
            errorCode=code;
            errorMessage=message;
        }
    }
    private List<Error> errors;

    public static ErrorResponse setErrors(Error ... errors){
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.errors = Arrays.asList(errors);
        return errorResponse;
    }

    public static ErrorResponse setErrors(BaseCheckedException ... exceptions){
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.errors = new ArrayList<>();
        for(BaseCheckedException bce : exceptions){
            errorResponse.errors.add(new Error(bce.getErrorCode(),bce.getErrorText()));
        }
        return errorResponse;
    }
}
