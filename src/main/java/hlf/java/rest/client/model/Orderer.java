package hlf.java.rest.client.model;

import lombok.Data;

@Data
public class Orderer {

  private String name;
  private String mspid;
  private String grpcUrl;
  private String certificate;
}
