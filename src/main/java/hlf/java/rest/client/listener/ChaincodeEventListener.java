package hlf.java.rest.client.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import hlf.java.rest.client.config.FabricProperties;
import hlf.java.rest.client.model.EventType;
import hlf.java.rest.client.sdk.StandardCCEvent;
import hlf.java.rest.client.service.EventPublishService;
import hlf.java.rest.client.service.RecencyTransactionContext;
import hlf.java.rest.client.util.FabricClientConstants;
import hlf.java.rest.client.util.FabricEventParseUtil;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.fabric.gateway.ContractEvent;
import org.hyperledger.fabric.sdk.BlockInfo;
import org.hyperledger.fabric.sdk.ChaincodeEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "fabric.events", name = "enable", havingValue = "true")
@RefreshScope
public class ChaincodeEventListener {

  @Autowired(required = false)
  private EventPublishService eventPublishService;

  @Autowired private FabricProperties fabricProperties;

  @Autowired private RecencyTransactionContext recencyTransactionContext;

  private static String eventTxnId = FabricClientConstants.FABRIC_TRANSACTION_ID;

  public void chaincodeEventListener(ContractEvent contractEvent) {

    String chaincodeId = contractEvent.getChaincodeId();
    String eventName = contractEvent.getName();
    String txId = contractEvent.getTransactionEvent().getTransactionID();
    String channelName = contractEvent.getTransactionEvent().getChannelId();
    long blockNumber = contractEvent.getTransactionEvent().getBlockEvent().getBlockNumber();
    String payload =
        contractEvent.getPayload().isPresent()
            ? new String(contractEvent.getPayload().get(), StandardCharsets.UTF_8)
            : StringUtils.EMPTY;

    if (recencyTransactionContext.validateAndRemoveTransactionContext(txId)) {
      publishChaincodeEvent(txId, chaincodeId, eventName, payload, channelName, blockNumber);
      return;
    }

    log.info(
        "TxnID {} for Block Number {} qualifies as a duplicate event.. Discarding the payload {} from being published.",
        txId,
        blockNumber,
        payload);
  }

  @Deprecated
  public void listener(
      String handle, BlockInfo blockEvent, ChaincodeEvent chaincodeEvent, String channelName) {

    long blockNumber = blockEvent.getBlockNumber();
    String txId = chaincodeEvent.getTxId();
    String chaincodeId = chaincodeEvent.getChaincodeId();
    String eventName = chaincodeEvent.getEventName();
    String payload = new String(chaincodeEvent.getPayload(), StandardCharsets.UTF_8);

    publishChaincodeEvent(txId, chaincodeId, eventName, payload, channelName, blockNumber);
  }

  private void publishChaincodeEvent(
      String txId,
      String chaincodeId,
      String eventName,
      String payload,
      String channelName,
      long blockNumber) {
    synchronized (this) {
      if (!txId.equalsIgnoreCase(eventTxnId)) {

        log.info("Chaincode ID: {}", chaincodeId);
        log.info("Event Name: {}", eventName);
        log.info("Transaction ID: {}", txId);
        log.info("Payload: {}", payload);
        log.info("Channel Name: {}", channelName);

        if (eventPublishService == null) {
          log.info("Event Publish is disabled, skipping this Chaincode event");
          return;
        }

        String messageKey = String.valueOf(payload.hashCode());
        String payloadToPublish = payload;

        if (fabricProperties.getEvents().isStandardCCEventEnabled()) {
          // Fetch the key information for chaincode events, only if the feature is enabled.
          // Parse the payload and use the key.
          try {
            StandardCCEvent standardCCEvent =
                FabricEventParseUtil.parseString(payload, StandardCCEvent.class);
            messageKey =
                StringUtils.isNotBlank(standardCCEvent.getKey())
                    ? standardCCEvent.getKey()
                    : messageKey;
            // Prefer the Raw Event Payload.
            payloadToPublish =
                StringUtils.isNotBlank(standardCCEvent.getEvent())
                    ? standardCCEvent.getEvent()
                    : payloadToPublish;
          } catch (JsonProcessingException e) {
            // Likely thrown if the Event generated from Chaincode might not be wrapped in a model
            // that matches 'StandardCCEvent'
            // Instead of failing the op, fallback to the defaults and proceed with the publish.
            log.error(
                "Failed to deserialize Event payload to StandardCCEvent structure. Incoming Event Payload and Default Key will be utilised for publishing.");
          }
        }

        eventPublishService.publishChaincodeEvents(
            FabricEventParseUtil.createEventStructure(
                payloadToPublish, "", txId, blockNumber, EventType.CHAINCODE_EVENT),
            chaincodeId,
            txId,
            eventName,
            channelName,
            messageKey);
        eventTxnId = txId;
      } else {
        log.debug("Duplicate Transaction; ID: {}", txId);
      }
    }
  }
}
