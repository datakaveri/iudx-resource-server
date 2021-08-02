package iudx.resource.server.metering.util;

import static iudx.resource.server.metering.util.Constants.API;
import static iudx.resource.server.metering.util.Constants.EMAIL_ID;
import static iudx.resource.server.metering.util.Constants.EMAIL_QUERY;
import static iudx.resource.server.metering.util.Constants.END_TIME;
import static iudx.resource.server.metering.util.Constants.ERROR;
import static iudx.resource.server.metering.util.Constants.ID;
import static iudx.resource.server.metering.util.Constants.INVALID_DATE_TIME;
import static iudx.resource.server.metering.util.Constants.QUERY_KEY;
import static iudx.resource.server.metering.util.Constants.RESOURCE_QUERY;
import static iudx.resource.server.metering.util.Constants.START_TIME;
import static iudx.resource.server.metering.util.Constants.TIME_INTERVAL_QUERY;
import static iudx.resource.server.metering.util.Constants.TIME_NOT_FOUND;
import static iudx.resource.server.metering.util.Constants.WRITE_QUERY;

import io.vertx.core.json.JsonObject;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class QueryBuilder {

  private static final Logger LOGGER = LogManager.getLogger(QueryBuilder.class);

  public JsonObject buildReadingQuery(JsonObject request) {
    String startTime = request.getString(START_TIME);
    String endTime = request.getString(END_TIME);
    String resourceId = request.getString(ID);
    String emailId = request.getString(EMAIL_ID);

    if (!request.containsKey(START_TIME) || !request.containsKey(END_TIME)) {
      return new JsonObject().put(ERROR, TIME_NOT_FOUND);
    }

    /* check if the time is valid based on ISO 8601 format. */
    ZonedDateTime zdt;
    try {
      zdt = ZonedDateTime.parse(startTime);
      LOGGER.debug("Parsed time: " + zdt.toString());
    } catch (DateTimeParseException e) {
      LOGGER.error("Invalid Date exception: " + e.getMessage());
      return new JsonObject().put(ERROR, INVALID_DATE_TIME);
    }

    try {
      zdt = ZonedDateTime.parse(endTime);
      LOGGER.debug("Parsed time: " + zdt.toString());
    } catch (DateTimeParseException e) {
      LOGGER.error("Invalid Date exception: " + e.getMessage());
      return new JsonObject().put(ERROR, INVALID_DATE_TIME);
    }

    ZonedDateTime startZDT = ZonedDateTime.parse(startTime);
    ZonedDateTime endZDT = ZonedDateTime.parse(endTime);

    if (startZDT.isAfter(endZDT)) {
      LOGGER.error("Invalid Date exception");
      return new JsonObject().put(ERROR, INVALID_DATE_TIME);
    }

    long fromTime = getEpochTime(startZDT);
    LOGGER.debug("Epoch fromTime: " + fromTime);

    long toTime = getEpochTime(endZDT);

    LOGGER.debug("Epoch toTime: " + toTime);
    StringBuilder timeQuery =
        new StringBuilder(
            TIME_INTERVAL_QUERY
                .replace("$1", Long.toString(fromTime))
                .replace("$2", Long.toString(toTime)));
    LOGGER.info("Info: QUERY " + timeQuery);

    if (emailId != null && resourceId != null) {
      StringBuilder tempQuery = timeQuery;
      for (String s :
          Arrays.asList(
              EMAIL_QUERY.replace("$3", emailId), RESOURCE_QUERY.replace("$4", resourceId))) {
        tempQuery = tempQuery.append(s);
      }
      LOGGER.info("Info: QUERY " + tempQuery);
      return new JsonObject().put(QUERY_KEY, tempQuery);
    } else if (emailId != null) {
      StringBuilder tempQuery = timeQuery;
      tempQuery = tempQuery.append(EMAIL_QUERY.replace("$3", emailId));
      LOGGER.info("Info: QUERY " + tempQuery);
      return new JsonObject().put(QUERY_KEY, tempQuery);
    } else if (resourceId != null) {
      StringBuilder tempQuery = timeQuery;
      tempQuery = tempQuery.append(RESOURCE_QUERY.replace("$4", resourceId));
      LOGGER.info("Info: QUERY " + tempQuery);
      return new JsonObject().put(QUERY_KEY, tempQuery);
    } else {
      StringBuilder tempQuery = timeQuery;
      LOGGER.info("Info: QUERY " + tempQuery);
      return new JsonObject().put(QUERY_KEY, tempQuery);
    }
  }

  public JsonObject buildWritingQuery(JsonObject request) {

    String primaryKey = UUID.randomUUID().toString().replace("-", "");
    String email = request.getString(EMAIL_ID);
    String resourceId = request.getString(ID);
    String api = request.getString(API);
    ZonedDateTime zst = ZonedDateTime.now();
    LOGGER.info("TIME ZST: " + zst);
    long time = getEpochTime(zst);

    StringBuilder query =
        new StringBuilder(
            WRITE_QUERY
                .replace("$1", primaryKey)
                .replace("$2", Long.toString(time))
                .replace("$3", resourceId)
                .replace("$4", api)
                .replace("$5", email));

    LOGGER.info("Info: Query " + query);
    return new JsonObject().put(QUERY_KEY, query);
  }

  private long getEpochTime(ZonedDateTime time) {
    return time.toInstant().toEpochMilli();
  }
}
