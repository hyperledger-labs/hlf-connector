package hlf.java.rest.client.service;

import hlf.java.rest.client.model.ChannelOperationRequest;
import hlf.java.rest.client.model.ClientResponseModel;
import java.util.Set;

public interface ChannelService {

  /**
   * create new channel
   *
   * @param channelOperationRequest request that includes channel name, org msp, peer, and orderer
   *     information
   * @return channel operation response
   */
  ClientResponseModel createChannel(ChannelOperationRequest channelOperationRequest);

  /**
   * join peer to the channel
   *
   * @param channelOperationRequest request that includes channel name, org msp, peer, and orderer
   *     information
   * @return channel operation response
   */
  ClientResponseModel joinChannel(ChannelOperationRequest channelOperationRequest);

  /**
   * get channel members mspid
   *
   * @param channelName
   * @return
   */
  Set<String> getChannelMembersMSPID(String channelName);
}
