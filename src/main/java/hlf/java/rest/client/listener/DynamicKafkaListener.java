package hlf.java.rest.client.listener;

import hlf.java.rest.client.config.KafkaConsumerConfig;
import hlf.java.rest.client.config.KafkaProperties;
import hlf.java.rest.client.exception.ServiceException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.BatchAcknowledgingMessageListener;
import org.springframework.kafka.listener.BatchListenerFailedException;
import org.springframework.kafka.listener.CommonErrorHandler;
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

  private static final int MAX_CONCURRENT_LISTENERS_PER_CONSUMER = 12;

  @Getter private List<ConcurrentMessageListenerContainer> existingContainers = new ArrayList<>();

  @Autowired private KafkaProperties kafkaProperties;

  @Autowired private KafkaConsumerConfig kafkaConsumerConfig;

  @Autowired private TransactionConsumer transactionConsumer;

  @Autowired private TaskExecutor defaultTaskExecutor;

  @Autowired private CommonErrorHandler topicTransactionErrorHandler;

  private final AtomicInteger inFlightRecords = new AtomicInteger(0);

  @Value("${kafka.general.consumer-shutdown-timeout-in-sec:30}")
  private int shutdownTimeoutInSeconds;

  @EventListener(ContextClosedEvent.class)
  public void onContextClosed() {
    log.info("Application context closing, performing graceful Kafka shutdown");
    performGracefulShutdown();
  }

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

    containerProperties.setMessageListener(determineMessageListenerForTransactions(consumer));

    ConcurrentMessageListenerContainer<String, String> container =
        new ConcurrentMessageListenerContainer<>(factory, containerProperties);

    int consumerListenerConcurrency = 1; // Kafka default if no concurrency is set.

    if (consumer.getTopicPartitions() > 1) {
      consumerListenerConcurrency =
          Math.min(consumer.getTopicPartitions(), MAX_CONCURRENT_LISTENERS_PER_CONSUMER);
    }

    container.setConcurrency(consumerListenerConcurrency);
    container.setCommonErrorHandler(topicTransactionErrorHandler);

    container.start();
    existingContainers.add(container);

    log.debug(
        "Created kafka message listener container"
            + container.metrics().keySet().iterator().next());
  }

  private Object determineMessageListenerForTransactions(KafkaProperties.Consumer consumer) {

    return consumer.isEnableParallelListenerCapabilities()
        ? getMultithreadedBatchAcknowledgingMessageListener()
        : getPerRecordAcknowledgingListener();
  }

  /**
   * A Message listener, where each Consumer container would get the list of Records fetched as part
   * of poll() to process. The records are then supplied to an Async Task pool so that multiple
   * individual Records can be processed in Parallel asynchronously. In case if one of the
   * tasks/record fails with an Exception, we perform a partial Batch commit, in which the next
   * poll() from the server would contain the non-committed records of the previous Batch to
   * process.
   *
   * @return
   */
  private Object getMultithreadedBatchAcknowledgingMessageListener() {
    return new BatchAcknowledgingMessageListener<String, String>() {
      @SneakyThrows
      @Override
      public void onMessage(
          List<ConsumerRecord<String, String>> consumerRecords, Acknowledgment acknowledgment) {
        log.debug("Consumer got assigned with a Batch of size : {}", consumerRecords.size());

        // Track the number of records we're processing
        inFlightRecords.addAndGet(consumerRecords.size());

        List<CompletableFuture<Void>> transactionSubmissionTasks = new ArrayList<>();

        // Dispatch workers for asynchronously processing Individual records
        for (ConsumerRecord<String, String> message : consumerRecords) {
          transactionSubmissionTasks.add(
              CompletableFuture.runAsync(
                  () -> {
                    try {
                      transactionConsumer.listen(message);
                    } finally {
                      // No need to decrement here as we'll do it after all tasks complete or fail
                    }
                  },
                  defaultTaskExecutor));
        }

        boolean batchSuccess = true;
        int failedIndex = -1;

        for (int i = 0; i < transactionSubmissionTasks.size(); i++) {
          try {
            transactionSubmissionTasks.get(i).get();
          } catch (InterruptedException | ExecutionException e) {
            batchSuccess = false;
            failedIndex = i;

            final Throwable cause = e.getCause();

            if (cause instanceof ServiceException) {
              log.error(
                  "One of the Consumer Record in Async Batch Processor failed with message {}",
                  cause.getMessage());
            }

            if (cause instanceof InterruptedException) {
              throw e;
            }
          }
        }

        // Always decrement the counter for all records in the batch
        inFlightRecords.addAndGet(-consumerRecords.size());

        // If the entire Records were processed successfully, Ack & commit the entire Batch
        if (batchSuccess) {
          acknowledgment.acknowledge();
        } else {
          throw new BatchListenerFailedException(
              "Failed to process a Consumer Record from the Batch", failedIndex);
        }
      }
    };
  }

  private Object getPerRecordAcknowledgingListener() {
    return (AcknowledgingMessageListener<String, String>)
        (message, acknowledgment) -> {
          try {
            // Increment counter before processing
            inFlightRecords.incrementAndGet();

            transactionConsumer.listen(message);
            // Manually ack the single Record
            acknowledgment.acknowledge();
          } finally {
            // Always decrement counter, even if exception occurred
            inFlightRecords.decrementAndGet();
          }
        };
  }

  private void performGracefulShutdown() {
    log.info("Starting graceful shutdown of Kafka consumers");

    // Stop all containers from polling new messages
    if (!CollectionUtils.isEmpty(existingContainers)) {
      existingContainers.forEach(
          container -> {
            log.info("Stopping container: {}", container.metrics().keySet().iterator().next());
            container.stop();
          });
    }

    // Wait for in-flight messages to be processed
    log.info(
        "All Kafka containers stopped from polling. Waiting for {} in-flight records to be processed...",
        inFlightRecords.get());

    long startTime = System.currentTimeMillis();

    try {
      while (inFlightRecords.get() > 0
          && System.currentTimeMillis() - startTime < (shutdownTimeoutInSeconds * 1000L)) {
        log.info("Still waiting for {} records to be acknowledged", inFlightRecords.get());
        Thread.sleep(500);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Interrupted during shutdown wait", e);
    }

    if (inFlightRecords.get() > 0) {
      log.warn("{} records were not acknowledged before shutdown timeout", inFlightRecords.get());
    } else {
      log.info("All records successfully processed and acknowledged");
    }

    log.info("Kafka consumer graceful shutdown completed");
  }
}
