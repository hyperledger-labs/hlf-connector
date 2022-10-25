package hlf.java.rest.client.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class SSLAuthFilesCreationHelper {

  static void createSSLAuthFiles(KafkaProperties.SSLProperties kafkaSSLProperties) {

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
}
