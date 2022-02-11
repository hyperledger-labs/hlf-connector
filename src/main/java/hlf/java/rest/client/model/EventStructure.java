package hlf.java.rest.client.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventStructure {
  private String data;
  private String privateData;
  private String error;
  private String eventURI;
}
