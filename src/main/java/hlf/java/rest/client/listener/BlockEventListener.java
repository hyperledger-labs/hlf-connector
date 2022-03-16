package hlf.java.rest.client.listener;

import hlf.java.rest.client.model.EventType;
import hlf.java.rest.client.service.EventPublishService;
import hlf.java.rest.client.util.FabricClientConstants;
import hlf.java.rest.client.util.FabricEventParseUtil;
import java.nio.charset.StandardCharsets;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.BlockListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "fabric.events", name = "enable", havingValue = "true")
public class BlockEventListener implements BlockListener {

  @Autowired(required = false)
  EventPublishService eventPublishServiceImpl;

  private static String blockTxId = FabricClientConstants.FABRIC_TRANSACTION_ID;

  @SneakyThrows
  @Override
  public void received(BlockEvent blockEvent) {
    if (blockEvent.getTransactionEvents().iterator().hasNext()) {
      BlockEvent.TransactionEvent transactionEvent =
          blockEvent.getTransactionEvents().iterator().next();
      synchronized (this) {
        if (!transactionEvent.getTransactionID().equalsIgnoreCase(BlockEventListener.blockTxId)) {
          log.info("Channel ID: {}", transactionEvent.getChannelId());
          log.info("Envelop Type: {}", transactionEvent.getType().toString());
          log.info("Transaction ID: {}", transactionEvent.getTransactionID());
          log.info("Is Valid: {}", transactionEvent.isValid());
          log.info("Block Number: {}", blockEvent.getBlockNumber());
          log.info(
              "Chaincode Name: {}",
              transactionEvent.getTransactionActionInfo(0).getChaincodeIDName());
          log.info(
              "Function Name: {}",
              new String(
                  transactionEvent.getTransactionActionInfo(0).getChaincodeInputArgs(0),
                  StandardCharsets.UTF_8));
          String privateDataPayload =
              FabricEventParseUtil.getPrivateDataFromBlock(blockEvent.getBlockAndPrivateData());
          log.info("Private Data: {}", privateDataPayload);
          String blockPayload = FabricEventParseUtil.getWriteInfoFromBlock(transactionEvent);
          log.info("Block Data: {}", blockPayload);

          eventPublishServiceImpl.publishBlockEvents(
              FabricEventParseUtil.createEventStructure(
                  blockPayload,
                  privateDataPayload,
                  transactionEvent.getTransactionID(),
                  blockEvent.getBlockNumber(),
                  EventType.BLOCK_EVENT),
              transactionEvent.getTransactionID(),
              transactionEvent.getChannelId(),
              transactionEvent.getTransactionActionInfo(0).getChaincodeIDName(),
              new String(
                  transactionEvent.getTransactionActionInfo(0).getChaincodeInputArgs(0),
                  StandardCharsets.UTF_8),
              StringUtils.isNotEmpty(privateDataPayload));
          BlockEventListener.blockTxId = transactionEvent.getTransactionID();
        }
      }
    }
  }
}
