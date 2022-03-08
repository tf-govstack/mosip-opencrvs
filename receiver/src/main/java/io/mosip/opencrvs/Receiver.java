package io.mosip.opencrvs;

import java.io.IOException;
import java.util.Collections;

import javax.annotation.PreDestroy;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;

import java.util.concurrent.Executor;
import java.util.concurrent.CompletableFuture;

import io.mosip.kernel.core.logger.spi.Logger;

@SpringBootApplication(scanBasePackages = { "io.mosip.opencrvs","io.mosip.opencrvs.*", "io.mosip.kernel.core.*",
		"io.mosip.kernel.crypto.jce.*", "io.mosip.commons.packet.*", "io.mosip.kernel.keygenerator.bouncycastle.*"})
@EnableAsync
public class Receiver {

	private static Logger LOGGER = Utilities.getLogger(Receiver.class);

	@Autowired
	Environment env;

	@Bean
	public Executor taskExecutor() {
	  ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
	  executor.setCorePoolSize(Integer.parseInt(env.getProperty("receiver.core.pool.size")));
	  executor.setMaxPoolSize(Integer.parseInt(env.getProperty("receiver.max.pool.size")));
	  executor.setQueueCapacity(Integer.parseInt(env.getProperty("receiver.queue.capacity")));
	  executor.setThreadNamePrefix("Receiver-");
	  executor.initialize();
	  return executor;
	}

	@Bean
	@ConditionalOnProperty(name="kernel.auth.adapter.available",havingValue="false",matchIfMissing=false)
	public RestTemplate selfTokenRestTemplate() throws IOException {
		// BasicCookieStore cookieStore = new BasicCookieStore();
		// BasicClientCookie cookie = new BasicClientCookie("Authorization",Utilities.getToken(env));
		// cookie.setDomain(env.getProperty("mosip.api.internal.host"));
		// cookieStore.addCookie(cookie);
		RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory(HttpClientBuilder.create().disableCookieManagement().build()));
		restTemplate.setInterceptors(Collections.singletonList(new ClientHttpRequestInterceptor(){
			@Override
			public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException{
				request.getHeaders().set("Cookie","Authorization="+Utilities.getToken(env));	
				return execution.execute(request, body);
			}
		}));
		return restTemplate;
	}

	public static void main(String[] args){
	  SpringApplication.run(Receiver.class, args);
	  LOGGER.info(Constants.SESSION, Constants.ID, "ROOT", "Show started");
	}

	@EventListener(ApplicationReadyEvent.class)
	public void init(){
		if(env.getProperty("opencrvs.subscribe.startup").equals("true")){
			try{
				String res = new RestTemplate().postForObject("http://localhost:"+env.getProperty("server.port")+"/subscribe",null,String.class);
				// if(res!="Success"){LOGGER.error(Constants.SESSION, Constants.ID, "ROOT", "Unable to subscribe to opencrvs, response: "+res);}
				LOGGER.info(Constants.SESSION, Constants.ID, "ROOT", "Subscription Successful");
			}
			catch(Exception e){
				LOGGER.error(Constants.SESSION, Constants.ID, "ROOT", "Unable to subscribe to opencrvs, exception: "+e);
			}
		}
	}

	@PreDestroy
	public void tearDown(){
		try{
			// String res = new RestTemplate().postForObject("http://localhost:"+env.getProperty("server.port")+"/unsubscribe",null,String.class);
			LOGGER.info(Constants.SESSION, Constants.ID, "ROOT", "Unsubscribe Successful");
		}
		catch(Exception e){}
	}

}
