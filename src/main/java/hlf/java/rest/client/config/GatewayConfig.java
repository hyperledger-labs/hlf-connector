package hlf.java.rest.client.config;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configure Gateway connection for the Fabric network. */
@Slf4j
@Configuration
public class GatewayConfig {

  @Autowired ApplicationProperties applicationProperties;

  /**
   * Create the gateway connection for connecting to the peer.
   *
   * @return gateway Gateway object to connect to Fabric network
   * @throws IOException
   */
  @Bean
  public Gateway createGatewayConnection() throws IOException {
    Wallet wallet = obtainWallet();
    // Load the Network Connection Configuration path
    Path networkConfigPath =
        Paths.get(
            applicationProperties.getOrgConnectionConfigPath(),
            applicationProperties.getOrgConnectionConfigFilename());
    // Create the gateway builder based on the path to the org configuration file
    // using the specified user, then connect
    Gateway.Builder builder = Gateway.createBuilder();
    builder
        .identity(wallet, applicationProperties.getWalletClientUserName())
        .networkConfig(networkConfigPath)
        .discovery(true);
    return builder.connect();
  }

  /**
   * Obtain the Wallet details containing the user id information.
   *
   * @return wallet Wallet pull credentials from wallet
   * @throws IOException
   */
  @Bean
  public Wallet obtainWallet() throws IOException {
    log.info("Obtain the Wallet containing Admin and Client user information");
    // Load a file system based wallet for managing identities.
    Path walletPath = Paths.get(applicationProperties.getWalletPath());
    return Wallets.newFileSystemWallet(walletPath);
  }
}
