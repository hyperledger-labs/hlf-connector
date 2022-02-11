package hlf.java.rest.client.exception;

public class NotFoundException extends BaseException {
  private static final long serialVersionUID = 2554714419375055302L;

  public NotFoundException(ErrorCode code, String message) {
    super(code, message);
  }

  public NotFoundException(ErrorCode code, String message, Throwable cause) {
    super(code, message, cause);
  }
}
