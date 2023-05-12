package hlf.java.rest.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MultiDataTransactionPayload extends ValidatedDataModel implements Serializable {
  private List<String> peerNames;
  private List<PrivateTransactionPayload> privatePayload;
  private List<String> publicPayload;
}
