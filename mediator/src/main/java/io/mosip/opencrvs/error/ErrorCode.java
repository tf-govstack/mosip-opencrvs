package io.mosip.opencrvs.error;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.exception.BaseUncheckedException;

public enum ErrorCode{

    AUTH_TOKEN_EXCEPTION("OPN-AUT-001","Error retrieving auth token"),
    VALIDATE_TOKEN_EXCEPTION("OPN-AUT-002", "Invalid auth token / Unable to validate Token"),
    MISSING_TOKEN_EXCEPTION("OPN-AUT-003", "Missing Authorization Cookie / Empty Authorization Token."),
    MISSING_ROLE_AUTH_TOKEN_EXCEPTION("OPN-AUT-004", "Missing Required Roles"),
    ERROR_ROLE_AUTH_TOKEN_EXCEPTION("OPN-AUT-005", "Error getting realm_access.roles from jwt token"),
    SUBSCRIBE_FAILED_EXCEPTION("OPN-SUB-001", "Improper response from opencrvs on subscribe"),
    JSON_PROCESSING_EXCEPTION("OPN-CRVS-001","JSON Processing Exception occured"),
    PACKET_CREATION_EXCEPTION("OPN-CRVS-002","Exception while creating packet"),
    TOKEN_GENERATION_FAILED("OPN-CRVS-003", "Exception while generating token"),
    API_RESOURCE_UNAVAILABLE("OPN-CRVS-004","Exception occurred while accessing "),
    API_RESOURCE_UNAVAILABLE_2("OPN-CRVS-004","Could not fetch idschema with version : "),
    RID_GENERATE_EXCEPTION("OPN-CRVS-005","Unable to generate RID for new packet "),
    IDJSON_BUILD_EXCEPTION("OPN-CRVS-006","Unable to build idJson Fields from opencrvs message body "),
    UNKNOWN_EXCEPTION("OPN-CRVS-010","Unknown Exception occured"),
    SYNC_UPLOAD_EXCEPTION("OPN-CRVS-011","Exception occured while Packet Sync&Upload "),
    KAFKA_CONNECTION_EXCEPTION("OPN-CRVS-012","Error connecting to kafka server"),
    KAFKA_TOPIC_CREATE_EXCEPTION("OPN-CRVS-013","Error creating kafka topic"),
    KAFKA_MSG_SEND_EXCEPTION("OPN-CRVS-014", "Error putting message in kafka topic"),
    CRYPTO_READ_PRIVATE_KEY_EXCEPTION("OPN-CRVS-020", "Error reading private key "),
    CRYPTO_INIT_PRIVATE_KEY_EXCEPTION("OPN-CRVS-021", "Unable to initialize private key "),
    CRYPTO_DECRYPT_EXCEPTION("OPN-CRVS-022", "Unable to decrypt encrypted data "),
    CRYPTO_ENCRYPT_EXCEPTION("OPN-CRVS-023", "Unable to encrypt encrypted data "),
    CRYPTO_READ_PUBLIC_KEY_EXCEPTION("OPN-CRVS-024", "Error reading public key "),
    CRYPTO_INIT_PUBLIC_KEY_EXCEPTION("OPN-CRVS-025", "Unable to initialize public key "),
    CRYPTO_SIGN_VERIFY_EXCEPTION("OPN-CRVS-026", "Improper Signature / Unable to verify signed data "),
    PRE_PROCESS_DATA_EXCEPTION("OPN-CRVS-027", "Error while preProcessing data"),
    UIN_NOT_VALID_IN_DEATH_EVENT("OPN-CRVS-028", "Invalid UIN. Or UIN not found in db"),
    MISSING_UIN_IN_DEATH_EVENT("OPN-CRVS-029", "Couldnot get UIN/VID from the death event"),
    UIN_DEACTIVATE_ERROR_DEATH_EVENT("OPN-CRVS-030", "Error Deactivating UIN on Death Event"),
    PARSE_RID_FROM_REQUEST("OPN-CRVS-031", "Error Parsing RID from request"),

    ;
    private final String errorCode;
    private final String errorMessage;
    private ErrorCode(final String errorCode, final String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
    public String getErrorCode() {
        return errorCode;
    }
    public String getErrorMessage() {
        return errorMessage;
    }
    public boolean compareBase(BaseCheckedException e){
        return getErrorCode().equals(e.getErrorCode());
    }
    public boolean compareBase(BaseUncheckedException e){
        return getErrorCode().equals(e.getErrorCode());
    }
    public BaseCheckedException throwChecked(){
        return new BaseCheckedException(getErrorCode(), getErrorMessage());
    }
    public BaseCheckedException throwChecked(Exception e){
        return new BaseCheckedException(getErrorCode(), getErrorMessage(), e);
    }
    public BaseCheckedException throwChecked(String extraMessage, Exception e){
        return new BaseCheckedException(getErrorCode(), getErrorMessage() + extraMessage, e);
    }
    public BaseUncheckedException throwUnchecked() {
        return new BaseUncheckedException(getErrorCode(), getErrorMessage());
    }
    public BaseUncheckedException throwUnchecked(Exception e) {
        return new BaseUncheckedException(getErrorCode(), getErrorMessage(), e);
    }
    public BaseUncheckedException throwUnchecked(String extraMessage, Exception e) {
        return new BaseUncheckedException(getErrorCode(), getErrorMessage() + extraMessage, e);
    }
}
