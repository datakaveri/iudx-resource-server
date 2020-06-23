package iudx.resource.server.apiserver.management;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public interface ManagementApi {

  public Future<JsonObject> createExchange(JsonObject json);

  public Future<JsonObject> deleteExchange(String exchangeid);

  public Future<JsonObject> getExchangeDetails(String exchangeid);

  public Future<JsonObject> createQueue(JsonObject jsonObject);

  public Future<JsonObject> deleteQueue(String queueId);

  public Future<JsonObject> getQueueDetails(String queueId);

}
