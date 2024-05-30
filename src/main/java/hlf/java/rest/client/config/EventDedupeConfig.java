package hlf.java.rest.client.config;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import hlf.java.rest.client.service.RecencyTransactionContext;
import hlf.java.rest.client.service.impl.DefaultCacheBasedRecencyTransactionContext;
import hlf.java.rest.client.service.impl.NoOpRecencyTransactionContext;
import java.util.concurrent.TimeUnit;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "dedupe")
@Slf4j
public class EventDedupeConfig {

  private boolean enable;
  private int recencyWindowSize;
  private int recencyWindowExpiryInMinutes;

  @Bean
  public RecencyTransactionContext recencyTransactionContext() {

    if (!this.isEnable()) {
      log.info(
          "Dedupe config is disabled. Events wont be validated prior to submission to publisher topic");
      return new NoOpRecencyTransactionContext();
    }

    Cache<String, Object> recencyCache =
        CacheBuilder.newBuilder()
            .maximumSize(this.getRecencyWindowSize())
            .expireAfterAccess(this.getRecencyWindowExpiryInMinutes(), TimeUnit.MINUTES)
            .build();

    log.info(
        "Enabling recency check with cache size {} and TTL {} minutes",
        recencyWindowSize,
        recencyWindowExpiryInMinutes);

    return new DefaultCacheBasedRecencyTransactionContext(recencyCache);
  }
}
