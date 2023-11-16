package hlf.java.rest.client.exception;

public class RetryableServiceException extends ServiceException {

  public RetryableServiceException(ErrorCode code, String message, Throwable cause) {
    super(code, message, cause);
  }
}
