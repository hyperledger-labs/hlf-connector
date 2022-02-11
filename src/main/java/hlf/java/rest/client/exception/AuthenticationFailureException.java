package hlf.java.rest.client.exception;

public class AuthenticationFailureException extends BaseException {
  private static final long serialVersionUID = 2554714419375055302L;

  public AuthenticationFailureException(ErrorCode code, String message) {
    super(code, message);
  }

  public AuthenticationFailureException(ErrorCode code, String message, Throwable cause) {
    super(code, message, cause);
  }
}
