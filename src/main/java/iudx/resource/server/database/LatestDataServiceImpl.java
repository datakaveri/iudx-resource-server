package iudx.resource.server.database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.resource.server.database.Constants.*;
import static iudx.resource.server.database.Constants.EMPTY_RESOURCE_ID;

/**
 * The LatestData Service Implementation.
 * <h1>LatestData Service Implementation</h1>
 * <p>
 * The LatestData Service implementation in the IUDX Resource Server implements the definitions of the
 * {@link iudx.resource.server.database.LatestDataService}.
 * </p>
 *
 * @version 1.0
 * @since 2021-03-26
 */

public class LatestDataServiceImpl implements LatestDataService{

    RedisClient redisClient;
    private ResponseBuilder responseBuilder;
    JsonObject attributeList;
    private static final Logger LOGGER = LogManager.getLogger(DatabaseServiceImpl.class);
    // private RedisAPI redisAPI;
    private QueryDecoder decoder = new QueryDecoder();
    private JsonObject query;

    public LatestDataServiceImpl(RedisClient client, JsonObject attributeList) {
        this.redisClient = client;
        this.attributeList = attributeList;
    }

    /**
     * Performs a Latest search query using the Redis JReJSON client.
     *
     * @param request Json object received from the ApiServerVerticle
     * @param handler Handler to return redis response in case of success and appropriate error
     *        message in case of failure
     */

    @Override
    public LatestDataService getLatestData(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

        request.put(ATTRIBUTE_LIST, attributeList);

        // Exceptions
        if (!request.containsKey(ID)) {
            LOGGER.debug("Info: " + ID_NOT_FOUND);
            responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(ID_NOT_FOUND);
            handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
            return null;
        }

        if (request.getJsonArray(ID).isEmpty()) {
            LOGGER.debug("Info: " + EMPTY_RESOURCE_ID);
            responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400)
                    .setMessage(EMPTY_RESOURCE_ID);
            handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
            return null;
        }

//        if (!request.containsKey(OPTIONS)) {
//            LOGGER.debug("Info: " + OPTIONS_NOT_FOUND);
//            responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(OPTIONS_NOT_FOUND);
//            handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
//            return null;
//        }
//
//        if (request.getString(OPTIONS).isEmpty()) {
//            LOGGER.debug("Info: " + EMPTY_OPTIONS);
//            responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400)
//                    .setMessage(EMPTY_OPTIONS);
//            handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
//            return null;
//        }

        // options has to be equal to group or id

//        if (!GROUP.equalsIgnoreCase(request.getString(OPTIONS)) && !ID.equalsIgnoreCase(request.getString(OPTIONS))) {
//            LOGGER.debug("Info: " + EMPTY_OPTIONS);
//            responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400)
//                    .setMessage(INVALID_OPTIONS);
//            handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
//            return null;
//        }

        /** Reusing QueryDecoder
         * TODO: can use the QueryDecoder module to throw another request concurrently
         * to ES to ensure better data consistency between cache and DB
         * query = {key: rg, pathParam: pathParam} for Redis command.
         */

        LOGGER.debug("<LatestDataServiceImpl---> "+request);
        query = decoder.queryDecoder(request);
        if (query.containsKey(ERROR)) {
            LOGGER.error("Fail: Query returned with an error: " + query.getString(ERROR));
            responseBuilder =
                    new ResponseBuilder(FAILED).setTypeAndTitle(400)
                            .setMessage(query.getString(ERROR));
            handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
            return null;
        }

        LOGGER.debug("Info: Query constructed: " + query.toString());
        redisClient.searchAsync(query.getString(KEY), query.getString(PATH_PARAM), searchRes -> {
            if (searchRes.succeeded()) {
                LOGGER.debug("Success: Successful Redis request");
                handler.handle(Future.succeededFuture(searchRes.result()));
            } else {
                LOGGER.error("Fail: Redis Cache Request;" + searchRes.cause().getMessage());
                handler.handle(Future.failedFuture(searchRes.cause().getMessage()));
            }
        });

        return null;
    }
}
