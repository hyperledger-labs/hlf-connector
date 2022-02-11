package hlf.java.rest.client.exception;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hlf.java.rest.client.controller.FabricClientController;
import hlf.java.rest.client.service.TransactionFulfillment;
import org.hyperledger.fabric.gateway.ContractException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(SpringExtension.class)
@WebMvcTest(
    controllers = {FabricClientController.class},
    properties = {"fabric.client.rest.apikey=ePVYHwAaQ0V1XOTX6U"})
@DirtiesContext
@ComponentScan(basePackages = "hlf.java.rest.client")
public class GlobalExceptionHandlerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean TransactionFulfillment transactionFulfillment;

  private String testNetworkString = "some string";
  private String testContractString = "some string";
  private String testTransactionFunctionString = "some string";
  private String[] testTransactionParamsArrary = {"test param", "test param 2"};

  @Test
  public void checkHyperledgerFabricExceptionTest() throws Exception {
    when(transactionFulfillment.writeTransactionToLedger(
            anyString(), anyString(), anyString(), any()))
        .thenThrow(
            new FabricTransactionException(
                ErrorCode.HYPERLEDGER_FABRIC_CONNECTION_ERROR,
                "some message",
                new ContractException("some message")));
    mockMvc
        .perform(
            get("/write_transaction")
                .param("network_name", testNetworkString)
                .param("contract_name", testContractString)
                .param("transaction_function_name", testTransactionFunctionString)
                .param("transaction_params", testTransactionParamsArrary)
                .header("api-key", "ePVYHwAaQ0V1XOTX6U"))
        .andExpect(status().isConflict())
        .andReturn();
  }

  @Test
  public void checkServiceFabricExceptionTest() throws Exception {
    doThrow(new ServiceException(ErrorCode.HYPERLEDGER_FABRIC_CONNECTION_ERROR, "some message"))
        .when(transactionFulfillment)
        .writeTransactionToLedger(anyString(), anyString(), anyString(), any());
    mockMvc
        .perform(
            get("/write_transaction")
                .param("network_name", testNetworkString)
                .param("contract_name", testContractString)
                .param("transaction_function_name", testTransactionFunctionString)
                .param("transaction_params", testTransactionParamsArrary)
                .header("api-key", "ePVYHwAaQ0V1XOTX6U"))
        .andExpect(status().isInternalServerError())
        .andReturn();
  }
}
