package iudx.resource.server.metering.util;

import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static iudx.resource.server.apiserver.util.Constants.RESPONSE_SIZE;
import static iudx.resource.server.metering.util.Constants.*;

public class QueryBuilder {

    private static final Logger LOGGER = LogManager.getLogger(QueryBuilder.class);

    public JsonObject buildMessageForWriteQuery(JsonObject request) {

        String primaryKey = UUID.randomUUID().toString().replace("-", "");
        String userId = request.getString(USER_ID);
        String resourceId = request.getString(ID);
        String providerID =
                resourceId.substring(0, resourceId.indexOf('/', resourceId.indexOf('/') + 1));


        request.put(PRIMARY_KEY,primaryKey);
        request.put(USER_ID,userId);
        request.put(PROVIDER_ID,providerID);
        request.put(ORIGIN,ORIGIN_SERVER);
        LOGGER.trace("Info: Request " + request);
        return request;
    }

    private long getEpochTime(ZonedDateTime time) {
        return time.toInstant().toEpochMilli();
    }

    public String buildReadQueryFromPG(JsonObject request) {
        String startTime = request.getString(START_TIME);
        String endTime = request.getString(END_TIME);
        String resourceId = request.getString(RESOURCE_ID);
        String userId = request.getString(USER_ID);
        String api = request.getString(API);
        String providerID = request.getString(PROVIDER_ID);
        String consumerID = request.getString(CONSUMER_ID);
        String databaseTableName = request.getString(TABLE_NAME);
        StringBuilder query;

        ZonedDateTime startZDT = ZonedDateTime.parse(startTime);
        ZonedDateTime endZDT = ZonedDateTime.parse(endTime);

        long fromTime = getEpochTime(startZDT);
        long toTime = getEpochTime(endZDT);

        if (providerID != null) {
            query =
                    new StringBuilder(
                            PROVIDERID_TIME_INTERVAL_READ_QUERY
                                    .replace("$0", databaseTableName)
                                    .replace("$1", Long.toString(fromTime))
                                    .replace("$2", Long.toString(toTime))
                                    .replace("$3", providerID));
        } else {
            query =
                    new StringBuilder(
                            CONSUMERID_TIME_INTERVAL_READ_QUERY
                                    .replace("$0", databaseTableName)
                                    .replace("$1", Long.toString(fromTime))
                                    .replace("$2", Long.toString(toTime))
                                    .replace("$3", userId));
        }
        return query.toString();
    }

    public String buildCountReadQueryFromPG(JsonObject request) {
        String startTime = request.getString(START_TIME);
        String endTime = request.getString(END_TIME);
        String resourceId = request.getString(RESOURCE_ID);
        String userId = request.getString(USER_ID);
        String api = request.getString(API);
        String providerID = request.getString(PROVIDER_ID);
        String consumerID = request.getString(CONSUMER_ID);
        String databaseTableName = request.getString(TABLE_NAME);
        StringBuilder query;

        ZonedDateTime startZDT = ZonedDateTime.parse(startTime);
        ZonedDateTime endZDT = ZonedDateTime.parse(endTime);

        long fromTime = getEpochTime(startZDT);
        long toTime = getEpochTime(endZDT);

        if (providerID != null) {
            query =
                    new StringBuilder(
                            PROVIDERID_TIME_INTERVAL_COUNT_QUERY
                                    .replace("$0", databaseTableName)
                                    .replace("$1", Long.toString(fromTime))
                                    .replace("$2", Long.toString(toTime))
                                    .replace("$3", providerID));
        } else {
            query =
                    new StringBuilder(
                            CONSUMERID_TIME_INTERVAL_COUNT_QUERY
                                    .replace("$0", databaseTableName)
                                    .replace("$1", Long.toString(fromTime))
                                    .replace("$2", Long.toString(toTime))
                                    .replace("$3", userId));
        }
        return query.toString();
    }
}
