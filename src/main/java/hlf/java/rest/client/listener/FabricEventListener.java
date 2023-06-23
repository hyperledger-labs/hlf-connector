package hlf.java.rest.client.listener;

import hlf.java.rest.client.config.FabricProperties;
import hlf.java.rest.client.service.HFClientWrapper;
import java.util.List;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Peer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.util.CollectionUtils;

@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "fabric.events", name = "enable", havingValue = "true")
@RefreshScope
public class FabricEventListener {

  @Autowired private FabricProperties fabricProperties;

  @Autowired private BlockEventListener blockEventListener;

  @Autowired private Gateway gateway;

  @Autowired private HFClientWrapper hfClientWrapper;

  @Autowired private ChaincodeEventListener chaincodeEventService;

  @EventListener
  public void handleEvent(ContextRefreshedEvent event) {
    log.info("Initializing Chaincode/Block Event Listeners..");
    startEventListener();
  }

  @EventListener(RefreshScopeRefreshedEvent.class)
  public void onRefresh(RefreshScopeRefreshedEvent event) {
    log.info("Initializing Chaincode/Block Event Listeners..");
    startEventListener();
  }

  public void startEventListener() {

    try {
      List<String> blockChannelNames = fabricProperties.getEvents().getBlock();
      if (!CollectionUtils.isEmpty(blockChannelNames)) {

        for (String channelName : blockChannelNames) {
          log.info("channel names {}", channelName);
          Network network = gateway.getNetwork(channelName);

          if (null != network) {
            log.info("Creating block-listener for channel: {}", network);
            Channel channel = network.getChannel();
            channel.initialize();
            Channel newChannel =
                hfClientWrapper.getHfClient().deSerializeChannel(channel.serializeChannel());
            log.info("Channel {} is initialized {}", channelName, newChannel.isInitialized());
            for (Peer peer : channel.getPeers()) {
              Channel.PeerOptions options = channel.getPeersOptions(peer);
              options.registerEventsForPrivateData();
              Peer newPeer =
                  hfClientWrapper
                      .getHfClient()
                      .newPeer(peer.getName(), peer.getUrl(), peer.getProperties());
              newChannel.addPeer(newPeer, options);
            }
            newChannel.initialize();
            newChannel.registerBlockListener(this.blockEventListener);
          }
        }
      }

      List<FabricProperties.ChaincodeDetails> chaincodeDetails =
          fabricProperties.getEvents().getChaincodeDetails();
      List<String> chaincodeChannelNames = fabricProperties.getEvents().getChaincode();

      /**
       * In-order to ensure backward compatiability, registering event-listeners through
       * 'chaincodeChannelNames' is preserved until this Listener service fully moves to utilising
       * Chaincode & Channel names provided via the 'chaincodeDetails' property. Until that,
       * registering events via Channel names provided through 'chaincodeChannelNames' will be used
       * if 'chaincodeDetails' is empty. If 'chaincodeDetails' is a non-empty list, then preference
       * will be given to register Event-listener via the 'Contract' object and registering events
       * through 'chaincodeChannelNames' will be skipped regardless whether it's populated or not.
       *
       * <p>P.S it is recommended to use 'Contract' object for registering Event-Listeners over
       * registering it through 'Channel' Object.
       */
      if (!CollectionUtils.isEmpty(chaincodeDetails)) {

        for (FabricProperties.ChaincodeDetails chaincodeDetail : chaincodeDetails) {
          Network network = gateway.getNetwork(chaincodeDetail.getChannelName());
          Contract contract = network.getContract(chaincodeDetail.getChaincodeId());

          contract.addContractListener(chaincodeEventService::chaincodeEventListener);
        }
      } else if (!CollectionUtils.isEmpty(chaincodeChannelNames)) {

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
      log.error("Failed to register Block/Chaincode listener with error {}, ", ex.getMessage(), ex);
    }
  }
}
