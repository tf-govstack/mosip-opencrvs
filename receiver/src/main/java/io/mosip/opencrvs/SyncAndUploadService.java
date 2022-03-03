package io.mosip.opencrvs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

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

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.kernel.core.util.HMACUtils2;

import io.mosip.commons.packet.impl.OnlinePacketCryptoServiceImpl;

@Service
public class SyncAndUploadService {

	private static final String PACKET_RECEIVED = "PACKET_RECEIVED";
	private static final String SUCCESS = "SUCCESS";
	private static final String FAILURE = "FAILURE";

  private static final Logger LOGGER = Utilities.getLogger(SyncAndUploadService.class);

	@Autowired
	private RestTemplate selfTokenRestTemplate;

	@Autowired
	OnlinePacketCryptoServiceImpl crypto;

	@Autowired
	private Environment env;

	public String uploadUinPacket(String registartionId, String refId, String creationTime, String regType, byte[] packetZipBytes) throws BaseCheckedException {
		String syncStatus = "";

		try {
			LOGGER.debug(Constants.SESSION, Constants.ID, " ", "SyncAndUploadService::uploadUinPacket()::entry");

			ByteArrayResource contentsAsResource = new ByteArrayResource(packetZipBytes) {
				@Override
				public String getFilename() {
					return registartionId + ".zip";
				}
			};
			String packetSyncRes = packetSync(registartionId, refId, regType, packetZipBytes, creationTime);

			if (packetSyncRes != null || !packetSyncRes.isEmpty()) {
				try{
          JSONArray synList = new JSONObject(packetSyncRes).getJSONArray("response");
          if (synList != null) {
  					JSONObject syncResponseDto = synList.getJSONObject(0);
  					syncStatus = syncResponseDto.getString("status");
  				}
        } catch(JSONException je) {}
			}
			if (SUCCESS.equalsIgnoreCase(syncStatus)) {
				LOGGER.info(Constants.SESSION, Constants.ID, registartionId, "Packet Generator sync successfull");

				LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
				map.add("file", contentsAsResource);
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.MULTIPART_FORM_DATA);
				HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<LinkedMultiValueMap<String, Object>>(map, headers);

				String packetReceiverRes = selfTokenRestTemplate.postForObject(env.getProperty(Constants.APINAME_PACKETRECEIVER), requestEntity, String.class);
        JSONObject packetReceiverResJson = null;
				if (packetReceiverRes != null) {
					try{
            packetReceiverResJson = new JSONObject(packetReceiverRes);
            LOGGER.debug(Constants.SESSION, Constants.ID, registartionId, "SyncUploadEncryptionServiceImpl::uploadUinPacket()::Packet receiver service  call ended with response data : " + packetReceiverRes);
            String uploadStatus = packetReceiverResJson.getJSONObject("response").getString("status");
            LOGGER.info(Constants.SESSION, Constants.ID, registartionId, "Packet Upload Completed with status: " + uploadStatus);
  					return uploadStatus;
          }
          catch(JSONException je){
            LOGGER.error(Constants.SESSION, Constants.ID, registartionId, "Packet Upload Failed with response: " + packetReceiverRes);
            return FAILURE;
          }
				}

			} else {
        LOGGER.error(Constants.SESSION, Constants.ID, registartionId, "Packet Sync Failed with status: " + syncStatus);
        return FAILURE;
			}

		} catch (Exception e) {
			LOGGER.error(Constants.SESSION, Constants.ID, registartionId, "Packet Sync&Upload Failed with exception: " + e);
			throw new BaseCheckedException(Constants.SYNC_UPLOAD_EXCEPTION_CODE, Constants.SYNC_UPLOAD_EXCEPTION_MESSAGE, e);
		}

		return FAILURE;
	}

	@SuppressWarnings("unchecked")
	private String packetSync(String regId, String refId, String regType, byte[] enryptedUinZipFile, String creationTime) throws BaseCheckedException {
    String registrationSyncRes = null;
		try {
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
        // "{" +
        // "\"id\":\"" + env.getProperty(Constants.REG_SYNC_SERVICE_ID) + "\"," +
        // "\"version\":\"" + env.getProperty(Constants.REG_SYNC_APPLICATION_VERSION) + "\"," +
        // "\"requesttime\":\"" + DateUtils.getUTCCurrentDateTimeString(env.getProperty(Constants.DATETIME_PATTERN)) + "\"," +
        // "\"request\":[{" +
        //   "\"registrationId\":\"" + regId + "\"," +
        //   "\"packetId\":\"\"," +
        //   "\"additionalInfoReqId\":\"\"," +
        //   "\"name\":\"\"," +
        //   "\"email\":\"\"," +
        //   "\"phone\":\"\"," +
        //   "\"registrationType\":\"" + regType + "\"," +
        //   "\"packetHashValue\":\"" + HMACUtils2.digestAsPlainText(enryptedUinZipFile) + "\"," +
        //   "\"packetSize\":" + BigInteger.valueOf(enryptedUinZipFile.length) + "," +
        //   "\"supervisorStatus\":\"" + Constants.SUPERVISOR_STATUS_APPROVED + "\"," +
        //   "\"supervisorComment\":\"" + Constants.SUPERVISOR_COMMENT + "\"," +
        //   "\"optionalValues\":[]," +
        //   "\"langCode\":\"" + "eng" + "\"," +
        //   "\"createDateTime\":\"\"," +
        //   "\"updateDateTime\":\"\"," +
        //   "\"deletedDateTime\":\"\"," +
        //   "\"isActive\":true," +
        //   "\"isDeleted\":false" +
        // "}]" +
        // "}"
			String syncRequestString =
				"{" +
        "\"id\":\"" + env.getProperty(Constants.REG_SYNC_SERVICE_ID) + "\"," +
        "\"version\":\"" + env.getProperty(Constants.REG_SYNC_APPLICATION_VERSION) + "\"," +
        "\"requesttime\":\"" + DateUtils.getUTCCurrentDateTimeString(env.getProperty(Constants.DATETIME_PATTERN)) + "\"," +
        "\"request\":[{" +
          "\"registrationId\":\"" + regId + "\"," +
          "\"registrationType\":\"" + regType + "\"," +
          "\"packetHashValue\":\"" + HMACUtils2.digestAsPlainText(enryptedUinZipFile) + "\"," +
          "\"packetSize\":" + BigInteger.valueOf(enryptedUinZipFile.length) + "," +
          "\"supervisorStatus\":\"" + Constants.SUPERVISOR_STATUS_APPROVED + "\"," +
          "\"supervisorComment\":\"" + Constants.SUPERVISOR_COMMENT + "\"," +
          "\"langCode\":\"" + "eng" + "\"" +
        "}]" +
        "}\n"
      ;

			// String syncRequestString;
			// try{
			// 	syncRequestString = JsonUtils.javaObjectToJsonString(registrationSyncRequest);
			// }
			// catch(JsonProcessingException jpe){
			// 	System.out.println("Hello Dear Friend ERROR : "+jpe);
			// 	return FAILURE;
			// }
      LOGGER.debug(Constants.SESSION, Constants.ID, regId, "SyncAndUploadService::packetSync()::Sync service call started with request data : " + syncRequestString);
      System.out.println("Hello dear friend 5 " + syncRequestString);

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
			headers.add("Center-Machine-RefId", refId);
			headers.add("timestamp", creationTime);

			HttpEntity<Object> requestEntity = new HttpEntity<Object>(CryptoUtil.encodeToURLSafeBase64(crypto.encrypt(refId,syncRequestString.getBytes())).getBytes(), headers);

			System.out.println("Hello dear friend 6.prev timestamp: " + creationTime + " encrypted encoded: " +  new String((byte[])requestEntity.getBody()));
			System.out.println("Hello Dear Friend 6 " + JsonUtils.jsonStringToJavaObject(SyncServiceRequestDto.class,new String(crypto.decrypt(refId,CryptoUtil.decodeURLSafeBase64(new String((byte[])requestEntity.getBody()))))));

			registrationSyncRes = selfTokenRestTemplate.postForObject(env.getProperty(Constants.APINAME_SYNCSERVICE), requestEntity, String.class);

      System.out.println("Hello Dear Friend 7 " + registrationSyncRes);

      LOGGER.debug(Constants.SESSION, Constants.ID, regId, "SyncAndUploadService::packetSync()::Sync service call ended with response data : " + registrationSyncRes);

		} catch (Exception e) {
			throw new BaseCheckedException(Constants.API_RESOURCE_UNAVAILABLE_CODE, Constants.API_RESOURCE_UNAVAILABLE_1_MESSAGE, e);
		}
		return registrationSyncRes;
	}

}
