package hlf.java.rest.client.service;

import hlf.java.rest.client.model.ChaincodeOperations;
import hlf.java.rest.client.model.ChaincodeOperationsType;

public interface ChaincodeOperationsService {

  String performChaincodeOperation(
      String networkName,
      ChaincodeOperations chaincodeOperationsModel,
      ChaincodeOperationsType operationsType);

  String getCurrentSequence(String networkName, String chaincodeName, String chaincodeVersion);
}
