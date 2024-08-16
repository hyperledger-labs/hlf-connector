package hlf.java.rest.client.integration;

import hlf.java.rest.client.config.FabricProperties;
import hlf.java.rest.client.config.KafkaProperties;
import java.io.File;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.hyperledger.fabric.gateway.Wallet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ConfigHandlerControllerIntegrationTest {
  @LocalServerPort private int randomServerPort;
  @Autowired private FabricProperties fabricProperties;
  @Autowired private KafkaProperties kafkaProperties;
  @Autowired private Wallet wallet;

  @BeforeAll
  static void setup() throws IOException {
    System.setProperty("spring.config.location", "src/test/resources/application.yml");
    FileUtils.copyFile(
        new File("src/test/resources/application.yml"),
        new File("src/test/resources/integration/application.yml"));
  }

  @Test
  void testContextRefresh() throws Exception {

    String incomingApplicationYMLFile = "src/test/resources/integration/sample-application.yml";
    String existingApplicationYMLFile = "src/test/resources/application.yml";

    FileUtils.copyFile(new File(incomingApplicationYMLFile), new File(existingApplicationYMLFile));

    triggerActuatorRefresh();

    Assertions.assertEquals(
        TestConfiguration.FABRIC_PROPERTIES_CLIENT, fabricProperties.getClient().toString());
    Assertions.assertEquals(
        TestConfiguration.FABRIC_PROPERTIES_EVENTS, fabricProperties.getEvents().toString());
    Assertions.assertEquals(
        TestConfiguration.KAFKA_PROPERTIES_PRODUCER,
        kafkaProperties.getEventListeners().get(0).toString());
    Assertions.assertEquals(
        TestConfiguration.KAFKA_CONSUMER_PROPERTIES,
        kafkaProperties.getIntegrationPoints().toString());
  }

  void triggerActuatorRefresh() {
    RestTemplate restTemplate = new RestTemplate();
    final String baseUrl = "http://localhost:" + this.randomServerPort + "/actuator/refresh";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<>(null, headers);
    ResponseEntity<String> response = restTemplate.postForEntity(baseUrl, entity, String.class);
  }

  @AfterAll
  static void cleanUp() throws IOException {
    FileUtils.delete(new File("src/test/resources/application.yml"));
    FileUtils.moveFile(
        new File("src/test/resources/integration/application.yml"),
        new File("src/test/resources/application.yml"));
  }

  private static class TestConfiguration {
    static String FABRIC_PROPERTIES_CLIENT =
        "FabricProperties.Client(rest=FabricProperties.Client.Rest(apikey=expected-key))";
    static String FABRIC_PROPERTIES_EVENTS =
        "FabricProperties.Events(enable=true, chaincode=[chaincode12, chaincode2], standardCCEventEnabled=false, blockDetails=[FabricProperties.BlockDetails(channelName=block111, listenerTopics=[topic-1])], chaincodeDetails=null)";
    static String KAFKA_PROPERTIES_PRODUCER =
        "Producer{brokerHost='localhost:8087', topic='hlf-offchain-topic1', saslJaasConfig='null'}";
    static String KAFKA_CONSUMER_PROPERTIES =
        "[Consumer{brokerHost='localhost:8087', groupId='fabric-consumer1', topic='hlf-integration-topic11', saslJaasConfig='null'}, Consumer{brokerHost='localhost:8087', groupId='fabric-consumer1', topic='hlf-integration-topic21', saslJaasConfig='null'}]";
  }
}
