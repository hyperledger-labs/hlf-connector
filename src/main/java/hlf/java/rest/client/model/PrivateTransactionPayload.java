package hlf.java.rest.client.model;

import lombok.Data;

@Data
public class PrivateTransactionPayload {
  private String key;
  private String collectionName;
  private String data;
}
