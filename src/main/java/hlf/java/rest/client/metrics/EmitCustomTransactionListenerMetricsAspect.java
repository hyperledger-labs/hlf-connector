package hlf.java.rest.client.metrics;

import hlf.java.rest.client.exception.UnrecognizedTransactionPayloadException;
import io.micrometer.core.instrument.Counter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class EmitCustomTransactionListenerMetricsAspect {

  private static final String ANNOTATION_NAME =
      "hlf.java.rest.client.metrics.EmitCustomTransactionListenerMetrics";

  @Autowired private Counter customKafkaSuccessCounter;

  @Autowired private Counter customKafkaFailureCounter;

  @Autowired private Counter invalidInboundTransactionMessageCounter;

  @Autowired private Counter inboundTxnProcessingFailureCounter;

  @Around("@annotation(" + ANNOTATION_NAME + ")")
  public Object interceptedKafkaMetricsEmissionAdvice(ProceedingJoinPoint proceedingJoinPoint)
      throws Throwable {

    try {
      Object returnValue = proceedingJoinPoint.proceed();
      customKafkaSuccessCounter.increment();
      return returnValue;
    } catch (Throwable e) {

      customKafkaFailureCounter.increment();

      if (e instanceof UnrecognizedTransactionPayloadException) {
        invalidInboundTransactionMessageCounter.increment();
      } else {
        inboundTxnProcessingFailureCounter.increment();
      }

      throw e;
    }
  }
}
