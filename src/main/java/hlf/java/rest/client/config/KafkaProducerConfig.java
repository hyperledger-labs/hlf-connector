package hlf.java.rest.client.config;

import hlf.java.rest.client.exception.ErrorCode;
import hlf.java.rest.client.exception.ServiceException;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.MicrometerProducerListener;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.core.RoutingKafkaTemplate;

/** This class is the configuration class for sending to Chaincode event to eventHub/Kafka Topic. */
@Slf4j
@Configuration
@ConditionalOnProperty("kafka.event-listeners[0].brokerHost")
@RefreshScope
public class KafkaProducerConfig extends BaseKafkaConfig {

  private static final int RETRIES_CONFIG_FOR_AT_MOST_ONCE = 0;

  @Autowired private KafkaProperties kafkaProperties;

  @Autowired private MeterRegistry meterRegistry;

  public ProducerFactory<Object, Object> eventProducerFactory(
      KafkaProperties.Producer kafkaProducerProperties) {
    Map<String, Object> props = new HashMap<>();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProducerProperties.getBrokerHost());
    props.put(
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
        org.apache.kafka.common.serialization.StringSerializer.class);
    props.put(
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
        org.apache.kafka.common.serialization.StringSerializer.class);
    props.put(
        ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, kafkaProducerProperties.getEnableIdempotence());

    if (kafkaProducerProperties.getEnableAtMostOnceSemantics()) {
      // at-most once requires retries to be set as zero since the client wouldn't re-attempt a
      // publish in case of Broker failure.
      props.put(ProducerConfig.RETRIES_CONFIG, RETRIES_CONFIG_FOR_AT_MOST_ONCE);
      props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, false);

      log.info("Kafka producer will be initialised with at-most once behaviour");
    }

    // Azure event-hub config
    configureSaslProperties(props, kafkaProducerProperties.getSaslJaasConfig());

    // Adding SSL configuration if Kafka Cluster is SSL secured
    configureSSLProperties(
        props, kafkaProducerProperties, kafkaProducerProperties.getTopic(), meterRegistry);

    log.info("Generating Kafka producer factory..");

    DefaultKafkaProducerFactory<Object, Object> defaultKafkaProducerFactory =
        new DefaultKafkaProducerFactory<>(props);
    defaultKafkaProducerFactory.addListener(new MicrometerProducerListener<>(meterRegistry));

    return defaultKafkaProducerFactory;
  }

  @Bean
  @RefreshScope
  public RoutingKafkaTemplate routingTemplate(GenericApplicationContext context) {

    Set<String> topicSet = new HashSet<>();
    Map<Pattern, ProducerFactory<Object, Object>> map = new LinkedHashMap<>();
    for (KafkaProperties.EventProducer eventProducer : kafkaProperties.getEventListeners()) {
      if (topicSet.contains(eventProducer.getTopic())) {
        throw new ServiceException(ErrorCode.NOT_SUPPORTED, "Topic name should be unique");
      }
      topicSet.add(eventProducer.getTopic());
      ProducerFactory<Object, Object> defaultKafkaProducerFactory =
          eventProducerFactory(eventProducer);
      context.registerBean(
          eventProducer.getTopic() + "PF",
          ProducerFactory.class,
          () -> defaultKafkaProducerFactory);
      map.put(Pattern.compile(eventProducer.getTopic()), defaultKafkaProducerFactory);
    }
    return new RoutingKafkaTemplate(map);
  }

  @Override
  protected ConfigType getConfigType() {
    return ConfigType.PRODUCER;
  }
}
