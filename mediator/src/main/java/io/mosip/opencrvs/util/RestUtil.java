package io.mosip.opencrvs.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.util.MultiValueMap;
import org.springframework.util.LinkedMultiValueMap;

import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.kernel.core.util.TokenHandlerUtil;
import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.kernel.core.logger.spi.Logger;

import io.mosip.opencrvs.constant.ApiName;
import io.mosip.opencrvs.constant.LoggingConstants;
import io.mosip.opencrvs.error.ErrorCode;

@Component
public class RestUtil{
  private static final Logger LOGGER = LogUtil.getLogger(RestUtil.class);

  @Autowired
  private Environment env;

  @Autowired
  private RestTemplate selfTokenRestTemplate;

  public List<Map<String, String>> generateAudit(String rid, String app_name, String  app_id) {
    // Getting Host IP Address and Name
    String hostIP = null;
    String hostName = null;
    try {
      hostIP = InetAddress.getLocalHost().getHostAddress();
      hostName = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException unknownHostException) {
      // logger log
      hostIP = "UNKNOWN-HOST";
      hostName = "UNKNOWN-HOST";
    }

    List<Map<String, String>> mapList = new ArrayList<>();

    Map<String, String> auditDtos = new HashMap<>();
    auditDtos.put("uuid", UUID.randomUUID().toString());
    auditDtos.put("createdAt", DateUtils.getUTCCurrentDateTimeString());
    auditDtos.put("eventId", "OPENCRVS_CREATE_PACKET");
    auditDtos.put("eventName", "opencrvs packet created");
    auditDtos.put("eventType", "OPENCRVS");
    auditDtos.put("actionTimeStamp", DateUtils.getUTCCurrentDateTimeString());
    auditDtos.put("hostName", hostName);
    auditDtos.put("hostIp", hostIP);
    auditDtos.put("applicationId", app_id);
    auditDtos.put("applicationName", app_name);
    auditDtos.put("id", rid);
    auditDtos.put("idType", "REGISTRATION_ID");

    mapList.add(auditDtos);

    return mapList;
  }
  public String getIdSchema(Double version, Map<Double,String> idschemaCache) throws BaseCheckedException {
    String apiNameMidSchemaUrl = env.getProperty(ApiName.MIDSCHEMAURL);

    if (idschemaCache.get(version) != null)
      return idschemaCache.get(version);

    String response;
    try{
      response= (String) selfTokenRestTemplate.getForObject(apiNameMidSchemaUrl +"?schemaVersion="+version.toString(), String.class);
    } catch(RestClientException e){
      throw new BaseCheckedException(ErrorCode.API_RESOURCE_UNAVAILABLE_CODE,ErrorCode.API_RESOURCE_UNAVAILABLE_2_MESSAGE, e);
    }
    LOGGER.debug(LoggingConstants.SESSION, LoggingConstants.ID,"RestUtil.getIdschema","Obtained this reponse from server for getting IdSchema "+response);
    if (response == null)
      throw new BaseCheckedException(ErrorCode.API_RESOURCE_UNAVAILABLE_CODE,ErrorCode.API_RESOURCE_UNAVAILABLE_2_MESSAGE + version);

    String responseString;
    try{
      JSONObject jsonObject = new JSONObject(response);
      JSONObject respObj = jsonObject.getJSONObject("response");
      responseString = respObj != null ? respObj.getString("schemaJson") : null;
    } catch(JSONException je) {
      LOGGER.error(LoggingConstants.SESSION,LoggingConstants.ID,"RestUtil.getIdschema", ErrorCode.JSON_PROCESSING_EXCEPTION_MESSAGE);
			throw new BaseUncheckedException(ErrorCode.JSON_PROCESSING_EXCEPTION_CODE, ErrorCode.JSON_PROCESSING_EXCEPTION_MESSAGE);
    }

    idschemaCache.putIfAbsent(version,responseString);

    return responseString;
  }
  public static String getMosipAuthToken(Environment env) throws BaseCheckedException{
    String clientId = env.getProperty("mosip.opencrvs.client.id");
    String clientSecret = env.getProperty("mosip.opencrvs.client.secret.key");
    String iamUrl = env.getProperty("mosip.iam.token_endpoint");

    MultiValueMap<String, String> formData = new LinkedMultiValueMap<String, String>();
    formData.set("grant_type","client_credentials");
    formData.set("client_id",clientId);
    formData.set("client_secret",clientSecret);

    String responseJson;
    try{
      responseJson = new RestTemplate().postForObject(iamUrl, formData, String.class);
    } catch(RestClientException e){
      throw new BaseCheckedException(ErrorCode.TOKEN_GENERATION_FAILED_CODE, ErrorCode.TOKEN_GENERATION_FAILED_MESSAGE,e);
    }
    if (responseJson==null || responseJson.isEmpty()) throw new BaseCheckedException(ErrorCode.TOKEN_GENERATION_FAILED_CODE, ErrorCode.TOKEN_GENERATION_FAILED_MESSAGE);
    try{
      return (String)(new JSONObject(responseJson)).get("access_token");
    }
    catch(JSONException je){
      throw new BaseUncheckedException(ErrorCode.TOKEN_GENERATION_FAILED_CODE, ErrorCode.TOKEN_GENERATION_FAILED_MESSAGE);
    }
  }

  public String webhooksSubscribe() throws Exception{
    //get authtoken
    String opencrvsClientId = env.getProperty("opencrvs.client.id");
    String opencrvsClientSecret = env.getProperty("opencrvs.client.secret.key");
    String opencrvsClientShaSecret = env.getProperty("opencrvs.client.sha.secret");
    String opencrvsAuthUrl = env.getProperty("opencrvs.auth.url");
    String opencrvsWebhooksUrl = env.getProperty("opencrvs.webhooks.url");
    String opencrvsCallbackUrl = env.getProperty("opencrvs.callback.url");

    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> request = new HttpEntity<>("{\"client_id\":\""+ opencrvsClientId +"\",\"client_secret\":\""+ opencrvsClientSecret +"\"}",requestHeaders);
    ResponseEntity<String> responseForRequest;
    try{
      responseForRequest = restTemplate.postForEntity(opencrvsAuthUrl, request, String.class);
    } catch(RestClientException e){
      throw new Exception(ErrorCode.AUTH_TOKEN_EXCEPTION);
    }
    if(!responseForRequest.getStatusCode().equals(HttpStatus.OK)){
      throw new Exception(ErrorCode.AUTH_TOKEN_EXCEPTION);
    }
    String token;
    try{token = (String) new JSONObject(responseForRequest.getBody()).get("token");}
    catch(JSONException e){
      throw new Exception(ErrorCode.TOKEN_PARSING_EXCEPTION);
    }

    //subscribe
    requestHeaders = new HttpHeaders();
    requestHeaders.setContentType(MediaType.APPLICATION_JSON);
    requestHeaders.set("Authorization","Bearer "+token);
    request = new HttpEntity<>("{\"hub\":{\"callback\":\""+ opencrvsCallbackUrl +"\",\"mode\":\"subscribe\",\"secret\":\""+ opencrvsClientShaSecret +"\",\"topic\":\"BIRTH_REGISTERED\"}}",requestHeaders);
    try{responseForRequest = restTemplate.postForEntity(opencrvsWebhooksUrl, request, String.class);}
    catch(RestClientException e){
      throw new Exception(ErrorCode.SUBSCRIBE_FAILED_EXCEPTION);
    }
    if(!responseForRequest.getStatusCode().equals(HttpStatus.ACCEPTED)){
      throw new Exception(ErrorCode.SUBSCRIBE_FAILED_EXCEPTION);
    }
    return responseForRequest.getBody();
  }

  public Map<String,String> getMetadata(String type, String uin, String centerId, String machineId, String opencrvsBirthId){
    Map<String,String> map = new HashMap<>();
    map.put("metadata","{ \"REGISTRATIONTYPE\" : \"" + type + "\", \"uin\" : \"" + uin + "\", \"centerId\":\""+centerId+"\", \"machineId\":\""+machineId+"\", \"opencrvsId\":\""+opencrvsBirthId+"\" }");
    return map;
  }

	public String getDefaultSource(){
		String provider = env.getProperty("provider.packetwriter.opencrvs");
		List<String> strList = Arrays.asList(provider.split(","));
		Optional<String> optional = strList.stream().filter(s -> s.contains("source")).findAny();
		String source = optional.isPresent() ? optional.get().replace("source:", "") : null;
		return source;
	}

	public String getDefaultProcess(){
		String provider = env.getProperty("provider.packetwriter.opencrvs");
		List<String> strList = Arrays.asList(provider.split(","));
		Optional<String> optional = strList.stream().filter(s -> s.contains("process")).findAny();
		String process = optional.isPresent() ? optional.get().replace("process:", "") : null;
		return process;
	}
}
