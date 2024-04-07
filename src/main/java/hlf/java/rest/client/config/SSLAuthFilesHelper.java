package hlf.java.rest.client.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

@Slf4j
@UtilityClass
public class SSLAuthFilesHelper {

  void createSSLAuthFiles(KafkaProperties.SSLProperties kafkaSSLProperties) {

    log.info("Creating Kafka ssl keystore file");
    createSSLFileFromBase64(
        kafkaSSLProperties.getSslKeystoreBase64(), kafkaSSLProperties.getSslKeystoreLocation());

    log.info("Keystore file created at {}", kafkaSSLProperties.getSslKeystoreLocation());

    log.info("Creating Kafka ssl truststore file");
    createSSLFileFromBase64(
        kafkaSSLProperties.getSslTruststoreBase64(), kafkaSSLProperties.getSslTruststoreLocation());

    log.info("TrustStore file created at {}", kafkaSSLProperties.getSslTruststoreLocation());
  }

  private static void createSSLFileFromBase64(
      String sslKeystoreBase64, String sslKeystoreLocation) {
    log.info("Creating file at: {}", sslKeystoreLocation);
    try {
      final Path path = Paths.get(sslKeystoreLocation);
      final Path parentPath = path.getParent();
      if (!Files.isDirectory(parentPath)) {
        log.info("Creating directory: {}", parentPath);
        Files.createDirectory(parentPath);
      }
      Files.write(path, Base64.getDecoder().decode(sslKeystoreBase64));
    } catch (IOException e) {
      log.error("Failed to create the ssl auth file at location: {}", sslKeystoreLocation, e);
    }
  }

  Timestamp getExpiryTimestampForKeyStore(String keyStorePath, String keyStorePassword)
      throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {

    KeyStore keyStore = loadKeyStore(keyStorePath, keyStorePassword);

    List<Timestamp> certExpiryTimestamps = new ArrayList<>();

    Enumeration<String> aliases = keyStore.aliases();
    while (aliases.hasMoreElements()) {
      String alias = aliases.nextElement();
      Certificate cert = keyStore.getCertificate(alias);
      if (cert instanceof X509Certificate) {
        X509Certificate x509Cert = (X509Certificate) cert;
        certExpiryTimestamps.add(new Timestamp(x509Cert.getNotAfter().getTime()));
      }
    }

    if (CollectionUtils.isEmpty(certExpiryTimestamps)) {
      throw new CertificateException(
          "Couldn't extract an instance of X509Certificate for fetching expiry details");
    }

    // Return the earliest (minimum) timestamp from the list
    return Collections.min(certExpiryTimestamps);
  }

  private static KeyStore loadKeyStore(String path, String password)
      throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
    KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
    try (FileInputStream fis = new FileInputStream(path)) {
      keyStore.load(fis, password.toCharArray());
    }
    return keyStore;
  }
}
