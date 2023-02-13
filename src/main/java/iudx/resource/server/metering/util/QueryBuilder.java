package iudx.resource.server.metering.util;

import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import static iudx.resource.server.apiserver.util.Constants.ENDT;
import static iudx.resource.server.apiserver.util.Constants.STARTT;
import static iudx.resource.server.authenticator.Constants.ROLE;
import static iudx.resource.server.metering.util.Constants.*;

public class QueryBuilder {

    private static final Logger LOGGER = LogManager.getLogger(QueryBuilder.class);
    StringBuilder monthQuery;
    long today;

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
        String startTime = request.getString(STARTT);
        String endTime = request.getString(ENDT);

        String current = ZonedDateTime.now().toString();
        LOGGER.debug("zone IST =" + ZonedDateTime.now());
        ZonedDateTime zonedDateTimeUTC = ZonedDateTime.parse(current);
        zonedDateTimeUTC = zonedDateTimeUTC.withZoneSameInstant(ZoneId.of("UTC"));
        LOGGER.debug("zonedDateTimeUTC UTC = " + zonedDateTimeUTC);
        LocalDateTime utcTime = zonedDateTimeUTC.toLocalDateTime();
        LOGGER.debug("UTCtime =" + utcTime);
        today = zonedDateTimeUTC.now().getDayOfMonth();

        String timeYearBack = utcTime.minusYears(1)
                .minusDays(today).plusDays(1).withHour(0).withMinute(0).withSecond(0).toString();
        LOGGER.debug("Year back =" + timeYearBack);

        if (startTime != null || endTime != null) {
            if (role.equalsIgnoreCase("admin")) {
                monthQuery =
                        new StringBuilder(MONTHLY_OVERVIEW_QUERY.concat(MONTHLY_OVERVIEW_GROUPBY)
                                .replace("$0", startTime)
                                .replace("$1", endTime));
            } else if (role.equalsIgnoreCase("consumer")) {
                String userId = request.getString(USER_ID);
                monthQuery =
                        new StringBuilder(MONTHLY_OVERVIEW_QUERY.concat(" and userid = '$2' ").concat(MONTHLY_OVERVIEW_GROUPBY)
                                .replace("$0", startTime)
                                .replace("$1", endTime)
                                .replace("$2", userId));
            } else if (role.equalsIgnoreCase("provider") || role.equalsIgnoreCase("delegate")) {
                String resourceId = request.getString(IID);
                String providerID =
                        resourceId.substring(0, resourceId.indexOf('/', resourceId.indexOf('/') + 1));
                LOGGER.debug("Provider =" + providerID);
                monthQuery =
                        new StringBuilder(MONTHLY_OVERVIEW_QUERY.concat(" and providerid = '$2' ").concat(MONTHLY_OVERVIEW_GROUPBY)
                                .replace("$0", startTime)
                                .replace("$1", endTime)
                                .replace("$2", providerID));
            }
        } else {
            if (role.equalsIgnoreCase("admin")) {
                monthQuery =
                        new StringBuilder(MONTHLY_OVERVIEW_QUERY.concat(MONTHLY_OVERVIEW_GROUPBY)
                                .replace("$0", timeYearBack)
                                .replace("$1", utcTime.toString()));
            } else if (role.equalsIgnoreCase("consumer")) {
                String userId = request.getString(USER_ID);
                monthQuery =
                        new StringBuilder(MONTHLY_OVERVIEW_QUERY.concat(" and userid = '$2' ").concat(MONTHLY_OVERVIEW_GROUPBY)
                                .replace("$0", timeYearBack)
                                .replace("$1", utcTime.toString())
                                .replace("$2", userId));
            } else if (role.equalsIgnoreCase("provider") || role.equalsIgnoreCase("delegate")) {
                String resourceId = request.getString(IID);
                String providerID =
                        resourceId.substring(0, resourceId.indexOf('/', resourceId.indexOf('/') + 1));
                LOGGER.debug("Provider =" + providerID);
                monthQuery =
                        new StringBuilder(MONTHLY_OVERVIEW_QUERY.concat(" and providerid = '$2' ").concat(MONTHLY_OVERVIEW_GROUPBY)
                                .replace("$0", timeYearBack)
                                .replace("$1", utcTime.toString())
                                .replace("$2", providerID));
            }
        }

        return monthQuery.toString();
    }
}
