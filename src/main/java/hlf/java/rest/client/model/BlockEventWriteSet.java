package hlf.java.rest.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BlockEventWriteSet {
  @JsonProperty("key")
  private String key;

  @JsonProperty("value")
  private String value;

  @JsonProperty("is_delete")
  private boolean isDelete;
}
