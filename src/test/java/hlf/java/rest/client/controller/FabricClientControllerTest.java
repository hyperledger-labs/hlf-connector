package hlf.java.rest.client.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hlf.java.rest.client.exception.ErrorConstants;
import hlf.java.rest.client.model.ClientResponseModel;
import hlf.java.rest.client.service.TransactionFulfillment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

@ExtendWith(MockitoExtension.class)
@DirtiesContext
public class FabricClientControllerTest {

  @InjectMocks FabricClientController fabricClientController;

  @Mock TransactionFulfillment transactionFulfillment;

  private String testNetworkString = "some string";
  private String testContractString = "some string";
  private String testTransactionFunctionString = "some string";
  private String[] testTransactionParamsArrary = {"test param", "test param 2"};
  private String testTransactionIdString = "some string";
  private String testTransactionCollection = "some string";
  private String testTransactionTransientKey = "some string";
  private static ResponseEntity<ClientResponseModel> response;

  @BeforeAll
  public static void setup() {
    response =
        new ResponseEntity<ClientResponseModel>(
            new ClientResponseModel(ErrorConstants.NO_ERROR, ""), HttpStatus.OK);
  }

  @Test
  public void postTransactionTest() {
    Mockito.when(
            transactionFulfillment.writeTransactionToLedger(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.<String>any()))
        .thenReturn(response);
    assertEquals(
        response,
        fabricClientController.postTransaction(
            testNetworkString,
            testContractString,
            testTransactionFunctionString,
            testTransactionParamsArrary));
  }

  @Test
  public void getTransactionTest() {
    Mockito.when(
            transactionFulfillment.readTransactionFromPrivateLedger(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyString()))
        .thenReturn(response);
    assertEquals(
        response,
        fabricClientController.getTransaction(
            testNetworkString,
            testContractString,
            testTransactionFunctionString,
            testTransactionCollection,
            testTransactionTransientKey,
            testTransactionIdString));
  }
}
