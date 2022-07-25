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
import hlf.java.rest.client.service.HFClientWrapper;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.ChaincodeCollectionConfiguration;
import org.hyperledger.fabric.sdk.ChaincodeResponse;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.LifecycleApproveChaincodeDefinitionForMyOrgProposalResponse;
import org.hyperledger.fabric.sdk.LifecycleApproveChaincodeDefinitionForMyOrgRequest;
import org.hyperledger.fabric.sdk.LifecycleChaincodeEndorsementPolicy;
import org.hyperledger.fabric.sdk.LifecycleCheckCommitReadinessProposalResponse;
import org.hyperledger.fabric.sdk.LifecycleCheckCommitReadinessRequest;
import org.hyperledger.fabric.sdk.LifecycleCommitChaincodeDefinitionProposalResponse;
import org.hyperledger.fabric.sdk.LifecycleCommitChaincodeDefinitionRequest;
import org.hyperledger.fabric.sdk.LifecycleQueryChaincodeDefinitionProposalResponse;
import org.hyperledger.fabric.sdk.LifecycleQueryInstalledChaincodesProposalResponse;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.QueryLifecycleQueryChaincodeDefinitionRequest;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ChaincodeOperationsServiceImpl implements ChaincodeOperationsService {

  @Autowired private Gateway gateway;
  @Autowired private HFClientWrapper hfClientWrapper;

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
          return approveChaincode(channel, chaincodeOperationsModel);
        }
      case commit:
        {
          return commitChaincode(channel, chaincodeOperationsModel);
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
              hfClientWrapper.getHfClient().newQueryLifecycleQueryChaincodeDefinitionRequest();
      queryLifecycleQueryChaincodeDefinitionRequest.setChaincodeName(chaincodeName);

      Collection<LifecycleQueryChaincodeDefinitionProposalResponse>
          queryChaincodeDefinitionProposalResponses =
              channel.lifecycleQueryChaincodeDefinition(
                  queryLifecycleQueryChaincodeDefinitionRequest, peers);
      long sequence = -1L;
      for (LifecycleQueryChaincodeDefinitionProposalResponse response :
          queryChaincodeDefinitionProposalResponses) {
        if (response.getStatus() == ProposalResponse.Status.SUCCESS
            && response.getVersion().equals(chaincodeVersion)) {
          sequence = response.getSequence();
          break;
        } else {
          if (HttpStatus.NOT_FOUND.value() == response.getChaincodeActionResponseStatus()) {
            // not found set sequence to 1;
            sequence = 1;
            break;
          }
        }
      }

      if (sequence == -1) {
        throw new ServiceException(
            SEQUENCE_NUMBER_VALIDATION_FAILED,
            "Sequence Number not present in peers for channel: " + networkName);
      }

      return String.valueOf(sequence);
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
          hfClientWrapper
              .getHfClient()
              .sendLifecycleQueryInstalledChaincodes(
                  hfClientWrapper.getHfClient().newLifecycleQueryInstalledChaincodesRequest(),
                  peers);
      String packageId = null;
      for (LifecycleQueryInstalledChaincodesProposalResponse peerResults : results) {
        for (LifecycleQueryInstalledChaincodesProposalResponse
                .LifecycleQueryInstalledChaincodesResult
            lifecycleQueryInstalledChaincodeResult :
                peerResults.getLifecycleQueryInstalledChaincodesResult()) {
          if (lifecycleQueryInstalledChaincodeResult.getLabel().equals(chaincodeName)) {
            packageId = lifecycleQueryInstalledChaincodeResult.getPackageId();
            break;
          }
        }
      }

      if (isNull(packageId)) {
        throw new ServiceException(
            CHAINCODE_PACKAGE_ID_VALIDATION_FAILED,
            "Chaincode PackageId not present in peers for channel: " + networkName);
      }

      return packageId;
    } catch (InvalidArgumentException | ProposalException e) {
      throw new ServiceException(
          ErrorCode.HYPERLEDGER_FABRIC_CHAINCODE_OPERATIONS_REQUEST_REJECTION, e.getMessage(), e);
    }
  }

  @Override
  public Set<String> getApprovedOrganizations(
      String networkName,
      ChaincodeOperations chaincodeOperationsModel,
      Optional<LifecycleChaincodeEndorsementPolicy> chaincodeEndorsementPolicyOptional,
      Optional<ChaincodeCollectionConfiguration> chaincodeCollectionConfigurationOptional) {
    Set<String> organizationSet = new HashSet<>();
    try {

      Network network = gateway.getNetwork(networkName);
      Channel channel = network.getChannel();

      LifecycleCheckCommitReadinessRequest lifecycleCheckCommitReadinessRequest =
          hfClientWrapper.getHfClient().newLifecycleSimulateCommitChaincodeDefinitionRequest();
      lifecycleCheckCommitReadinessRequest.setSequence(chaincodeOperationsModel.getSequence());

      lifecycleCheckCommitReadinessRequest.setChaincodeName(
          chaincodeOperationsModel.getChaincodeName());
      lifecycleCheckCommitReadinessRequest.setChaincodeVersion(
          chaincodeOperationsModel.getChaincodeVersion());

      if (chaincodeEndorsementPolicyOptional.isPresent()) {
        lifecycleCheckCommitReadinessRequest.setChaincodeEndorsementPolicy(
            chaincodeEndorsementPolicyOptional.get());
      }
      if (chaincodeCollectionConfigurationOptional.isPresent()) {
        lifecycleCheckCommitReadinessRequest.setChaincodeCollectionConfiguration(
            chaincodeCollectionConfigurationOptional.get());
      }

      lifecycleCheckCommitReadinessRequest.setInitRequired(true);

      Collection<LifecycleCheckCommitReadinessProposalResponse>
          lifecycleSimulateCommitChaincodeDefinitionProposalResponse =
              channel.sendLifecycleCheckCommitReadinessRequest(
                  lifecycleCheckCommitReadinessRequest, channel.getPeers());
      for (LifecycleCheckCommitReadinessProposalResponse resp :
          lifecycleSimulateCommitChaincodeDefinitionProposalResponse) {
        if (resp.getStatus() == ChaincodeResponse.Status.SUCCESS) {
          organizationSet.addAll(resp.getApprovedOrgs());
        }
      }

    } catch (InvalidArgumentException | ProposalException e) {
      throw new ServiceException(ErrorCode.HYPERLEDGER_FABRIC_CONNECTION_ERROR, e.getMessage());
    }

    return organizationSet;
  }

  private String approveChaincode(Channel channel, ChaincodeOperations chaincodeOperationsModel) {

    Collection<Peer> peers = channel.getPeers();
    try {
      LifecycleApproveChaincodeDefinitionForMyOrgRequest
          lifecycleApproveChaincodeDefinitionForMyOrgRequest =
              hfClientWrapper.getHfClient().newLifecycleApproveChaincodeDefinitionForMyOrgRequest();
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

  private String commitChaincode(Channel channel, ChaincodeOperations chaincodeOperationsModel) {

    Collection<Peer> peers = channel.getPeers();
    try {
      LifecycleCommitChaincodeDefinitionRequest lifecycleCommitChaincodeDefinitionRequest =
          hfClientWrapper.getHfClient().newLifecycleCommitChaincodeDefinitionRequest();

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
