package hlf.java.rest.client.model;

import lombok.Data;

@Data
public class Peer {

  private String name;
  private String mspid;
  private MSPDTO mspDTO;
  private String grpcUrl;
  private String certificate;
}
