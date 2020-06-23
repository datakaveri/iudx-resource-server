package iudx.resource.server.apiserver.management;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.databroker.DataBrokerService;

public interface ManagementApi {

  public Future<JsonObject> createExchange(JsonObject json, DataBrokerService databroker);

  public Future<JsonObject> deleteExchange(String exchangeid, DataBrokerService databroker);

  public Future<JsonObject> getExchangeDetails(String exchangeid, DataBrokerService databroker);

  public Future<JsonObject> createQueue(JsonObject jsonObject, DataBrokerService databroker);

  public Future<JsonObject> deleteQueue(String queueId, DataBrokerService databroker);

  public Future<JsonObject> getQueueDetails(String queueId, DataBrokerService databroker);

}
