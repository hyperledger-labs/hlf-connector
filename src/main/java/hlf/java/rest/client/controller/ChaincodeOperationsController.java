package hlf.java.rest.client.controller;

import hlf.java.rest.client.model.ChaincodeOperations;
import hlf.java.rest.client.model.ChaincodeOperationsType;
import hlf.java.rest.client.service.ChaincodeOperationsService;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/chaincode")
public class ChaincodeOperationsController {

  @Autowired private ChaincodeOperationsService chaincodeOperationsService;

  @PutMapping(value = "/operations")
  public ResponseEntity<String> performChaincodeOperation(
      @RequestParam("network_name") @Validated String networkName,
      @RequestParam("operations_type") @Validated ChaincodeOperationsType operationsType,
      @RequestBody ChaincodeOperations chaincodeOperations) {
    return new ResponseEntity<>(
        chaincodeOperationsService.performChaincodeOperation(
            networkName, chaincodeOperations, operationsType),
        HttpStatus.OK);
  }

  @GetMapping(value = "/sequence")
  public ResponseEntity<String> getCurrentSequence(
      @RequestParam("network_name") @Validated String networkName,
      @RequestParam("chaincode_name") @Validated String chaincodeName,
      @RequestParam("chaincode_version") @Validated String chaincodeVersion) {
    return new ResponseEntity<>(
        chaincodeOperationsService.getCurrentSequence(networkName, chaincodeName, chaincodeVersion),
        HttpStatus.OK);
  }

  @GetMapping(value = "/packageId")
  public ResponseEntity<String> getCurrentPackageId(
      @RequestParam("network_name") @Validated String networkName,
      @RequestParam("chaincode_name") @Validated String chaincodeName,
      @RequestParam("chaincode_version") @Validated String chaincodeVersion) {
    return new ResponseEntity<>(
        chaincodeOperationsService.getCurrentPackageId(
            networkName, chaincodeName, chaincodeVersion),
        HttpStatus.OK);
  }

  @GetMapping(value = "/approved-organisations")
  public ResponseEntity<Set<String>> getApprovedOrganisationListForSmartContract(
      @RequestParam("network_name") @Validated String networkName,
      @RequestParam("chaincode_name") String chaincodeName,
      @RequestParam("chaincode_version") String chaincodeVersion,
      @RequestParam("sequence") Long sequence) {
    ChaincodeOperations chaincodeOperations =
        ChaincodeOperations.builder()
            .chaincodeName(chaincodeName)
            .chaincodeVersion(chaincodeVersion)
            .sequence(sequence)
            .build();
    return new ResponseEntity<>(
        chaincodeOperationsService.getApprovedOrganizations(
            networkName, chaincodeOperations, Optional.empty(), Optional.empty()),
        HttpStatus.OK);
  }
}
