package iudx.resource.server.cache;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.cache.cacheImpl.CacheType;
import iudx.resource.server.cache.cacheImpl.CacheValue;
import iudx.resource.server.cache.cacheImpl.CatalogueCacheImpl;
import iudx.resource.server.cache.cacheImpl.IudxCache;
import iudx.resource.server.cache.cacheImpl.RevokedClientCache;
import iudx.resource.server.cache.cacheImpl.UniqueAttributeCache;
import iudx.resource.server.database.postgres.PostgresService;

public class CacheServiceImpl implements CacheService {

  private static final Logger LOGGER = LogManager.getLogger(CacheServiceImpl.class);

  private IudxCache revokedClientCache;
  private IudxCache uniqueAttributeCache;
  private IudxCache catalogueCache;
  private PostgresService postgresService;

  public CacheServiceImpl(
      Vertx vertx, PostgresService pgService, CatalogueCacheImpl catalogueCache) {
    this.postgresService = pgService;
    this.revokedClientCache = new RevokedClientCache(vertx, postgresService);
    this.uniqueAttributeCache = new UniqueAttributeCache(vertx, postgresService);
    this.catalogueCache = catalogueCache;
  }

  /** {@inheritDoc} */
  @Override
  public Future<JsonObject> get(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    IudxCache cache = null;

    try {
      cache = getCache(request);
    } catch (IllegalArgumentException ex) {
      LOGGER.error("No cache defined for given argument.");
      return Future.failedFuture("No cache defined for given type");
    }

    String key = request.getString("key");

    if (cache != null && key != null) {
      Future<CacheValue<JsonObject>> getValueFuture = cache.get(key);
      getValueFuture
          .onSuccess(
              successHandler -> {
                promise.complete(successHandler.getValue());
              })
          .onFailure(
              failureHandler -> {
                promise.fail("No entry for given key");
              });
    } else {
      promise.fail("null key passed.");
    }
    return promise.future();
  }

  /** {@inheritDoc} */
  @Override
  public Future<JsonObject> put(JsonObject request) {
    LOGGER.trace("message received from for cache put operation");
    LOGGER.debug("message : " + request);
    Promise<JsonObject> promise = Promise.promise();
    IudxCache cache = null;
    try {
      cache = getCache(request);
    } catch (IllegalArgumentException ex) {
      LOGGER.error("No cache defined for given argument.");
      return Future.failedFuture("No cache defined for given type");
    }

    String key = request.getString("key");
    String value = request.getString("value");
    if (cache != null && key != null && value != null) {
      cache.put(key, cache.createCacheValue(key, value));
      promise.complete(new JsonObject().put(key, value));
    } else {
      promise.fail("'null' key or value not allowed in cache.");
    }
    return promise.future();
  }

  /** {@inheritDoc} */
  @Override
  public Future<JsonObject> refresh(JsonObject request) {
    LOGGER.trace("message received for cache refresh()");
    LOGGER.debug("message : " + request);
    Promise<JsonObject> promise = Promise.promise();
    IudxCache cache = null;
    try {
      cache = getCache(request);
    } catch (IllegalArgumentException ex) {
      LOGGER.error("No cache defined for given argument.");
      return Future.failedFuture("No cache defined for given type");
    }
    String key = request.getString("key");
    String value = request.getString("value");

    if (cache != null && key != null && value != null) {
      cache.put(key, cache.createCacheValue(key, value));
    } else {
      cache.refreshCache();
    }
    promise.complete(new JsonObject());
    return promise.future();
  }

  private IudxCache getCache(JsonObject json) {
    if (!json.containsKey("type")) {
      throw new IllegalArgumentException("No cache type specified");
    }

    CacheType cacheType = CacheType.valueOf(json.getString("type"));
    IudxCache cache = null;
    switch (cacheType) {
      case REVOKED_CLIENT:
        {
          cache = revokedClientCache;
          break;
        }
      case UNIQUE_ATTRIBUTE:
        {
          cache = uniqueAttributeCache;
          break;
        }
      case CATALOGUE_CACHE:
        {
          cache = catalogueCache;
          break;
        }
      default:
        {
          throw new IllegalArgumentException("No cache type specified");
        }
    }
    return cache;
  }
}
