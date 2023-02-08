package iudx.resource.server.metering.util;

import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.exceptions.DxRuntimeException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.UUID;

import static iudx.resource.server.apiserver.util.Constants.ENDT;
import static iudx.resource.server.apiserver.util.Constants.STARTT;
import static iudx.resource.server.authenticator.Constants.ROLE;
import static iudx.resource.server.metering.util.Constants.*;

public class QueryBuilder {

    private static final Logger LOGGER = LogManager.getLogger(QueryBuilder.class);
    StringBuilder monthQuery;

    public JsonObject buildMessageForRMQ(JsonObject request) {

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

    public String buildMonthlyOverview(JsonObject request) {
        String role = request.getString(ROLE);

        LocalDate currentDate
                = LocalDate.parse(LocalDate.now().toString());
        int day = currentDate.getDayOfMonth();
        String today = String.valueOf(day);
        LOGGER.info(day);

        String startTime = request.getString(STARTT);
        String endTime = request.getString(ENDT);
        if (startTime != null || endTime != null) {
            String splitStartTime = startTime.split("T")[0];
            String splitEndTime = endTime.split("T")[0];
            String replace = MONTHLY_OVERVIEW_ADMIN_WITH_ST_ET
                    .replace("$0", splitStartTime)
                    .replace("$1", splitEndTime);
            if (role.equalsIgnoreCase("admin")) {
                monthQuery =
                        new StringBuilder(replace);
            } else if (role.equalsIgnoreCase("consumer")) {
                String userId = request.getString(USER_ID);
                monthQuery =
                        new StringBuilder(MONTHLY_OVERVIEW_CONSUMER_WITH_ST_ET
                                .replace("$0", splitStartTime)
                                .replace("$1", splitEndTime)
                                .replace("$2", userId));
            } else if (role.equalsIgnoreCase("provider") || role.equalsIgnoreCase("delegate")) {
                String resourceId = request.getString(IID);
                String providerID =
                        resourceId.substring(0, resourceId.indexOf('/', resourceId.indexOf('/') + 1));
                LOGGER.debug("Provider =" + providerID);
                monthQuery =
                        new StringBuilder(MONTHLY_OVERVIEW_PROVIDERID_WITH_ST_ET
                                .replace("$0", splitStartTime)
                                .replace("$1", splitEndTime)
                                .replace("$2", providerID));
            }
        } else {
            if (role.equalsIgnoreCase("admin")) {//TODO: Decorator or template
                monthQuery =
                        new StringBuilder(MONTHLY_OVERVIEW_ADMIN_WITHOUT_ST_ET
                                .replace("$0", today));
            } else if (role.equalsIgnoreCase("consumer")) {
                String userId = request.getString(USER_ID);
                monthQuery =
                        new StringBuilder(MONTHLY_OVERVIEW_CONSUMER_WITHOUT_ST_ET
                                .replace("$0", today)
                                .replace("$1", userId));
            } else if (role.equalsIgnoreCase("provider") || role.equalsIgnoreCase("delegate")) {
                String resourceId = request.getString(IID);
                String providerID =
                        resourceId.substring(0, resourceId.indexOf('/', resourceId.indexOf('/') + 1));
                LOGGER.debug("Provider =" + providerID);
                monthQuery =
                        new StringBuilder(MONTHLY_OVERVIEW_PROVIDERID_WITHOUT_ST_ET
                                .replace("$0", today)
                                .replace("$1", providerID));
            }
        }
        return monthQuery.toString();
    }
}
