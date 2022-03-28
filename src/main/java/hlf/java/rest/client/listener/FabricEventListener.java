package hlf.java.rest.client.listener;

import hlf.java.rest.client.config.FabricProperties;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Peer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "fabric.events", name = "enable", havingValue = "true")
public class FabricEventListener {

  @Autowired private FabricProperties fabricProperties;

  @Autowired private BlockEventListener blockEventListener;

  @Autowired private Gateway gateway;

  @Autowired private HFClient hfClient;

  @Autowired private ChaincodeEventListener chaincodeEventService;

  @PostConstruct
  public void startEventListener() {

    try {
      List<String> blockChannelNames = fabricProperties.getEvents().getBlock();
      if (!CollectionUtils.isEmpty(blockChannelNames)) {

        for (String channelName : blockChannelNames) {
          Network network = gateway.getNetwork(channelName);

          if (null != network) {
            log.info("Creating block-listener for channel: {}", network);
            Channel channel = network.getChannel();
            channel.initialize();
            Channel newChannel = hfClient.deSerializeChannel(channel.serializeChannel());
            log.info("Channel {} is initialized {}", channelName, newChannel.isInitialized());
            for (Peer peer : channel.getPeers()) {
              Channel.PeerOptions options = channel.getPeersOptions(peer);
              options.registerEventsForPrivateData();
              Peer newPeer = hfClient.newPeer(peer.getName(), peer.getUrl(), peer.getProperties());
              newChannel.addPeer(newPeer, options);
            }
            newChannel.initialize();
            newChannel.registerBlockListener(this.blockEventListener);
          }
        }
      }

      List<String> chaincodeChannelNames = fabricProperties.getEvents().getChaincode();
      if (!CollectionUtils.isEmpty(chaincodeChannelNames)) {

        for (String channelName : chaincodeChannelNames) {
          Network network = gateway.getNetwork(channelName);

          if (null != network) {
            log.info("Creating event-listener for channel: {}", network);
            Channel channel = network.getChannel();
            channel.initialize();
            channel.registerChaincodeEventListener(
                Pattern.compile(".*"),
                Pattern.compile(".*"),
                (handle, blockEvent, chaincodeEvent) ->
                    chaincodeEventService.listener(
                        handle, blockEvent, chaincodeEvent, channel.getName()));
          }
        }
      }

    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
