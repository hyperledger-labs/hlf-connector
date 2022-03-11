package hlf.java.rest.client.controller;

import hlf.java.rest.client.model.ChaincodeOperations;
import hlf.java.rest.client.model.ChaincodeOperationsType;
import hlf.java.rest.client.service.ChaincodeOperationsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class FabricOperationsController {

  @Autowired private ChaincodeOperationsService chaincodeOperationsService;

  @PutMapping(value = "/chaincode/operations")
  public ResponseEntity<String> performChaincodeOperation(
      @RequestParam("network_name") @Validated String networkName,
      @RequestParam("operations_type") @Validated ChaincodeOperationsType operationsType,
      @RequestBody ChaincodeOperations chaincodeOperations) {
    return new ResponseEntity<>(
        chaincodeOperationsService.performChaincodeOperation(
            networkName, chaincodeOperations, operationsType),
        HttpStatus.OK);
  }
}
