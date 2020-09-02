package iudx.resource.server.authenticator;

import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.http.HttpStatus;

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

  private static final Logger logger = LoggerFactory.getLogger(AuthenticationServiceImpl.class);
  private static final ConcurrentHashMap<String, JsonObject> tipCache = new ConcurrentHashMap<>();
  private static final ConcurrentHashMap<String, String> catCache = new ConcurrentHashMap<>();
  private static final Properties properties = new Properties();
  private final WebClient webClient;
  private final Vertx vertxObj;

  /**
   * This is a constructor which is used by the DataBroker Verticle to instantiate a RabbitMQ
   * client.
   * 
   * @param vertx which is a vertx instance
   * @param client which is a Vertx Web client
   */

  public AuthenticationServiceImpl(Vertx vertx, WebClient client) {
    webClient = client;
    vertxObj = vertx;
    try {
      FileInputStream configFile = new FileInputStream(Constants.CONFIG_FILE);
      if (properties.isEmpty()) {
        properties.load(configFile);
      }
    } catch (IOException e) {
      logger.error("Could not load properties from config file", e);
    }

    long cacheCleanupTime = 1000 * 60 * Constants.TIP_CACHE_TIMEOUT_AMOUNT;
    vertx.setPeriodic(cacheCleanupTime, timerID -> tipCache.values().removeIf(entry -> {
      Instant tokenExpiry = Instant.parse(entry.getString("expiry"));
      Instant cacheExpiry = Instant.parse(entry.getString("cache-expiry"));
      Instant now = Instant.now(Clock.systemUTC());
      return (now.isAfter(tokenExpiry) || now.isAfter(cacheExpiry));
    }));
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public AuthenticationService tokenInterospect(JsonObject request, JsonObject authenticationInfo,
      Handler<AsyncResult<JsonObject>> handler) {

    String token = authenticationInfo.getString("token", Constants.PUBLIC_TOKEN);
    String requestEndpoint = authenticationInfo.getString("apiEndpoint");
    logger.info("requested endpoint :" + requestEndpoint);
    if (token.equals(Constants.PUBLIC_TOKEN)
        && !Constants.OPEN_ENDPOINTS.contains(requestEndpoint)) {
      JsonObject result = new JsonObject();
      result.put("status", "error");
      result.put("message", "Public token cannot access requested endpoint");
      handler.handle(Future.succeededFuture(result));
      return this;
    }

    Future<JsonObject> tipResponseFut = retrieveTipResponse(token);
    Future<HashMap<String, Boolean>> catResponseFut = isOpenResource(request.getJsonArray("ids"));

    CompositeFuture.all(tipResponseFut, catResponseFut).onFailure(throwable -> {
      JsonObject result = new JsonObject();
      result.put("status", "error");
      result.put("message", throwable.getMessage());
      handler.handle(Future.succeededFuture(result));
    }).onSuccess(compositeFuture -> {
      JsonObject tipResponse = compositeFuture.resultAt(0);
      HashMap<String, Boolean> catResponse = compositeFuture.resultAt(1);
      JsonArray result = new JsonArray();

      for (Object reqID : request.getJsonArray("ids")) {
        String requestID = (String) reqID;
        JsonObject tipRequest = retrieveTipRequest(requestID, tipResponse);
        boolean isAccessible = tipRequest.isEmpty() ? catResponse.getOrDefault(requestID, false)
            : isValidEndpoint(requestEndpoint, tipRequest.getJsonArray("apis"));
        result.add(new JsonObject().put("id", requestID).put("accessible", isAccessible));
      }

      JsonArray inAccessibleIDs = new JsonArray(
          result.stream().filter(res -> !(((JsonObject) res).getBoolean("accessible")))
              .collect(Collectors.toList()));

      JsonObject json = new JsonObject();
      json.put("status", inAccessibleIDs.isEmpty() ? "success" : "error");
      json.put("body", new JsonObject());
      if (!inAccessibleIDs.isEmpty()) {
        json.put("message", "Unauthorized resource IDs");
        json.getJsonObject("body").put("rejected", inAccessibleIDs);
      }
      handler.handle(Future.succeededFuture(json));
    });
    return this;
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

    JsonObject cacheResponse = tipCache.getOrDefault(token, new JsonObject());
    if (!cacheResponse.isEmpty()) {
      try {
        Instant tokenExpiry = Instant.parse(cacheResponse.getString("expiry"));
        Instant cacheExpiry = Instant.parse(cacheResponse.getString("cache-expiry"));
        Instant now = Instant.now(Clock.systemUTC());
        if (tokenExpiry.isBefore(now)) {
          if (!tipCache.remove(token, cacheResponse)) {
            throw new ConcurrentModificationException("TIP cache premature invalidation");
          }
          promise.fail(new Throwable("Token has expired"));
        }
        if (cacheExpiry.isAfter(now)) {
          String extendedCacheExpiry =
              now.plus(Constants.TIP_CACHE_TIMEOUT_AMOUNT, Constants.TIP_CACHE_TIMEOUT_UNIT)
                  .toString();
          JsonObject newCacheEntry = cacheResponse.copy();
          newCacheEntry.put("cache-expiry", extendedCacheExpiry);
          if (!tipCache.replace(token, cacheResponse, newCacheEntry)) {
            throw new ConcurrentModificationException("TIP cache premature invalidation");
          }
          promise.complete(newCacheEntry);
          return promise.future();
        } else {
          if (!tipCache.remove(token, cacheResponse)) {
            throw new ConcurrentModificationException("TIP cache premature invalidation");
          }
        }
      } catch (DateTimeParseException | ConcurrentModificationException e) {
        logger.error(e.getMessage());
      }
    }

    JsonObject body = new JsonObject();
    body.put("token", token);
    webClient.post(443, properties.getProperty(Constants.AUTH_SERVER_HOST), Constants.AUTH_TIP_PATH)
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
              .plus(Constants.TIP_CACHE_TIMEOUT_AMOUNT, Constants.TIP_CACHE_TIMEOUT_UNIT)
              .toString();
          responseBody.put("cache-expiry", cacheExpiry);
          tipCache.put(token, responseBody);
          promise.complete(responseBody);
        });
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
  private Future<HashMap<String, Boolean>> isOpenResource(JsonArray requestIDs) {
    Promise<HashMap<String, Boolean>> promise = Promise.promise();
    HashMap<String, Boolean> result = new HashMap<>();
    List<Future> catResponses = new ArrayList<>();
    WebClientOptions options =
        new WebClientOptions().setTrustAll(true).setVerifyHost(false).setSsl(true);
    WebClient catWebClient = WebClient.create(vertxObj, options);
    for (Object rID : requestIDs) {
      String resourceID = (String) rID;
      String[] idComponents = resourceID.split("/");
      if (idComponents.length < 4) {
        continue;
      }
      String groupID = (idComponents.length == 4) ? resourceID
          : String.join("/", Arrays.copyOfRange(idComponents, 0, 4));
      Promise prom = Promise.promise();
      catResponses.add(prom.future());
      if (catCache.containsKey(groupID)) {
        result.put(resourceID, catCache.get(groupID).equals("OPEN"));
        prom.complete();
        continue;
      }
      String catHost = properties.getProperty("catServerHost");
      int catPort = Integer.parseInt(properties.getProperty("catServerPort"));
      String catPath = Constants.CAT_RSG_PATH;
      catWebClient.get(catPort, catHost, catPath).addQueryParam("property", "[id]")
          .addQueryParam("value", "[[" + groupID + "]]")
          .addQueryParam("filter", "[resourceAuthControlLevel]").expect(ResponsePredicate.JSON)
          .send(httpResponseAsyncResult -> {
            if (httpResponseAsyncResult.failed()) {
              result.put(resourceID, false);
              prom.complete();
              return;
            }
            HttpResponse<Buffer> response = httpResponseAsyncResult.result();
            if (response.statusCode() != HttpStatus.SC_OK) {
              result.put(resourceID, false);
              prom.complete();
              return;
            }
            JsonObject responseBody = response.bodyAsJsonObject();
            if (!responseBody.getString("status").equals("success")) {
              result.put(resourceID, false);
              prom.complete();
              return;
            }
            String resourceACL = "CLOSED";
            try {
              resourceACL = responseBody.getJsonArray("results").getJsonObject(0)
                  .getString("resourceAuthControlLevel");
              result.put(resourceID, resourceACL.equals("OPEN"));
              catCache.put(groupID, resourceACL);
            } catch (IndexOutOfBoundsException ignored) {
              logger.error(ignored.getMessage());
              logger.info("Group ID invalid : Empty response in results from Catalogue");
            }
            prom.complete();
          });
    }

    CompositeFuture.all(catResponses).onSuccess(compositeFuture -> promise.complete(result));

    return promise.future();
  }

}
