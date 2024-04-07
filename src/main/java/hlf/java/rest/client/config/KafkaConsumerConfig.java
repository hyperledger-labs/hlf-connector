package hlf.java.rest.client.config;

import hlf.java.rest.client.util.FabricClientConstants;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.MicrometerConsumerListener;

/*
 * This class is the configuration class for setting the properties for the kafka consumers.
 *
 */
@Slf4j
@Configuration
@ConditionalOnProperty("kafka.integration-points[0].brokerHost")
@RefreshScope
public class KafkaConsumerConfig extends BaseKafkaConfig {

  @Autowired private MeterRegistry meterRegistry;

  public DefaultKafkaConsumerFactory<String, String> consumerFactory(
      KafkaProperties.Consumer kafkaConsumerProperties) {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConsumerProperties.getBrokerHost());
    props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaConsumerProperties.getGroupId());
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    props.put(
        ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, FabricClientConstants.KAFKA_INTG_SESSION_TIMEOUT);
    props.put(
        ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG,
        FabricClientConstants.KAFKA_INTG_MAX_POLL_INTERVAL);
    props.put(
        ConsumerConfig.MAX_POLL_RECORDS_CONFIG, FabricClientConstants.KAFKA_INTG_MAX_POLL_RECORDS);

    // Distribute available partitions evenly across all consumers (or consumer threads)
    props.put(
        ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG,
        FabricClientConstants.ROUND_ROBIN_CONSUMER_PARTITION_ASSIGNEMENT_STRATEGY);

    // Azure event-hub config
    configureSaslProperties(props, kafkaConsumerProperties.getSaslJaasConfig());

    if (StringUtils.isNotBlank(kafkaConsumerProperties.getOffsetResetPolicy())) {
      props.put(
          ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, kafkaConsumerProperties.getOffsetResetPolicy());
    }

    // Adding SSL configuration if Kafka Cluster is SSL secured
    configureSSLProperties(
        props, kafkaConsumerProperties, kafkaConsumerProperties.getTopic(), meterRegistry);

    log.info("Generating Kafka consumer factory..");

    DefaultKafkaConsumerFactory<String, String> defaultKafkaConsumerFactory =
        new DefaultKafkaConsumerFactory<>(props);
    defaultKafkaConsumerFactory.addListener(new MicrometerConsumerListener<>(meterRegistry));

    return defaultKafkaConsumerFactory;
  }

  @Override
  protected ConfigType getConfigType() {
    return ConfigType.CONSUMER;
  }
}
