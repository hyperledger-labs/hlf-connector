package hlf.java.rest.client.exception;

/**
 * ErrorCode enum will help us to uniquely identify each and every exception which has been manually
 * thrown in the execution flow
 */
public enum ErrorCode {

  // @formatter:off
  NOT_FOUND(1000, "Not able to find the requested resource data"),

  NO_EVENTS_FOUND(1001, "Not able to find any events for the passed input."),

  NOT_SUPPORTED(1002, "Operation passed not supported."),

  VALIDATION_FAILED(1003, "The data passed fails validation from the resource."),

  SEQUENCE_NUMBER_VALIDATION_FAILED(
      1004, "The sequence number is not same in all the peers or not present."),

  HYPERLEDGER_FABRIC_CONNECTION_ERROR(5000, "Hyperledger Fabric connection related error"),

  HYPERLEDGER_FABRIC_CHANNEL_TXN_ERROR(
      5001, "Hyperledger Fabric channel transaction error by block Number"),

  HYPERLEDGER_FABRIC_CHAINCODE_OPERATIONS_REQUEST_REJECTION(
      5002,
      "Hyperledger Fabric chaincode operations request has illegal argument or argument is missing."),

  HYPERLEDGER_FABRIC_TRANSACTION_ERROR(6000, "Hyperledger Fabric transaction related error"),

  HYPERLEDGER_FABRIC_NOT_SUPPORTED(8000, "In Hyperledger Fabric this feature is not supported"),

  AUTH_INVALID_API_KEY(9000, "Invalid api key"),

  AUTH_EMPTY_USER_ID(9001, "Empty Or null User Id"),

  NOT_DEFINED(
      9999,
      "The exception is not a BaseException OR error code is not yet defined by the developer");
  // @formatter:on

  private final int value;
  private final String reason;

  ErrorCode(int value, String reason) {
    this.value = value;
    this.reason = reason;
  }

  public int getValue() {
    return value;
  }

  public String getReason() {
    return reason;
  }
}
