package hlf.java.rest.client.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomMetricsConfig {

  @Bean
  @ConditionalOnProperty(value = "management.metrics.custom.kafka.enabled", havingValue = "true")
  public Counter customKafkaSuccessCounter(MeterRegistry meterRegistry) {
    return meterRegistry.counter("kafka.messages.processed.messages");
  }

  @Bean
  @ConditionalOnProperty(value = "management.metrics.custom.kafka.enabled", havingValue = "true")
  public Counter customKafkaFailureCounter(MeterRegistry meterRegistry) {
    return meterRegistry.counter("kafka.messages.failed.messages");
  }
}
