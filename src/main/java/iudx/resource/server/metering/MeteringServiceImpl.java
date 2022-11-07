package iudx.resource.server.metering;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import iudx.resource.server.database.postgres.PostgresService;
import iudx.resource.server.metering.util.ParamsValidation;
import iudx.resource.server.metering.util.QueryBuilder;
import iudx.resource.server.metering.util.ResponseBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.resource.server.metering.util.Constants.*;

public class MeteringServiceImpl implements MeteringService {

    private static final Logger LOGGER = LogManager.getLogger(MeteringServiceImpl.class);
    public final String _COUNT_COLUMN;
    public final String _RESOURCEID_COLUMN;
    public final String _API_COLUMN;
    public final String _USERID_COLUMN;
    public final String _TIME_COLUMN;
    public final String _RESPONSE_SIZE_COLUMN;
    public final String _ID_COLUMN;
    private final Vertx vertx;
    private final QueryBuilder queryBuilder = new QueryBuilder();
    private final ParamsValidation validation = new ParamsValidation();
    PgConnectOptions connectOptions;
    PoolOptions poolOptions;
    PgPool pool;
    String queryPg, queryCount;
    int total;
    JsonObject validationCheck = new JsonObject();
    private JsonObject query = new JsonObject();
    private String databaseIP;
    private int databasePort;
    private String databaseName;
    private String databaseUserName;
    private String databasePassword;
    private int databasePoolSize;
    private String databaseTableName;
    private ResponseBuilder responseBuilder;
    private PostgresService postgresService;

    public MeteringServiceImpl(JsonObject propObj, Vertx vertxInstance, PostgresService postgresService) {
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
        this.postgresService = postgresService;

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
        _ID_COLUMN =
                ID_COLUMN
                        .insert(0, "(" + databaseName + "." + databaseTableName + ".")
                        .toString();
    }

    @Override
    public MeteringService executeReadQuery(
            JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

        Promise<JsonObject> promise = Promise.promise();
        JsonObject response = new JsonObject();

        LOGGER.trace("Info: Read Query" + request.toString());

        validationCheck = validation.paramsCheck(request);

        if (validationCheck != null && validationCheck.containsKey(ERROR)) {
            responseBuilder =
                    new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(validationCheck.getString(ERROR));
            handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
            return this;
        }
        request.put(TABLE_NAME, databaseTableName);

        String count = request.getString("options");
        if (count == null) {
            countQueryForRead(request,handler);
        } else {
            countQuery(request, handler);
        }

        return this;
    }

    private void countQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
        queryCount = queryBuilder.buildCountReadQueryByPG(request);
        Future<JsonObject> resultCountPg = databaseOperation(queryCount);
        resultCountPg.onComplete(countHandler -> {
            if (countHandler.succeeded()) {
                total = Integer.parseInt(countHandler.result().getJsonArray("result").getJsonObject(0).getString("count"));
                if (total == 0) {
                    responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(204).setCount(0);
                    handler.handle(Future.succeededFuture(responseBuilder.getResponse()));

                } else {
                    responseBuilder = new ResponseBuilder(SUCCESS).setTypeAndTitle(200).setCount(total);
                    handler.handle(Future.succeededFuture(responseBuilder.getResponse()));
                }
            }
        });
    }
    private void countQueryForRead(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
        queryCount = queryBuilder.buildCountReadQueryByPG(request);
        Future<JsonObject> resultCountPg = databaseOperation(queryCount);
        resultCountPg.onComplete(countHandler -> {
            if (countHandler.succeeded()) {
                total = Integer.parseInt(countHandler.result().getJsonArray("result").getJsonObject(0).getString("count"));
                if (total == 0) {
                    responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(204).setCount(0);
                    handler.handle(Future.succeededFuture(responseBuilder.getResponse()));

                } else {
                    readMethod(request,handler);
                }
            }
        });
    }

    private void readMethod(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
            queryPg = queryBuilder.buildReadQueryByPG(request);
            Future<JsonObject> resultsPg = databaseOperation(queryPg);
            resultsPg.onComplete(readHandler -> {
                if (readHandler.succeeded()) {
                    LOGGER.info("Read Completed successfully");
                    handler.handle(Future.succeededFuture(readHandler.result()));

                } else {
                    LOGGER.debug("Could not read from DB : " + readHandler.cause());
                    handler.handle(Future.failedFuture(readHandler.cause().getMessage()));
                }
            });
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

    private Future<JsonObject> databaseOperation(String query) {
        Promise<JsonObject> promise = Promise.promise();
        JsonObject response = new JsonObject();
        postgresService.executeQuery(
                query,
                dbHandler -> {
                    if (dbHandler.succeeded()) {
                        promise.complete(dbHandler.result());
                    } else {

                        promise.fail(dbHandler.cause().getMessage());
                    }
                });

        return promise.future();
    }


}
