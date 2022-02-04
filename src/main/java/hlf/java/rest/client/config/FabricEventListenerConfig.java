package hlf.java.rest.client.config;

import static hlf.java.rest.client.util.FabricClientConstants.URI_PATH_BLOCKS;
import static hlf.java.rest.client.util.FabricClientConstants.URI_PATH_TRANSACTIONS;
import static hlf.java.rest.client.util.FabricClientConstants.URI_QUERY_PARAM_EVENTS;
import static hlf.java.rest.client.util.FabricClientConstants.URI_QUERY_PARAM_EVENT_TYPE;

import com.fasterxml.jackson.core.JsonProcessingException;
import hlf.java.rest.client.listener.BlockEventListener;
import hlf.java.rest.client.model.EventStructure;
import hlf.java.rest.client.model.EventType;
import hlf.java.rest.client.service.EventPublishService;
import hlf.java.rest.client.util.FabricClientConstants;
import hlf.java.rest.client.util.FabricEventParseUtil;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.impl.identity.X509IdentityProvider;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "fabric.events", name = "enable", havingValue = "true")
public class FabricEventListenerConfig {

  @Autowired ApplicationProperties applicationProperties;

  @Autowired GatewayConfig gatewayConfiguration;

  @Autowired EventPublishService eventPublishServiceImpl;

  @Autowired BlockEventListener blockEventListener;

  @Autowired Gateway gateway;

  @Autowired Wallet wallet;

  @Value("${fabric.events.chaincode:}")
  private String chaincodeChannelNames;

  @Value("${fabric.events.block:}")
  private String blockChannelNames;

  private static String eventTxId = FabricClientConstants.FABRIC_TRANSACTION_ID;

  private String setChaincodeEventListener(Channel channel) throws InvalidArgumentException {

    return channel.registerChaincodeEventListener(
        Pattern.compile(".*"),
        Pattern.compile(".*"),
        (handle, blockEvent, chaincodeEvent) -> {
          String es =
              blockEvent.getPeer() != null ? blockEvent.getPeer().getName() : "peer was null!!!";

          synchronized (this) {
            if (!chaincodeEvent.getTxId().equalsIgnoreCase(FabricEventListenerConfig.eventTxId)) {

              log.info("Chaincode ID: {}", chaincodeEvent.getChaincodeId());
              log.info("Event Name: {}", chaincodeEvent.getEventName());
              log.info("Transaction ID: {}", chaincodeEvent.getTxId());
              log.info("Payload: {}", new String(chaincodeEvent.getPayload()));
              log.info("Event Source: {}", es);
              log.info("Channel Name: {}", channel.getName());
              eventPublishServiceImpl.publishChaincodeEvents(
                  createEventStructure(
                      new String(chaincodeEvent.getPayload()),
                      "",
                      chaincodeEvent.getTxId(),
                      blockEvent.getBlockNumber(),
                      EventType.CHAINCODE_EVENT),
                  chaincodeEvent.getTxId(),
                  chaincodeEvent.getEventName(),
                  channel.getName());
              FabricEventListenerConfig.eventTxId = chaincodeEvent.getTxId();
            } else {
              log.debug("Duplicate Transaction; ID: {}", chaincodeEvent.getTxId());
            }
          }
        });
  }

  public static String createEventStructure(
      String data,
      String privateDataPayload,
      String transactionId,
      Long blockNumber,
      EventType eventType) {
    String uri =
        UriComponentsBuilder.fromUriString(
                URI_PATH_BLOCKS
                    + blockNumber
                    + URI_PATH_TRANSACTIONS
                    + transactionId
                    + URI_QUERY_PARAM_EVENTS)
            .queryParam(URI_QUERY_PARAM_EVENT_TYPE, eventType.toString())
            .build()
            .toUriString();
    String message = null;
    try {
      message =
          FabricEventParseUtil.stringify(
              EventStructure.builder()
                  .data(data)
                  .privateData(privateDataPayload)
                  .eventURI(uri)
                  .build());
    } catch (JsonProcessingException e) {
      log.error("Error in transforming the event into Json format for Kafka ", e);
    }
    return message;
  }

  @Bean
  public void startEventListener() {

    try {
      if (null != blockChannelNames && !blockChannelNames.isEmpty()) {
        String[] listBlockChannelNames = blockChannelNames.split(",");
        for (String channelName : listBlockChannelNames) {
          Network network = gateway.getNetwork(channelName);

          if (null != network) {
            log.info("Creating block-listener for channel: {}", network);
            Channel channel = network.getChannel();
            channel.initialize();
            HFClient hfClient = HFClient.createNewInstance();
            hfClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
            X509IdentityProvider.INSTANCE.setUserContext(
                hfClient, gateway.getIdentity(), "hlf-rest-client");
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

      if (null != chaincodeChannelNames && !chaincodeChannelNames.isEmpty()) {
        String[] listChaincodeChannelNames = chaincodeChannelNames.split(",");

        for (String channelName : listChaincodeChannelNames) {
          Network network = gateway.getNetwork(channelName);

          if (null != network) {
            log.info("Creating event-listener for channel: {}", network);
            Channel channel = network.getChannel();
            channel.initialize();
            setChaincodeEventListener(channel);
          }
        }
      }

    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
