package iudx.resource.server.async;

import com.amazonaws.regions.Regions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.resource.server.database.archives.S3FileOpsHelper;
import iudx.resource.server.database.archives.elastic.ElasticClient;
import iudx.resource.server.database.postgres.PostgresService;

import static iudx.resource.server.common.Constants.ASYNC_SERVICE_ADDRESS;
import static iudx.resource.server.common.Constants.PG_SERVICE_ADDRESS;

/**
 * The Async worker Verticle.
 *
 * <h1>Async Verticle</h1>
 *
 * <p>The Async Verticle implementation in the IUDX Resource Server exposes the {@link
 * iudx.resource.server.async.AsyncService} over the Vert.x Event Bus.
 *
 * @version 1.0
 * @since 2022-02-08
 */
public class AsyncVerticle extends AbstractVerticle {

  private AsyncService asyncService;
  private ElasticClient client;
  private PostgresService pgService;
  private S3FileOpsHelper fileOpsHelper;
  private Regions clientRegion;
  private String databaseIP;
  private String user;
  private String password;
  private String timeLimit;
  private int databasePort;
  private String filePath;
  private String bucketName;
  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, registers the
   * service with the Event Bus against an address, publishes the service with the service discovery
   * interface.
   *
   * @throws Exception which is a start up exception.
   */
  @Override
  public void start() throws Exception {

    databaseIP = config().getString("databaseIP");
    databasePort = config().getInteger("databasePort");
    user = config().getString("dbUser");
    password = config().getString("dbPassword");
    timeLimit = config().getString("timeLimit");
    filePath = config().getString("filePath");
    clientRegion = Regions.AP_SOUTH_1;
    bucketName = config().getString("bucketName");

    client = new ElasticClient(databaseIP, databasePort, user, password, filePath);
    pgService = PostgresService.createProxy(vertx, PG_SERVICE_ADDRESS);
    fileOpsHelper = new S3FileOpsHelper(clientRegion, bucketName);
    binder = new ServiceBinder(vertx);
    asyncService = new AsyncServiceImpl(client, pgService, fileOpsHelper, timeLimit, filePath);

    consumer = binder.setAddress(ASYNC_SERVICE_ADDRESS).register(AsyncService.class, asyncService);
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
