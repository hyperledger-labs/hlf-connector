package hlf.java.rest.client.service.impl;

import hlf.java.rest.client.config.ApplicationProperties;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Singleton implementation of the GatewayBuilder object instantiation for establishing the
 * connection from the client to the peer node.
 *
 * @author c0c00ub
 */
@Slf4j
@Component
public class GatewayBuilderSingleton {

  @Autowired ApplicationProperties applicationProperties;

  private Gateway gatewayConnetion;

  private GatewayBuilderSingleton() {}

  /**
   * Obtain the Gateway connection if it already exists, establish the connection then obtain it if
   * the connection does not.
   *
   * @return gatewayConnetion Gateway object to connect to Fabric network
   * @throws IOException Wallet files not available
   */
  public Gateway getGatewayConnection() throws IOException {
    if (gatewayConnetion == null) {
      gatewayConnetion = createGatewayConnection();
    }
    return gatewayConnetion;
  }

  /**
   * Create the gateway connection for connecting to the peer.
   *
   * @return gateway Gateway object to connect to Fabric network
   * @throws IOException
   */
  private Gateway createGatewayConnection() throws IOException {
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
  private Wallet obtainWallet() throws IOException {
    log.info("Obtain the Wallet containing Admin and Client user information");
    // Load a file system based wallet for managing identities.
    Path walletPath = Paths.get(applicationProperties.getWalletPath());
    return Wallets.newFileSystemWallet(walletPath);
  }

  /**
   * For Testing Only: Set the gateway connection explicitly
   *
   * @param gatewayConnetion Gateway Object for testing
   */
  public void setGatewayConnetion(Gateway gatewayConnetion) {
    this.gatewayConnetion = gatewayConnetion;
  }
}
