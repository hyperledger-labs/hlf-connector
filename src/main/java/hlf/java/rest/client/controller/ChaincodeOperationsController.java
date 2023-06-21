package hlf.java.rest.client.controller;

import hlf.java.rest.client.model.ChaincodeOperations;
import hlf.java.rest.client.model.ChaincodeOperationsType;
import hlf.java.rest.client.service.ChaincodeOperationsService;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.owasp.esapi.ESAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/chaincode")
public class ChaincodeOperationsController {

  @Autowired private ChaincodeOperationsService chaincodeOperationsService;

  @PutMapping(value = "/operations")
  public ResponseEntity<String> performChaincodeOperation(
      @RequestParam("network_name") @Validated String networkName,
      @RequestParam("operations_type") @Validated ChaincodeOperationsType operationsType,
      @RequestPart("chaincodeOperations") ChaincodeOperations chaincodeOperations,
      // accept optional collection configuration for the approval and commit
      @RequestPart(value = "collection_config", required = false)
          MultipartFile collectionConfigFile) {

    String operationResponse =
        chaincodeOperationsService.performChaincodeOperation(
            networkName,
            chaincodeOperations,
            operationsType,
            Optional.ofNullable(collectionConfigFile));

    operationResponse = ESAPI.encoder().encodeForHTML(operationResponse);

    return new ResponseEntity<>(operationResponse, HttpStatus.OK);
  }

  @GetMapping(value = "/sequence")
  public ResponseEntity<String> getCurrentSequence(
      @RequestParam("network_name") @Validated String networkName,
      @RequestParam("chaincode_name") @Validated String chaincodeName,
      @RequestParam("chaincode_version") @Validated String chaincodeVersion) {

    String operationResponse =
        chaincodeOperationsService.getCurrentSequence(networkName, chaincodeName, chaincodeVersion);

    operationResponse = ESAPI.encoder().encodeForHTML(operationResponse);

    return new ResponseEntity<>(operationResponse, HttpStatus.OK);
  }

  @GetMapping(value = "/packageId")
  public ResponseEntity<String> getCurrentPackageId(
      @RequestParam("network_name") @Validated String networkName,
      @RequestParam("chaincode_name") @Validated String chaincodeName,
      @RequestParam("chaincode_version") @Validated String chaincodeVersion) {

    String operationResponse =
        chaincodeOperationsService.getCurrentSequence(networkName, chaincodeName, chaincodeVersion);

    operationResponse = ESAPI.encoder().encodeForHTML(operationResponse);

    return new ResponseEntity<>(operationResponse, HttpStatus.OK);
  }

  @GetMapping(value = "/approved-organisations")
  public ResponseEntity<Set<String>> getApprovedOrganisationListForSmartContract(
      @RequestParam("network_name") @Validated String networkName,
      @RequestParam("chaincode_name") String chaincodeName,
      @RequestParam("chaincode_version") String chaincodeVersion,
      @RequestParam("sequence") Long sequence,
      @RequestParam(value = "init_required", defaultValue = "false") boolean initRequired) {
    ChaincodeOperations chaincodeOperations =
        ChaincodeOperations.builder()
            .chaincodeName(chaincodeName)
            .chaincodeVersion(chaincodeVersion)
            .sequence(sequence)
            .initRequired(initRequired)
            .build();

    Set<String> approvedOrganizations =
        chaincodeOperationsService.getApprovedOrganizations(
            networkName, chaincodeOperations, Optional.empty(), Optional.empty());

    return new ResponseEntity<>(approvedOrganizations, HttpStatus.OK);
  }
}
