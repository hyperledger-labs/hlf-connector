package hlf.java.rest.client.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@RefreshScope
public class WorkerPoolConfig {

  /**
   * Core Pool and Max Pool size of executors are made configurable since these parameters are
   * determined by the environment in which the Connector is running, like the number of CPU cores.
   * If these values are not configured, fallback to the application defaults.
   */
  @Value("${executors.defaultExecutor.corePoolSize:20}")
  private int defaultTaskExecutorCorePoolSize;

  @Value("${executors.defaultExecutor.maxPoolSize:30}")
  private int defaultExecutorMaxPoolSize;

  @Value("${executors.defaultExecutor.waitQueueSize:400}")
  private int defaultExecutorQueueSize;

  /** A general-purpose, re-usable Task executor. */
  @Bean
  public TaskExecutor defaultTaskExecutor() {
    ThreadPoolTaskExecutor defaultTaskExecutor = new ThreadPoolTaskExecutor();
    defaultTaskExecutor.setCorePoolSize(defaultTaskExecutorCorePoolSize);
    defaultTaskExecutor.setMaxPoolSize(defaultExecutorMaxPoolSize);
    defaultTaskExecutor.setQueueCapacity(defaultExecutorQueueSize);
    return defaultTaskExecutor;
  }
}
