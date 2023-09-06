package hlf.java.rest.client.service;

import hlf.java.rest.client.model.ChaincodeOperations;
import hlf.java.rest.client.model.ChaincodeOperationsType;
import java.util.Optional;
import java.util.Set;
import org.hyperledger.fabric.sdk.LifecycleChaincodeEndorsementPolicy;
import org.springframework.web.multipart.MultipartFile;

public interface ChaincodeOperationsService {

  /**
   * Perform chaincode operations passed in the arguments.
   *
   * @param networkName the network name
   * @param chaincodeOperationsModel the chaincode operations model
   * @param operationsType the operations type
   * @return the transactionId
   */
  String performChaincodeOperation(
      String networkName,
      ChaincodeOperations chaincodeOperationsModel,
      ChaincodeOperationsType operationsType,
      Optional<MultipartFile> collectionConfigFile);

  /**
   * Gets current sequence for the channel and chaincode.
   *
   * @param networkName the network name
   * @param chaincodeName the chaincode name
   * @param chaincodeVersion the chaincode version
   * @return the current sequence
   */
  String getCurrentSequence(String networkName, String chaincodeName, String chaincodeVersion);

  /**
   * Gets current package id for the passed channel and chaincode.
   *
   * @param networkName the network name
   * @param chaincodeName the chaincode name
   * @param chaincodeVersion the chaincode version
   * @return the current package id
   */
  String getCurrentPackageId(String networkName, String chaincodeName, String chaincodeVersion);

  Set<String> getApprovedOrganizations(
      String networkName,
      ChaincodeOperations chaincodeOperationsModel,
      Optional<LifecycleChaincodeEndorsementPolicy> chaincodeEndorsementPolicyOptional,
      Optional<MultipartFile> collectionConfigFileOptional);
}
