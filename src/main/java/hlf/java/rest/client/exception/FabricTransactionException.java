package hlf.java.rest.client.exception;

/**
 * Used to represent all service level exceptions coming from the Hyperledger SDK
 *
 * @author c0c00ub
 */
public class FabricTransactionException extends BaseException {

  private static final long serialVersionUID = -1162154215509853616L;

  public FabricTransactionException(ErrorCode code, String message) {
    super(code, message);
  }

  public FabricTransactionException(ErrorCode code, String message, Throwable cause) {
    super(code, message, cause);
  }
}
