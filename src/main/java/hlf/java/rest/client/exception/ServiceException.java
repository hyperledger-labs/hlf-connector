package hlf.java.rest.client.exception;

/** ServiceException class is for all our service exceptions. */
public class ServiceException extends BaseException {

  private static final long serialVersionUID = 8145724920013541503L;

  public ServiceException(ErrorCode code, String message) {
    super(code, message);
  }

  public ServiceException(ErrorCode code, String message, Throwable cause) {
    super(code, message, cause);
  }
}
