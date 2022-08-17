package io.mosip.opencrvs.error;

public class ErrorCode{
  // for rest util
  public static final String TOKEN_PARSING_EXCEPTION = "Token json parsing exception";

  public static final String AUTH_TOKEN_EXCEPTION_CODE = "OPN-AUT-001";
  public static final String AUTH_TOKEN_EXCEPTION_MESSAGE = "Error retrieving auth token";

  public static final String VALIDATE_TOKEN_EXCEPTION_CODE = "OPN-AUT-002";
  public static final String VALIDATE_TOKEN_EXCEPTION_MESSAGE = "Invalid auth token / Unable to validate Token";

  public static final String MISSING_ROLE_AUTH_TOKEN_EXCEPTION_CODE = "OPN-AUT-003";
  public static final String MISSING_ROLE_AUTH_TOKEN_EXCEPTION_MESSAGE = "Missing Required Roles";
  public static final String ERROR_ROLE_AUTH_TOKEN_EXCEPTION_CODE = "OPN-AUT-004";
  public static final String ERROR_ROLE_AUTH_TOKEN_EXCEPTION_MESSAGE = "Error getting realm_access.roles from jwt token";

  public static final String SUBSCRIBE_FAILED_EXCEPTION_CODE = "OPN-SUB-001";
  public static final String SUBSCRIBE_FAILED_EXCEPTION_MESSAGE = "Improper response from opencrvs on subscribe";

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

  public static final String IDJSON_BUILD_EXCEPTION_CODE="OPN-CRVS-006";
  public static final String IDJSON_BUILD_EXCEPTION_MESSAGE="Unable to build idJson Fields from opencrvs message body ";

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

  public static final String CRYPTO_READ_PRIVATE_KEY_EXCEPTION_CODE="OPN-CRVS-020";
  public static final String CRYPTO_READ_PRIVATE_KEY_EXCEPTION_MESSAGE="Error reading private key ";
  public static final String CRYPTO_INIT_PRIVATE_KEY_EXCEPTION_CODE="OPN-CRVS-021";
  public static final String CRYPTO_INIT_PRIVATE_KEY_EXCEPTION_MESSAGE="Unable to initialize private key ";
  public static final String CRYPTO_DECRYPT_EXCEPTION_CODE="OPN-CRVS-022";
  public static final String CRYPTO_DECRYPT_EXCEPTION_MESSAGE="Unable to decrypt encrypted data ";
  public static final String CRYPTO_ENCRYPT_EXCEPTION_CODE="OPN-CRVS-023";
  public static final String CRYPTO_ENCRYPT_EXCEPTION_MESSAGE="Unable to encrypt encrypted data ";
  public static final String CRYPTO_READ_PUBLIC_KEY_EXCEPTION_CODE="OPN-CRVS-024";
  public static final String CRYPTO_READ_PUBLIC_KEY_EXCEPTION_MESSAGE="Error reading public key ";
  public static final String CRYPTO_INIT_PUBLIC_KEY_EXCEPTION_CODE="OPN-CRVS-025";
  public static final String CRYPTO_INIT_PUBLIC_KEY_EXCEPTION_MESSAGE="Unable to initialize public key ";
  public static final String CRYPTO_SIGN_VERIFY_EXCEPTION_CODE="OPN-CRVS-026";
  public static final String CRYPTO_SIGN_VERIFY_EXCEPTION_MESSAGE="Improper Signature / Unable to verify signed data ";
  public static final String PRE_PROCESS_DATA_EXCEPTION_CODE="OPN-CRVS-027";
  public static final String PRE_PROCESS_DATA_EXCEPTION_MESSAGE="Error while preProcessing data";
}
