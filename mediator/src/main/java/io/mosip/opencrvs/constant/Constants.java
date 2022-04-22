package io.mosip.opencrvs.constant;

public class Constants{
  //for generating audit
  public static final String AUDIT_APP_NAME = "opencrvs.audit.app.name";
  public static final String AUDIT_APP_ID = "opencrvs.audit.app.id";

  // for packet creation
  public static final String PROOF_OF_ADDRESS = "proofOfAddress";
  public static final String PROOF_OF_DOB = "proofOfDOB";
  public static final String PROOF_OF_RELATIONSHIP = "proofOfRelationship";
  public static final String PROOF_OF_IDENTITY = "proofOfIdentity";
  public static final String IDENTITY = "identity";

  public static final String PACKET_CREATION_STARTED = "Received. Packet Creation Started.";
  public static final String PACKET_CREATION_FAILED_IMPROPER_JSON = "Improperly JSON Received.";

  public static final String DATETIME_PATTERN = "opencrvs.datetime.pattern";

  // for packet sync
  public static final String SUPERVISOR_STATUS_APPROVED = "APPROVED";
  public static final String SUPERVISOR_COMMENT = "UIN Creation for Birth wehbook received from Opencrvs";

  public static final String REG_SYNC_SERVICE_ID = "mosip.registration.processor.registration.sync.id";
  public static final String REG_SYNC_APPLICATION_VERSION = "mosip.registration.processor.application.version";

}
