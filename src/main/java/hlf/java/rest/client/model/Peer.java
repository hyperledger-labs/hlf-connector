package hlf.java.rest.client.model;

import lombok.Data;

@Data
public class Peer {

  private String name;

  private String grpcUrl;

  private String certificate;
}
