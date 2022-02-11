package hlf.java.rest.client.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventAPIResponseModel {
  private String data;
  private String error;
}
