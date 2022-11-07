package hlf.java.rest.client.model;

import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChaincodeOperations {
  private String chaincodeName;
  private String chaincodeVersion;
  private Long sequence;
  private Boolean initRequired;
  private String chaincodePackageID;
  private Set<String> peerNames;
  private String stringifiedPolicy;
}
