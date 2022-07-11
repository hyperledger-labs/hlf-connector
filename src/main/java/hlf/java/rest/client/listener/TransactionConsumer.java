package hlf.java.rest.client.listener;

import hlf.java.rest.client.exception.FabricTransactionException;
import hlf.java.rest.client.service.EventPublishService;
import hlf.java.rest.client.service.TransactionFulfillment;
import hlf.java.rest.client.util.FabricClientConstants;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;

/*
 * This class has the consumer logic for processing and adding transaction to fabric
 */
@Slf4j
@Controller
public class TransactionConsumer {

  @Autowired TransactionFulfillment transactionFulfillment;

  @Autowired(required = false)
  EventPublishService eventPublishServiceImpl;

  /**
   * This method routes the kafka messages to appropriate methods and acknowledges once processing
   * is complete
   *
   * @param message ConsumerRecord payload from upstream system
   * @param acknowledgment Acknowledgment manual commit offset
   */
  public void listen(ConsumerRecord<String, String> message, Acknowledgment acknowledgment) {
    log.info(
        "Incoming Message details : Topic : "
            + message.topic()
            + ", partition : "
            + message.partition()
            + " , offset : "
            + message.offset()
            + " , message :"
            + message.value());

    Header[] kafkaHeaders = message.headers().toArray();

    String networkName = "";
    String contractName = "";
    String transactionFunctionName = "";
    String transactionParams = "";
    String peerNames = "";
    String transientKey = "";
    String collections = "";

    try {
      if (!message.value().isEmpty()) {
        transactionParams = message.value();
      }

      for (Header msgHeader : kafkaHeaders) {
        switch (msgHeader.key()) {
          case FabricClientConstants.CHANNEL_NAME:
            networkName = new String(msgHeader.value(), StandardCharsets.UTF_8);
            break;
          case FabricClientConstants.CHAINCODE_NAME:
            contractName = new String(msgHeader.value(), StandardCharsets.UTF_8);
            break;
          case FabricClientConstants.FUNCTION_NAME:
            transactionFunctionName = new String(msgHeader.value(), StandardCharsets.UTF_8);
            break;
          case FabricClientConstants.PEER_LIST:
            peerNames = new String(msgHeader.value(), StandardCharsets.UTF_8);
            break;
          case FabricClientConstants.FABRIC_TRANSIENT_KEY:
            transientKey = new String(msgHeader.value(), StandardCharsets.UTF_8);
            break;
          case FabricClientConstants.FABRIC_COLLECTION_NAME:
            collections = new String(msgHeader.value(), StandardCharsets.UTF_8);
            break;
          default:
            break;
        }
      }
      log.info(
          "Invoking channel: {}, chaincode: {}, function: {}, peerNames: {}, transientKey: {}, collections: {}",
          networkName,
          contractName,
          transactionFunctionName,
          peerNames,
          transientKey,
          collections);

      if (!networkName.isEmpty()
          && !contractName.isEmpty()
          && !transactionFunctionName.isEmpty()
          && !transactionParams.isEmpty()) {

        if (StringUtils.isNotBlank(peerNames)) {
          List<String> lstPeerNames = Arrays.asList(peerNames.split(","));
          if (!CollectionUtils.isEmpty(lstPeerNames)) {
            if (StringUtils.isNotBlank(collections) && StringUtils.isNotBlank(transientKey)) {
              transactionFulfillment.writePrivateTransactionToLedger(
                  networkName,
                  contractName,
                  transactionFunctionName,
                  collections,
                  transientKey,
                  lstPeerNames,
                  transactionParams);
            } else {
              transactionFulfillment.writeTransactionToLedger(
                  networkName,
                  contractName,
                  transactionFunctionName,
                  lstPeerNames,
                  transactionParams);
            }
          }
        } else {
          if (StringUtils.isNotBlank(collections) && StringUtils.isNotBlank(transientKey)) {
            transactionFulfillment.writePrivateTransactionToLedger(
                networkName,
                contractName,
                transactionFunctionName,
                collections,
                transientKey,
                transactionParams);
          } else {
            transactionFulfillment.writeTransactionToLedger(
                networkName, contractName, transactionFunctionName, transactionParams);
          }
        }

      } else {
        log.info("Incorrect Transaction Payload");
      }
      acknowledgment.acknowledge();

    } catch (FabricTransactionException fte) {
      acknowledgment.acknowledge();
      eventPublishServiceImpl.publishTransactionFailureEvent(
          fte.getMessage(), networkName, contractName, transactionFunctionName, transactionParams);
      log.error("Error in Submitting Transaction - Exception - ", fte);
    } catch (Exception ex) {
      log.error("Error in Kafka Listener - Message Format exception - ", ex);
    }
  }
}
