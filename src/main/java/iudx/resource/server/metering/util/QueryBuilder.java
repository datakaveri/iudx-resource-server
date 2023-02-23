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
        StringBuilder query = null;
        String limit = request.getString("limit");
        String offset = request.getString("offset");

        ZonedDateTime startZDT = ZonedDateTime.parse(startTime);
        ZonedDateTime endZDT = ZonedDateTime.parse(endTime);

        long fromTime = getEpochTime(startZDT);
        long toTime = getEpochTime(endZDT);

        if (limit!=null && offset!=null){
                if (providerID != null) {
                    if (api != null && resourceId != null && consumerID != null) {
                        query =
                                new StringBuilder(
                                        PROVIDERID_TIME_INTERVAL_READ_QUERY.concat(" and api = '$4' and resourceid = '$5' and userid = '$6'")
                                                .concat(ORDER_BY).concat(OFFSET).concat(LIMIT)
                                                .replace("$0", databaseTableName)
                                                .replace("$1", Long.toString(fromTime))
                                                .replace("$2", Long.toString(toTime))
                                                .replace("$3", providerID)
                                                .replace("$4", api)
                                                .replace("$5", resourceId)
                                                .replace("$6", consumerID)
                                                .replace("$7", limit)
                                                .replace("$8", offset));
                    } else if (api != null && resourceId != null) {
                        query =
                                new StringBuilder(
                                        PROVIDERID_TIME_INTERVAL_READ_QUERY.concat(" and api = '$4' and resourceid = '$5'").concat(ORDER_BY)
                                                .concat(OFFSET).concat(LIMIT)
                                                .replace("$0", databaseTableName)
                                                .replace("$1", Long.toString(fromTime))
                                                .replace("$2", Long.toString(toTime))
                                                .replace("$3", providerID)
                                                .replace("$4", api)
                                                .replace("$5", resourceId)
                                                .replace("$7", limit)
                                                .replace("$8", offset));
                    } else if (api != null && consumerID != null) {
                        query =
                                new StringBuilder(
                                        PROVIDERID_TIME_INTERVAL_READ_QUERY.concat(" and api = '$4' and userid = '$5'")
                                                .concat(ORDER_BY).concat(OFFSET).concat(LIMIT)
                                                .replace("$0", databaseTableName)
                                                .replace("$1", Long.toString(fromTime))
                                                .replace("$2", Long.toString(toTime))
                                                .replace("$3", providerID)
                                                .replace("$4", api)
                                                .replace("$5", consumerID)
                                                .replace("$7", limit)
                                                .replace("$8", offset));
                    } else if (resourceId != null && consumerID != null) {
                        query =
                                new StringBuilder(
                                        PROVIDERID_TIME_INTERVAL_READ_QUERY.concat(" and resourceid = '$4' and userid = '$5'")
                                                .concat(ORDER_BY).concat(OFFSET).concat(LIMIT)
                                                .replace("$0", databaseTableName)
                                                .replace("$1", Long.toString(fromTime))
                                                .replace("$2", Long.toString(toTime))
                                                .replace("$3", providerID)
                                                .replace("$4", resourceId)
                                                .replace("$5", consumerID)
                                                .replace("$7", limit)
                                                .replace("$8", offset));
                    } else if (api != null) {
                        query =
                                new StringBuilder(
                                        PROVIDERID_TIME_INTERVAL_READ_QUERY.concat(" and api = '$4'")
                                                .concat(ORDER_BY).concat(OFFSET).concat(LIMIT)
                                                .replace("$0", databaseTableName)
                                                .replace("$1", Long.toString(fromTime))
                                                .replace("$2", Long.toString(toTime))
                                                .replace("$3", providerID)
                                                .replace("$4", api)
                                                .replace("$7", limit)
                                                .replace("$8", offset));
                    } else if (resourceId != null) {
                        query =
                                new StringBuilder(
                                        PROVIDERID_TIME_INTERVAL_READ_QUERY.concat(" and resourceid = '$4'")
                                                .concat(ORDER_BY).concat(OFFSET).concat(LIMIT)
                                                .replace("$0", databaseTableName)
                                                .replace("$1", Long.toString(fromTime))
                                                .replace("$2", Long.toString(toTime))
                                                .replace("$3", providerID)
                                                .replace("$4", resourceId)
                                                .replace("$7", limit)
                                                .replace("$8", offset));
                    } else if (consumerID != null) {
                        query =
                                new StringBuilder(
                                        PROVIDERID_TIME_INTERVAL_READ_QUERY.concat(" and userid = '$4'")
                                                .concat(ORDER_BY).concat(OFFSET).concat(LIMIT)
                                                .replace("$0", databaseTableName)
                                                .replace("$1", Long.toString(fromTime))
                                                .replace("$2", Long.toString(toTime))
                                                .replace("$3", providerID)
                                                .replace("$4", consumerID)
                                                .replace("$7", limit)
                                                .replace("$8", offset));
                    } else {
                        query =
                                new StringBuilder(
                                        PROVIDERID_TIME_INTERVAL_READ_QUERY.concat(ORDER_BY)
                                                .concat(OFFSET).concat(LIMIT)
                                                .replace("$0", databaseTableName)
                                                .replace("$1", Long.toString(fromTime))
                                                .replace("$2", Long.toString(toTime))
                                                .replace("$3", providerID)
                                                .replace("$7", limit)
                                                .replace("$8", offset));
                    }
                } else {
                    if (api != null && resourceId != null) {
                        query =
                                new StringBuilder(
                                        CONSUMERID_TIME_INTERVAL_READ_QUERY.concat(" and api = '$4' and resourceid = '$5'")
                                                .concat(ORDER_BY).concat(OFFSET).concat(LIMIT)
                                                .replace("$0", databaseTableName)
                                                .replace("$1", Long.toString(fromTime))
                                                .replace("$2", Long.toString(toTime))
                                                .replace("$3", userId)
                                                .replace("$4", api)
                                                .replace("$5", resourceId)
                                                .replace("$7", limit)
                                                .replace("$8", offset));
                    } else if (api != null) {
                        query =
                                new StringBuilder(
                                        CONSUMERID_TIME_INTERVAL_READ_QUERY.concat(" and api = '$4'").concat(ORDER_BY)
                                                .concat(OFFSET).concat(LIMIT)
                                                .replace("$0", databaseTableName)
                                                .replace("$1", Long.toString(fromTime))
                                                .replace("$2", Long.toString(toTime))
                                                .replace("$3", userId)
                                                .replace("$4", api)
                                                .replace("$7", limit)
                                                .replace("$8", offset));
                    } else if (resourceId != null) {
                        query =
                                new StringBuilder(
                                        CONSUMERID_TIME_INTERVAL_READ_QUERY.concat(" and resourceid = '$4'")
                                                .concat(ORDER_BY).concat(OFFSET).concat(LIMIT)
                                                .replace("$0", databaseTableName)
                                                .replace("$1", Long.toString(fromTime))
                                                .replace("$2", Long.toString(toTime))
                                                .replace("$3", userId)
                                                .replace("$4", resourceId)
                                                .replace("$7", limit)
                                                .replace("$8", offset));
                    } else {
                        query =
                                new StringBuilder(
                                        CONSUMERID_TIME_INTERVAL_READ_QUERY.concat(ORDER_BY).concat(OFFSET)
                                                .concat(LIMIT)
                                                .replace("$0", databaseTableName)
                                                .replace("$1", Long.toString(fromTime))
                                                .replace("$2", Long.toString(toTime))
                                                .replace("$3", userId)
                                                .replace("$7", limit)
                                                .replace("$8", offset));
                    }
                }
        } else if (limit!=null) {
            if (providerID != null) {
                if (api != null && resourceId != null && consumerID != null) {
                    query =
                            new StringBuilder(
                                    PROVIDERID_TIME_INTERVAL_READ_QUERY.concat(" and api = '$4' and resourceid = '$5' and userid = '$6'")
                                            .concat(ORDER_BY).concat(LIMIT)
                                            .replace("$0", databaseTableName)
                                            .replace("$1", Long.toString(fromTime))
                                            .replace("$2", Long.toString(toTime))
                                            .replace("$3", providerID)
                                            .replace("$4", api)
                                            .replace("$5", resourceId)
                                            .replace("$6", consumerID)
                                            .replace("$7", limit));
                } else if (api != null && resourceId != null) {
                    query =
                            new StringBuilder(
                                    PROVIDERID_TIME_INTERVAL_READ_QUERY.concat(" and api = '$4' and resourceid = '$5'")
                                            .concat(ORDER_BY).concat(LIMIT)
                                            .replace("$0", databaseTableName)
                                            .replace("$1", Long.toString(fromTime))
                                            .replace("$2", Long.toString(toTime))
                                            .replace("$3", providerID)
                                            .replace("$4", api)
                                            .replace("$5", resourceId)
                                            .replace("$7", limit));
                } else if (api != null && consumerID != null) {
                    query =
                            new StringBuilder(
                                    PROVIDERID_TIME_INTERVAL_READ_QUERY.concat(" and api = '$4' and userid = '$5'")
                                            .concat(ORDER_BY).concat(LIMIT)
                                            .replace("$0", databaseTableName)
                                            .replace("$1", Long.toString(fromTime))
                                            .replace("$2", Long.toString(toTime))
                                            .replace("$3", providerID)
                                            .replace("$4", api)
                                            .replace("$5", consumerID)
                                            .replace("$7", limit));
                } else if (resourceId != null && consumerID != null) {
                    query =
                            new StringBuilder(
                                    PROVIDERID_TIME_INTERVAL_READ_QUERY.concat(" and resourceid = '$4' and userid = '$5'")
                                            .concat(ORDER_BY).concat(LIMIT)
                                            .replace("$0", databaseTableName)
                                            .replace("$1", Long.toString(fromTime))
                                            .replace("$2", Long.toString(toTime))
                                            .replace("$3", providerID)
                                            .replace("$4", resourceId)
                                            .replace("$5", consumerID)
                                            .replace("$7", limit));
                } else if (api != null) {
                    query =
                            new StringBuilder(
                                    PROVIDERID_TIME_INTERVAL_READ_QUERY.concat(" and api = '$4'")
                                            .concat(ORDER_BY).concat(LIMIT)
                                            .replace("$0", databaseTableName)
                                            .replace("$1", Long.toString(fromTime))
                                            .replace("$2", Long.toString(toTime))
                                            .replace("$3", providerID)
                                            .replace("$4", api)
                                            .replace("$7", limit));
                } else if (resourceId != null) {
                    query =
                            new StringBuilder(
                                    PROVIDERID_TIME_INTERVAL_READ_QUERY.concat(" and resourceid = '$4'")
                                            .concat(ORDER_BY).concat(LIMIT)
                                            .replace("$0", databaseTableName)
                                            .replace("$1", Long.toString(fromTime))
                                            .replace("$2", Long.toString(toTime))
                                            .replace("$3", providerID)
                                            .replace("$4", resourceId)
                                            .replace("$7", limit));
                } else if (consumerID != null) {
                    query =
                            new StringBuilder(
                                    PROVIDERID_TIME_INTERVAL_READ_QUERY.concat(" and userid = '$4'")
                                            .concat(ORDER_BY).concat(LIMIT)
                                            .replace("$0", databaseTableName)
                                            .replace("$1", Long.toString(fromTime))
                                            .replace("$2", Long.toString(toTime))
                                            .replace("$3", providerID)
                                            .replace("$4", consumerID)
                                            .replace("$7", limit));
                } else {
                    query =
                            new StringBuilder(
                                    PROVIDERID_TIME_INTERVAL_READ_QUERY.concat(ORDER_BY).concat(LIMIT)
                                            .replace("$0", databaseTableName)
                                            .replace("$1", Long.toString(fromTime))
                                            .replace("$2", Long.toString(toTime))
                                            .replace("$3", providerID)
                                            .replace("$7", limit));
                }
            } else {
                if (api != null && resourceId != null) {
                    query =
                            new StringBuilder(
                                    CONSUMERID_TIME_INTERVAL_READ_QUERY.concat(" and api = '$4' and resourceid = '$5'")
                                            .concat(ORDER_BY).concat(LIMIT)
                                            .replace("$0", databaseTableName)
                                            .replace("$1", Long.toString(fromTime))
                                            .replace("$2", Long.toString(toTime))
                                            .replace("$3", userId)
                                            .replace("$4", api)
                                            .replace("$5", resourceId)
                                            .replace("$7", limit));
                } else if (api != null) {
                    query =
                            new StringBuilder(
                                    CONSUMERID_TIME_INTERVAL_READ_QUERY.concat(" and api = '$4'").concat(ORDER_BY).concat(LIMIT)
                                            .replace("$0", databaseTableName)
                                            .replace("$1", Long.toString(fromTime))
                                            .replace("$2", Long.toString(toTime))
                                            .replace("$3", userId)
                                            .replace("$4", api)
                                            .replace("$7", limit));
                } else if (resourceId != null) {
                    query =
                            new StringBuilder(
                                    CONSUMERID_TIME_INTERVAL_READ_QUERY.concat(" and resourceid = '$4'").concat(ORDER_BY).concat(LIMIT)
                                            .replace("$0", databaseTableName)
                                            .replace("$1", Long.toString(fromTime))
                                            .replace("$2", Long.toString(toTime))
                                            .replace("$3", userId)
                                            .replace("$4", resourceId)
                                            .replace("$7", limit));
                } else {
                    query =
                            new StringBuilder(
                                    CONSUMERID_TIME_INTERVAL_READ_QUERY.concat(ORDER_BY).concat(LIMIT)
                                            .replace("$0", databaseTableName)
                                            .replace("$1", Long.toString(fromTime))
                                            .replace("$2", Long.toString(toTime))
                                            .replace("$3", userId)
                                            .replace("$7", limit));
                }
            }
        }else {
            if (providerID != null) {
                if (api != null && resourceId != null && consumerID != null) {
                    query =
                            new StringBuilder(
                                    PROVIDERID_TIME_INTERVAL_READ_QUERY.concat(" and api = '$4' and resourceid = '$5' and userid = '$6'").concat(ORDER_BY)
                                            .replace("$0", databaseTableName)
                                            .replace("$1", Long.toString(fromTime))
                                            .replace("$2", Long.toString(toTime))
                                            .replace("$3", providerID)
                                            .replace("$4", api)
                                            .replace("$5", resourceId)
                                            .replace("$6", consumerID));
                } else if (api != null && resourceId != null) {
                    query =
                            new StringBuilder(
                                    PROVIDERID_TIME_INTERVAL_READ_QUERY.concat(" and api = '$4' and resourceid = '$5'").concat(ORDER_BY)
                                            .replace("$0", databaseTableName)
                                            .replace("$1", Long.toString(fromTime))
                                            .replace("$2", Long.toString(toTime))
                                            .replace("$3", providerID)
                                            .replace("$4", api)
                                            .replace("$5", resourceId));
                } else if (api != null && consumerID != null) {
                    query =
                            new StringBuilder(
                                    PROVIDERID_TIME_INTERVAL_READ_QUERY.concat(" and api = '$4' and userid = '$5'").concat(ORDER_BY)
                                            .replace("$0", databaseTableName)
                                            .replace("$1", Long.toString(fromTime))
                                            .replace("$2", Long.toString(toTime))
                                            .replace("$3", providerID)
                                            .replace("$4", api)
                                            .replace("$5", consumerID));
                } else if (resourceId != null && consumerID != null) {
                    query =
                            new StringBuilder(
                                    PROVIDERID_TIME_INTERVAL_READ_QUERY.concat(" and resourceid = '$4' and userid = '$5'").concat(ORDER_BY)
                                            .replace("$0", databaseTableName)
                                            .replace("$1", Long.toString(fromTime))
                                            .replace("$2", Long.toString(toTime))
                                            .replace("$3", providerID)
                                            .replace("$4", resourceId)
                                            .replace("$5", consumerID));
                } else if (api != null) {
                    query =
                            new StringBuilder(
                                    PROVIDERID_TIME_INTERVAL_READ_QUERY.concat(" and api = '$4'").concat(ORDER_BY)
                                            .replace("$0", databaseTableName)
                                            .replace("$1", Long.toString(fromTime))
                                            .replace("$2", Long.toString(toTime))
                                            .replace("$3", providerID)
                                            .replace("$4", api));
                } else if (resourceId != null) {
                    query =
                            new StringBuilder(
                                    PROVIDERID_TIME_INTERVAL_READ_QUERY.concat(" and resourceid = '$4'").concat(ORDER_BY)
                                            .replace("$0", databaseTableName)
                                            .replace("$1", Long.toString(fromTime))
                                            .replace("$2", Long.toString(toTime))
                                            .replace("$3", providerID)
                                            .replace("$4", resourceId));
                } else if (consumerID != null) {
                    query =
                            new StringBuilder(
                                    PROVIDERID_TIME_INTERVAL_READ_QUERY.concat(" and userid = '$4'").concat(ORDER_BY)
                                            .replace("$0", databaseTableName)
                                            .replace("$1", Long.toString(fromTime))
                                            .replace("$2", Long.toString(toTime))
                                            .replace("$3", providerID)
                                            .replace("$4", consumerID));
                } else {
                    query =
                            new StringBuilder(
                                    PROVIDERID_TIME_INTERVAL_READ_QUERY.concat(ORDER_BY)
                                            .replace("$0", databaseTableName)
                                            .replace("$1", Long.toString(fromTime))
                                            .replace("$2", Long.toString(toTime))
                                            .replace("$3", providerID));
                }
            } else {
                if (api != null && resourceId != null) {
                    query =
                            new StringBuilder(
                                    CONSUMERID_TIME_INTERVAL_READ_QUERY.concat(" and api = '$4' and resourceid = '$5'").concat(ORDER_BY)
                                            .replace("$0", databaseTableName)
                                            .replace("$1", Long.toString(fromTime))
                                            .replace("$2", Long.toString(toTime))
                                            .replace("$3", userId)
                                            .replace("$4", api)
                                            .replace("$5", resourceId));
                } else if (api != null) {
                    query =
                            new StringBuilder(
                                    CONSUMERID_TIME_INTERVAL_READ_QUERY.concat(" and api = '$4'").concat(ORDER_BY)
                                            .replace("$0", databaseTableName)
                                            .replace("$1", Long.toString(fromTime))
                                            .replace("$2", Long.toString(toTime))
                                            .replace("$3", userId)
                                            .replace("$4", api));
                } else if (resourceId != null) {
                    query =
                            new StringBuilder(
                                    CONSUMERID_TIME_INTERVAL_READ_QUERY.concat(" and resourceid = '$4'").concat(ORDER_BY)
                                            .replace("$0", databaseTableName)
                                            .replace("$1", Long.toString(fromTime))
                                            .replace("$2", Long.toString(toTime))
                                            .replace("$3", userId)
                                            .replace("$4", resourceId));
                } else {
                    query =
                            new StringBuilder(
                                    CONSUMERID_TIME_INTERVAL_READ_QUERY.concat(ORDER_BY)
                                            .replace("$0", databaseTableName)
                                            .replace("$1", Long.toString(fromTime))
                                            .replace("$2", Long.toString(toTime))
                                            .replace("$3", userId));
                }
            }
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
        StringBuilder query = null;

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
            ZonedDateTime timeSeries = ZonedDateTime.parse(startTime);
            String timeSeriesToFirstDay = String.valueOf(timeSeries.withDayOfMonth(1));
            LOGGER.debug("Time series = "+ timeSeriesToFirstDay);
            if (role.equalsIgnoreCase("admin")) {
                monthQuery =
                        new StringBuilder(OVERVIEW_QUERY.concat(GROUPBY)
                                .replace("$0", timeSeriesToFirstDay)
                                .replace("$1", endTime)
                                .replace("$2", startTime)
                                .replace("$3", endTime));
            } else if (role.equalsIgnoreCase("consumer")) {
                String userId = request.getString(USER_ID);
                monthQuery =
                        new StringBuilder(OVERVIEW_QUERY.concat(" and userid = '$4' ").concat(GROUPBY)
                                .replace("$0", timeSeriesToFirstDay)
                                .replace("$1", endTime)
                                .replace("$2", startTime)
                                .replace("$3", endTime)
                                .replace("$4", userId));
            } else if (role.equalsIgnoreCase("provider") || role.equalsIgnoreCase("delegate")) {
                String resourceId = request.getString(IID);
                String providerID =
                        resourceId.substring(0, resourceId.indexOf('/', resourceId.indexOf('/') + 1));
                LOGGER.debug("Provider =" + providerID);
                monthQuery =
                        new StringBuilder(OVERVIEW_QUERY.concat(" and providerid = '$4' ").concat(GROUPBY)
                                .replace("$0", timeSeriesToFirstDay)
                                .replace("$1", endTime)
                                .replace("$2", startTime)
                                .replace("$3", endTime)
                                .replace("$4", providerID));
            }
        } else {
            if (role.equalsIgnoreCase("admin")) {
                monthQuery =
                        new StringBuilder(OVERVIEW_QUERY.concat(GROUPBY)
                                .replace("$0", timeYearBack)
                                .replace("$1", utcTime.toString())
                                .replace("$2", timeYearBack)
                                .replace("$3", utcTime.toString()));
            } else if (role.equalsIgnoreCase("consumer")) {
                String userId = request.getString(USER_ID);
                monthQuery =
                        new StringBuilder(OVERVIEW_QUERY.concat(" and userid = '$4' ").concat(GROUPBY)
                                .replace("$0", timeYearBack)
                                .replace("$1", utcTime.toString())
                                .replace("$2", timeYearBack)
                                .replace("$3", utcTime.toString())
                                .replace("$4", userId));
            } else if (role.equalsIgnoreCase("provider") || role.equalsIgnoreCase("delegate")) {
                String resourceId = request.getString(IID);
                String providerID =
                        resourceId.substring(0, resourceId.indexOf('/', resourceId.indexOf('/') + 1));
                LOGGER.debug("Provider =" + providerID);
                monthQuery =
                        new StringBuilder(OVERVIEW_QUERY.concat(" and providerid = '$4' ").concat(GROUPBY)
                                .replace("$0", timeYearBack)
                                .replace("$1", utcTime.toString())
                                .replace("$2", timeYearBack)
                                .replace("$3", utcTime.toString())
                                .replace("$4", providerID));
            }
        }

        return monthQuery.toString();
    }
    public String buildSummaryOverview(JsonObject request) {
        StringBuilder summaryQuery=
                new StringBuilder(SUMMARY_QUERY_FOR_METERING);
        return summaryQuery.toString();
    }
}
