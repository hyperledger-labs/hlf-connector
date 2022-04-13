package hlf.java.rest.client.controller.e2e;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.Acknowledgment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ConsumeEventFromMultipleKafkaHosts {

  public static void main(String[] args) {
    ConsumeEventFromMultipleKafkaHosts consumeEventFromMultipleKafkaHosts = new ConsumeEventFromMultipleKafkaHosts();
    createConsumers();
  }

  private static void createConsumers(){
    List<Map<String, Object>> consumersList = new ArrayList<>();
    Map<String, Object> props = new HashMap<>();
    props.put("bootstrap.servers", "localhost:9093");
    props.put("group.id", "analytics2125");
    props.put("topic","hlf-integration-topic1");
    props.put("auto.offset.reset", "earliest");
    props.put("enable.auto.commit", "false");
    props.put("auto.commit.interval.ms", "1000");
    props.put("session.timeout.ms", "30000");
    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    consumersList.add(props);
    Map<String, Object> props1 = new HashMap<>();
    props1.put("bootstrap.servers", "localhost:9094");
    props1.put("group.id", "analytics2125");
    props1.put("topic","hlf-integration-topic2");
    props1.put("auto.offset.reset", "earliest");
    props1.put("enable.auto.commit", "false");
    props1.put("auto.commit.interval.ms", "1000");
    props1.put("session.timeout.ms", "30000");
    props1.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    props1.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    consumersList.add(props1);
    Map<String, Object> props2 = new HashMap<>();
    props2.put("bootstrap.servers", "localhost:9092");
    props2.put("group.id", "analytics2125");
    props2.put("topic","hlf-integration-topic");
    props2.put("auto.offset.reset", "earliest");
    props2.put("enable.auto.commit", "false");
    props2.put("auto.commit.interval.ms", "1000");
    props2.put("session.timeout.ms", "30000");
    props2.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    props2.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    consumersList.add(props2);
    consumersList.forEach(prop -> generateAndStartConsumerGroup(prop));
  }

  private static void generateAndStartConsumerGroup(Map<String, Object> props) {
    DefaultKafkaConsumerFactory<String, String> factory = new DefaultKafkaConsumerFactory<>(props);
    ContainerProperties containerProperties = new ContainerProperties((String)props.get("topic"));
    containerProperties.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
    containerProperties.setMessageListener(new AcknowledgingMessageListener<String, String>() {
      @Override public void onMessage(ConsumerRecord<String, String> message, Acknowledgment acknowledgment) {
        log.info(
            "Incoming Message details : Topic : "
                + message.topic()
                + ", partition : "
                + message.partition()
                + " , offset : "
                + message.offset()
                + " , message :"
                + message.value());
      }
    });
    ConcurrentMessageListenerContainer container = new ConcurrentMessageListenerContainer<>(factory,
        containerProperties);
    container.start();
    log.debug("Created kafka message listener container");
  }
}
