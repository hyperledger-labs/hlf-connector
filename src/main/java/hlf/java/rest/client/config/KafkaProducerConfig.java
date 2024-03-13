package hlf.java.rest.client.config;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.MicrometerProducerListener;
import org.springframework.kafka.core.ProducerFactory;

/** This class is the configuration class for sending to Chaincode event to eventHub/Kafka Topic. */
@Slf4j
@Configuration
@ConditionalOnProperty("kafka.event-listener.brokerHost")
@RefreshScope
public class KafkaProducerConfig extends BaseKafkaConfig {

  @Autowired private KafkaProperties kafkaProperties;

  @Autowired private MeterRegistry meterRegistry;

  public ProducerFactory<String, String> eventProducerFactory(
      KafkaProperties.Producer kafkaProducerProperties) {
    Map<String, Object> props = new HashMap<>();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProducerProperties.getBrokerHost());
    props.put(
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
        org.apache.kafka.common.serialization.StringSerializer.class);
    props.put(
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
        org.apache.kafka.common.serialization.StringSerializer.class);
    props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, Boolean.FALSE);

    // Azure event-hub config
    configureSaslProperties(props, kafkaProducerProperties.getSaslJaasConfig());

    // Adding SSL configuration if Kafka Cluster is SSL secured
    configureSSLProperties(
        props, kafkaProducerProperties, kafkaProducerProperties.getTopic(), meterRegistry);

    log.info("Generating Kafka producer factory..");

    DefaultKafkaProducerFactory<String, String> defaultKafkaProducerFactory =
        new DefaultKafkaProducerFactory<>(props);
    defaultKafkaProducerFactory.addListener(new MicrometerProducerListener<>(meterRegistry));

    return defaultKafkaProducerFactory;
  }

  @Bean
  @RefreshScope
  public KafkaTemplate<String, String> kafkaTemplate() {
    return new KafkaTemplate<>(eventProducerFactory(kafkaProperties.getEventListener()));
  }

  @Override
  protected ConfigType getConfigType() {
    return ConfigType.PRODUCER;
  }
}
