package hlf.java.rest.client.config;

import hlf.java.rest.client.util.FabricClientConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;

/*
 * This class is the configuration class for setting the properties for the kafka consumers.
 *
 */
@Slf4j
@Configuration
public class KafkaConsumerConfig {

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
    // Azure event-hub config
    if (StringUtils.isNotEmpty(kafkaConsumerProperties.getSaslJaasConfig())) {
      props.put(
          FabricClientConstants.KAFKA_SECURITY_PROTOCOL_KEY,
          FabricClientConstants.KAFKA_SECURITY_PROTOCOL_VALUE);
      props.put(
          FabricClientConstants.KAFKA_SASL_MECHANISM_KEY,
          FabricClientConstants.KAFKA_SASL_MECHANISM_VALUE);
      props.put(
          FabricClientConstants.KAFKA_SASL_JASS_ENDPOINT_KEY,
          kafkaConsumerProperties.getSaslJaasConfig());
    }

    // Adding SSL configuration if Kafka Cluster is SSL secured
    if (kafkaConsumerProperties.isSslEnabled()) {
      props.put(
          CommonClientConfigs.SECURITY_PROTOCOL_CONFIG,
          kafkaConsumerProperties.getSecurityProtocol());
      props.put(
          SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG,
          kafkaConsumerProperties.getSslKeystoreLocation());
      props.put(
          SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG,
          kafkaConsumerProperties.getSslKeystorePassword());
      props.put(
          SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG,
          kafkaConsumerProperties.getSslTruststoreLocation());
      props.put(
          SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG,
          kafkaConsumerProperties.getSslTruststorePassword());
      props.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, kafkaConsumerProperties.getSslKeyPassword());
    }

    log.info("Created kafka consumer factory");
    return new DefaultKafkaConsumerFactory<>(props);
  }
}
