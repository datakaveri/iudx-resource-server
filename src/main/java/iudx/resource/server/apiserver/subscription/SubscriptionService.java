package iudx.resource.server.apiserver.subscription;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import iudx.resource.server.apiserver.util.Constants;
import iudx.resource.server.database.DatabaseService;
import iudx.resource.server.databroker.DataBrokerService;

/**
 * class contains all method for operations on subscriptions.
 *
 */
public class SubscriptionService {
  
  private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionService.class);

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
      return new CallbackSubscription(databroker, databaseService);
    } else {
      LOGGER.info("streaming subscription context");
      return new StreamingSubscription(databroker, databaseService);
    }
  }

  /**
   * create a subscription
   * 
   * @param json subscription json
   * @param databroker databroker verticle object
   * @param databaseService database verticle object
   * @return a future of JsonObject
   */
  public Future<JsonObject> createSubscription(JsonObject json, DataBrokerService databroker,
      DatabaseService databaseService) {
    LOGGER.info("createSubscription() method started");
    subscription = getSubscriptionContext(json.getString(Constants.JSON_TYPE), databroker,
        databaseService);
    assertNotNull(subscription);
    return subscription.create(json);
  }

  /**
   * update an existing subscription
   * 
   * @param json subscription json
   * @param databroker databroker verticle object
   * @param databaseService database verticle object
   * @return a future of josbObject
   */
  public Future<JsonObject> updateSubscription(JsonObject json, DataBrokerService databroker,
      DatabaseService databaseService) {
    LOGGER.info("updateSubscription() method started");
    subscription = getSubscriptionContext(json.getString(Constants.JSON_TYPE), databroker,
        databaseService);
    assertNotNull(subscription);
    return subscription.update(json);
  }

  /**
   * delete a subscription
   * 
   * @param json subscription json
   * @param databroker databroker verticle object
   * @param databaseService database verticle object
   * @return a future of josbObject
   */
  public Future<JsonObject> deleteSubscription(JsonObject json, DataBrokerService databroker,
      DatabaseService databaseService) {
    LOGGER.info("deleteSubscription() method started");
    subscription = getSubscriptionContext(json.getString(Constants.JSON_TYPE), databroker,
        databaseService);
    assertNotNull(subscription);
    return subscription.delete(json);
  }

  /**
   * get a subscription
   * 
   * @param json subscription json
   * @param databroker databroker verticle object
   * @param databaseService database verticle object
   * @return a future of josbObject
   */
  public Future<JsonObject> getSubscription(JsonObject json, DataBrokerService databroker,
      DatabaseService databaseService) {
    LOGGER.info("getSubscription() method started");
    subscription = getSubscriptionContext(json.getString(Constants.JSON_TYPE), databroker,
        databaseService);
    assertNotNull(subscription);
    return subscription.get(json);
  }

  /**
   * append an existing subscription
   * 
   * @param json subscription json
   * @param databroker databroker verticle object
   * @param databaseService database verticle object
   * @return a future of josbObject
   */
  public Future<JsonObject> appendSubscription(JsonObject json, DataBrokerService databroker,
      DatabaseService databaseService) {
    LOGGER.info("appendSubscription() method started");
    subscription =
        getSubscriptionContext(json.getString(Constants.JSON_TYPE), databroker, databaseService);
    assertNotNull(subscription);
    return subscription.append(json);
  }
}
