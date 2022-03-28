package io.mosip.registration.processor.opencrvs.exception;

public enum OpencrvsErrorMessages {
    RPR_OPC_UIN_NOT_FOUND_IN_DATABASE("RPR-OPC-002", "UIN not found in database"),
    RPR_OPC_VID_NOT_AVAILABLE_EXCEPTION("RPR-OPC-027", "vid not available"),
    RPR_OPC_OPENCRVS_REQUEST_FAILED("RPR-OPC-025", "OpenCRVS Stage request failed"),
    ;

    private final String errorMessage;
    private final String errorCode;

    private OpencrvsErrorMessages(String errorCode, String errorMsg) {
        this.errorCode = errorCode;
        this.errorMessage = errorMsg;
    }

    public String getMessage() {
        return this.errorMessage;
    }

    public String getCode() {
        return this.errorCode;
    }
}
