package iudx.resource.server.authenticator;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import iudx.resource.server.databroker.util.Util;

/**
 * The Authentication Service Implementation.
 * <h1>Authentication Service Implementation</h1>
 * <p>
 * The Authentication Service implementation in the IUDX Resource Server implements the definitions
 * of the {@link iudx.resource.server.authenticator.AuthenticationService}.
 * </p>
 *
 * @version 1.0
 * @since 2020-05-31
 */

public class AuthenticationServiceImpl implements AuthenticationService {

  private static final Logger LOGGER = LogManager.getLogger(AuthenticationServiceImpl.class);

  private final WebClient webClient;
  private final Vertx vertxObj;
  private JsonObject config;
  private long catCacheTimerId;
  private long catCacheResTimerid;
  private static String catHost;
  private static int catPort;;
  private static String catPath;
  private String resourceServerId;
  private WebClient catWebClient;

  /**
   * Cache/'s will hold at-most 1000 objects and only for a duration of TIP_CACHE_TIMEOUT_AMOUNT
   * from the last access to object
   */
  // Cache for all token.
  // what if token is revoked ?
  private final Cache<String, JsonObject> tipCache = CacheBuilder.newBuilder().maximumSize(1000)
      .expireAfterAccess(Constants.CACHE_TIMEOUT_AMOUNT, TimeUnit.MINUTES).build();
  // resourceGroupCache will contains ACL info about all resource group in a resource server
  private final Cache<String, String> resourceGroupCache =
      CacheBuilder.newBuilder().maximumSize(1000)
          .expireAfterAccess(Constants.CACHE_TIMEOUT_AMOUNT, TimeUnit.MINUTES).build();
  // resourceIdCache will contains info about resources available(& their ACL) in resource server.
  // what if resource id ACL is changed ?
  private final Cache<String, String> resourceIdCache = CacheBuilder.newBuilder().maximumSize(1000)
      .expireAfterAccess(Constants.CACHE_TIMEOUT_AMOUNT, TimeUnit.MINUTES).build();

  /**
   * This is a constructor which is used by the DataBroker Verticle to instantiate a RabbitMQ
   * client.
   * 
   * @param vertx which is a vertx instance
   * @param client which is a Vertx Web client
   */

  public AuthenticationServiceImpl(Vertx vertx, WebClient client, JsonObject config) {
    webClient = client;
    vertxObj = vertx;
    this.config = config;
    catHost = config.getString("catServerHost");
    catPort = Integer.parseInt(config.getString("catServerPort"));
    catPath = Constants.CAT_RSG_PATH;
    resourceServerId = config.getString("resourceServerId");

    WebClientOptions options =
        new WebClientOptions().setTrustAll(true).setVerifyHost(false).setSsl(true);
    catWebClient = WebClient.create(vertxObj, options);


    Future<Boolean> groupCacheFuture = populateCatCache(client);
    groupCacheFuture.onComplete(handler -> {
      populateCatResourceIdCache(client);
    });
    catCacheTimerId = vertx.setPeriodic(TimeUnit.DAYS.toMillis(1), handler -> {
      populateCatCache(webClient);
    });
    catCacheResTimerid = vertx.setPeriodic(TimeUnit.DAYS.toMillis(1), handler -> {
      populateCatResourceIdCache(webClient);
    });

  }

  // populate all resource groups available in resource server with access policy
  private Future<Boolean> populateCatCache(WebClient client) {
    LOGGER.debug("Info : starting populateCatCache()");
    Promise<Boolean> promise = Promise.promise();
    catWebClient.get(catPort, catHost, catPath).addQueryParam("property", "[resourceServer]")
        .addQueryParam("value", resourceServerId).expect(ResponsePredicate.JSON).send(handler -> {
          if (handler.succeeded()) {
            JsonArray response = handler.result().bodyAsJsonObject().getJsonArray("results");
            response.forEach(json -> {
              JsonObject res = (JsonObject) json;
              LOGGER.debug("cat id cat: " + res.getString("id"));
              resourceGroupCache.put(res.getString("id"), res.getString("accessPolicy", "SECURE"));
            });
          } else if (handler.failed()) {
            LOGGER.error(handler.cause());
          }
        });
    promise.complete(true);
    return promise.future();
  }

  // populate all resource Ids available in resource server for all resource group with access
  // policy
  private Future<Void> populateCatResourceIdCache(WebClient client) {
    LOGGER.debug("Info : starting populateCatResourceIdCache()");
    Promise<Void> promise = Promise.promise();
    LOGGER.debug("size : " + resourceGroupCache.size());
    // for every key call cat to get all resources and their ACL(?)/itemstatus
    resourceGroupCache.asMap().forEach((key, value) -> {
      catWebClient.get(catPort, catHost, catPath).addQueryParam("id", key)
          .addQueryParam("rel", "resource").expect(ResponsePredicate.JSON).send(handler -> {
            if (handler.succeeded()) {
              JsonArray response = handler.result().bodyAsJsonObject().getJsonArray("results");
              response.forEach(json -> {
                JsonObject res = (JsonObject) json;
                LOGGER.debug("cat id res: " + res.getString("id"));
                resourceIdCache.put(res.getString("id"), resourceGroupCache.getIfPresent(key));
              });
            } else if (handler.failed()) {
              LOGGER.error(handler.cause());
            }
          });
    });
    promise.complete();
    return promise.future();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AuthenticationService tokenInterospect(JsonObject request, JsonObject authenticationInfo,
      Handler<AsyncResult<JsonObject>> handler) {

    //System.out.println(authenticationInfo);
    String token = authenticationInfo.getString("token");
    String requestEndpoint = authenticationInfo.getString("apiEndpoint");

    LOGGER.debug("Info: requested endpoint :" + requestEndpoint);

    if (config.getString(Constants.SERVER_MODE).equalsIgnoreCase("testing")) {
      if (token.equals(Constants.PUBLIC_TOKEN)
          && Constants.OPEN_ENDPOINTS.contains(requestEndpoint)) {
        JsonObject result = new JsonObject();
        result.put("status", "success");
        handler.handle(Future.succeededFuture(result));
        return this;
      } else if (token.equals(Constants.PUBLIC_TOKEN)
          && !Constants.OPEN_ENDPOINTS.contains(requestEndpoint)) {
        JsonObject result = new JsonObject();
        result.put(Constants.JSON_CONSUMER, Constants.JSON_TEST_CONSUMER);
        result.put(Constants.JSON_PROVIDER, Constants.JSON_TEST_PROVIDER_SHA);
        handler.handle(Future.succeededFuture(result));
        return this;
      } else if (!token.equals(Constants.PUBLIC_TOKEN)) {
        // Perform TIP with Auth Server
        Future<JsonObject> tipResponseFut = retrieveTipResponse(token);
        // Check if resource is Open or Secure with Catalogue Server
        Future<HashMap<String, Boolean>> catResponseFut =
            isOpenResource1(request.getJsonArray("ids"), requestEndpoint);
        CompositeFuture.all(tipResponseFut, catResponseFut).onFailure(failedHandler -> {
          LOGGER.debug("Info: TIP / Cat Failed");
          JsonObject result = new JsonObject();
          result.put("status", "error");
          result.put("message", failedHandler.getMessage());
          handler.handle(Future.failedFuture(result.toString()));
        }).onSuccess(successHandler -> {
          JsonObject tipResponse = successHandler.resultAt(0);
          HashMap<String, Boolean> catResponse = successHandler.resultAt(1);
          LOGGER.debug("Info: TIP Response is : " + tipResponse);
          LOGGER.debug("Info: CAT Response is : " + Collections.singletonList(catResponse));

          Future<JsonObject> validateAPI =
              validateAccess(tipResponse, catResponse, authenticationInfo, request);

          validateAPI.onComplete(validateAPIResponseHandler -> {
            if (validateAPIResponseHandler.succeeded()) {
              LOGGER.debug("Info: Success :: TIP Response is : " + tipResponse);
              JsonObject response = validateAPIResponseHandler.result();
              handler.handle(Future.succeededFuture(response));
            } else if (validateAPIResponseHandler.failed()) {
              LOGGER.debug("Info: Failure :: TIP Response is : " + tipResponse);
              String response = validateAPIResponseHandler.cause().getMessage();
              handler.handle(Future.failedFuture(response));
            }
          });
        });
        return this;
      }

    } else {
      if (token.equals(Constants.PUBLIC_TOKEN)
          && !Constants.OPEN_ENDPOINTS.contains(requestEndpoint)) {
        JsonObject result = new JsonObject();
        result.put("status", "error");
        result.put("message", "Public token cannot access requested endpoint");
        handler.handle(Future.failedFuture(result.toString()));
        return this;
      } else {
         if (Constants.CLOSED_ENDPOINTS.contains(requestEndpoint)) {
          tokenInterospectionResultContainer responseContainer =
              new tokenInterospectionResultContainer();
          Future<JsonObject> tipResponseFut = retrieveTipResponse(token);
          tipResponseFut.compose(tipResponse -> {
            responseContainer.tipResponse = tipResponse;
            LOGGER.debug("Info: TIP Response is : " + tipResponse);
            String id = tipResponse.getJsonArray("request").getJsonObject(0).getString("id");
            return isOpenResource1(new JsonArray().add(id), requestEndpoint);
          }).onSuccess(success -> {

            responseContainer.catResponse = success;
            Future<JsonObject> validateAPI = validateAccess(responseContainer.tipResponse,
                responseContainer.catResponse, authenticationInfo, request);
            validateAPI.onComplete(validateAPIResponseHandler -> {
              if (validateAPIResponseHandler.succeeded()) {
                LOGGER.debug("Info: Success :: TIP Response is : " + responseContainer.tipResponse);
                JsonObject response = validateAPIResponseHandler.result();
                handler.handle(Future.succeededFuture(response));
              } else if (validateAPIResponseHandler.failed()) {
                LOGGER.debug("Info: Failure :: TIP Response is : " + responseContainer.tipResponse);
                String response = validateAPIResponseHandler.cause().getMessage();
                handler.handle(Future.failedFuture(response));
              }
            });
            /*
             * String providerID = responseContainer.tipResponse.getJsonArray("request")
             * .getJsonObject(0).getString("id"); String[] id = providerID.split("/"); String
             * providerSHA = id[0] + "/" + id[1]; responseContainer.tipResponse.put("provider",
             * providerSHA); handler.handle(Future.succeededFuture(responseContainer.tipResponse));
             */
          }).onFailure(failure -> {
            JsonObject result = new JsonObject();
            result.put("status", "error");
            result.put("message", failure.getMessage());
            LOGGER.debug("RESULT : " + failure.getCause());
          });
        } else {
          // Based on API perform TIP.
          // For management and subscription no need to look-up at catalogue
          Future<JsonObject> tipResponseFut = retrieveTipResponse(token);
          Future<HashMap<String, Boolean>> catResponseFut =
              isOpenResource1(request.getJsonArray("ids"), requestEndpoint);
          // Based on catalogue item accessPolicy, decide the TIP
          CompositeFuture.all(tipResponseFut, catResponseFut).onFailure(throwable -> {
            LOGGER.debug("Info: TIP / Cat Failed");
            JsonObject result = new JsonObject();
            result.put("status", "error");
            result.put("message", throwable.getMessage());
            LOGGER.debug("RESULT : " + result);
            handler.handle(Future.failedFuture(result.toString()));
          }).onSuccess(compositeFuture -> {
            JsonObject tipResponse = compositeFuture.resultAt(0);
            HashMap<String, Boolean> catResponse = compositeFuture.resultAt(1);
            LOGGER.debug("Info: TIP Response is : " + tipResponse);
            LOGGER.debug("Info: CAT Response is : " + Collections.singletonList(catResponse));

            Future<JsonObject> validateAPI =
                validateAccess(tipResponse, catResponse, authenticationInfo, request);
            validateAPI.onComplete(validateAPIResponseHandler -> {
              if (validateAPIResponseHandler.succeeded()) {
                LOGGER.debug("Info: Success :: TIP Response is : " + tipResponse);
                JsonObject response = validateAPIResponseHandler.result();
                handler.handle(Future.succeededFuture(response));
              } else if (validateAPIResponseHandler.failed()) {
                LOGGER.debug("Info: Failure :: TIP Response is : " + tipResponse);
                String response = validateAPIResponseHandler.cause().getMessage();
                handler.handle(Future.failedFuture(response));
              }
            });
          });
          return this;
        }
      }
    }
    return this;
  }

  private class tokenInterospectionResultContainer {
    JsonObject tipResponse;
    HashMap<String, Boolean> catResponse;
  }

  private boolean isValidEndpoint(String requestEndpoint, JsonArray apis) {
    for (Object es : apis) {
      String endpoint = (String) es;
      if (endpoint.equals("/*") || endpoint.equals(requestEndpoint)) {
        return true;
      }
    }
    return false;
  }

  private JsonObject retrieveTipRequest(String requestID, JsonObject tipResponse) {
    for (Object r : tipResponse.getJsonArray("request")) {
      JsonObject tipRequest = (JsonObject) r;
      String responseID = tipRequest.getString("id");
      if (requestID.equals(responseID)) {
        return tipRequest;
      }
      String escapedResponseID =
          responseID.replace("/", "\\/").replace(".", "\\.").replace("*", ".*");
      Pattern pattern = Pattern.compile(escapedResponseID);
      if (pattern.matcher(requestID).matches()) {
        return tipRequest;
      }
    }
    return new JsonObject();
  }

  private Future<JsonObject> retrieveTipResponse(String token) {
    Promise<JsonObject> promise = Promise.promise();
    if (token.equalsIgnoreCase("public")) {
      promise.complete(Constants.JSON_PUBLIC_TIP_RESPONSE);
      return promise.future();
    }
    JsonObject cacheResponse = tipCache.getIfPresent(token);
    if (cacheResponse == null) {
      LOGGER.debug("Cache miss calling auth server");
      //cache miss
      // call cat-server only when token not found in cache.
      JsonObject body = new JsonObject();
      body.put("token", token);
      webClient.post(443, config.getString(Constants.AUTH_SERVER_HOST), Constants.AUTH_TIP_PATH)
          .expect(ResponsePredicate.JSON).sendJsonObject(body, httpResponseAsyncResult -> {
            if (httpResponseAsyncResult.failed()) {
              promise.fail(httpResponseAsyncResult.cause());
              return;
            }
            HttpResponse<Buffer> response = httpResponseAsyncResult.result();
            if (response.statusCode() != HttpStatus.SC_OK) {
              String errorMessage =
                  response.bodyAsJsonObject().getJsonObject("error").getString("message");
              promise.fail(new Throwable(errorMessage));
              return;
            }
            JsonObject responseBody = response.bodyAsJsonObject();
            String cacheExpiry = Instant.now(Clock.systemUTC())
                .plus(Constants.CACHE_TIMEOUT_AMOUNT, Constants.TIP_CACHE_TIMEOUT_UNIT)
                .toString();
            responseBody.put("cache-expiry", cacheExpiry);
            tipCache.put(token, responseBody);
            promise.complete(responseBody);
          });
    } else {
      LOGGER.debug("Cache Hit");
      promise.complete(cacheResponse);
    }
    return promise.future();
  }

  /**
   * The open resource validator method.
   * 
   * @param requestIDs A json array of strings which are resource IDs
   * @return A future of a hashmap with the key as the resource ID and a boolean value indicating
   *         open or not
   * 
   *         <p>
   *         There is a known problem with the caching mechanism in this function. If an invalid ID
   *         is received, the CAT is called and the result is not cached. The result is cached only
   *         if a valid ID exists in the CAT. If every ID is stored then the cache can balloon in
   *         size until a server reload. Also a non existent ID will get cached and later if the ID
   *         entry is done on the CAT, it does not get auto propagated to the RS. However, in the
   *         current mechanism, there is a DDoS attack vector where an attacker can send multiple
   *         requests for invalid IDs.
   *         </p>
   */
  @Deprecated // clean/delete when all test pass with new method
  private Future<HashMap<String, Boolean>> isOpenResource(JsonArray requestIDs,
      String requestEndpoint) {
    Promise<HashMap<String, Boolean>> promise = Promise.promise();
    HashMap<String, Boolean> result = new HashMap<>();
    if (Constants.OPEN_ENDPOINTS.contains(requestEndpoint)) {
      List<Future> catResponses = new ArrayList<>();
      Promise prom = Promise.promise();
      catResponses.add(prom.future());
      // Check if the resource is already fetched in the cache
      String resID = requestIDs.getString(0);
      if (resourceIdCache.asMap().containsKey(resID)) {
        result.put(resID, resourceIdCache.getIfPresent(resID).equalsIgnoreCase("OPEN"));
        prom.complete();
      } else {
        /*
         * WebClientOptions options = new
         * WebClientOptions().setTrustAll(true).setVerifyHost(false).setSsl(true); WebClient
         * catWebClient = WebClient.create(vertxObj, options);
         */
        // instead of for is there any endpoint in catalogue server to get all resource ACL based on
        // ids array
        for (Object rID : requestIDs) {
          String resourceID = (String) rID;
          String[] idComponents = resourceID.split("/");
          if (idComponents.length < 4) {
            continue;
          }
          String groupID = (idComponents.length == 4) ? resourceID
              : String.join("/", Arrays.copyOfRange(idComponents, 0, 4));

          String catHost = config.getString("catServerHost");
          int catPort = Integer.parseInt(config.getString("catServerPort"));
          String catPath = Constants.CAT_RSG_PATH;
          LOGGER.debug("Info: Host " + catHost + " Port " + catPort + " Path " + catPath);
          // Check if resourceID is available
          catWebClient.get(catPort, catHost, catPath).addQueryParam("property", "[id]")
              .addQueryParam("value", "[[" + resourceID + "]]").addQueryParam("filter", "[id]")
              .expect(ResponsePredicate.JSON).send(httpResponserIDAsyncResult -> {
                if (httpResponserIDAsyncResult.failed()) {
                  result.put(resourceID, false);
                  prom.fail("Not Found");
                  return;
                }
                HttpResponse<Buffer> rIDResponse = httpResponserIDAsyncResult.result();
                JsonObject rIDResponseBody = rIDResponse.bodyAsJsonObject();

                if (rIDResponse.statusCode() != HttpStatus.SC_OK) {
                  LOGGER.debug("Info: Catalogue Query failed");
                  result.put(resourceID, false);
                  prom.fail("Not Found");
                  return;
                } else if (!rIDResponseBody.getString("status").equals("success")) {
                  LOGGER.debug("Info: Catalogue Query failed");
                  result.put(resourceID, false);
                  prom.fail("Not Found");
                  return;
                } else if (rIDResponseBody.getInteger("totalHits") == 0) {
                  LOGGER.debug("Info: Resource ID invalid : Catalogue item Not Found");
                  result.put(resourceID, false);
                  prom.fail("Not Found");
                  return;
                } else {
                  LOGGER.debug("Info: Resource ID valid : Catalogue item Found");
                  catWebClient.get(catPort, catHost, catPath).addQueryParam("property", "[id]")
                      .addQueryParam("value", "[[" + groupID + "]]")
                      .addQueryParam("filter", "[accessPolicy]").expect(ResponsePredicate.JSON)
                      .send(httpResponseAsyncResult -> {
                        if (httpResponseAsyncResult.failed()) {
                          result.put(resourceID, false);
                          prom.fail("Not Found");
                          return;
                        }
                        HttpResponse<Buffer> response = httpResponseAsyncResult.result();
                        if (response.statusCode() != HttpStatus.SC_OK) {
                          result.put(resourceID, false);
                          prom.fail("Not Found");
                          return;
                        }
                        JsonObject responseBody = response.bodyAsJsonObject();
                        if (!responseBody.getString("status").equals("success")) {
                          result.put(resourceID, false);
                          prom.fail("Not Found");
                          return;
                        }
                        String resourceACL = "SECURE";
                        try {
                          resourceACL = responseBody.getJsonArray("results").getJsonObject(0)
                              .getString("accessPolicy");
                          result.put(resourceID, resourceACL.equals("OPEN"));
                          resourceGroupCache.put(groupID, resourceACL);
                          resourceIdCache.put(resourceID, resourceACL);
                          LOGGER.debug("Info: Group ID valid : Catalogue item Found");
                        } catch (IndexOutOfBoundsException ignored) {
                          LOGGER.error(ignored.getMessage());
                          LOGGER.debug(
                              "Info: Group ID invalid : Empty response in results from Catalogue");
                        }
                        prom.complete();
                      });
                }
              });
        }
      }
    } else {
      result.put("Closed End Point", true);
      promise.complete();
    }
    return promise.future();
  }

  private Future<HashMap<String, Boolean>> isOpenResource1(JsonArray requestIDs,
      String requestEndpoint) {
    LOGGER.debug("isOpenResource1() started");
    Promise<HashMap<String, Boolean>> promise = Promise.promise();
    HashMap<String, Boolean> result = new HashMap<>();
    final int requestIdSize = requestIDs.size();
    final AtomicInteger counter = new AtomicInteger();
    if (Constants.OPEN_ENDPOINTS.contains(requestEndpoint) && requestIDs.size() > 0) {
      Iterator<Object> itr = requestIDs.iterator();
      while (itr.hasNext()) {
        String rId = (String) itr.next();
        String ACL = resourceIdCache.getIfPresent(rId);
        if (ACL != null) {
          LOGGER.debug("Cache Hit");
          result.put(rId, ACL.equalsIgnoreCase("OPEN"));
          counter.getAndIncrement();
          doComplete(promise, counter.intValue(), requestIdSize, result);
        } else {
          // cache miss
          LOGGER.debug("Cache miss calling cat server");
          String[] idComponents = rId.split("/");
          if (idComponents.length < 4) {
            promise.fail("Not Found " + rId);
          }
          String groupId = (idComponents.length == 4) ? rId
              : String.join("/", Arrays.copyOfRange(idComponents, 0, 4));
          // 1. check group accessPolicy.
          // 2. check resource exist, if exist set accessPolicy to group accessPolicy. else fail
          Future<String> groupACLFuture = getGroupAccessPolicy(groupId);
          groupACLFuture.compose(groupACLResult -> {
            String groupPolicy = (String) groupACLResult;
            return isResourceExist(rId, groupPolicy);
          }).onSuccess(handler -> {
            result.put(rId, resourceIdCache.getIfPresent(rId).equalsIgnoreCase("OPEN"));
            counter.getAndIncrement();
            doComplete(promise, counter.intValue(), requestIdSize, result);
          }).onFailure(handler -> {
            LOGGER.error("cat response failed for Id : (" + rId + ")" + handler.getCause());
            result.put(rId, false);
            promise.fail("Not Found " + rId);
          });
        }
      }
    } else {
      // process for /adapter or /subscription
      LOGGER.debug("resource exist" + requestIDs.getString(0));
      isItemExist(requestIDs.getString(0)).onComplete(handler -> {
        if (handler.succeeded()) {
          LOGGER.debug("item exist succeeded");
          result.put("Closed End Point", true);
          promise.complete(result);
        } else {
          LOGGER.error("cat response failed for Item : ");
          result.put("Closed End Point", false);
          promise.fail("Not Found ");
        }
      });
    }
    return promise.future();
  }


  private <T> void doComplete(Promise<T> promise, int counter, int size, T result) {
    if (counter == size) {
      promise.complete(result);
    }
  }

  private Future<String> getGroupAccessPolicy(String groupId) {
    LOGGER.debug("getGroupAccessPolicy() started");
    Promise<String> promise = Promise.promise();
    String groupACL = resourceGroupCache.getIfPresent(groupId);
    if (groupACL != null) {
      LOGGER.debug("Info : cache Hit");
      promise.complete(groupACL);
    } else {
      LOGGER.debug("Info : cache miss");
      catWebClient.get(catPort, catHost, catPath).addQueryParam("property", "[id]")
          .addQueryParam("value", "[[" + groupId + "]]").addQueryParam("filter", "[accessPolicy]")
          .expect(ResponsePredicate.JSON).send(httpResponseAsyncResult -> {
            if (httpResponseAsyncResult.failed()) {
              LOGGER.error(httpResponseAsyncResult.cause());
              promise.fail("Resource not found");
              return;
            }
            HttpResponse<Buffer> response = httpResponseAsyncResult.result();
            if (response.statusCode() != HttpStatus.SC_OK) {
              promise.fail("Resource not found");
              return;
            }
            JsonObject responseBody = response.bodyAsJsonObject();
            if (!responseBody.getString("status").equals("success")) {
              promise.fail("Resource not found");
              return;
            }
            String resourceACL = "SECURE";
            try {
              resourceACL =
                  responseBody.getJsonArray("results").getJsonObject(0).getString("accessPolicy");
              resourceGroupCache.put(groupId, resourceACL);
              LOGGER.debug("Info: Group ID valid : Catalogue item Found");
              promise.complete(resourceACL);
            } catch (Exception ignored) {
              LOGGER.error(ignored.getMessage());
              LOGGER.debug("Info: Group ID invalid : Empty response in results from Catalogue");
              promise.fail("Resource not found");
            }
          });
    }
    return promise.future();
  }

  private Future<Boolean> isResourceExist(String id, String groupACL) {
    LOGGER.debug("isResourceExist() started");
    Promise<Boolean> promise = Promise.promise();
    String catHost = config.getString("catServerHost");
    int catPort = Integer.parseInt(config.getString("catServerPort"));
    String catPath = Constants.CAT_RSG_PATH;
    String resourceExist = resourceIdCache.getIfPresent(id);
    if (resourceExist != null) {
      LOGGER.debug("Info : cache Hit");
      promise.complete(true);
    } else {
      LOGGER.debug("Info : Cache miss : call cat server");
      catWebClient.get(catPort, catHost, catPath).addQueryParam("property", "[id]")
          .addQueryParam("value", "[[" + id + "]]").addQueryParam("filter", "[id]")
          .expect(ResponsePredicate.JSON).send(responseHandler -> {
            if (responseHandler.failed()) {
              promise.fail("false");
            }
            HttpResponse<Buffer> response = responseHandler.result();
            JsonObject responseBody = response.bodyAsJsonObject();
            if (response.statusCode() != HttpStatus.SC_OK) {
              promise.fail("false");
            } else if (!responseBody.getString("status").equals("success")) {
              promise.fail("Not Found");
              return;
            } else if (responseBody.getInteger("totalHits") == 0) {
              LOGGER.debug("Info: Resource ID invalid : Catalogue item Not Found");
              promise.fail("Not Found");
            } else {
              LOGGER.debug("is Exist response : " + responseBody);
              resourceIdCache.put(id, groupACL);
              promise.complete(true);
            }
          });
    }
    return promise.future();
  }

  private Future<Boolean> isItemExist(String itemId) {
    LOGGER.debug("isItemExist() started");
    Promise<Boolean> promise = Promise.promise();
    String id = itemId.replace("/*", "");
    LOGGER.info("id : " + id);
    catWebClient.get(catPort, catHost, "/iudx/cat/v1/item").addQueryParam("id", id)
        .expect(ResponsePredicate.JSON).send(responseHandler -> {
          if (responseHandler.succeeded()) {
            HttpResponse<Buffer> response = responseHandler.result();
            JsonObject responseBody = response.bodyAsJsonObject();
            if (responseBody.getString("status").equalsIgnoreCase("success")
                && responseBody.getInteger("totalHits") > 0) {
              promise.complete(true);
            } else {
              promise.fail(responseHandler.cause());
            }
          } else {
            promise.fail(responseHandler.cause());
          }
        });
    return promise.future();
  }

  private Future<JsonObject> validateAccess(JsonObject result, HashMap<String, Boolean> catResponse,
      JsonObject authenticationInfo, JsonObject userRequest) {

    Promise<JsonObject> promise = Promise.promise();
    LOGGER.debug("Info: TIP response is " + result);
    LOGGER.debug("Info: Authentication Info is " + authenticationInfo);
    LOGGER.debug("Info: catResponse is " + catResponse);
    String requestEndpoint = authenticationInfo.getString("apiEndpoint");
    String requestMethod = authenticationInfo.getString("method");

    LOGGER.debug("Info: requested endpoint :" + requestEndpoint);
    
    //TODO : check for validation placement.
    if(!result.getJsonObject("request").getJsonArray("apis").contains(requestEndpoint)) {
      promise.fail("Info: Failure :: No access to " + requestEndpoint);
    }

    // 1. Check the API requested.
    if (Constants.OPEN_ENDPOINTS.contains(requestEndpoint)) {
      JsonObject response = new JsonObject();
      LOGGER.info(Constants.OPEN_ENDPOINTS);

      // 1.1. Check with catalogue if resource is open or secure.
      // 1.2. If open respond success.
      // 1.3. If closed, check if auth response has access to the requested resource.

      LOGGER.debug("Info: TIP response is " + result);

      String allowedID = result.getJsonArray("request").getJsonObject(0).getString("id");
      String allowedGroupID = allowedID.substring(0, allowedID.lastIndexOf("/"));

      LOGGER.debug("Info: allowedID is " + allowedID);
      LOGGER.debug("Info: allowedGroupID is " + allowedGroupID);

      LOGGER.debug("Info: userRequest is " + userRequest);

      String requestedID = userRequest.getJsonArray("ids").getString(0);
      String requestedGroupID = requestedID.substring(0, requestedID.lastIndexOf("/"));

      LOGGER.debug("Info: requestedID is " + requestedID);
      LOGGER.debug("Info: requestedGroupID is " + requestedGroupID);

      // Check if resource is available in Catalogue
      if (catResponse.isEmpty()) {
        LOGGER.debug("Info: No such catalogue item");
        response.put("item", "Not Found");
        promise.fail(response.toString());
      } else {
        if (catResponse.get(requestedID)) {
          LOGGER.debug("Info: Catalogue item is OPEN");
          response.put(Constants.JSON_CONSUMER, result.getString(Constants.JSON_CONSUMER));
          promise.complete(response);
        } else {
          // Check if the token has access to the requestedID
          LOGGER.debug("Info: Catalogue item is SECURE");
          if (requestedGroupID.equalsIgnoreCase(allowedGroupID)) {
            LOGGER.debug("Info: Catalogue item is SECURE and User has ACCESS");
            response.put(Constants.JSON_CONSUMER, result.getString(Constants.JSON_CONSUMER));
            promise.complete(response);
          } else {
            LOGGER.debug("Info: Catalogue item is SECURE and User does not have ACCESS");
            response.put(Constants.JSON_CONSUMER, result.getString(Constants.JSON_PUBLIC_CONSUMER));
            promise.fail(response.toString());
          }
        }
      }

    } else if (Constants.ADAPTER_ENDPOINT.contains(requestEndpoint)) {
      LOGGER.debug("Info: Requested access for " + requestEndpoint);
      JsonArray tipresult = result.getJsonArray("request");
      JsonObject tipresponse = tipresult.getJsonObject(0);
      LOGGER.debug("Info: Allowed APIs " + tipresponse);
      JsonArray allowedAPIs = tipresponse.getJsonArray("apis");
      int total = allowedAPIs.size();
      boolean allowedAccess = false;
      for (int i = 0; i < total; i++) {
        if (Constants.ADAPTER_ENDPOINT.contains(allowedAPIs.getString(i))) {
          LOGGER.debug("Info: Success :: User has access to " + requestEndpoint + " API");
          allowedAccess = true;
          break;
        }
      }
      if (allowedAccess) {
        String providerID = tipresponse.getString("id");
        String adapterID = providerID.substring(0, providerID.lastIndexOf("/"));
        String[] id = providerID.split("/");
        String providerSHA = id[0] + "/" + id[1];
        LOGGER
            .debug("Info: Success :: Provider SHA is " + providerSHA + "method : " + requestMethod);
        if (requestMethod.equalsIgnoreCase("POST")) {
          String resourceGroup = userRequest.getString("resourceGroup");
          String resourceServer = userRequest.getString("resourceServer");
          //System.out.println(providerID);
          //System.out.println(resourceGroup);
          //System.out.println(resourceServer);
          if (providerID.contains(resourceServer + "/" + resourceGroup)) {
            LOGGER.info(
                "Success :: Has access to " + requestEndpoint + " API and Adapter " + adapterID);
            result.put("provider", providerSHA);
            promise.complete(result);
          } else {
            LOGGER.debug("Info: Failure :: Has access to " + requestEndpoint
                + " API but not for Adapter " + adapterID);
            promise.fail(result.toString());
          }
        } else {
          String requestId = authenticationInfo.getString("id");
          LOGGER.debug("id : " + requestId);
          if (requestId.contains(adapterID)) {
            LOGGER.info(
                "Success :: Has access to " + requestEndpoint + " API and Adapter " + requestId);
            promise.complete(result);
          } else {
            LOGGER.debug("Info: Failure :: Has access to " + requestEndpoint
                + " API but not for Adapter " + requestId);
            promise.fail(result.toString());
          }
        }
      } else {
        LOGGER.debug("Info: Failure :: No access to " + requestEndpoint + " API");
        promise.fail(result.toString());
      }
    } else if (Constants.SUBSCRIPTION_ENDPOINT.contains(requestEndpoint)) {
      LOGGER.debug("Info: Requested access for " + requestEndpoint);
      JsonArray tipresult = result.getJsonArray("request");
      JsonObject tipresponse = tipresult.getJsonObject(0);
      LOGGER.debug("Info: Allowed APIs " + tipresponse);
      JsonArray allowedAPIs = tipresponse.getJsonArray("apis");
      int total = allowedAPIs.size();
      boolean allowedAccess = false;
      for (int i = 0; i < total; i++) {
        if (Constants.SUBSCRIPTION_ENDPOINT.contains(allowedAPIs.getString(i))) {
          LOGGER.debug("Info: Success :: User has access to API");
          allowedAccess = true;
          break;
        }
      }

      if (allowedAccess) {
        if (requestMethod.equalsIgnoreCase("POST")) {
          String allowedId = tipresponse.getString("id");
          String id = userRequest.getJsonArray("entities").getString(0);
          String requestedId = id.substring(0, id.lastIndexOf("/"));

          if (allowedId.contains(requestedId)) {
            LOGGER.debug(
                "Info: Success :: Has access to " + requestEndpoint + " API and entity " + id);
            promise.complete(result);
          } else {
            LOGGER.info(
                "Failure :: Has access to " + requestEndpoint + " API but not for entity " + id);
            promise.fail(result.toString());
          }
        } else if (requestMethod.equalsIgnoreCase("PUT")
            || requestMethod.equalsIgnoreCase("PATCH")) {
          String requestId = authenticationInfo.getString("id");
          String email = result.getString("consumer");
          String allowedId = tipresponse.getString("id");
          String id = userRequest.getJsonArray("entities").getString(0);
          String requestedId = id.substring(0, id.lastIndexOf("/"));
          if (requestId.contains(Util.getSha(email))) {
            LOGGER.debug("Info: Success :: Has access to " + requestEndpoint
                + " API and Subscription ID " + requestId);
            if (allowedId.contains(requestedId)) {
              LOGGER.debug("Info: Success :: Has access to " + requestEndpoint
                  + " API and Subscription ID " + requestId + " and entity " + id);
              promise.complete(result);
            } else {
              LOGGER.debug("Info: Failure :: Has access to " + requestEndpoint
                  + " API and Subscription ID " + requestId + " but not for entity " + id);
              promise.fail(result.toString());
            }
          } else {
            LOGGER.debug("Info: Failure");
            promise.fail(result.toString());
          }
        } else {
          String requestId = authenticationInfo.getString("id");
          String email = result.getString("consumer");
          if (requestId.contains(Util.getSha(email))) {
            LOGGER.debug("Info: Success :: Has access to " + requestEndpoint
                + " API and Subscription ID " + requestId);
            promise.complete(result);
          } else {
            LOGGER.info("Failure :: Has access to " + requestEndpoint
                + " API but not for Subscription ID " + requestId);
            promise.fail(result.toString());
          }
        }
      } else {
        LOGGER.debug("Info: Failure :: No access to " + requestEndpoint + " API");
        promise.fail(result.toString());
      }
    } else if (Constants.MANAGEMENT_ENDPOINTS.contains(requestEndpoint)) {
      LOGGER.debug("Info: Requested access for " + requestEndpoint);
      JsonArray tipresult = result.getJsonArray("request");
      JsonObject tipresponse = tipresult.getJsonObject(0);
      LOGGER.debug("Info: Allowed APIs " + tipresponse);
      JsonArray allowedAPIs = tipresponse.getJsonArray("apis");
      int total = allowedAPIs.size();
      boolean allowedAccess = false;
      String providerID = tipresponse.getString("id");
      String[] id = providerID.split("/");
      String providerSHA = id[0] + "/" + id[1];
      String email = result.getString("consumer");
      if (providerSHA.equalsIgnoreCase(Constants.JSON_IUDX_ADMIN_SHA)) {
        for (int i = 0; i < total; i++) {
          if (Constants.MANAGEMENT_ENDPOINT.contains(allowedAPIs.getString(i))) {
            LOGGER.debug("Info: Success :: User " + email + " has access to API");
            allowedAccess = true;
            break;
          }
        }
      }

      if (allowedAccess) {
        LOGGER.debug("Info: Success :: Has access to " + requestEndpoint + " API");
        promise.complete(result);
      } else {
        LOGGER.debug("Info: Failure :: No access to " + requestEndpoint + " API");
        promise.fail(result.toString());
      }
    }
    return promise.future();
  }
}
