package hlf.java.rest.client.config;

import hlf.java.rest.client.exception.FabricTransactionException;
import hlf.java.rest.client.exception.RetryableServiceException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ConsumerAwareRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@ConditionalOnProperty("kafka.integration-points[0].brokerHost")
@RefreshScope
@Slf4j
public class KafkaConsumerErrorHandler {

  private static final long RETRY_INTERVAL_IN_MS = 1000L;
  private static final int MAX_RETRY_ATTEMPTS = 5;
  private static final List<Class<? extends Exception>> connectorRetryableExceptions =
      Arrays.asList(RetryableServiceException.class, FabricTransactionException.class);

  @Autowired private KafkaProperties kafkaProperties;

  @Autowired private KafkaProducerConfig kafkaProducerConfig;

  @Bean
  public CommonErrorHandler topicTransactionErrorHandler() {

    ConsumerAwareRecordRecoverer deadLetterPublishingRecoverer = null;

    /**
     * Check if the runtime config has a valid Failed Message Listener. If a dedicated Failed
     * message listener is available, then the dead letter configuration would push the failed
     * message to the exclusive dead letter topic. If a dedicated failed message listener is not
     * configured, check if the event publisher topic can be utilised for publishing the failed
     * message.
     */
    if (Objects.nonNull(kafkaProperties.getFailedMessageListener())
        && StringUtils.isNotBlank(kafkaProperties.getFailedMessageListener().getTopic())) {

      deadLetterPublishingRecoverer =
          generateRecordRecovererWithPublisher(kafkaProperties.getFailedMessageListener());
    } else if (Objects.nonNull(kafkaProperties.getEventListener())
        && kafkaProperties.getEventListener().isListenToFailedMessages()) {

      deadLetterPublishingRecoverer =
          generateRecordRecovererWithPublisher(kafkaProperties.getEventListener());
    }

    /*
     If no topics are available to publish the failed message, fall-back to a 'NO-OP' record recoverer that would simply log
     the message after retry attempts
    */
    if (Objects.isNull(deadLetterPublishingRecoverer)) {
      deadLetterPublishingRecoverer =
          new ConsumerAwareRecordRecoverer() {
            @Override
            public void accept(
                ConsumerRecord<?, ?> consumerRecord, Consumer<?, ?> consumer, Exception e) {
              log.warn(
                  "Retries exhausted.. Committing offset. Dead letter record is not published since the configuration doesnt use subscription topic for publishing failed messages "
                      + "nor it has a dead letter topic configured.");
            }
          };
    }

    DefaultErrorHandler defaultErrorHandler =
        new DefaultErrorHandler(
            deadLetterPublishingRecoverer,
            new FixedBackOff(RETRY_INTERVAL_IN_MS, MAX_RETRY_ATTEMPTS));
    defaultErrorHandler.setCommitRecovered(true);

    for (Class<? extends Exception> retryableExceptionClass : connectorRetryableExceptions) {
      defaultErrorHandler.addRetryableExceptions(retryableExceptionClass);
    }

    return defaultErrorHandler;
  }

  private DeadLetterPublishingRecoverer generateRecordRecovererWithPublisher(
      KafkaProperties.Producer destination) {

    KafkaTemplate<String, String> deadLetterPublisherTemplate =
        new KafkaTemplate<>(kafkaProducerConfig.eventProducerFactory(destination));
    deadLetterPublisherTemplate.setDefaultTopic(destination.getTopic());

    return new DeadLetterPublishingRecoverer(
        deadLetterPublisherTemplate,
        (cr, e) -> new TopicPartition(destination.getTopic(), cr.partition()));
  }
}
