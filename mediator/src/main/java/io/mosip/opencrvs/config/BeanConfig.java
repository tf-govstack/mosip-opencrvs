package io.mosip.opencrvs.config;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.CompletableFuture;

import javax.sql.DataSource;

import io.mosip.opencrvs.util.RestTokenUtil;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
// import org.apache.http.impl.client.BasicCookieStore;
// import org.apache.http.impl.cookie.BasicClientCookie;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import io.mosip.kernel.core.logger.spi.Logger;

import io.mosip.opencrvs.util.LogUtil;

@Configuration
public class BeanConfig{

	private static Logger LOGGER = LogUtil.getLogger(BeanConfig.class);

	@Value("${mediator.core.pool.size}")
	private int mediatorCorePoolSize;
	@Value("${mediator.max.pool.size}")
	private int mediatorMaxPoolSize;
	@Value("${mediator.queue.capacity}")
	private int mediatorQueueCapacity;

	@Autowired
	private RestTokenUtil restTokenUtil;

	@Bean
	public Executor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(mediatorCorePoolSize);
		executor.setMaxPoolSize(mediatorMaxPoolSize);
		executor.setQueueCapacity(mediatorQueueCapacity);
		executor.setThreadNamePrefix("Mediator-TaskExecutor-");
		executor.initialize();
		return executor;
	}

	@Bean
	public TaskScheduler taskScheduler() {
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.setPoolSize(mediatorCorePoolSize);
		taskScheduler.setThreadNamePrefix("Mediator-TaskScheduler-");
		return taskScheduler;
	}

	@Bean
	@Primary
	@ConfigurationProperties(prefix="mosip.opencrvs.db.datasource")
	public DataSource mosipOpencrvsDbDatasource() {
		return DataSourceBuilder.create().build();
	}

	@Bean
	@ConditionalOnProperty(name="kernel.auth.adapter.available",havingValue="false",matchIfMissing=false)
	public RestTemplate selfTokenRestTemplate() {
		RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory(HttpClientBuilder.create().disableCookieManagement().build()));
		restTemplate.setInterceptors(Collections.singletonList(new ClientHttpRequestInterceptor(){
			@Override
			public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution){
				String mosipAuthToken = restTokenUtil.getMosipAuthToken("selfCacheRestTemplate");
				if(mosipAuthToken==null || mosipAuthToken.isEmpty()) throw new RestClientException("Unable to get mosip auth token");

				try{
					request.getHeaders().set("Cookie","Authorization="+mosipAuthToken);
					return execution.execute(request, body);
				} catch(IOException e){
					throw new RestClientException("Some Error while making selfCacheRestTemplate call",e);
				}
			}
		}));
		return restTemplate;
	}
}
