package hlf.java.rest.client.model;

import java.util.List;
import lombok.Data;

@Data
public class CommitChannelParamsDTO {

  private List<byte[]> signatures;
  private String configUpdateBase64Encoded;
}
