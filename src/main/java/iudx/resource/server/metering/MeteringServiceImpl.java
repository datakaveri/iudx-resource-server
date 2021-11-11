package iudx.resource.server.metering;

import static iudx.resource.server.metering.util.Constants.COLUMN_NAME;
import static iudx.resource.server.metering.util.Constants.COUNT;
import static iudx.resource.server.metering.util.Constants.EMPTY_RESPONSE;
import static iudx.resource.server.metering.util.Constants.END_TIME;
import static iudx.resource.server.metering.util.Constants.ERROR;
import static iudx.resource.server.metering.util.Constants.FAILED;
import static iudx.resource.server.metering.util.Constants.MESSAGE;
import static iudx.resource.server.metering.util.Constants.QUERY_KEY;
import static iudx.resource.server.metering.util.Constants.START_TIME;
import static iudx.resource.server.metering.util.Constants.SUCCESS;
import static iudx.resource.server.metering.util.Constants.TIME_NOT_FOUND;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import iudx.resource.server.metering.util.QueryBuilder;
import iudx.resource.server.metering.util.ResponseBuilder;

public class MeteringServiceImpl implements MeteringService {

  private static final Logger LOGGER = LogManager.getLogger(MeteringServiceImpl.class);
  PgConnectOptions connectOptions;
  PoolOptions poolOptions;
  PgPool pool;
  private Vertx vertx;
  private QueryBuilder queryBuilder = new QueryBuilder();
  private JsonObject query = new JsonObject();
  private String databaseIP;
  private int databasePort;
  private String databaseName;
  private String databaseUserName;
  private String databasePassword;
  private int databasePoolSize;
  private ResponseBuilder responseBuilder;

  public MeteringServiceImpl(JsonObject propObj, Vertx vertxInstance) {

    if (propObj != null && !propObj.isEmpty()) {
      databaseIP = propObj.getString("meteringDatabaseIP");
      databasePort = propObj.getInteger("meteringDatabasePort");
      databaseName = propObj.getString("meteringDatabaseName");
      databaseUserName = propObj.getString("meteringDatabaseUserName");
      databasePassword = propObj.getString("meteringDatabasePassword");
      databasePoolSize = propObj.getInteger("meteringPoolSize");
    }

    this.connectOptions =
        new PgConnectOptions()
            .setPort(databasePort)
            .setHost(databaseIP)
            .setDatabase(databaseName)
            .setUser(databaseUserName)
            .setPassword(databasePassword);

    this.poolOptions = new PoolOptions().setMaxSize(databasePoolSize);
    this.pool = PgPool.pool(vertxInstance, connectOptions, poolOptions);
    this.vertx = vertxInstance;
  }

  @Override
  public MeteringService executeCountQuery(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    LOGGER.debug("Info: Count Query" + request.toString());

    if (!request.containsKey(START_TIME) || !request.containsKey(END_TIME)) {
      LOGGER.debug("Info: " + TIME_NOT_FOUND);
      responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(TIME_NOT_FOUND);
      handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
      return null;
    }
    query = queryBuilder.buildReadingQuery(request);

    if (query.containsKey(ERROR)) {
      LOGGER.error("Fail: Query returned with an error: " + query.getString(ERROR));
      responseBuilder =
          new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(query.getString(ERROR));
      handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
      return null;
    }
    LOGGER.debug("Info: Query constructed: " + query.getString(QUERY_KEY));

    Future<JsonObject> result = executeCountQuery(query);
    result.onComplete(
        resultHandler -> {
          if (resultHandler.succeeded()) {
            handler.handle(Future.succeededFuture(resultHandler.result()));
          } else if (resultHandler.failed()) {
            LOGGER.error("Read from DB failed:" + resultHandler.cause());
            handler.handle(Future.failedFuture(resultHandler.cause().getMessage()));
          }
        });
    return this;
  }

  private Future<JsonObject> executeCountQuery(JsonObject query) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject response = new JsonObject();
    pool.getConnection()
        .compose(connection -> connection.query(query.getString(QUERY_KEY)).execute())
        .onComplete(
            rows -> {
              RowSet<Row> result = rows.result();
              for (Row rs : result) {
                LOGGER.debug("COUNT: " + (rs.getInteger(COLUMN_NAME)));
                response.put(COUNT, rs.getInteger(COLUMN_NAME));
              }
              if (response.getInteger(COUNT) == 0) {
                responseBuilder =
                    new ResponseBuilder(FAILED).setTypeAndTitle(204).setCount(response.getInteger(COUNT))
                        .setMessage(EMPTY_RESPONSE);
                promise.complete(responseBuilder.getResponse());
              } else {
                responseBuilder =
                    new ResponseBuilder(SUCCESS)
                        .setTypeAndTitle(200)
                        .setCount(response.getInteger(COUNT));
                LOGGER.info("Info: " + responseBuilder.getResponse().toString());
                promise.complete(responseBuilder.getResponse());
              }
            });
    return promise.future();
  }

  @Override
  public MeteringService executeWriteQuery(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    query = queryBuilder.buildWritingQuery(request);

    Future<JsonObject> result = writeInDatabase(query);
    result.onComplete(
        resultHandler -> {
          if (resultHandler.succeeded()) {
            handler.handle(Future.succeededFuture(resultHandler.result()));
          } else if (resultHandler.failed()) {
            LOGGER.error("failed ::" + resultHandler.cause());
            handler.handle(Future.failedFuture(resultHandler.cause().getMessage()));
          }
        });
    return this;
  }

  private Future<JsonObject> writeInDatabase(JsonObject query) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject response = new JsonObject();
    pool.getConnection()
        .compose(connection -> connection.query(query.getString(QUERY_KEY)).execute())
        .onComplete(
            rows -> {
              if (rows.succeeded()) {
                response.put(MESSAGE, "Table Updated Successfully");
                responseBuilder =
                    new ResponseBuilder(SUCCESS)
                        .setTypeAndTitle(200)
                        .setMessage(response.getString(MESSAGE));
                LOGGER.info("Info: " + responseBuilder.getResponse().toString());
                promise.complete(responseBuilder.getResponse());
              }
              if (rows.failed()) {
                LOGGER.error("Info: failed :" + rows.cause());
                response.put(MESSAGE, rows.cause().getMessage());
                responseBuilder =
                    new ResponseBuilder(FAILED)
                        .setTypeAndTitle(400)
                        .setMessage(response.getString(MESSAGE));
                LOGGER.info("Info: " + responseBuilder.getResponse().toString());
                promise.fail(responseBuilder.getResponse().toString());
              }
            });
    return promise.future();
  }
}
