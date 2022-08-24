package hlf.java.rest.client.model;

import java.util.List;
import lombok.Data;

@Data
public class MSPDTO {

  private List<String> rootCerts;
  private List<String> tlsRootCerts;
  private String adminOUCert;
  private String clientOUCert;
  private String ordererOUCert;
  private String peerOUCert;
}
