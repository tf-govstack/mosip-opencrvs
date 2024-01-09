package io.mosip.opencrvs.controller;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.opencrvs.constant.LoggingConstants;
import io.mosip.opencrvs.dto.SimpleMessageResponse;
import io.mosip.opencrvs.dto.WebsubRequest;
import io.mosip.opencrvs.service.ReceiveCredentialService;
import io.mosip.opencrvs.util.LogUtil;
import io.mosip.opencrvs.util.RestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal")
public class InternalRestController {
    private static final Logger LOGGER = LogUtil.getLogger(InternalRestController.class);

    @Autowired
    private ReceiveCredentialService receiveCredentialService;

    @Autowired
    private RestUtil restUtil;

    @PostMapping("/unsubscribe")
    public SimpleMessageResponse unsubscribe() {
        return SimpleMessageResponse.setResponseMessage("unable to unsubscribe");
    }

    @PostMapping(value = "/receiveCredentialBirth", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SimpleMessageResponse postReceiveUinOnBirth(@RequestBody WebsubRequest body) {
        LOGGER.info(LoggingConstants.SESSION, LoggingConstants.ID, "postReceiveUinOnBirth", "Here is the request received - " + body);
        receiveCredentialService.tokenizeReceivedCredential(body);
        return SimpleMessageResponse.setResponseMessage("Received");
    }

    @PostMapping("/subscribe")
    public SimpleMessageResponse postSubscribe() throws BaseCheckedException {
        LOGGER.debug(LoggingConstants.SESSION, LoggingConstants.ID, "RestController", "POST /subscribe");
        restUtil.websubSubscribe();
        return SimpleMessageResponse.setResponseMessage("Successfully subscribed");
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
}
