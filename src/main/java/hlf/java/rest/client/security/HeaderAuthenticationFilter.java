package hlf.java.rest.client.security;

import static hlf.java.rest.client.exception.ErrorCode.AUTH_INVALID_API_KEY;

import hlf.java.rest.client.config.FabricProperties;
import hlf.java.rest.client.exception.AuthenticationFailureException;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Slf4j
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

  private FabricProperties fabricProperties;

  private HandlerExceptionResolver handlerExceptionResolver;

  /**
   * Instantiates a new Header authentication filter.
   *
   * @param fabricProperties the application
   * @param handlerExceptionResolver the handler exception resolver
   */
  public HeaderAuthenticationFilter(
      FabricProperties fabricProperties, HandlerExceptionResolver handlerExceptionResolver) {
    this.fabricProperties = fabricProperties;
    this.handlerExceptionResolver = handlerExceptionResolver;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
    try {
      authenticate(request);
      filterChain.doFilter(request, response);
    } catch (Exception ex) {
      // This resolver will take care of handling all the AuthenticationExceptions
      // and delegating the handling of the response to appropriate exception handler
      handlerExceptionResolver.resolveException(request, response, null, ex);
    }
  }

  /**
   * This function checks if apiKey and userId are part of header and verifies apikey against apiKey
   * configured for the application.
   *
   * <p>Also we set the userId as part of SecurityContextHolder
   *
   * @param request HttpServletRequest
   */
  private void authenticate(HttpServletRequest request) {

    String apiKey = request.getHeader("api-key");
    if (!fabricProperties.getClient().getRest().getApikey().equals(apiKey)) {
      log.debug("API Key does not match");
      throw new AuthenticationFailureException(AUTH_INVALID_API_KEY, "Invalid API Key");
    }
  }
}
