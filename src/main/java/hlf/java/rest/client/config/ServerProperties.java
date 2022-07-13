package hlf.java.rest.client.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@RefreshScope
public class ServerProperties {

  @Value("${server.config.location}")
  private String configLocation;
}
