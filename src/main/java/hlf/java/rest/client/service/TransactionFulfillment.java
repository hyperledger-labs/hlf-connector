package hlf.java.rest.client.service;

import hlf.java.rest.client.model.ClientResponseModel;
import hlf.java.rest.client.model.EventAPIResponseModel;
import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;

/**
 * Handles the business logic for processing transaction related actions affecting the ledger
 *
 * @author c0c00ub
 */
public interface TransactionFulfillment {

  /**
   * Business logic to initialize the smart contract that is installed in a channel. Initialization
   * allows one to set the default values or do settings in their state database entry.
   *
   * @param networkName String channel name
   * @param contractName String chaincode name
   * @param functionName String chaincode function for initializing
   * @param peerNames List of String type peer names to initialize the contract
   * @param transactionParams String array for arguments to the chaincode
   * @return responseEntity ResponseEntity Transaction Response
   */
  ResponseEntity<ClientResponseModel> initSmartContract(
      String networkName,
      String contractName,
      String functionName,
      Optional<List<String>> peerNames,
      String... transactionParams);

  /**
   * Business logic for writing a transaction to the ledger. Returns TRUE when the action is
   * successful and FALSE otherwise.
   *
   * @param networkName String channel name
   * @param contractName String chaincode name
   * @param transactionFunctionName String function name in chaincode
   * @param peerNames List of String type peer name for endorsement
   * @param transactionParams String[] String array for arguments to the chaincode.
   * @return responseEntity ResponseEntity Transaction Response
   */
  ResponseEntity<ClientResponseModel> writeTransactionToLedger(
      String networkName,
      String contractName,
      String transactionFunctionName,
      Optional<List<String>> peerNames,
      String... transactionParams);

  /**
   * Business logic for writing a transaction to the ledger. Returns TRUE when the action is
   *
   * <p>successful and FALSE otherwise.
   *
   * @param networkName String channel name
   * @param contractName String chaincode name
   * @param transactionFunctionName String function name in chaincode
   * @param collection String Private Data collection
   * @param transientKey String The key for the Transient Map
   * @param jsonPayload String The arguments as Json Payload
   * @return responseEntity ResponseEntity Transaction Response
   */
  ResponseEntity<ClientResponseModel> writePrivateTransactionToLedger(
      String networkName,
      String contractName,
      String transactionFunctionName,
      String collection,
      String transientKey,
      String jsonPayload);

  /**
   * Business logic for writing a transaction to the ledger. Returns TRUE when the action is
   *
   * <p>successful and FALSE otherwise.
   *
   * @param networkName String channel name
   * @param contractName String chaincode name
   * @param transactionFunctionName String function name in chaincode
   * @param collection String Private Data collection
   * @param transientKey String The key for the Transient Map
   * @param peerNames List of String type peer name for endorsement
   * @param jsonPayload String The arguments as Json Payload
   * @return responseEntity ResponseEntity Transaction Response
   */
  ResponseEntity<ClientResponseModel> writePrivateTransactionToLedger(
      String networkName,
      String contractName,
      String transactionFunctionName,
      String collection,
      String transientKey,
      Optional<List<String>> peerNames,
      String jsonPayload);

  /**
   * Business logic for reading a transaction from the ledger. Returns result set of the query when
   * successful and null otherwise.
   *
   * @param networkName String channel name
   * @param contractName String chaincode name
   * @param transactionFunctionName String function name in chaincode
   * @param transactionId String key for payload
   * @return responseEntity ResponseEntity Transaction Response
   */
  ResponseEntity<ClientResponseModel> readTransactionFromLedger(
      String networkName,
      String contractName,
      String transactionFunctionName,
      String transactionId);

  /**
   * Business logic for reading a private transaction from the ledger. Returns result set of the
   * query when successful and null otherwise.
   *
   * @param networkName String channel name
   * @param contractName String chaincode name
   * @param transactionFunctionName String function name in chaincode
   * @param transactionId String key for payload
   * @param collections String Private Data collection
   * @param transientKey String The key for the Transient Map
   * @return responseEntity ResponseEntity Transaction Response
   */
  ResponseEntity<ClientResponseModel> readTransactionFromPrivateLedger(
      String networkName,
      String contractName,
      String transactionFunctionName,
      String transactionId,
      String collections,
      String transientKey);

  ResponseEntity<EventAPIResponseModel> readTransactionEventByBlockNumber(
      Long blockNumber,
      String networkName,
      String transactionId,
      String chaincode,
      String eventType);
}
