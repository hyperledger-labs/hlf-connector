package hlf.java.rest.client.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * ApplicationProperties reads and binds with the application.yml and provide all the configuration
 * as a bean configuration To use any configuration, just autowire and call the associated "get"
 * method
 */
@Getter
@Component
public class ApplicationProperties {

  @Value("${fabric.localhostReportAddress}")
  private String localhostReportAddress;

  @Value("${fabric.wallet.path}")
  private String walletPath;

  @Value("${fabric.wallet.adminUser.name}")
  private String walletAdminUserName;

  @Value("${fabric.wallet.clientUser.name}")
  private String walletClientUserName;

  @Value("${fabric.orgConnectionConfig.path}")
  private String orgConnectionConfigPath;

  @Value("${fabric.orgConnectionConfig.filename}")
  private String orgConnectionConfigFilename;

  @Value("${fabric.client.rest.apikey}")
  private String restApiKey;
}
