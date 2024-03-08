package hlf.java.rest.client.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty("kafka.event-listener.brokerHost")
public class CustomTxnListenerMetricsConfig {

  @Bean
  public Counter customKafkaSuccessCounter(MeterRegistry meterRegistry) {
    return meterRegistry.counter("kafka.messages.processed.messages");
  }

  @Bean
  public Counter customKafkaFailureCounter(MeterRegistry meterRegistry) {
    return meterRegistry.counter("kafka.messages.failed.messages");
  }

  @Bean
  public Counter invalidInboundTransactionMessageCounter(MeterRegistry meterRegistry) {
    return meterRegistry.counter("transaction.messages.unrecognized");
  }

  @Bean
  public Counter inboundTxnProcessingFailureCounter(MeterRegistry meterRegistry) {
    return meterRegistry.counter("transaction.messages.process.failures");
  }
}
