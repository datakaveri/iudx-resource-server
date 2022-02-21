package iudx.resource.server.async;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.async.util.Utilities;
import iudx.resource.server.database.archives.ResponseBuilder;
import iudx.resource.server.database.archives.elastic.ElasticClient;
import iudx.resource.server.database.archives.elastic.QueryDecoder;
import iudx.resource.server.database.postgres.PostgresService;
import iudx.resource.server.database.archives.S3FileOpsHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.index.query.QueryBuilder;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static iudx.resource.server.database.archives.Constants.SUCCESS;
import static iudx.resource.server.database.archives.Constants.SEARCH_KEY;
import static iudx.resource.server.database.archives.Constants.TIME_LIMIT;
import static iudx.resource.server.database.archives.Constants.ID;
import static iudx.resource.server.database.archives.Constants.ID_NOT_FOUND;
import static iudx.resource.server.database.archives.Constants.FAILED;
import static iudx.resource.server.database.archives.Constants.EMPTY_RESOURCE_ID;
import static iudx.resource.server.database.archives.Constants.SEARCH_TYPE;
import static iudx.resource.server.database.archives.Constants.SEARCHTYPE_NOT_FOUND;
import static iudx.resource.server.database.archives.Constants.MALFORMED_ID;
import static iudx.resource.server.database.archives.Constants.ERROR;
import static iudx.resource.server.database.postgres.Constants.SELECT_S3_SEARCH_SQL;

/**
 * The Async Service Implementation.
 *
 * <h1>Async Service Implementation</h1>
 *
 * <p>The Async Service implementation in the IUDX Resource Server implements the definitions of the
 * {@link iudx.resource.server.async.AsyncService}.
 *
 * @version 1.0
 * @since 2022-02-08
 */
public class AsyncServiceImpl implements AsyncService {

  private static final Logger LOGGER = LogManager.getLogger(AsyncServiceImpl.class);
  private final ElasticClient client;
  private JsonObject query;
  private ResponseBuilder responseBuilder;
  private String timeLimit;
  private String filePath;
  private final PostgresService pgService;
  private final S3FileOpsHelper s3FileOpsHelper;
  private final Utilities utilities;

  public AsyncServiceImpl(
      ElasticClient client,
      PostgresService pgService,
      S3FileOpsHelper s3FileOpsHelper,
      String timeLimit,
      String filePath) {
    this.client = client;
    this.pgService = pgService;
    this.s3FileOpsHelper = s3FileOpsHelper;
    this.timeLimit = timeLimit;
    this.filePath = filePath;
    this.utilities = new Utilities(pgService);
  }

  /**
   * Performs a fetch from DB of the URL if the given requestID already exists otherwise calls the
   * ES scroll API flow
   *
   * @param requestID String received to identify incoming request
   * @param sub String received to identify user
   * @param scrollJson JsonObject received for scroll API flow
   * @param handler Handler to return URL in case of success and appropriate error message in case
   *     of failure
   */
  @Override
  public AsyncService asyncSearch(
      String requestID,
      String sub,
      JsonObject scrollJson,
      Handler<AsyncResult<JsonObject>> handler) {
    LOGGER.trace("Info: fetch URL started");

    LocalDateTime zdt = LocalDateTime.now();

    StringBuilder query = new StringBuilder(SELECT_S3_SEARCH_SQL.replace("$1", requestID));

    LOGGER.debug(query);
    pgService.executeQuery(
        query.toString(),
        pgHandler -> {
          if (pgHandler.succeeded()) {
            if (pgHandler.result().getJsonArray("result").isEmpty()) {

              String searchID = UUID.randomUUID().toString();
              JsonArray responseJson =
                  new JsonArray().add(new JsonObject().put("searchID", searchID));

              // respond with searchID for /async/status
              responseBuilder =
                  new ResponseBuilder(SUCCESS).setTypeAndTitle(201).setMessage(responseJson);
              handler.handle(Future.succeededFuture(responseBuilder.getResponse()));

              // if db result does not have a matching requestID, ONLY then ES scroll API is called
              scrollQuery(
                  scrollJson,
                  scrollHandler -> {
                    if (scrollHandler.succeeded()) {

                      // write pending status to DB
                      Future.future(future -> utilities.writeToDB(searchID, requestID, sub));

                      uploadScrollResultToS3(
                          scrollJson.getJsonArray(ID).getString(0),
                          uploadHandler -> {
                            if (uploadHandler.succeeded()) {
                              utilities.updateDBRecord(
                                  searchID,
                                  uploadHandler.result().getString("s3_url"),
                                  uploadHandler.result().getString("expiry"),
                                  uploadHandler.result().getString("object_id"));
                            } else {
                              handler.handle(Future.failedFuture(uploadHandler.cause()));
                            }
                          });
                    } else {
                      handler.handle(Future.failedFuture(scrollHandler.cause()));
                    }
                  });
            } else {
              JsonArray results = pgHandler.result().getJsonArray("result");
              String s3_url =
                  checkExpiryAndUserID(
                      results, zdt,
                      sub); // since request ID exists, get or generate the url based on user and/or
              // the expiry of the url
              JsonArray responseJson =
                  new JsonArray()
                      .add(
                          new JsonObject()
                              .put("file-download-url", s3_url != null ? s3_url : "dummy/url/123"));

              responseBuilder =
                  new ResponseBuilder(SUCCESS).setTypeAndTitle(200).setMessage(responseJson);
              handler.handle(Future.succeededFuture(responseBuilder.getResponse()));
            }
          }
        });
    return null;
  }

  private void uploadScrollResultToS3(String id, Handler<AsyncResult<JsonObject>> handler) {
    List<String> splitId = new LinkedList<>(Arrays.asList(id.split("/")));
    String objectKey = String.join("__", splitId);
    splitId.remove(splitId.size() - 1);
    final String searchIndex = String.join("__", splitId);
    String fileName = filePath.concat("response.json");
    File file = new File(fileName);

    s3FileOpsHelper.s3Upload(
        file,
        objectKey,
        uploadHandler -> {
          if (uploadHandler.succeeded()) {
            handler.handle(Future.succeededFuture(uploadHandler.result()));
          } else {
            handler.handle(Future.failedFuture(uploadHandler.cause()));
          }
        });
  }

  private String checkExpiryAndUserID(JsonArray results, LocalDateTime zdt, String sub) {

    for (Object json : results) {
      JsonObject result = (JsonObject) json;
      String searchID = result.getString("search_id");

      if (result.getString("s3_url") == null) {
        continue;
      }
      String s3_url = result.getString("s3_url");
      String userID = result.getString("user_id");
      LocalDateTime expiry = LocalDateTime.parse(result.getString("expiry"));
      String object_id = result.getString("object_id");

      if (userID.equalsIgnoreCase(sub) && zdt.isBefore(expiry)) {
        return s3_url;
      } else if (!userID.equalsIgnoreCase(sub) && zdt.isBefore(expiry)) {
        Future.future(future -> utilities.writeToDB(sub, result));
        return s3_url;
      } else if (userID.equalsIgnoreCase(sub) && !zdt.isBefore(expiry)) {
        String newS3_url = generateNewURL(object_id);
        // TODO: change expiry to long?
        LocalDateTime newExpiry = zdt.plusDays(1);
        Future.future(
            future ->
                utilities.updateDBRecord(searchID, newS3_url, newExpiry.toString(), object_id));
        return newS3_url;
      } else if (!userID.equalsIgnoreCase(sub) && !zdt.isBefore(expiry)) {
        String newS3_url = generateNewURL(object_id);
        // TODO: change expiry to long?
        String newExpiry = zdt.plusDays(1).toString();
        Future.future(future -> utilities.writeToDB(sub, newS3_url, newExpiry, result));
        return newS3_url;
      }
    }
    return null;
  }

  private String generateNewURL(String object_id) {

    long expiry = ZonedDateTime.now().toEpochSecond() * 1000 + TimeUnit.DAYS.toMillis(1);
    URL s3_url = s3FileOpsHelper.generatePreSignedUrl(expiry, object_id);
    return s3_url.toString();
  }

  /**
   * Performs a ElasticSearch scrolling search using the high level REST client.
   *
   * @param request JsonObject received from the AsyncRestApi
   * @param handler Handler to return database response in case of success and appropriate error
   *     message in case of failure
   */
  @Override
  public AsyncService scrollQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    LOGGER.trace("Info: scrollQuery; " + request.toString());
    request.put(SEARCH_KEY, true);
    request.put(TIME_LIMIT, "test,2020-10-22T00:00:00Z,10"); // TODO: what is time limit?
    request.put("isTest", true);
    if (!request.containsKey(ID)) {
      LOGGER.debug("Info: " + ID_NOT_FOUND);
      responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(ID_NOT_FOUND);
      handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
      return null;
    }

    if (request.getJsonArray(ID).isEmpty()) {
      LOGGER.debug("Info: " + EMPTY_RESOURCE_ID);
      responseBuilder =
          new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(EMPTY_RESOURCE_ID);
      handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
      return null;
    }

    if (!request.containsKey(SEARCH_TYPE)) {
      LOGGER.debug("Info: " + SEARCHTYPE_NOT_FOUND);
      responseBuilder =
          new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(SEARCHTYPE_NOT_FOUND);
      handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
      return null;
    }

    if (request.getJsonArray(ID).getString(0).split("/").length != 5) {
      LOGGER.error("Malformed ID: " + request.getJsonArray(ID).getString(0));
      responseBuilder =
          new ResponseBuilder(FAILED)
              .setTypeAndTitle(400)
              .setMessage(MALFORMED_ID + request.getJsonArray(ID));
      handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
      return null;
    }

    List<String> splitId =
        new LinkedList<>(Arrays.asList(request.getJsonArray(ID).getString(0).split("/")));
    splitId.remove(splitId.size() - 1);
    final String searchIndex = String.join("__", splitId);
    LOGGER.debug("Index name: " + searchIndex);

    try {
      query = new QueryDecoder().getESquery(request);
    } catch (Exception e) {
      responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(e.getMessage());
      handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
      return null;
    }

    if (query.containsKey(ERROR)) {
      LOGGER.error("Fail: Query returned with an error: " + query.getString(ERROR));
      responseBuilder =
          new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(query.getString(ERROR));
      handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
      return null;
    }

    LOGGER.info("Info: index: " + searchIndex);
    LOGGER.info("Info: Query constructed: " + query.toString());

    QueryBuilder queryBuilder = utilities.getESquery1(request, true);

    client.scrollAsync(
        searchIndex,
        queryBuilder,
        scrollHandler -> {
          if (scrollHandler.succeeded()) {
            handler.handle(Future.succeededFuture());
          } else {
            handler.handle(Future.failedFuture(scrollHandler.cause()));
          }
        });
    return null;
  }
}
