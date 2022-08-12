package io.mosip.opencrvs.util;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.opencrvs.constant.LoggingConstants;
import io.mosip.opencrvs.error.ErrorCode;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;

@Component
public class RestTokenUtil {
    private static final Logger LOGGER = LogUtil.getLogger(RestTokenUtil.class);

    @Value("${mosip.opencrvs.client.id}")
    private String mosipClientId;
    @Value("${mosip.opencrvs.client.secret.key}")
    private String mosipClientSecret;
    @Value("${mosip.opencrvs.partner.client.id}")
    private String partnerClientId;
    @Value("${mosip.opencrvs.partner.client.secret}")
    private String partnerClientSecret;
    @Value("${mosip.opencrvs.partner.username}")
    private String partnerUsername;
    @Value("${mosip.opencrvs.partner.password}")
    private String partnerPassword;
    @Value("${mosip.iam.token_endpoint}")
    private String iamTokenEndpoint;
    @Value("${opencrvs.client.id}")
    private String opencrvsClientId;
    @Value("${opencrvs.client.secret.key}")
    private String opencrvsClientSecret;
    @Value("${opencrvs.client.sha.secret}")
    private String opencrvsClientShaSecret;
    @Value("${opencrvs.auth.url}")
    private String opencrvsAuthUrl;

    private String getOIDCToken(String tokenEndpoint, String clientId, String clientSecret, String username, String password, String grantType) throws BaseCheckedException {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.set("grant_type", grantType);
        formData.set("client_id", clientId);
        if(clientSecret!=null)formData.set("client_secret", clientSecret);
        if(username!=null)formData.set("username", username);
        if(password!=null)formData.set("password", password);

        try {
            String responseJson = new RestTemplate().postForObject(tokenEndpoint, formData, String.class);
            if (responseJson == null || responseJson.isEmpty()) {
                throw new BaseCheckedException(ErrorCode.TOKEN_GENERATION_FAILED_CODE, ErrorCode.TOKEN_GENERATION_FAILED_MESSAGE);
            }
            return new JSONObject(responseJson).getString("access_token");
        } catch (JSONException | RestClientException e) {
            throw new BaseCheckedException(ErrorCode.TOKEN_GENERATION_FAILED_CODE, ErrorCode.TOKEN_GENERATION_FAILED_MESSAGE, e);
        }
    }

    private String getOIDCToken(String tokenEndpoint, String clientId, String clientSecret, String username, String password) throws BaseCheckedException{
        return getOIDCToken(tokenEndpoint,clientId,clientSecret,username,password,"password");
    }

    private String getOIDCToken(String tokenEndpoint, String clientId, String username, String password) throws BaseCheckedException {
        return getOIDCToken(tokenEndpoint,clientId,null,username,password);
    }

    private String getOIDCToken(String tokenEndpoint, String clientId, String clientSecret) throws BaseCheckedException {
        return getOIDCToken(tokenEndpoint,clientId,clientSecret,null,null,"client_credentials");
    }

    private String getOpencrvsAuthTokenInterface(String tokenEndpoint, String clientId, String clientSecret) throws BaseCheckedException{
        String body = "{" +
            "\"client_id\":\"" + clientId + "\"" + "," +
            "\"client_secret\":\"" + clientSecret + "\"" +
        "}";
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(body, requestHeaders);
        try{
            String responseJson = new RestTemplate().postForObject(tokenEndpoint,request,String.class);
            if (responseJson == null || responseJson.isEmpty()) {
                throw new BaseCheckedException(ErrorCode.TOKEN_GENERATION_FAILED_CODE, ErrorCode.TOKEN_GENERATION_FAILED_MESSAGE);
            }
            return new JSONObject(responseJson).getString("token");
        } catch (JSONException | RestClientException e) {
            throw new BaseCheckedException(ErrorCode.TOKEN_GENERATION_FAILED_CODE, ErrorCode.TOKEN_GENERATION_FAILED_MESSAGE, e);
        }
    }

    public String getMosipAuthToken(String context) {
        try{
            return getOIDCToken(iamTokenEndpoint,mosipClientId,mosipClientSecret);
        } catch (Exception e) {
            LOGGER.error(LoggingConstants.SESSION,LoggingConstants.ID,context,"Error getting mosip auth token "+ ExceptionUtils.getStackTrace(e));
            return null;
        }
    }

    public String getPartnerAuthToken(String context){
        try {
            return getOIDCToken(iamTokenEndpoint,partnerClientId,partnerClientSecret,partnerUsername,partnerPassword);
        } catch (Exception e) {
            LOGGER.error(LoggingConstants.SESSION,LoggingConstants.ID,context,"Error getting partner auth token "+ ExceptionUtils.getStackTrace(e));
            return null;
        }
    }

    public String getOpencrvsAuthToken(String context){
        try {
            return getOpencrvsAuthTokenInterface(opencrvsAuthUrl,opencrvsClientId,opencrvsClientSecret);
        } catch (Exception e) {
            LOGGER.error(LoggingConstants.SESSION,LoggingConstants.ID,context,"Error getting opencrvs auth token "+ ExceptionUtils.getStackTrace(e));
            return null;
        }
    }

    public void validateToken(String validateEndpoint, String authToken, List<String> requiredRoleList) throws BaseCheckedException{
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("Authorization","Bearer " + authToken);
        HttpEntity<String> request = new HttpEntity<>(requestHeaders);
        try{
            ResponseEntity<String> res = new RestTemplate().exchange(validateEndpoint, HttpMethod.GET,request,String.class);
        } catch(Exception e){
            throw new BaseCheckedException(ErrorCode.VALIDATE_TOKEN_EXCEPTION_CODE,ErrorCode.VALIDATE_TOKEN_EXCEPTION_MESSAGE);
        }

        if(requiredRoleList==null) return;
        // check if required roles are there.
        try{
            String tokenD = new String(Base64.getDecoder().decode(authToken.split("\\.")[1]));
            String realmRoles = new JSONObject(tokenD).getJSONObject("realm_access").getJSONArray("roles").toString();
            for(String role: requiredRoleList){
                if(!realmRoles.contains("\""+role+"\"")) throw new BaseCheckedException(ErrorCode.MISSING_ROLE_AUTH_TOKEN_EXCEPTION_CODE,ErrorCode.MISSING_ROLE_AUTH_TOKEN_EXCEPTION_MESSAGE);
            }
        } catch(JSONException e) {
            throw new BaseCheckedException(ErrorCode.ERROR_ROLE_AUTH_TOKEN_EXCEPTION_CODE,ErrorCode.ERROR_ROLE_AUTH_TOKEN_EXCEPTION_MESSAGE,e);
        }
    }

}
