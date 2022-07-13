package hlf.java.rest.client.service.impl;

import hlf.java.rest.client.service.HFClientWrapper;
import lombok.Getter;
import org.hyperledger.fabric.sdk.HFClient;

@Getter
public class HFClientWrapperImpl implements HFClientWrapper {
  private final HFClient hfClient;

  public HFClientWrapperImpl(HFClient hfClient) {
    this.hfClient = hfClient;
  }
}
