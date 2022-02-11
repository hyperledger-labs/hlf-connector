package hlf.java.rest.client.exception;

public class ChannelOperationException extends BaseException {

  public ChannelOperationException(ErrorCode code) {
    super(code, code.getReason());
  }

  public ChannelOperationException(ErrorCode code, String message) {
    super(code, message);
  }

  public ChannelOperationException(ErrorCode code, String message, Throwable cause) {
    super(code, message, cause);
  }
}
