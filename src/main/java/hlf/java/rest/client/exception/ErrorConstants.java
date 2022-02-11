package hlf.java.rest.client.exception;

public final class ErrorConstants {

  /** Default value used by spring-cloud-sleuth to save the span id in Slf4j MDC */
  public static final String LOG_SPAN_ID = "X-B3-SpanId";

  /**
   * The generic error message to be used by {@link GlobalExceptionHandler}
   *
   * <p>When we encounter any exception, which is not a {@link BaseException}, and the most specific
   * cause is also not a {@link BaseException}, then we will return this error message, instead of
   * returning the technical error message
   */
  public static final String GENERIC_ERROR_MESSAGE =
      "An error has occurred. Please contact the support team.";

  public static final Integer NO_ERROR = null;

  private ErrorConstants() {}
}
