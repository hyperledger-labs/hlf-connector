package hlf.java.rest.client.IT;

import hlf.java.rest.client.util.FabricClientConstants;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;

public abstract class KafkaBaseIT {

  private static final String IN_MEMORY_BROKER_ADDRESS = "PLAINTEXT://localhost:9092";
  protected static final String INBOUND_TOPIC_NAME = "test-consumer-inbound-topic";
  protected static final String OUTBOUND_TOPIC_NAME = "test-publisher-event-topic";

  protected static final String OUTBOUND_DLT_NAME = "test-consumer-dlt";

  protected static final String DEFAULT_CHANNEL_NAME = "test-channel";
  protected static final String DEFAULT_CONTRACT_NAME = "test-contract";
  protected static final String DEFAULT_FUNCTION_NAME = "test-function";

  protected static final String DEFAULT_TRANSACTION_BODY = "stringified-transaction-message";

  private static EmbeddedKafkaBroker embeddedKafkaBroker;
  protected static Producer<Object, Object> testProducer;

  protected static Consumer<Object, Object> testDltConsumer;

  @BeforeAll
  public static void setUpClass() {
    startEmbeddedKafkaBroker();
  }

  @AfterAll
  public static void tearDownClass() {
    if (embeddedKafkaBroker != null) {
      embeddedKafkaBroker.destroy();
      embeddedKafkaBroker = null;
    }
  }

  private static void startEmbeddedKafkaBroker() {
    if (embeddedKafkaBroker == null) {
      embeddedKafkaBroker =
          new EmbeddedKafkaBroker(1, false, getDefaultPartitionSize(), getTopicsToBootstrap())
              .brokerProperties(getBrokerProperties())
              .kafkaPorts(9092);
      embeddedKafkaBroker.afterPropertiesSet();

      testProducer = configureProducer();
      testDltConsumer = configureDltConsumer();
    }
  }

  private static Producer<Object, Object> configureProducer() {
    Map<String, Object> producerProps =
        new HashMap<>(KafkaTestUtils.producerProps(embeddedKafkaBroker));
    return new DefaultKafkaProducerFactory<>(producerProps).createProducer();
  }

  private static Consumer<Object, Object> configureDltConsumer() {
    Map<String, Object> consumerProps =
        new HashMap<>(KafkaTestUtils.producerProps(embeddedKafkaBroker));
    consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "dlt_group");
    consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    Consumer<Object, Object> dltConsumer =
        new DefaultKafkaConsumerFactory<>(consumerProps).createConsumer();

    dltConsumer.subscribe(Collections.singleton(OUTBOUND_DLT_NAME));

    return dltConsumer;
  }

  private static Map<String, String> getBrokerProperties() {
    Map<String, String> brokerProperties = new HashMap<>();
    brokerProperties.put("listeners", IN_MEMORY_BROKER_ADDRESS);
    brokerProperties.put("port", "9092");
    return brokerProperties;
  }

  protected static String[] getTopicsToBootstrap() {
    return new String[] {INBOUND_TOPIC_NAME, OUTBOUND_TOPIC_NAME, OUTBOUND_DLT_NAME};
  }

  protected static int getDefaultPartitionSize() {
    return 12;
  }

  protected static String getBrokerAddress() {
    return IN_MEMORY_BROKER_ADDRESS;
  }

  protected void publishValidTransactionToInboundTopic(
      String channelName, String contractName, String functionName) {

    ProducerRecord<Object, Object> producerRecord =
        new ProducerRecord<Object, Object>(INBOUND_TOPIC_NAME, "stringified-transaction-message");

    producerRecord.headers().add(getHeader(FabricClientConstants.CHANNEL_NAME, channelName));
    producerRecord.headers().add(getHeader(FabricClientConstants.CHAINCODE_NAME, contractName));
    producerRecord.headers().add(getHeader(FabricClientConstants.FUNCTION_NAME, functionName));

    testProducer.send(producerRecord);
  }

  private Header getHeader(String headerName, String headerValue) {
    return new Header() {
      @Override
      public String key() {
        return headerName;
      }

      @Override
      public byte[] value() {
        return headerValue.getBytes(StandardCharsets.UTF_8);
      }
    };
  }

  protected long getCurrentCommittedMessageCountForInboundTopic(String groupId) throws Exception {

    long currentOffset = 0;

    for (int i = 0; i < getDefaultPartitionSize(); i++) {
      currentOffset +=
          KafkaTestUtils.getCurrentOffset(getBrokerAddress(), groupId, INBOUND_TOPIC_NAME, i)
              .offset();
    }

    return currentOffset;
  }
}
