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
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;

public class CatalogueCacheImpl implements IudxCache {

  private static final Logger LOGGER = LogManager.getLogger(CatalogueCacheImpl.class);
  private final static CacheType cacheType = CacheType.CATALOGUE_CACHE;

  private WebClient catWebClient;
  private String catHost;
  private int catPort;;

  private final Cache<String, CacheValue<JsonObject>> cache =
      CacheBuilder.newBuilder().maximumSize(5000).expireAfterWrite(1L, TimeUnit.DAYS).build();
  private Vertx vertx;

  public CatalogueCacheImpl(Vertx vertx, JsonObject config) {
    this.vertx = vertx;
    LOGGER.debug("config : {}",config);
    this.catHost=config.getString("catServerHost");
    this.catPort=config.getInteger("catServerPort");
    
    
    WebClientOptions options =
        new WebClientOptions().setTrustAll(true).setVerifyHost(false).setSsl(true);
    if(catWebClient == null)
    {
      catWebClient = WebClient.create(vertx, options);
    }
    
    refreshCache();
    vertx.setPeriodic(TimeUnit.HOURS.toMillis(1), handler -> {
      refreshCache();
    });
  }

  @Override
  public Future<Void> put(String key, CacheValue<JsonObject> value) {
    throw new RuntimeException("Adding elements in cache are not allowed, only refresh can be used");
  }

  @Override
  public Future<CacheValue<JsonObject>> get(String key) {
    LOGGER.trace("request for id : {}",key);
    Promise<CacheValue<JsonObject>> promise=Promise.promise();
    if (cache.getIfPresent(key) != null) {
      return Future.succeededFuture(cache.getIfPresent(key));
    } else {
      populateCache()
      .onSuccess(successHandler -> {
        if(cache.getIfPresent(key)!=null) {
          promise.complete(cache.getIfPresent(key));
        }else {
          promise.fail("key not found");
        }
      }).onFailure(failureHandler -> {
        promise.fail("Value not found");
      });
    }
    return promise.future();
  }

  @Override
  public Future<Void> refreshCache() {
    populateCache();
    LOGGER.trace(cacheType + " refreshCache() called");
    return Future.succeededFuture();
  }

  private Future<Void> populateCache() {
    Promise<Void> promise = Promise.promise();
    catWebClient
        .get(catPort, catHost, "/iudx/cat/v1/search")
          .addQueryParam("property", "[itemStatus]")
          .addQueryParam("value", "[[ACTIVE]]")
          .addQueryParam("filter", "[id,provider,name,description,authControlGroup,accessPolicy,iudxResourceAPIs]")
          .expect(ResponsePredicate.JSON)
          .send(catHandler -> {
            if (catHandler.succeeded()) {
              JsonArray response = catHandler.result().bodyAsJsonObject().getJsonArray("results");
              //LOGGER.debug("results : {}",catHandler.result().bodyAsJsonObject());
              cache.invalidateAll();
              response.forEach(json -> {
                JsonObject res = (JsonObject) json;
                String id=res.getString("id");
                CacheValue<JsonObject> cacheValue=createCacheValue(id, res.toString());
                cache.put(id, cacheValue);
              });
             // LOGGER.debug("cache value :{}",cache.getIfPresent("datakaveri.org/b8bd3e3f39615c8ec96722131ae95056b5938f2f/rs.iudx.io/varanasi-swm-vehicle-live").getValue());
              promise.complete();
            } else if (catHandler.failed()) {
              LOGGER.error("Failed to populate catalogue cache");
              promise.fail("Failed to populate catalogue cache");
            }
          });
    
    return promise.future();
  }

  @Override
  public CacheValue<JsonObject> createCacheValue(String key, String value){
    return new CacheValue<JsonObject>() {
      @Override
      public JsonObject getValue() {
        return new JsonObject(value);
      }
    };
  }

}
