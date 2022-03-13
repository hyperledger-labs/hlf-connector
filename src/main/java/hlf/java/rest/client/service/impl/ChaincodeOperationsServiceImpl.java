package hlf.java.rest.client.service.impl;

import static hlf.java.rest.client.exception.ErrorCode.CHAINCODE_PACKAGE_ID_VALIDATION_FAILED;
import static hlf.java.rest.client.exception.ErrorCode.SEQUENCE_NUMBER_VALIDATION_FAILED;
import static hlf.java.rest.client.model.ChaincodeOperationsType.approve;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import hlf.java.rest.client.exception.ErrorCode;
import hlf.java.rest.client.exception.ServiceException;
import hlf.java.rest.client.model.ChaincodeOperations;
import hlf.java.rest.client.model.ChaincodeOperationsType;
import hlf.java.rest.client.service.ChaincodeOperationsService;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.LifecycleApproveChaincodeDefinitionForMyOrgProposalResponse;
import org.hyperledger.fabric.sdk.LifecycleApproveChaincodeDefinitionForMyOrgRequest;
import org.hyperledger.fabric.sdk.LifecycleCommitChaincodeDefinitionProposalResponse;
import org.hyperledger.fabric.sdk.LifecycleCommitChaincodeDefinitionRequest;
import org.hyperledger.fabric.sdk.LifecycleQueryChaincodeDefinitionProposalResponse;
import org.hyperledger.fabric.sdk.LifecycleQueryInstalledChaincodesProposalResponse;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.QueryLifecycleQueryChaincodeDefinitionRequest;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ChaincodeOperationsServiceImpl implements ChaincodeOperationsService {

  @Autowired private Gateway gateway;

  @Autowired private HFClient hfClient;

  @Override
  public String performChaincodeOperation(
      String networkName,
      ChaincodeOperations chaincodeOperationsModel,
      ChaincodeOperationsType operationsType) {

    validateChaincodeOperationsInput(chaincodeOperationsModel, operationsType);

    Network network = gateway.getNetwork(networkName);
    Channel channel = network.getChannel();

    switch (operationsType) {
      case approve:
        {
          return approveChaincode(hfClient, channel, chaincodeOperationsModel);
        }
      case commit:
        {
          return commitChaincode(hfClient, channel, chaincodeOperationsModel);
        }
      default:
        {
          throw new ServiceException(
              ErrorCode.NOT_SUPPORTED, "The passed chaincode operation not supported.");
        }
    }
  }

  @Override
  public String getCurrentSequence(
      String networkName, String chaincodeName, String chaincodeVersion) {
    try {

      Network network = gateway.getNetwork(networkName);
      Channel channel = network.getChannel();

      Collection<Peer> peers = channel.getPeers();

      final QueryLifecycleQueryChaincodeDefinitionRequest
          queryLifecycleQueryChaincodeDefinitionRequest =
              hfClient.newQueryLifecycleQueryChaincodeDefinitionRequest();
      queryLifecycleQueryChaincodeDefinitionRequest.setChaincodeName(chaincodeName);

      Collection<LifecycleQueryChaincodeDefinitionProposalResponse>
          queryChaincodeDefinitionProposalResponses =
              channel.lifecycleQueryChaincodeDefinition(
                  queryLifecycleQueryChaincodeDefinitionRequest, peers);
      Set<Long> sequenceNumbers = new HashSet<>();
      for (LifecycleQueryChaincodeDefinitionProposalResponse response :
          queryChaincodeDefinitionProposalResponses) {
        if (response.getVersion().equals(chaincodeVersion)) {
          sequenceNumbers.add(response.getSequence());
        }
      }

      if (sequenceNumbers.size() == 0) {
        throw new ServiceException(
            SEQUENCE_NUMBER_VALIDATION_FAILED,
            "Sequence Number not present in peers for channel: " + networkName);
      }

      if (sequenceNumbers.size() > 1) {
        throw new ServiceException(
            SEQUENCE_NUMBER_VALIDATION_FAILED,
            "Different sequence numbers present in peers for channel: " + networkName);
      }
      return String.valueOf(sequenceNumbers.stream().findFirst().get());
    } catch (ProposalException | InvalidArgumentException e) {
      throw new ServiceException(
          ErrorCode.HYPERLEDGER_FABRIC_CHAINCODE_OPERATIONS_REQUEST_REJECTION, e.getMessage(), e);
    }
  }

  @Override
  public String getCurrentPackageId(
      String networkName, String chaincodeName, String chaincodeVersion) {

    Network network = gateway.getNetwork(networkName);
    Channel channel = network.getChannel();

    Collection<Peer> peers = channel.getPeers();

    try {
      Collection<LifecycleQueryInstalledChaincodesProposalResponse> results =
          hfClient.sendLifecycleQueryInstalledChaincodes(
              hfClient.newLifecycleQueryInstalledChaincodesRequest(), peers);
      Set<String> packageIds = new HashSet<>();
      for (LifecycleQueryInstalledChaincodesProposalResponse peerResults : results) {
        for (LifecycleQueryInstalledChaincodesProposalResponse
                .LifecycleQueryInstalledChaincodesResult
            lifecycleQueryInstalledChaincodeResult :
                peerResults.getLifecycleQueryInstalledChaincodesResult()) {
          packageIds.add(lifecycleQueryInstalledChaincodeResult.getPackageId());
        }
      }

      if (packageIds.size() == 0) {
        throw new ServiceException(
            CHAINCODE_PACKAGE_ID_VALIDATION_FAILED,
            "Chaincode PackageId not present in peers for channel: " + networkName);
      }

      // packageIds will be same in all fabric peers as per design
      if (packageIds.size() > 1) {
        throw new ServiceException(
            CHAINCODE_PACKAGE_ID_VALIDATION_FAILED,
            "Different packageIds present in peers for channel: " + networkName);
      }

      return packageIds.stream().findFirst().get();
    } catch (InvalidArgumentException | ProposalException e) {
      throw new ServiceException(
          ErrorCode.HYPERLEDGER_FABRIC_CHAINCODE_OPERATIONS_REQUEST_REJECTION, e.getMessage(), e);
    }
  }

  private String approveChaincode(
      HFClient hfClient, Channel channel, ChaincodeOperations chaincodeOperationsModel) {

    Collection<Peer> peers = channel.getPeers();
    try {
      LifecycleApproveChaincodeDefinitionForMyOrgRequest
          lifecycleApproveChaincodeDefinitionForMyOrgRequest =
              hfClient.newLifecycleApproveChaincodeDefinitionForMyOrgRequest();
      lifecycleApproveChaincodeDefinitionForMyOrgRequest.setSequence(
          chaincodeOperationsModel.getSequence());
      lifecycleApproveChaincodeDefinitionForMyOrgRequest.setChaincodeName(
          chaincodeOperationsModel.getChaincodeName());
      lifecycleApproveChaincodeDefinitionForMyOrgRequest.setChaincodeVersion(
          chaincodeOperationsModel.getChaincodeVersion());
      lifecycleApproveChaincodeDefinitionForMyOrgRequest.setInitRequired(
          chaincodeOperationsModel.getInitRequired());

      // TODO: Add chaincodeCollectionConfiguration and chaincodeEndorsementPolicy

      lifecycleApproveChaincodeDefinitionForMyOrgRequest.setPackageId(
          chaincodeOperationsModel.getChaincodePackageID());

      Collection<LifecycleApproveChaincodeDefinitionForMyOrgProposalResponse>
          lifecycleApproveChaincodeDefinitionForMyOrgProposalResponse =
              channel.sendLifecycleApproveChaincodeDefinitionForMyOrgProposal(
                  lifecycleApproveChaincodeDefinitionForMyOrgRequest, peers);

      CompletableFuture<BlockEvent.TransactionEvent> transactionEventCompletableFuture =
          channel.sendTransaction(lifecycleApproveChaincodeDefinitionForMyOrgProposalResponse);

      // Making the sendTransaction call synchronous
      BlockEvent.TransactionEvent transactionEvent = transactionEventCompletableFuture.join();
      return transactionEvent.getTransactionID();
    } catch (InvalidArgumentException e) {
      log.error(
          "Action Failed: A problem occurred while creating request for chaincode approval with InvalidArgumentException.",
          e);
      throw new ServiceException(
          ErrorCode.HYPERLEDGER_FABRIC_CHAINCODE_OPERATIONS_REQUEST_REJECTION, e.getMessage(), e);
    } catch (ProposalException e) {
      log.error(
          "Action Failed: A problem occurred while sending transaction for chaincode approval.", e);
      throw new ServiceException(
          ErrorCode.HYPERLEDGER_FABRIC_CHAINCODE_OPERATIONS_REQUEST_REJECTION, e.getMessage(), e);
    }
  }

  private String commitChaincode(
      HFClient hfClient, Channel channel, ChaincodeOperations chaincodeOperationsModel) {

    Collection<Peer> peers = channel.getPeers();
    try {
      LifecycleCommitChaincodeDefinitionRequest lifecycleCommitChaincodeDefinitionRequest =
          hfClient.newLifecycleCommitChaincodeDefinitionRequest();

      lifecycleCommitChaincodeDefinitionRequest.setSequence(chaincodeOperationsModel.getSequence());
      lifecycleCommitChaincodeDefinitionRequest.setChaincodeName(
          chaincodeOperationsModel.getChaincodeName());
      lifecycleCommitChaincodeDefinitionRequest.setChaincodeVersion(
          chaincodeOperationsModel.getChaincodeVersion());

      // TODO: Add chaincodeCollectionConfiguration and chaincodeEndorsementPolicy

      lifecycleCommitChaincodeDefinitionRequest.setInitRequired(
          chaincodeOperationsModel.getInitRequired());

      Collection<LifecycleCommitChaincodeDefinitionProposalResponse>
          lifecycleCommitChaincodeDefinitionProposalResponses =
              channel.sendLifecycleCommitChaincodeDefinitionProposal(
                  lifecycleCommitChaincodeDefinitionRequest, peers);

      CompletableFuture<BlockEvent.TransactionEvent> transactionEventCompletableFuture =
          channel.sendTransaction(lifecycleCommitChaincodeDefinitionProposalResponses);

      // Making the sendTransaction call synchronous
      BlockEvent.TransactionEvent transactionEvent = transactionEventCompletableFuture.join();
      return transactionEvent.getTransactionID();

    } catch (InvalidArgumentException e) {
      log.error(
          "Action Failed: A problem occurred while creating request for chaincode commit with InvalidArgumentException.",
          e);
      throw new ServiceException(
          ErrorCode.HYPERLEDGER_FABRIC_CHAINCODE_OPERATIONS_REQUEST_REJECTION, e.getMessage(), e);
    } catch (ProposalException e) {
      log.error(
          "Action Failed: A problem occurred while sending transaction for chaincode commit.", e);
      throw new ServiceException(
          ErrorCode.HYPERLEDGER_FABRIC_CHAINCODE_OPERATIONS_REQUEST_REJECTION, e.getMessage(), e);
    }
  }

  private void validateChaincodeOperationsInput(
      ChaincodeOperations chaincodeOperations, ChaincodeOperationsType operationsType) {
    if (isEmpty(chaincodeOperations.getChaincodeName())
        || isEmpty(chaincodeOperations.getChaincodeVersion())
        || isNull(chaincodeOperations.getSequence())
        || isNull(chaincodeOperations.getInitRequired())
        || (operationsType.equals(approve)
            && isEmpty(chaincodeOperations.getChaincodePackageID()))) {
      throw new ServiceException(
          ErrorCode.VALIDATION_FAILED,
          "Chaincode operations data passed is incorrect or not supported.");
    }
  }
}
