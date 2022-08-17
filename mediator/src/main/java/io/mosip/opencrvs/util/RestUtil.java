package io.mosip.opencrvs.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.opencrvs.dto.DecryptedWebsubCredential;
import io.mosip.opencrvs.dto.WebsubRequest;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.util.MultiValueMap;
import org.springframework.util.LinkedMultiValueMap;

import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.kernel.core.logger.spi.Logger;

import io.mosip.opencrvs.constant.ApiName;
import io.mosip.opencrvs.constant.LoggingConstants;
import io.mosip.opencrvs.error.ErrorCode;

@Component
public class RestUtil {
    private static final Logger LOGGER = LogUtil.getLogger(RestUtil.class);

    @Value("${mosip.opencrvs.partner.client.id}")
    private String partnerClientId;
    @Value("${mosip.opencrvs.partner.client.sha.secret}")
    private String partnerClientShaSecret;
    @Value("${opencrvs.receive.credential.url}")
    private String opencrvsReceiveCredUrl;
    @Value("${websub.hub.url}")
    private String mosipWebSubHubUrl;
    @Value("${mosip.receive.credential.url}")
    private String mosipReceiveCredCallback;

    @Autowired
    private Environment env;

    @Autowired
    private RestTokenUtil restTokenUtil;

    @Autowired
    private OpencrvsCryptoUtil opencrvsCryptoUtil;

    @Autowired
    private RestTemplate selfTokenRestTemplate;

    public List<Map<String, String>> generateAudit(String rid, String app_name, String app_id) {
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

    public String getIdSchema(Double version, Map<Double, String> idschemaCache) throws BaseCheckedException {
        String apiNameMidSchemaUrl = env.getProperty(ApiName.MIDSCHEMAURL);

        if (idschemaCache.get(version) != null)
            return idschemaCache.get(version);

        String response;
        try {
            response = selfTokenRestTemplate.getForObject(apiNameMidSchemaUrl + "?schemaVersion=" + version.toString(), String.class);
        } catch (RestClientException e) {
            throw new BaseCheckedException(ErrorCode.API_RESOURCE_UNAVAILABLE_CODE, ErrorCode.API_RESOURCE_UNAVAILABLE_2_MESSAGE, e);
        }
        LOGGER.debug(LoggingConstants.SESSION, LoggingConstants.ID, "RestUtil.getIdschema", "Obtained this reponse from server for getting IdSchema " + response);
        if (response == null)
            throw new BaseCheckedException(ErrorCode.API_RESOURCE_UNAVAILABLE_CODE, ErrorCode.API_RESOURCE_UNAVAILABLE_2_MESSAGE + version);

        String responseString;
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONObject respObj = jsonObject.getJSONObject("response");
            responseString = respObj != null ? respObj.getString("schemaJson") : null;
        } catch (JSONException je) {
            LOGGER.error(LoggingConstants.SESSION, LoggingConstants.ID, "RestUtil.getIdschema", ErrorCode.JSON_PROCESSING_EXCEPTION_MESSAGE);
            throw new BaseUncheckedException(ErrorCode.JSON_PROCESSING_EXCEPTION_CODE, ErrorCode.JSON_PROCESSING_EXCEPTION_MESSAGE);
        }

        idschemaCache.putIfAbsent(version, responseString);

        return responseString;
    }

    @Scheduled(fixedDelayString = "${mosip.opencrvs.websub.resubscribe.delay.ms}",
            initialDelayString = "${mosip.opencrvs.websub.resubscribe.init.delay.ms}")
    public void initSubscriptions() {
        if(!"true".equalsIgnoreCase(env.getProperty("mosip.opencrvs.websub.resubscribe"))) return;
        LOGGER.info(LoggingConstants.SESSION, LoggingConstants.ID, "websubSubscribe", "Initializing Websub Subscriptions");
        try {
            websubSubscribe();
        } catch (Exception e) {
            LOGGER.error(LoggingConstants.FORMATTER_PREFIX, LoggingConstants.SESSION, LoggingConstants.ID, "websubSubscribe", "Error in init websub Subscriptions: ", e);
        }
    }

    public void websubSubscribe() throws BaseCheckedException {
        //get authtoken
        String token = restTokenUtil.getPartnerAuthToken("subscribe to websub");
        if(token==null || token.isEmpty()) throw new BaseCheckedException(ErrorCode.AUTH_TOKEN_EXCEPTION_CODE, ErrorCode.AUTH_TOKEN_EXCEPTION_MESSAGE);

        LOGGER.debug(LoggingConstants.SESSION,LoggingConstants.ID,"websubSubscribe","Here partner Auth token: "+token);

        //subscribe
        try {
            HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.set("Cookie", "Authorization=" + token);
            MultiValueMap<String, String> request = new LinkedMultiValueMap<>();
            request.set("hub.callback",mosipReceiveCredCallback);
            request.set("hub.secret",partnerClientShaSecret);
            request.set("hub.mode","subscribe");
            request.set("hub.topic",partnerClientId +"/CREDENTIAL_ISSUED");
            request.set("hub.lease_seconds",env.getProperty("mosip.opencrvs.websub.resubscribe.lease.seconds"));
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(request, requestHeaders);
            String res = new RestTemplate().postForObject(mosipWebSubHubUrl, requestEntity, String.class);
        } catch (Exception e) {
            LOGGER.error(LoggingConstants.FORMATTER_PREFIX, LoggingConstants.SESSION, LoggingConstants.ID, "subscribe to websub", "Failed to subscribe. Exception: ", e);
            throw new BaseCheckedException(ErrorCode.SUBSCRIBE_FAILED_EXCEPTION_CODE, ErrorCode.SUBSCRIBE_FAILED_EXCEPTION_MESSAGE);
        }
    }

    public Map<String, String> getMetadata(String type, String rid, String centerId, String machineId, String opencrvsBirthId) {
        Map<String, String> map = new HashMap<>();
        String packetCreatedDateTime = rid.substring(rid.length() - 14);
        String formattedDate = packetCreatedDateTime.substring(0, 8) + "T" + packetCreatedDateTime.substring(packetCreatedDateTime.length() - 6);
        String creationTime = LocalDateTime.parse(formattedDate, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")) + ".000Z";
        map.put("metaData",
            "["+
                "{" +
                    "\"label\":\"registrationType\"," +
                    "\"value\":\"" + type + "\"" +
                "}" +
                "," +
                "{" +
                    "\"label\":\"uin\"," +
                    "\"value\":\"" + rid + "\"" +
                "}" +
                "," +
                "{" +
                    "\"label\":\"centerId\"," +
                    "\"value\":\"" + centerId + "\"" +
                "}" +
                "," +
                "{" +
                    "\"label\":\"machineId\"," +
                    "\"value\":\"" + machineId + "\"" +
                "}" +
                "," +
                "{" +
                    "\"label\":\"opencrvsBRN\"," +
                    "\"value\":\"" + opencrvsBirthId + "\"" +
                "}" +
            "]");
        map.put("registrationId", "\"" + rid + "\"");
        map.put("operationsData",
            "[" +
                "{" +
                    "\"label\":\"officerId\"," +
                    "\"value\":null" +
                "}," +
                "{" +
                    "\"label\":\"officerBiometricFileName\"," +
                    "\"value\":null" +
                "}," +
                "{" +
                    "\"label\":\"supervisorId\"," +
                    "\"value\":null" +
                "}," +
                "{" +
                    "\"label\":\"supervisorBiometricFileName\"," +
                    "\"value\":null" +
                "}," +
                "{" +
                    "\"label\":\"supervisorPassword\"," +
                    "\"value\":null" +
                "}," +
                "{" +
                    "\"label\":\"officerPassword\"," +
                    "\"value\":null" +
                "}," +
                "{" +
                    "\"label\":\"supervisorPIN\"," +
                    "\"value\":null" +
                "}," +
                "{" +
                    "\"label\":\"officerPIN\"," +
                    "\"value\":null" +
                "}," +
                "{" +
                    "\"label\":\"supervisorOTPAuthentication\"," +
                    "\"value\":null" +
                "}," +
                "{" +
                    "\"label\":\"officerOTPAuthentication\"," +
                    "\"value\":null" +
                "}" +
            "]");
        map.put("capturedRegisteredDevices","[]");
        map.put("documents","[]");
        map.put("creationDate","\"" + creationTime + "\"");
        return map;
    }

    public String getDefaultSource() {
        String provider = env.getProperty("provider.packetwriter.opencrvs");
        List<String> strList = Arrays.asList(provider.split(","));
        Optional<String> optional = strList.stream().filter(s -> s.contains("source")).findAny();
        String source = optional.isPresent() ? optional.get().replace("source:", "") : null;
        return source;
    }

    public void publishStatusWebsub(WebsubRequest cred, String status){
        //get authtoken
        String credStatusUpdateTopic = "CREDENTIAL_STATUS_UPDATE";
        String token = restTokenUtil.getPartnerAuthToken("Publish status to websub");
        if(token==null || token.isEmpty()) return;

        try{
            String requestId = cred.event.transactionId;
            String topic = partnerClientId + "/CREDENTIAL_ISSUED";
            String req =
                "{" +
                    "\"publishedOn\":\"" + new DateTime() + "\"" + "," +
                    "\"publisher\":\"OPENCRVS_MEDIATOR\"" + "," +
                    "\"topic\":\"" + credStatusUpdateTopic + "\"" + "," +
                    "\"event\":" +
                        "{" +
                            "\"id\":\"" + UUID.randomUUID() + "\"" + "," +
                            "\"requestId\":\"" + requestId + "\"" + "," +
                            "\"status\":\"" + status + "\"" + "," +
                            "\"timestamp\":\"" + Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()) + "\"" + "," +
                            "\"url\":null" +
                        "}" +
                "}";
            String url = UriComponentsBuilder.fromHttpUrl(mosipWebSubHubUrl).queryParam("hub.mode", "publish").queryParam("hub.topic", credStatusUpdateTopic).toUriString();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
            headers.add("Cookie","Authorization="+token);
            HttpEntity<String> request = new HttpEntity<>(req,headers);
            String res = new RestTemplate().postForObject(url,request,String.class);
            LOGGER.info(LoggingConstants.SESSION,LoggingConstants.ID,"publish status to websub","Published status. Response: "+res);
        } catch (Exception e) {
            LOGGER.error(LoggingConstants.FORMATTER_PREFIX, LoggingConstants.SESSION,LoggingConstants.ID,"publish status to websub","Error publishing status ", e);
        }
    }
}
