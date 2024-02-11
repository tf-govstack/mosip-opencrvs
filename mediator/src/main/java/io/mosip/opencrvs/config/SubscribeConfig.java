package io.mosip.opencrvs.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import io.mosip.kernel.core.websub.spi.SubscriptionClient;
import io.mosip.kernel.websub.api.model.SubscriptionChangeRequest;
import io.mosip.kernel.websub.api.model.SubscriptionChangeResponse;
import io.mosip.kernel.websub.api.model.UnsubscriptionRequest;

@Configuration
public class SubscribeConfig {
	
	@Autowired
	protected SubscriptionClient<SubscriptionChangeRequest, UnsubscriptionRequest, SubscriptionChangeResponse> subscriptionClient;
	
	@Value("${websub.hub.url}")
	private String hubUrl;
	
	@Value("${mosip.opencrvs.partner.client.sha.secret}")
	private String callBackSecret;
	
	@Value("${mosip.opencrvs.partner.client.id}")
	private String topic;
	
	@Value("${mosip.receive.credential.url}")
	private String callbackUrl;
	
	protected void subscribeForPartnerCertEvent() {
		try {
			System.out.println("Started subscribing.......");
			SubscriptionChangeRequest subscriptionRequest = new SubscriptionChangeRequest();
			subscriptionRequest.setCallbackURL(callbackUrl);
			subscriptionRequest.setSecret(callBackSecret);
			subscriptionRequest.setTopic(topic+ "/CREDENTIAL_ISSUED");
			subscriptionRequest.setHubURL(hubUrl);
			subscriptionClient.subscribe(subscriptionRequest);
			System.out.println("Subscribing done.......... for event " + topic+ "/CREDENTIAL_ISSUED");
		} catch (Exception e) {
			System.out.println("Error while subscribing..........");
			e.printStackTrace();
			throw e;
		}
	}
}
