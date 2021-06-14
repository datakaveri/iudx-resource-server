package iudx.resource.server.database.latest;

import static iudx.resource.server.database.archives.Constants.*;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.redislabs.modules.rejson.JReJSON;
import com.redislabs.modules.rejson.Path;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.database.archives.ResponseBuilder;

public class RedisClient {
    // private Redis redisClient;
    private ResponseBuilder responseBuilder;
    private static final Logger LOGGER = LogManager.getLogger(RedisClient.class);
    private Vertx vertx;
    private JReJSON client;

//    public RedisClient(Vertx vertx, String connectionString){
//        this.vertx = vertx;
//        //redisClient = Redis.createClient(vertx, connectionString);
//        //redis = RedisAPI.api(redisClient);

//    }

    /**
     * RedisClient - Redis JReJSON Client Low level wrapper.
     *
     * @param vertx Vertx Instance
     * @param ip    IP of the ElasticDB
     * @param port  Port of the ElasticDB
     */

    public RedisClient(Vertx vertx, String ip, int port) {
        this.vertx = vertx;
        this.client = new JReJSON(ip, port);
    }

    /**
     * searchAsync - Wrapper around Redis async search requests.
     *
     * @param key Redis Key
     * @param pathParam Path Parameter for Redis Nested JSON object
     * @param searchHandler JsonObject result {@link AsyncResult}
     */

    public RedisClient searchAsync(String key, String pathParam, Handler<AsyncResult<JsonObject>> searchHandler) {
        // using get command
        JsonArray response = new JsonArray();
        get(key, pathParam).onComplete(resultRedis -> {
            if (resultRedis.succeeded()) {
                LOGGER.debug("Key found!");
                JsonObject fromRedis = resultRedis.result();
                LOGGER.debug("Result from Redis: " + fromRedis);
                    response.add(fromRedis);
                    responseBuilder = new ResponseBuilder(SUCCESS).setTypeAndTitle(200).setMessage(response);
                    searchHandler.handle(Future.succeededFuture(responseBuilder.getResponse()));
            }
            else {
                LOGGER.error("Redis Error: " + resultRedis.cause());
                resultRedis.cause().printStackTrace();
                responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(204)
                        .setMessage(resultRedis.cause().getLocalizedMessage());
                searchHandler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
            }
        });

        // using Vertx get command
        // cannot be used with ReJSON since JSON.GET command is not supported

//        redis.get(".".concat(key).concat(".").concat(pathParam), responseAsyncResult -> {
//            if (responseAsyncResult.succeeded()){
//                LOGGER.debug("Successful request " + responseAsyncResult.result());
//                Response redisResponse = responseAsyncResult.result();
//                Object[] resources = redisResponse.stream().toArray();
//                for (Object r: resources) {
//                    response.add((JsonObject)r);
//                }
//                // two for loops
////                for (Object rR : redisResponse.toArray(new Response[0]) )
//                responseBuilder = new ResponseBuilder(SUCCESS).setTypeAndTitle(200).setMessage(response);
//                searchHandler.handle(Future.succeededFuture(responseBuilder.getResponse()));
//
//            } else {
//                LOGGER.error("Redis Error: " + responseAsyncResult.toString());
//                responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(500)
//                        .setMessage(REDIS_ERROR);
//                searchHandler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
//            }
//
//
//
//            // id
//
//            if (!".".equalsIgnoreCase(pathParam)) {
//                get(key, pathParam).onComplete(resultRedis -> {
//                    if (resultRedis.succeeded()) {
//                        LOGGER.debug("Key found!");
//                        JsonObject fromRedis = resultRedis.result();
//                        response.add(fromRedis);
//                        responseBuilder = new ResponseBuilder(SUCCESS).setTypeAndTitle(200).setMessage(response);
//                        searchHandler.handle(Future.succeededFuture(responseBuilder.getResponse()));
//                    }
//                });
//            }
//
//            // group
//
//            else {
//                get
//
//            }
//
//
//        });


        // using Native send command
        // Overriding send command to add JSON.GET

//        redis.send(new Command() {
//            @Override
//            public byte[] getBytes() {
//                return new byte[0];
//            }
//
//            @Override
//            public int getArity() {
//                return 0;
//            }
//
//            @Override
//            public boolean isMultiKey() {
//                return false;
//            }
//
//            @Override
//            public int getFirstKey() {
//                return 0;
//            }
//
//            @Override
//            public int getLastKey() {
//                return 0;
//            }
//
//            @Override
//            public int getInterval() {
//                return 0;
//            }
//
//            @Override
//            public boolean isKeyless() {
//                return false;
//            }
//
//            @Override
//            public boolean isReadOnly() {
//                return false;
//            }
//
//            @Override
//            public boolean isMovable() {
//                return false;
//            }
//
//            @Override
//            public boolean isVoid() {
//                return false;
//            }
//        }, key.concat(" .").concat(pathParam)).handle(redisResponseHandler);
        return this;
    }
    /**
     * get - makes sync Redis call asynchronous
     *
     * @param key Redis Key
     * returns Future Object with (JSON) result from Redis
     */

    public Future<JsonObject> get(String key) {
        return get(key, Path.ROOT_PATH.toString());
    }

    /**
     * get - makes sync Redis call asynchronous
     * overridden method to include path parameter
     * @param key Redis Key
     * @param path Redis Path parameter
     * returns Future Object with (JSON) result from Redis
     */

    public Future<JsonObject> get(String key, String path) {
        Promise<JsonObject> promise = Promise.promise();
        vertx.executeBlocking(getFromRedisHandler -> {
            JsonObject json = getFromRedis(key, path);
            if (json == null) {
                getFromRedisHandler.fail(ID_NOT_PRESENT);
            } else {
                getFromRedisHandler.complete(json);
            }
        }, resultHandler -> {
            if (resultHandler.succeeded()) {
                promise.complete((JsonObject) resultHandler.result());
            } else {
                promise.fail(resultHandler.cause());
            }
        });
        return promise.future();
    }

    /**
     * getFromRedis - wrapper around Redis JReJSON client get command
     * @param key Redis Key
     * @param path Redis Path parameter
     * returns (JsonObject) response from Redis
     */

    private JsonObject getFromRedis(String key, String path) {
        Map result = null;
        try {
            result = client.get(key, Map.class, new Path(path));
            if (result != null) {
                JsonObject res = new JsonObject(result);
                return res;
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

}
