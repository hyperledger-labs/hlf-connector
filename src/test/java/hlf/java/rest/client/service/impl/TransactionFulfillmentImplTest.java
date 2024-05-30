package hlf.java.rest.client.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hlf.java.rest.client.config.FabricProperties;
import hlf.java.rest.client.config.GatewayConfig;
import hlf.java.rest.client.exception.ErrorConstants;
import hlf.java.rest.client.exception.FabricTransactionException;
import hlf.java.rest.client.exception.ServiceException;
import hlf.java.rest.client.model.ClientResponseModel;
import hlf.java.rest.client.service.RecencyTransactionContext;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.ContractException;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.Transaction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

@ExtendWith(MockitoExtension.class)
@DirtiesContext
public class TransactionFulfillmentImplTest {

  @InjectMocks TransactionFulfillmentImpl transactionFulfillment;
  @Mock FabricProperties fabricProperties;
  @Mock GatewayConfig gatewayConfig;
  @Mock Gateway gatewayConnetion;
  @Mock Network network;
  @Mock Contract contract;
  @Mock Transaction transaction;
  @Mock RecencyTransactionContext recencyTransactionContext;

  private String testNetworkString = "some string";
  private String testContractString = "some string";
  private String testTransactionFunctionString = "some string";
  private String[] testTransactionParamsArrary = {"test param", "test param 2"};
  private String testTransactionIdString = "some string";
  byte[] byteArrayResponse = new byte[10];

  // Read more about @Captor here - https://www.baeldung.com/mockito-annotations#captor-annotationq
  @Captor private ArgumentCaptor<Optional<List<String>>> peerNames;

  private ResponseEntity<ClientResponseModel> response;

  @BeforeEach
  public void setup() throws IOException {
    response =
        new ResponseEntity<ClientResponseModel>(
            new ClientResponseModel(ErrorConstants.NO_ERROR, ""), HttpStatus.OK);
  }

  /* Write Transaction Tests */
  @Test
  public void writeTransactionToLedgerTest()
      throws IOException, ContractException, TimeoutException, InterruptedException {
    Mockito.when(gatewayConnetion.getNetwork(Mockito.anyString())).thenReturn(network);
    Mockito.when(network.getContract(Mockito.anyString())).thenReturn(contract);
    Mockito.when(contract.createTransaction(Mockito.anyString())).thenReturn(transaction);
    Mockito.when(transaction.getTransactionId()).thenReturn("TX-1234");
    Mockito.when(transaction.submit(Mockito.<String>any())).thenReturn(byteArrayResponse);
    Mockito.doNothing().when(recencyTransactionContext).setTransactionContext(Mockito.anyString());
    /*
     * Mockito.when(contract.submitTransaction(Mockito.anyString(),
     * Mockito.<String>any())) .thenReturn(byteArrayResponse);
     */
    assertEquals(
        response.getStatusCode(),
        transactionFulfillment
            .writeTransactionToLedger(
                testNetworkString,
                testContractString,
                testTransactionFunctionString,
                Optional.empty(),
                testTransactionParamsArrary)
            .getStatusCode());
  }

  @Test
  public void writeTransactionToLedgerContractExceptionTest()
      throws IOException, ContractException, TimeoutException, InterruptedException {
    Mockito.when(gatewayConnetion.getNetwork(Mockito.anyString())).thenReturn(network);
    Mockito.when(network.getContract(Mockito.anyString())).thenReturn(contract);
    Mockito.when(contract.createTransaction(Mockito.anyString())).thenReturn(transaction);
    Mockito.when(transaction.getTransactionId()).thenReturn("TX-1234");
    Mockito.when(transaction.submit(Mockito.<String>any())).thenThrow(new ContractException(""));
    Mockito.doNothing().when(recencyTransactionContext).setTransactionContext(Mockito.anyString());
    /*
     * Mockito.when(contract.submitTransaction(Mockito.anyString(),
     * Mockito.<String>any())) .thenThrow(new ContractException(""));
     */
    Assertions.assertThrows(
        FabricTransactionException.class,
        () -> {
          transactionFulfillment.writeTransactionToLedger(
              testNetworkString,
              testContractString,
              testTransactionFunctionString,
              Optional.empty(),
              testTransactionParamsArrary);
        });
  }

  @Test
  public void writeTransactionToLedgerTimeoutExceptionTest()
      throws IOException, ContractException, TimeoutException, InterruptedException {
    Mockito.when(gatewayConnetion.getNetwork(Mockito.anyString())).thenReturn(network);
    Mockito.when(network.getContract(Mockito.anyString())).thenReturn(contract);
    Mockito.when(contract.createTransaction(Mockito.anyString())).thenReturn(transaction);
    Mockito.when(transaction.getTransactionId()).thenReturn("TX-1234");
    Mockito.when(transaction.submit(Mockito.<String>any())).thenThrow(new TimeoutException());
    Mockito.doNothing().when(recencyTransactionContext).setTransactionContext(Mockito.anyString());
    /*
     * Mockito.when(contract.submitTransaction(Mockito.anyString(),
     * Mockito.<String>any())) .thenThrow(new TimeoutException());
     */
    Assertions.assertThrows(
        ServiceException.class,
        () -> {
          transactionFulfillment.writeTransactionToLedger(
              testNetworkString,
              testContractString,
              testTransactionFunctionString,
              Optional.empty(),
              testTransactionParamsArrary);
        });
  }

  @Test
  public void writeTransactionToLedgerInterruptExceptionTest()
      throws IOException, ContractException, TimeoutException, InterruptedException {
    Mockito.when(gatewayConnetion.getNetwork(Mockito.anyString())).thenReturn(network);
    Mockito.when(network.getContract(Mockito.anyString())).thenReturn(contract);
    Mockito.when(contract.createTransaction(Mockito.anyString())).thenReturn(transaction);
    Mockito.when(transaction.getTransactionId()).thenReturn("TX-1234");
    Mockito.when(transaction.submit(Mockito.<String>any())).thenThrow(new InterruptedException());
    Mockito.doNothing().when(recencyTransactionContext).setTransactionContext(Mockito.anyString());
    /*
     * Mockito.when(contract.submitTransaction(Mockito.anyString(),
     * Mockito.<String>any())) .thenThrow(new InterruptedException());
     */
    Assertions.assertThrows(
        FabricTransactionException.class,
        () -> {
          transactionFulfillment.writeTransactionToLedger(
              testNetworkString,
              testContractString,
              testTransactionFunctionString,
              Optional.empty(),
              testTransactionParamsArrary);
        });
  }

  /* Read Transaction Tests */
  @Test
  public void readTransactionFromLedgerTest() throws ContractException, IOException {
    Mockito.when(gatewayConnetion.getNetwork(Mockito.anyString())).thenReturn(network);
    Mockito.when(network.getContract(Mockito.anyString())).thenReturn(contract);
    Mockito.when(contract.evaluateTransaction(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(byteArrayResponse);
    assertEquals(
        response.getStatusCode(),
        transactionFulfillment
            .readTransactionFromLedger(
                testNetworkString,
                testContractString,
                testTransactionFunctionString,
                testTransactionIdString)
            .getStatusCode());
  }

  @Test
  public void readTransactionFromLedgerContractExceptionTest()
      throws IOException, ContractException, TimeoutException, InterruptedException {
    Mockito.when(gatewayConnetion.getNetwork(Mockito.anyString())).thenReturn(network);
    Mockito.when(network.getContract(Mockito.anyString())).thenReturn(contract);
    Mockito.when(contract.evaluateTransaction(Mockito.anyString(), Mockito.anyString()))
        .thenThrow(new ContractException(""));
    Assertions.assertThrows(
        FabricTransactionException.class,
        () -> {
          transactionFulfillment.readTransactionFromLedger(
              testNetworkString,
              testContractString,
              testTransactionFunctionString,
              testTransactionIdString);
        });
  }
}
