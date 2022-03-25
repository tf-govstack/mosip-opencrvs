package io.mosip.registration.processor.opencrvs.constants;

public enum OpencrvsStatusCode {
    OPENCRVS_REQUEST_FAILED("RPR-OPC-FAILED-009", "OpenCRVS request failed"),
    OPENCRVS_REQUEST_SUCCESS("RPR-OPC-SUCCESS-001", "OpenCRVS request submitted"),;

    private final String statusComment;
    private final String statusCode;

    private OpencrvsStatusCode(String statusCode, String statusComment) {
        this.statusCode = statusCode;
        this.statusComment = statusComment;
    }

    public String getMessage() {
        return this.statusComment;
    }

    public String getCode() {
        return this.statusCode;
    }
}
