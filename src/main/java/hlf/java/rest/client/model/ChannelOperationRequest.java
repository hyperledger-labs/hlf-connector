package hlf.java.rest.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import lombok.Data;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.springframework.util.CollectionUtils;

@Data
public class ChannelOperationRequest {

  private static final String TRUST_CERT_BYTES = "pemBytes";

  private String channelName;

  private String consortiumName;

  private List<String> orgMsps;

  private List<Orderer> orderers;

  private List<Peer> peers;

  /**
   * get the list of peers from the request
   *
   * @param hfClient the requester sending the client object to construct the peer instance
   * @return list of fabric peers
   * @throws InvalidArgumentException
   */
  public List<org.hyperledger.fabric.sdk.Peer> getFabricPeers(HFClient hfClient)
      throws InvalidArgumentException {
    List<org.hyperledger.fabric.sdk.Peer> peers = new ArrayList<>();
    if (CollectionUtils.isEmpty(this.getPeers())) {
      return peers;
    }
    for (hlf.java.rest.client.model.Peer peerModel : this.getPeers()) {
      String peerName = peerModel.getName();
      String peerGrpcUrl = peerModel.getGrpcUrl();
      Properties properties = new Properties();
      properties.put(TRUST_CERT_BYTES, peerModel.getCertificate().getBytes());
      org.hyperledger.fabric.sdk.Peer peer = hfClient.newPeer(peerName, peerGrpcUrl, properties);
      peers.add(peer);
    }
    return peers;
  }

  /**
   * get the list of orderers from the request
   *
   * @param hfClient the requester sending the client object to construct the peer instance
   * @return list of fabric orderers
   * @throws InvalidArgumentException
   */
  public List<org.hyperledger.fabric.sdk.Orderer> getFabricOrderers(HFClient hfClient)
      throws InvalidArgumentException {
    List<org.hyperledger.fabric.sdk.Orderer> orderers = new ArrayList<>();
    if (CollectionUtils.isEmpty(this.getOrderers())) {
      return orderers;
    }
    for (hlf.java.rest.client.model.Orderer ordererModel : this.getOrderers()) {
      String ordererName = ordererModel.getName();
      String ordererGrpcUrl = ordererModel.getGrpcUrl();
      Properties properties = new Properties();
      properties.put(TRUST_CERT_BYTES, ordererModel.getCertificate().getBytes());
      org.hyperledger.fabric.sdk.Orderer orderer =
          hfClient.newOrderer(ordererName, ordererGrpcUrl, properties);
      orderers.add(orderer);
    }
    return orderers;
  }
}
