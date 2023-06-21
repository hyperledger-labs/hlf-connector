package hlf.java.rest.client.config;

import hlf.java.rest.client.security.HeaderAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * SecurityConfig provides configuration which allows http calls which can be accessed by
 * application
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

  @Autowired private HandlerExceptionResolver handlerExceptionResolver;

  @Autowired private FabricProperties fabricProperties;

  /**
   * The configure method sets the permission limits to the Http request. Also, filter has been
   * configured which intercepts each request.
   */
  @Override
  protected void configure(HttpSecurity http) throws Exception {
    // @formatter:off
    // Enable header based authentication for all the requests
    http.requestMatchers()
        .anyRequest()
        .and()
        .addFilterBefore(
            new HeaderAuthenticationFilter(fabricProperties, handlerExceptionResolver),
            LogoutFilter.class)
        .csrf()
        .disable() // NOSONAR
        .headers()
        .xssProtection();
    // @formatter:on
  }

  /**
   * This configure method helps in ignoring spring security for particular http requests. This is
   * required because of the filter usage.
   *
   * @param web the web
   */
  @Override
  public void configure(WebSecurity web) {
    // Enable Swagger Access
    web.ignoring()
        .antMatchers(
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs/swagger-config",
            "/webjars/**",
            "/actuator/**",
            "/open-api.*");
  }
}
