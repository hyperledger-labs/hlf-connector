package hlf.java.rest.client.exception;

public class UnrecognizedTransactionPayloadException extends BaseException {

  private static final long serialVersionUID = 6585724920013541503L;

  public UnrecognizedTransactionPayloadException(ErrorCode code, String message) {
    super(code, message);
  }
}
