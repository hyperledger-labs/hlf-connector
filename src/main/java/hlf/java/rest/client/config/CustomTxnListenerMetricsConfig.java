package hlf.java.rest.client.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty("kafka.integration-points[0].brokerHost")
public class CustomTxnListenerMetricsConfig {

  @Bean
  public Counter customKafkaSuccessCounter(MeterRegistry meterRegistry) {
    return meterRegistry.counter("kafka.messages.processed.messages");
  }

  @Bean
  public Counter invalidInboundTransactionMessageCounter(MeterRegistry meterRegistry) {
    return meterRegistry.counter("transaction.messages.unrecognized.failures");
  }

  @Bean
  public Counter inboundTxnProcessingFailureCounter(MeterRegistry meterRegistry) {
    return meterRegistry.counter("transaction.messages.process.failures");
  }

  @Bean
  public Counter inboundTxnContractExceptionCounter(MeterRegistry meterRegistry) {
    return meterRegistry.counter("transaction.messages.contract.failures");
  }
}
