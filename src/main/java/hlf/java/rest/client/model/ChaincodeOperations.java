package hlf.java.rest.client.model;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class ChaincodeOperations {
  private String chaincodeName;
  private String chaincodeVersion;
  private Long sequence;
  private Boolean initRequired;
  private String chaincodePackageID;
  private Set<String> peerNames;
}
