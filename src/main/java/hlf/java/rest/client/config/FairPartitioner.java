package hlf.java.rest.client.config;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.PartitionInfo;

@Slf4j
public class FairPartitioner implements Partitioner {

  private final ConcurrentHashMap<String, AtomicInteger> topicCounters = new ConcurrentHashMap<>();

  @Override
  public void configure(Map<String, ?> configs) {}

  @Override
  public int partition(
      String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {

    List<PartitionInfo> availablePartitions = cluster.availablePartitionsForTopic(topic);

    for (PartitionInfo partitionInfo : availablePartitions) {
      log.info("Available partition: {} for topic: {}", partitionInfo.partition(), topic);
    }

    log.info("Total available partitions {}", availablePartitions.size());

    List<PartitionInfo> partitionsToUse =
        (availablePartitions != null && !availablePartitions.isEmpty())
            ? availablePartitions
            : cluster.partitionsForTopic(topic);

    if (partitionsToUse == null || partitionsToUse.isEmpty()) {
      throw new IllegalStateException("No partitions found for topic: " + topic);
    }

    AtomicInteger counter = topicCounters.computeIfAbsent(topic, t -> new AtomicInteger(0));

    int next = counter.getAndIncrement();
    int index = Math.floorMod(next, partitionsToUse.size());

    return partitionsToUse.get(index).partition();
  }

  @Override
  public void close() {
    topicCounters.clear();
  }
}
