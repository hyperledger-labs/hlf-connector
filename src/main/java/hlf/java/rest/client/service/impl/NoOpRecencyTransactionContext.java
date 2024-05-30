package hlf.java.rest.client.service.impl;

import hlf.java.rest.client.service.RecencyTransactionContext;

public class NoOpRecencyTransactionContext implements RecencyTransactionContext {
  @Override
  public void setTransactionContext(String transactionId) {
    // NO-OP
  }

  @Override
  public boolean validateAndRemoveTransactionContext(String transactionId) {
    return true;
  }
}
