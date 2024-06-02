package hlf.java.rest.client.config;

import java.util.List;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

/**
 * The type Kafka properties is added for fetching Kafka properties as configuration and can be used
 * in Consumer and Producer using @Autowired
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "kafka")
@RefreshScope
public class KafkaProperties {

  private List<Consumer> integrationPoints;
  private EventProducer eventListener;
  private Producer failedMessageListener;

  @Getter
  @Setter
  public static class Producer extends SSLProperties {
    private String brokerHost;
    private String topic;
    private String saslJaasConfig;
    private Boolean enableIdempotence;
    private Boolean enableAtMostOnceSemantics;

    @Override
    public String toString() {
      return "Producer{"
          + "brokerHost='"
          + brokerHost
          + '\''
          + ", topic='"
          + topic
          + '\''
          + ", saslJaasConfig='"
          + saslJaasConfig
          + '\''
          + '}';
    }
  }

  @Getter
  @Setter
  public static class EventProducer extends Producer {
    private boolean listenToFailedMessages;
  }

  /** The type Ssl properties is added for configuring SSL configuration for Kafka Cluster. */
  @Getter
  @Setter
  public static class SSLProperties {

    protected boolean sslEnabled;
    protected String securityProtocol;
    protected String sslKeystoreLocation;
    protected String sslKeystorePassword;
    protected String sslTruststoreLocation;
    protected String sslTruststorePassword;
    protected String sslKeyPassword;
    protected String sslKeystoreBase64;
    protected String sslTruststoreBase64;

    protected boolean isSslAuthRequired() {
      boolean isProtocolSSL =
          StringUtils.isNotBlank(securityProtocol) && "ssl".equalsIgnoreCase(securityProtocol);
      return isProtocolSSL && sslEnabled;
    }
  }

  @Getter
  @Setter
  public static class Consumer extends SSLProperties {
    private String brokerHost;
    private String groupId;
    private String topic;
    private int topicPartitions = 1;
    private boolean enableParallelListenerCapabilities = false;
    private String saslJaasConfig;
    private String offsetResetPolicy;

    @Override
    public String toString() {
      return "Consumer{"
          + "brokerHost='"
          + brokerHost
          + '\''
          + ", groupId='"
          + groupId
          + '\''
          + ", topic='"
          + topic
          + '\''
          + ", saslJaasConfig='"
          + saslJaasConfig
          + '\''
          + '}';
    }
  }
}
