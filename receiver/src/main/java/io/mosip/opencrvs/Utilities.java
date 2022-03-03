package io.mosip.opencrvs;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import org.json.JSONException;
import org.json.JSONObject;

import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.MultiValueMap;
import org.springframework.util.LinkedMultiValueMap;

import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.kernel.core.util.TokenHandlerUtil;
import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.exception.BaseUncheckedException;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.logger.logback.factory.Logfactory;

class Utilities{
  private static final Logger LOGGER = getLogger(Utilities.class);

  static Logger getLogger(Class<?> clazz) {
      return Logfactory.getSlf4jLogger(clazz);
  }

  static Map<String,String> getMetadata(String uin, String opencrvsBirthId,  String type){
    Map<String,String> map = new HashMap<>();
    map.put("metadata","{ \"REGISTRATIONTYPE\" : \"" + type + "\", \"uin\" : \"" + uin + "\", \"OPENCRVS_ID\" : \"" + opencrvsBirthId + "\" }");
    return map;
  }

  static List<Map<String, String>> generateAudit(String rid, String app_name, String  app_id) {
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
  static String getIdSchema(Double version, Map<Double,String> idschemaCache, String apiHostIpPort, RestTemplate selfTokenRestTemplate) throws BaseCheckedException, JSONException, IOException {
    String RESPONSE = "response";
    String SCHEMA_JSON = "schemaJson";
    String SCHEMA_VERSION_QUERY_PARAM = "schemaVersion";

    if (idschemaCache.get(version) != null)
      return idschemaCache.get(version);

    String response = (String) selfTokenRestTemplate.getForObject(apiHostIpPort +"?"+SCHEMA_VERSION_QUERY_PARAM+"="+version.toString(), String.class);
    LOGGER.debug(Constants.SESSION, Constants.ID,"null","Obtained this reponse from server for getting IdSchema "+response);

    if (response == null)
      throw new BaseCheckedException(Constants.API_RESOURCE_UNAVAILABLE_CODE,Constants.API_RESOURCE_UNAVAILABLE_2_MESSAGE + version);

    JSONObject jsonObject = new JSONObject(response);
    JSONObject respObj = jsonObject.getJSONObject(RESPONSE);
    String responseString = respObj != null ? (String) respObj.get(SCHEMA_JSON) : null;

    if (idschemaCache.get(version) == null)
      idschemaCache.put(version,responseString);

    return responseString;
  }
  static String getToken(Environment environment) throws IOException {
    MultiValueMap<String, String> formData = new LinkedMultiValueMap<String, String>();
    formData.set("grant_type","client_credentials");
    formData.set("client_id",environment.getProperty("mosip.opencrvs.client.id"));
    formData.set("client_secret",environment.getProperty("mosip.opencrvs.client.secret.key"));
    try{
      String responseJson = new RestTemplate().postForObject(environment.getProperty("mosip.iam.token_endpoint"), formData, String.class);
      if (responseJson==null || responseJson.isEmpty()) throw new BaseUncheckedException(Constants.TOKEN_GENERATION_FAILED_CODE, Constants.TOKEN_GENERATION_FAILED_MESSAGE);
      return (String)(new JSONObject(responseJson)).get("access_token");
    }
    catch(JSONException je){
      throw new BaseUncheckedException(Constants.TOKEN_GENERATION_FAILED_CODE, Constants.TOKEN_GENERATION_FAILED_MESSAGE);
    }
  }
}
