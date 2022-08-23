package io.mosip.opencrvs.config;

import javax.annotation.PreDestroy;

import io.mosip.kernel.core.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.exception.BaseUncheckedException;

import io.mosip.opencrvs.constant.LoggingConstants;
import io.mosip.opencrvs.service.Receiver;
import io.mosip.opencrvs.util.LogUtil;
import io.mosip.opencrvs.util.KafkaUtil;

@Configuration
public class AppInitConfig{

	private static final Logger LOGGER = LogUtil.getLogger(AppInitConfig.class);

	@Value("${mosip.opencrvs.kafka.topic.name}")
	private String kafkaTopicName;
	@Value("${mosip.opencrvs.kafka.topic.partitions}")
	private int kafkaTopicPartitions;
	@Value("${mosip.opencrvs.kafka.topic.replication.factor}")
	private short kafkaTopicReplication;

	@Autowired
	private KafkaUtil kafkaUtil;

	@Autowired
	private Receiver receiver;

	@EventListener(ApplicationReadyEvent.class)
	public void init() throws BaseCheckedException{
		kafkaUtil.createTopicIfNotExist(kafkaTopicName, kafkaTopicPartitions, kafkaTopicReplication);

		receiver.receive();
		LOGGER.info(LoggingConstants.SESSION, LoggingConstants.ID, "ROOT", "Started Receiver.");
	}

	@PreDestroy
	public void tearDown(){
		try{
			// Unsubscribe here
			LOGGER.info(LoggingConstants.SESSION, LoggingConstants.ID, "ROOT", "Unsubscribe Successful");
		}
		catch(Exception e){
			LOGGER.error(LoggingConstants.FORMATTER_PREFIX, LoggingConstants.SESSION, LoggingConstants.ID, "ROOT", "Error while unsubscribing ", e);
		}
	}
}
