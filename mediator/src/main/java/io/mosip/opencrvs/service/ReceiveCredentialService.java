package io.mosip.opencrvs.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.opencrvs.constant.LoggingConstants;
import io.mosip.opencrvs.dto.BaseEventRequest;
import io.mosip.opencrvs.dto.DecryptedWebsubCredential;
import io.mosip.opencrvs.dto.WebsubRequest;
import io.mosip.opencrvs.util.LogUtil;
import io.mosip.opencrvs.util.OpencrvsCryptoUtil;
import io.mosip.opencrvs.util.RestTokenUtil;
import io.mosip.opencrvs.util.RestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ReceiveCredentialService {
    private static final Logger LOGGER = LogUtil.getLogger(ReceiveCredentialService.class);

    @Value("${mosip.opencrvs.partner.client.id}")
    private String partnerClientId;
    @Value("${mosip.opencrvs.partner.client.sha.secret}")
    private String partnerClientShaSecret;
    @Value("${opencrvs.receive.credential.url}")
    private String opencrvsReceiveCredUrl;

    @Autowired
    private RestTokenUtil restTokenUtil;
    @Autowired
    private RestUtil restUtil;
    @Autowired
    private OpencrvsCryptoUtil opencrvsCryptoUtil;

    @Async
    public void tokenizeReceivedCredential(WebsubRequest credentialData){
        //get authtoken
        String token = restTokenUtil.getOpencrvsAuthToken("sending cred back to opencrvs");
        if(token==null || token.isEmpty()) return;

        //skip signature check
        String decryptedJson;
        try{
            decryptedJson = new String(opencrvsCryptoUtil.decrypt(CryptoUtil.decodeURLSafeBase64(credentialData.event.data.credential)));
        } catch(BaseCheckedException e){
            LOGGER.error(LoggingConstants.FORMATTER_PREFIX, LoggingConstants.SESSION,LoggingConstants.ID,"send credentials","Error decrypting Credentials: ", e);
            restUtil.publishStatusWebsub(credentialData, "serving_error_decrypt");
            return;
        }
        DecryptedWebsubCredential decryptedWebsubCredential;
        try {
            decryptedWebsubCredential = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .readValue(decryptedJson, DecryptedWebsubCredential.class);
        } catch(JsonProcessingException jpe) {
            LOGGER.error(LoggingConstants.FORMATTER_PREFIX, LoggingConstants.SESSION, LoggingConstants.ID, "send credentials", "Error parsing json", jpe);
            restUtil.publishStatusWebsub(credentialData, "serving_error_json_parse");
            return;
        }

        String uinToken;
        try {
            uinToken = restTokenUtil.mosipUINPartnerToken(decryptedWebsubCredential.credentialSubject.UIN);
        } catch(BaseCheckedException bce) {
            LOGGER.error(LoggingConstants.FORMATTER_PREFIX, LoggingConstants.SESSION,LoggingConstants.ID,"send credentials","Error Getting UIN Token", bce);
            restUtil.publishStatusWebsub(credentialData, "serving_error_uin_token");
            return;
        }
        BaseEventRequest encryptedCredentialResponse;
        try{
            encryptedCredentialResponse = opencrvsCryptoUtil.encryptSign("{\"uinToken\":\"" + uinToken + "\", \"opencrvsBRN\": \"" + credentialData.event.data.opencrvsBRN + "\"}");
        } catch(BaseCheckedException bce) {
            LOGGER.error(LoggingConstants.FORMATTER_PREFIX, LoggingConstants.SESSION,LoggingConstants.ID,"send credentials","Error Encrypting Data", bce);
            restUtil.publishStatusWebsub(credentialData, "serving_error_encrypting");
            return;
        }

        try{
            encryptedCredentialResponse.setRequestTime("");
            encryptedCredentialResponse.setId("");
            HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.setContentType(MediaType.APPLICATION_JSON);
            requestHeaders.add("Authorization","Bearer "+token);
            HttpEntity<BaseEventRequest> request = new HttpEntity<>(encryptedCredentialResponse, requestHeaders);
            String res = new RestTemplate().postForObject(opencrvsReceiveCredUrl,request,String.class);
            LOGGER.info(LoggingConstants.SESSION,LoggingConstants.ID,"send credentials","Sent Credentials. response - "+res);
        } catch (Exception e) {
            LOGGER.error(LoggingConstants.FORMATTER_PREFIX, LoggingConstants.SESSION,LoggingConstants.ID,"send credentials","Error sending Credentials: ", e);
            restUtil.publishStatusWebsub(credentialData, "serving_error_sending_back");
            return;
        }

        restUtil.publishStatusWebsub(credentialData, "served");
    }
}
