package iudx.resource.server.database.archives;

import static iudx.resource.server.database.archives.Constants.*;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.database.elastic.ElasticClient;
import iudx.resource.server.database.elastic.QueryDecoder;
import iudx.resource.server.database.elastic.exception.EsQueryException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Database Service Implementation.
 *
 * <h1>Database Service Implementation</h1>
 *
 * <p>The Database Service implementation in the IUDX Resource Server implements the definitions of
 * the {@link iudx.resource.server.database.archives.DatabaseService}.
 *
 * @version 1.0
 * @since 2020-05-31
 */
public class DatabaseServiceImpl implements DatabaseService {

  private static final Logger LOGGER = LogManager.getLogger(DatabaseServiceImpl.class);
  private final ElasticClient client;
  private QueryDecoder queryDecoder = new QueryDecoder();
  private String timeLimit;
  private String tenantPrefix;

  public DatabaseServiceImpl(ElasticClient client, String timeLimit, String tenantPrefix) {
    this.client = client;
    this.timeLimit = timeLimit;
    this.tenantPrefix = tenantPrefix;
  }

  public int getOrDefault(JsonObject json, String key, int def) {
    if (json.containsKey(key)) {
      int value = Integer.parseInt(json.getString(key));
      return value;
    }
    return def;
  }

  @Override
  public Future<JsonObject> search(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();

    request.put(TIME_LIMIT, timeLimit);
    try {
      Future<JsonObject> validationFuture = checkQuery(request);

      validationFuture
          .onFailure(
              handler -> {
                promise.fail(handler.toString());
              })
          .onSuccess(
              handler -> {
                String id = request.getJsonArray(ID).getString(0);
                final String searchIndex = getIndex(id);

                final int sizeKeyValue = getOrDefault(request, PARAM_SIZE, DEFAULT_SIZE_VALUE);
                final int fromKeyValue = getOrDefault(request, PARAM_FROM, DEFAULT_FROM_VALUE);

                Query query = queryDecoder.getQuery(request);
                LOGGER.info("query : " + query.toString());

                CountResultPlaceholder countPlaceHolder = new CountResultPlaceholder();
                Future<JsonObject> countFuture = client.asyncCount(searchIndex, query);

                countFuture
                    .compose(
                        countQueryHandler -> {
                          long count =
                              countQueryHandler
                                  .getJsonArray("results")
                                  .getJsonObject(0)
                                  .getInteger("totalHits");
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
                          return client.asyncSearch(
                              searchIndex, query, sizeKeyValue, fromKeyValue, sourceFilter);
                        })
                    .onSuccess(
                        successHandler -> {
                          LOGGER.debug("Success: Successful DB request");
                          JsonObject responseJson = successHandler;
                          responseJson
                              .put(PARAM_SIZE, sizeKeyValue)
                              .put(PARAM_FROM, fromKeyValue)
                              .put("totalHits", countPlaceHolder.getCount());
                          promise.complete(responseJson);
                        })
                    .onFailure(
                        failureHandler -> {
                          LOGGER.info("failed to query : " + failureHandler);
                          promise.fail(failureHandler.getMessage());
                        });
              });
      // TODO : we can use ServiceException here, check for feasibility
    } catch (EsQueryException ex) {
      ResponseUrn exceptionUrn = ResponseUrn.BAD_REQUEST_URN;
      promise.fail(new EsQueryException(exceptionUrn, ex.getMessage()).toString());
    } catch (Exception ex) {
      promise.fail(new EsQueryException("Exception occured executing query").toString());
    }
    return promise.future();
  }

  private String getIndex(String id) {
    List<String> splitId = new LinkedList<>(Arrays.asList(id.split("/")));
    splitId.remove(splitId.size() - 1);

    if (!this.tenantPrefix.equals("none")) {
      splitId.add(0, this.tenantPrefix);
    }
    /*
     * Example: searchIndex =
     * iudx__datakaveri.org__b8bd3e3f39615c8ec96722131ae95056b5938f2f__rs.iudx.io__pune-env-aqm
     */
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
      validationFuture
          .onFailure(
              handler -> {
                promise.fail(handler.toString());
              })
          .onSuccess(
              handler -> {
                String searchType = request.getString(SEARCH_TYPE);
                if (searchType.matches(RESPONSE_FILTER_REGEX)) {
                  throw new EsQueryException("Count is not supported with filtering");
                }

                String id = request.getJsonArray(ID).getString(0);
                final String searchIndex = getIndex(id);

                Query query = queryDecoder.getQuery(request);
                LOGGER.info("query : " + query.toString());
                Future<JsonObject> countFuture = client.asyncCount(searchIndex, query);
                countFuture
                    .onSuccess(
                        success -> {
                          promise.complete(success);
                        })
                    .onFailure(
                        failure -> {
                          promise.fail(failure.getMessage());
                        });
              });
    } catch (EsQueryException ex) {
      ResponseUrn exceptionUrn = ResponseUrn.BAD_REQUEST_URN;
      promise.fail(new EsQueryException(exceptionUrn, ex.getMessage()).toString());
    } catch (Exception ex) {
      promise.fail(new EsQueryException("Exception occured executing query").toString());
    }
    return promise.future();
  }

  public Future<JsonObject> checkQuery(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    if (!request.containsKey(ID)) {
      LOGGER.debug("Info: " + ID_NOT_FOUND);
      promise.fail(new EsQueryException(ResponseUrn.BAD_REQUEST_URN, ID_NOT_FOUND));
    } else if (request.getJsonArray(ID).isEmpty()) {
      LOGGER.debug("Info: " + EMPTY_RESOURCE_ID);
      promise.fail(new EsQueryException(ResponseUrn.BAD_REQUEST_URN, EMPTY_RESOURCE_ID));
    } else if (!request.containsKey(SEARCH_TYPE)) {
      LOGGER.debug("Info: " + SEARCHTYPE_NOT_FOUND);
      promise.fail(new EsQueryException(ResponseUrn.BAD_REQUEST_URN, SEARCHTYPE_NOT_FOUND));
    } else if (request.getJsonArray(ID).getString(0).split("/").length != 5) {
      LOGGER.error("Malformed ID: " + request.getJsonArray(ID).getString(0));
      promise.fail(new EsQueryException(ResponseUrn.BAD_REQUEST_URN, MALFORMED_ID));
    } else {
      promise.complete();
    }
    return promise.future();
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
}
