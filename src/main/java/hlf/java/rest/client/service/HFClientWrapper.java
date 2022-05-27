package hlf.java.rest.client.service;
import org.hyperledger.fabric.sdk.HFClient;

public interface HFClientWrapper {
  HFClient getHfClient();
}
