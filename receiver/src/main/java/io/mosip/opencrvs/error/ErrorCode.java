package io.mosip.opencrvs.error;

public class ErrorCode{
  // for rest util
  public static final String TOKEN_PARSING_EXCEPTION = "Token json parsing exception";
  public static final String AUTH_TOKEN_EXCEPTION = "Auth token error";
  public static final String SUBSCRIBE_FAILED_EXCEPTION = "Improper response from opencrvs on subscribe";

  // exceptions
  public static final String JSON_PROCESSING_EXCEPTION_CODE="OPN-CRVS-001";
  public static final String JSON_PROCESSING_EXCEPTION_MESSAGE="JSON Processing Exception occured";

  public static final String PACKET_CREATION_EXCEPTION_CODE="OPN-CRVS-002";
  public static final String PACKET_CREATION_EXCEPTION_MESSAGE="Exception while creating packet";

  public static final String TOKEN_GENERATION_FAILED_CODE="OPN-CRVS-003";
  public static final String TOKEN_GENERATION_FAILED_MESSAGE="Exception while generating token";

  public static final String API_RESOURCE_UNAVAILABLE_CODE="OPN-CRVS-004";
  public static final String API_RESOURCE_UNAVAILABLE_1_MESSAGE="Exception occurred while accessing ";
  public static final String API_RESOURCE_UNAVAILABLE_2_MESSAGE="Could not fetch idschema with version : ";

  public static final String RID_GENERATE_EXCEPTION_CODE="OPN-CRVS-005";
  public static final String RID_GENERATE_EXCEPTION_MESSAGE="Unable to generate RID for new packet ";

  public static final String UNKNOWN_EXCEPTION_CODE="OPN-CRVS-010";
  public static final String UNKNOWN_EXCEPTION_MESSAGE="Unknown Exception occured";

  public static final String SYNC_UPLOAD_EXCEPTION_CODE="OPN-CRVS-011";
  public static final String SYNC_UPLOAD_EXCEPTION_MESSAGE="Exception occured while Packet Sync&Upload ";

  public static final String KAFKA_CONNECTION_EXCEPTION_CODE="OPN-CRVS-012";
  public static final String KAFKA_CONNECTION_EXCEPTION_MESSAGE="Error connecting to kafka server";

  public static final String KAFKA_TOPIC_CREATE_EXCEPTION_CODE="OPN-CRVS-013";
  public static final String KAFKA_TOPIC_CREATE_EXCEPTION_MESSAGE="Error creating kafka topic";

  public static final String KAFKA_MSG_SEND_EXCEPTION_CODE="OPN-CRVS-014";
  public static final String KAFKA_MSG_SEND_EXCEPTION_MESSAGE="Error putting message in kafka topic";
}
