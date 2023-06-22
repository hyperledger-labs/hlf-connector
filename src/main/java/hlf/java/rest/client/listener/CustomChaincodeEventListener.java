package hlf.java.rest.client.listener;

import hlf.java.rest.client.model.EventType;
import hlf.java.rest.client.service.EventPublishService;
import hlf.java.rest.client.util.FabricClientConstants;
import hlf.java.rest.client.util.FabricEventParseUtil;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
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
public class CustomChaincodeEventListener {

  @Autowired(required = false)
  private EventPublishService eventPublishService;

  private static String eventTxnId = FabricClientConstants.FABRIC_TRANSACTION_ID;

  public void chaincodeEventListener(ContractEvent contractEvent) {

    String chaincodeId = contractEvent.getChaincodeId();
    String eventName = contractEvent.getName();
    String txId = contractEvent.getTransactionEvent().getTransactionID();
    String channelName = contractEvent.getTransactionEvent().getChannelId();
    long blockNumber = contractEvent.getTransactionEvent().getBlockEvent().getBlockNumber();
    // Optional Check
    String payload = new String(contractEvent.getPayload().get(), StandardCharsets.UTF_8);

    publishChaincodeEvent(txId, chaincodeId, eventName, payload, channelName, blockNumber);
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

        eventPublishService.publishChaincodeEvents(
            FabricEventParseUtil.createEventStructure(
                payload, "", txId, blockNumber, EventType.CHAINCODE_EVENT),
            chaincodeId,
            txId,
            eventName,
            channelName);
        eventTxnId = txId;
      } else {
        log.debug("Duplicate Transaction; ID: {}", txId);
      }
    }
  }
}
