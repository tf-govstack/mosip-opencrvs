package io.mosip.opencrvs.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WebsubRequest {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Event{
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Data{
        	@JsonIgnoreProperties(ignoreUnknown = true)
        	public static class Proof{
        		public String signature;
        	}
            public String opencrvsBRN;
            public String credential;
            public String credentialType;
            public String protectionKey;
            public Proof proof;
        }
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Type{
    		public String namespace;
    		public String name;
    	}
    	public String id;
    	public String transactionId;
    	public Type type;
    	public String timestamp;
    	public String dataShareUri;
        public Data data;
    }
    public String publisher;
    public String topic;
    public String publishedOn;
    public Event event;
}
