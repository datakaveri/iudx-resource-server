package iudx.resource.server.apiserver.management;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import iudx.resource.server.databroker.DataBrokerService;

public class ManagementApiImpl implements ManagementApi {

  private static final Logger LOGGER = LoggerFactory.getLogger(ManagementApiImpl.class);
  private DataBrokerService databroker;

  public ManagementApiImpl(DataBrokerService databroker) {
    this.databroker = databroker;
  }

  @Override
  public Future<JsonObject> createExchange(JsonObject json) {
    Promise<JsonObject> promise = Promise.promise();
    System.out.println("data broker ::: " + databroker);
    databroker.createExchange(json, handler -> {
      if (handler.succeeded()) {
        JsonObject result = handler.result();
        if (result.containsKey("exchange")) {
          promise.complete(result);
        } else {
          promise.fail(result.toString());
        }
      } else if (handler.failed()) {
        promise.fail(handler.cause().toString());
      }
    });
    return promise.future();
  }

  @Override
  public Future<JsonObject> deleteExchange(String exchangeid) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject json = new JsonObject();
    json.put("exchangeName", exchangeid);
    databroker.deleteExchange(json, handler -> {
      if (handler.succeeded()) {
        JsonObject result = handler.result();
        if (result.containsKey("exchange")) {
          promise.complete(result);
        } else {
          promise.fail(result.toString());
        }
      } else if (handler.failed()) {
        promise.fail(handler.cause().toString());
      }
    });
    return promise.future();
  }

  @Override
  public Future<JsonObject> getExchangeDetails(String exchangeid) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject json = new JsonObject();
    json.put("exchangeName", exchangeid);
    databroker.listExchangeSubscribers(json, handler -> {
      if (handler.succeeded()) {
        JsonObject result = handler.result();
        if (result.containsKey("exchange")) {
          promise.complete(result);
        } else {
          promise.fail(result.toString());
        }
      } else if (handler.failed()) {
        promise.fail(handler.cause().toString());
      }
    });
    return promise.future();
  }

  @Override
  public Future<JsonObject> createQueue(JsonObject json) {
    Promise<JsonObject> promise = Promise.promise();
    databroker.createQueue(json, handler -> {
      if (handler.succeeded()) {
        JsonObject result = handler.result();
        if (result.containsKey("queue")) {
          promise.complete(result);
        } else {
          promise.fail(result.toString());
        }
      } else if (handler.failed()) {
        promise.fail(handler.cause().toString());
      }
    });
    return promise.future();
  }

  @Override
  public Future<JsonObject> deleteQueue(String queueId) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject json = new JsonObject();
    json.put("queueName", queueId);
    databroker.deleteQueue(json, handler -> {
      if (handler.succeeded()) {
        JsonObject result = handler.result();
        if (result.containsKey("queue")) {
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

  @Override
  public Future<JsonObject> getQueueDetails(String queueId) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject json = new JsonObject();
    json.put("queueName", queueId);
    databroker.listQueueSubscribers(json, handler -> {
      if (handler.succeeded()) {
        JsonObject result = handler.result();
        if (result.containsKey("queue")) {
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
