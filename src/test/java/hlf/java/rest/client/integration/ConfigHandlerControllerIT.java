package hlf.java.rest.client.integration;

import hlf.java.rest.client.config.FabricProperties;
import hlf.java.rest.client.config.KafkaProperties;
import hlf.java.rest.client.config.KafkaProducerConfig;
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

import java.io.File;
import java.io.IOException;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ConfigHandlerControllerIT {
  @LocalServerPort
  private int randomServerPort;
  @Autowired private FabricProperties fabricProperties;
  @Autowired private KafkaProperties kafkaProperties;
  @Autowired private KafkaProperties.ConsumerProperties consumerProperties;
  @Autowired private Wallet wallet;
  @Autowired private KafkaProducerConfig kafkaProducerConfig;

  @BeforeAll
  static void setup() throws IOException {
    System.setProperty("spring.config.location", "src/test/resources/application.yml");
    FileUtils.copyFile(new File("src/test/resources/application.yml"), new File("src/test/resources/integration/application.yml"));
  }

  @Test
  void uploadConfigFile() throws Exception {
    String applicationYMLFile = "src/test/resources/integration/sample-application.yml";
    RestTemplate restTemplate = new RestTemplate();
    String baseUrl = "http://localhost:" + this.randomServerPort + "/configuration/update";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
    headers.set("api-key", "ePVYHwAaQ0V1XOTX6U");
    HttpEntity<byte[]> requestEntity
        = new HttpEntity<>(FileUtils.readFileToByteArray(new File(applicationYMLFile)), headers);
    ResponseEntity<String> response = restTemplate.postForEntity(baseUrl, requestEntity, String.class);
    triggerActuatorRefresh();
    Assertions.assertEquals(TestConfiguration.FABRIC_PROPERTIES_CLIENT, fabricProperties.getClient().toString());
    Assertions.assertEquals(TestConfiguration.FABRIC_PROPERTIES_EVENTS, fabricProperties.getEvents().toString());
    Assertions.assertEquals(TestConfiguration.KAFKA_PROPERTIES_PRODUCER, kafkaProperties.getEventListener().toString());
    Assertions.assertEquals(TestConfiguration.KAFKA_CONSUMER_PROPERTIES, consumerProperties.getIntegrationPoints().toString());
  }

  void triggerActuatorRefresh(){
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
    FileUtils.moveFile(new File("src/test/resources/integration/application.yml"), new File("src/test/resources/application.yml"));
  }

  private static class TestConfiguration {
    static String FABRIC_PROPERTIES_CLIENT = "FabricProperties.Client(rest=FabricProperties.Client.Rest(apikey=expected-key))";
    static String FABRIC_PROPERTIES_EVENTS = "FabricProperties.Events(enable=true, chaincode=[chaincode12, chaincode2], block=[block111, block2])";
    static String KAFKA_PROPERTIES_PRODUCER = "KafkaProperties.Producer(brokerHost=localhost:90931, topic=hlf-offchain-topic1, saslJaasConfig=null)";
    static String KAFKA_CONSUMER_PROPERTIES = "[KafkaProperties.Consumer(brokerHost=localhost:90931, groupId=fabric-consumer1, topic=hlf-integration-topic11, saslJaasConfig=null), KafkaProperties.Consumer(brokerHost=localhost:90941, groupId=fabric-consumer1, topic=hlf-integration-topic21, saslJaasConfig=null)]";
  }
}
