package hlf.java.rest.client.controller.e2e;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;

@Slf4j
public class ConsumeFromEventKafka {
  public static void main(String[] args) {

    ConsumeFromEventKafka consumeFromEventKafka = new ConsumeFromEventKafka();

    consumeFromEventKafka.showMessagesFromTopic("hlf-offchain-topic");
  }

  public void showMessagesFromTopic(String topicName) {

    Properties props = new Properties();

    props.put("bootstrap.servers", "localhost:9093");

    props.put("group.id", "analytics2125");
    props.put("auto.offset.reset", "earliest");
    props.put("enable.auto.commit", "false");
    props.put("auto.commit.interval.ms", "1000");
    props.put("session.timeout.ms", "30000");
    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

    TopicPartition topicPartition = new TopicPartition(topicName, 0);

    KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(props);
    consumer.subscribe(Arrays.asList(topicName));
    // KafkaConsumer subscribes list of topics here.
    log.info("Subscribed to topic " + topicName);

    int i = 0;

    while (i < 10) {

      ConsumerRecords<String, String> records = consumer.poll(100);

      for (ConsumerRecord<String, ?> record : records) {

        Header[] headers = record.headers().toArray();

        Charset charset = Charset.forName("UTF-8");

        /*
         * String value = new String(headers[0].value(), charset);
         *
         *
         * log.info("Key: "+headers[0].key()+"\t Value: "+value);
         */

        for (Header header : headers) {
          log.info("Keys : " + header.key());
          // log.info("Value : "+String.valueOf(header.value()));
          StringDeserializer strDeserializer = new StringDeserializer();

          log.info("Value : " + strDeserializer.deserialize(topicName, header.value()));
        }

        // print the offset,key and value for the consumer records.
        log.info("offset = " + record.offset() + " value = " + record.value().toString());
        // log.info("Header: " + record.headers().toArray().length);

        // log.info(new Timestamp(record.timestamp()).toString());

      }

      consumer.commitSync();
      // consumer.close();

    }
    consumer.close();
  }
}
