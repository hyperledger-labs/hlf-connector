package hlf.java.rest.client.sdk;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * StandardCCEvent can be used by smart contract developers to send a commonly wrapped event that
 * the hlf-connector decodes. The decoded event can be used to publish to Kafka.
 */
@Data
public class StandardCCEvent implements Serializable {
  @JsonProperty("key")
  private String key;

  @JsonProperty("event")
  private String event;
}
