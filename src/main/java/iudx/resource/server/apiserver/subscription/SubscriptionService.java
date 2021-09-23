package iudx.resource.server.apiserver.subscription;

import static iudx.resource.server.apiserver.util.Constants.JSON_DETAIL;
import static iudx.resource.server.apiserver.util.Constants.JSON_TITLE;
import static iudx.resource.server.apiserver.util.Constants.JSON_TYPE;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.response.ResponseType;
import iudx.resource.server.apiserver.util.Constants;
import iudx.resource.server.database.archives.DatabaseService;
import iudx.resource.server.databroker.DataBrokerService;

/**
 * class contains all method for operations on subscriptions.
 *
 */
public class SubscriptionService {

  private static final Logger LOGGER = LogManager.getLogger(SubscriptionService.class);

  Subscription subscription = null;

  /**
   * get the context of subscription according to the type passed in message body.
   * 
   * @param type type of subscription either <strong>streaming</strong> or <strong>callback</strong>
   * @param databroker databroker verticle object
   * @param databaseService database verticle object
   * @return an object of Subscription class
   */
  private Subscription getSubscriptionContext(String type, DataBrokerService databroker,
      DatabaseService databaseService) {
    LOGGER.info("getSubscriptionContext() method started");
    if (type != null && type.equalsIgnoreCase(SubsType.CALLBACK.getMessage())) {
      LOGGER.info("callback subscription context");
      return CallbackSubscription.getInstance(databroker, databaseService);
    } else {
      LOGGER.info("streaming subscription context");
      return StreamingSubscription.getInstance(databroker, databaseService);
    }
  }

  /**
   * create a subscription.
   * 
   * @param json subscription json
   * @param databroker databroker verticle object
   * @param databaseService database verticle object
   * @return a future of JsonObject
   */
  public Future<JsonObject> createSubscription(JsonObject json, DataBrokerService databroker,
      DatabaseService databaseService) {
    LOGGER.info("createSubscription() method started");
    Promise<JsonObject> promise = Promise.promise();
    subscription =
        getSubscriptionContext(json.getString(Constants.SUB_TYPE), databroker, databaseService);
    assertNotNull(subscription);
    subscription.create(json).onComplete(handler -> {
      if (handler.succeeded()) {
        promise.complete(handler.result());
      } else {
        JsonObject res = new JsonObject(handler.cause().getMessage());
        promise.fail(generateResponse(res).toString());
      }
    });
    return promise.future();
  }

  /**
   * update an existing subscription.
   * 
   * @param json subscription json
   * @param databroker databroker verticle object
   * @param databaseService database verticle object
   * @return a future of josbObject
   */
  public Future<JsonObject> updateSubscription(JsonObject json, DataBrokerService databroker,
      DatabaseService databaseService) {
    LOGGER.info("updateSubscription() method started");
    Promise<JsonObject> promise = Promise.promise();
    subscription =
        getSubscriptionContext(json.getString(Constants.SUB_TYPE), databroker, databaseService);
    assertNotNull(subscription);
    subscription.update(json).onComplete(handler -> {
      if (handler.succeeded()) {
        promise.complete(handler.result());
      } else {
        JsonObject res = new JsonObject(handler.cause().getMessage());
        promise.fail(generateResponse(res).toString());
      }
    });
    return promise.future();
  }

  /**
   * delete a subscription.
   * 
   * @param json subscription json
   * @param databroker databroker verticle object
   * @param databaseService database verticle object
   * @return a future of josbObject
   */
  public Future<JsonObject> deleteSubscription(JsonObject json, DataBrokerService databroker,
      DatabaseService databaseService) {
    LOGGER.info("deleteSubscription() method started");
    Promise<JsonObject> promise = Promise.promise();
    subscription =
        getSubscriptionContext(json.getString(Constants.SUB_TYPE), databroker, databaseService);
    assertNotNull(subscription);
    subscription.delete(json).onComplete(handler -> {
      if (handler.succeeded()) {
        promise.complete(handler.result());
      } else {
        JsonObject res = new JsonObject(handler.cause().getMessage());
        promise.fail(generateResponse(res).toString());
      }
    });
    return promise.future();
  }

  /**
   * get a subscription.
   * 
   * @param json subscription json
   * @param databroker databroker verticle object
   * @param databaseService database verticle object
   * @return a future of josbObject
   */
  public Future<JsonObject> getSubscription(JsonObject json, DataBrokerService databroker,
      DatabaseService databaseService) {
    LOGGER.info("getSubscription() method started");
    Promise<JsonObject> promise = Promise.promise();
    subscription =
        getSubscriptionContext(json.getString(Constants.SUB_TYPE), databroker, databaseService);
    assertNotNull(subscription);
    subscription.get(json).onComplete(handler -> {
      if (handler.succeeded()) {
        promise.complete(handler.result());
      } else {
        JsonObject res = new JsonObject(handler.cause().getMessage());
        promise.fail(generateResponse(res).toString());
      }
    });
    return promise.future();
  }

  /**
   * append an existing subscription.
   * 
   * @param json subscription json
   * @param databroker databroker verticle object
   * @param databaseService database verticle object
   * @return a future of josbObject
   */
  public Future<JsonObject> appendSubscription(JsonObject json, DataBrokerService databroker,
      DatabaseService databaseService) {
    LOGGER.info("appendSubscription() method started");
    Promise<JsonObject> promise = Promise.promise();
    subscription =
        getSubscriptionContext(json.getString(Constants.SUB_TYPE), databroker, databaseService);
    assertNotNull(subscription);
    subscription.append(json).onComplete(handler -> {
      if (handler.succeeded()) {
        promise.complete(handler.result());
      } else {
        JsonObject res = new JsonObject(handler.cause().getMessage());
        promise.fail(generateResponse(res).toString());
      }
    });
    return promise.future();
  }

  private JsonObject generateResponse(JsonObject response) {
    JsonObject finalResponse = new JsonObject();
    int type = response.getInteger(JSON_TYPE);
    String title=response.getString(JSON_TITLE);
    switch(type) {
      case 400:{
        finalResponse.put(JSON_TYPE, type)
            .put(JSON_TITLE, ResponseType.fromCode(type).getMessage())
            .put(JSON_DETAIL, response.getString(JSON_DETAIL));
        break;
      }
      case 404:{
        finalResponse.put(JSON_TYPE, type)
            .put(JSON_TITLE, ResponseType.fromCode(type).getMessage())
            .put(JSON_DETAIL, ResponseType.ResourceNotFound.getMessage());
        break;
      }
      case 409: {
        finalResponse.put(JSON_TYPE, type)
            .put(JSON_TITLE,  ResponseType.fromCode(type).getMessage())
            .put(JSON_DETAIL, "Subscription " + ResponseType.AlreadyExists.getMessage());
        break;
      }
      default: {
        finalResponse.put(JSON_TYPE, type)
            .put(JSON_TITLE, ResponseType.BadRequestData.getMessage())
            .put(JSON_DETAIL, response.getString(JSON_DETAIL));
        break;
      }
    }
    return finalResponse;
  }
}
