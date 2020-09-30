package iudx.resource.server.database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static iudx.resource.server.database.Constants.*;

/**
 * The Database Service Implementation.
 * <h1>Database Service Implementation</h1>
 * <p>
 * The Database Service implementation in the IUDX Resource Server implements the definitions of the
 * {@link iudx.resource.server.database.DatabaseService}.
 * </p>
 * 
 * @version 1.0
 * @since 2020-05-31
 */

public class DatabaseServiceImpl implements DatabaseService {

  private static final Logger LOGGER = LogManager.getLogger(DatabaseServiceImpl.class);
  private final ElasticClient client;
  private JsonObject query;
  private QueryDecoder queryDecoder = new QueryDecoder();
  private ResponseBuilder responseBuilder;

  public DatabaseServiceImpl(ElasticClient client) {
    this.client = client;
  }

  /**
   * Performs a ElasticSearch search query using the low level REST client.
   * 
   * @param request Json object received from the ApiServerVerticle
   * @param handler Handler to return database response in case of success and appropriate error
   *        message in case of failure
   */
  @Override
  public DatabaseService searchQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    LOGGER.debug("Info: searchQuery;" + request.toString());

    request.put(SEARCH_KEY, true);
    // TODO : only for testing comment after testing.
    request.put("isTest", true);

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

    if (!request.containsKey(SEARCH_TYPE)) {
      LOGGER.debug("Info: " + SEARCHTYPE_NOT_FOUND);
      responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400)
          .setMessage(SEARCHTYPE_NOT_FOUND);
      handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
      return null;
    }

    if (request.getJsonArray(ID).getString(0).split("/").length != 5) {
      LOGGER.error("Malformed ID: " + request.getJsonArray(ID).getString(0));
      responseBuilder =
          new ResponseBuilder(FAILED).setTypeAndTitle(400)
              .setMessage(MALFORMED_ID + request.getJsonArray(ID));
      handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
      return null;
    }

    List<String> splitId = new LinkedList<>(Arrays.asList(request.getJsonArray(ID)
        .getString(0).split("/")));
    splitId.remove(splitId.size() - 1);
    String index = String.join("__", splitId);
    index = index.concat(SEARCH_REQ_PARAM);
    LOGGER.debug("Index name: " + index);

    query = queryDecoder.queryDecoder(request);
    if (query.containsKey(ERROR)) {
      LOGGER.error("Fail: Query returned with an error: " + query.getString(ERROR));
      responseBuilder =
          new ResponseBuilder(FAILED).setTypeAndTitle(400)
              .setMessage(query.getString(ERROR));
      handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
      return null;
    }

    LOGGER.debug("Info: Query constructed: " + query.toString());
    if (LATEST_SEARCH.equalsIgnoreCase(request.getString(SEARCH_TYPE))) {
      client.searchAsync(LATEST_RESOURCE_INDEX, FILTER_PATH_VAL_LATEST, query.toString(),
          searchRes -> {
            if (searchRes.succeeded()) {
              LOGGER.debug("Success: Successful DB request");
              handler.handle(Future.succeededFuture(searchRes.result()));
            } else {
              LOGGER.error("Fail: DB Request;" + searchRes.cause().getMessage());
              handler.handle(Future.failedFuture(searchRes.cause().getMessage()));
            }
          });
    } else {
      client.searchAsync(index, FILTER_PATH_VAL, query.toString(),
          searchRes -> {
          if (searchRes.succeeded()) {
            LOGGER.debug("Success: Successful DB request");
            handler.handle(Future.succeededFuture(searchRes.result()));
          } else {
            LOGGER.error("Fail: DB Request;" + searchRes.cause().getMessage());
            handler.handle(Future.failedFuture(searchRes.cause().getMessage()));
          }
        });
    }
    return this;
  }

  /**
   * Performs a ElasticSearch count query using the low level REST client.
   * 
   * @param request Json object received from the ApiServerVerticle
   * @param handler Handler to return database response in case of success and appropriate error
   *        message in case of failure
   */
  @Override
  public DatabaseService countQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    LOGGER.debug("Info: countQuery;" + request.toString());

    request.put(SEARCH_KEY, false);

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

    if (!request.containsKey(SEARCH_TYPE)) {
      LOGGER.debug("Info: " + SEARCHTYPE_NOT_FOUND);
      responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400)
          .setMessage(SEARCHTYPE_NOT_FOUND);
      handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
      return null;
    }

    if (request.getJsonArray(ID).getString(0).split("/").length != 5) {
      LOGGER.error("Malformed ID: " + request.getJsonArray(ID).getString(0));
      responseBuilder =
          new ResponseBuilder(FAILED).setTypeAndTitle(400)
              .setMessage(MALFORMED_ID + request.getJsonArray(ID));
      handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
      return null;
    }

    List<String> splitId = new LinkedList<>(Arrays.asList(request.getJsonArray(ID)
        .getString(0).split("/")));
    splitId.remove(splitId.size() - 1);
    String index = String.join("__", splitId);
    index = index.concat(COUNT_REQ_PARAM);
    LOGGER.debug("Index name: " + index);

    query = queryDecoder.queryDecoder(request);
    if (query.containsKey(ERROR)) {
      LOGGER.error("Fail: Query returned with an error: " + query.getString(ERROR));
      responseBuilder =
          new ResponseBuilder(FAILED).setTypeAndTitle(400)
              .setMessage(query.getString(ERROR));
      handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
      return null;
    }

    LOGGER.debug("Info: Query constructed: " + query.toString());

    client.countAsync(index, query.toString(), countRes -> {
      if (countRes.succeeded()) {
        LOGGER.debug("Success: Successful DB request");
        handler.handle(Future.succeededFuture(countRes.result()));
      } else {
        LOGGER.error("Fail: DB Request;" + countRes.cause().getMessage());
        handler.handle(Future.failedFuture(countRes.cause().getMessage()));
      }
    });
    return this;
  }
}
