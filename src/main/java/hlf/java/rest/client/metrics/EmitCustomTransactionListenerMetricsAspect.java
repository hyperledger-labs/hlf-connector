package hlf.java.rest.client.metrics;

import hlf.java.rest.client.exception.ErrorCode;
import hlf.java.rest.client.exception.FabricTransactionException;
import hlf.java.rest.client.exception.ServiceException;
import hlf.java.rest.client.exception.UnrecognizedTransactionPayloadException;
import io.micrometer.core.instrument.Counter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
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

  @Autowired private Counter inboundTxnTimeoutExceptionCounter;

  @Around("@annotation(" + ANNOTATION_NAME + ")")
  public Object interceptedKafkaMetricsEmissionAdvice(ProceedingJoinPoint proceedingJoinPoint)
      throws Throwable {
    try {
      Object returnValue = proceedingJoinPoint.proceed();
      customKafkaSuccessCounter.increment();
      return returnValue;
    } catch (UnrecognizedTransactionPayloadException e) {
      invalidInboundTransactionMessageCounter.increment();
      inboundTxnProcessingFailureCounter.increment();
      throw e;
    } catch (FabricTransactionException e) {
      inboundTxnProcessingFailureCounter.increment();
      if (e.getCode().equals(ErrorCode.HYPERLEDGER_FABRIC_TRANSACTION_CONTRACT_ERROR)) {
        inboundTxnContractExceptionCounter.increment();
      } else if (e.getCode().equals(ErrorCode.HYPERLEDGER_FABRIC_TRANSACTION_TIMEOUT_ERROR)) {
        inboundTxnTimeoutExceptionCounter.increment();
      }
      throw e;
    } catch (ServiceException e) {
      inboundTxnProcessingFailureCounter.increment();
      throw e;
    }
  }
}
