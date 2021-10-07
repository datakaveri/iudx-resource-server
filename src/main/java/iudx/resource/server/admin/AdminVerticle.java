package iudx.resource.server.admin;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.sqlclient.PoolOptions;
import iudx.resource.server.databroker.PostgresClient;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class AdminVerticle extends AbstractVerticle {

  private static final Logger logger = LogManager.getLogger(AdminVerticle.class);
  private static final String ADMIN_SERVICE_ADDRESS = "iudx.rs.admin.service";

  private ServiceBinder binder;
  private AdminServiceImpl adminService;
  private MessageConsumer<JsonObject> consumer;
  private PostgresClient postgresClient;
  private String databaseIP;
  private int databasePort;
  private String databaseName;
  private String databaseUserName;
  private String databasePassword;
  private int poolSize;
  private String audience;
  private String issuer;
  private PoolOptions poolOptions;
  private PgConnectOptions connectOptions;

  @Override
  public void start() throws Exception {

    /* Read the configurations and set the database properties */
    databaseIP = config().getString("callbackDatabaseIP");
    databasePort = config().getInteger("callbackDatabasePort");
    databaseName = config().getString("callbackDatabaseName");
    databaseUserName = config().getString("callbackDatabaseUserName");
    databasePassword = config().getString("callbackDatabasePassword");
    poolSize = config().getInteger("callbackpoolSize");
    audience = config().getString("jwtAudience");
    issuer = config().getString("jwtIssuer");

    /* Set Connection Object */
    if (connectOptions == null) {
      connectOptions = new PgConnectOptions().setPort(databasePort).setHost(databaseIP)
          .setDatabase(databaseName).setUser(databaseUserName).setPassword(databasePassword);
    }

    /* Pool options */
    if (poolOptions == null) {
      poolOptions = new PoolOptions().setMaxSize(poolSize);
    }

    /* Call the admin service constructor with the Postgres client. */

    postgresClient = new PostgresClient(vertx, connectOptions, poolOptions);

    binder = new ServiceBinder(vertx);
    adminService = new AdminServiceImpl(postgresClient);

    consumer =
        binder.setAddress(ADMIN_SERVICE_ADDRESS)
            .register(AdminService.class, adminService);
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
