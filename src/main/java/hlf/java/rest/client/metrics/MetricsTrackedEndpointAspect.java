package hlf.java.rest.client.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class MetricsTrackedEndpointAspect {

  @Autowired private MeterRegistry meterRegistry;

  private Map<String, Boolean> trackedEndpoints = new ConcurrentHashMap<>();

  @After("@annotation(metricsTrackedEndpoint)")
  public void afterTimedEndpoint(
      JoinPoint joinPoint, MetricsTrackedEndpoint metricsTrackedEndpoint) {

    trackedEndpoints.computeIfAbsent(
        metricsTrackedEndpoint.name(),
        val -> {
          Gauge.builder("tracked_endpoints", () -> 1L)
              .tag("name", metricsTrackedEndpoint.name())
              .tag("method", metricsTrackedEndpoint.method())
              .tag("uri", metricsTrackedEndpoint.uri())
              .register(meterRegistry);

          return true;
        });
  }
}
