package hlf.java.rest.client.listener;

import hlf.java.rest.client.config.KafkaConsumerConfig;
import hlf.java.rest.client.config.KafkaProperties;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.util.CollectionUtils;

/*
 * This class is the configuration class for dynamically creating consumers to receiving the blockchain
 *  transaction from Kafka Topic and send it to Fabric channel.
 */
@Slf4j
@Configuration
@ConditionalOnProperty("kafka.integration-points[0].brokerHost")
public class DynamicKafkaListener {

  private List<ConcurrentMessageListenerContainer> existingContainers = new ArrayList<>();

  @Autowired KafkaProperties kafkaProperties;

  @Autowired KafkaConsumerConfig kafkaConsumerConfig;

  @Autowired TransactionConsumer transactionConsumer;

  @EventListener
  public void handleEvent(ContextRefreshedEvent event) {
    log.info("Initializing Kafka Consumers..");
    registerKafkaConsumersInternal();
  }

  @EventListener(RefreshScopeRefreshedEvent.class)
  public void onRefresh(RefreshScopeRefreshedEvent event) {
    log.info("Refreshing Kafka Consumers..");

    if (!CollectionUtils.isEmpty(existingContainers)) {
      log.info("Destroying stale containers..");
      existingContainers.forEach(ConcurrentMessageListenerContainer::destroy);

      existingContainers.clear();
    }

    registerKafkaConsumersInternal();
  }

  private void registerKafkaConsumersInternal() {
    log.info("Initializing/Refreshing Kafka Consumers..");

    List<KafkaProperties.Consumer> consumerList = kafkaProperties.getIntegrationPoints();
    consumerList.forEach(this::generateAndStartConsumerGroup);
  }

  public void generateAndStartConsumerGroup(KafkaProperties.Consumer consumer) {

    DefaultKafkaConsumerFactory<String, String> factory =
        kafkaConsumerConfig.consumerFactory(consumer);
    ContainerProperties containerProperties = new ContainerProperties(consumer.getTopic());
    containerProperties.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

    containerProperties.setMessageListener(
        new AcknowledgingMessageListener<String, String>() {
          @Override
          public void onMessage(
              ConsumerRecord<String, String> message, Acknowledgment acknowledgment) {
            transactionConsumer.listen(message, acknowledgment);
          }
        });

    ConcurrentMessageListenerContainer container =
        new ConcurrentMessageListenerContainer<>(factory, containerProperties);

    container.start();
    existingContainers.add(container);
    log.debug(
        "Created kafka message listener container"
            + container.metrics().keySet().iterator().next());
  }
}
