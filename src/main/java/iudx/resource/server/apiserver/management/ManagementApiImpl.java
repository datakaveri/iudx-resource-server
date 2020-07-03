package iudx.resource.server.apiserver.management;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import iudx.resource.server.databroker.DataBrokerService;

/**
 * This class handles all DataBrokerService related interactions of API server.
 *
 */
public class ManagementApiImpl implements ManagementApi {

  private static final Logger LOGGER = LoggerFactory.getLogger(ManagementApiImpl.class);

  /**
   * {@inheritDoc}
   */
  @Override
  public Future<JsonObject> createExchange(JsonObject json, DataBrokerService databroker) {
    Promise<JsonObject> promise = Promise.promise();
    System.out.println("data broker ::: " + databroker);
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
        promise.fail(handler.cause().toString());
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
    json.put("exchangeName", exchangeid);
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
        promise.fail(handler.cause().toString());
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
    json.put("exchangeName", exchangeid);
    databroker.listExchangeSubscribers(json, handler -> {
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
        promise.fail(handler.cause().toString());
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
        if (!result.containsKey("type")) {
          promise.complete(result);
        } else {
          promise.fail(result.toString());
        }
      } else if (handler.failed()) {
        LOGGER.error(handler.cause());
        promise.fail(handler.cause().toString());
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
    json.put("queueName", queueId);
    databroker.deleteQueue(json, handler -> {
      if (handler.succeeded()) {
        JsonObject result = handler.result();
        LOGGER.info("Result from databroker verticle :: " + result);
        if (!result.containsKey("type")) {
          promise.complete(result);
        } else {
          promise.fail(result.toString());
        }
      } else {
        LOGGER.error(handler.cause());
        promise.fail(handler.cause().toString());
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
    json.put("queueName", queueId);
    databroker.listQueueSubscribers(json, handler -> {
      if (handler.succeeded()) {
        JsonObject result = handler.result();
        LOGGER.info("Result from databroker verticle :: " + result);
        if (!result.containsKey("type")) {
          promise.complete(result);
        } else {
          promise.fail(result.toString());
        }
      } else {
        LOGGER.error(handler.cause());
        promise.fail(handler.cause().toString());
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
        if (!result.containsKey("type")) {
          promise.complete(result);
        } else {
          promise.fail(result.toString());
        }
      } else {
        LOGGER.error(handler.cause());
        promise.fail(handler.cause().toString());
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
        if (!result.containsKey("type")) {
          promise.complete(result);
        } else {
          promise.fail(result.toString());
        }
      } else {
        LOGGER.error(handler.cause());
        promise.fail(handler.cause().toString());
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
        if (!result.containsKey("type")) {
          promise.complete(result);
        } else {
          promise.fail(result.toString());
        }
      } else {
        LOGGER.error(handler.cause());
        promise.fail(handler.cause().toString());
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
    json.put("vHost", vhostID);
    databroker.deletevHost(json, handler -> {
      if (handler.succeeded()) {
        JsonObject result = handler.result();
        LOGGER.info("Result from databroker verticle :: " + result);
        if (!result.containsKey("type")) {
          promise.complete(result);
        } else {
          promise.fail(result.toString());
        }
      } else {
        LOGGER.error(handler.cause());
        promise.fail(handler.cause().toString());
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
        if (!result.containsKey("type")) {
          promise.complete(result);
        } else {
          promise.fail(result.toString());
        }
      } else {
        promise.fail(handler.cause().toString());
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
    json.put("id", adapterId);
    databroker.deleteAdaptor(json, handler -> {
      if (handler.succeeded()) {
        JsonObject result = handler.result();
        LOGGER.info("Result from databroker verticle :: " + result);
        if (result.containsKey("id")) {
          promise.complete(result);
        } else {
          promise.fail(result.toString());
        }
      } else {
        promise.fail(handler.cause().toString());
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
    json.put("id", adapterId);
    databroker.listAdaptor(json, handler -> {
      if (handler.succeeded()) {
        JsonObject result = handler.result();
        LOGGER.info("Result from databroker verticle :: " + result);
        if (!result.containsKey("type")) {
          promise.complete(result);
        } else {
          promise.fail(result.toString());
        }
      } else {
        promise.fail(handler.cause().toString());
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
        if (result.containsKey("type") && result.getString("type").equalsIgnoreCase("success")) {
          promise.complete(result);
        } else {
          promise.fail(result.toString());
        }
      } else {
        promise.fail(handler.cause().toString());
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
        if (result.getString("type").equalsIgnoreCase("success")) {
          promise.complete(result);
        } else {
          promise.fail(result.toString());
        }
      } else {
        promise.fail(handler.cause().toString());
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
        if (result.getString("type").equalsIgnoreCase("success")) {
          promise.complete(result);
        } else {
          promise.fail(result.toString());
        }
      } else {
        promise.fail(handler.cause().toString());
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
        if (!result.containsKey("type")) {
          promise.complete(result);
        } else {
          promise.fail(result.toString());
        }
      } else {
        promise.fail(handler.cause().toString());
      }
    });
    return promise.future();
  }

}
