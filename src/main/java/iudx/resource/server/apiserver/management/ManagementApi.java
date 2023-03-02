package iudx.resource.server.apiserver.management;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.cache.CacheService;
import iudx.resource.server.database.postgres.PostgresService;
import iudx.resource.server.databroker.DataBrokerService;

public interface ManagementApi {

  /**
   * create a exchange in rabbit MQ through DataBrokerService.
   *
   * @param json jsonObject containing exachange name
   * @param databroker DataBrokerService object
   * @return Future
   */
  Future<JsonObject> createExchange(JsonObject json, DataBrokerService databroker);

  /**
   * delete a exchange in rabbit MQ through DataBrokerService.
   *
   * @param exchangeid exchange id to be deleted
   * @param databroker DataBrokerService object
   * @return Future
   */
  Future<JsonObject> deleteExchange(String exchangeid, DataBrokerService databroker);

  /**
   * get exchange detail for exchange id passed.
   *
   * @param exchangeid exchange id
   * @param databroker DataBrokerService object
   * @return Future
   */
  Future<JsonObject> getExchangeDetails(String exchangeid, DataBrokerService databroker);

  /**
   * create a queue in rabbit MQ through DataBrokerService.
   *
   * @param jsonObject json containing queue name
   * @param databroker DataBrokerService object
   * @return Future
   */
  Future<JsonObject> createQueue(JsonObject jsonObject, DataBrokerService databroker);

  /**
   * delete a queue in rabbit MQ through databroker service.
   *
   * @param queueId queue id to be deleted
   * @param databroker DataBrokerService object
   * @return Future
   */
  Future<JsonObject> deleteQueue(String queueId, DataBrokerService databroker);

  /**
   * get queue details for queue id passes.
   *
   * @param queueId queue id
   * @param databroker DataBrokerService object
   * @return Future
   */
  Future<JsonObject> getQueueDetails(String queueId, DataBrokerService databroker);

  /**
   * bind a queue to exchange with passed entities as routing queue.
   *
   * @param json json containing queue,exchange and entities details
   * @param databroker DataBrokerService object
   * @return Future
   */
  Future<JsonObject> bindQueue2Exchange(JsonObject json, DataBrokerService databroker);

  /**
   * unbind a queue from exchange with passed entities as routing key.
   *
   * @param json json object containing queue,exchange and entities details
   * @param databroker DataBrokerService object
   * @return Future
   */
  Future<JsonObject> unbindQueue2Exchange(JsonObject json, DataBrokerService databroker);

  /**
   * create a vHost in rabbit MQ.
   *
   * @param json json containing vhost name
   * @param databroker DataBrokerService object
   * @return Future
   */
  Future<JsonObject> createVHost(JsonObject json, DataBrokerService databroker);

  /**
   * delete a vhost.
   *
   * @param vhostID vhost id to be passed
   * @param databroker DataBrokerService object
   * @return Future
   */
  Future<JsonObject> deleteVHost(String vhostID, DataBrokerService databroker);

  /**
   * register a adapter.
   *
   * @param json request json
   * @param dataBroker DataBrokerService object
   * @param cacheService CacheService object
   * @param postgresService PostgresService object
   * @return Future
   */
  Future<JsonObject> registerAdapter(JsonObject json, DataBrokerService dataBroker, CacheService cacheService, PostgresService postgresService);

  /**
   * delete a adapter.
   *
   * @param adapterId adapter id to be deleted
   * @param databroker DataBrokerService object
   * @return Future
   */
  Future<JsonObject> deleteAdapter(String adapterId, String userId, DataBrokerService databroker);

  /**
   * get adapter details.
   *
   * @param adapterId adapter id to get details
   * @param databroker DataBrokerService object
   * @return Future
   */
  Future<JsonObject> getAdapterDetails(String adapterId, DataBrokerService databroker);

  /**
   * publish heartbeat data.
   *
   * @param json request json
   * @param databroker DataBrokerService object
   * @return Future
   */
  Future<JsonObject> publishHeartbeat(JsonObject json, DataBrokerService databroker);

  /**
   * publish downstream issues.
   *
   * @param json request json
   * @param databroker DataBrokerService object
   * @return Future
   */
  Future<JsonObject> publishDownstreamIssues(JsonObject json, DataBrokerService databroker);

  /**
   * publish data issue.
   *
   * @param json request json
   * @param databroker DataBrokerService object
   * @return Future
   */
  Future<JsonObject> publishDataIssue(JsonObject json, DataBrokerService databroker);

  /**
   * publish data from adapter.
   *
   * @param json request json
   * @param databroker DataBrokerService object
   * @return Future
   */
  Future<JsonObject> publishDataFromAdapter(JsonObject json, DataBrokerService databroker);

  /**
   * publish all adapter from exchange.
   *
   * @param json request json
   * @param postgresService PostgresService object
   * @return Future
   */
  Future<JsonObject> publishAllAdapterForUser(JsonObject json, PostgresService postgresService);

}
