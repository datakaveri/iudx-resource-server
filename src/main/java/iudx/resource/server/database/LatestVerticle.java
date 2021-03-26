package iudx.resource.server.database;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import static iudx.resource.server.database.Constants.*;

public class LatestVerticle extends AbstractVerticle {


    /**
     * The Latest Verticle.
     * <h1>Latest Verticle</h1>
     * <p>
     * The Database Verticle implementation in the the IUDX Resource Server exposes the
     * {@link iudx.resource.server.database.DatabaseService} over the Vert.x Event Bus.
     * </p>
     *
     * @version 1.0
     * @since 2020-05-31
     */
    private LatestDataService latestData;
    private RedisClient redisClient;
    private String redisHost;
    private String redisUser;
    private String password;
    private JsonObject attributeList;
    private int port;
    private ServiceBinder binder;
    private MessageConsumer<JsonObject> consumer;
    private String connectionString;


    /**
         * This method is used to start the Verticle. It deploys a verticle in a cluster, registers the
         * service with the Event bus against an address, publishes the service with the service discovery
         * interface.
         *
         * @throws Exception which is a start up exception.
         */

        @Override
        public void start() throws Exception {

            /** config to read the Redis credentials
             * IP
             * redisUser
             * password
             * port
             * */
            redisHost = config().getString("redisHost");
            port = config().getInteger("redisPort");
            redisUser = config().getString("redisUser");
            password = config().getString("redisPassword");
            attributeList = config().getJsonObject("attributeList");
            //connectionString = "redis://:".concat(redisUser).concat(":").concat(password).concat("@").concat(redisHost)
              //      .concat(":").concat(String.valueOf(port));
            // connectionString = "redis://:@https://database.iudx.io:28734/1";
            // System.out.println("RedisConnectionString: " + connectionString);
            // redisClient = new RedisClient(vertx, connectionString);
            redisClient = new RedisClient(vertx, redisHost, port);
            binder = new ServiceBinder(vertx);
            latestData = new LatestDataServiceImpl(redisClient, attributeList);

            consumer =
                    binder.setAddress(LATEST_DATA_SERVICE_ADDRESS)
                            .register(LatestDataService.class, latestData);
        }

        @Override
        public void stop() {
            binder.unregister(consumer);
        }
}

