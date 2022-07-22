package hlf.java.rest.client.service;

import org.springframework.http.ResponseEntity;

import hlf.java.rest.client.model.ClientResponseModel;
import hlf.java.rest.client.model.CommitChannelParamsDTO;
import hlf.java.rest.client.model.NewOrgParamsDTO;

public interface NetworkStatus {

  ResponseEntity<ClientResponseModel> getChannelFromNetwork(String channelName);

  ResponseEntity<ClientResponseModel> signChannelConfigTransaction(String channelName, String configUpdate);
  
  ResponseEntity<ClientResponseModel> generateConfigUpdate(String channelName, NewOrgParamsDTO organizationDetails);

  ResponseEntity<ClientResponseModel> commitChannelConfigTransaction(String channelName,
      CommitChannelParamsDTO commitChannelParamsDTO);

  ResponseEntity<ClientResponseModel> addOrgToChannel(String channelName, NewOrgParamsDTO organizationDetails);
  
}
