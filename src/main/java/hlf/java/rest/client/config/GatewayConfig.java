package hlf.java.rest.client.config;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallets;
import org.hyperledger.fabric.gateway.impl.identity.X509IdentityProvider;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configure Gateway connection for the Fabric network. */
@Slf4j
@Configuration
public class GatewayConfig {

  @Autowired FabricProperties fabricProperties;

  /**
   * Create the gateway connection for connecting to the peer.
   *
   * @return gateway Gateway object to connect to Fabric network
   * @throws IOException
   */
  @Bean
  public Gateway gateway(Wallet wallet) throws IOException {
    // Load the Network Connection Configuration path
    Path networkConfigPath =
        Paths.get(
            fabricProperties.getOrgConnectionConfig().getPath(),
            fabricProperties.getOrgConnectionConfig().getFilename());
    // Create the gateway builder based on the path to the org configuration file
    // using the specified user, then connect
    Gateway.Builder builder = Gateway.createBuilder();
    builder
        .identity(wallet, fabricProperties.getWallet().getClientUser().getName())
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
  public Wallet wallet() throws IOException {
    log.info("Obtain the Wallet containing Admin and Client user information");
    // Load a file system based wallet for managing identities.
    Path walletPath = Paths.get(fabricProperties.getWallet().getPath());
    return Wallets.newFileSystemWallet(walletPath);
  }

  /**
   * Hf client for operations APIs provided by fabric sdk.
   *
   * @param gateway the gateway
   * @return the hf client
   * @throws InvalidArgumentException the invalid argument exception
   * @throws CryptoException the crypto exception
   * @throws ClassNotFoundException the class not found exception
   * @throws InvocationTargetException the invocation target exception
   * @throws IllegalAccessException the illegal access exception
   * @throws InstantiationException the instantiation exception
   * @throws NoSuchMethodException the no such method exception
   */
  @Bean
  public HFClient hfClient(Gateway gateway)
      throws InvalidArgumentException, CryptoException, ClassNotFoundException,
          InvocationTargetException, IllegalAccessException, InstantiationException,
          NoSuchMethodException {
    log.info("Setting up HFClient for operations APIs.");
    HFClient hfClient = HFClient.createNewInstance();
    hfClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
    X509IdentityProvider.INSTANCE.setUserContext(hfClient, gateway.getIdentity(), "hlf-connector");
    return hfClient;
  }

  @Bean
  public User user(HFClient hfClient) throws IOException {
    return hfClient.getUserContext();
  }
}
