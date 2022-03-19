package io.mosip.opencrvs.service;


import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;

import io.mosip.kernel.core.exception.ExceptionUtils;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.kernel.core.util.HMACUtils2;

import io.mosip.commons.packet.impl.OnlinePacketCryptoServiceImpl;

import io.mosip.opencrvs.constant.ApiName;
import io.mosip.opencrvs.constant.Constants;
import io.mosip.opencrvs.constant.LoggingConstants;
import io.mosip.opencrvs.error.ErrorCode;
import io.mosip.opencrvs.util.LogUtil;
import io.mosip.opencrvs.util.JdbcUtil;

@Service
public class SyncAndUploadService {

    private static final String PACKET_RECEIVED = "PACKET_RECEIVED";
    private static final String SUCCESS = "SUCCESS";
    private static final String FAILURE = "FAILURE";

    private static final Logger LOGGER = LogUtil.getLogger(SyncAndUploadService.class);

    @Autowired
    private RestTemplate selfTokenRestTemplate;

    @Autowired
    private OnlinePacketCryptoServiceImpl crypto;

    @Autowired
    private JdbcUtil jdbcUtil;

    @Autowired
    private Environment env;

    public String uploadUinPacket(String registrationId, String refId, String creationTime, String regType, byte[] packetZipBytes) throws BaseCheckedException {
        String apiNamePacketReceiver = env.getProperty(ApiName.PACKETRECEIVER);

        String syncStatus = "";

        LOGGER.debug(LoggingConstants.SESSION, LoggingConstants.ID, " ", "SyncAndUploadService::uploadUinPacket()::entry");

        String packetSyncRes;
        try {
            packetSyncRes = packetSync(registrationId, refId, regType, packetZipBytes, creationTime);
            try {
                syncStatus = new JSONObject(packetSyncRes).getJSONArray("response").getJSONObject(0).getString("status");
            } catch (JSONException e) {
                LOGGER.error(LoggingConstants.SESSION, LoggingConstants.ID, registrationId, "Improper result of sync call. Output : " + packetSyncRes);
                throw new BaseCheckedException(ErrorCode.JSON_PROCESSING_EXCEPTION_CODE, ErrorCode.JSON_PROCESSING_EXCEPTION_MESSAGE, e);
            }
        } catch (RuntimeException e) {
            throw new BaseCheckedException(ErrorCode.API_RESOURCE_UNAVAILABLE_CODE, ErrorCode.API_RESOURCE_UNAVAILABLE_1_MESSAGE, e);
        }

        if (!SUCCESS.equalsIgnoreCase(syncStatus)) {
            LOGGER.error(LoggingConstants.SESSION, LoggingConstants.ID, registrationId, "Packet Sync Failed with status: " + syncStatus);
            throw new BaseCheckedException(ErrorCode.SYNC_UPLOAD_EXCEPTION_CODE, ErrorCode.SYNC_UPLOAD_EXCEPTION_MESSAGE);
        }

        LOGGER.info(LoggingConstants.SESSION, LoggingConstants.ID, registrationId, "Packet Generator sync successfull");
        jdbcUtil.updateBirthStatusOfRid(registrationId, "Packet Synced");

        ByteArrayResource contentsAsResource = new ByteArrayResource(packetZipBytes) {
            @Override
            public String getFilename() {
                return registrationId + ".zip";
            }
        };
        LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        map.add("file", contentsAsResource);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<LinkedMultiValueMap<String, Object>>(map, headers);

        try {
            String packetReceiverRes = selfTokenRestTemplate.postForObject(apiNamePacketReceiver, requestEntity, String.class);
            LOGGER.debug(LoggingConstants.SESSION, LoggingConstants.ID, registrationId, "SyncAndUploadService::uploadUinPacket()::Packet receiver service  call ended with response data : " + packetReceiverRes);
            try {
                String uploadStatus = new JSONObject(packetReceiverRes).getJSONObject("response").getString("status");
                LOGGER.info(LoggingConstants.SESSION, LoggingConstants.ID, registrationId, "Packet Upload Completed with status: " + uploadStatus);
                return uploadStatus;
            } catch (JSONException je) {
                LOGGER.error(LoggingConstants.SESSION, LoggingConstants.ID, registrationId, "Packet Upload. Unable to parse upload response: " + packetReceiverRes);
                throw new BaseCheckedException(ErrorCode.JSON_PROCESSING_EXCEPTION_CODE, ErrorCode.JSON_PROCESSING_EXCEPTION_MESSAGE, je);
            }
        } catch (RestClientException e) {
            LOGGER.error(LoggingConstants.SESSION, LoggingConstants.ID, registrationId, "Packet Upload Failed with exception: " + ExceptionUtils.getStackTrace(e));
            throw new BaseCheckedException(ErrorCode.SYNC_UPLOAD_EXCEPTION_CODE, ErrorCode.SYNC_UPLOAD_EXCEPTION_MESSAGE, e);
        }
    }

    private String packetSync(String regId, String refId, String regType, byte[] enryptedUinZipFile, String creationTime) {
        String regSyncServiceId = env.getProperty(Constants.REG_SYNC_SERVICE_ID);
        String regSyncApplicationVersion = env.getProperty(Constants.REG_SYNC_APPLICATION_VERSION);
        String dateTimePattern = env.getProperty(Constants.DATETIME_PATTERN);
        String apiNameSyncService = env.getProperty(ApiName.SYNCSERVICE);

        String registrationSyncRes = null;
        String packetHash;
        // Calculate HashSequense for the enryptedUinZipFile file
        // HMACUtils2.update(enryptedUinZipFile);
        // ArrayList<SyncDto> syncList = new ArrayList<>();
        // SyncDto syncReq = new SyncDto();
        // syncReq.registrationId=regId;
        // syncReq.registrationType=regType;
        // syncReq.packetHashValue=HMACUtils2.digestAsPlainText(enryptedUinZipFile);
        // syncReq.packetSize=BigInteger.valueOf(enryptedUinZipFile.length);
        // syncReq.supervisorStatus=Constants.SUPERVISOR_STATUS_APPROVED;
        // syncReq.supervisorComment=Constants.SUPERVISOR_COMMENT;
        // syncReq.langCode="eng";
        // syncList.add(syncReq);
        //
        // SyncServiceRequestDto registrationSyncRequest= new  SyncServiceRequestDto();
        // registrationSyncRequest.id = env.getProperty(Constants.REG_SYNC_SERVICE_ID);
        // registrationSyncRequest.version = env.getProperty(Constants.REG_SYNC_APPLICATION_VERSION);
        // registrationSyncRequest.requesttime = DateUtils.getUTCCurrentDateTimeString(env.getProperty(Constants.DATETIME_PATTERN));
        // registrationSyncRequest.request = syncList;
        //
        // String syncRequestString;
        // try{
        // 	syncRequestString = JsonUtils.javaObjectToJsonString(registrationSyncRequest);
        // }
        // catch(JsonProcessingException jpe){
        // 	return FAILURE;
        // }
        try {
            packetHash = HMACUtils2.digestAsPlainText(enryptedUinZipFile);
        } catch (NoSuchAlgorithmException nsa) {
            throw new BaseUncheckedException(ErrorCode.UNKNOWN_EXCEPTION_CODE, ErrorCode.UNKNOWN_EXCEPTION_MESSAGE, nsa);
        }

        String syncRequestString =
            "{" +
                "\"id\":\"" + regSyncServiceId + "\"," +
                "\"version\":\"" + regSyncApplicationVersion + "\"," +
                "\"requesttime\":\"" + DateUtils.getUTCCurrentDateTimeString(dateTimePattern) + "\"," +
                "\"request\":[{" +
                    "\"registrationId\":\"" + regId + "\"," +
                    "\"registrationType\":\"" + regType + "\"," +
                    "\"packetHashValue\":\"" + packetHash + "\"," +
                    "\"packetSize\":" + BigInteger.valueOf(enryptedUinZipFile.length) + "," +
                    "\"supervisorStatus\":\"" + Constants.SUPERVISOR_STATUS_APPROVED + "\"," +
                    "\"supervisorComment\":\"" + Constants.SUPERVISOR_COMMENT + "\"," +
                    "\"langCode\":\"" + "eng" + "\"" +
                "}]" +
            "}";

        LOGGER.debug(LoggingConstants.SESSION, LoggingConstants.ID, regId, "SyncAndUploadService::packetSync()::Sync service call started with request data : " + syncRequestString);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Center-Machine-RefId", refId);
        headers.add("timestamp", creationTime);

        String encryptedString = "\"" + CryptoUtil.encodeToURLSafeBase64(crypto.encrypt(refId, syncRequestString.getBytes())) + "\"";
        HttpEntity<Object> requestEntity = new HttpEntity<Object>(encryptedString.getBytes(), headers);
        registrationSyncRes = selfTokenRestTemplate.postForObject(apiNameSyncService, requestEntity, String.class);
        LOGGER.debug(LoggingConstants.SESSION, LoggingConstants.ID, regId, "SyncAndUploadService::packetSync()::Sync service call ended with response data : " + registrationSyncRes);
        return registrationSyncRes;
    }

}
