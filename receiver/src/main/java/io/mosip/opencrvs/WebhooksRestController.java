package io.mosip.opencrvs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.concurrent.CompletableFuture;

import org.json.JSONObject;
import org.json.JSONException;

import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@RestController
class WebhooksRestController{

  RestTemplate restTemplate = new RestTemplate();

  @Autowired
  Environment env;

  @Autowired
  ReceiverCreatePacket receiverCreatePacket;

  @GetMapping("/ping")
  public String ping(){
    System.out.println(env.getProperty("ram.ka.nam"));
    return "ok";
  }

  @GetMapping("/webhooks")
  public ResponseEntity<String> getWebhooks(@RequestParam String mode, @RequestParam String challenge, @RequestParam String topic){
    if((!mode.equals("subscribe"))||(!topic.equals("BIRTH_REGISTERED")) ){
      return ResponseEntity.badRequest().build();
    }
    return ResponseEntity.ok("{\"challenge\":\""+ challenge +"\"}");
  }

  @PostMapping("/subscribe")
  public ResponseEntity<String> postSubscribe(){
    //get authtoken
    HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> request = new HttpEntity<>("{\"client_id\":\""+ env.getProperty("CLIENT_ID") +"\",\"client_secret\":\""+ env.getProperty("CLIENT_SECRET") +"\"}",requestHeaders);
    ResponseEntity<String> responseForRequest = restTemplate.postForEntity(env.getProperty("AUTH_URL")+"/authenticateSystemClient", request, String.class);
    if(!responseForRequest.getStatusCode().equals(HttpStatus.OK)){
      return ResponseEntity.badRequest().body("{\"message\":\"Cannot get auth token\"}");
    }
    String token;
    try{token = (String) new JSONObject(responseForRequest.getBody()).get("token");}
    catch(JSONException e){return new ResponseEntity<>("{\"message\":\"Cannot understand token\"}",HttpStatus.INTERNAL_SERVER_ERROR);}

    //subscribe
    requestHeaders = new HttpHeaders();
    requestHeaders.setContentType(MediaType.APPLICATION_JSON);
    requestHeaders.set("Authorization","Bearer "+token);
    request = new HttpEntity<>("{\"hub\":{\"callback\":\""+ env.getProperty("CALLBACK_URL") +"\",\"mode\":\"subscribe\",\"secret\":\""+ env.getProperty("SHA_SECRET") +"\",\"topic\":\"BIRTH_REGISTERED\"}}",requestHeaders);
    responseForRequest = restTemplate.postForEntity(env.getProperty("WEBHOOK_URL"), request, String.class);
    if(responseForRequest.getStatusCode().equals(HttpStatus.ACCEPTED)) return new ResponseEntity("{\"message\":\"Error while subscription\"}",HttpStatus.INTERNAL_SERVER_ERROR);
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

  @PostMapping("/webhooks")
  public ResponseEntity<String> postWebhooks(@RequestBody String body){
    try{
      File yourFile = new File(/*id*/ "datafile"+".json");
      yourFile.createNewFile();
      FileOutputStream oFile = new FileOutputStream(yourFile, false);
      oFile.write(body.getBytes());
    } catch(IOException ioe){}

    try{
      CompletableFuture<String> cf = receiverCreatePacket.createPacket(body);
    } catch(Exception be){/*LOGG*/}

    return ResponseEntity.ok(Constants.PACKET_CREATION_STARTED);
  }

}
