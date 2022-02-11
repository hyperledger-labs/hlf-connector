package hlf.java.rest.client.util;

import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.Properties;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.gateway.Identities;
import org.hyperledger.fabric.gateway.Identity;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallets;
import org.hyperledger.fabric.gateway.X509Identity;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric.sdk.security.CryptoSuiteFactory;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;
import org.hyperledger.fabric_ca.sdk.exception.RegistrationException;

@Slf4j
public class RegisterUser {

  static {
    System.setProperty("org.hyperledger.fabric.sdk.service_discovery.as_localhost", "true");
  }

  public static void main(String[] args) {
    RegisterUser registerUser = new RegisterUser();
    try {
      String pemFilePath =
          "./fabric-samples/test-network/organizations/peerOrganizations/org1.example.com/ca/ca.org1.example.com-cert.pem";
      registerUser.generateWallet(pemFilePath);
    } catch (Exception e) {

      e.printStackTrace();
    }
  }

  public void generateWallet(String pemFilePath) throws Exception {

    // Create a CA client for interacting with the CA.
    Properties props = new Properties();
    props.put("pemFile", pemFilePath);
    props.put("allowAllHostNames", "true");
    HFCAClient caClient = HFCAClient.createNewInstance("https://localhost:7054", props);
    CryptoSuite cryptoSuite = CryptoSuiteFactory.getDefault().getCryptoSuite();
    caClient.setCryptoSuite(cryptoSuite);

    // Create a wallet for managing identities
    Wallet wallet = Wallets.newFileSystemWallet(Paths.get("testwallet"));

    // Check to see if we've already enrolled the user.
    if (wallet.get("clientUser") != null) {
      log.info("An identity for the user \"clientUser\" already exists in the wallet");
      return;
    }

    X509Identity adminIdentity = (X509Identity) wallet.get("admin");
    if (adminIdentity == null) {
      log.info("\"admin\" needs to be enrolled and added to the wallet first");
      return;
    }
    User admin =
        new User() {

          @Override
          public String getName() {
            return "admin";
          }

          @Override
          public Set<String> getRoles() {
            return null;
          }

          @Override
          public String getAccount() {
            return null;
          }

          @Override
          public String getAffiliation() {
            return "org1.supplychain";
          }

          @Override
          public Enrollment getEnrollment() {
            return new Enrollment() {

              @Override
              public PrivateKey getKey() {
                return adminIdentity.getPrivateKey();
              }

              @Override
              public String getCert() {
                return Identities.toPemString(adminIdentity.getCertificate());
              }
            };
          }

          @Override
          public String getMspId() {
            return "Org1MSP";
          }
        };

    // Register the user, enroll the user, and import the new identity into the wallet.
    RegistrationRequest registrationRequest = new RegistrationRequest("clientUser");
    registrationRequest.setAffiliation("org1.supplychain");
    registrationRequest.setEnrollmentID("clientUser");

    try {
      String enrollmentSecret = caClient.register(registrationRequest, admin);
      Enrollment enrollment = caClient.enroll("clientUser", enrollmentSecret);
      log.debug("Certificate: " + enrollment.getCert());
    } catch (RegistrationException rex) {
      log.error("Error: " + rex.getMessage());
    }

    Identity user =
        Identities.newX509Identity(
            "Org1MSP", adminIdentity.getCertificate(), adminIdentity.getPrivateKey());
    wallet.put("clientUser", user);
    log.info("Successfully enrolled user \"clientUser\" and imported it into the wallet");
  }
}
