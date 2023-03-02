package iudx.resource.server.metering;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import iudx.resource.server.cache.CacheService;
import iudx.resource.server.cache.cacheImpl.CacheType;
import iudx.resource.server.common.Response;
import iudx.resource.server.database.postgres.PostgresService;
import iudx.resource.server.databroker.DataBrokerService;
import iudx.resource.server.metering.readpg.ReadQueryBuilder;
import iudx.resource.server.metering.util.ParamsValidation;
import iudx.resource.server.metering.util.QueryBuilder;
import iudx.resource.server.metering.util.ResponseBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.common.Constants.BROKER_SERVICE_ADDRESS;
import static iudx.resource.server.common.Constants.CACHE_SERVICE_ADDRESS;
import static iudx.resource.server.metering.util.Constants.*;

public class MeteringServiceImpl implements MeteringService {

    private static final Logger LOGGER = LogManager.getLogger(MeteringServiceImpl.class);
    public static DataBrokerService rmqService;
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
    private final ObjectMapper objectMapper = new ObjectMapper();
    PgConnectOptions connectOptions;
    PoolOptions poolOptions;
    PgPool pool;
    String queryPg, queryCount, queryOverview, summaryOverview;
    long total;
    JsonObject validationCheck = new JsonObject();
    JsonArray jsonArray;
    JsonArray resultJsonArray;
    int i;
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
    private CacheService cacheService;

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
        this.rmqService = DataBrokerService.createProxy(vertxInstance, BROKER_SERVICE_ADDRESS);

        this.cacheService = CacheService.createProxy(vertxInstance, CACHE_SERVICE_ADDRESS);

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
            countQueryForRead(request, handler);
        } else {
            countQuery(request, handler);
        }

        return this;
    }

    private void countQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
        queryCount = queryBuilder.buildCountReadQueryFromPG(request);
        Future<JsonObject> resultCountPg = executeQueryDatabaseOperation(queryCount);
        resultCountPg.onComplete(countHandler -> {
            if (countHandler.succeeded()) {
                try {
                    var countHandle = countHandler.result().getJsonArray("result");
                    total = countHandle.getJsonObject(0).getInteger("count");
                    if (total == 0) {
                        responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(204).setCount(0);
                        handler.handle(Future.succeededFuture(responseBuilder.getResponse()));

                    } else {
                        responseBuilder = new ResponseBuilder(SUCCESS).setTypeAndTitle(200).setCount((int) total);
                        handler.handle(Future.succeededFuture(responseBuilder.getResponse()));
                    }
                } catch (NullPointerException nullPointerException) {
                    LOGGER.debug(nullPointerException.toString());
                }
            }
        });
    }

    private void countQueryForRead(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
        queryCount = queryBuilder.buildCountReadQueryFromPG(request);

        Future<JsonObject> resultCountPg = executeQueryDatabaseOperation(queryCount);
        resultCountPg.onComplete(countHandler -> {
            if (countHandler.succeeded()) {
                try {
                    var countHandle = countHandler.result().getJsonArray("result");
                    total = countHandle.getJsonObject(0).getInteger("count");
                    request.put(TOTALHITS, total);
                    if (total == 0) {
                        responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(204).setCount(0);
                        handler.handle(Future.succeededFuture(responseBuilder.getResponse()));

                    } else {
                        readMethod(request, handler);
                    }
                } catch (NullPointerException nullPointerException) {
                    LOGGER.debug(nullPointerException.toString());
                }
            }
        });
    }

    private void readMethod(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
        ReadQueryBuilder readQueryBuilder = new ReadQueryBuilder();
        String limit, offset;
        if (request.getString(LIMITPARAM) == null) {
            limit = "2000";
            request.put(LIMITPARAM, limit);
        } else {
            limit = request.getString(LIMITPARAM);
        }
        if (request.getString(OFFSETPARAM) == null) {
            offset = "0";
            request.put(OFFSETPARAM, offset);
        } else {
            offset = request.getString(OFFSETPARAM);
        }
        queryPg = readQueryBuilder.getQuery(request);
        LOGGER.debug("read query = " + queryPg);
        Future<JsonObject> resultsPg = executeQueryDatabaseOperation(queryPg);
        resultsPg.onComplete(readHandler -> {
            if (readHandler.succeeded()) {
                LOGGER.info("Read Completed successfully");
                JsonObject resultJsonObject = readHandler.result();
                resultJsonObject.put(LIMITPARAM, limit);
                resultJsonObject.put(OFFSETPARAM, offset);
                resultJsonObject.put(TOTALHITS, request.getLong(TOTALHITS));
                handler.handle(Future.succeededFuture(resultJsonObject));
            } else {
                LOGGER.debug("Could not read from DB : " + readHandler.cause());
                handler.handle(Future.failedFuture(readHandler.cause().getMessage()));
            }
        });
    }

    @Override
    public MeteringService monthlyOverview(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
        String startTime = request.getString(STARTT);
        String endTime = request.getString(ENDT);
        if ((startTime != null && endTime == null) || (startTime == null && endTime != null)) {
            handler.handle(Future.failedFuture("Bad Request"));
        }
        queryOverview = queryBuilder.buildMonthlyOverview(request);
        LOGGER.debug("query Overview =" + queryOverview);

        Future<JsonObject> result = executeQueryDatabaseOperation(queryOverview);
        result.onComplete(handlers -> {
            if (handlers.succeeded()) {
                LOGGER.debug("Count return Successfully");
                handler.handle(Future.succeededFuture(handlers.result()));
            } else {
                LOGGER.debug("Could not read from DB : " + handlers.cause());
                handler.handle(Future.failedFuture(handlers.cause().getMessage()));
            }
        });
        return this;
    }

    @Override
    public MeteringService summaryOverview(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
        String startTime = request.getString(STARTT);
        String endTime = request.getString(ENDT);
        if ((startTime != null && endTime == null) || (startTime == null && endTime != null)) {
            handler.handle(Future.failedFuture("Bad Request"));
        }
        summaryOverview = queryBuilder.buildSummaryOverview(request);
        LOGGER.debug("summary query =" + summaryOverview);
        Future<JsonObject> result = executeQueryDatabaseOperation(summaryOverview);
        result.onComplete(handlers -> {
            if (handlers.succeeded()) {
                jsonArray = handlers.result().getJsonArray("result");
                if (jsonArray.size() == 0) {
                    responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(204).setMessage("NO ID Present");
                    handler.handle(Future.succeededFuture(responseBuilder.getResponse()));

                }
                cacheCall(jsonArray).onSuccess(resultHandler -> {
                    JsonObject resultJson = new JsonObject().put("type", "urn:dx:dm:Success")
                            .put("title", "Success")
                            .put("results", resultHandler);
                    handler.handle(Future.succeededFuture(resultJson));
                });
            } else {
                LOGGER.debug("Could not read from DB : " + handlers.cause());
                handler.handle(Future.failedFuture(handlers.cause().getMessage()));
            }
        });

        return this;
    }

    public Future<JsonArray> cacheCall(JsonArray jsonArray) {
        Promise<JsonArray> promise = Promise.promise();
        HashMap<String, Integer> resourceCount = new HashMap<>();
        resultJsonArray = new JsonArray();
        List<Future> list = new ArrayList<>();

        for (i = 0; i < jsonArray.size(); i++) {
            JsonObject jsonObject = new JsonObject()
                    .put("type", CacheType.CATALOGUE_CACHE)
                    .put("key", jsonArray.getJsonObject(i).getString("resourceid"));
            resourceCount.put(jsonArray.getJsonObject(i).getString("resourceid"), Integer.valueOf(jsonArray.getJsonObject(i).getString("count")));

            list.add(cacheService.get(jsonObject));
        }

        CompositeFuture.join(list).map(CompositeFuture::list).map(result -> result.stream()
                .filter(Objects::nonNull).collect(Collectors.toList())).onSuccess(l -> {
            for (int i = 0; i < l.size(); i++) {
                JsonObject result = (JsonObject) l.get(i);
                JsonObject outputFormat = new JsonObject()
                        .put("resourceid", result.getString("id"))
                        .put("resource_label", result.getString("description"))
                        .put("publisher", result.getString("name"))
                        .put("publisher_id", result.getString("provider"))
                        .put("city", result.getString("instance"))
                        .put("count", resourceCount.get(result.getString("id")));

                resultJsonArray.add(outputFormat);
            }
            promise.complete(resultJsonArray);
        });

        return promise.future();
    }

    @Override
    public MeteringService insertMeteringValuesInRMQ(
            JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

        JsonObject writeMessage = queryBuilder.buildMessageForRMQ(request);

        rmqService.publishMessage(writeMessage, EXCHANGE_NAME, ROUTING_KEY,
                rmqHandler -> {
                    if (rmqHandler.succeeded()) {
                        handler.handle(Future.succeededFuture());
                        LOGGER.info("inserted into rmq");
                    } else {
                        LOGGER.error(rmqHandler.cause());
                        try {
                            Response resp =
                                    objectMapper.readValue(rmqHandler.cause().getMessage(), Response.class);
                            LOGGER.debug("response from rmq " + resp);
                            handler.handle(Future.failedFuture(resp.toString()));
                        } catch (JsonProcessingException e) {
                            LOGGER.error("Failure message not in format [type,title,detail]");
                            handler.handle(Future.failedFuture(e.getMessage()));
                        }
                    }
                });
        return this;
    }

    private Future<JsonObject> executeQueryDatabaseOperation(String query) {
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
