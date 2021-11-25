package iudx.resource.server.database.latest;

import static iudx.resource.server.database.archives.Constants.ATTRIBUTE_LIST;
import static iudx.resource.server.database.archives.Constants.EMPTY_RESOURCE_ID;
import static iudx.resource.server.database.archives.Constants.FAILED;
import static iudx.resource.server.database.archives.Constants.ID;
import static iudx.resource.server.database.archives.Constants.ID_NOT_FOUND;
import static iudx.resource.server.database.archives.Constants.SUCCESS;

import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.database.archives.ResponseBuilder;

/**
 * The LatestData Service Implementation.
 * <h1>LatestData Service Implementation</h1>
 * <p>
 * The LatestData Service implementation in the IUDX Resource Server implements the definitions of
 * the {@link iudx.resource.server.database.latest.LatestDataService}.
 * </p>
 *
 * @version 1.0
 * @since 2021-03-26
 */

public class LatestDataServiceImpl implements LatestDataService {

  RedisClient redisClient;
  private ResponseBuilder responseBuilder;
  JsonObject attributeList;
  private static final Logger LOGGER = LogManager.getLogger(LatestDataServiceImpl.class);
  // private RedisAPI redisAPI;
  private RedisCommandArgsBuilder redisCmdBuilder = new RedisCommandArgsBuilder();

  public LatestDataServiceImpl(RedisClient client, JsonObject attributeList) {
    this.redisClient = client;
    this.attributeList = attributeList;
  }

  /**
   * Performs a Latest search query using the Redis JReJSON client.
   *
   * @param request Json object received from the ApiServerVerticle
   * @param handler Handler to return redis response in case of success and appropriate error message
   *        in case of failure
   */

  @Override
  public LatestDataService getLatestData(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    request.put(ATTRIBUTE_LIST, attributeList);

      System.out.println(request);
    // Exceptions
    if (!request.containsKey(ID)) {
      LOGGER.debug("Info: " + ID_NOT_FOUND);
      responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(ID_NOT_FOUND);
      handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
      return null;
    }

    if (request.getJsonArray(ID).isEmpty()) {
      LOGGER.debug("Info: " + EMPTY_RESOURCE_ID);
      responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400)
          .setMessage(EMPTY_RESOURCE_ID);
      handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
      return null;
    }

    String id = request.getJsonArray(ID).getString(0);
    RedisArgs args = redisCmdBuilder.getRedisCommandArgs(id, isGroupLevelRecord(id));

    System.out.println("key : "+args.getKey()+"path : "+args.getPath());
    JsonArray response = new JsonArray();
    redisClient.searchAsync(args.getKey(), args.getPath(), searchRes -> {
      if (searchRes.succeeded()) {
        LOGGER.debug("Success: Successful Redis request");
        response.addAll(extractValues(args.getKey(), searchRes.result(), isGroupLevelRecord(id)));
        responseBuilder = new ResponseBuilder(SUCCESS).setTypeAndTitle(200).setMessage(response);
        handler.handle(Future.succeededFuture(responseBuilder.getResponse()));
      } else {
        LOGGER.error("Fail: Redis Cache Request;" + searchRes.cause().getMessage());
        handler.handle(Future.failedFuture(searchRes.cause().getMessage()));
      }
    });

    return this;
  }

  private JsonArray extractValues(String key, JsonObject result, boolean groupSnapshot) {
    if (groupSnapshot) {
      result.remove(key);
      return new JsonArray(result.stream().map(e -> e.getValue()).collect(Collectors.toList()));
    }
    return new JsonArray().add(result);
  }

  public boolean isGroupLevelRecord(String id) {
    return this.attributeList.containsKey(id);
  }
}
