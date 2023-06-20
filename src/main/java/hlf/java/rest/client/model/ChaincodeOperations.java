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
  // lombok generates a method by the name `IsWithCollectionConfig()`
  private boolean isWithCollectionConfig;

  public String getChaincodeName() {
    return chaincodeName;
  }

  public void setChaincodeName(String chaincodeName) {
    this.chaincodeName = chaincodeName;
  }

  public String getChaincodeVersion() {
    return chaincodeVersion;
  }

  public void setChaincodeVersion(String chaincodeVersion) {
    this.chaincodeVersion = chaincodeVersion;
  }

  public Long getSequence() {
    return sequence;
  }

  public void setSequence(Long sequence) {
    this.sequence = sequence;
  }

  public Boolean getInitRequired() {
    return initRequired;
  }

  public void setInitRequired(Boolean initRequired) {
    this.initRequired = initRequired;
  }

  public String getChaincodePackageID() {
    return chaincodePackageID;
  }

  public void setChaincodePackageID(String chaincodePackageID) {
    this.chaincodePackageID = chaincodePackageID;
  }

  public Set<String> getPeerNames() {
    return peerNames;
  }

  public void setPeerNames(Set<String> peerNames) {
    this.peerNames = peerNames;
  }

  public boolean isWithCollectionConfig() {
    return isWithCollectionConfig;
  }

  public void setWithCollectionConfig(boolean withCollectionConfig) {
    isWithCollectionConfig = withCollectionConfig;
  }
}
