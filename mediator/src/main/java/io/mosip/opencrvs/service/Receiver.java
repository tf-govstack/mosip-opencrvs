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

import java.util.concurrent.CompletableFuture;

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
import io.mosip.opencrvs.util.RestUtil;
import io.mosip.opencrvs.util.KafkaUtil;
import io.mosip.opencrvs.util.JdbcUtil;
import io.mosip.opencrvs.util.LogUtil;

@Service
public class Receiver {

	private static Logger LOGGER = LogUtil.getLogger(Receiver.class);

	@Value("${IDSchema.Version}")
	private String idschemaVersion;

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
	private RestTemplate selfTokenRestTemplate;

	@Async
	public CompletableFuture<Void> receive() throws BaseCheckedException{
		String pollIntervalMs = env.getProperty("mosip.opencrvs.kafka.consumer.poll.interval.ms");
		while (true) {
			ConsumerRecords<String, String> records = kafkaUtil.consumerPoll(Duration.ofMillis(Long.valueOf(pollIntervalMs)));
			for (ConsumerRecord<String, String> record : records) {
				try {
					LOGGER.info(LoggingConstants.SESSION, LoggingConstants.ID, "txn_id - "+record.key(), "Received transaction");
					createAndUploadPacket(record.value());
				} catch (BaseCheckedException e){
					LOGGER.error(LoggingConstants.SESSION, LoggingConstants.ID, "txn_id - "+record.key(), "Error while processing transaction. Sending back to produce. "+e);
					try{
						producer.produce(record.value());
					}
					catch(Exception ioe){
						LOGGER.error(LoggingConstants.SESSION, LoggingConstants.ID, "txn_id - "+record.key(), "Unable to put back failed transaction to producer. "+ioe);
					}
				} catch (Exception e){
					LOGGER.error(LoggingConstants.SESSION, LoggingConstants.ID, "txn_id - "+record.key(), "Error while processing transaction. "+e);
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

		String txnId = Producer.getIdFromBody(requestBody);

		if(!jdbcUtil.ifTxnExists(txnId)){
			jdbcUtil.createBirthTransaction(txnId);
		}
		else if("Uploaded".equals(jdbcUtil.getBirthStatus(txnId))){
			LOGGER.error(LoggingConstants.SESSION, LoggingConstants.ID, "txn_id - "+txnId, "Packet Already Uploaded.");
			return;
		}

		// make some receivedto out of requestbody
		ReceiveDto request ;
		try{
			String ridObtained = jdbcUtil.getBirthRid(txnId);
			request = ReceiveDto.build(requestBody, (ridObtained==null || ridObtained.isEmpty()) ? generateRegistrationId(centerId,machineId) : ridObtained, centerId, machineId);
		}
		catch(Exception e){
			LOGGER.error(LoggingConstants.SESSION,LoggingConstants.ID,"generate RID", ErrorCode.RID_GENERATE_EXCEPTION_MESSAGE + e);
			throw new BaseUncheckedException(ErrorCode.RID_GENERATE_EXCEPTION_CODE,ErrorCode.RID_GENERATE_EXCEPTION_MESSAGE, e);
		}

		LOGGER.info(LoggingConstants.SESSION,LoggingConstants.ID,request.getIdValue(),"Started Packet Creation");

		File file = null;

		jdbcUtil.updateBirthRidAndStatus(txnId,request.getIdValue(),"RID Generated");

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
			if (request.getProofOfAddress() != null && !request.getProofOfAddress().isEmpty())
				setDemographicDocuments(request.getProofOfAddress(), demoJsonObject, Constants.PROOF_OF_ADDRESS, docsMap);
			if (request.getProofOfDateOfBirth() != null && !request.getProofOfDateOfBirth().isEmpty())
				setDemographicDocuments(request.getProofOfDateOfBirth(), demoJsonObject, Constants.PROOF_OF_DOB, docsMap);
			if (request.getProofOfRelationship() != null && !request.getProofOfRelationship().isEmpty())
				setDemographicDocuments(request.getProofOfRelationship(), demoJsonObject, Constants.PROOF_OF_RELATIONSHIP, docsMap);
			if (request.getProofOfIdentity() != null && !request.getProofOfIdentity().isEmpty())
				setDemographicDocuments(request.getProofOfIdentity(), demoJsonObject, Constants.PROOF_OF_IDENTITY, docsMap);

			LOGGER.info(LoggingConstants.SESSION,LoggingConstants.ID,request.getIdValue(),"Passing the packet for creation, to PacketManager Library");

			jdbcUtil.updateBirthStatus(txnId,"Creating Packet");

			PacketDto packetDto = new PacketDto();
			// packetDto.setId(generateRegistrationId(request.centerId, request.machineId));
			packetDto.setId(request.getIdValue());
			packetDto.setSource(restUtil.getDefaultSource());
			packetDto.setProcess(restUtil.getDefaultProcess());
			packetDto.setSchemaVersion(idschemaVersion);
			packetDto.setSchemaJson(restUtil.getIdSchema(Double.valueOf(idschemaVersion),idschemaCache));
			LOGGER.debug(LoggingConstants.SESSION,LoggingConstants.ID,request.getIdValue(),"Received This schemaJson from API: " + packetDto.getSchemaJson());
			packetDto.setFields(idMap);
			packetDto.setDocuments(docsMap);
      packetDto.setMetaInfo(restUtil.getMetadata(Constants.CREATION_TYPE, request.getIdValue(),request.getCenterId(),request.getMachineId(), request.getOpencrvsId()));
			packetDto.setAudits(restUtil.generateAudit(packetDto.getId(),auditAppName,auditAppId));
			packetDto.setOfflineMode(false);
			packetDto.setRefId(request.getCenterId() + "_" + request.getMachineId());
			List<PacketInfo> packetInfos = packetWriter.createPacket(packetDto);

			if (CollectionUtils.isEmpty(packetInfos) || packetInfos.iterator().next().getId() == null)
				throw new PacketCreatorException(ErrorCode.PACKET_CREATION_EXCEPTION_CODE, ErrorCode.PACKET_CREATION_EXCEPTION_MESSAGE);

			jdbcUtil.updateBirthStatus(txnId,"Packet Created");

			LOGGER.info(LoggingConstants.SESSION,LoggingConstants.ID,request.getIdValue(),"Packet Created Successfully.");
			LOGGER.debug(LoggingConstants.SESSION,LoggingConstants.ID,request.getIdValue(),"Packet Created Successfully. Info: "+packetInfos);

			file = new File(objectStoreBaseLocation
					+ File.separator + packetManagerAccountName
					+ File.separator + packetInfos.iterator().next().getId() + ".zip");

			FileInputStream fis = new FileInputStream(file);

			byte[] packetZipBytes = IOUtils.toByteArray(fis);

			String packetCreatedDateTime = packetDto.getId().substring(packetDto.getId().length() - 14);
			String formattedDate = packetCreatedDateTime.substring(0, 8) + "T" + packetCreatedDateTime.substring(packetCreatedDateTime.length() - 6);
			String creationTime = LocalDateTime.parse(formattedDate, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")).toString() + ".000Z";

			LOGGER.debug(LoggingConstants.SESSION, LoggingConstants.ID, packetDto.getId(), "Receiver::createPacket()::packet created and sent for sync service");

			String packetGeneratorRes = syncUploadEncryptionService.uploadUinPacket(packetDto.getId(), packetDto.getRefId(), creationTime, Constants.PACKET_CREATION_TYPE, packetZipBytes);

			jdbcUtil.updateBirthStatus(txnId,"Uploaded");

			LOGGER.info(LoggingConstants.SESSION, LoggingConstants.ID, packetDto.getId(), "Receiver::createPacket()::packet synched and uploaded");
		} catch (Exception e) {
			LOGGER.error(LoggingConstants.SESSION,LoggingConstants.ID, request.getIdValue(),"Encoutnered error while create packet"+e);
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
