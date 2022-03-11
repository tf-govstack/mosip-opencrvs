package io.mosip.opencrvs.config;

import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.core.env.Environment;
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
import io.mosip.opencrvs.util.RestUtil;
import io.mosip.opencrvs.util.KafkaUtil;

@Configuration
public class AppInitConfig{

  private static final Logger LOGGER = LogUtil.getLogger(AppInitConfig.class);

  @Autowired
  private Environment env;

  @Autowired
  private RestUtil restUtil;

  @Autowired
  private KafkaUtil kafkaUtil;

  @Autowired
  private Receiver receiver;

  @EventListener(ApplicationReadyEvent.class)
	public void init() throws BaseCheckedException{
		if(env.getProperty("opencrvs.subscribe.startup").equals("true")){
			try{
				String res = restUtil.webhooksSubscribe();
				// if(res!="Success"){LOGGER.error(LoggingConstants.SESSION, LoggingConstants.ID, "ROOT", "Unable to subscribe to opencrvs, response: "+res);}
				LOGGER.info(LoggingConstants.SESSION, LoggingConstants.ID, "ROOT", "Subscription Successful");
			}
			catch(Exception e){
				LOGGER.error(LoggingConstants.SESSION, LoggingConstants.ID, "ROOT", "Unable to subscribe to opencrvs, exception: "+e);
			}
		}
    kafkaUtil.createTopicIfNotExist(env.getProperty("mosip.opencrvs.kafka.topic"),1,(short)1);

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
      LOGGER.error(LoggingConstants.SESSION, LoggingConstants.ID, "ROOT", "Error while unsubscribing " + e);
    }
	}
}
