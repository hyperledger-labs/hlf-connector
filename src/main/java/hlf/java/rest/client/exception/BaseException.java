package hlf.java.rest.client.exception;

/**
 * BaseException class which is created as abstract and will be super class of all our exception
 * classes.
 */
public abstract class BaseException extends RuntimeException {

  private static final long serialVersionUID = 2554714415375055302L;

  /** Uniquely identifies the exception instance thrown in code execution */
  private final ErrorCode code;

  /**
   * Constructs a new BaseException exception with the specified detail message and {@link #code}
   *
   * @param code the {@link ErrorCode} to identify this exception instance
   * @param message the detailed message about the exception
   */
  public BaseException(ErrorCode code, String message) {
    super(message);
    this.code = code;
  }

  /**
   * Constructs a new Base exception with the specified detail message, cause and {@link #code}
   *
   * <p>Note that the detail message associated with {@link #getCause()} is <i>not</i> automatically
   * incorporated in this runtime exception's detail message.
   *
   * @param code the error code
   * @param message the detail message
   * @param cause the cause (A null value is permitted, and indicates that the cause is nonexistent
   *     or unknown.)
   */
  public BaseException(ErrorCode code, String message, Throwable cause) {
    super(message, cause);
    this.code = code;
  }

  public ErrorCode getCode() {
    return code;
  }
}
