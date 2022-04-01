package io.mosip.opencrvs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import io.mosip.kernel.core.logger.spi.Logger;

import io.mosip.opencrvs.constant.LoggingConstants;
import io.mosip.opencrvs.util.LogUtil;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = { "io.mosip.opencrvs","io.mosip.opencrvs.*", "io.mosip.kernel.core.*",
		"io.mosip.kernel.crypto.jce.*", "io.mosip.commons.packet.*", "io.mosip.kernel.keygenerator.bouncycastle.*"})
@EnableAsync
@EnableScheduling
public class Mediator {

	private static final Logger LOGGER = LogUtil.getLogger(Mediator.class);

	public static void main(String[] args){
	  SpringApplication.run(Mediator.class, args);
	  LOGGER.info(LoggingConstants.SESSION, LoggingConstants.ID, "ROOT", "App started");
	}
}
