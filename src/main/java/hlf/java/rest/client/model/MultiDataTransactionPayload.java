package hlf.java.rest.client.model;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

@Data
public class MultiDataTransactionPayload extends ValidatedDataModel implements Serializable {
  private List<String> peerNames;
  private List<PrivateTransactionPayload> privatePayload;
  private List<String> publicPayload;
}
