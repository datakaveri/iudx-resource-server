package iudx.resource.server.apiserver.management;

import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.cache.cacheImpl.CacheType.CATALOGUE_CACHE;
import static iudx.resource.server.common.Constants.CREATE_INGESTION_SQL;
import static iudx.resource.server.common.Constants.DELETE_INGESTION_SQL;
import static iudx.resource.server.common.Constants.SELECT_INGESTION_SQL;
import static iudx.resource.server.databroker.util.Constants.RESULTS;
import static iudx.resource.server.databroker.util.Constants.TITLE;
import static iudx.resource.server.databroker.util.Constants.TYPE;
import static iudx.resource.server.metering.util.Constants.PROVIDER_ID;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.response.ResponseType;
import iudx.resource.server.cache.CacheService;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.common.VHosts;
import iudx.resource.server.database.postgres.PostgresService;
import iudx.resource.server.databroker.DataBrokerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class handles all DataBrokerService related interactions of API server. TODO Need to add
 * documentation.
 */
public class ManagementApiImpl implements ManagementApi {

  private static final Logger LOGGER = LogManager.getLogger(ManagementApiImpl.class);

  /** {@inheritDoc} */
  @Override
  public Future<JsonObject> createExchange(JsonObject json, DataBrokerService databroker) {
    Promise<JsonObject> promise = Promise.promise();
    LOGGER.info("data broker ::: " + databroker);
    databroker.createExchange(
        json,
        VHosts.IUDX_PROD.name(),
        handler -> {
          if (handler.succeeded()) {
            JsonObject result = handler.result();
            LOGGER.debug("Result from databroker verticle :: " + result);
            if (!result.containsKey("type")) {
              promise.complete(result);
            } else {
              promise.fail(result.toString());
            }
          } else if (handler.failed()) {
            LOGGER.error(handler.cause());
            promise.fail(handler.cause().getMessage());
          }
        });
    return promise.future();
  }

  /** {@inheritDoc} */
  @Override
  public Future<JsonObject> deleteExchange(String exchangeid, DataBrokerService databroker) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject json = new JsonObject();
    json.put(JSON_EXCHANGE_NAME, exchangeid);
    databroker.deleteExchange(
        json,
        VHosts.IUDX_PROD.name(),
        handler -> {
          if (handler.succeeded()) {
            JsonObject result = handler.result();
            LOGGER.debug("Result from databroker verticle :: " + result);
            if (!result.containsKey("type")) {
              promise.complete(result);
            } else {
              promise.fail(result.toString());
            }
          } else if (handler.failed()) {
            LOGGER.error(handler.cause());
            promise.fail(handler.cause().getMessage());
          }
        });
    return promise.future();
  }

  /** {@inheritDoc} */
  @Override
  public Future<JsonObject> getExchangeDetails(String exchangeid, DataBrokerService databroker) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject json = new JsonObject();
    json.put(JSON_EXCHANGE_NAME, exchangeid);
    json.put("id", exchangeid);
    databroker.listExchangeSubscribers(
        json,
        VHosts.IUDX_PROD.name(),
        handler -> {
          if (handler.succeeded()) {
            JsonObject result = handler.result();
            LOGGER.debug("Result from databroker verticle :: " + result);
            if (!result.containsKey(JSON_TYPE)) {
              promise.complete(result);
            } else {
              promise.fail(result.toString());
            }
          } else if (handler.failed()) {
            LOGGER.error(handler.cause());
            promise.fail(handler.cause().getMessage());
          }
        });
    return promise.future();
  }

  /** {@inheritDoc} */
  @Override
  public Future<JsonObject> createQueue(JsonObject json, DataBrokerService databroker) {
    Promise<JsonObject> promise = Promise.promise();
    databroker.createQueue(
        json,
        VHosts.IUDX_PROD.name(),
        handler -> {
          if (handler.succeeded()) {
            JsonObject result = handler.result();
            LOGGER.debug("Result from databroker verticle :: " + result);
            if (!result.containsKey(JSON_TYPE)) {
              promise.complete(result);
            } else {
              promise.fail(result.toString());
            }
          } else if (handler.failed()) {
            LOGGER.error(handler.cause());
            promise.fail(handler.cause().getMessage());
          }
        });
    return promise.future();
  }

  /** {@inheritDoc} */
  @Override
  public Future<JsonObject> deleteQueue(String queueId, DataBrokerService databroker) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject json = new JsonObject();
    json.put(JSON_QUEUE_NAME, queueId);
    databroker.deleteQueue(
        json,
        VHosts.IUDX_PROD.name(),
        handler -> {
          if (handler.succeeded()) {
            JsonObject result = handler.result();
            LOGGER.debug("Result from databroker verticle :: " + result);
            if (!result.containsKey(JSON_TYPE)) {
              promise.complete(result);
            } else {
              promise.fail(result.toString());
            }
          } else {
            LOGGER.error(handler.cause());
            promise.fail(handler.cause().getMessage());
          }
        });
    return promise.future();
  }

  /** {@inheritDoc} */
  @Override
  public Future<JsonObject> getQueueDetails(String queueId, DataBrokerService databroker) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject json = new JsonObject();
    json.put(JSON_QUEUE_NAME, queueId);
    databroker.listQueueSubscribers(
        json,
        VHosts.IUDX_PROD.name(),
        handler -> {
          if (handler.succeeded()) {
            JsonObject result = handler.result();
            LOGGER.debug("Result from databroker verticle :: " + result);
            if (!result.containsKey(JSON_TYPE)) {
              promise.complete(result);
            } else {
              promise.fail(result.toString());
            }
          } else {
            LOGGER.error(handler.cause());
            promise.fail(handler.cause().getMessage());
          }
        });
    return promise.future();
  }

  /** {@inheritDoc} */
  @Override
  public Future<JsonObject> bindQueue2Exchange(JsonObject json, DataBrokerService databroker) {
    Promise<JsonObject> promise = Promise.promise();
    databroker.bindQueue(
        json,
        VHosts.IUDX_PROD.name(),
        handler -> {
          if (handler.succeeded()) {
            JsonObject result = handler.result();
            LOGGER.debug("Result from databroker verticle :: " + result);
            if (!result.containsKey(JSON_TYPE)) {
              promise.complete(result);
            } else {
              promise.fail(result.toString());
            }
          } else {
            LOGGER.error(handler.cause());
            promise.fail(handler.cause().getMessage());
          }
        });
    return promise.future();
  }

  /** {@inheritDoc} */
  @Override
  public Future<JsonObject> unbindQueue2Exchange(JsonObject json, DataBrokerService databroker) {
    Promise<JsonObject> promise = Promise.promise();
    LOGGER.trace("unbind request :: " + json);
    databroker.unbindQueue(
        json,
        VHosts.IUDX_PROD.name(),
        handler -> {
          if (handler.succeeded()) {
            JsonObject result = handler.result();
            LOGGER.debug("Result from databroker verticle :: " + result);
            if (!result.containsKey(JSON_TYPE)) {
              promise.complete(result);
            } else {
              promise.fail(result.toString());
            }
          } else {
            LOGGER.error(handler.cause());
            promise.fail(handler.cause().getMessage());
          }
        });
    return promise.future();
  }

  /** {@inheritDoc} */
  @Override
  public Future<JsonObject> createVhost(JsonObject json, DataBrokerService databroker) {
    Promise<JsonObject> promise = Promise.promise();
    databroker.createvHost(
        json,
        handler -> {
          if (handler.succeeded()) {
            JsonObject result = handler.result();
            LOGGER.debug("Result from databroker verticle :: " + result);
            if (!result.containsKey(JSON_TYPE)) {
              promise.complete(result);
            } else {
              promise.fail(result.toString());
            }
          } else {
            LOGGER.error(handler.cause());
            promise.fail(handler.cause().getMessage());
          }
        });
    return promise.future();
  }

  /** {@inheritDoc} */
  @Override
  public Future<JsonObject> deleteVhost(String vhostId, DataBrokerService databroker) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject json = new JsonObject();
    json.put(JSON_VHOST, vhostId);
    databroker.deletevHost(
        json,
        handler -> {
          if (handler.succeeded()) {
            JsonObject result = handler.result();
            LOGGER.debug("Result from databroker verticle :: " + result);
            if (!result.containsKey(JSON_TYPE)) {
              promise.complete(result);
            } else {
              promise.fail(result.toString());
            }
          } else {
            LOGGER.error(handler.cause());
            promise.fail(handler.cause().getMessage());
          }
        });
    return promise.future();
  }

  /** {@inheritDoc} */
  @Override
  public Future<JsonObject> registerAdapter(
      JsonObject requestJson,
      DataBrokerService dataBroker,
      CacheService cacheService,
      PostgresService postgresService) {
    Promise<JsonObject> promise = Promise.promise();

    JsonObject cacheJson =
        new JsonObject()
            .put("key", requestJson.getJsonArray("entities").getString(0))
            .put("type", CATALOGUE_CACHE);

    cacheService
        .get(cacheJson)
        .onSuccess(
            cacheServiceResult -> {
              StringBuilder query =
                  new StringBuilder(
                      CREATE_INGESTION_SQL
                          .replace(
                              "$1",
                              requestJson.getJsonArray("entities").getString(0)) /* exchange name */
                          .replace("$2", cacheServiceResult.getString("id")) /* resource id */
                          .replace("$3", cacheServiceResult.getString("name")) /* dataset name */
                          .replace("$4", cacheServiceResult.toString()) /* dataset json */
                          .replace("$5", requestJson.getString("userid"))); /* user id */

              postgresService.executeQuery(
                  query.toString(),
                  pgHandler -> {
                    if (pgHandler.succeeded()) {
                      LOGGER.debug("Inserted in postgres.");
                      dataBroker.registerAdaptor(
                          requestJson,
                          VHosts.IUDX_PROD.name(),
                          brokerHandler -> {
                            if (brokerHandler.succeeded()) {
                              JsonObject brokerResponse = brokerHandler.result();
                              if (!brokerResponse.containsKey(JSON_TYPE)) {
                                JsonObject iudxResponse = new JsonObject();
                                iudxResponse.put(TYPE, ResponseUrn.SUCCESS_URN.getUrn());
                                iudxResponse.put(TITLE, "Success");
                                iudxResponse.put(RESULTS, new JsonArray().add(brokerResponse));
                                promise.complete(iudxResponse);
                              } else {
                                promise.fail(generateResponse(brokerResponse).toString());
                              }
                            } else {
                              StringBuilder deleteQuery =
                                  new StringBuilder(
                                      DELETE_INGESTION_SQL.replace(
                                          "$0", requestJson.getJsonArray("entities").getString(0)));
                              postgresService.executeQuery(
                                  deleteQuery.toString(),
                                  deletePgHandler -> {
                                    if (deletePgHandler.succeeded()) {
                                      LOGGER.debug("Deleted from postgres.");
                                      LOGGER.error(
                                          "broker fail " + brokerHandler.cause().getMessage());
                                      promise.fail(brokerHandler.cause().getMessage());
                                    }
                                  });
                            }
                          });
                    } else {
                      JsonObject pgFailResponseBuild = new JsonObject();
                      pgFailResponseBuild.put(TYPE, 409);
                      pgFailResponseBuild.put(TITLE, "urn:dx:rs:resourceAlreadyExist");
                      promise.fail(pgFailResponseBuild.toString());
                    }
                  });
            })
        .onFailure(
            cacheFailHandler -> {
              JsonObject cacheFailResponseBuild = new JsonObject();
              cacheFailResponseBuild.put(TYPE, 404);
              cacheFailResponseBuild.put(TITLE, "urn:dx:rs:resourceNotFound");
              promise.fail(cacheFailResponseBuild.toString());
            });
    return promise.future();
  }

  /** {@inheritDoc} */
  @Override
  public Future<JsonObject> deleteAdapter(
      String adapterId,
      String userId,
      DataBrokerService dataBrokerService,
      PostgresService postgresService) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject json = new JsonObject();
    json.put(JSON_ID, adapterId);
    json.put(USER_ID, userId);
    dataBrokerService.deleteAdaptor(
        json,
        VHosts.IUDX_PROD.name(),
        dataBrokerHandler -> {
          if (dataBrokerHandler.succeeded()) {
            postgresService.executeQuery(
                DELETE_INGESTION_SQL.replace("$0", adapterId),
                pgHandler -> {
                  if (pgHandler.succeeded()) {
                    JsonObject result = dataBrokerHandler.result();
                    LOGGER.debug("Result from dataBroker verticle :: " + result);
                    JsonObject iudxResponse = new JsonObject();
                    iudxResponse.put(TYPE, ResponseUrn.SUCCESS_URN.getUrn());
                    iudxResponse.put(TITLE, "Success");
                    iudxResponse.put(RESULTS, "Adapter deleted");
                    promise.complete(iudxResponse);
                  } else {
                    // TODO need to figure out the rollback if postgres delete fails
                    LOGGER.debug("fail to delete");
                    promise.fail("unable to delete");
                  }
                });
          } else if (dataBrokerHandler.failed()) {
            String result = dataBrokerHandler.cause().getMessage();
            promise.fail(generateResponse(new JsonObject(result)).toString());
          }
        });
    return promise.future();
  }

  /** {@inheritDoc} */
  @Override
  public Future<JsonObject> getAdapterDetails(String adapterId, DataBrokerService databroker) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject json = new JsonObject();
    json.put(JSON_ID, adapterId);
    databroker.listAdaptor(
        json,
        VHosts.IUDX_PROD.name(),
        handler -> {
          if (handler.succeeded()) {
            JsonObject result = handler.result();
            LOGGER.debug("Result from databroker verticle :: " + result);
            if (!result.containsKey(JSON_TYPE)) {

              JsonObject iudxResponse = new JsonObject();
              iudxResponse.put(TYPE, ResponseUrn.SUCCESS_URN.getUrn());
              iudxResponse.put(TITLE, "Success");
              iudxResponse.put(RESULTS, new JsonArray().add(result));

              promise.complete(iudxResponse);
            } else {
              promise.fail(generateResponse(result).toString());
            }
          } else {
            promise.fail(handler.cause().getMessage());
          }
        });
    return promise.future();
  }

  /** {@inheritDoc} */
  @Override
  public Future<JsonObject> publishHeartbeat(JsonObject json, DataBrokerService databroker) {
    Promise<JsonObject> promise = Promise.promise();
    databroker.publishHeartbeat(
        json,
        VHosts.IUDX_PROD.name(),
        handler -> {
          if (handler.succeeded()) {
            JsonObject result = handler.result();
            LOGGER.debug("Result from databroker verticle :: " + result);
            if (result.containsKey(JSON_TYPE)
                && result.getString(JSON_TYPE).equalsIgnoreCase(SUCCCESS)) {
              promise.complete(result);
            } else {
              promise.fail(result.toString());
            }
          } else {
            promise.fail(handler.cause().getMessage());
          }
        });
    return promise.future();
  }

  /** {@inheritDoc} */
  @Override
  public Future<JsonObject> publishDownstreamIssues(JsonObject json, DataBrokerService databroker) {
    Promise<JsonObject> promise = Promise.promise();
    databroker.publishHeartbeat(
        json,
        VHosts.IUDX_PROD.name(),
        handler -> {
          if (handler.succeeded()) {
            JsonObject result = handler.result();
            LOGGER.debug("Result from databroker verticle :: " + result);
            if (result.getString(JSON_TYPE).equalsIgnoreCase(SUCCCESS)) {
              promise.complete(result);
            } else {
              promise.fail(result.toString());
            }
          } else {
            promise.fail(handler.cause().getMessage());
          }
        });
    return promise.future();
  }

  /** {@inheritDoc} */
  @Override
  public Future<JsonObject> publishDataIssue(JsonObject json, DataBrokerService databroker) {
    Promise<JsonObject> promise = Promise.promise();
    databroker.publishHeartbeat(
        json,
        VHosts.IUDX_PROD.name(),
        handler -> {
          if (handler.succeeded()) {
            JsonObject result = handler.result();
            LOGGER.debug("Result from databroker verticle :: " + result);
            if (result.getString(JSON_TYPE).equalsIgnoreCase(SUCCCESS)) {
              promise.complete(result);
            } else {
              promise.fail(result.toString());
            }
          } else {
            promise.fail(handler.cause().getMessage());
          }
        });
    return promise.future();
  }

  /** {@inheritDoc} */
  @Override
  public Future<JsonObject> publishDataFromAdapter(JsonObject json, DataBrokerService databroker) {
    Promise<JsonObject> promise = Promise.promise();
    databroker.publishFromAdaptor(
        json,
        VHosts.IUDX_PROD.name(),
        handler -> {
          if (handler.succeeded()) {
            JsonObject result = handler.result();
            LOGGER.debug("Result from databroker verticle :: " + result);
            if (!result.containsKey(JSON_TYPE)) {
              promise.complete(result);
            } else {
              promise.fail(result.toString());
            }
          } else {
            promise.fail(handler.cause().getMessage());
          }
        });
    return promise.future();
  }

  @Override
  public Future<JsonObject> getAllAdapterDetailsForUser(
      JsonObject request, PostgresService postgresService) {
    LOGGER.debug("getAllAdapterDetailsForUser() started");
    Promise<JsonObject> promise = Promise.promise();

    StringBuilder selectIngestionQuery =
        new StringBuilder(SELECT_INGESTION_SQL.replace("$0", request.getString(PROVIDER_ID)));
    postgresService.executeQuery(
        selectIngestionQuery.toString(),
        postgresServiceHandler -> {
          if (postgresServiceHandler.succeeded()) {
            JsonObject result = postgresServiceHandler.result();
            promise.complete(result);
          } else {
            JsonObject pgFailResponseBuild = new JsonObject();
            pgFailResponseBuild.put(TYPE, 400);
            LOGGER.debug("cause " + postgresServiceHandler.cause());
            promise.fail(pgFailResponseBuild.toString());
          }
        });
    return promise.future();
  }

  private JsonObject generateResponse(JsonObject response) {
    JsonObject finalResponse = new JsonObject();
    int type = response.getInteger(JSON_TYPE);
    switch (type) {
      case 200:
        finalResponse
            .put(JSON_TYPE, type)
            .put(JSON_TITLE, ResponseType.fromCode(type).getMessage())
            .put(JSON_DETAIL, response.getString(JSON_DETAIL));
        break;
      case 400:
        finalResponse
            .put(JSON_TYPE, type)
            .put(JSON_TITLE, ResponseType.fromCode(type).getMessage())
            .put(JSON_DETAIL, response.getString(JSON_DETAIL));
        break;
      case 404:
        finalResponse
            .put(JSON_TYPE, type)
            .put(JSON_TITLE, ResponseType.fromCode(type).getMessage())
            .put(JSON_DETAIL, ResponseType.ResourceNotFound.getMessage());
        break;
      case 409:
        finalResponse
            .put(JSON_TYPE, type)
            .put(JSON_TITLE, ResponseType.fromCode(type).getMessage())
            .put(JSON_DETAIL, ResponseType.AlreadyExists.getMessage());
        break;
      default:
        finalResponse
            .put(JSON_TYPE, ResponseType.BadRequestData.getCode())
            .put(JSON_TITLE, ResponseType.BadRequestData.getMessage())
            .put(JSON_DETAIL, response.getString(JSON_DETAIL));
        break;
    }
    return finalResponse;
  }
}
