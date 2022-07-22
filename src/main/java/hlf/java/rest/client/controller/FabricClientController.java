package hlf.java.rest.client.controller;

import hlf.java.rest.client.model.ClientResponseModel;
import hlf.java.rest.client.model.EventAPIResponseModel;
import hlf.java.rest.client.service.TransactionFulfillment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes REST endpoints to take actions from the client on the fabric ledger
 *
 * @author c0c00ub
 */
@Slf4j
@RestController
public class FabricClientController {

  @Autowired TransactionFulfillment transactionFulfillment;

  /**
   * The REST Endpoint for writing a transaction to the ledger
   *
   * @param networkName String channel name
   * @param contractName String chaincode name
   * @param transactionFunctionName String function name in chaincode
   * @param transactionParams String[] String array for arguments to the chaincode.
   * @return responseEntity ResponseEntity Transaction Response
   */
  @GetMapping(value = "/write_transaction")
  public ResponseEntity<ClientResponseModel> postTransaction(
      @RequestParam("network_name") @Validated String networkName,
      @RequestParam("contract_name") @Validated String contractName,
      @RequestParam("transaction_function_name") @Validated String transactionFunctionName,
      @RequestParam("transaction_params") @Validated String... transactionParams) {
    log.info(
        "Initiated Transaction Write for Network Name: {}, Contract Name: {}, Transaction Function Name: {}, Transaction Parameters: {}",
        networkName,
        contractName,
        transactionFunctionName,
        transactionParams);
    return transactionFulfillment.writeTransactionToLedger(
        networkName, contractName, transactionFunctionName, Optional.empty(), transactionParams);
  }

  /**
   * The REST Endpoint for initializing a chaincode once it is committed
   * to a specific sequence number.
   *
   * @param channelName String channel name
   * @param chaincodeName String chaincode name
   * @param functionName String function name in CC
   * @param peerNames String endorsing peers
   * @param payload JSON String for arguments to the chaincode.
   * @return responseEntity ResponseEntity Transaction Response
   */
  @PostMapping(value = "/init_transaction")
  public ResponseEntity<ClientResponseModel> initTransaction(
      @RequestHeader("channel") @Validated String channelName,
      @RequestHeader("chaincode") @Validated String chaincodeName,
      @RequestHeader("function") @Validated String functionName,
      @RequestHeader(value = "peers", required = false) String peerNames,
      @RequestBody @Validated String payload) {
    log.info(
        "Smart Contract init request Network Name: {}, Contract Name: {}, Function Name: {}, Endorsing Peers: {}, Transaction Parameters: {}",
        functionName,
        channelName,
        chaincodeName,
        peerNames,
        payload);
    List<String> lstPeerNames = new ArrayList<>();
    if (null != peerNames) {
      lstPeerNames = Arrays.asList(peerNames.split(","));
    }
    return transactionFulfillment.initSmartContract(
        channelName, chaincodeName, functionName, Optional.of(lstPeerNames), payload);
  }

  /**
   * The REST Endpoint for writing a transaction to the ledger
   *
   * @param channelName String channel name
   * @param chaincodeName String chaincode name
   * @param functionName String function name in chaincode
   * @param peerNames String endorsing peers
   * @param collections String collection name for PDC transactions
   * @param transientKey String transient key name to be added to the Transient Map
   * @param payload JSON String for arguments to the chaincode.
   * @return responseEntity ResponseEntity Transaction Response
   */
  @PostMapping(value = "/invoke_transaction")
  public ResponseEntity<ClientResponseModel> invokeTransaction(
      @RequestHeader("channel") @Validated String channelName,
      @RequestHeader("chaincode") @Validated String chaincodeName,
      @RequestHeader("function") @Validated String functionName,
      @RequestHeader(value = "peers", required = false) String peerNames,
      @RequestHeader(value = "collection", required = false) String collections,
      @RequestHeader(value = "transientKey", required = false) String transientKey,
      @RequestBody @Validated String payload) {
    log.info(
        "Initiated Transaction Write for Network Name: {}, Contract Name: {}, Transaction Function Name: {}, Endorsing Peers: {},Transaction Parameters: {}",
        channelName,
        chaincodeName,
        functionName,
        peerNames,
        payload);
    if (null != peerNames && !peerNames.isEmpty()) {
      List<String> lstPeerNames = Arrays.asList(peerNames.split(","));
      if (!lstPeerNames.isEmpty()) {
        if (null != collections
            && !collections.isEmpty()
            && null != transientKey
            && !transientKey.isEmpty()) {
          return transactionFulfillment.writePrivateTransactionToLedger(
              channelName,
              chaincodeName,
              functionName,
              collections,
              transientKey,
              Optional.of(lstPeerNames),
              payload);
        } else {
          return transactionFulfillment.writeTransactionToLedger(
              channelName, chaincodeName, functionName, Optional.of(lstPeerNames), payload);
        }
      } else {
        if (StringUtils.isNotBlank(collections) && StringUtils.isNotBlank(transientKey)) {

          return transactionFulfillment.writePrivateTransactionToLedger(
              channelName, chaincodeName, functionName, collections, transientKey, payload);
        } else {
          log.warn("Incorrect Peer name format");
          return transactionFulfillment.writeTransactionToLedger(
              channelName, chaincodeName, functionName, Optional.empty(), payload);
        }
      }
    } else {
      if (StringUtils.isNotBlank(collections) && StringUtils.isNotBlank(transientKey)) {
        return transactionFulfillment.writePrivateTransactionToLedger(
            channelName, chaincodeName, functionName, collections, transientKey, payload);
      } else {
        return transactionFulfillment.writeTransactionToLedger(
            channelName, chaincodeName, functionName, Optional.empty(), payload);
      }
    }
  }

  /**
   * The REST Endpoint for reading a transaction from the ledger
   *
   * @param networkName String channel name
   * @param contractName String chaincode name
   * @param transactionFunctionName String function name in chaincode
   * @param transactionId String key for payload
   * @param collections String collection name for PDC
   * @param transientKey String transientKey for PDC
   * @return responseEntity ResponseEntity Transaction Response
   */
  @GetMapping(value = "/query_transaction")
  public ResponseEntity<ClientResponseModel> getTransaction(
      @RequestParam("channel") @Validated String networkName,
      @RequestParam("chaincode") @Validated String contractName,
      @RequestParam("function") @Validated String transactionFunctionName,
      @RequestParam("key") @Validated String transactionId,
      @RequestParam(value = "collection", required = false) String collections,
      @RequestParam(value = "transientKey", required = false) String transientKey) {
    log.info(
        "Initiated Transaction Read for Network Name: {}, Contract Name: {}, Transaction Function Name: {}, Transaction Id: {}",
        networkName,
        contractName,
        transactionFunctionName,
        transactionId);
    if (null != collections
        && !collections.isEmpty()
        && null != transientKey
        && !transientKey.isEmpty()) {
      return transactionFulfillment.readTransactionFromPrivateLedger(
          networkName,
          contractName,
          transactionFunctionName,
          transactionId,
          collections,
          transientKey);
    } else {
      return transactionFulfillment.readTransactionFromLedger(
          networkName, contractName, transactionFunctionName, transactionId);
    }
  }

  /**
   * The REST Endpoint for reading the transactions based on block number, transactionId,
   * networkName and eventType
   *
   * @param blockNumber block Number for querying
   * @param networkName networkName/channel
   * @param transactionId transactionId
   * @param eventType type of event
   * @return the EventAPIResponseModel which contain all the events
   */
  @GetMapping(value = "/blocks/{block-number}/transactions/{transaction-id}/events")
  public ResponseEntity<EventAPIResponseModel> getTransactionByBlockNumber(
      @PathVariable("transaction-id") String transactionId,
      @PathVariable("block-number") Long blockNumber,
      @RequestParam("channel") @Validated String networkName,
      @RequestParam(value = "chaincode", required = false) String chaincode,
      @RequestParam("eventType") @Validated String eventType) {
    log.info(
        "Initiated Transaction Read for Network Name: {}, Block Number: {}",
        networkName,
        blockNumber);
    return transactionFulfillment.readTransactionEventByBlockNumber(
        blockNumber, networkName, transactionId, chaincode, eventType);
  }
}
