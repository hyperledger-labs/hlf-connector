package hlf.java.rest.client.service.impl;

import hlf.java.rest.client.config.FabricProperties;
import hlf.java.rest.client.exception.ErrorCode;
import hlf.java.rest.client.exception.ErrorConstants;
import hlf.java.rest.client.exception.FabricTransactionException;
import hlf.java.rest.client.exception.NotFoundException;
import hlf.java.rest.client.exception.RetryableServiceException;
import hlf.java.rest.client.exception.ServiceException;
import hlf.java.rest.client.model.AbstractModelValidator;
import hlf.java.rest.client.model.BlockEventWriteSet;
import hlf.java.rest.client.model.ClientResponseModel;
import hlf.java.rest.client.model.EventAPIResponseModel;
import hlf.java.rest.client.model.EventType;
import hlf.java.rest.client.model.MultiDataTransactionPayload;
import hlf.java.rest.client.model.MultiPrivateDataTransactionPayloadValidator;
import hlf.java.rest.client.service.HFClientWrapper;
import hlf.java.rest.client.service.RecencyTransactionContext;
import hlf.java.rest.client.service.TransactionFulfillment;
import hlf.java.rest.client.util.FabricEventParseUtil;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.ContractException;
import org.hyperledger.fabric.gateway.DefaultCommitHandlers;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.GatewayRuntimeException;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.Transaction;
import org.hyperledger.fabric.gateway.spi.CommitHandler;
import org.hyperledger.fabric.sdk.BlockInfo;
import org.hyperledger.fabric.sdk.ChaincodeResponse;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
public class TransactionFulfillmentImpl implements TransactionFulfillment {

  // List of exceptions that would be wrapped as 'FabricTransactionException' once caught.
  private static final List<Class<? extends Exception>> fabricTransactionExceptionCandidates =
      Arrays.asList(
          GatewayRuntimeException.class, ContractException.class, InterruptedException.class);

  private static final long DEFAULT_ORDERER_TIMEOUT = 60;
  private static final TimeUnit DEFAULT_ORDERER_TIMEOUT_UNIT = TimeUnit.SECONDS;

  private static final long DEFAULT_COMMIT_TIMEOUT = 5;
  private static final TimeUnit DEFAULT_COMMIT_TIMEOUT_UNIT = TimeUnit.MINUTES;

  // Error messages from Fabric while connecting to chaincode from peer
  private static final List<String> fabricTxErrorList =
      Arrays.asList(
          "Failed to send transaction to the orderer",
          "error creating grpc connection",
          "error cannot create connection",
          "could not launch chaincode");

  @Autowired private FabricProperties fabricProperties;

  @Autowired private Gateway gateway;

  @Autowired private HFClientWrapper hfClientWrapper;

  @Autowired private RecencyTransactionContext recencyTransactionContext;

  @Override
  public ResponseEntity<ClientResponseModel> initSmartContract(
      String networkName,
      String contractName,
      String functionName,
      Optional<List<String>> peerNames,
      String... transactionParams) {
    log.info("initialize the smart contract, this is done once every install");
    // get the network information to send the request
    Network network = gateway.getNetwork(networkName);

    // set the init flag
    Channel channel = network.getChannel();

    // Generate the transaction proposal, set the init flag
    TransactionProposalRequest transactionProposalRequest =
        hfClientWrapper.getHfClient().newTransactionProposalRequest();
    transactionProposalRequest.setInit(true);
    transactionProposalRequest.setChaincodeName(contractName);
    transactionProposalRequest.setFcn(functionName);
    transactionProposalRequest.setTransactionContext(channel.newTransactionContext());
    transactionProposalRequest.setArgs(transactionParams);

    List<Peer> endorsingPeers = new ArrayList<>();
    // get the peers if present
    if (peerNames.isPresent()) {
      for (Peer channelPeer : network.getChannel().getPeers()) {
        log.info("Peer Name: " + channelPeer.getName());
        if (peerNames.get().contains(channelPeer.getName())) {
          endorsingPeers.add(channelPeer);
        }
      }
    }

    Collection<ProposalResponse> proposalResponses;
    // send the transaction
    try {
      if (!endorsingPeers.isEmpty()) {
        proposalResponses =
            channel.sendTransactionProposal(transactionProposalRequest, endorsingPeers);
      } else {
        proposalResponses = channel.sendTransactionProposal(transactionProposalRequest);
      }
    } catch (Exception ex) {
      throw new FabricTransactionException(
          ErrorCode.HYPERLEDGER_FABRIC_TRANSACTION_ERROR, ex.getMessage(), ex);
    }
    // check response status
    List<ProposalResponse> validResponses =
        proposalResponses.stream()
            .filter(response -> response.getStatus().equals(ChaincodeResponse.Status.SUCCESS))
            .collect(Collectors.toList());
    if (validResponses.isEmpty()) {
      throw new FabricTransactionException(
          ErrorCode.HYPERLEDGER_FABRIC_TRANSACTION_ERROR, "no successful endorsements");
    }
    // send the endorsements to the orderer nodes
    byte[] response =
        commitTransaction(validResponses, network, validResponses.get(0).getTransactionID());
    log.info("successfully initialized the chaincode");
    String resultString = new String(response, StandardCharsets.UTF_8);
    return new ResponseEntity<>(
        new ClientResponseModel(ErrorConstants.NO_ERROR, resultString), HttpStatus.OK);
  }

  private byte[] commitTransaction(
      final Collection<ProposalResponse> validResponses, Network network, String transactionId) {
    ProposalResponse proposalResponse = validResponses.iterator().next();

    CommitHandler commitHandler =
        DefaultCommitHandlers.MSPID_SCOPE_ANYFORTX.create(transactionId, network);
    commitHandler.startListening();

    try {
      Channel.TransactionOptions transactionOptions =
          Channel.TransactionOptions.createTransactionOptions()
              .nOfEvents(
                  Channel.NOfEvents.createNoEvents()); // Disable default commit wait behaviour
      network
          .getChannel()
          .sendTransaction(validResponses, transactionOptions)
          .get(DEFAULT_ORDERER_TIMEOUT, DEFAULT_ORDERER_TIMEOUT_UNIT);
    } catch (TimeoutException e) {
      commitHandler.cancelListening();
      throw new FabricTransactionException(
          ErrorCode.HYPERLEDGER_FABRIC_TRANSACTION_ERROR, "timed out", e);
    } catch (Exception e) {
      commitHandler.cancelListening();
      throw new FabricTransactionException(
          ErrorCode.HYPERLEDGER_FABRIC_TRANSACTION_ERROR,
          "Failed to send transaction to the orderer",
          e);
    }

    try {
      commitHandler.waitForEvents(DEFAULT_COMMIT_TIMEOUT, DEFAULT_COMMIT_TIMEOUT_UNIT);
      return proposalResponse.getChaincodeActionResponsePayload();
    } catch (Exception e) {
      throw new FabricTransactionException(
          ErrorCode.HYPERLEDGER_FABRIC_TRANSACTION_ERROR, "exception while waiting", e);
    }
  }

  @Override
  public ResponseEntity<ClientResponseModel> writeTransactionToLedger(
      String networkName,
      String contractName,
      String transactionFunctionName,
      Optional<List<String>> peerNames,
      String... transactionParams) {
    log.info("Initiate the Write Transaction to Ledger process");
    String resultString = StringUtils.EMPTY;
    List<Peer> endorsingPeers = new ArrayList<>();
    try {
      Network network = gateway.getNetwork(networkName);
      Contract contract = network.getContract(contractName);
      Transaction fabricTransaction = contract.createTransaction(transactionFunctionName);
      // override default commit handler to wait for any response from msp
      fabricTransaction.setCommitHandler(DefaultCommitHandlers.MSPID_SCOPE_ANYFORTX);
      if (peerNames.isPresent()) {
        for (Peer channelPeer : network.getChannel().getPeers()) {
          log.info("Peer Name: " + channelPeer.getName());
          if (peerNames.get().contains(channelPeer.getName())) {
            endorsingPeers.add(channelPeer);
          }
        }

        if (!endorsingPeers.isEmpty()) {
          fabricTransaction.setEndorsingPeers(endorsingPeers);
        } else {
          log.warn("Peer names don't match channel peers");
        }
      }

      recencyTransactionContext.setTransactionContext(fabricTransaction.getTransactionId());

      log.info(
          "Performing Write Transaction to Ledger with Tx ID {}",
          fabricTransaction.getTransactionId());
      byte[] result = fabricTransaction.submit(transactionParams);
      resultString = new String(result, StandardCharsets.UTF_8);
      log.info("Transaction Successfully Submitted - Response: " + resultString);

    } catch (Exception exception) {
      handleTransactionException(exception);
    }
    return new ResponseEntity<>(
        new ClientResponseModel(ErrorConstants.NO_ERROR, resultString), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<ClientResponseModel> writePrivateTransactionToLedger(
      String networkName,
      String contractName,
      String transactionFunctionName,
      String collection,
      String transientKey,
      String jsonPayload) {
    return writePrivateTransactionToLedger(
        networkName,
        contractName,
        transactionFunctionName,
        collection,
        transientKey,
        Optional.empty(),
        jsonPayload);
  }

  @Override
  public ResponseEntity<ClientResponseModel> writePrivateTransactionToLedger(
      String networkName,
      String contractName,
      String transactionFunctionName,
      String collection,
      String transientKey,
      Optional<List<String>> peerNames,
      String jsonPayload) {
    log.info("Initiate the Write Transaction to Ledger process");
    String resultString = StringUtils.EMPTY;
    Collection<Peer> endorsingPeers = new ArrayList<>();
    Map<String, byte[]> transientParam = new HashMap<>();
    try {
      Network network = gateway.getNetwork(networkName);
      Contract contract = network.getContract(contractName);
      Transaction fabricTransaction = contract.createTransaction(transactionFunctionName);
      // override default commithandler to wait for any response from msp
      fabricTransaction.setCommitHandler(DefaultCommitHandlers.MSPID_SCOPE_ANYFORTX);

      if (peerNames.isPresent()) {
        for (Peer channelPeer : network.getChannel().getPeers()) {
          log.info("Peer Name: " + channelPeer.getName());
          if (peerNames.get().contains(channelPeer.getName())) {
            endorsingPeers.add(channelPeer);
          }
        }
        if (!CollectionUtils.isEmpty(endorsingPeers)) {
          fabricTransaction.setEndorsingPeers(endorsingPeers);
        } else {
          log.warn("Peer names don't match channel peers");
        }
      }

      transientParam.put(transientKey, jsonPayload.getBytes());
      fabricTransaction.setTransient(transientParam);

      recencyTransactionContext.setTransactionContext(fabricTransaction.getTransactionId());

      log.info(
          "Performing Write Transaction to Ledger with Tx ID {}",
          fabricTransaction.getTransactionId());
      byte[] result = fabricTransaction.submit(collection, transientKey);
      resultString = new String(result, StandardCharsets.UTF_8);
      log.info("Transaction Successfully Submitted - Response: " + resultString);
    } catch (Exception exception) {
      handleTransactionException(exception);
    }
    return new ResponseEntity<>(
        new ClientResponseModel(ErrorConstants.NO_ERROR, resultString), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<ClientResponseModel> readTransactionFromLedger(
      String networkName,
      String contractName,
      String transactionFunctionName,
      String transactionId) {
    log.info("Initiate the Read Transaction from Ledger process");
    String resultString;

    try {
      Network network = gateway.getNetwork(networkName);
      Contract contract = network.getContract(contractName);
      byte[] result = contract.evaluateTransaction(transactionFunctionName, transactionId);
      resultString = new String(result, StandardCharsets.UTF_8);
      log.info("Result from Query: " + resultString);

    } catch (ContractException e) {
      log.error(
          "Action Failed: A problem occurred while retrieving the transaction from the Ledger", e);
      throw new FabricTransactionException(
          ErrorCode.HYPERLEDGER_FABRIC_TRANSACTION_ERROR, e.getMessage(), e);
    }
    return new ResponseEntity<>(
        new ClientResponseModel(ErrorConstants.NO_ERROR, resultString), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<ClientResponseModel> readTransactionFromPrivateLedger(
      String networkName,
      String contractName,
      String transactionFunctionName,
      String transactionId,
      String collections,
      String transientKey) {
    log.info("Initiate the Read Transaction from Ledger process");
    String resultString;
    Map<String, byte[]> transientParam = new HashMap<>();

    try {
      Network network = gateway.getNetwork(networkName);
      Contract contract = network.getContract(contractName);
      transientParam.put(transientKey, transactionId.getBytes());
      Transaction fabricTransaction = contract.createTransaction(transactionFunctionName);
      fabricTransaction.setTransient(transientParam);
      byte[] result = fabricTransaction.evaluate(collections, transientKey);
      resultString = new String(result, StandardCharsets.UTF_8);
      log.info("Result from Query: " + resultString);

    } catch (ContractException e) {
      log.error(
          "Action Failed: A problem occurred while retrieving the transaction from the Ledger", e);
      throw new FabricTransactionException(
          ErrorCode.HYPERLEDGER_FABRIC_TRANSACTION_ERROR, e.getMessage(), e);
    }
    return new ResponseEntity<>(
        new ClientResponseModel(ErrorConstants.NO_ERROR, resultString), HttpStatus.OK);
  }

  /**
   * This function returns the list of all the events in the block with the block number and
   * transactionId provided It returns the type of event passed i.e. BLOCK_EVENT and CHAINCODE_EVENT
   *
   * @param blockNumber block Number for querying
   * @param networkName networkName/channel
   * @param transactionId transactionId
   * @param eventType type of event
   * @return the EventAPIResponseModel which contain all the events
   */
  @Override
  public ResponseEntity<EventAPIResponseModel> readTransactionEventByBlockNumber(
      Long blockNumber,
      String networkName,
      String transactionId,
      String chaincode,
      String eventType) {
    log.info(
        "Initiate the Read Transaction from Ledger process by blockNumber, networkName and transactionId based on eventType");

    if (Objects.isNull(fabricProperties.getEvents())) {
      throw new NotFoundException(
          ErrorCode.NO_EVENTS_FOUND, "Events API not enabled in the configuration.");
    }

    List<BlockEventWriteSet> blockEventWriteSets = new ArrayList<>();
    List<String> chainCodeEventSets = new ArrayList<>();
    try {
      Network network = gateway.getNetwork(networkName);
      Channel channel = network.getChannel();
      BlockInfo blockInfo = channel.queryBlockByNumber(blockNumber);

      Iterable<BlockInfo.EnvelopeInfo> envelopeInfos = blockInfo.getEnvelopeInfos();
      for (BlockInfo.EnvelopeInfo info : envelopeInfos) {
        BlockInfo.TransactionEnvelopeInfo envelopeInfo = (BlockInfo.TransactionEnvelopeInfo) info;

        if (envelopeInfo.getTransactionID().equals(transactionId)) {

          // Getting the BLOCK_EVENT for the transactionId and blockNumber
          if (eventType.equals(EventType.BLOCK_EVENT.toString())) {

            blockEventWriteSets.addAll(
                FabricEventParseUtil.getBlockEventWriteSet(
                    envelopeInfo.getTransactionActionInfos(), chaincode));

            // Getting the CHAINCODE_EVENT for the transactionId and blockNumber
          } else if (eventType.equals(EventType.CHAINCODE_EVENT.toString())) {

            chainCodeEventSets.addAll(
                FabricEventParseUtil.getChaincodeEventWriteSet(
                    envelopeInfo.getTransactionActionInfos(), chaincode));
          }
        }
      }

      if (blockEventWriteSets.isEmpty() && chainCodeEventSets.isEmpty()) {
        throw new NotFoundException(
            ErrorCode.NO_EVENTS_FOUND, "No Events found for the passed parameters.");
      } else if (!blockEventWriteSets.isEmpty()) {
        return new ResponseEntity<>(
            EventAPIResponseModel.builder()
                .data(
                    FabricEventParseUtil.stringify(Collections.singletonList(blockEventWriteSets)))
                .build(),
            HttpStatus.OK);
      } else {
        return new ResponseEntity<>(
            EventAPIResponseModel.builder()
                .data(FabricEventParseUtil.stringify(Collections.singletonList(chainCodeEventSets)))
                .build(),
            HttpStatus.OK);
      }
    } catch (IOException e) {
      log.error("Action Failed: A problem occurred while retrieving the network connection", e);
      throw new ServiceException(ErrorCode.HYPERLEDGER_FABRIC_CONNECTION_ERROR, e.getMessage(), e);
    } catch (InvalidArgumentException e) {
      log.error(
          "Action Failed: A problem occurred while parsing the block data with InvalidArgumentException.",
          e);
      throw new ServiceException(ErrorCode.HYPERLEDGER_FABRIC_CHANNEL_TXN_ERROR, e.getMessage(), e);
    } catch (ProposalException e) {
      log.error("Action Failed: A problem occurred while fetching transaction by block number", e);
      throw new ServiceException(ErrorCode.HYPERLEDGER_FABRIC_CHANNEL_TXN_ERROR, e.getMessage(), e);
    }
  }

  @Override
  public ResponseEntity<ClientResponseModel> writeMultiDataTransactionToLedger(
      String channelName,
      String chaincodeName,
      String transactionFunctionName,
      MultiDataTransactionPayload multiDataTransactionPayload) {

    // Validate the incoming payload
    AbstractModelValidator<MultiDataTransactionPayload> validator =
        new MultiPrivateDataTransactionPayloadValidator();
    validator.validate(multiDataTransactionPayload);

    Collection<Peer> endorsingPeers;
    String resultString = StringUtils.EMPTY;

    try {

      // get the network object through connection profile
      Network network = gateway.getNetwork(channelName);
      // fetch associated smart contract information from the blockchain
      // network
      Contract contract = network.getContract(chaincodeName);
      // start composing the transaction
      Transaction fabricTransaction = contract.createTransaction(transactionFunctionName);
      // override default commithandler to wait for any response from msp
      fabricTransaction.setCommitHandler(DefaultCommitHandlers.MSPID_SCOPE_ANYFORTX);

      endorsingPeers =
          network.getChannel().getPeers().stream()
              .filter(
                  peer ->
                      Objects.nonNull(multiDataTransactionPayload.getPeerNames())
                          && multiDataTransactionPayload.getPeerNames().contains(peer.getName()))
              .collect(Collectors.toList());
      if (!endorsingPeers.isEmpty()) {
        fabricTransaction.setEndorsingPeers(endorsingPeers);
      } else {
        log.warn("Peer names don't match channel peers");
      }

      List<String> publicParamsList = new ArrayList<>();

      /*
       * Scan through Private Data details the incoming transaction Request. If the Private data
       * details consist of a Collection name, then add the key-value pair to the transient map and
       * also ensure that the Collection name and key name is also part of the public params list.
       * If Collection name is not present, simply populate the key-value pair to the transient map.
       */
      if (!CollectionUtils.isEmpty(multiDataTransactionPayload.getPrivatePayload())) {
        Map<String, byte[]> transientParam = new HashMap<>();
        multiDataTransactionPayload
            .getPrivatePayload()
            .forEach(
                privateTransactionPayload -> {
                  if (StringUtils.isNotBlank(privateTransactionPayload.getCollectionName())) {
                    publicParamsList.add(privateTransactionPayload.getCollectionName());
                    publicParamsList.add(privateTransactionPayload.getKey());
                  }

                  transientParam.put(
                      privateTransactionPayload.getKey(),
                      privateTransactionPayload.getData().getBytes(StandardCharsets.UTF_8));
                });

        fabricTransaction.setTransient(transientParam);
      }

      // Check if Public params are also passed in the request. If provided, append them to the
      // public params list
      if (!CollectionUtils.isEmpty(multiDataTransactionPayload.getPublicPayload())) {
        publicParamsList.addAll(multiDataTransactionPayload.getPublicPayload());
      }

      // Map to String Array for dispatching via SDK method
      String[] publicDataArgs = publicParamsList.toArray(new String[publicParamsList.size()]);

      recencyTransactionContext.setTransactionContext(fabricTransaction.getTransactionId());

      log.info(
          "Performing Write Transaction to Ledger with Tx ID {}",
          fabricTransaction.getTransactionId());
      byte[] result = fabricTransaction.submit(publicDataArgs);

      resultString = new String(result, StandardCharsets.UTF_8);
      log.info("Transaction Successfully Submitted - Response: " + resultString);
    } catch (Exception exception) {
      handleTransactionException(exception);
    }
    return new ResponseEntity<>(
        new ClientResponseModel(ErrorConstants.NO_ERROR, resultString), HttpStatus.OK);
  }

  private void handleTransactionException(Exception incomingException) {

    log.error(
        "An error occurred while submitting the transaction to the Network. Error Type: {} & Error Message: {}",
        incomingException.getCause(),
        incomingException.getMessage());

    if (fabricTransactionExceptionCandidates.contains(incomingException.getClass())) {

      if (Objects.nonNull(incomingException.getCause())
          && (incomingException.getCause() instanceof IOException
              || fabricTxErrorList.stream().anyMatch(incomingException.getMessage()::contains))) {
        throw new RetryableServiceException(
            ErrorCode.HYPERLEDGER_FABRIC_CONNECTION_ERROR,
            incomingException.getMessage(),
            incomingException);
      }

      throw new FabricTransactionException(
          ErrorCode.HYPERLEDGER_FABRIC_TRANSACTION_ERROR,
          incomingException.getMessage(),
          incomingException);
    }

    if (incomingException instanceof TimeoutException) {
      throw new RetryableServiceException(
          ErrorCode.HYPERLEDGER_FABRIC_CONNECTION_TIMEOUT_ERROR,
          incomingException.getMessage(),
          incomingException);
    }

    throw new ServiceException(
        ErrorCode.HYPERLEDGER_FABRIC_TRANSACTION_ERROR,
        incomingException.getMessage(),
        incomingException);
  }
}
