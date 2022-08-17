package io.mosip.opencrvs.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WebsubRequest {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Event{
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Data{
            public String opencrvsBRN;
            public String credential;
        }
        public Data data;
        public String transactionId;
    }
    public Event event;
}
