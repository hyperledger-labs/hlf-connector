package hlf.java.rest.client.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.protobuf.InvalidProtocolBufferException;
import hlf.java.rest.client.config.FabricProperties;
import hlf.java.rest.client.exception.ErrorCode;
import hlf.java.rest.client.exception.ErrorConstants;
import hlf.java.rest.client.exception.NotFoundException;
import hlf.java.rest.client.exception.ServiceException;
import hlf.java.rest.client.listener.BlockEventListener;
import hlf.java.rest.client.listener.ChaincodeEventListener;
import hlf.java.rest.client.model.ClientResponseModel;
import hlf.java.rest.client.model.EventType;
import hlf.java.rest.client.service.EventFulfillment;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.sdk.BlockInfo;
import org.hyperledger.fabric.sdk.ChaincodeEvent;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@ConditionalOnProperty(prefix = "fabric.events", name = "enable", havingValue = "true")
@RefreshScope
public class EventFulfillmentImpl implements EventFulfillment {

  @Autowired private FabricProperties fabricProperties;

  @Autowired private Gateway gateway;

  @Autowired private BlockEventListener blockEventListener;

  @Autowired private ChaincodeEventListener chaincodeEventListener;

  /**
   * This function replays all the events from blockchain It returns the type of event requested
   * i.e. BLOCK_EVENT and CHAINCODE_EVENT
   *
   * @param startBlockNumber block number since when the events are requested
   * @param endBlockNumber block number until when to replay events
   * @param networkName channel name
   * @param eventType type of event
   * @return the ClientResponseModel with the status that request is honored.
   */
  @Override
  public ResponseEntity<ClientResponseModel> replayEvents(
      Long startBlockNumber,
      Long endBlockNumber,
      String transactionId,
      String networkName,
      String eventType) {
    log.info(
        "Initiate the replay of events since {} until {} on channel {} and type {}",
        startBlockNumber,
        endBlockNumber,
        networkName,
        eventType);

    if (Objects.isNull(fabricProperties.getEvents())) {
      throw new NotFoundException(
          ErrorCode.NO_EVENTS_FOUND, "Events API not enabled in the configuration.");
    }

    try {
      Network network = gateway.getNetwork(networkName);
      Channel channel = network.getChannel();
      for (Long blockNumber = startBlockNumber; blockNumber <= endBlockNumber; blockNumber++) {
        BlockInfo blockInfo = channel.queryBlockByNumber(blockNumber);
        // subscribed to the block event
        if (eventType.equals(EventType.BLOCK_EVENT.toString())) {
          // process the block info
          blockEventListener.receivedBlockInfo(blockInfo);
        } else if (eventType.equals(EventType.CHAINCODE_EVENT.toString())) {
          Iterable<BlockInfo.EnvelopeInfo> envelopeInfos = blockInfo.getEnvelopeInfos();
          for (BlockInfo.EnvelopeInfo info : envelopeInfos) {
            BlockInfo.TransactionEnvelopeInfo envelopeInfo =
                (BlockInfo.TransactionEnvelopeInfo) info;
            envelopeInfo
                .getTransactionActionInfos()
                .forEach(
                    transactionActionInfo -> {
                      ChaincodeEvent chaincodeEvent = transactionActionInfo.getEvent();

                      if (Objects.isNull(transactionId)
                          || chaincodeEvent.getTxId().equals(transactionId)) {
                        chaincodeEventListener.listener(
                            StringUtils.EMPTY,
                            blockInfo,
                            chaincodeEvent,
                            networkName,
                            info.isValid());
                      } else {
                        log.info(
                            "Event TransactionID {} does not match the provided TransactionID filter {}. Skipping event.",
                            chaincodeEvent.getTxId(),
                            transactionId);
                      }
                    });
          }
        }
      }
    } catch (InvalidArgumentException e) {
      log.error(
          "Action Failed: A problem occurred while parsing the block data with InvalidArgumentException.",
          e);
      throw new ServiceException(ErrorCode.HYPERLEDGER_FABRIC_CHANNEL_TXN_ERROR, e.getMessage(), e);
    } catch (ProposalException | InvalidProtocolBufferException | JsonProcessingException e) {
      log.error("Action Failed: A problem occurred while fetching transaction by block number", e);
      throw new ServiceException(ErrorCode.HYPERLEDGER_FABRIC_CHANNEL_TXN_ERROR, e.getMessage(), e);
    }
    return new ResponseEntity<>(
        new ClientResponseModel(ErrorConstants.NO_ERROR, StringUtils.EMPTY), HttpStatus.ACCEPTED);
  }
}
