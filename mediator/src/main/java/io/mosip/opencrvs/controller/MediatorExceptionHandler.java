package io.mosip.opencrvs.controller;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.opencrvs.constant.LoggingConstants;
import io.mosip.opencrvs.dto.ErrorResponse;
import io.mosip.opencrvs.error.ErrorCode;
import io.mosip.opencrvs.util.LogUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class MediatorExceptionHandler {

    private static final Logger LOGGER = LogUtil.getLogger(MediatorExceptionHandler.class);
    
    @ExceptionHandler(value= BaseCheckedException.class)
    public ResponseEntity<ErrorResponse> mediatorExceptionHandler(BaseCheckedException bce){
        LOGGER.error(LoggingConstants.FORMATTER_PREFIX, LoggingConstants.SESSION, LoggingConstants.ID, "Mediator Exception Handler", "Exception Caught ", bce);
        if (ErrorCode.SUBSCRIBE_FAILED_EXCEPTION.compareBase(bce) || ErrorCode.ERROR_ROLE_AUTH_TOKEN_EXCEPTION.compareBase(bce)) {
            return new ResponseEntity<>(ErrorResponse.setErrors(bce), HttpStatus.INTERNAL_SERVER_ERROR);
        } else if (ErrorCode.VALIDATE_TOKEN_EXCEPTION.compareBase(bce) || ErrorCode.MISSING_TOKEN_EXCEPTION.compareBase(bce)) {
            return new ResponseEntity<>(ErrorResponse.setErrors(bce), HttpStatus.UNAUTHORIZED);
        } else if (ErrorCode.MISSING_ROLE_AUTH_TOKEN_EXCEPTION.compareBase(bce)) {
            return new ResponseEntity<>(ErrorResponse.setErrors(bce), HttpStatus.FORBIDDEN);
        } else {
            return ResponseEntity.badRequest().body(ErrorResponse.setErrors(bce));
        }
    }
}
