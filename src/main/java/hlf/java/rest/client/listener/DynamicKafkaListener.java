package hlf.java.rest.client.listener;

import hlf.java.rest.client.config.KafkaConsumerConfig;
import hlf.java.rest.client.config.KafkaProperties;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.Acknowledgment;

/*
 * This class is the configuration class for dynamically creating consumers to receiving the blockchain
 *  transaction from Kafka Topic and send it to Fabric channel.
 */
@Slf4j
@Configuration
@ConditionalOnProperty("kafka.integration-points[0].brokerHost")
@RefreshScope
public class DynamicKafkaListener {

  @Autowired KafkaProperties.ConsumerProperties consumerProperties;

  @Autowired KafkaConsumerConfig kafkaConsumerConfig;

  @Autowired TransactionConsumer transactionConsumer;

  @EventListener
  public void handleEvent(ContextRefreshedEvent event) {
    List<KafkaProperties.Consumer> consumerList = consumerProperties.getIntegrationPoints();
    consumerList.forEach(consumer -> generateAndStartConsumerGroup(consumer));
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
    log.debug(
        "Created kafka message listener container"
            + container.metrics().keySet().iterator().next());
  }
}
