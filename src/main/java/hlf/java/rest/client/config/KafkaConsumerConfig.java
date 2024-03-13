package hlf.java.rest.client.config;

import hlf.java.rest.client.util.FabricClientConstants;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SslConfigs;
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
public class KafkaConsumerConfig {

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

    if (StringUtils.isNotBlank(kafkaConsumerProperties.getOffsetResetPolicy())) {
      props.put(
          ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, kafkaConsumerProperties.getOffsetResetPolicy());
    }

    // Adding SSL configuration if Kafka Cluster is SSL secured
    if (kafkaConsumerProperties.isSslAuthRequired()) {

      SSLAuthFilesHelper.createSSLAuthFiles(kafkaConsumerProperties);

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

      try {
        Timestamp keyStoreCertExpiryTimestamp =
            SSLAuthFilesHelper.getExpiryTimestampForKeyStore(
                kafkaConsumerProperties.getSslKeystoreLocation(),
                kafkaConsumerProperties.getSslKeystorePassword());
        Timestamp trustStoreCertExpiryTimestamp =
            SSLAuthFilesHelper.getExpiryTimestampForKeyStore(
                kafkaConsumerProperties.getSslTruststoreLocation(),
                kafkaConsumerProperties.getSslTruststorePassword());

        Gauge.builder(
                "consumer." + kafkaConsumerProperties.getTopic() + ".keystore.expiryTs",
                keyStoreCertExpiryTimestamp::getTime)
            .strongReference(true)
            .register(meterRegistry);

        Gauge.builder(
                "consumer." + kafkaConsumerProperties.getTopic() + ".truststore.expiryTs",
                trustStoreCertExpiryTimestamp::getTime)
            .strongReference(true)
            .register(meterRegistry);

      } catch (Exception e) {
        log.error(
            "Failed to extract expiry details of Consumer SSL Certs. Metrics for Consumer SSL cert-expiry will not be available.");
      }
    }

    log.info("Generating Kafka consumer factory..");

    DefaultKafkaConsumerFactory<String, String> defaultKafkaConsumerFactory =
        new DefaultKafkaConsumerFactory<>(props);
    defaultKafkaConsumerFactory.addListener(new MicrometerConsumerListener<>(meterRegistry));

    return defaultKafkaConsumerFactory;
  }
}
