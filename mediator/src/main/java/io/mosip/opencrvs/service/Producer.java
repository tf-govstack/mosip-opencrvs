package io.mosip.opencrvs.service;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.opencrvs.constant.LoggingConstants;
import io.mosip.opencrvs.dto.ReceiveDto;
import io.mosip.opencrvs.util.OpencrvsDataUtil;
import org.json.JSONObject;
import org.json.JSONException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.kernel.core.logger.spi.Logger;

import io.mosip.opencrvs.error.ErrorCode;
import io.mosip.opencrvs.util.KafkaUtil;
import io.mosip.opencrvs.util.LogUtil;

@Service
public class Producer {
    private static Logger LOGGER = LogUtil.getLogger(Producer.class);

    @Autowired
    private Environment env;

    @Autowired
    private KafkaUtil kafkaUtil;

    @Autowired
    private OpencrvsDataUtil opencrvsDataUtil;

    @Value("${opencrvs.reproduce.on.error}")
    private String reproducerOnError;

    @Value("${opencrvs.reproduce.on.error.delay.ms}")
    private String reproducerOnErrorDelayMs;

    public void produce(String txnId, String data) throws BaseCheckedException {
        String topicName = env.getProperty("mosip.opencrvs.kafka.topic");
        if(txnId!=null && !txnId.isEmpty()) {
            kafkaUtil.syncPutMessageInKafka(topicName, txnId, data);
        } else {
            kafkaUtil.syncPutMessageInKafka(topicName, opencrvsDataUtil.getTxnIdFromBody(data), data);
        }
    }

    @Async
    public void reproduce(String txnId, String data) {
        if (!("true".equalsIgnoreCase(reproducerOnError))) return;
        try {
            try {
                Thread.sleep(Long.parseLong(reproducerOnErrorDelayMs));
            } catch (Exception ignored) {}
            produce(txnId, data);
        } catch (Exception e) {
            LOGGER.error(LoggingConstants.SESSION, LoggingConstants.ID, "txn_id - " + txnId, "Unable to put back failed transaction to producer. " + ExceptionUtils.getStackTrace(e));
        }
    }
}
