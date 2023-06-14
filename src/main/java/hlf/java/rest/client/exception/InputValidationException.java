package hlf.java.rest.client.exception;

public class InputValidationException extends BaseException {

  private static final long serialVersionUID = -1162154215509853616L;

  public InputValidationException(ErrorCode code, String message) {
    super(code, message);
  }

  public InputValidationException(ErrorCode code, String message, Throwable cause) {
    super(code, message, cause);
  }
}
