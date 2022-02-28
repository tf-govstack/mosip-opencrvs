package io.mosip.opencrvs;

import java.io.IOException;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;
import java.util.concurrent.CompletableFuture;

@SpringBootApplication(scanBasePackages = { "io.mosip.opencrvs","io.mosip.opencrvs.*", "io.mosip.kernel.core.*",
		"io.mosip.kernel.crypto.jce.*", "io.mosip.commons.packet.*", "io.mosip.kernel.keygenerator.bouncycastle.*"})
@EnableAsync
public class Receiver {

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
	public RestTemplate selfTokenRestTemplate() throws IOException {
		BasicCookieStore cookieStore = new BasicCookieStore();
		BasicClientCookie cookie = new BasicClientCookie("Authorization",Utilities.getToken(env));
		cookie.setDomain(env.getProperty("mosip.api.internal.host"));
		cookieStore.addCookie(cookie);
		return new RestTemplate(new HttpComponentsClientHttpRequestFactory(HttpClientBuilder.create().setDefaultCookieStore(cookieStore).build()));
	}

	public static void main(String[] args){
	  SpringApplication.run(Receiver.class, args);
	  System.out.println("Show started");
	}
}
