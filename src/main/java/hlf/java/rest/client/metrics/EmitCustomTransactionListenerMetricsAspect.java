package hlf.java.rest.client.metrics;

import hlf.java.rest.client.exception.UnrecognizedTransactionPayloadException;
import io.micrometer.core.instrument.Counter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hyperledger.fabric.gateway.ContractException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Aspect
@Component
@ConditionalOnProperty("kafka.integration-points[0].brokerHost")
public class EmitCustomTransactionListenerMetricsAspect {

  private static final String ANNOTATION_NAME =
      "hlf.java.rest.client.metrics.EmitCustomTransactionListenerMetrics";

  @Autowired private Counter customKafkaSuccessCounter;

  @Autowired private Counter invalidInboundTransactionMessageCounter;

  @Autowired private Counter inboundTxnProcessingFailureCounter;

  @Autowired private Counter inboundTxnContractExceptionCounter;

  @Around("@annotation(" + ANNOTATION_NAME + ")")
  public Object interceptedKafkaMetricsEmissionAdvice(ProceedingJoinPoint proceedingJoinPoint)
      throws Throwable {

    try {
      Object returnValue = proceedingJoinPoint.proceed();
      customKafkaSuccessCounter.increment();
      return returnValue;
    } catch (Throwable e) {

      if (e instanceof UnrecognizedTransactionPayloadException) {
        invalidInboundTransactionMessageCounter.increment();
        throw e;
      }

      if (e instanceof ContractException) {
        inboundTxnContractExceptionCounter.increment();
        throw e;
      }

      inboundTxnProcessingFailureCounter.increment();
      throw e;
    }
  }
}
