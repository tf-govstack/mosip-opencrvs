package io.mosip.opencrvs.controller;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.opencrvs.dto.BaseEventRequest;
import io.mosip.opencrvs.dto.ErrorResponse;
import io.mosip.opencrvs.dto.SimpleMessageResponse;
import io.mosip.opencrvs.service.Receiver;
import io.mosip.opencrvs.util.OpencrvsCryptoUtil;
import io.mosip.opencrvs.util.RestTokenUtil;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import io.mosip.kernel.core.logger.spi.Logger;

import io.mosip.opencrvs.constant.Constants;
import io.mosip.opencrvs.constant.LoggingConstants;
import io.mosip.opencrvs.error.ErrorCode;
import io.mosip.opencrvs.util.RestUtil;
import io.mosip.opencrvs.util.LogUtil;
import io.mosip.opencrvs.service.Producer;

@RestController
public class WebhooksRestController {

    private static final Logger LOGGER = LogUtil.getLogger(WebhooksRestController.class);

    @Autowired
    private Environment env;

    @Autowired
    private Producer producer;

    @Autowired
    private Receiver receiver;

    @Autowired
    private RestUtil restUtil;
    @Autowired
    private OpencrvsCryptoUtil opencrvsCryptoUtil;

    @Autowired
    private RestTokenUtil restTokenUtil;

    @ExceptionHandler(value=BaseCheckedException.class)
    public ResponseEntity<ErrorResponse> mediatorExceptionHandler(BaseCheckedException bce){
        if (bce.getErrorCode().equals(ErrorCode.SUBSCRIBE_FAILED_EXCEPTION_CODE) || bce.getErrorCode().equals(ErrorCode.ERROR_ROLE_AUTH_TOKEN_EXCEPTION_CODE) ) {
            return new ResponseEntity<>(ErrorResponse.setErrors(bce), HttpStatus.INTERNAL_SERVER_ERROR);
        } else if (bce.getErrorCode().equals(ErrorCode.VALIDATE_TOKEN_EXCEPTION_CODE)) {
            return new ResponseEntity<>(ErrorResponse.setErrors(bce), HttpStatus.UNAUTHORIZED);
        } else if (bce.getErrorCode().equals(ErrorCode.MISSING_ROLE_AUTH_TOKEN_EXCEPTION_CODE)) {
            return new ResponseEntity<>(ErrorResponse.setErrors(bce), HttpStatus.FORBIDDEN);
        } else {
            return ResponseEntity.badRequest().body(ErrorResponse.setErrors(bce));
        }
    }

    @PostMapping("/subscribe")
    public SimpleMessageResponse postSubscribe() throws BaseCheckedException {
        LOGGER.debug(LoggingConstants.SESSION, LoggingConstants.ID, "RestController", "POST /subscribe");
        restUtil.websubSubscribe();
        return SimpleMessageResponse.setResponseMessage("Successfully subscribed");
    }

    @PostMapping("/unsubscribe")
    public SimpleMessageResponse unsubscribe() {
        //until unsubscription
        return SimpleMessageResponse.setResponseMessage("unable to unsubscribe");
    }

    @PostMapping(value = "/birth", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SimpleMessageResponse postBirth(@CookieValue("Authorization") String authToken, @RequestBody BaseEventRequest body) throws BaseCheckedException{
        LOGGER.debug(LoggingConstants.SESSION, LoggingConstants.ID, "RestController", "POST /birth; data: " + body);

        restTokenUtil.validateToken(env.getProperty("mosip.iam.validate_endpoint"), authToken, null);

        opencrvsCryptoUtil.verifyThrowException(CryptoUtil.decodePlainBase64(body.getData()), CryptoUtil.decodePlainBase64(body.getSignature()));

        producer.produce(body.getId(), body.toString());

        return SimpleMessageResponse.setResponseMessage(Constants.PACKET_CREATION_STARTED);
    }

    @PostMapping(value = "/receiveCredentialBirth", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SimpleMessageResponse postReceiveUinOnBirth(@RequestBody String body) {
        restUtil.proxyPassReceivedCredential(body);
        return SimpleMessageResponse.setResponseMessage("Received");
    }

    @GetMapping("/receiveCredentialBirth")
    public ResponseEntity<String> getReceiveUinOnBirth(
            @CookieValue(value = "Authorization", required = false) String authToken,
            @RequestParam(value = "hub.topic") String topic,
            @RequestParam(value = "hub.mode") String mode,
            @RequestParam(value = "hub.reason", required = false) String reason,
            @RequestParam(value = "hub.challenge", required = false) String challenge
    ) {
        if (reason != null && !reason.isEmpty()) {
            LOGGER.info(LoggingConstants.SESSION, LoggingConstants.ID, "websubSubscribe", "Subscription Success request called. Data: {\"hub.topic\":\"" + topic + "\", \"hub.mode\":\"" + mode + "\",\"hub.reason\":\"" + reason + "\"}");
            return ResponseEntity.ok().build();
        }
        if (challenge != null && !challenge.isEmpty()) {
            LOGGER.info(LoggingConstants.SESSION, LoggingConstants.ID, "websubSubscribe", "Subscription Verification request called. Data: {\"hub.topic\":\"" + topic + "\", \"hub.mode\":\"" + mode + "\",\"hub.challenge\":\"" + challenge + "\"}");
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/death", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SimpleMessageResponse postDeath(@CookieValue("Authorization") String authToken, @RequestBody String body) throws BaseCheckedException {
        LOGGER.debug(LoggingConstants.SESSION, LoggingConstants.ID, "RestController", "POST /death; data: " + body);

        restTokenUtil.validateToken(env.getProperty("mosip.iam.validate_endpoint"), authToken, null);

        //work this out

        LOGGER.info(LoggingConstants.SESSION, LoggingConstants.ID, "deactivateUIN","UIN Deactivated");
        return SimpleMessageResponse.setResponseMessage("Received");
    }
}
