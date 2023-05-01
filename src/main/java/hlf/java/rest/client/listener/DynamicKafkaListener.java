package hlf.java.rest.client.listener;

import hlf.java.rest.client.config.KafkaConsumerConfig;
import hlf.java.rest.client.config.KafkaProperties;
import hlf.java.rest.client.exception.ServiceException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.BatchAcknowledgingMessageListener;
import org.springframework.kafka.listener.BatchListenerFailedException;
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

  private static final int MAX_CONCURRENT_LISTENERS_PER_CONSUMER = 6;

  private List<ConcurrentMessageListenerContainer> existingContainers = new ArrayList<>();

  @Autowired KafkaProperties kafkaProperties;

  @Autowired KafkaConsumerConfig kafkaConsumerConfig;

  @Autowired TransactionConsumer transactionConsumer;

  @Autowired TaskExecutor defaultTaskExecutor;

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

    if (consumer.isEnableParallelListenerCapabilities() && consumer.getTopicPartitions() > 1) {
      consumerListenerConcurrency =
          Math.min(consumer.getTopicPartitions(), MAX_CONCURRENT_LISTENERS_PER_CONSUMER);
    }

    container.setConcurrency(consumerListenerConcurrency);

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
   * individual Records can be processed in Parallel aynchronously. In case if one of the
   * tasks/record fails with an Exception, we perform a partial Batch commit, in which the next
   * poll() from the server would contain the non committed records of the previous Batch to
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

        List<CompletableFuture<Void>> transactionSubmissionTasks = new ArrayList<>();

        // Dispatch workers for asynchronously processing Individual records
        for (ConsumerRecord<String, String> message : consumerRecords) {
          transactionSubmissionTasks.add(
              CompletableFuture.runAsync(
                  () -> {
                    transactionConsumer.listen(message);
                  },
                  defaultTaskExecutor));
        }

        for (int i = 0; i < transactionSubmissionTasks.size(); i++) {
          try {
            transactionSubmissionTasks.get(i).get();
          } catch (InterruptedException | ExecutionException e) {

            final Throwable cause = e.getCause();

            if (cause instanceof ServiceException) {
              log.error(
                  "One of the Consumer Record in Async Batch Processor failed with message {}",
                  cause.getMessage());
              throw new BatchListenerFailedException(
                  "Failed to process a Consumer Record from the Batch", i);
            }

            if (cause instanceof InterruptedException) {
              throw e;
            }
          }
        }
        // If the entire Records were processed successfully, Ack & commit the entire Batch
        acknowledgment.acknowledge();
      }
    };
  }

  private Object getPerRecordAcknowledgingListener() {

    return new AcknowledgingMessageListener<String, String>() {
      @Override
      public void onMessage(ConsumerRecord<String, String> message, Acknowledgment acknowledgment) {
        transactionConsumer.listen(message);
        // Manually ack the single Record
        acknowledgment.acknowledge();
      }
    };
  }
}
