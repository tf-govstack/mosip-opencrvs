package io.mosip.opencrvs;

public class Constants{
  static String SESSION = "SESSION";
  static String ID = "ID";

  static String AUDIT_APP_NAME = "opencrvs.audit.app.name";
  static String AUDIT_APP_ID = "opencrvs.audit.app.id";

  static String PROOF_OF_ADDRESS = "proofOfAddress";
  static String PROOF_OF_DOB = "proofOfDOB";
  static String PROOF_OF_RELATIONSHIP = "proofOfRelationship";
  static String PROOF_OF_IDENTITY = "proofOfIdentity";
  static String IDENTITY = "identity";
  static String FORMAT = "format";
  static String TYPE = "type";
  static String VALUE = "value";

  static String PACKET_CREATION_STARTED = "Received. Packet Creation Started...\n";
  static String PACKET_CREATION_TYPE = "NEW";

  static String APINAME_MIDSCHEMAURL="MIDSCHEMAURL";
  static String APINAME_PACKETRECEIVER="PACKETRECEIVER";
  static String APINAME_SYNCSERVICE="SYNCSERVICE";
  static String APINAME_RIDGENERATION="RIDGENERATION";

  static String DATETIME_PATTERN = "opencrvs.datetime.pattern";

  static String PROCESS_TYPE="OPENCRVS_NEW";
  static String CREATION_TYPE="OPENCRVS";

  static String SUPERVISOR_STATUS_APPROVED = "APPROVED";
  static String SUPERVISOR_COMMENT = "UIN Creation for Birth wehbook received from Opencrvs";

  static String REG_SYNC_SERVICE_ID = "mosip.registration.processor.registration.sync.id";
  static String REG_SYNC_APPLICATION_VERSION = "mosip.registration.processor.application.version";

  static String JSON_PROCESSING_EXCEPTION_CODE="OPN-CRVS-001";
  static String JSON_PROCESSING_EXCEPTION_MESSAGE="JSON Processing Exception occured";

  static String PACKET_CREATION_EXCEPTION_CODE="OPN-CRVS-002";
  static String PACKET_CREATION_EXCEPTION_MESSAGE="Exception while creating packet";

  static String TOKEN_GENERATION_FAILED_CODE="OPN-CRVS-003";
  static String TOKEN_GENERATION_FAILED_MESSAGE="Exception while generating token";

  static String API_RESOURCE_UNAVAILABLE_CODE="OPN-CRVS-004";
  static String API_RESOURCE_UNAVAILABLE_1_MESSAGE="Exception occurred while accessing ";
  static String API_RESOURCE_UNAVAILABLE_2_MESSAGE="Could not fetch idschema with version : ";

  static String RID_GENERATE_EXCEPTION_CODE="OPN-CRVS-005";
  static String RID_GENERATE_EXCEPTION_MESSAGE="Unable to generate RID for new packet ";

  static String UNKNOWN_EXCEPTION_CODE="OPN-CRVS-010";
  static String UNKNOWN_EXCEPTION_MESSAGE="Unknown Exception occured";

  static String SYNC_UPLOAD_EXCEPTION_CODE="OPN-CRVS-011";
  static String SYNC_UPLOAD_EXCEPTION_MESSAGE="Exception occured while Packet Sync&Upload ";

  static String SOURCE = "OPENCRVS";
}
