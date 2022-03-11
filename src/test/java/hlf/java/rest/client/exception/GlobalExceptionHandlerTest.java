package hlf.java.rest.client.exception;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hlf.java.rest.client.config.GatewayConfig;
import hlf.java.rest.client.service.TransactionFulfillment;
import hlf.java.rest.client.service.impl.ChaincodeOperationsServiceImpl;
import org.hyperledger.fabric.gateway.ContractException;
import org.hyperledger.fabric.gateway.Gateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class GlobalExceptionHandlerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean TransactionFulfillment transactionFulfillment;

  @MockBean ChaincodeOperationsServiceImpl chaincodeOperationsService;

  @MockBean GatewayConfig gatewayConfig;

  @MockBean Gateway gateway;

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
