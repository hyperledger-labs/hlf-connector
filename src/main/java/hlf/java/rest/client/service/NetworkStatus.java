package hlf.java.rest.client.service;

import hlf.java.rest.client.model.ChannelUpdateParamsDTO;
import hlf.java.rest.client.model.ClientResponseModel;
import hlf.java.rest.client.model.CommitChannelParamsDTO;
import java.util.Set;
import org.springframework.http.ResponseEntity;

public interface NetworkStatus {

  ResponseEntity<ClientResponseModel> getChannelFromNetwork(String channelName);

  Set<String> getAnchorPeerForChannel(String channelName);

  ResponseEntity<ClientResponseModel> signChannelConfigTransaction(
      String channelName, String configUpdate);

  ResponseEntity<ClientResponseModel> generateConfigUpdate(
      String channelName, ChannelUpdateParamsDTO organizationDetails);

  ResponseEntity<ClientResponseModel> commitChannelConfigTransaction(
      String channelName, CommitChannelParamsDTO commitChannelParamsDTO);

  ResponseEntity<ClientResponseModel> addOrgToChannel(
      String channelName, ChannelUpdateParamsDTO organizationDetails);

  ResponseEntity<ClientResponseModel> addAnchorPeersToChannel(
      String channelName, ChannelUpdateParamsDTO channelUpdateParamsDTO);
}
