package hlf.java.rest.client.controller.e2e;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;

@Slf4j
public class SendToIntgKafka {
  public static void main(String[] args) {

    SendToIntgKafka sendToIntgKafka = new SendToIntgKafka();
    sendToIntgKafka.putMessageOnTopic("hlf-integration-topic");
  }

  public void putMessageOnTopic(String topicName) {
    Properties props = new Properties();
    props.put("bootstrap.servers", "localhost:9093");
    props.put("acks", "all");
    props.put("retries", 0);
    props.put("batch.size", 16384);
    props.put("linger.ms", 1);
    props.put("buffer.memory", 33554432);
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    log.info("Properties added");
    System.out.println("Properties added");
    try {
      Producer<String, String> producer = new KafkaProducer<String, String>(props);
      log.info("Got the Topic");
      for (int i = 0; i < 1; i++) {
        String text =
            new String(
                Files.readAllBytes(Paths.get("./src/test/resources/car.json")),
                StandardCharsets.UTF_8);
        log.info("Sending message to Topic: " + text);
        System.out.println("Sending message to Topic: " + text);
        ProducerRecord<String, String> producerRecord =
            new ProducerRecord<String, String>(topicName, 0, String.valueOf(text.hashCode()), text);
        producerRecord.headers().add(new RecordHeader("channel-name", "supplychain".getBytes()));
        producerRecord
            .headers()
            .add(new RecordHeader("function-name", "createCarByJson".getBytes()));
        producerRecord.headers().add(new RecordHeader("chaincode-name", "fabcar".getBytes()));
        producer.send(producerRecord);
      }
      log.info("Message sent successfully");
      producer.close();

    } catch (Exception e) {
      log.error(e.getLocalizedMessage());
      e.printStackTrace();
    }
  }
}
