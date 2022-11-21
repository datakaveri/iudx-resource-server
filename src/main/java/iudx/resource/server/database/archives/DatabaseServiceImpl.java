package iudx.resource.server.database.archives;


import static iudx.resource.server.database.archives.Constants.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.handlers.FailureHandler;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.database.elastic.ElasticClient;
import iudx.resource.server.database.elastic.QueryDecoder;
import iudx.resource.server.database.elastic.exception.ESQueryException;

/**
 * The Database Service Implementation.
 * <h1>Database Service Implementation</h1>
 * <p>
 * The Database Service implementation in the IUDX Resource Server implements the definitions of the
 * {@link iudx.resource.server.database.archives.DatabaseService}.
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
  private String timeLimit;

  public DatabaseServiceImpl(ElasticClient client, String timeLimit) {
    this.client = client;
    this.timeLimit = timeLimit;
  }

  /**
   * Performs a ElasticSearch search query using the low level REST client.
   * 
   * @param request Json object received from the ApiServerVerticle
   * @param handler Handler to return database response in case of success and appropriate error
   *        message in case of failure
   */
  // @Override
  // public DatabaseService searchQuery(JsonObject request, Handler<AsyncResult<JsonObject>>
  // handler) {
  //
  // LOGGER.trace("Info: searchQuery;" + request.toString());
  //
  // request.put(SEARCH_KEY, true);
  // request.put(TIME_LIMIT, timeLimit);
  // request.put("isTest", true);
  //
  // if (!request.containsKey(ID)) {
  // LOGGER.debug("Info: " + ID_NOT_FOUND);
  // responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(ID_NOT_FOUND);
  // handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
  // return null;
  // }
  //
  // if (request.getJsonArray(ID).isEmpty()) {
  // LOGGER.debug("Info: " + EMPTY_RESOURCE_ID);
  // responseBuilder =
  // new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(EMPTY_RESOURCE_ID);
  // handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
  // return null;
  // }
  //
  // if (!request.containsKey(SEARCH_TYPE)) {
  // LOGGER.debug("Info: " + SEARCHTYPE_NOT_FOUND);
  // responseBuilder =
  // new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(SEARCHTYPE_NOT_FOUND);
  // handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
  // return null;
  // }
  //
  // if (request.getJsonArray(ID).getString(0).split("/").length != 5) {
  // LOGGER.error("Malformed ID: " + request.getJsonArray(ID).getString(0));
  // responseBuilder = new ResponseBuilder(FAILED)
  // .setTypeAndTitle(400)
  // .setMessage(MALFORMED_ID + request.getJsonArray(ID));
  // handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
  // return null;
  // }
  //
  // List<String> splitId =
  // new LinkedList<>(Arrays.asList(request.getJsonArray(ID).getString(0).split("/")));
  // splitId.remove(splitId.size() - 1);
  // final String searchIndex = String.join("__", splitId).concat(SEARCH_REQ_PARAM);
  // LOGGER.debug("Index name: " + searchIndex);
  //
  // try {
  // query = new QueryDecoder().getESquery(request);
  // } catch (Exception e) {
  // LOGGER.error("query decode error", e);
  // responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(e.getMessage());
  // handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
  // return null;
  // }
  //
  // if (query.containsKey(ERROR)) {
  // LOGGER.error("Fail: Query returned with an error: " + query.getString(ERROR));
  // responseBuilder =
  // new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(query.getString(ERROR));
  // handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
  // return null;
  // }
  //
  // LOGGER.debug("Info: Query constructed: " + query.toString());
  // if (LATEST_SEARCH.equalsIgnoreCase(request.getString(SEARCH_TYPE))) {
  // client
  // .searchAsync(LATEST_RESOURCE_INDEX, FILTER_PATH_VAL_LATEST, query.toString(),
  // searchRes -> {
  // if (searchRes.succeeded()) {
  // LOGGER.debug("Success: Successful DB request");
  // handler.handle(Future.succeededFuture(searchRes.result()));
  // } else {
  // LOGGER.error("Fail: DB Request;" + searchRes.cause().getMessage());
  // handler.handle(Future.failedFuture(searchRes.cause().getMessage()));
  // }
  // });
  // } else {
  // String countIndex = String.join("__", splitId);
  // countIndex = countIndex.concat(COUNT_REQ_PARAM);
  // JsonObject countQuery = query.copy();
  // countQuery.remove(SOURCE_FILTER_KEY);
  // client.countAsync(countIndex, countQuery.toString(), countHandler -> {
  // if (countHandler.succeeded()) {
  // query.put(SIZE_KEY, getOrDefault(request, PARAM_SIZE, DEFAULT_SIZE_VALUE));
  // query.put(FROM_KEY, getOrDefault(request, PARAM_FROM, DEFAULT_FROM_VALUE));
  // JsonObject countJson = countHandler.result();
  // LOGGER.debug("count json : " + countJson);
  // int count = countJson.getJsonArray("results").getJsonObject(0).getInteger("totalHits");
  // if (count > 50000) {
  // JsonObject json = new JsonObject();
  // json.put("type", 413);
  // json.put("title", ResponseUrn.PAYLOAD_TOO_LARGE_URN.getUrn());
  // json.put("details", ResponseUrn.PAYLOAD_TOO_LARGE_URN.getMessage());
  // handler.handle(Future.failedFuture(json.toString()));
  // return;
  // }
  // client.searchAsync(searchIndex, FILTER_PATH_VAL, query.toString(), searchRes -> {
  // if (searchRes.succeeded()) {
  // LOGGER.debug("Success: Successful DB request");
  // handler
  // .handle(Future
  // .succeededFuture(searchRes
  // .result()
  // .put(PARAM_SIZE, query.getInteger(SIZE_KEY))
  // .put(PARAM_FROM, query.getInteger(FROM_KEY))
  // .put("totalHits", count)));
  // } else {
  // LOGGER.error("Fail: DB Request;" + searchRes.cause().getMessage());
  // handler.handle(Future.failedFuture(searchRes.cause().getMessage()));
  // }
  // });
  // } else {
  // LOGGER.error("Fail: DB Request;" + countHandler.cause().getMessage());
  // handler.handle(Future.failedFuture(countHandler.cause().getMessage()));
  // }
  // });
  //
  // }
  // return this;
  // }
  //
  // /**
  // * Performs a ElasticSearch count query using the low level REST client.
  // *
  // * @param request Json object received from the ApiServerVerticle
  // * @param handler Handler to return database response in case of success and appropriate error
  // * message in case of failure
  // */
  // @Override
  // public DatabaseService countQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler)
  // {
  //
  // LOGGER.trace("Info: countQuery;" + request.toString());
  //
  // request.put(SEARCH_KEY, false);
  // request.put(TIME_LIMIT, timeLimit);
  //
  //
  // if (!request.containsKey(ID)) {
  // LOGGER.debug("Info: " + ID_NOT_FOUND);
  // responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(ID_NOT_FOUND);
  // handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
  // return null;
  // }
  //
  // if (request.getJsonArray(ID).isEmpty()) {
  // LOGGER.debug("Info: " + EMPTY_RESOURCE_ID);
  // responseBuilder =
  // new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(EMPTY_RESOURCE_ID);
  // handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
  // return null;
  // }
  //
  // if (!request.containsKey(SEARCH_TYPE)) {
  // LOGGER.debug("Info: " + SEARCHTYPE_NOT_FOUND);
  // responseBuilder =
  // new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(SEARCHTYPE_NOT_FOUND);
  // handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
  // return null;
  // }
  //
  // if (request.getJsonArray(ID).getString(0).split("/").length != 5) {
  // LOGGER.error("Malformed ID: " + request.getJsonArray(ID).getString(0));
  // responseBuilder = new ResponseBuilder(FAILED)
  // .setTypeAndTitle(400)
  // .setMessage(MALFORMED_ID + request.getJsonArray(ID));
  // handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
  // return null;
  // }
  //
  // List<String> splitId =
  // new LinkedList<>(Arrays.asList(request.getJsonArray(ID).getString(0).split("/")));
  // splitId.remove(splitId.size() - 1);
  // String index = String.join("__", splitId);
  // index = index.concat(COUNT_REQ_PARAM);
  // LOGGER.debug("Index name: " + index);
  //
  // try {
  // query = new QueryDecoder().getESquery(request);
  // } catch (Exception e) {
  // responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(e.getMessage());
  // handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
  // return null;
  // }
  //
  // if (query.containsKey(ERROR)) {
  // LOGGER.error("Fail: Query returned with an error: " + query.getString(ERROR));
  // responseBuilder =
  // new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(query.getString(ERROR));
  // handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
  // return null;
  // }
  //
  // LOGGER.debug("Info: Query constructed: " + query.toString());
  //
  // client.countAsync(index, query.toString(), countRes -> {
  // if (countRes.succeeded()) {
  // LOGGER.debug("Success: Successful DB request");
  // handler.handle(Future.succeededFuture(countRes.result()));
  // } else {
  // LOGGER.error("Fail: DB Request;" + countRes.cause().getMessage());
  // handler.handle(Future.failedFuture(countRes.cause().getMessage()));
  // }
  // });
  // return this;
  // }

  public int getOrDefault(JsonObject json, String key, int def) {
    if (json.containsKey(key)) {
      int value = Integer.parseInt(json.getString(key));
      return value;
    }
    return def;
  }

  private class CountResultPlaceholder {
    private long count;

    public CountResultPlaceholder() {
      this.count = 0L;
    }

    public long getCount() {
      return this.count;
    }

    public void setCount(long count) {
      this.count = count;
    }
  }

  @Override
  public Future<JsonObject> search(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();

    request.put(TIME_LIMIT, timeLimit);
    try {
      Future<JsonObject> validationFuture = checkQuery(request);

      validationFuture.onFailure(handler -> {
        promise.fail(handler.toString());
      }).onSuccess(handler -> {
        String id = request.getJsonArray(ID).getString(0);
        final String searchIndex = getIndex(id);

        final int sizeKeyValue = getOrDefault(request, PARAM_SIZE, DEFAULT_SIZE_VALUE);
        final int fromKeyValue = getOrDefault(request, PARAM_FROM, DEFAULT_FROM_VALUE);


        Query query = queryDecoder.getQuery(request);
        LOGGER.info("query : " + query.toString());

        CountResultPlaceholder countPlaceHolder = new CountResultPlaceholder();
        Future<JsonObject> countFuture = client.asyncCount(searchIndex, query);

        countFuture.compose(countQueryHandler -> {
          long count =
              countQueryHandler.getJsonArray("results").getJsonObject(0).getInteger("totalHits");
          LOGGER.info("count : " + count);
          if (count > 50000) {
            JsonObject json = new JsonObject();
            json.put("type", 413);
            json.put("title", ResponseUrn.PAYLOAD_TOO_LARGE_URN.getUrn());
            json.put("details", ResponseUrn.PAYLOAD_TOO_LARGE_URN.getMessage());
            return Future.failedFuture("Result Limit exceeds");
          }
          countPlaceHolder.setCount(count);
          SourceConfig sourceFilter = queryDecoder.getSourceConfigFilters(request);
          return client.asyncSearch(searchIndex, query, sizeKeyValue, fromKeyValue, sourceFilter);
        }).onSuccess(successHandler -> {
          LOGGER.debug("Success: Successful DB request");
          JsonObject responseJson = successHandler;
          responseJson
              .put(PARAM_SIZE, sizeKeyValue)
                .put(PARAM_FROM, fromKeyValue)
                .put("totalHits", countPlaceHolder.getCount());
          promise.complete(responseJson);
        }).onFailure(failureHandler -> {
          LOGGER.info("failed to query : " + failureHandler);
          promise.fail(failureHandler.getMessage());
        });
      });
      // TODO : we can use ServiceException here, check for feasibility
    } catch (ESQueryException ex) {
      ResponseUrn exception_urn = ResponseUrn.BAD_REQUEST_URN;
      promise.fail(new ESQueryException(exception_urn, ex.getMessage()).toString());
    } catch (Exception ex) {
      promise.fail(new ESQueryException("Exception occured executing query").toString());
    }
    return promise.future();
  }

  private String getIndex(String id) {
    List<String> splitId = new LinkedList<>(Arrays.asList(id.split("/")));
    splitId.remove(splitId.size() - 1);
    final String searchIndex = String.join("__", splitId);
    LOGGER.debug("Index name: " + searchIndex);
    return searchIndex;
  }

  @Override
  public Future<JsonObject> count(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();

    request.put(TIME_LIMIT, timeLimit);
    try {
      Future<JsonObject> validationFuture = checkQuery(request);
      validationFuture.onFailure(handler -> {
        promise.fail(handler.toString());
      }).onSuccess(handler -> {

        String searchType = request.getString(SEARCH_TYPE);
        if (searchType.matches(RESPONSE_FILTER_REGEX)) {
          throw new ESQueryException("Count is not supported with filtering");
        }

        String id = request.getJsonArray(ID).getString(0);
        final String searchIndex = getIndex(id);

        Query query = queryDecoder.getQuery(request);
        LOGGER.info("query : " + query.toString());
        Future<JsonObject> countFuture = client.asyncCount(searchIndex, query);
        countFuture.onSuccess(success -> {
          promise.complete(success);
        }).onFailure(failure -> {
          promise.fail(failure.getMessage());
        });
      });
    } catch (ESQueryException ex) {
      ResponseUrn exception_urn = ResponseUrn.BAD_REQUEST_URN;
      promise.fail(new ESQueryException(exception_urn, ex.getMessage()).toString());
    } catch (Exception ex) {
      promise.fail(new ESQueryException("Exception occured executing query").toString());
    }
    return promise.future();
  }



  public Future<JsonObject> checkQuery(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    if (!request.containsKey(ID)) {
      LOGGER.debug("Info: " + ID_NOT_FOUND);
      promise.fail(new ESQueryException(ResponseUrn.BAD_REQUEST_URN, ID_NOT_FOUND));
    } else if (request.getJsonArray(ID).isEmpty()) {
      LOGGER.debug("Info: " + EMPTY_RESOURCE_ID);
      promise.fail(new ESQueryException(ResponseUrn.BAD_REQUEST_URN, EMPTY_RESOURCE_ID));
    } else if (!request.containsKey(SEARCH_TYPE)) {
      LOGGER.debug("Info: " + SEARCHTYPE_NOT_FOUND);
      promise.fail(new ESQueryException(ResponseUrn.BAD_REQUEST_URN, SEARCHTYPE_NOT_FOUND));
    } else if (request.getJsonArray(ID).getString(0).split("/").length != 5) {
      LOGGER.error("Malformed ID: " + request.getJsonArray(ID).getString(0));
      promise.fail(new ESQueryException(ResponseUrn.BAD_REQUEST_URN, MALFORMED_ID));
    } else {
      promise.complete();
    }
    return promise.future();
  }
}
