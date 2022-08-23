package io.mosip.opencrvs.dto;

import lombok.Data;

@Data
public class BaseResponse {
    private static String DEFAULT_VERSION="1.0";
    private static String DEFAULT_ID="mosip-opencrvs-mediator";
    private String version;
    private String id;
    private String responseTime;
    public BaseResponse(){
        this.version=DEFAULT_VERSION;
        this.id=DEFAULT_ID;
        this.responseTime="";
    }
}
