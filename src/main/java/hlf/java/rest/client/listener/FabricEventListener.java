package hlf.java.rest.client.listener;

import static hlf.java.rest.client.util.FabricClientConstants.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import hlf.java.rest.client.config.ApplicationProperties;
import hlf.java.rest.client.model.EventStructure;
import hlf.java.rest.client.model.EventType;
import hlf.java.rest.client.service.EventPublishService;
import hlf.java.rest.client.service.impl.GatewayBuilderSingleton;
import hlf.java.rest.client.util.FabricClientConstants;
import hlf.java.rest.client.util.FabricEventParseUtil;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.BlockEvent.TransactionEvent;
import org.hyperledger.fabric.sdk.BlockListener;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "fabric.events", name = "enable", havingValue = "true")
public class FabricEventListener {

  @Autowired ApplicationProperties applicationProperties;

  @Autowired GatewayBuilderSingleton gatewayBuilderSingleton;

  @Autowired EventPublishService eventPublishServiceImpl;

  @Value("${fabric.events.chaincode:}")
  private String chaincodeChannelNames;

  @Value("${fabric.events.block:}")
  private String blockChannelNames;

  private static String eventTxId = FabricClientConstants.FABRIC_TRANSACTION_ID;

  private static String blockTxId = FabricClientConstants.FABRIC_TRANSACTION_ID;

  private String setChaincodeEventListener(Channel channel) throws InvalidArgumentException {

    return channel.registerChaincodeEventListener(
        Pattern.compile(".*"),
        Pattern.compile(".*"),
        (handle, blockEvent, chaincodeEvent) -> {
          String es =
              blockEvent.getPeer() != null ? blockEvent.getPeer().getName() : "peer was null!!!";

          synchronized (this) {
            if (!chaincodeEvent.getTxId().equalsIgnoreCase(FabricEventListener.eventTxId)) {

              log.info("Chaincode ID: " + chaincodeEvent.getChaincodeId());
              log.info("Event Name: " + chaincodeEvent.getEventName());
              log.info("Transaction ID: " + chaincodeEvent.getTxId());
              log.info("Payload: " + new String(chaincodeEvent.getPayload()));
              log.info("Event Source: " + es);
              log.info("Channel Name: " + channel.getName());
              eventPublishServiceImpl.publishChaincodeEvents(
                  createEventStructure(
                      new String(chaincodeEvent.getPayload()),
                      chaincodeEvent.getTxId(),
                      blockEvent.getBlockNumber(),
                      EventType.CHAINCODE_EVENT),
                  chaincodeEvent.getTxId(),
                  chaincodeEvent.getEventName(),
                  channel.getName());
              FabricEventListener.eventTxId = chaincodeEvent.getTxId();
            } else {
              log.debug("Duplicate Transaction; ID: " + chaincodeEvent.getTxId());
            }
          }
        });
  }

  private String setBlockEventListener(Channel channel) throws InvalidArgumentException {

    BlockListener blockListener =
        new BlockListener() {
          @SneakyThrows
          @Override
          public void received(BlockEvent blockEvent) {
            if (blockEvent.getTransactionEvents().iterator().hasNext()) {
              TransactionEvent transactionEvent =
                  blockEvent.getTransactionEvents().iterator().next();
              synchronized (this) {
                if (!transactionEvent
                    .getTransactionID()
                    .equalsIgnoreCase(FabricEventListener.blockTxId)) {
                  log.info("Channel ID: " + transactionEvent.getChannelId());
                  log.info("Envelop Type: " + transactionEvent.getType().toString());
                  log.info("Transaction ID: " + transactionEvent.getTransactionID());
                  log.info("Is Valid:" + transactionEvent.isValid());
                  log.info("Block Number :" + blockEvent.getBlockNumber());
                  log.info(
                      "Chaincode Name: "
                          + transactionEvent.getTransactionActionInfo(0).getChaincodeIDName());
                  log.info(
                      "Function Name:"
                          + new String(
                              transactionEvent
                                  .getTransactionActionInfo(0)
                                  .getChaincodeInputArgs(0)),
                      StandardCharsets.UTF_8);
                  String payload = FabricEventParseUtil.getWriteInfoFromBlock(transactionEvent);
                  log.info("Block Data: " + payload);

                  eventPublishServiceImpl.publishBlockEvents(
                      createEventStructure(
                          payload,
                          transactionEvent.getTransactionID(),
                          blockEvent.getBlockNumber(),
                          EventType.BLOCK_EVENT),
                      transactionEvent.getTransactionID(),
                      transactionEvent.getChannelId(),
                      transactionEvent.getTransactionActionInfo(0).getChaincodeIDName(),
                      new String(
                          transactionEvent.getTransactionActionInfo(0).getChaincodeInputArgs(0),
                          StandardCharsets.UTF_8));
                  FabricEventListener.blockTxId = transactionEvent.getTransactionID();
                }
              }
            }
          }
        };

    return channel.registerBlockListener(blockListener);
  }

  private String createEventStructure(
      String data, String transactionId, Long blockNumber, EventType eventType) {
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
          FabricEventParseUtil.stringify(EventStructure.builder().data(data).eventURI(uri).build());
    } catch (JsonProcessingException e) {
      log.error("Error in transforming the event into Json format for Kafka ", e);
    }
    return message;
  }

  @Bean
  public void startEventListener() {

    try {

      Gateway gateway = gatewayBuilderSingleton.getGatewayConnection();

      if (null != chaincodeChannelNames && !chaincodeChannelNames.isEmpty()) {
        String[] listChaincodeChannelNames = chaincodeChannelNames.split(",");

        for (String channelName : listChaincodeChannelNames) {
          Network network = gateway.getNetwork(channelName);

          if (null != network) {
            log.info("Creating event-listener for channel:  " + network);
            Channel channel = network.getChannel();
            channel.initialize();
            setChaincodeEventListener(channel);
          }
        }
      }

      if (null != blockChannelNames && !blockChannelNames.isEmpty()) {
        String[] listBlockChannelNames = blockChannelNames.split(",");
        for (String channelName : listBlockChannelNames) {
          Network network = gateway.getNetwork(channelName);

          if (null != network) {
            log.info("Creating block-listener for channel:  " + network);
            Channel channel = network.getChannel();
            channel.initialize();
            setBlockEventListener(channel);
          }
        }
      }

    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
