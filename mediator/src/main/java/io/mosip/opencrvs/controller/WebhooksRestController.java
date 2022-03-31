package io.mosip.opencrvs.controller;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.opencrvs.util.RestTokenUtil;
import org.assertj.core.util.Arrays;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import io.mosip.kernel.core.logger.spi.Logger;

import io.mosip.opencrvs.constant.Constants;
import io.mosip.opencrvs.constant.LoggingConstants;
import io.mosip.opencrvs.error.ErrorCode;
import io.mosip.opencrvs.util.RestUtil;
import io.mosip.opencrvs.util.LogUtil;
import io.mosip.opencrvs.service.Producer;

@RestController
public class WebhooksRestController{

  private static final Logger LOGGER = LogUtil.getLogger(WebhooksRestController.class);

  @Autowired
  private Environment env;

  @Autowired
  private Producer producer;

  @Autowired
  private RestUtil restUtil;

  @Autowired
  private RestTokenUtil restTokenUtil;

  @GetMapping("/ping")
  public String ping(){
    return "ok";
  }

  @PostMapping("/subscribe")
  public ResponseEntity<String> postSubscribe(){
    LOGGER.debug(LoggingConstants.SESSION, LoggingConstants.ID, "RestController", "POST /subscribe");
    try{
      restUtil.websubSubscribe();
    }
    catch(Exception e){
      if(e.getMessage().equals(ErrorCode.AUTH_TOKEN_EXCEPTION)){
        return ResponseEntity.badRequest().body("{\"message\":\"Cannot get auth token\"}");
      }
      else if(e.getMessage().equals(ErrorCode.SUBSCRIBE_FAILED_EXCEPTION)){
        return new ResponseEntity("{\"message\":\"Error while subscription\"}",HttpStatus.INTERNAL_SERVER_ERROR);
      }
    }
    return ResponseEntity.ok("{\"message\":\"Successfully subscribed\"}");
  }

  // @PostMapping("/unsubscribe")
  // public ResponseEntity<String> postUnsubscribe(){
  //   HttpHeaders requestHeaders = new HttpHeaders();
  //   requestHeaders.setContentType(MediaType.APPLICATION_JSON);
  //   HttpEntity<String> request = new HttpEntity<>("{\"client_id\":\""+ env.getProperty("CLIENT_ID") +"\",\"client_secret\":\""+ env.getProperty("CLIENT_SECRET") +"\"}",requestHeaders);
  //   ResponseEntity<String> responseForRequest = restTemplate.postForEntity(env.getProperty("AUTH_URL")+"/authenticateSystemClient", request);
  //   if(!responseForRequest.getStatusCode().equals(HttpStatus.OK)){
  //     return ResponseEntity.badRequest().body("{\"message\":\"Cannot get auth token\"}");
  //   }
  //   String token;
  //   try{token = (String) new JSONException(responseForRequest).get("token");}
  //   catch(Exception e){return ResponseEntity.internalServerError().body("{\"message\":\"Cannot understand token\"}");}
  //
  //   // get webhook id first
  //
  //   //unsubscribe
  //   requestHeaders = new HttpHeaders();
  //   requestHeaders.setContentType(MediaType.APPLICATION_JSON);
  //   requestHeaders.setBearerAuth(token);
  //   request = new HttpEntity<>(null,requestHeaders);
  //   responseForRequest = restTemplate.delete(env.getProperty("WEBHOOK_URL")+"/{id}");
  //   if(responseForRequest.getStatusCode() != 200) return ResponseEntity.internalServerError().body("{\"message\":\"Error while unsubscription\"}");
  //   return ResponseEntity.ok("{\"message\":\"Successfully unsubscribed\"}");
  // }

  @PostMapping(value="/birth",consumes=MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> postBirth(@CookieValue("Authorization") String authToken, @RequestBody String body){
    LOGGER.debug(LoggingConstants.SESSION, LoggingConstants.ID, "RestController", "POST /birth; data: " + body);
    try {
      restTokenUtil.validateToken(env.getProperty("mosip.iam.validate_endpoint"), authToken, null);
    } catch(Exception e) {
      LOGGER.error(LoggingConstants.SESSION,LoggingConstants.ID,ExceptionUtils.getStackTrace(e));
      return new ResponseEntity<>("{\"message\":\""+e.getMessage()+"\"}", HttpStatus.UNAUTHORIZED);
    }

    try{
      producer.produce(body);
    } catch(Exception e){
      LOGGER.error(LoggingConstants.SESSION,LoggingConstants.ID,"RestController","POST / birth; Error while producing data " + ExceptionUtils.getStackTrace(e));
    }

    return ResponseEntity.ok("{\"message\":\"" + Constants.PACKET_CREATION_STARTED + "\"}");
  }

  @PostMapping(value="/receiveCredentialBirth", consumes=MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> postReceiveUinOnBirth(@CookieValue(value="Authorization",required = false) String authToken, @RequestBody String body){
    restUtil.proxyPassReceivedCredential(body);
    return ResponseEntity.ok("{\"message\":\"Received\"}");
  }
  @GetMapping("/receiveCredentialBirth")
  public ResponseEntity<String> getReceiveUinOnBirth(
          @CookieValue(value="Authorization",required=false) String authToken,
          @RequestParam(value="hub.topic") String topic,
          @RequestParam(value="hub.mode") String mode,
          @RequestParam(value="hub.reason",required = false) String reason,
          @RequestParam(value="hub.challenge",required = false) String challenge
  ){
    if(reason!=null && !reason.isEmpty()){
      LOGGER.info(LoggingConstants.SESSION, LoggingConstants.ID, "websubSubscribe", "Subscription Success request called. Data: {\"hub.topic\":\""+topic+"\", \"hub.mode\":\""+mode+"\",\"hub.reason\":\""+reason+"\"}");
      return ResponseEntity.ok().build();
    }
    if(challenge!=null && !challenge.isEmpty()){
      LOGGER.info(LoggingConstants.SESSION, LoggingConstants.ID, "websubSubscribe", "Subscription Verification request called. Data: {\"hub.topic\":\""+topic+"\", \"hub.mode\":\""+mode+"\",\"hub.challenge\":\""+challenge+"\"}");
      return ResponseEntity.ok(challenge);
    }
    return ResponseEntity.ok().build();
  }

}
