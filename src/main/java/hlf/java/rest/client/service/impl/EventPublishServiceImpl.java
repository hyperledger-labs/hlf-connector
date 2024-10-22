package hlf.java.rest.client.service.impl;

import hlf.java.rest.client.config.FabricProperties;
import hlf.java.rest.client.config.KafkaProperties;
import hlf.java.rest.client.service.EventPublishService;
import hlf.java.rest.client.util.FabricClientConstants;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.RoutingKafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

@Slf4j
@Service("eventPublishService")
@ConditionalOnProperty("kafka.event-listeners[0].topic")
public class EventPublishServiceImpl implements EventPublishService {

  @Autowired private KafkaProperties kafkaProperties;

  @Autowired private FabricProperties fabricProperties;

  @Autowired private RoutingKafkaTemplate routingKafkaTemplate;

  @Override
  public void publishChaincodeEvents(
      String payload,
      String chaincodeName,
      String fabricTxId,
      String eventName,
      String channelName,
      String messageKey) {

    Optional<FabricProperties.ChaincodeDetails> optionalChaincodeDetails =
        fabricProperties.getEvents().getChaincodeDetails().stream()
            .filter(
                chaincodeDetail ->
                    chaincodeDetail.getChannelName().equals(channelName)
                        && chaincodeDetail.getChaincodeId().equals(chaincodeName))
            .findAny();

    if (optionalChaincodeDetails.isPresent()
        && CollectionUtils.isEmpty(optionalChaincodeDetails.get().getListenerTopics())) {
      sendMessage(
          kafkaProperties.getEventListeners().get(0).getTopic(),
          payload,
          chaincodeName,
          fabricTxId,
          eventName,
          channelName,
          messageKey);
      return;
    }

    for (String topic : optionalChaincodeDetails.get().getListenerTopics()) {
      sendMessage(topic, payload, chaincodeName, fabricTxId, eventName, channelName, messageKey);
    }
  }

  private void sendMessage(
      String topic,
      String payload,
      String chaincodeName,
      String fabricTxId,
      String eventName,
      String channelName,
      String messageKey) {
    try {
      ProducerRecord<Object, Object> producerRecord =
          new ProducerRecord<>(topic, messageKey, payload);

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

      log.info("Publishing Chaincode event to outbound topic {}", topic);

      ListenableFuture<SendResult<Object, Object>> future =
          routingKafkaTemplate.send(producerRecord);

      future.addCallback(
          new ListenableFutureCallback<SendResult<Object, Object>>() {

            @Override
            public void onSuccess(SendResult<Object, Object> result) {
              log.info(
                  "Sent message '{}' to partition {} for offset {}",
                  payload,
                  result.getRecordMetadata().partition(),
                  result.getRecordMetadata().offset());
            }

            @Override
            public void onFailure(Throwable ex) {
              log.error(
                  "Failed to send message event for Transaction ID {} due to {}",
                  fabricTxId,
                  ex.getMessage());
            }
          });

    } catch (Exception ex) {
      log.error("Error sending message - " + ex.getMessage());
    }
  }

  @Override
  public void publishBlockEvents(
      String payload,
      String fabricTxId,
      String channelName,
      String chaincodeName,
      String functionName,
      Boolean isPrivateDataPresent) {

    Optional<FabricProperties.BlockDetails> optionalBlockDetails =
        fabricProperties.getEvents().getBlockDetails().stream()
            .filter(blockDetails -> blockDetails.getChannelName().equals(channelName))
            .findAny();

    if (optionalBlockDetails.isPresent()
        && CollectionUtils.isEmpty(optionalBlockDetails.get().getListenerTopics())) {
      sendMessage(
          kafkaProperties.getEventListeners().get(0).getTopic(),
          payload,
          fabricTxId,
          channelName,
          chaincodeName,
          functionName,
          isPrivateDataPresent);
      return;
    }

    for (String topic : optionalBlockDetails.get().getListenerTopics()) {
      sendMessage(
          topic,
          payload,
          fabricTxId,
          channelName,
          chaincodeName,
          functionName,
          isPrivateDataPresent);
    }
  }

  private void sendMessage(
      String topic,
      String payload,
      String fabricTxId,
      String channelName,
      String chaincodeName,
      String functionName,
      Boolean isPrivateDataPresent) {
    try {

      ProducerRecord<Object, Object> producerRecord =
          new ProducerRecord<>(topic, String.valueOf(payload.hashCode()), payload);

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

      log.info("Publishing Block event to outbound topic {}", topic);

      ListenableFuture<SendResult<Object, Object>> future =
          routingKafkaTemplate.send(producerRecord);

      future.addCallback(
          new ListenableFutureCallback<SendResult<Object, Object>>() {

            @Override
            public void onSuccess(SendResult<Object, Object> result) {
              log.info(
                  "Sent message '{}' to partition {} for offset {}",
                  payload,
                  result.getRecordMetadata().partition(),
                  result.getRecordMetadata().offset());
            }

            @Override
            public void onFailure(Throwable ex) {
              log.error(
                  "Failed to send message event for Transaction ID {} due to {}",
                  fabricTxId,
                  ex.getMessage());
            }
          });

    } catch (Exception ex) {
      log.error("Error sending message - " + ex.getMessage());
    }
  }
}
