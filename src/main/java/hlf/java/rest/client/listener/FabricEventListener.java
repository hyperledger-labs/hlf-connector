package hlf.java.rest.client.listener;

import hlf.java.rest.client.config.FabricProperties;
import hlf.java.rest.client.service.HFClientWrapper;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
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
  public void handleEvent(ContextRefreshedEvent event)
      throws InvalidArgumentException, TransactionException, IOException, ClassNotFoundException {
    log.info("Initializing Chaincode/Block Event Listeners..");
    startEventListener();
  }

  @EventListener(RefreshScopeRefreshedEvent.class)
  public void onRefresh(RefreshScopeRefreshedEvent event)
      throws InvalidArgumentException, TransactionException, IOException, ClassNotFoundException {
    log.info("Initializing Chaincode/Block Event Listeners..");
    startEventListener();
  }

  private void startEventListener()
      throws InvalidArgumentException, TransactionException, IOException, ClassNotFoundException {

    try {
      List<FabricProperties.BlockDetails> blockDetailsList =
          fabricProperties.getEvents().getBlockDetails();
      if (!CollectionUtils.isEmpty(blockDetailsList)) {

        for (FabricProperties.BlockDetails blockDetails : blockDetailsList) {
          String channelName = blockDetails.getChannelName();
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

      if (!CollectionUtils.isEmpty(chaincodeDetails)) {

        for (FabricProperties.ChaincodeDetails chaincodeDetail : chaincodeDetails) {
          Network network = gateway.getNetwork(chaincodeDetail.getChannelName());
          Contract contract = network.getContract(chaincodeDetail.getChaincodeId());

          contract.addContractListener(chaincodeEventService::chaincodeEventListener);
        }
      } else if (!CollectionUtils.isEmpty(chaincodeChannelNames)) {
        throw new InvalidArgumentException("Chaincode details are missing in the configuration");
      }

    } catch (Exception ex) {
      log.error("Failed to register Block/Chaincode listener with error {}, ", ex.getMessage(), ex);
      throw ex;
    }
  }
}
