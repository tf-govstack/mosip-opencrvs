package io.mosip.opencrvs.util;

import java.time.Duration;

import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.Future;
import javax.annotation.PostConstruct;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.exception.ExceptionUtils;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.kernel.core.logger.spi.Logger;

import io.mosip.opencrvs.constant.LoggingConstants;
import io.mosip.opencrvs.error.ErrorCode;

@Component
public class KafkaUtil{

  private static Logger LOGGER = LogUtil.getLogger(KafkaUtil.class);

  @Value("${mosip.opencrvs.kafka.bootstrap.server}")
  private String bootstrapServer;
  @Value("${mosip.opencrvs.kafka.topic.name}")
  private String topicName;
  @Value("${mosip.opencrvs.kafka.admin.request.timeout.ms}")
  private String adminRequestTimeout;
  @Value("${mosip.opencrvs.kafka.consumer.group.id}")
  private String consumerGroupId;
  @Value("${mosip.opencrvs.kafka.consumer.enable.auto.commit}")
  private String consumerEnableAutoCommit;
  @Value("${mosip.opencrvs.kafka.consumer.auto.commit.interval.ms}")
  private String consumerAutoCommitInterval;
  @Value("${mosip.opencrvs.kafka.consumer.auto.offset.reset}")
  private String consumerAutoOffsetReset;

  private Admin kafkaAdmin;
  private KafkaProducer producer;
  private KafkaConsumer consumer;

  @PostConstruct
  public void init() {
    try{
      Properties adminProps = new Properties();
      adminProps.setProperty(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
      adminProps.setProperty(AdminClientConfig.	REQUEST_TIMEOUT_MS_CONFIG, adminRequestTimeout);
      kafkaAdmin = Admin.create(adminProps);
      LOGGER.info(LoggingConstants.SESSION,LoggingConstants.ID,"KafkaUtil","Admin client created");

      Properties prodProps = new Properties();
      prodProps.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
      prodProps.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
      prodProps.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
      producer = new KafkaProducer<String, String>(prodProps);
      LOGGER.info(LoggingConstants.SESSION,LoggingConstants.ID,"KafkaUtil","Producer Created");

      Properties consProps = new Properties();
      consProps.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
      consProps.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
      consProps.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
      consProps.setProperty(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
      consProps.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, consumerEnableAutoCommit);
      consProps.setProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, consumerAutoCommitInterval);
      consProps.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumerAutoOffsetReset);
      consumer = new KafkaConsumer<String, String>(consProps);
      consumer.subscribe(Collections.singletonList(topicName));
      LOGGER.info(LoggingConstants.SESSION,LoggingConstants.ID,"KafkaUtil","Consumer Created and subscribed");

      kafkaAdmin.listTopics().names().get();
    }
    catch(Exception e){
      LOGGER.error(LoggingConstants.SESSION,LoggingConstants.ID,"KafkaUtil","Error connecting to kafka " + ExceptionUtils.getStackTrace(e));
      throw new BaseUncheckedException(ErrorCode.KAFKA_CONNECTION_EXCEPTION_CODE,ErrorCode.KAFKA_CONNECTION_EXCEPTION_MESSAGE,e);
    }
  }

  public void createTopicIfNotExist(String topicName, int partitions, short replicationFactor) {
    try{
      if(!kafkaAdmin.listTopics().names().get().contains(topicName)){
        kafkaAdmin.createTopics(Collections.singletonList(new NewTopic(topicName,partitions,replicationFactor))).values().get(topicName).get();
        LOGGER.info(LoggingConstants.SESSION,LoggingConstants.ID,"KafkaUtil","Topic Created");
      }
      else{
        LOGGER.info(LoggingConstants.SESSION,LoggingConstants.ID,"KafkaUtil","Topic already exists");
      }
    }
    catch(Exception e){
      LOGGER.error(LoggingConstants.SESSION,LoggingConstants.ID,"KafkaUtil","Error creating topic " + ExceptionUtils.getStackTrace(e));
      throw new BaseUncheckedException(ErrorCode.KAFKA_TOPIC_CREATE_EXCEPTION_CODE,ErrorCode.KAFKA_TOPIC_CREATE_EXCEPTION_MESSAGE,e);
    }

  }

  public void syncPutMessageInKafka(String topic, String key, String value) throws BaseCheckedException{
    LOGGER.info(LoggingConstants.SESSION, LoggingConstants.ID, "Kafka Key - "+key, "Received put message request");
    LOGGER.debug(LoggingConstants.SESSION, LoggingConstants.ID, "Kafka Key - "+key, "Received put message request with value : " + value);
    try{
      ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
      Future<RecordMetadata> future = producer.send(record);
      producer.flush();
      RecordMetadata recordMetadata = future.get();
      if(recordMetadata == null){
        LOGGER.error(LoggingConstants.SESSION, LoggingConstants.ID, "Kafka Key - "+key, "Error putting message");
        throw new BaseCheckedException(ErrorCode.KAFKA_MSG_SEND_EXCEPTION_CODE,ErrorCode.KAFKA_MSG_SEND_EXCEPTION_MESSAGE);
      }
    }
    catch(Exception e){
      LOGGER.error(LoggingConstants.SESSION, LoggingConstants.ID, "Kafka Key - "+key, "Error putting message : "+ ExceptionUtils.getStackTrace(e));
      throw new BaseCheckedException(ErrorCode.KAFKA_MSG_SEND_EXCEPTION_CODE,ErrorCode.KAFKA_MSG_SEND_EXCEPTION_MESSAGE,e);
    }
    LOGGER.info(LoggingConstants.SESSION, LoggingConstants.ID, "Kafka Key - "+key, "Message sent.");
  }

  public ConsumerRecords<String, String> consumerPoll(Duration dur){
    return consumer.poll(dur);
  }
}
