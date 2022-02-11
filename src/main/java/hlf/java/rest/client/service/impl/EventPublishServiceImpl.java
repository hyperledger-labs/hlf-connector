package hlf.java.rest.client.service.impl;

import hlf.java.rest.client.service.EventPublishService;
import hlf.java.rest.client.util.FabricClientConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

@Slf4j
@Service("eventPublishService")
@ConditionalOnProperty("kafka.event-listener.topic")
public class EventPublishServiceImpl implements EventPublishService {

  @Value("${kafka.event-listener.topic}")
  private String topicName;

  @Autowired private KafkaTemplate<String, String> kafkaTemplate;

  @Override
  public boolean sendMessage(String msg, String fabricTxId, String eventName, String channelName) {

    log.debug("Send Event Message - " + msg);

    boolean status = true;

    try {

      ProducerRecord<String, String> producerRecord =
          new ProducerRecord<String, String>(topicName, String.valueOf(msg.hashCode()), msg);

      producerRecord
          .headers()
          .add(
              new RecordHeader(FabricClientConstants.FABRIC_TRANSACTION_ID, fabricTxId.getBytes()));
      producerRecord
          .headers()
          .add(new RecordHeader(FabricClientConstants.FABRIC_EVENT_NAME, eventName.getBytes()));
      producerRecord
          .headers()
          .add(new RecordHeader(FabricClientConstants.FABRIC_CHANNEL_NAME, channelName.getBytes()));

      ListenableFuture<SendResult<String, String>> future = kafkaTemplate.send(producerRecord);

      future.addCallback(
          new ListenableFutureCallback<SendResult<String, String>>() {

            @Override
            public void onSuccess(SendResult<String, String> result) {
              log.info(
                  "Sent message=["
                      + msg
                      + "] with offset=["
                      + result.getRecordMetadata().offset()
                      + "]");
            }

            @Override
            public void onFailure(Throwable ex) {
              log.warn("Unable to send message=[" + msg + "] due to : " + ex.getMessage());
            }
          });

    } catch (Exception ex) {
      status = false;
      log.error("Error sending message - " + ex.getMessage());
    }

    return status;
  }

  @Override
  public boolean publishChaincodeEvents(
      String payload, String fabricTxId, String eventName, String channelName) {
    boolean status = true;

    try {

      ProducerRecord<String, String> producerRecord =
          new ProducerRecord<>(topicName, String.valueOf(payload.hashCode()), payload);

      producerRecord
          .headers()
          .add(
              new RecordHeader(FabricClientConstants.FABRIC_TRANSACTION_ID, fabricTxId.getBytes()));
      producerRecord
          .headers()
          .add(new RecordHeader(FabricClientConstants.FABRIC_EVENT_NAME, eventName.getBytes()));
      producerRecord
          .headers()
          .add(new RecordHeader(FabricClientConstants.FABRIC_CHANNEL_NAME, channelName.getBytes()));

      producerRecord
          .headers()
          .add(
              new RecordHeader(
                  FabricClientConstants.FABRIC_EVENT_TYPE,
                  FabricClientConstants.FABRIC_EVENT_TYPE_CHAINCODE.getBytes()));

      ListenableFuture<SendResult<String, String>> future = kafkaTemplate.send(producerRecord);

      future.addCallback(
          new ListenableFutureCallback<SendResult<String, String>>() {

            @Override
            public void onSuccess(SendResult<String, String> result) {
              log.info(
                  "Sent message=["
                      + payload
                      + "] with offset=["
                      + result.getRecordMetadata().offset()
                      + "]");
            }

            @Override
            public void onFailure(Throwable ex) {
              log.error("Unable to send message=[" + payload + "] due to : " + ex.getMessage());
            }
          });

    } catch (Exception ex) {
      status = false;
      log.error("Error sending message - " + ex.getMessage());
    }

    return status;
  }

  @Override
  public boolean publishBlockEvents(
      String payload,
      String fabricTxId,
      String channelName,
      String chaincodeName,
      String functionName,
      Boolean isPrivateDataPresent) {
    boolean status = true;

    try {

      ProducerRecord<String, String> producerRecord =
          new ProducerRecord<>(topicName, String.valueOf(payload.hashCode()), payload);

      producerRecord
          .headers()
          .add(
              new RecordHeader(FabricClientConstants.FABRIC_TRANSACTION_ID, fabricTxId.getBytes()));
      producerRecord
          .headers()
          .add(
              new RecordHeader(
                  FabricClientConstants.FABRIC_CHAINCODE_NAME, chaincodeName.getBytes()));
      producerRecord
          .headers()
          .add(new RecordHeader(FabricClientConstants.FABRIC_CHANNEL_NAME, channelName.getBytes()));

      producerRecord
          .headers()
          .add(
              new RecordHeader(
                  FabricClientConstants.FABRIC_EVENT_TYPE,
                  FabricClientConstants.FABRIC_EVENT_TYPE_BLOCK.getBytes()));

      producerRecord
          .headers()
          .add(
              new RecordHeader(
                  FabricClientConstants.FABRIC_EVENT_FUNC_NAME, functionName.getBytes()));

      producerRecord
          .headers()
          .add(
              new RecordHeader(
                  FabricClientConstants.IS_PRIVATE_DATA_PRESENT,
                  isPrivateDataPresent.toString().getBytes()));

      ListenableFuture<SendResult<String, String>> future = kafkaTemplate.send(producerRecord);

      future.addCallback(
          new ListenableFutureCallback<SendResult<String, String>>() {

            @Override
            public void onSuccess(SendResult<String, String> result) {
              log.info(
                  "Sent message=["
                      + payload
                      + "] with offset=["
                      + result.getRecordMetadata().offset()
                      + "]");
            }

            @Override
            public void onFailure(Throwable ex) {
              log.error("Unable to send message=[" + payload + "] due to : " + ex.getMessage());
            }
          });

    } catch (Exception ex) {
      status = false;
      log.error("Error sending message - " + ex.getMessage());
    }

    return status;
  }

  @Override
  public boolean publishTransactionFailureEvent(
      String errorMsg,
      String networkName,
      String contractName,
      String functionName,
      String parameters) {
    boolean status = true;

    try {

      ProducerRecord<String, String> producerRecord =
          new ProducerRecord<String, String>(topicName, functionName, parameters);

      producerRecord
          .headers()
          .add(new RecordHeader(FabricClientConstants.ERROR_MSG, errorMsg.getBytes()));

      producerRecord
          .headers()
          .add(
              new RecordHeader(
                  FabricClientConstants.FABRIC_CHAINCODE_NAME, contractName.getBytes()));
      producerRecord
          .headers()
          .add(new RecordHeader(FabricClientConstants.FABRIC_CHANNEL_NAME, networkName.getBytes()));

      producerRecord
          .headers()
          .add(
              new RecordHeader(
                  FabricClientConstants.FABRIC_EVENT_TYPE,
                  FabricClientConstants.FABRIC_EVENT_TYPE_ERROR.getBytes()));

      producerRecord
          .headers()
          .add(
              new RecordHeader(
                  FabricClientConstants.FABRIC_EVENT_FUNC_NAME, functionName.getBytes()));

      ListenableFuture<SendResult<String, String>> future = kafkaTemplate.send(producerRecord);

      future.addCallback(
          new ListenableFutureCallback<SendResult<String, String>>() {

            @Override
            public void onSuccess(SendResult<String, String> result) {
              log.info(
                  "Sent message=["
                      + parameters
                      + "] with offset=["
                      + result.getRecordMetadata().offset()
                      + "]");
            }

            @Override
            public void onFailure(Throwable ex) {
              log.error("Unable to send message=[" + parameters + "] due to : " + ex.getMessage());
            }
          });

    } catch (Exception ex) {
      status = false;
      log.error("Error sending message - " + ex.getMessage());
    }

    return status;
  }
}
