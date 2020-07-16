package iudx.resource.server.apiserver.subscription;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.util.Constants;
import iudx.resource.server.database.DatabaseService;
import iudx.resource.server.databroker.DataBrokerService;

public class SubscriptionService {

  Subscription subscription = null;

  private Subscription getSubscriptionContext(String type, DataBrokerService databroker,
      DatabaseService databaseService) {
    if (type != null && type.equalsIgnoreCase(SubsType.CALLBACK.getMessage())) {
      return new CallbackSuscription(databroker, databaseService);
    } else {
      return new StreamingSubscription(databroker, databaseService);
    }
  }

  public Future<JsonObject> createSubscription(JsonObject json, DataBrokerService databroker,
      DatabaseService databaseService) {
    subscription = getSubscriptionContext(json.getString(Constants.JSON_TYPE), databroker,
        databaseService);
    assertNotNull(subscription);
    return subscription.create(json);
  }

  public Future<JsonObject> updateSubscription(JsonObject json, DataBrokerService databroker,
      DatabaseService databaseService) {
    subscription = getSubscriptionContext(json.getString(Constants.JSON_TYPE), databroker,
        databaseService);
    assertNotNull(subscription);
    return subscription.update(json);
  }

  public Future<JsonObject> deleteSubscription(JsonObject json, DataBrokerService databroker,
      DatabaseService databaseService) {
    subscription = getSubscriptionContext(json.getString(Constants.JSON_TYPE), databroker,
        databaseService);
    assertNotNull(subscription);
    return subscription.delete(json);
  }

  public Future<JsonObject> getSubscription(JsonObject json, DataBrokerService databroker,
      DatabaseService databaseService) {
    subscription = getSubscriptionContext(json.getString(Constants.JSON_TYPE), databroker,
        databaseService);
    assertNotNull(subscription);
    return subscription.get(json);
  }

}
