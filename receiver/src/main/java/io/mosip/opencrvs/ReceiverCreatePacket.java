package io.mosip.opencrvs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;

import java.util.concurrent.CompletableFuture;

import org.json.JSONObject;
import org.json.JSONException;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.scheduling.annotation.Async;

// import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.commons.packet.dto.Document;
import io.mosip.commons.packet.dto.PacketInfo;
import io.mosip.commons.packet.dto.packet.PacketDto;
import io.mosip.commons.packet.exception.PacketCreatorException;
import io.mosip.commons.packet.facade.PacketWriter;
import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.FileUtils;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
// import io.mosip.resident.config.LoggerConfiguration;
// import io.mosip.resident.constant.ApiName;
// import io.mosip.resident.constant.LoggerFileConstant;
// import io.mosip.resident.constant.PacketMetaInfoConstants;
// import io.mosip.resident.constant.ResidentErrorCode;
// import io.mosip.resident.dto.FieldValue;
// import io.mosip.resident.dto.PackerGeneratorFailureDto;
// import io.mosip.resident.dto.PacketGeneratorResDto;
// import io.mosip.resident.dto.RegistrationType;
// import io.mosip.resident.dto.ResidentIndividialIDType;
// import io.mosip.resident.dto.ResidentUpdateDto;
// import io.mosip.resident.dto.ResponseWrapper;
// import io.mosip.resident.exception.ApisResourceAccessException;
// import io.mosip.resident.util.AuditUtil;
// import io.mosip.resident.util.EventEnum;
// import io.mosip.resident.util.IdSchemaUtil;
// import io.mosip.resident.util.JsonUtil;
// import io.mosip.resident.util.ResidentServiceRestClient;
// import io.mosip.resident.util.TokenGenerator;
// import io.mosip.resident.util.Utilities;
// import io.mosip.resident.validator.RequestHandlerRequestValidator;

@Service
class ReceiverCreatePacket {

	private static Logger LOGGER = Utilities.getLogger(ReceiverCreatePacket.class);

	@Value("${IDSchema.Version}")
	private String idschemaVersion;

  Map<Double, String> idschemaCache = new HashMap<>();

	@Autowired
	SyncAndUploadService syncUploadEncryptionService;

	@Autowired
	private PacketWriter packetWriter;

	@Autowired
	private Environment env;

	@Autowired
	private RestTemplate selfTokenRestTemplate;

	// @Autowired
	// AuditUtil audit;

	String generateRegistrationId(String centerId, String machineId){
		String response = selfTokenRestTemplate.getForObject(env.getProperty(Constants.APINAME_RIDGENERATION)+"/"+centerId+"/"+machineId,String.class);
		try{
			JSONObject responseJson = new JSONObject(response);
			return responseJson.getJSONObject("response").getString("rid");
		}
		catch(JSONException je){
			LOGGER.error(Constants.SESSION,Constants.ID,"generate RID", Constants.JSON_PROCESSING_EXCEPTION_MESSAGE);
			throw new BaseUncheckedException(Constants.JSON_PROCESSING_EXCEPTION_CODE, Constants.JSON_PROCESSING_EXCEPTION_MESSAGE);
		}
	}

	@Async
  public CompletableFuture<String> createPacket(String requestBody) throws BaseCheckedException, IOException {
		// make some receivedto out of requestbody
		String centerId = env.getProperty("opencrvs.center.id");
		String machineId = env.getProperty("opencrvs.machine.id");
		ReceiveDto request ;
		try{
			request = ReceiveDto.build(requestBody, generateRegistrationId(centerId,machineId), centerId, machineId);
		}
		catch(Exception e){
			LOGGER.error(Constants.SESSION,Constants.ID,"generate RID", Constants.RID_GENERATE_EXCEPTION_MESSAGE + e);
			throw new BaseCheckedException(Constants.RID_GENERATE_EXCEPTION_CODE,Constants.RID_GENERATE_EXCEPTION_MESSAGE, e);
		}

		File file = null;

		LOGGER.info(Constants.SESSION,Constants.ID,request.idValue,"Started Packet Creation");

		try {
			Map<String, String> idMap = new HashMap<>();
			JSONObject demoJsonObject = new JSONObject(request.identityJson);
			JSONObject fieldsJson = demoJsonObject.getJSONObject(Constants.IDENTITY);
			Iterator<?> fields = fieldsJson.keys();

			while(fields.hasNext()){
				String key = (String)fields.next();
				Object value = fieldsJson.get(key);
				idMap.put(key, value == null ? null : value.toString());
			}

			// set demographic documents
			Map<String, Document> docsMap = new HashMap<>();
			if (request.proofOfAddress != null && !request.proofOfAddress.isEmpty())
				setDemographicDocuments(request.proofOfAddress, demoJsonObject, Constants.PROOF_OF_ADDRESS, docsMap);
			if (request.proofOfDateOfBirth != null && !request.proofOfDateOfBirth.isEmpty())
				setDemographicDocuments(request.proofOfDateOfBirth, demoJsonObject, Constants.PROOF_OF_DOB, docsMap);
			if (request.proofOfRelationship != null && !request.proofOfRelationship.isEmpty())
				setDemographicDocuments(request.proofOfRelationship, demoJsonObject, Constants.PROOF_OF_RELATIONSHIP, docsMap);
			if (request.proofOfIdentity != null && !request.proofOfIdentity.isEmpty())
				setDemographicDocuments(request.proofOfIdentity, demoJsonObject, Constants.PROOF_OF_IDENTITY, docsMap);

			LOGGER.info(Constants.SESSION,Constants.ID,request.idValue,"Passing the packet for creation, to PacketManager Library");

			PacketDto packetDto = new PacketDto();
			// packetDto.setId(generateRegistrationId(request.centerId, request.machineId));
			packetDto.setId(request.idValue);
			packetDto.setSource(Constants.SOURCE);
			packetDto.setProcess(Constants.PROCESS_TYPE);
			packetDto.setSchemaVersion(idschemaVersion);
			packetDto.setSchemaJson(Utilities.getIdSchema(Double.valueOf(idschemaVersion),idschemaCache,env.getProperty(Constants.APINAME_MIDSCHEMAURL),selfTokenRestTemplate));
			LOGGER.debug(Constants.SESSION,Constants.ID,request.idValue,"Received This schemaJson from API: " + packetDto.getSchemaJson());
			packetDto.setFields(idMap);
			packetDto.setDocuments(docsMap);
      packetDto.setMetaInfo(Utilities.getMetadata(request.idValue, Constants.CREATION_TYPE, request.opencrvsId));
			packetDto.setAudits(Utilities.generateAudit(packetDto.getId(),env.getProperty(Constants.AUDIT_APP_NAME),env.getProperty(Constants.AUDIT_APP_ID)));
			packetDto.setOfflineMode(false);
			packetDto.setRefId(request.centerId + "_" + request.machineId);
			List<PacketInfo> packetInfos = packetWriter.createPacket(packetDto);

			LOGGER.info(Constants.SESSION,Constants.ID,request.idValue,"Packet Created Successfully.");
			LOGGER.debug(Constants.SESSION,Constants.ID,request.idValue,"Packet Created Successfully. Info: "+packetInfos);

			if (CollectionUtils.isEmpty(packetInfos) || packetInfos.iterator().next().getId() == null)
				throw new PacketCreatorException(Constants.PACKET_CREATION_EXCEPTION_CODE, Constants.PACKET_CREATION_EXCEPTION_MESSAGE);

			file = new File(env.getProperty("object.store.base.location")
					+ File.separator + env.getProperty("packet.manager.account.name")
					+ File.separator + packetInfos.iterator().next().getId() + ".zip");

			FileInputStream fis = new FileInputStream(file);

			byte[] packetZipBytes = IOUtils.toByteArray(fis);

			String packetCreatedDateTime = packetDto.getId().substring(packetDto.getId().length() - 14);
			String formattedDate = packetCreatedDateTime.substring(0, 8) + "T" + packetCreatedDateTime.substring(packetCreatedDateTime.length() - 6);
			String creationTime = LocalDateTime.parse(formattedDate, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")).toString() + ".000Z";

			LOGGER.debug(Constants.SESSION, Constants.ID, packetDto.getId(), "ReceiverCreatePacket::createPacket()::packet created and sent for sync service");

			String packerGeneratorRes = syncUploadEncryptionService.uploadUinPacket(packetDto.getId(), packetDto.getRefId(), creationTime, Constants.PACKET_CREATION_TYPE, packetZipBytes);

			LOGGER.info(Constants.SESSION, Constants.ID, packetDto.getId(), "ReceiverCreatePacket::createPacket()::packet synched and uploaded");
		} catch (Exception e) {
			LOGGER.error(Constants.SESSION,Constants.ID,request.idValue,"Encoutnered error while create packet"+e);
			if (e instanceof BaseCheckedException) {
				throw (BaseCheckedException) e;
			}
			throw new BaseCheckedException(Constants.UNKNOWN_EXCEPTION_CODE, Constants.UNKNOWN_EXCEPTION_MESSAGE, e);
		}
		return CompletableFuture.completedFuture("done");
	}
  private void setDemographicDocuments(String documentBytes, JSONObject demoJsonObject, String documentName, Map<String, Document> map) {
		try{
      JSONObject identityJson = demoJsonObject.getJSONObject(Constants.IDENTITY);
		  JSONObject documentJson = identityJson.getJSONObject(documentName);

      Document docDetailsDto = new Document();
  		docDetailsDto.setDocument(CryptoUtil.decodeURLSafeBase64(documentBytes));
  		docDetailsDto.setFormat((String) documentJson.get(Constants.FORMAT));
  		docDetailsDto.setValue((String) documentJson.get(Constants.VALUE));
  		docDetailsDto.setType((String) documentJson.get(Constants.TYPE));
  		map.put(documentName, docDetailsDto);
    }
		catch(Exception e){}
	}
}
