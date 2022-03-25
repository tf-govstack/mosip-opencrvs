package io.mosip.registration.processor.opencrvs;

import io.mosip.registration.processor.opencrvs.stage.OpencrvsStage;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;;

public class OpencrvsStageApplication {

	public static void main(String[] args){
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.scan("io.mosip.registration.processor.core.config",
				"io.mosip.registration.processor.opencrvs.config",
				"io.mosip.registration.processor.opencrvs.*",
				"io.mosip.registration.processor.rest.client.config",
				"io.mosip.registration.processor.core.kernel.beans",
				"io.mosip.registration.processor.status.config",
				"io.mosip.registration.processor.packet.storage.config");
		ctx.refresh();

		OpencrvsStage printStage = ctx.getBean(OpencrvsStage.class);
		printStage.deployVerticle();
	}
}
