package hlf.java.rest.client.exception;

import hlf.java.rest.client.model.ClientResponseModel;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final String EXCEPTION_OCCURRED_URL_MSG = "Exception Occurred :: URL = {}";

  /**
   * Returns a new {@link ClientResponseModel} from the passed Exception instance
   *
   * @param cause the Exception instance to be used for creating error response
   * @return a new error response model instance
   */
  private ResponseEntity<ClientResponseModel> getErrorResponse(
      Throwable cause, HttpStatus httpStatus) {

    Throwable rootCause = NestedExceptionUtils.getMostSpecificCause(cause);

    // The default errorCode and message values initialization
    int code = ErrorCode.NOT_DEFINED.getValue();
    String message = ErrorConstants.GENERIC_ERROR_MESSAGE;

    // If the wrapped exception (most specific cause) is thrown manually in the
    // application
    if (rootCause instanceof BaseException) {
      code = ((BaseException) rootCause).getCode().getValue();
      message = rootCause.getMessage();
    } else
    // If the exception occurred is manually thrown in the application code
    if (cause instanceof BaseException) {
      code = ((BaseException) cause).getCode().getValue();
      message = cause.getMessage();
    }
    return new ResponseEntity<>(new ClientResponseModel(code, message), httpStatus);
  }

  @ExceptionHandler(FabricTransactionException.class)
  private ResponseEntity<ClientResponseModel> handleTransactionException(
      HttpServletRequest request, Throwable cause) {
    log.error(EXCEPTION_OCCURRED_URL_MSG, request.getRequestURL());
    log.error("HyperledgerFabricTransaction Exception raised", cause);
    return getErrorResponse(cause, HttpStatus.CONFLICT);
  }

  @ExceptionHandler(ServiceException.class)
  private ResponseEntity<ClientResponseModel> handleServiceException(
      HttpServletRequest request, Throwable cause) {
    log.error(EXCEPTION_OCCURRED_URL_MSG, request.getRequestURL());
    log.error("Service Exception raised", cause);
    return getErrorResponse(cause, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(NotFoundException.class)
  private ResponseEntity<ClientResponseModel> handleNotFoundException(
      HttpServletRequest request, Throwable cause) {
    log.error(EXCEPTION_OCCURRED_URL_MSG, request.getRequestURL());
    log.error("Not Found Exception raised", cause);
    return getErrorResponse(cause, HttpStatus.NOT_FOUND);
  }

  /**
   * This ExceptionHandler handles all the remaining exception and returns 500
   * (INTERNAL_SERVER_ERROR)
   *
   * @param request the {@link HttpServletRequest} which caused this error
   * @param cause the {@link Exception} to be handled
   * @return the {@link ResponseEntity} having status code as {@link
   *     HttpStatus#INTERNAL_SERVER_ERROR} and body as an instance of ErrorResponseModel
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ClientResponseModel> handleDefaultException(
      HttpServletRequest request, Throwable cause) {

    log.error("Exception Exception raised : ", cause);
    return getErrorResponse(cause, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
