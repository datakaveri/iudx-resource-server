package iudx.resource.server.database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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
  public DatabaseService searchQuery(JsonObject request, Handler<AsyncResult<JsonArray>> handler) {

    LOGGER.debug("Info: searchQuery;" + request.toString());

    request.put(SEARCH_KEY, true);
    // TODO : only for testing comment after testing.
    request.put("isTest", true);

    if (!request.containsKey(ID)) {
      LOGGER.debug("Info: " + ID_NOT_FOUND);
      handler.handle(Future.failedFuture(ID_NOT_FOUND));
      return null;
    }

    if (request.getJsonArray(ID).isEmpty()) {
      LOGGER.debug("Info: " + EMPTY_RESOURCE_ID);
      handler.handle(Future.failedFuture(EMPTY_RESOURCE_ID));
      return null;
    }

    if (!request.containsKey(SEARCH_TYPE)) {
      LOGGER.debug("Info: " + SEARCHTYPE_NOT_FOUND);
      handler.handle(Future.failedFuture(SEARCHTYPE_NOT_FOUND));
      return null;
    }

    // TODO: Need to automate the Index flow using the instanceID field.
    // Need to populate a HashMap containing the instanceID and the indexName
    // We need to discuss if we need to have a single index or an index per group to
    // avoid any dependency
    // String resourceGroup = "";
    // request.getJsonArray("id").getString(0).split("/")[3];

    String resourceServer = request.getJsonArray(ID).getString(0).split("/")[0];
    LOGGER.debug("Info: Resource Server instanceID is " + resourceServer);

    query = queryDecoder.queryDecoder(request);
    if (query.containsKey(ERROR)) {
      LOGGER.error("Fail: Query returned with an error: " + query.getString(ERROR));
      handler.handle(Future.failedFuture(query.getString(ERROR)));
      return null;
    }

    LOGGER.info("Info: Query constructed: " + query.toString());

    client.searchAsync(VARANASI_TEST_SEARCH_INDEX, query.toString(), searchRes -> {
      if (searchRes.succeeded()) {
        LOGGER.debug("Success: Successful DB request");
        handler.handle(Future.succeededFuture(searchRes.result()));
      } else {
        LOGGER.error("Fail: DB Request;" + searchRes.cause().getMessage());
        handler.handle(Future.failedFuture(searchRes.cause().getMessage()));
      }
    });
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
      handler.handle(Future.failedFuture(ID_NOT_FOUND));
      return null;
    }

    if (request.getJsonArray(ID).isEmpty()) {
      LOGGER.debug("Info: " + EMPTY_RESOURCE_ID);
      handler.handle(Future.failedFuture(EMPTY_RESOURCE_ID));
      return null;
    }

    if (!request.containsKey(SEARCH_TYPE)) {
      LOGGER.debug("Info: " + SEARCHTYPE_NOT_FOUND);
      handler.handle(Future.failedFuture(SEARCHTYPE_NOT_FOUND));
      return null;
    }

    // String resourceGroup = "";
    // request.getJsonArray("id").getString(0).split("/")[3];

    query = queryDecoder.queryDecoder(request);
    if (query.containsKey(ERROR)) {
      LOGGER.error("Fail: Query returned with an error: " + query.getString(ERROR));
      handler.handle(Future.failedFuture(query.getString(ERROR)));
      return null;
    }

    LOGGER.debug("Info: Query constructed: " + query.toString());

    client.countAsync(VARANASI_TEST_COUNT_INDEX, query.toString(), countRes -> {
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
