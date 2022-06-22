package iudx.resource.server.metering;

import static iudx.resource.server.apiserver.util.Constants.HEADER_OPTIONS;
import static iudx.resource.server.apiserver.util.Constants.IUDX_PROVIDER_AUDIT_URL;
import static iudx.resource.server.metering.util.Constants.API_COLUMN;
import static iudx.resource.server.metering.util.Constants.COUNT_COLUMN;
import static iudx.resource.server.metering.util.Constants.DURING;
import static iudx.resource.server.metering.util.Constants.BETWEEN;
import static iudx.resource.server.metering.util.Constants.ENDPOINT;
import static iudx.resource.server.metering.util.Constants.END_TIME;
import static iudx.resource.server.metering.util.Constants.ERROR;
import static iudx.resource.server.metering.util.Constants.FAILED;
import static iudx.resource.server.metering.util.Constants.INVALID_PROVIDER_REQUIRED;
import static iudx.resource.server.metering.util.Constants.MESSAGE;
import static iudx.resource.server.metering.util.Constants.PROVIDER_ID;
import static iudx.resource.server.metering.util.Constants.QUERY_KEY;
import static iudx.resource.server.metering.util.Constants.RESOURCEID_COLUMN;
import static iudx.resource.server.metering.util.Constants.RESPONSE_SIZE_COLUMN;
import static iudx.resource.server.metering.util.Constants.START_TIME;
import static iudx.resource.server.metering.util.Constants.SUCCESS;
import static iudx.resource.server.metering.util.Constants.TABLE_NAME;
import static iudx.resource.server.metering.util.Constants.TIME_COLUMN;
import static iudx.resource.server.metering.util.Constants.TIME_NOT_FOUND;
import static iudx.resource.server.metering.util.Constants.TIME_RELATION;
import static iudx.resource.server.metering.util.Constants.TIME_RELATION_NOT_FOUND;
import static iudx.resource.server.metering.util.Constants.TOTAL;
import static iudx.resource.server.metering.util.Constants.USERID_COLUMN;
import static iudx.resource.server.metering.util.Constants.USERID_NOT_FOUND;
import static iudx.resource.server.metering.util.Constants.USER_ID;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import iudx.resource.server.metering.util.QueryBuilder;
import iudx.resource.server.metering.util.ResponseBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MeteringServiceImpl implements MeteringService {

  private static final Logger LOGGER = LogManager.getLogger(MeteringServiceImpl.class);
  public final String _COUNT_COLUMN;
  public final String _RESOURCEID_COLUMN;
  public final String _API_COLUMN;
  public final String _USERID_COLUMN;
  public final String _TIME_COLUMN;
  public final String _RESPONSE_SIZE_COLUMN;
  private final Vertx vertx;
  private final QueryBuilder queryBuilder = new QueryBuilder();
  PgConnectOptions connectOptions;
  PoolOptions poolOptions;
  PgPool pool;
  private JsonObject query = new JsonObject();
  private String databaseIP;
  private int databasePort;
  private String databaseName;
  private String databaseUserName;
  private String databasePassword;
  private int databasePoolSize;
  private String databaseTableName;
  private ResponseBuilder responseBuilder;

  public final String _COUNT_COLUMN;
  public final String _RESOURCEID_COLUMN;
  public final String _API_COLUMN;
  public final String _USERID_COLUMN;
  public final String _TIME_COLUMN;
  
  public MeteringServiceImpl(JsonObject propObj, Vertx vertxInstance) {

    if (propObj != null && !propObj.isEmpty()) {
      databaseIP = propObj.getString("meteringDatabaseIP");
      databasePort = propObj.getInteger("meteringDatabasePort");
      databaseName = propObj.getString("meteringDatabaseName");
      databaseUserName = propObj.getString("meteringDatabaseUserName");
      databasePassword = propObj.getString("meteringDatabasePassword");
      databasePoolSize = propObj.getInteger("meteringPoolSize");
      databaseTableName = propObj.getString("meteringDatabaseTableName");
    }

    this.connectOptions =
        new PgConnectOptions()
            .setPort(databasePort)
            .setHost(databaseIP)
            .setDatabase(databaseName)
            .setUser(databaseUserName)
            .setPassword(databasePassword)
            .setReconnectAttempts(2)
            .setReconnectInterval(1000);

    this.poolOptions = new PoolOptions().setMaxSize(databasePoolSize);
    this.pool = PgPool.pool(vertxInstance, connectOptions, poolOptions);
    this.vertx = vertxInstance;

    _COUNT_COLUMN =
        COUNT_COLUMN.insert(0, "(" + databaseName + "." + databaseTableName + ".").toString();
    _RESOURCEID_COLUMN =
        RESOURCEID_COLUMN.insert(0, "(" + databaseName + "." + databaseTableName + ".").toString();
    _API_COLUMN =
        API_COLUMN.insert(0, "(" + databaseName + "." + databaseTableName + ".").toString();
    _USERID_COLUMN =
        USERID_COLUMN.insert(0, "(" + databaseName + "." + databaseTableName + ".").toString();
    _TIME_COLUMN =
        TIME_COLUMN.insert(0, "(" + databaseName + "." + databaseTableName + ".").toString();
    _RESPONSE_SIZE_COLUMN =
        RESPONSE_SIZE_COLUMN
            .insert(0, "(" + databaseName + "." + databaseTableName + ".")
            .toString();
  }

  @Override
  public MeteringService executeReadQuery(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    LOGGER.trace("Info: Count Query" + request.toString());

    if (request.getString(ENDPOINT).equals(IUDX_PROVIDER_AUDIT_URL)
        && request.getString(PROVIDER_ID) == null) {
      responseBuilder =
          new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(INVALID_PROVIDER_REQUIRED);
      handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
      return this;
    }

    if (request.getString(TIME_RELATION) == null
        || !(request.getString(TIME_RELATION).equals(DURING)
            || request.getString(TIME_RELATION).equals(BETWEEN))) {
      LOGGER.debug("Info: " + TIME_RELATION_NOT_FOUND);
      responseBuilder =
          new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(TIME_RELATION_NOT_FOUND);
      handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
      return this;
    }

    if (request.getString(START_TIME) == null || request.getString(END_TIME) == null) {
      LOGGER.debug("Info: " + TIME_NOT_FOUND);
      responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(TIME_NOT_FOUND);
      handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
      return this;
    }

    if (request.getString(USER_ID) == null || request.getString(USER_ID).isEmpty()) {
      LOGGER.debug("Info: " + USERID_NOT_FOUND);
      responseBuilder =
          new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(USERID_NOT_FOUND);
      handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
      return this;
    }
    request.put(TABLE_NAME, databaseTableName);
    query = queryBuilder.buildReadingQuery(request);

    LOGGER.info(query);
    if (query.containsKey(ERROR)) {
      LOGGER.error("Fail: Query returned with an error: " + query.getString(ERROR));
      responseBuilder =
          new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(query.getString(ERROR));
      handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
      return this;
    }
    LOGGER.debug("Info: Query constructed: " + query.getString(QUERY_KEY));
    Future<JsonObject> result;

    if (request.getString(HEADER_OPTIONS) != null) {
      result = executeCountQuery(query);
    } else result = executeReadQuery(query);

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

  private Future<JsonObject> executeReadQuery(JsonObject query) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject response = new JsonObject();
    pool.withConnection(connection -> connection.query(query.getString(QUERY_KEY)).execute())
        .onSuccess(
            rows -> {
              JsonArray jsonArray = new JsonArray();
              RowSet<Row> result = rows;
              for (Row rs : result) {
                JsonObject temp = new JsonObject();
                temp.put("id", rs.getString(_RESOURCEID_COLUMN));
                temp.put("time", rs.getString(_TIME_COLUMN));
                temp.put("api", rs.getString(_API_COLUMN));
                temp.put("consumer", rs.getString(_USERID_COLUMN));
                jsonArray.add(temp);
              }

              if (jsonArray.isEmpty()) {
                responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(204);
                promise.fail(responseBuilder.getResponse().toString());
              } else {
                responseBuilder =
                    new ResponseBuilder(SUCCESS).setTypeAndTitle(200).setData(jsonArray);
                //                LOGGER.info("Info: " + responseBuilder.getResponse().toString());
                promise.complete(responseBuilder.getResponse());
              }
            })
        .onFailure(
            event -> {
              promise.fail("Failed to get connection from the database");
            });

    return promise.future();
  }

  private Future<JsonObject> executeCountQuery(JsonObject query) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject response = new JsonObject();
    pool.withConnection(connection -> connection.query(query.getString(QUERY_KEY)).execute())
        .onSuccess(
            rows -> {
              RowSet<Row> result = rows;
              for (Row rs : result) {
                LOGGER.debug("COUNT: " + (rs.getInteger(_COUNT_COLUMN)));
                response.put(TOTAL, rs.getInteger(_COUNT_COLUMN));
              }
              if (response.getInteger(TOTAL) == 0) {
                responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(204);
                promise.fail(responseBuilder.getResponse().toString());
              } else {
                responseBuilder =
                    new ResponseBuilder(SUCCESS)
                        .setTypeAndTitle(200)
                        .setCount(response.getInteger(TOTAL));
                LOGGER.debug("Info: " + responseBuilder.getResponse().toString());
                promise.complete(responseBuilder.getResponse());
              }
            })
        .onFailure(
            event -> {
              promise.fail("Failed to get connection from the database");
            });
    return promise.future();
  }

  @Override
  public MeteringService executeWriteQuery(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    request.put(TABLE_NAME, databaseTableName);
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
    pool.withConnection(connection -> connection.query(query.getString(QUERY_KEY)).execute())
        .onComplete(
            rows -> {
              if (rows.succeeded()) {
                response.put(MESSAGE, "Table Updated Successfully");
                responseBuilder =
                    new ResponseBuilder(SUCCESS)
                        .setTypeAndTitle(200)
                        .setMessage(response.getString(MESSAGE));
                LOGGER.debug("Info: " + responseBuilder.getResponse().toString());
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
