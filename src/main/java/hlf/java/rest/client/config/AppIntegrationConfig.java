package hlf.java.rest.client.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
public class AppIntegrationConfig {

  @PropertySource(
      value = {
        "${kafka.integration.jaasConfigFile}",
        "${kafka.event-listener.jaasConfigFile}",
        "${fabric.client.rest.apikeyFile}"
      },
      ignoreResourceNotFound = true)
  @Configuration
  static class PropertiesConfig {}
}
