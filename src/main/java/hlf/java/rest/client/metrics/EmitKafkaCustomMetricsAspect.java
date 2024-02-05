package hlf.java.rest.client.metrics;

import io.micrometer.core.instrument.Counter;
import java.util.Objects;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class EmitKafkaCustomMetricsAspect {

  private static final String ANNOTATION_NAME =
      "hlf.java.rest.client.metrics.EmitKafkaCustomMetrics";

  @Autowired(required = false)
  private Counter customKafkaSuccessCounter;

  @Autowired(required = false)
  private Counter customKafkaFailureCounter;

  @Around("@annotation(" + ANNOTATION_NAME + ")")
  public Object interceptedKafkaMetricsEmissionAdvice(ProceedingJoinPoint proceedingJoinPoint)
      throws Throwable {

    try {
      Object returnValue = proceedingJoinPoint.proceed();

      if (Objects.nonNull(customKafkaSuccessCounter)) {
        customKafkaSuccessCounter.increment();
      }

      return returnValue;
    } catch (Throwable e) {

      if (Objects.nonNull(customKafkaFailureCounter)) {
        customKafkaFailureCounter.increment();
      }
      throw e;
    }
  }
}
