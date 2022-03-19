package io.mosip.opencrvs.service;

import java.io.File;
import java.io.FileInputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.opencrvs.util.*;
import org.json.JSONObject;
import org.json.JSONException;

import org.apache.commons.io.IOUtils;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.scheduling.annotation.Async;

import io.mosip.commons.packet.dto.Document;
import io.mosip.commons.packet.dto.PacketInfo;
import io.mosip.commons.packet.dto.packet.PacketDto;
import io.mosip.commons.packet.exception.PacketCreatorException;
import io.mosip.commons.packet.facade.PacketWriter;
import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.FileUtils;

import io.mosip.opencrvs.constant.ApiName;
import io.mosip.opencrvs.constant.Constants;
import io.mosip.opencrvs.constant.LoggingConstants;
import io.mosip.opencrvs.dto.ReceiveDto;
import io.mosip.opencrvs.error.ErrorCode;

@Service
public class Receiver {

	private static Logger LOGGER = LogUtil.getLogger(Receiver.class);

	@Value("${IDSchema.Version}")
	private String idschemaVersion;

	@Value("${opencrvs.birth.process.type}")
	private String birthPacketProcessType;

	@Value("${opencrvs.reproduce.on.error}")
	private String reproducerOnError;

	@Value("${opencrvs.reproduce.on.error.delay.ms}")
	private String reproducerOnErrorDelayMs;

	private Map<Double, String> idschemaCache = new HashMap<>();

	@Autowired
	private SyncAndUploadService syncUploadEncryptionService;

	@Autowired
	private PacketWriter packetWriter;

	@Autowired
	private Environment env;

	@Autowired
	private Producer producer;

	@Autowired
	private RestUtil restUtil;

	@Autowired
	private KafkaUtil kafkaUtil;

	@Autowired
	private JdbcUtil jdbcUtil;

	@Autowired
	private OpencrvsDataUtil opencrvsDataUtil;

	@Autowired
	private RestTemplate selfTokenRestTemplate;

	@Async
	public void receive(){
		String pollIntervalMs = env.getProperty("mosip.opencrvs.kafka.consumer.poll.interval.ms");
		while (true) {
			ConsumerRecords<String, String> records = kafkaUtil.consumerPoll(Duration.ofMillis(Long.valueOf(pollIntervalMs)));
			for (ConsumerRecord<String, String> record : records) {
				try {
					LOGGER.info(LoggingConstants.SESSION, LoggingConstants.ID, "txn_id - "+record.key(), "Received transaction");
					createAndUploadPacket(record.value());
				} catch (BaseCheckedException e){
					LOGGER.error(LoggingConstants.SESSION, LoggingConstants.ID, "txn_id - "+record.key(), "Error while processing transaction. Sending back to produce. " + ExceptionUtils.getStackTrace(e));
					try{
						if("true".equalsIgnoreCase(reproducerOnError)){
							// TODO: improve this so that it doesnt halt execution
							try{
								Thread.sleep(Long.valueOf(reproducerOnErrorDelayMs));
							} catch(Exception ignored) {}
							producer.produce(record.value());
						}
					}
					catch(Exception ioe){
						LOGGER.error(LoggingConstants.SESSION, LoggingConstants.ID, "txn_id - "+record.key(), "Unable to put back failed transaction to producer. " + ExceptionUtils.getStackTrace(ioe));
					}
				} catch (Exception e){
					LOGGER.error(LoggingConstants.SESSION, LoggingConstants.ID, "txn_id - "+record.key(), "Error while processing transaction. " + ExceptionUtils.getStackTrace(e));
				}
			}
		}
	}

	public String generateRegistrationId(String centerId, String machineId){
		String apiNameRidGeneration = env.getProperty(ApiName.RIDGENERATION);
		String response = selfTokenRestTemplate.getForObject(apiNameRidGeneration+"/"+centerId+"/"+machineId,String.class);
		try{
			JSONObject responseJson = new JSONObject(response);
			return responseJson.getJSONObject("response").getString("rid");
		}
		catch(JSONException je){
			LOGGER.error(LoggingConstants.SESSION,LoggingConstants.ID,"generate RID", ErrorCode.JSON_PROCESSING_EXCEPTION_MESSAGE);
			throw new BaseUncheckedException(ErrorCode.JSON_PROCESSING_EXCEPTION_CODE, ErrorCode.JSON_PROCESSING_EXCEPTION_MESSAGE);
		}
	}

	public void createAndUploadPacket(String requestBody) throws BaseCheckedException {
		String auditAppName = env.getProperty(Constants.AUDIT_APP_NAME);
		String auditAppId = env.getProperty(Constants.AUDIT_APP_ID);
		String objectStoreBaseLocation = env.getProperty("object.store.base.location");
		String packetManagerAccountName = env.getProperty("packet.manager.account.name");

		String centerId = env.getProperty("opencrvs.center.id");
		String machineId = env.getProperty("opencrvs.machine.id");

		String txnId = opencrvsDataUtil.getTxnIdFromBody(requestBody);

		if(!jdbcUtil.ifTxnExists(txnId)){
			jdbcUtil.createBirthTransaction(txnId);
		}
		else if("Uploaded".equals(jdbcUtil.getBirthStatus(txnId))){
			LOGGER.info(LoggingConstants.SESSION, LoggingConstants.ID, "txn_id - "+txnId, "Packet Already Uploaded.");
			return;
		}

		// make some receivedto out of requestbody
		ReceiveDto request ;
		try {
			request = opencrvsDataUtil.buildIdJson(requestBody);
		}
		catch(Exception e){
			LOGGER.error(LoggingConstants.SESSION,LoggingConstants.ID,"build IdJson Request", ErrorCode.IDJSON_BUILD_EXCEPTION_MESSAGE + ExceptionUtils.getStackTrace(e));
			throw new BaseUncheckedException(ErrorCode.IDJSON_BUILD_EXCEPTION_CODE,ErrorCode.IDJSON_BUILD_EXCEPTION_MESSAGE, e);
		}

		try{
			String ridObtained = jdbcUtil.getBirthRid(txnId);
			if (ridObtained==null || ridObtained.isEmpty()){
				request.setRid(generateRegistrationId(centerId,machineId));
				jdbcUtil.updateBirthRidAndStatus(txnId,request.getRid(),"RID Generated");
			} else {
				request.setRid(generateRegistrationId(centerId,machineId));
			}
		}
		catch(Exception e){
			LOGGER.error(LoggingConstants.SESSION,LoggingConstants.ID,"generate RID", ErrorCode.RID_GENERATE_EXCEPTION_MESSAGE + ExceptionUtils.getStackTrace(e));
			throw new BaseUncheckedException(ErrorCode.RID_GENERATE_EXCEPTION_CODE,ErrorCode.RID_GENERATE_EXCEPTION_MESSAGE, e);
		}

		// BaseUncheckedException only till this point

		LOGGER.info(LoggingConstants.SESSION,LoggingConstants.ID,request.getRid(),"Started Packet Creation");

		File file = null;

		try {
			Map<String, String> idMap = new HashMap<>();
			JSONObject demoJsonObject = new JSONObject(request.getIdentityJson());
			JSONObject fieldsJson = demoJsonObject.getJSONObject(Constants.IDENTITY);
			Iterator<?> fields = fieldsJson.keys();

			while(fields.hasNext()){
				String key = (String)fields.next();
				Object value = fieldsJson.get(key);
				idMap.put(key, value == null ? null : value.toString());
			}

			// set demographic documents
			Map<String, Document> docsMap = new HashMap<>();
			//if (request.getProofOfAddress() != null && !request.getProofOfAddress().isEmpty())
			//	setDemographicDocuments(request.getProofOfAddress(), demoJsonObject, Constants.PROOF_OF_ADDRESS, docsMap);
			//if (request.getProofOfDateOfBirth() != null && !request.getProofOfDateOfBirth().isEmpty())
			//	setDemographicDocuments(request.getProofOfDateOfBirth(), demoJsonObject, Constants.PROOF_OF_DOB, docsMap);
			//if (request.getProofOfRelationship() != null && !request.getProofOfRelationship().isEmpty())
			//	setDemographicDocuments(request.getProofOfRelationship(), demoJsonObject, Constants.PROOF_OF_RELATIONSHIP, docsMap);
			if (request.getProofOfIdentity() != null && !request.getProofOfIdentity().isEmpty())
				setDemographicDocuments(request.getProofOfIdentity(), demoJsonObject, Constants.PROOF_OF_IDENTITY, docsMap);

			LOGGER.info(LoggingConstants.SESSION,LoggingConstants.ID,request.getRid(),"Passing the packet for creation, to PacketManager Library");

			jdbcUtil.updateBirthStatus(txnId,"Creating Packet");

			PacketDto packetDto = new PacketDto();
			// packetDto.setId(generateRegistrationId(request.centerId, request.machineId));
			packetDto.setId(request.getRid());
			packetDto.setSource(restUtil.getDefaultSource());
			packetDto.setProcess(birthPacketProcessType);
			packetDto.setSchemaVersion(idschemaVersion);
			packetDto.setSchemaJson(restUtil.getIdSchema(Double.valueOf(idschemaVersion),idschemaCache));
			LOGGER.debug(LoggingConstants.SESSION,LoggingConstants.ID,request.getRid(),"Received This schemaJson from API: " + packetDto.getSchemaJson());
			packetDto.setFields(idMap);
			packetDto.setDocuments(docsMap);
			packetDto.setMetaInfo(restUtil.getMetadata(birthPacketProcessType, request.getRid(),centerId,machineId, request.getOpencrvsId()));
			packetDto.setAudits(restUtil.generateAudit(packetDto.getId(),auditAppName,auditAppId));
			packetDto.setOfflineMode(false);
			packetDto.setRefId(centerId + "_" + machineId);
			List<PacketInfo> packetInfos = packetWriter.createPacket(packetDto);

			if (CollectionUtils.isEmpty(packetInfos) || packetInfos.iterator().next().getId() == null)
				throw new PacketCreatorException(ErrorCode.PACKET_CREATION_EXCEPTION_CODE, ErrorCode.PACKET_CREATION_EXCEPTION_MESSAGE);

			jdbcUtil.updateBirthStatus(txnId,"Packet Created");

			LOGGER.info(LoggingConstants.SESSION,LoggingConstants.ID,request.getRid(),"Packet Created Successfully.");
			LOGGER.debug(LoggingConstants.SESSION,LoggingConstants.ID,request.getRid(),"Packet Created Successfully. Info: "+packetInfos);

			file = new File(objectStoreBaseLocation
					+ File.separator + packetManagerAccountName
					+ File.separator + packetInfos.iterator().next().getId() + ".zip");

			FileInputStream fis = new FileInputStream(file);

			byte[] packetZipBytes = IOUtils.toByteArray(fis);

			String packetCreatedDateTime = packetDto.getId().substring(packetDto.getId().length() - 14);
			String formattedDate = packetCreatedDateTime.substring(0, 8) + "T" + packetCreatedDateTime.substring(packetCreatedDateTime.length() - 6);
			String creationTime = LocalDateTime.parse(formattedDate, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")) + ".000Z";

			LOGGER.debug(LoggingConstants.SESSION, LoggingConstants.ID, packetDto.getId(), "Receiver::createPacket()::packet created and sent for sync service");

			String packetGeneratorRes = syncUploadEncryptionService.uploadUinPacket(packetDto.getId(), packetDto.getRefId(), creationTime, birthPacketProcessType, packetZipBytes);

			jdbcUtil.updateBirthStatus(txnId,"Uploaded");

			LOGGER.info(LoggingConstants.SESSION, LoggingConstants.ID, packetDto.getId(), "Receiver::createPacket()::packet synced and uploaded");
		} catch (Exception e) {
			LOGGER.error(LoggingConstants.SESSION,LoggingConstants.ID, request.getRid(),"Encountered error while create packet" + ExceptionUtils.getStackTrace(e));
			if (e instanceof BaseCheckedException) {
				throw (BaseCheckedException) e;
			} else if (e instanceof BaseUncheckedException) {
				throw (BaseUncheckedException) e;
			} else {
				throw new BaseUncheckedException(ErrorCode.UNKNOWN_EXCEPTION_CODE, ErrorCode.UNKNOWN_EXCEPTION_MESSAGE, e);
			}
		} finally {
			if (file != null && file.exists())
				FileUtils.forceDelete(file);
		}
	}
	private void setDemographicDocuments(String documentBytes, JSONObject demoJsonObject, String documentName, Map<String, Document> map){
		try{
			JSONObject identityJson = demoJsonObject.getJSONObject(Constants.IDENTITY);
			JSONObject documentJson = identityJson.getJSONObject(documentName);

			Document docDetailsDto = new Document();
			docDetailsDto.setDocument(CryptoUtil.decodeURLSafeBase64(documentBytes));
			docDetailsDto.setFormat((String) documentJson.get("format"));
			docDetailsDto.setValue((String) documentJson.get("value"));
			docDetailsDto.setType((String) documentJson.get("type"));
			map.put(documentName, docDetailsDto);
		}
		catch(JSONException je){
			throw new BaseUncheckedException(ErrorCode.JSON_PROCESSING_EXCEPTION_CODE,ErrorCode.JSON_PROCESSING_EXCEPTION_MESSAGE,je);
		}
	}
}
