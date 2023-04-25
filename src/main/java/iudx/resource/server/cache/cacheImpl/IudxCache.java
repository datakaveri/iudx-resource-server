package iudx.resource.server.cache.cacheImpl;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public interface IudxCache {

  public Future<Void> put(String key, CacheValue<JsonObject> value);

  public Future<CacheValue<JsonObject>> get(String key);

  public Future<Void> refreshCache();

  public CacheValue<JsonObject> createCacheValue(String key, String value);
}
