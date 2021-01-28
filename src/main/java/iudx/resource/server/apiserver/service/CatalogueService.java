package iudx.resource.server.apiserver.service;

import java.util.ArrayList;
import java.util.List;
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
import iudx.resource.server.authenticator.Constants;

/**
 * catalogue service to fetch calatogue items and groups for the purpose of cache
 *
 */
public class CatalogueService {
  
  private static final Logger LOGGER=LogManager.getLogger(CatalogueService.class);

  private WebClient catWebClient;
  private long cacheTimerid;
  private static String catHost;
  private static int catPort;;
  private static String catSearchPath;
  private static String catItemPath;

  private final Cache<String, List<String>> applicableFilterCache =
      CacheBuilder.newBuilder().maximumSize(1000)
          .expireAfterAccess(Constants.CACHE_TIMEOUT_AMOUNT, TimeUnit.MINUTES).build();

  public CatalogueService(Vertx vertx, JsonObject config) {
    catHost = config.getString("catServerHost");
    catPort = Integer.parseInt(config.getString("catServerPort"));
    catSearchPath = Constants.CAT_RSG_PATH;
    catItemPath = Constants.CAT_ITEM_PATH;

    WebClientOptions options =
        new WebClientOptions().setTrustAll(true).setVerifyHost(false).setSsl(true);
    catWebClient = WebClient.create(vertx, options);
    populateCache();
    cacheTimerid = vertx.setPeriodic(TimeUnit.DAYS.toMillis(1), handler -> {
      populateCache();
    });
  }

  /**
   * populate 
   * @return
   */
  private Future<Boolean> populateCache() {
    Promise<Boolean> promise = Promise.promise();
    catWebClient.get(catPort, catHost, catSearchPath)
        .addQueryParam("property", "[iudxResourceAPIs]")
        .addQueryParam("value", "[[TEMPORAL,ATTR,SPATIAl]]")
        .addQueryParam("filter", "[iudxResourceAPIs,id]").expect(ResponsePredicate.JSON)
        .send(handler -> {
          if (handler.succeeded()) {
            JsonArray response = handler.result().bodyAsJsonObject().getJsonArray("results");
            response.forEach(json -> {
              JsonObject res = (JsonObject) json;
              System.out.println(res);
              String id = res.getString("id");
              String[] idArray = id.split("/");
              if (idArray.length == 4) {
                applicableFilterCache.put(id + "/*", toList(res.getJsonArray("iudxResourceAPIs")));
              } else {
                applicableFilterCache.put(id, toList(res.getJsonArray("iudxResourceAPIs")));
              }
            });
            promise.complete(true);
          } else if (handler.failed()) {
            promise.fail(handler.cause());
          }
        });
    return promise.future();
  }


  public List<String> getApplicableFilters(String id) {
    // Note: id should be a complete id not a group id (ex : domain/SHA/rs/rs-group/itemId)
    String groupId = id.substring(0, id.lastIndexOf("/"));
    // check for item in cache.
    List<String> filters = applicableFilterCache.getIfPresent(id);
    if (filters == null) {
      // check for group if not present by item key.
      filters = applicableFilterCache.getIfPresent(groupId + "/*");
    }
    if (filters == null) {
      filters=fetchFilters4Item(id, groupId);
    }
    return filters;
  }


  private List<String> fetchFilters4Item(String id, String groupId) {
    List<String> filters = new ArrayList<String>();
    List<String> itemFilters = getFilterFromItemId(id);
    if (itemFilters.isEmpty()) {
      List<String> itemGroupFilters = getFilterFromGroupId(groupId);
      if (!itemGroupFilters.isEmpty()) {
        filters = itemGroupFilters;
        applicableFilterCache.put(groupId+"/*", filters);
      } else {
        LOGGER.error("Failed to fetch applicable filters for id: "+id +"or group id : "+groupId);
      }
    } else {
      filters = itemFilters;
      applicableFilterCache.put(id, filters);
    }
    return filters;
  }

  private List<String> getFilterFromGroupId(String groupId) {
    return callCatalogueAPI(groupId);
  }

  private List<String> getFilterFromItemId(String itemId) {
    return callCatalogueAPI(itemId);
  }

  private List<String> callCatalogueAPI(String id) {
    List<String> filters = new ArrayList<String>();
    catWebClient.get(catPort, catHost, catItemPath).addQueryParam("id", id).send(handler -> {
      if (handler.succeeded()) {
        JsonArray response = handler.result().bodyAsJsonObject().getJsonArray("results");
        response.forEach(json -> {
          JsonObject res = (JsonObject) json;
          if (res.containsKey("iudxResourceAPIs")) {
            filters.addAll(toList(res.getJsonArray("iudxResourceAPIs")));
          }
        });
      } else if (handler.failed()) {
        LOGGER.error("catalogue call(/iudx/cat/v1/item) failed for id"+id);
      }
    });
    return filters;
  }

  private <T> List<T> toList(JsonArray arr) {
    if (arr == null) {
      return null;
    } else {
      return (List<T>) arr.getList();
    }
  }
}
