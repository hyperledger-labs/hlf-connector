package hlf.java.rest.client.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import hlf.java.rest.client.config.ApplicationProperties;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockedStatic.Verification;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.annotation.DirtiesContext;

@ExtendWith(MockitoExtension.class)
@DirtiesContext
public class GatewayBuilderSingletonTest {

  @InjectMocks GatewayBuilderSingleton gatewayBuilderSingleton;
  @Mock ApplicationProperties applicationProperties;
  @Mock Path walletPath;
  @Mock Path networkPath;
  @Mock Gateway.Builder gatewayBuilder;
  @Mock Wallet wallet;
  private String testNetworkPathString = "some network path";
  private String testNetworkNameString = "some network name";
  private String testWalletFilePathString = "some wallet path";
  private String testClientUserNameString = "some user name";

  static MockedStatic<Paths> pathsMockStatic;
  static MockedStatic<Gateway> gatewayMockStatic;
  static MockedStatic<Wallets> walletsMockStatic;

  @Test
  public void getGatewayConnectionFirstRunTest() throws IOException {
    Gateway gatewayMocked = Mockito.mock(Gateway.class);
    Mockito.when(applicationProperties.getOrgConnectionConfigPath())
        .thenReturn(testNetworkPathString);
    Mockito.when(applicationProperties.getOrgConnectionConfigFilename())
        .thenReturn(testNetworkNameString);
    Mockito.when(applicationProperties.getWalletClientUserName())
        .thenReturn(testClientUserNameString);
    Mockito.when(applicationProperties.getWalletPath()).thenReturn(testWalletFilePathString);

    pathsMockStatic = Mockito.mockStatic(Paths.class);
    gatewayMockStatic = Mockito.mockStatic(Gateway.class);
    walletsMockStatic = Mockito.mockStatic(Wallets.class);

    pathsMockStatic.when((Verification) Paths.get(Mockito.anyString())).thenReturn(walletPath);
    pathsMockStatic
        .when((Verification) Paths.get(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(networkPath);
    walletsMockStatic
        .when((Verification) Wallets.newFileSystemWallet(Mockito.<Path>any()))
        .thenReturn(wallet);
    gatewayMockStatic.when((Verification) Gateway.createBuilder()).thenReturn(gatewayBuilder);

    Mockito.when(gatewayBuilder.identity(Mockito.<Wallet>any(), Mockito.anyString()))
        .thenReturn(gatewayBuilder);
    Mockito.when(gatewayBuilder.networkConfig(Mockito.<Path>any())).thenReturn(gatewayBuilder);
    Mockito.when(gatewayBuilder.discovery(Mockito.anyBoolean())).thenReturn(gatewayBuilder);
    Mockito.when(gatewayBuilder.connect()).thenReturn(gatewayMocked);

    assertEquals(gatewayMocked, gatewayBuilderSingleton.getGatewayConnection());
  }

  @Test
  public void getGatewayConnectionCachedTest() throws IOException {
    Gateway gatewayMocked = Mockito.mock(Gateway.class);
    gatewayBuilderSingleton.setGatewayConnetion(gatewayMocked);

    verify(applicationProperties, never()).getWalletPath();
    assertEquals(gatewayMocked, gatewayBuilderSingleton.getGatewayConnection());
  }

  @AfterAll
  public static void tearDown() {
    pathsMockStatic.close();
    gatewayMockStatic.close();
    walletsMockStatic.close();
  }
}
