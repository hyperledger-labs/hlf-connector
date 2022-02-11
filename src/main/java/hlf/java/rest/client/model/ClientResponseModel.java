package hlf.java.rest.client.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import hlf.java.rest.client.exception.ErrorConstants;
import java.io.Serializable;
import java.time.Instant;
import lombok.Data;
import org.slf4j.MDC;

/**
 * ErrorResponseModel class is used as the standard response body for all the successful responses
 * or exceptions handled by the {@link hlf.java.rest.client.exception.GlobalExceptionHandler}
 */
@Data
public class ClientResponseModel {

  public ClientResponseModel(Integer code, Serializable message) {
    Instant instant = Instant.now();
    this.timestamp = instant.toString();
    this.fabricTransactionId = MDC.get(ErrorConstants.LOG_SPAN_ID);
    this.code = code;
    this.message = message;
  }

  public ClientResponseModel(String errorId, Integer code, Serializable message) {
    Instant instant = Instant.now();
    this.timestamp = instant.toString();
    this.fabricTransactionId = errorId;
    this.code = code;
    this.message = message;
  }

  /** End timestamp of the response in UTC format * */
  private String timestamp;

  /**
   * An UUID value which can be used to trace the log of all the events which led to this response
   * or error
   */
  private String fabricTransactionId;

  @JsonInclude(value = Include.NON_NULL)
  /** Used to uniquely identify the exception instance causing this error */
  private Integer code;

  /**
   * The details of response. It can be a detailed message string OR a java class having in-depth
   * details of this error
   */
  private Serializable message;
}
