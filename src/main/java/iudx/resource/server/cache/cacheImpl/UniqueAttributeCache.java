package iudx.resource.server.cache.cacheImpl;

import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.common.Constants;
import iudx.resource.server.database.postgres.PostgresService;

public class UniqueAttributeCache implements IudxCache {

  private static final Logger LOGGER = LogManager.getLogger(UniqueAttributeCache.class);
  private final static CacheType cacheType = CacheType.UNIQUE_ATTRIBUTE;

  private final PostgresService postgresService;

  private final Cache<String, CacheValue<JsonObject>> cache =
      CacheBuilder.newBuilder().maximumSize(5000).expireAfterWrite(1L, TimeUnit.DAYS).build();

  public UniqueAttributeCache(Vertx vertx, PostgresService postgresService) {
    this.postgresService = postgresService;
    refreshCache();

    vertx.setPeriodic(TimeUnit.HOURS.toMillis(1), handler -> {
      refreshCache();
    });
  }

  @Override
  public Future<Void> put(String key, CacheValue<JsonObject> value) {
    cache.put(key, value);
    return Future.succeededFuture();
  }

  @Override
  public Future<CacheValue<JsonObject>> get(String key) {
    if (cache.getIfPresent(key) != null) {
      return Future.succeededFuture(cache.getIfPresent(key));
    } else {
      return Future.failedFuture("Value not found");
    }
  }

  @Override
  public Future<Void> refreshCache() {
    Promise<Void> promise = Promise.promise();
    LOGGER.trace(cacheType + " refreshCache() called");
    String query = Constants.SELECT_UNIQUE_ATTRIBUTE;
    postgresService.executeQuery(query, handler -> {
      if (handler.succeeded()) {
        JsonArray clientIdArray = handler.result().getJsonArray("result");
        cache.invalidateAll();
        clientIdArray.forEach(e -> {
          JsonObject clientInfo = (JsonObject) e;
          String key = clientInfo.getString("resource_id");
          String value = clientInfo.getString("unique_attribute");
          CacheValue<JsonObject> cacheValue=createCacheValue(key, value);
          this.cache.put(key, cacheValue);
        });
        promise.complete();
      } else {
        promise.fail("failed to refreash");
      }
    });
    return promise.future();
  }
  
  @Override
  public CacheValue<JsonObject> createCacheValue(String id, String unique_attrib){
    return new CacheValue<JsonObject>() {
      @Override
      public JsonObject getValue() {
        JsonObject value=new JsonObject();
        value.put("resource_id", id);
        value.put("key", id);
        value.put("unique_attribute", unique_attrib);
        value.put("value", unique_attrib);
        return value;
      }
      
    };
  }

}
