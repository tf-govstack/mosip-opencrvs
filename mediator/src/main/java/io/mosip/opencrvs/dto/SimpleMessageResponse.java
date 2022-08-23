package io.mosip.opencrvs.dto;

import lombok.Data;

@Data
public class SimpleMessageResponse extends BaseResponse{
    public static class ResponseMessage{
        public String message;
        public ResponseMessage(String msg){
            this.message = msg;
        }
    }

    private ResponseMessage response;

    public SimpleMessageResponse(){
        super();
        this.response = null;
    }

    public static SimpleMessageResponse setResponseMessage(String message){
        SimpleMessageResponse smr = new SimpleMessageResponse();
        smr.response = new ResponseMessage(message);
        return smr;
    }
}
