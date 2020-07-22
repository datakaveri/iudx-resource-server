package iudx.resource.server.apiserver.subscription;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import iudx.resource.server.apiserver.util.Constants;
import iudx.resource.server.database.DatabaseService;
import iudx.resource.server.databroker.DataBrokerService;

public class SubscriptionService {
  
  private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionService.class);

  Subscription subscription = null;

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

  public Future<JsonObject> createSubscription(JsonObject json, DataBrokerService databroker,
      DatabaseService databaseService) {
    LOGGER.info("createSubscription() method started");
    subscription = getSubscriptionContext(json.getString(Constants.JSON_TYPE), databroker,
        databaseService);
    assertNotNull(subscription);
    return subscription.create(json);
  }

  public Future<JsonObject> updateSubscription(JsonObject json, DataBrokerService databroker,
      DatabaseService databaseService) {
    LOGGER.info("updateSubscription() method started");
    subscription = getSubscriptionContext(json.getString(Constants.JSON_TYPE), databroker,
        databaseService);
    assertNotNull(subscription);
    return subscription.update(json);
  }

  public Future<JsonObject> deleteSubscription(JsonObject json, DataBrokerService databroker,
      DatabaseService databaseService) {
    LOGGER.info("deleteSubscription() method started");
    subscription = getSubscriptionContext(json.getString(Constants.JSON_TYPE), databroker,
        databaseService);
    assertNotNull(subscription);
    return subscription.delete(json);
  }

  public Future<JsonObject> getSubscription(JsonObject json, DataBrokerService databroker,
      DatabaseService databaseService) {
    LOGGER.info("getSubscription() method started");
    subscription = getSubscriptionContext(json.getString(Constants.JSON_TYPE), databroker,
        databaseService);
    assertNotNull(subscription);
    return subscription.get(json);
  }

  public Future<JsonObject> appendSubscription(JsonObject json, DataBrokerService databroker,
      DatabaseService databaseService) {
    LOGGER.info("appendSubscription() method started");
    subscription =
        getSubscriptionContext(json.getString(Constants.JSON_TYPE), databroker, databaseService);
    assertNotNull(subscription);
    return subscription.append(json);
  }
}
