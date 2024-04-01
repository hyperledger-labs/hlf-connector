package hlf.java.rest.client.IT;

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
import org.apache.commons.lang3.StringUtils;
import org.awaitility.Awaitility;
import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.ContractException;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.Transaction;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The 'default-consumer' profile spins up a single container listener and a publisher with a
 * concurrency limit of '1'. This is the default configuration for the Connector unless the
 * partition-count and parallel-listener capabilities are adjusted accordingly.
 */
@Slf4j
@DirtiesContext
@SpringBootTest
@EmbeddedKafka
@ActiveProfiles("default-consumer")
public class SingleThreadedListenerIntegrationIT extends KafkaBaseIT {

  private static final CountDownLatch LATCH = new CountDownLatch(1);

  @Autowired private DynamicKafkaListener dynamicKafkaListener;
  @Autowired private TransactionFulfillment transactionFulfillment;
  @Autowired private KafkaProperties kafkaProperties;
  @Autowired private MeterRegistry meterRegistry;

  /**
   * This is a simple test to validate a scenario where a single, valid incoming transaction was
   * polled and dispatched to {@link hlf.java.rest.client.service.impl.TransactionFulfillmentImpl}
   * for processing. After successful processing, the record offset is committed.
   */
  @Test
  public void testHappyPathTxConsumption() throws Exception {

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
    publishValidTransactionToInboundTopic(
        DEFAULT_CHANNEL_NAME, DEFAULT_CONTRACT_NAME, DEFAULT_FUNCTION_NAME);

    Awaitility.await()
        .pollDelay(1, TimeUnit.SECONDS)
        .untilAsserted(() -> Assertions.assertTrue(true));

    // Then
    long endingOffset =
        getCurrentCommittedMessageCountForInboundTopic(configuredConsumerProp.getGroupId());

    // Exactly one message was polled and processed
    Assertions.assertEquals(1, (endingOffset - startingOffset));

    // There were no retries. Therefore, Transaction invocation was performed only once.
    verify(mockTransaction, times(1)).submit(Mockito.any());
  }

  /**
   * Test to validate a scenario where a list of valid incoming transaction was polled and
   * dispatched to {@link hlf.java.rest.client.service.impl.TransactionFulfillmentImpl} for
   * processing. After successful processing, all the offsets are committed.
   */
  @Test
  public void testHappyPathMultipleTxConsumption() throws Exception {

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

    Awaitility.await()
        .pollDelay(2, TimeUnit.SECONDS)
        .untilAsserted(() -> Assertions.assertTrue(true));

    // Then
    long endingOffset =
        getCurrentCommittedMessageCountForInboundTopic(configuredConsumerProp.getGroupId());

    // Verify that all the published messages were polled and processed
    Assertions.assertEquals(50, (endingOffset - startingOffset));

    // There were no retries. Therefore, Transaction invocation was performed for all inbound
    // messages.
    verify(mockTransaction, times(50)).submit(Mockito.any());
  }

  /**
   * This test validates a scenario where the inbound message does not conform to the expected
   * transaction payload format. It checks whether the transaction is rejected by tracking it via
   * the metrics registry. Due to the non-transient nature of the error, it also verifies that the
   * transaction is not retried. *
   */
  @Test
  public void testConsumerBehaviourOnInvalidTxPayload() throws Exception {

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

    // Passing 'functionName' as null since we want this payload be invalid.
    publishValidTransactionToInboundTopic(
        DEFAULT_CHANNEL_NAME, DEFAULT_CONTRACT_NAME, StringUtils.EMPTY);

    Awaitility.await()
        .pollDelay(5, TimeUnit.SECONDS)
        .untilAsserted(() -> Assertions.assertTrue(true));

    // Then
    long endingOffset =
        getCurrentCommittedMessageCountForInboundTopic(configuredConsumerProp.getGroupId());

    // Exactly one message was polled and processed
    Assertions.assertEquals(1, (endingOffset - startingOffset));

    /*
     Invalid Transaction Payload errors are tracked via Metrics. Validate whether the metrics are captured.
     Since Invalid Payload errors are non-transient in nature, it won't be retried.
    */
    Counter invalidTxMessagesCounter =
        meterRegistry.find("transaction.messages.unrecognized.failures").counter();
    Assertions.assertNotNull(invalidTxMessagesCounter);
    // This check is important!
    Assertions.assertEquals(1, invalidTxMessagesCounter.count());

    // Since the payload was invalid, transactionFulfillment service won't be invoked
    verify(mockTransaction, times(0)).submit(Mockito.any());
  }

  /**
   * This test validates the Listener's retry capability. It ensures that a transaction which
   * encounters a transient exception is retried for the configured number of attempts and verifies
   * that metrics are tracked. *
   */
  @Test
  public void testRetryBehaviourOnNetworkTransientErrors() throws Exception {

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
    // At the time of writing this test, retry count is statically configured as 5 for Retryable
    // exceptions
    // Expected invocations will then be 6
    verify(mockTransaction, times(6)).submit(Mockito.any());

    // After retries, the offset is committed
    long endingOffset =
        getCurrentCommittedMessageCountForInboundTopic(configuredConsumerProp.getGroupId());
    Assertions.assertEquals(1, (endingOffset - startingOffset));
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
