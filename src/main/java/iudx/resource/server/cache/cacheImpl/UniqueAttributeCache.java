package iudx.resource.server.cache.cacheImpl;

import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.common.Constants;
import iudx.resource.server.database.postgres.PostgresService;

public class UniqueAttributeCache implements IudxCache {

  private static final Logger LOGGER = LogManager.getLogger(UniqueAttributeCache.class);
  private final static CacheType cacheType = CacheType.UNIQUE_ATTRIBUTE;

  private final PostgresService postgresService;

  private final Cache<String, String> cache =
      CacheBuilder.newBuilder()
          .maximumSize(5000)
          .expireAfterWrite(1L, TimeUnit.DAYS)
          .build();

  public UniqueAttributeCache(Vertx vertx, PostgresService postgresService) {
    this.postgresService = postgresService;
    refreshCache();

    vertx.setPeriodic(TimeUnit.HOURS.toMillis(1), handler -> {
      refreshCache();
    });
  }

  @Override
  public void put(String key, String value) {
    cache.put(key, value);
  }

  @Override
  public String get(String key) {
    return cache.getIfPresent(key);
  }

  @Override
  public void refreshCache() {
    LOGGER.debug(cacheType + " refreshCache() called");
    String query = Constants.SELECT_UNIQUE_ATTRIBUTE;
    postgresService.executeQuery(query, handler -> {
      if (handler.succeeded()) {
        JsonArray clientIdArray = handler.result().getJsonArray("result");
        cache.invalidateAll();
        clientIdArray.forEach(e -> {
          JsonObject clientInfo = (JsonObject) e;
          String key = clientInfo.getString("resource_id");
          String value = clientInfo.getString("unique_attribute");
          this.cache.put(key, value);
        });

      }
    });

  }

}
