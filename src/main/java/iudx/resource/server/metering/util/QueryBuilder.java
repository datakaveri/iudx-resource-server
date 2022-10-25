package iudx.resource.server.metering.util;

import static iudx.resource.server.apiserver.util.Constants.RESPONSE_SIZE;
import static iudx.resource.server.metering.util.Constants.API;
import static iudx.resource.server.metering.util.Constants.API_QUERY;
import static iudx.resource.server.metering.util.Constants.CONSUMERID_TIME_INTERVAL_COUNT_QUERY;
import static iudx.resource.server.metering.util.Constants.CONSUMER_ID;
import static iudx.resource.server.metering.util.Constants.END_TIME;
import static iudx.resource.server.metering.util.Constants.ID;
import static iudx.resource.server.metering.util.Constants.ID_QUERY;
import static iudx.resource.server.metering.util.Constants.LAST_ID;
import static iudx.resource.server.metering.util.Constants.LATEST_ID;
import static iudx.resource.server.metering.util.Constants.ORDER_BY_AND_LIMIT;
import static iudx.resource.server.metering.util.Constants.PROVIDERID_TIME_INTERVAL_COUNT_QUERY;
import static iudx.resource.server.metering.util.Constants.PROVIDER_ID;
import static iudx.resource.server.metering.util.Constants.QUERY_KEY;
import static iudx.resource.server.metering.util.Constants.RESOURCE_ID;
import static iudx.resource.server.metering.util.Constants.RESOURCE_QUERY;
import static iudx.resource.server.metering.util.Constants.START_TIME;
import static iudx.resource.server.metering.util.Constants.TABLE_NAME;
import static iudx.resource.server.metering.util.Constants.USER_ID;
import static iudx.resource.server.metering.util.Constants.USER_ID_QUERY;
import static iudx.resource.server.metering.util.Constants.WHERE;
import static iudx.resource.server.metering.util.Constants.WRITE_QUERY;

import io.vertx.core.json.JsonObject;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class QueryBuilder {

  private static final Logger LOGGER = LogManager.getLogger(QueryBuilder.class);
  public JsonObject buildCountQuery(JsonObject request) {
    String startTime = request.getString(START_TIME);
    String endTime = request.getString(END_TIME);
    String resourceId = request.getString(RESOURCE_ID);
    String userId = request.getString(USER_ID);
    String api = request.getString(API);
    String providerID = request.getString(PROVIDER_ID);
    String consumerID = request.getString(CONSUMER_ID);
    String databaseTableName = request.getString(TABLE_NAME);

    StringBuilder query, tempQuery;

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
    if (consumerID != null) {
      tempQuery = query;
      tempQuery.append(USER_ID_QUERY.replace("$6", consumerID));
    }
    if (api != null && resourceId != null) {
      tempQuery = query;
      for (String s :
          Arrays.asList(API_QUERY.replace("$5", api), RESOURCE_QUERY.replace("$4", resourceId))) {
        tempQuery.append(s);
      }
    } else if (api != null) {
      tempQuery = query;
      tempQuery.append(API_QUERY.replace("$5", api));
    } else if (resourceId != null) {
      tempQuery = query;
      tempQuery.append(RESOURCE_QUERY.replace("$4", resourceId));
    } else {
      tempQuery = query;
    }
    tempQuery.insert(tempQuery.length(), ";");
    LOGGER.trace("Info: QUERY " + tempQuery);
    return new JsonObject().put(QUERY_KEY, tempQuery);
  }

  public JsonObject buildWritingQuery(JsonObject request) {

    String primaryKey = UUID.randomUUID().toString().replace("-", "");
    String userId = request.getString(USER_ID);
    String resourceId = request.getString(ID);
    String providerID =
        resourceId.substring(0, resourceId.indexOf('/', resourceId.indexOf('/') + 1));
    String api = request.getString(API);
    ZonedDateTime zst = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
    long time = getEpochTime(zst);
    String isoTime = zst.truncatedTo(ChronoUnit.SECONDS).toString();
    long response_size = request.getLong(RESPONSE_SIZE);
    String databaseTableName = request.getString(TABLE_NAME);

    StringBuilder query =
        new StringBuilder(
            WRITE_QUERY
                .replace("$0", databaseTableName)
                .replace("$1", primaryKey)
                .replace("$2", api)
                .replace("$3", userId)
                .replace("$4", Long.toString(time))
                .replace("$5", resourceId)
                .replace("$6", isoTime)
                .replace("$7", providerID)
                .replace("$8", Long.toString(response_size)));

    LOGGER.trace("Info: Query " + query);
    return new JsonObject().put(QUERY_KEY, query);
  }

  public JsonObject buildReadingQuery(JsonObject request) {
    StringBuilder query =
        new StringBuilder(
            request.getString(QUERY_KEY).replace("count(*)", "*").replace(";", ORDER_BY_AND_LIMIT));
    return new JsonObject().put(QUERY_KEY, query);
  }

  public String buildTempReadQuery(JsonObject request) {
    StringBuilder tempQuery = new StringBuilder(request.getString(QUERY_KEY));
    String lastId = request.getString(LAST_ID);
    String latestId = request.getString(LATEST_ID);
    if (tempQuery.toString().contains(lastId)) {
      return tempQuery.toString().replace(lastId, latestId);
    } else {
      tempQuery.insert(tempQuery.indexOf(WHERE) + 5, ID_QUERY.replace("$7", lastId));
    }
    return tempQuery.toString();
  }

  private long getEpochTime(ZonedDateTime time) {
    return time.toInstant().toEpochMilli();
  }
}