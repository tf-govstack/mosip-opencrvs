package io.mosip.opencrvs.dto;

import lombok.Data;

@Data
public class BaseEventRequest {
    private String id;
    private String requestTime;
    private String data;
    private String signature;

    public String toString(){
        return "{" +
            "\"id\": \"" + this.id + "\"" + "," +
            "\"requestTime\": \"" + this.requestTime + "\"" + "," +
            "\"data\": \"" + this.data + "\"" + "," +
            "\"signature\": \"" + this.signature + "\"" +
        "}";
    }
}
