package hlf.java.rest.client.service;

public interface RecencyTransactionContext {

  void setTransactionContext(String transactionId);

  boolean validateAndRemoveTransactionContext(String transactionId);
}
