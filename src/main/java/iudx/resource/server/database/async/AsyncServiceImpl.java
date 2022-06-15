package iudx.resource.server.database.async;

import static iudx.resource.server.database.archives.Constants.RESPONSE_ATTRS;
import static iudx.resource.server.database.async.util.Constants.FILE_DOWNLOAD_URL;
import static iudx.resource.server.database.async.util.Constants.OBJECT_ID;
import static iudx.resource.server.database.async.util.Constants.S3_URL;
import static iudx.resource.server.database.async.util.Constants.STATUS;
import static iudx.resource.server.database.async.util.Constants.USER_ID;
import static iudx.resource.server.database.postgres.Constants.INSERT_S3_READY_SQL;
import static iudx.resource.server.database.postgres.Constants.SELECT_S3_SEARCH_SQL;
import static iudx.resource.server.database.postgres.Constants.SELECT_S3_STATUS_SQL;
import static iudx.resource.server.database.postgres.Constants.UPDATE_S3_URL_SQL;
import static iudx.resource.server.database.postgres.Constants.UPDATE_STATUS_SQL;
import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.index.query.QueryBuilder;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.database.archives.ResponseBuilder;
import iudx.resource.server.database.async.util.QueryProgress;
import iudx.resource.server.database.async.util.S3FileOpsHelper;
import iudx.resource.server.database.async.util.Util;
import iudx.resource.server.database.elastic.ElasticClient;
import iudx.resource.server.database.elastic.QueryDecoder;
import iudx.resource.server.database.postgres.PostgresService;

public class AsyncServiceImpl implements AsyncService {

  private static final Logger LOGGER = LogManager.getLogger(AsyncServiceImpl.class);

  private final ElasticClient client;
  private ResponseBuilder responseBuilder;
  private String timeLimit;
  private String filePath;
  private final PostgresService pgService;
  private final S3FileOpsHelper s3FileOpsHelper;
  private final Util util;
  private final Vertx vertx;

  public AsyncServiceImpl(
          Vertx vertx,
          ElasticClient client,
          PostgresService pgService,
          S3FileOpsHelper s3FileOpsHelper,
          String timeLimit,
          String filePath) {
    this.vertx = vertx;
    this.client = client;
    this.pgService = pgService;
    this.s3FileOpsHelper = s3FileOpsHelper;
    this.timeLimit = timeLimit;
    this.filePath = filePath;
    this.util = new Util(pgService);
  }

  @Override
  public AsyncService asyncStatus(
          String sub, String searchID, Handler<AsyncResult<JsonObject>> handler) {
    StringBuilder query = new StringBuilder(SELECT_S3_STATUS_SQL.replace("$1", searchID));

    pgService.executeQuery(
            query.toString(),
            pgHandler -> {
              if (pgHandler.succeeded()) {
                JsonArray results = pgHandler.result().getJsonArray("result");
                if (results.isEmpty()) {
                  responseBuilder =
                          new ResponseBuilder("failed")
                                  .setTypeAndTitle(400)
                                  .setMessage("Fail: Incorrect search ID");
                  handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
                } else {
                  JsonObject answer = results.getJsonObject(0);

                  String user_id = answer.getString("user_id");
                  if (sub.equals(user_id)) {
                    String status = answer.getString(STATUS);
                    if (status.equalsIgnoreCase(QueryProgress.COMPLETE.toString())) {
                      answer.put(FILE_DOWNLOAD_URL, answer.getValue(S3_URL));
                    }
                    answer.put("searchId", answer.getString("search_id"));
                    answer.put("userId", user_id);

                    answer.remove(S3_URL);
                    answer.remove("search_id");
                    answer.remove(USER_ID);
                    LOGGER.debug(answer.encodePrettily());
                    JsonObject response =
                            new JsonObject()
                                    .put("type", ResponseUrn.SUCCESS_URN.getUrn())
                                    .put("title", ResponseUrn.SUCCESS_URN.getMessage())
                                    .put("results", new JsonArray().add(answer));
                    handler.handle(Future.succeededFuture(response));
                  } else {
                    responseBuilder =
                            new ResponseBuilder("failed")
                                    .setTypeAndTitle(400, ResponseUrn.BAD_REQUEST_URN.getUrn())
                                    .setMessage(
                                            "Please use same user token to check status as used while calling search API");
                    handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
                  }
                }
              }
            });
    return this;
  }

  @Override
  public AsyncService asyncSearch(String requestId, String sub, String searchId, JsonObject query) {
    // check DB whether requestId exist in DB
    // If yes, create a new Row with searchId with same requestId, new S3Url from objectId and
    // status COMPLETE
    // If No, create new row in DB with status as pending and start scroll request.
    getRecord4RequestId(requestId)
            .onSuccess(
                    handler -> {
                      process4ExistingRequestId(requestId, sub, searchId, handler);
                    })
            .onFailure(
                    handler -> {
                      util.writeToDB(searchId, requestId, sub)
                              .onSuccess(
                                      successHandler -> {
                                        process4NewRequestId(searchId, query);
                                      })
                              .onFailure(
                                      errorHandler -> {
                                        LOGGER.error(errorHandler);
                                      });;
                    });

    return this;
  }

  /**
   * This method will fetch results from database for a provided requestId, and status="COMPLETE".
   * This method returns a failed future if no record exist, else it will return a successful
   * Future<JsonArray> object with values for the requestId.
   *
   * @param requestId
   * @return
   */
  Future<JsonArray> getRecord4RequestId(String requestId) {
    Promise<JsonArray> promise = Promise.promise();

    Map<String, String> result = new HashMap<>();
    StringBuilder query =
            new StringBuilder(
                    SELECT_S3_SEARCH_SQL
                            .replace("$1", requestId)
                            .replace("$2", QueryProgress.COMPLETE.toString()));

    pgService.executeQuery(
            query.toString(),
            pgHandler -> {
              if (pgHandler.succeeded()) {
                JsonArray results = pgHandler.result().getJsonArray("result");
                if (results.isEmpty()) {
                  promise.fail("Record doesn't exist in db for requestId.");
                } else {
                  LOGGER.debug("record : " + results);
                  promise.complete(results);
                }
              }
            });

    return promise.future();
  }

  Future<Void> executePGQuery(String query) {
    Promise<Void> promise = Promise.promise();

    pgService.executeQuery(
            query,
            handler -> {
              if (handler.succeeded()) {
                promise.complete();
              } else {
                promise.fail("failed query execution " + handler.cause());
              }
            });

    return promise.future();
  }

  void process4ExistingRequestId(String requestId, String sub, String searchId, JsonArray record) {
    String object_id = record.getJsonObject(0).getString(OBJECT_ID);
    String expiry = LocalDateTime.now().plusDays(1).toString();
    String newS3_url = generateNewURL(object_id);

    StringBuilder queryBuilder =
            new StringBuilder(
                    INSERT_S3_READY_SQL
                            .replace("$1", UUID.randomUUID().toString())
                            .replace("$2", searchId)
                            .replace("$3", requestId)
                            .replace("$4", QueryProgress.COMPLETE.toString())
                            .replace("$5", newS3_url)
                            .replace("$6", expiry)
                            .replace("$7", sub)
                            .replace("$8", object_id)
                            .replace("$9", String.valueOf(1.0)));

    executePGQuery(queryBuilder.toString())
            .onSuccess(
                    handler -> {
                      LOGGER.info("Query completed with existing requestId & objectId");
                    })
            .onFailure(
                    handler -> {
                      LOGGER.error("Query execution failed for insert with existing requestId & objectId");
                    });
  }

  private void process4NewRequestId(String searchId, JsonObject query) {
    File file = new File(filePath + "/" + searchId + ".json");
    String objectId = UUID.randomUUID().toString();


    ProgressListener progressListener = new AsyncFileScrollProgressListener(searchId, pgService);
    scrollQuery(
            file,
            query,
            searchId,
            progressListener,
            scrollHandler -> {
              if (scrollHandler.succeeded()) {
                s3FileOpsHelper.s3Upload(
                        file,
                        objectId,
                        s3UploadHandler -> {
                          if (s3UploadHandler.succeeded()) {
                            String s3_url = generateNewURL(objectId);
                            String expiry = LocalDateTime.now().plusDays(1).toString();
                            // update DB for search ID and requestId;
                            progressListener.finish();
                            StringBuilder updateQuery =
                                    new StringBuilder(
                                            UPDATE_S3_URL_SQL
                                                    .replace("$1", s3_url)
                                                    .replace("$2", expiry)
                                                    .replace("$3", QueryProgress.COMPLETE.toString())
                                                    .replace("$4", objectId)
                                                    .replace("$5", String.valueOf(100.0))
                                                    .replace("$6", searchId));
                            executePGQuery(updateQuery.toString())
                                    .onSuccess(
                                            recordUpdateHandler -> {
                                              LOGGER.debug("updated status in postgres");
                                              try {
                                                vertx.fileSystem().deleteBlocking(filePath + "/" + file.getName());

                                              } catch (Exception ex) {
                                                LOGGER.error(
                                                        "File deletion operation failed for fileName : "
                                                                + file.getName()
                                                                + " try to delete manually to reclaim disk-space");
                                              }
                                            })
                                    .onFailure(
                                            recordInsertFailure -> {
                                              LOGGER.error(
                                                      "Postgres insert failure [COMPLETE status]"
                                                              + recordInsertFailure);
                                            });

                          } else {
                            LOGGER.error("File upload to S3 failed for fileName : " + file.getName());
                            StringBuilder updateFailQuery =
                                    new StringBuilder(
                                            UPDATE_STATUS_SQL
                                                    .replace("$1", QueryProgress.ERROR.toString())
                                                    .replace("$2", searchId));
                            Future.future(fu -> util.writeToDB(updateFailQuery));
                          }
                        });
              } else {
                LOGGER.error("Scroll API operation failed for searchId : " + searchId);
                StringBuilder updateFailQuery =
                        new StringBuilder(
                                UPDATE_STATUS_SQL
                                        .replace("$1", QueryProgress.ERROR.toString())
                                        .replace("$2", searchId));
                Future.future(fu -> util.writeToDB(updateFailQuery));
              }
            });
  }

  private String generateNewURL(String object_id) {

    long expiry = ZonedDateTime.now().toEpochSecond() * 1000 + TimeUnit.DAYS.toMillis(1);
    URL s3_url = s3FileOpsHelper.generatePreSignedUrl(expiry, object_id);
    return s3_url.toString();
  }

  public AsyncService scrollQuery(
          File file, JsonObject request, String searchId, ProgressListener progressListener,
          Handler<AsyncResult<JsonObject>> handler) {
    QueryBuilder query;

    request.put("search", true);

    if (!util.isValidQuery(request)) {
      responseBuilder =
              new ResponseBuilder("fail").setTypeAndTitle(400).setMessage("bad parameters");
      handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
      return this;
    }

    List<String> splitId =
            new LinkedList<>(Arrays.asList(request.getJsonArray("id").getString(0).split("/")));
    splitId.remove(splitId.size() - 1);
    final String searchIndex = String.join("__", splitId);
    LOGGER.debug("Index name: " + searchIndex);

    try {
      query = new QueryDecoder().getESquery4Scroll(request);
    } catch (Exception e) {
      LOGGER.error(e);
      e.printStackTrace();
      responseBuilder = new ResponseBuilder("fail").setTypeAndTitle(400).setMessage(e.getMessage());
      handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
      return this;
    }

    LOGGER.debug("Info: index: " + searchIndex);
    LOGGER.debug("Info: Query constructed: " + query.toString());

    String[] sourceFilters = null;
    if (request.containsKey(RESPONSE_ATTRS)) {
      JsonArray responseFilters = request.getJsonArray(RESPONSE_ATTRS);
      sourceFilters = new String[responseFilters.size()];
      for (int i = 0; i < sourceFilters.length; i++) {
        sourceFilters[i] = responseFilters.getString(i);
        LOGGER.debug(sourceFilters[i]);
      }
    }

    client.scrollAsync(
            file,
            searchIndex,
            query,
            sourceFilters,
            searchId,
            progressListener,
            scrollHandler -> {
              if (scrollHandler.succeeded()) {
                handler.handle(Future.succeededFuture());
              } else {
                handler.handle(Future.failedFuture(scrollHandler.cause()));
              }
            });
    return this;
  }
}
