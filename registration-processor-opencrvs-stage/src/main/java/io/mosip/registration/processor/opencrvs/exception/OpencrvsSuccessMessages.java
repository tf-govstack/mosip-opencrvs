package io.mosip.registration.processor.opencrvs.exception;

public enum OpencrvsSuccessMessages {
    RPR_OPENCRVS_STAGE_REQUEST_SUCCESS("RPR-OPC-000", "OpenCRVS request submitted"),;

    private final String successMessage;
    private final String successCode;

    private OpencrvsSuccessMessages(String errorCode, String errorMsg) {
        this.successCode = errorCode;
        this.successMessage = errorMsg;
    }

    public String getMessage() {
        return this.successMessage;
    }

    public String getCode() {
        return this.successCode;
    }
}
