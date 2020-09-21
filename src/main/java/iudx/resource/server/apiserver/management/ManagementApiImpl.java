package iudx.resource.server.apiserver.management;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import static iudx.resource.server.apiserver.util.Constants.JSON_DETAIL;
import static iudx.resource.server.apiserver.util.Constants.JSON_TITLE;
import static iudx.resource.server.apiserver.util.Constants.JSON_TYPE;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import iudx.resource.server.apiserver.response.ResponseType;
import iudx.resource.server.apiserver.util.Constants;
import iudx.resource.server.databroker.DataBrokerService;

/**
 * This class handles all DataBrokerService related interactions of API server.
 * TODO Need to add documentation.
 */
public class ManagementApiImpl implements ManagementApi {

  private static final Logger LOGGER = LogManager.getLogger(ManagementApiImpl.class);

  /**
   * {@inheritDoc}
   */
  @Override
  public Future<JsonObject> createExchange(JsonObject json, DataBrokerService databroker) {
    Promise<JsonObject> promise = Promise.promise();
    LOGGER.info("data broker ::: " + databroker);
    databroker.createExchange(json, handler -> {
      if (handler.succeeded()) {
        JsonObject result = handler.result();
        LOGGER.info("Result from databroker verticle :: " + result);
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

  /**
   * {@inheritDoc}
   */
  @Override
  public Future<JsonObject> deleteExchange(String exchangeid, DataBrokerService databroker) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject json = new JsonObject();
    json.put(Constants.JSON_EXCHANGE_NAME, exchangeid);
    databroker.deleteExchange(json, handler -> {
      if (handler.succeeded()) {
        JsonObject result = handler.result();
        LOGGER.info("Result from databroker verticle :: " + result);
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

  /**
   * {@inheritDoc}
   */
  @Override
  public Future<JsonObject> getExchangeDetails(String exchangeid, DataBrokerService databroker) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject json = new JsonObject();
    // TOOD : discuss about DataBrokerService json exchangeName changed to id that's why test case
    // is failing
    json.put(Constants.JSON_EXCHANGE_NAME, exchangeid);
    json.put("id", exchangeid);
    databroker.listExchangeSubscribers(json, handler -> {
      if (handler.succeeded()) {
        JsonObject result = handler.result();
        LOGGER.info("Result from databroker verticle :: " + result);
        if (!result.containsKey(Constants.JSON_TYPE)) {
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

  /**
   * {@inheritDoc}
   */
  @Override
  public Future<JsonObject> createQueue(JsonObject json, DataBrokerService databroker) {
    Promise<JsonObject> promise = Promise.promise();
    databroker.createQueue(json, handler -> {
      if (handler.succeeded()) {
        JsonObject result = handler.result();
        LOGGER.info("Result from databroker verticle :: " + result);
        if (!result.containsKey(Constants.JSON_TYPE)) {
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

  /**
   * {@inheritDoc}
   */
  @Override
  public Future<JsonObject> deleteQueue(String queueId, DataBrokerService databroker) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject json = new JsonObject();
    json.put(Constants.JSON_QUEUE_NAME, queueId);
    databroker.deleteQueue(json, handler -> {
      if (handler.succeeded()) {
        JsonObject result = handler.result();
        LOGGER.info("Result from databroker verticle :: " + result);
        if (!result.containsKey(Constants.JSON_TYPE)) {
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

  /**
   * {@inheritDoc}
   */
  @Override
  public Future<JsonObject> getQueueDetails(String queueId, DataBrokerService databroker) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject json = new JsonObject();
    json.put(Constants.JSON_QUEUE_NAME, queueId);
    databroker.listQueueSubscribers(json, handler -> {
      if (handler.succeeded()) {
        JsonObject result = handler.result();
        LOGGER.info("Result from databroker verticle :: " + result);
        if (!result.containsKey(Constants.JSON_TYPE)) {
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

  /**
   * {@inheritDoc}
   */
  @Override
  public Future<JsonObject> bindQueue2Exchange(JsonObject json, DataBrokerService databroker) {
    Promise<JsonObject> promise = Promise.promise();
    databroker.bindQueue(json, handler -> {
      if (handler.succeeded()) {
        JsonObject result = handler.result();
        LOGGER.info("Result from databroker verticle :: " + result);
        if (!result.containsKey(Constants.JSON_TYPE)) {
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

  /**
   * {@inheritDoc}
   */
  @Override
  public Future<JsonObject> unbindQueue2Exchange(JsonObject json, DataBrokerService databroker) {
    Promise<JsonObject> promise = Promise.promise();
    LOGGER.info("unbind request :: " + json);
    databroker.unbindQueue(json, handler -> {
      if (handler.succeeded()) {
        JsonObject result = handler.result();
        LOGGER.info("Result from databroker verticle :: " + result);
        if (!result.containsKey(Constants.JSON_TYPE)) {
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

  /**
   * {@inheritDoc}
   */
  @Override
  public Future<JsonObject> createVHost(JsonObject json, DataBrokerService databroker) {
    Promise<JsonObject> promise = Promise.promise();
    databroker.createvHost(json, handler -> {
      if (handler.succeeded()) {
        JsonObject result = handler.result();
        LOGGER.info("Result from databroker verticle :: " + result);
        if (!result.containsKey(Constants.JSON_TYPE)) {
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

  /**
   * {@inheritDoc}
   */
  @Override
  public Future<JsonObject> deleteVHost(String vhostID, DataBrokerService databroker) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject json = new JsonObject();
    json.put(Constants.JSON_VHOST, vhostID);
    databroker.deletevHost(json, handler -> {
      if (handler.succeeded()) {
        JsonObject result = handler.result();
        LOGGER.info("Result from databroker verticle :: " + result);
        if (!result.containsKey(Constants.JSON_TYPE)) {
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

  /**
   * {@inheritDoc}
   */
  @Override
  public Future<JsonObject> registerAdapter(JsonObject json, DataBrokerService databroker) {
    Promise<JsonObject> promise = Promise.promise();
    databroker.registerAdaptor(json, handler -> {
      if (handler.succeeded()) {
        JsonObject result = handler.result();
        LOGGER.info("Result from databroker verticle :: " + result);
        if (!result.containsKey(Constants.JSON_TYPE)) {
          promise.complete(result);
        } else {
          promise.fail(generateResponse(result).toString());
        }
      } else {
        promise.fail(handler.cause().getMessage());
      }
    });
    return promise.future();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Future<JsonObject> deleteAdapter(String adapterId, DataBrokerService databroker) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject json = new JsonObject();
    json.put(Constants.JSON_ID, adapterId);
    databroker.deleteAdaptor(json, handler -> {
      if (handler.succeeded()) {
        JsonObject result = handler.result();
        LOGGER.info("Result from databroker verticle :: " + result);
         promise.complete(generateResponse(result));
      } else if (handler.failed()) {
        JsonObject result = handler.result();
        promise.fail(generateResponse(result).toString());
      }
    });
    return promise.future();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Future<JsonObject> getAdapterDetails(String adapterId, DataBrokerService databroker) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject json = new JsonObject();
    json.put(Constants.JSON_ID, adapterId);
    databroker.listAdaptor(json, handler -> {
      if (handler.succeeded()) {
        JsonObject result = handler.result();
        LOGGER.info("Result from databroker verticle :: " + result);
        if (!result.containsKey(Constants.JSON_TYPE)) {
          promise.complete(result);
        } else {
          promise.fail(generateResponse(result).toString());
        }
      } else {
        promise.fail(handler.cause().getMessage());
      }
    });
    return promise.future();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Future<JsonObject> publishHeartbeat(JsonObject json, DataBrokerService databroker) {
    Promise<JsonObject> promise = Promise.promise();
    databroker.publishHeartbeat(json, handler -> {
      if (handler.succeeded()) {
        JsonObject result = new JsonObject();
        LOGGER.info("Result from databroker verticle :: " + result);
        if (result.containsKey(Constants.JSON_TYPE)
            && result.getString(Constants.JSON_TYPE).equalsIgnoreCase(Constants.SUCCCESS)) {
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

  /**
   * {@inheritDoc}
   */
  @Override
  public Future<JsonObject> publishDownstreamIssues(JsonObject json, DataBrokerService databroker) {
    Promise<JsonObject> promise = Promise.promise();
    databroker.publishHeartbeat(json, handler -> {
      if (handler.succeeded()) {
        JsonObject result = new JsonObject();
        LOGGER.info("Result from databroker verticle :: " + result);
        if (result.getString(Constants.JSON_TYPE).equalsIgnoreCase(Constants.SUCCCESS)) {
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

  /**
   * {@inheritDoc}
   */
  @Override
  public Future<JsonObject> publishDataIssue(JsonObject json, DataBrokerService databroker) {
    Promise<JsonObject> promise = Promise.promise();
    databroker.publishHeartbeat(json, handler -> {
      if (handler.succeeded()) {
        JsonObject result = new JsonObject();
        LOGGER.info("Result from databroker verticle :: " + result);
        if (result.getString(Constants.JSON_TYPE).equalsIgnoreCase(Constants.SUCCCESS)) {
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

  /**
   * {@inheritDoc}
   */
  @Override
  public Future<JsonObject> publishDataFromAdapter(JsonObject json, DataBrokerService databroker) {
    Promise<JsonObject> promise = Promise.promise();
    databroker.publishFromAdaptor(json, handler -> {
      if (handler.succeeded()) {
        JsonObject result = new JsonObject();
        LOGGER.info("Result from databroker verticle :: " + result);
        if (!result.containsKey(Constants.JSON_TYPE)) {
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

  private JsonObject generateResponse(JsonObject response) {
    JsonObject finalResponse = new JsonObject();
    int type = response.getInteger(JSON_TYPE);
    switch (type) {
      case 200: {
        finalResponse.put(JSON_TYPE, type)
            .put(JSON_TITLE, ResponseType.fromCode(type).getMessage())
            .put(JSON_DETAIL, response.getString(JSON_DETAIL));
        break;
      }
      case 400: {
        finalResponse.put(JSON_TYPE, type)
            .put(JSON_TITLE, ResponseType.fromCode(type).getMessage())
            .put(JSON_DETAIL, response.getString(JSON_DETAIL));
        break;
      }
      case 404: {
        finalResponse.put(JSON_TYPE, type)
            .put(JSON_TITLE, ResponseType.fromCode(type).getMessage())
            .put(JSON_DETAIL, ResponseType.ResourceNotFound.getMessage());
        break;
      }
      case 409: {
        finalResponse.put(JSON_TYPE, type)
            .put(JSON_TITLE, ResponseType.fromCode(type).getMessage())
            .put(JSON_DETAIL, ResponseType.AlreadyExists.getMessage());
        break;
      }
      default: {
        finalResponse.put(JSON_TYPE, ResponseType.BadRequestData.getCode())
            .put(JSON_TITLE, ResponseType.BadRequestData.getMessage())
            .put(JSON_DETAIL, response.getString(JSON_DETAIL));
        break;
      }
    }
    return finalResponse;
  }

}
