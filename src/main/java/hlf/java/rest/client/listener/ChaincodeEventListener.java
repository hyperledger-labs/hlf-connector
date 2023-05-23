package hlf.java.rest.client.listener;

import hlf.java.rest.client.model.EventType;
import hlf.java.rest.client.service.EventPublishService;
import hlf.java.rest.client.util.FabricClientConstants;
import hlf.java.rest.client.util.FabricEventParseUtil;
import lombok.extern.slf4j.Slf4j;
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

  private static String eventTxnId = FabricClientConstants.FABRIC_TRANSACTION_ID;

  public void listener(
      String handle, BlockInfo blockEvent, ChaincodeEvent chaincodeEvent, String channelName) {

    synchronized (this) {
      if (!chaincodeEvent.getTxId().equalsIgnoreCase(eventTxnId)) {

        log.info("Chaincode ID: {}", chaincodeEvent.getChaincodeId());
        log.info("Event Name: {}", chaincodeEvent.getEventName());
        log.info("Transaction ID: {}", chaincodeEvent.getTxId());
        log.info("Payload: {}", new String(chaincodeEvent.getPayload()));
        log.info("Channel Name: {}", channelName);

        if (eventPublishService == null) {
          log.info("Event Publish is disabled, skipping this Chaincode event");
          return;
        }

        eventPublishService.publishChaincodeEvents(
            FabricEventParseUtil.createEventStructure(
                new String(chaincodeEvent.getPayload()),
                "",
                chaincodeEvent.getTxId(),
                blockEvent.getBlockNumber(),
                EventType.CHAINCODE_EVENT),
            chaincodeEvent.getChaincodeId(),
            chaincodeEvent.getTxId(),
            chaincodeEvent.getEventName(),
            channelName);
        eventTxnId = chaincodeEvent.getTxId();
      } else {
        log.debug("Duplicate Transaction; ID: {}", chaincodeEvent.getTxId());
      }
    }
  }
}
