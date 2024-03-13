package hlf.java.rest.client.config;

import hlf.java.rest.client.util.FabricClientConstants;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SslConfigs;

@Slf4j
public abstract class BaseKafkaConfig {

  enum ConfigType {
    PRODUCER,
    CONSUMER
  }

  protected void configureSaslProperties(Map<String, Object> props, String saslJaasConfig) {
    if (StringUtils.isNotEmpty(saslJaasConfig)) {
      props.put(
          FabricClientConstants.KAFKA_SECURITY_PROTOCOL_KEY,
          FabricClientConstants.KAFKA_SECURITY_PROTOCOL_VALUE);
      props.put(
          FabricClientConstants.KAFKA_SASL_MECHANISM_KEY,
          FabricClientConstants.KAFKA_SASL_MECHANISM_VALUE);
      props.put(FabricClientConstants.KAFKA_SASL_JASS_ENDPOINT_KEY, saslJaasConfig);
    }
  }

  protected void configureSSLProperties(
      Map<String, Object> props,
      KafkaProperties.SSLProperties sslProperties,
      String topicName,
      MeterRegistry sslMetricsRegistry) {

    if (sslProperties.isSslAuthRequired()) {
      SSLAuthFilesHelper.createSSLAuthFiles(sslProperties);
      props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, sslProperties.getSecurityProtocol());
      props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, sslProperties.getSslKeystoreLocation());
      props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, sslProperties.getSslKeystorePassword());
      props.put(
          SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, sslProperties.getSslTruststoreLocation());
      props.put(
          SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, sslProperties.getSslTruststorePassword());
      props.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, sslProperties.getSslKeyPassword());

      try {
        Timestamp keyStoreCertExpiryTimestamp =
            SSLAuthFilesHelper.getExpiryTimestampForKeyStore(
                sslProperties.getSslKeystoreLocation(), sslProperties.getSslKeystorePassword());
        Timestamp trustStoreCertExpiryTimestamp =
            SSLAuthFilesHelper.getExpiryTimestampForKeyStore(
                sslProperties.getSslTruststoreLocation(), sslProperties.getSslTruststorePassword());

        String guagePrefix =
            getConfigType().equals(ConfigType.CONSUMER) ? "consumer." : "producer.";

        Gauge.builder(
                guagePrefix + topicName + ".keystore.expiryTs",
                keyStoreCertExpiryTimestamp::getTime)
            .strongReference(true)
            .register(sslMetricsRegistry);

        Gauge.builder(
                guagePrefix + topicName + ".truststore.expiryTs",
                trustStoreCertExpiryTimestamp::getTime)
            .strongReference(true)
            .register(sslMetricsRegistry);

        boolean hasKeyStoreCertExpired =
            keyStoreCertExpiryTimestamp.before(Timestamp.from(Instant.now()));
        boolean hasTrustStoreCertExpired =
            trustStoreCertExpiryTimestamp.before(Timestamp.from(Instant.now()));

        Gauge.builder(
                guagePrefix + topicName + ".keystore.hasExpired",
                hasKeyStoreCertExpired,
                BooleanUtils::toInteger)
            .strongReference(true)
            .register(sslMetricsRegistry);

        Gauge.builder(
                guagePrefix + topicName + ".truststore.hasExpired",
                hasTrustStoreCertExpired,
                BooleanUtils::toInteger)
            .strongReference(true)
            .register(sslMetricsRegistry);

      } catch (Exception e) {
        log.error(
            "Failed to extract expiry details of SSL Certs. Metrics for SSL cert-expiry will not be available.");
      }
    }
  }

  protected abstract ConfigType getConfigType();
}
