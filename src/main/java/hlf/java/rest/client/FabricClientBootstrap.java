package hlf.java.rest.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/** Bootstrap for the Fabric-Client application */
@Slf4j
@SpringBootApplication
@EnableConfigurationProperties
public class FabricClientBootstrap extends SpringBootServletInitializer {

  public static void main(String[] args) {
    SpringApplication.run(FabricClientBootstrap.class, args);
  }

  /**
   * Configure spring application builder.
   *
   * @param builder the builder
   * @return the spring application builder
   */
  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
    log.info("Action : Starting : Servlet Initializer.");
    return builder.sources(FabricClientBootstrap.class);
  }
}
