package hlf.java.rest.client.service.impl;

import com.google.common.cache.Cache;
import hlf.java.rest.client.service.RecencyTransactionContext;

public class DefaultCacheBasedRecencyTransactionContext implements RecencyTransactionContext {

  private Cache<String, Object> recencyCache;

  public DefaultCacheBasedRecencyTransactionContext(Cache<String, Object> recencyCache) {
    this.recencyCache = recencyCache;
  }

  @Override
  public void setTransactionContext(String transactionId) {
    recencyCache.put(transactionId, 1);
  }

  @Override
  public boolean validateAndRemoveTransactionContext(String transactionId) {
    synchronized (this) {
      if (recencyCache.getIfPresent(transactionId) == null) {
        return false;
      }
      recencyCache.invalidate(transactionId);
      return true;
    }
  }
}
