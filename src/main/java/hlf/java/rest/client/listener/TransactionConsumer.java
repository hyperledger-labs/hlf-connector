package hlf.java.rest.client.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import hlf.java.rest.client.exception.ErrorCode;
import hlf.java.rest.client.exception.FabricTransactionException;
import hlf.java.rest.client.exception.ServiceException;
import hlf.java.rest.client.exception.UnrecognizedTransactionPayloadException;
import hlf.java.rest.client.metrics.EmitCustomTransactionListenerMetrics;
import hlf.java.rest.client.model.MultiDataTransactionPayload;
import hlf.java.rest.client.service.TransactionFulfillment;
import hlf.java.rest.client.util.FabricClientConstants;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/*
 * This class has the consumer logic for processing and adding transaction to fabric
 */
@Slf4j
@Controller
public class TransactionConsumer {

  private static final String PAYLOAD_KIND = "payload_kind";
  private static final String PL_KIND_MULTI_DATA = "multi_data";

  @Autowired private TransactionFulfillment transactionFulfillment;
  @Autowired private ObjectMapper objectMapper;

  /**
   * This method routes the kafka messages to appropriate methods and acknowledges once processing
   * is complete
   *
   * @param message ConsumerRecord payload from upstream system
   */
  @EmitCustomTransactionListenerMetrics
  public void listen(ConsumerRecord<String, String> message) {
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
    String payloadKind = "";

    try {
      if (!message.value().isEmpty()) {
        transactionParams = message.value();
      }

      for (Header msgHeader : kafkaHeaders) {
        log.info(
            "Header-Key : "
                + msgHeader.key()
                + " Header-Value: "
                + new String(msgHeader.value(), StandardCharsets.UTF_8));
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
          case PAYLOAD_KIND:
            payloadKind = new String(msgHeader.value(), StandardCharsets.UTF_8);
            break;
          default:
            break;
        }
      }

      if (isIdentifiableFunction(networkName, contractName, transactionFunctionName)
          && payloadKind.equals(PL_KIND_MULTI_DATA)) {

        MultiDataTransactionPayload multiDataTransactionPayload;

        try {
          multiDataTransactionPayload =
              objectMapper.readValue(transactionParams, MultiDataTransactionPayload.class);
        } catch (Exception e) {
          throw new UnrecognizedTransactionPayloadException(
              ErrorCode.VALIDATION_FAILED, "Invalid transaction payload provided");
        }

        transactionFulfillment.writeMultiDataTransactionToLedger(
            networkName, contractName, transactionFunctionName, multiDataTransactionPayload);

        return;
      }

      if (isIdentifiableFunction(networkName, contractName, transactionFunctionName)
          && !transactionParams.isEmpty()) {

        if (!peerNames.isEmpty()) {
          List<String> lstPeerNames = Arrays.asList(peerNames.split(","));
          if (!lstPeerNames.isEmpty()) {
            if (StringUtils.isNotBlank(collections) && StringUtils.isNotBlank(transientKey)) {
              transactionFulfillment.writePrivateTransactionToLedger(
                  networkName,
                  contractName,
                  transactionFunctionName,
                  collections,
                  transientKey,
                  Optional.ofNullable(lstPeerNames),
                  transactionParams);
            } else {
              transactionFulfillment.writeTransactionToLedger(
                  networkName,
                  contractName,
                  transactionFunctionName,
                  Optional.ofNullable(lstPeerNames),
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
                networkName,
                contractName,
                transactionFunctionName,
                Optional.empty(),
                transactionParams);
          }
        }

      } else {
        log.error("Incorrect Transaction Payload");
        throw new UnrecognizedTransactionPayloadException(
            ErrorCode.VALIDATION_FAILED,
            "Inbound transaction format is incorrect or doesn't contain valid parameters.");
      }

    } catch (FabricTransactionException fte) {
      log.error("Error in Submitting Transaction - Exception - " + fte.getMessage());
      /*
       If the error handler has dead letter publish enabled, the errored Record header will be enriched by extracting
       the error cause and message from the thrown exception.
      */
      throw fte;
    } catch (Exception ex) {
      log.error("Error in Kafka Listener - Message Format exception - " + ex.getMessage());
      throw new ServiceException(ErrorCode.HYPERLEDGER_FABRIC_TRANSACTION_ERROR, ex.getMessage());
    }
  }

  private boolean isIdentifiableFunction(
      String networkName, String contractName, String transactionFunctionName) {
    return !networkName.isEmpty() && !contractName.isEmpty() && !transactionFunctionName.isEmpty();
  }
}
