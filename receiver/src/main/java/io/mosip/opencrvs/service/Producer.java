package io.mosip.opencrvs.service;

import org.json.JSONObject;
import org.json.JSONException;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.kernel.core.logger.spi.Logger;

import io.mosip.opencrvs.error.ErrorCode;
import io.mosip.opencrvs.util.KafkaUtil;
import io.mosip.opencrvs.util.LogUtil;

@Service
public class Producer{
  private static Logger LOGGER = LogUtil.getLogger(Producer.class);

  @Autowired
  private Environment env;

  @Autowired
  private KafkaUtil kafkaUtil;

  public void produce(String data) {
    String topicName = env.getProperty("mosip.opencrvs.kafka.topic");
    kafkaUtil.syncPutMessageInKafka(topicName,getIdFromBody(data),data);
  }

  public static String getIdFromBody(String requestBody){
    try{
      return new JSONObject(requestBody).getString("id");

    }
    catch(JSONException je){
      throw new BaseUncheckedException(ErrorCode.JSON_PROCESSING_EXCEPTION_CODE,ErrorCode.JSON_PROCESSING_EXCEPTION_MESSAGE+"while getting txn_id ",je);
    }
  }
}
