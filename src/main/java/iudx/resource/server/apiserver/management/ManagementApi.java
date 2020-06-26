package iudx.resource.server.apiserver.management;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.databroker.DataBrokerService;

public interface ManagementApi {

  /**
   * create a exchange in rabbit MQ through DataBrokerService.
   * 
   * @param json       jsonObject containing exachange name
   * @param databroker DataBrokerService object
   * @return Future
   */
  public Future<JsonObject> createExchange(JsonObject json, DataBrokerService databroker);

  /**
   * delete a exchange in rabbit MQ through DataBrokerService.
   * 
   * @param exchangeid exchange id to be deleted
   * @param databroker DataBrokerService object
   * @return Future
   */
  public Future<JsonObject> deleteExchange(String exchangeid, DataBrokerService databroker);

  /**
   * get exchange detail for exchange id passed.
   * 
   * @param exchangeid exchange id
   * @param databroker DataBrokerService object
   * @return Future
   */
  public Future<JsonObject> getExchangeDetails(String exchangeid, DataBrokerService databroker);

  /**
   * create a queue in rabbit MQ through DataBrokerService.
   * 
   * @param jsonObject json containing queue name
   * @param databroker DataBrokerService object
   * @return Future
   */
  public Future<JsonObject> createQueue(JsonObject jsonObject, DataBrokerService databroker);

  /**
   * delete a queue in rabbit MQ through databroker service.
   * 
   * @param queueId    queue id to be deleted
   * @param databroker DataBrokerService object
   * @return Future
   */
  public Future<JsonObject> deleteQueue(String queueId, DataBrokerService databroker);

  /**
   * get queue details for queue id passes.
   * 
   * @param queueId    queue id
   * @param databroker DataBrokerService object
   * @return Future
   */
  public Future<JsonObject> getQueueDetails(String queueId, DataBrokerService databroker);

  /**
   * bind a queue to exchange with passed entities as routing queue.
   * 
   * @param json       json containing queue,exchange and entities details
   * @param databroker DataBrokerService object
   * @return Future
   */
  public Future<JsonObject> bindQueue2Exchange(JsonObject json, DataBrokerService databroker);

  /**
   * unbind a queue from exchange with passed entities as routing key.
   * 
   * @param json       json object containing queue,exchange and entities details
   * @param databroker DataBrokerService object
   * @return Future
   */
  public Future<JsonObject> unbindQueue2Exchange(JsonObject json, DataBrokerService databroker);

  /**
   * create a vHost in rabbit MQ.
   * 
   * @param json       json containing vhost name
   * @param databroker DataBrokerService object
   * @return Future
   */
  public Future<JsonObject> createVHost(JsonObject json, DataBrokerService databroker);

  /**
   * delete a vhost.
   * 
   * @param vhostID    vhost id to be passed
   * @param databroker DataBrokerService object
   * @return Future
   */
  public Future<JsonObject> deleteVHost(String vhostID, DataBrokerService databroker);

}
