package io.mosip.opencrvs.controller;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.opencrvs.dto.BaseEventRequest;
import io.mosip.opencrvs.dto.SimpleMessageResponse;
import io.mosip.opencrvs.error.ErrorCode;
import io.mosip.opencrvs.service.DeathEventHandlerService;
import io.mosip.opencrvs.service.Receiver;
import io.mosip.opencrvs.util.OpencrvsCryptoUtil;
import io.mosip.opencrvs.util.RestTokenUtil;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;

import io.mosip.kernel.core.logger.spi.Logger;

import io.mosip.opencrvs.constant.Constants;
import io.mosip.opencrvs.constant.LoggingConstants;
import io.mosip.opencrvs.util.LogUtil;
import io.mosip.opencrvs.service.Producer;

import javax.servlet.http.HttpServletRequest;

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
    private DeathEventHandlerService deathEventService;

    @Autowired
    private OpencrvsCryptoUtil opencrvsCryptoUtil;

    @Autowired
    private RestTokenUtil restTokenUtil;

    @PostMapping(value = "/birth", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SimpleMessageResponse postBirth(HttpServletRequest request, @RequestBody BaseEventRequest body) throws BaseCheckedException{
        LOGGER.debug(LoggingConstants.SESSION, LoggingConstants.ID, "RestController", "POST /birth; data: " + body);

        restTokenUtil.validateToken(env.getProperty("mosip.iam.validate_endpoint"), request.getCookies(), null);

        opencrvsCryptoUtil.verifyThrowException(CryptoUtil.decodePlainBase64(body.getData()), CryptoUtil.decodePlainBase64(body.getSignature()));

        producer.produce(body.getId(), body.toString());

        return SimpleMessageResponse.setResponseMessage(Constants.PACKET_CREATION_STARTED);
    }

    @PostMapping(value = "/death", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SimpleMessageResponse postDeath(HttpServletRequest request, @RequestBody BaseEventRequest body) throws BaseCheckedException {
        LOGGER.debug(LoggingConstants.SESSION, LoggingConstants.ID, "RestController", "POST /death; data: " + body);

        restTokenUtil.validateToken(env.getProperty("mosip.iam.validate_endpoint"), request.getCookies(), null);

        String message = deathEventService.handleEvent(body);

        LOGGER.info(LoggingConstants.SESSION, LoggingConstants.ID, "DeathEvent","Message - " + message);
        LOGGER.info(LoggingConstants.SESSION, LoggingConstants.ID, "DeathEvent","UIN Deactivated");
        return SimpleMessageResponse.setResponseMessage(message);
    }

    @GetMapping(value = "/generateRid")
    public String generateRid(HttpServletRequest request) throws BaseCheckedException{
        restTokenUtil.validateToken(env.getProperty("mosip.iam.validate_endpoint"), request.getCookies(), null);

        try{
            return "\"" + receiver.generateDefaultRegistrationId() + "\"";
        } catch(Exception e){
            LOGGER.error(LoggingConstants.FORMATTER_PREFIX, LoggingConstants.SESSION, LoggingConstants.ID,"WebhooksRestController::generateRid","Unknown Error generating rid",e);
            throw ErrorCode.RID_GENERATE_EXCEPTION.throwChecked();
        }
    }
}
