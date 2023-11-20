package hlf.java.rest.client.exception;

/**
 * ErrorCode enum will help us to uniquely identify each and every exception which has been manually
 * thrown in the execution flow
 */
public enum ErrorCode {
  SUCCESS(0000, "Submission successful"),
  // @formatter:off
  NOT_FOUND(1000, "Not able to find the requested resource data"),

  NO_EVENTS_FOUND(1001, "Not able to find any events for the passed input."),

  DESERIALIZATION_FAILURE(2000, "Error while deserializing an object"),

  NOT_SUPPORTED(1002, "Operation passed not supported."),

  VALIDATION_FAILED(1003, "The data passed fails validation from the resource."),

  SEQUENCE_NUMBER_VALIDATION_FAILED(
      1004, "The sequence number is not same in all the peers or not present."),

  CHAINCODE_PACKAGE_ID_VALIDATION_FAILED(
      1005, "The chaincode packageId is not same in all the peers or not present."),

  CHANNEL_CREATION_FAILED(3000, "Failed to create Channel"),

  CHANNEL_JOIN_FAILED(3006, "Failed to join channel"),

  CHANNEL_CREATION_MISSING_ORDERER(4000, "Orderer is missing"),

  CHANNEL_CREATION_MISSING_PEER(4001, "Peer is missing"),

  CHANNEL_NAME_MISSING(4002, "Channel name is missing"),

  CHANNEL_CREATION_MISSING_CONSORTIUM_NAME(4003, "Consortium name is missing"),

  CHANNEL_CREATION_CHANNEL_EXISTS(4005, "Channel exists"),

  CHANNEL_MISSING_CERTIFICATE(4006, "Certificate is missing"),
  CHANNEL_NOT_FOUND(4007, "Channel does not exists"),

  CHANNEL_PAYLOAD_ERROR(4999, "Unknown error"),

  HYPERLEDGER_FABRIC_CONNECTION_ERROR(5000, "Hyperledger Fabric connection related error"),

  HYPERLEDGER_FABRIC_CHANNEL_TXN_ERROR(
      5001, "Hyperledger Fabric channel transaction error by block Number"),

  HYPERLEDGER_FABRIC_CHAINCODE_OPERATIONS_REQUEST_REJECTION(
      5002,
      "Hyperledger Fabric chaincode operations request has illegal argument or argument is missing."),

  HYPERLEDGER_FABRIC_CONNECTION_TIMEOUT_ERROR(
      5000, "Hyperledger Fabric Connection timed-out during Transaction"),

  HYPERLEDGER_FABRIC_TRANSACTION_ERROR(6000, "Hyperledger Fabric transaction related error"),

  HYPERLEDGER_FABRIC_NOT_SUPPORTED(8000, "In Hyperledger Fabric this feature is not supported"),

  AUTH_INVALID_API_KEY(9000, "Invalid api key"),

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
