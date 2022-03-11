package hlf.java.rest.client.model;

import lombok.Data;

@Data
public class ChaincodeOperations {
  private String chaincodeName;
  private String chaincodeVersion;
  private Long sequence;
  private Boolean initRequired;
  private String chaincodePackageID;
}
