package hlf.java.rest.client.IT;

import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import hlf.java.rest.client.config.KafkaProperties;
import hlf.java.rest.client.listener.DynamicKafkaListener;
import hlf.java.rest.client.service.TransactionFulfillment;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.awaitility.Awaitility;
import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.ContractException;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.Transaction;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The 'per-partition-consumer' profile spins up a container listener and a publisher with a
 * concurrency limit equal to the number of Partitions or 12 (whichever is lower).
 *
 * <p>Note : This test utilises Junit's 'OutputCaptureExtension' to verify the presence of consumer
 * threads polling from a Topic.
 */
@Slf4j
@DirtiesContext
@SpringBootTest
@EmbeddedKafka
@ActiveProfiles("per-partition-consumer")
@ExtendWith(OutputCaptureExtension.class)
public class PerPartitionListenerIntegrationIT extends KafkaBaseIT {

  private static final CountDownLatch LATCH = new CountDownLatch(1);

  private String singleListenerConsumerThreadNamePattern = "consumer-{{thread-count}}-C-1";

  @Autowired private DynamicKafkaListener dynamicKafkaListener;
  @Autowired private TransactionFulfillment transactionFulfillment;
  @Autowired private KafkaProperties kafkaProperties;
  @Autowired private MeterRegistry meterRegistry;

  /**
   * Test to validate a scenario where a list of valid incoming transaction was polled and
   * dispatched to {@link hlf.java.rest.client.service.impl.TransactionFulfillmentImpl} for
   * processing. Each partition is likely to be processed by a consumer thread. After successful
   * processing, all the offsets are committed.
   */
  @Test
  public void testHappyPathMultipleTxConsumption(CapturedOutput capturedLogOutput)
      throws Exception {

    // Given
    List<ConcurrentMessageListenerContainer> existingContainers =
        dynamicKafkaListener.getExistingContainers();
    Assertions.assertEquals(
        1, existingContainers.size()); // There should only be one Container at this point.

    KafkaProperties.Consumer configuredConsumerProp = kafkaProperties.getIntegrationPoints().get(0);

    long startingOffset =
        getCurrentCommittedMessageCountForInboundTopic(configuredConsumerProp.getGroupId());

    Transaction mockTransaction = setupMockNetworkInvocation(Optional.empty());

    // When

    // Publish multiple valid Transactions for consumption
    for (int publishCount = 0; publishCount < 50; publishCount++) {
      publishValidTransactionToInboundTopic(
          DEFAULT_CHANNEL_NAME, DEFAULT_CONTRACT_NAME, DEFAULT_FUNCTION_NAME);
    }

    // Reducing the timeout since we have dedicated consumer threads per partition
    Awaitility.await()
        .pollDelay(500, TimeUnit.MILLISECONDS)
        .untilAsserted(() -> Assertions.assertTrue(true));

    // Then
    long endingOffset =
        getCurrentCommittedMessageCountForInboundTopic(configuredConsumerProp.getGroupId());

    // Verify that all the published messages were polled and processed
    Assertions.assertEquals(50, (endingOffset - startingOffset));

    // There were no retries. Therefore, Transaction invocation was performed for all inbound
    // messages.
    verify(mockTransaction, times(50)).submit(Mockito.any());

    ConcurrentMessageListenerContainer<String, String> currentContainer = existingContainers.get(0);
    int containerConcurrencyLevel = currentContainer.getConcurrency();

    for (int threadCount = 0; threadCount < containerConcurrencyLevel; threadCount++) {
      String consumerThreadId =
          singleListenerConsumerThreadNamePattern.replace(
              "{{thread-count}}", String.valueOf(threadCount));
      // Does the logs talk about consumer thread processing the messages?
      Assertions.assertTrue(capturedLogOutput.getOut().contains(consumerThreadId));
    }
  }

  @Test
  public void testDLTBehaviourForFailedTxs(CapturedOutput capturedLogOutput) throws Exception {

    // Given
    List<ConcurrentMessageListenerContainer> existingContainers =
        dynamicKafkaListener.getExistingContainers();
    Assertions.assertEquals(
        1, existingContainers.size()); // There should only be one Container at this point.

    KafkaProperties.Consumer configuredConsumerProp = kafkaProperties.getIntegrationPoints().get(0);

    long startingOffset =
        getCurrentCommittedMessageCountForInboundTopic(configuredConsumerProp.getGroupId());

    // TimeoutException is a retryable Exception
    Transaction mockTransaction = setupMockNetworkInvocation(Optional.of(new TimeoutException()));

    // When

    Counter invalidTxMessagesCounter =
        meterRegistry.find("transaction.messages.process.failures").counter();
    long initialFailures = (long) invalidTxMessagesCounter.count();

    publishValidTransactionToInboundTopic(
        DEFAULT_CHANNEL_NAME, DEFAULT_CONTRACT_NAME, DEFAULT_FUNCTION_NAME);

    Awaitility.await()
        .pollDelay(7, TimeUnit.SECONDS)
        .untilAsserted(() -> Assertions.assertTrue(true));

    // Then

    /*
     Transaction errors are tracked via Metrics. Validate whether the metrics are captured and
     since the error is transient in nature, it will be retried.
    */
    long currentFailures = (long) invalidTxMessagesCounter.count();
    Assertions.assertTrue((currentFailures - initialFailures) > 5);

    // Initial attempts + Retried attempts.
    verify(mockTransaction, atLeast(5)).submit(Mockito.any());

    // After retries, the offset is committed
    long endingOffset =
        getCurrentCommittedMessageCountForInboundTopic(configuredConsumerProp.getGroupId());
    Assertions.assertEquals(1, (endingOffset - startingOffset));

    // Fetch Latest DLT Record
    ConsumerRecord<Object, Object> dltRecord =
        KafkaTestUtils.getSingleRecord(testDltConsumer, OUTBOUND_DLT_NAME);

    Assertions.assertNotNull(dltRecord);
    Assertions.assertNotNull(dltRecord.headers());

    // Verify if the DLT Contains the original, failed Tx msg
    Assertions.assertEquals(DEFAULT_TRANSACTION_BODY, dltRecord.value());
  }

  @NotNull
  private Transaction setupMockNetworkInvocation(Optional<Exception> optionalTxSubmitException)
      throws ContractException, TimeoutException, InterruptedException {
    Gateway gatewayMock = Mockito.mock(Gateway.class);
    ReflectionTestUtils.setField(transactionFulfillment, "gateway", gatewayMock);

    Network mockNetwork = Mockito.mock(Network.class);
    Contract mockContract = Mockito.mock(Contract.class);
    Transaction mockTransaction = Mockito.mock(Transaction.class);
    Mockito.when(gatewayMock.getNetwork(DEFAULT_CHANNEL_NAME)).thenReturn(mockNetwork);
    Mockito.when(mockNetwork.getContract(DEFAULT_CONTRACT_NAME)).thenReturn(mockContract);
    Mockito.when(mockContract.createTransaction(DEFAULT_FUNCTION_NAME)).thenReturn(mockTransaction);

    if (optionalTxSubmitException.isPresent()) {
      doThrow(optionalTxSubmitException.get()).when(mockTransaction).submit(Mockito.any());
    } else {
      Mockito.when(mockTransaction.submit(Mockito.any()))
          .thenReturn(UUID.randomUUID().toString().getBytes());
    }
    return mockTransaction;
  }
}
