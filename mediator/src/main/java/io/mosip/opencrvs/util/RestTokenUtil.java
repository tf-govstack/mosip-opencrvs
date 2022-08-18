package io.mosip.opencrvs.util;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.opencrvs.constant.ApiName;
import io.mosip.opencrvs.constant.LoggingConstants;
import io.mosip.opencrvs.error.ErrorCode;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

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

    @Value("${mosip.opencrvs.uin.token.intermediary.partner:mpartner-default-auth}")
    private String uinTokenIntermediaryPartnerId;
    @Value("${mosip.opencrvs.uin.token.partner}")
    private String uinTokenPartnerId;
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

    @Autowired
    private Environment env;

    public String getOIDCToken(String tokenEndpoint, String clientId, String clientSecret, String username, String password, String grantType) throws BaseCheckedException {
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

    public String getOIDCToken(String tokenEndpoint, String clientId, String clientSecret, String username, String password) throws BaseCheckedException{
        return getOIDCToken(tokenEndpoint,clientId,clientSecret,username,password,"password");
    }

    public String getOIDCToken(String tokenEndpoint, String clientId, String username, String password) throws BaseCheckedException {
        return getOIDCToken(tokenEndpoint,clientId,null,username,password);
    }

    public String getOIDCToken(String tokenEndpoint, String clientId, String clientSecret) throws BaseCheckedException {
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
            LOGGER.error(LoggingConstants.FORMATTER_PREFIX, LoggingConstants.SESSION,LoggingConstants.ID,context,"Error getting mosip auth token ", e);
            return null;
        }
    }

    public String getPartnerAuthToken(String context){
        try {
            return getOIDCToken(iamTokenEndpoint,partnerClientId,partnerClientSecret,partnerUsername,partnerPassword);
        } catch (Exception e) {
            LOGGER.error(LoggingConstants.FORMATTER_PREFIX, LoggingConstants.SESSION,LoggingConstants.ID,context,"Error getting partner auth token ", e);
            return null;
        }
    }

    public String getOpencrvsAuthToken(String context){
        if(opencrvsAuthUrl==null || opencrvsAuthUrl.isEmpty()) return "Authorization";
        try {
            return getOpencrvsAuthTokenInterface(opencrvsAuthUrl,opencrvsClientId,opencrvsClientSecret);
        } catch (Exception e) {
            LOGGER.error(LoggingConstants.FORMATTER_PREFIX, LoggingConstants.SESSION,LoggingConstants.ID,context,"Error getting opencrvs auth token ", e);
            return null;
        }
    }

    public void validateToken(String validateEndpoint, String authToken, List<String> requiredRoleList) throws BaseCheckedException{
        if(validateEndpoint==null || validateEndpoint.isEmpty()) return;
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

    public String mosipUINPartnerToken(String uin) throws BaseCheckedException{
        String mosipAuthToken = getMosipAuthToken("getting MOSIP UIN Partner Token");
        if (mosipAuthToken==null || mosipAuthToken.isEmpty()){
            throw new BaseCheckedException(ErrorCode.TOKEN_GENERATION_FAILED_CODE, ErrorCode.TOKEN_GENERATION_FAILED_MESSAGE);
        }
        String apiUrl = env.getProperty(ApiName.KEYMANAGER_TOKENID);
        apiUrl = UriComponentsBuilder.fromHttpUrl(apiUrl).pathSegment(uin, uinTokenIntermediaryPartnerId).toUriString();
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("Cookie","Authorization=" + mosipAuthToken);
        HttpEntity<String> request = new HttpEntity<>(requestHeaders);
        String intermediaryUINToken;
        try {
            String responseString = new RestTemplate().exchange(apiUrl, HttpMethod.GET, request, String.class).getBody();
            JSONObject res = new JSONObject(responseString);
            intermediaryUINToken = res.getJSONObject("response").getString("tokenID");
        } catch(RestClientException | JSONException e) {
            throw new BaseCheckedException(ErrorCode.UNKNOWN_EXCEPTION_CODE, ErrorCode.UNKNOWN_EXCEPTION_MESSAGE, e);
        }
        apiUrl = UriComponentsBuilder.fromHttpUrl(apiUrl).pathSegment(intermediaryUINToken, uinTokenPartnerId).toUriString();
        try {
            String responseString = new RestTemplate().exchange(apiUrl, HttpMethod.GET, request, String.class).getBody();
            JSONObject res = new JSONObject(responseString);
            return res.getJSONObject("response").getString("tokenID");
        } catch(RestClientException | JSONException e) {
            throw new BaseCheckedException(ErrorCode.UNKNOWN_EXCEPTION_CODE, ErrorCode.UNKNOWN_EXCEPTION_MESSAGE, e);
        }
    }

}
