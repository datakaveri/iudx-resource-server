package iudx.resource.server.database.elastic;

import static iudx.resource.server.database.archives.Constants.*;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.json.JsonData;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.database.elastic.exception.ESQueryException;

public class TemporalQueryFiltersDecorator implements ElasticsearchQueryDecorator {

  private static final Logger LOGGER = LogManager.getLogger(TemporalQueryFiltersDecorator.class);
  private Map<FilterType, List<Query>> queryFilters;
  private JsonObject requestQuery;
  private final int defaultDateLimit;

  public TemporalQueryFiltersDecorator(Map<FilterType, List<Query>> queryFilters,
      JsonObject requestQuery, int defaultDateLimit) {
    this.queryFilters = queryFilters;
    this.requestQuery = requestQuery;
    this.defaultDateLimit = defaultDateLimit;
  }

  @Override
  public Map<FilterType, List<Query>> add() {
    String queryRequestTimeRelation = requestQuery.getString(REQ_TIMEREL);
    String queryRequestStartTime = requestQuery.getString(TIME_KEY);
    String queryRequestEndTime = requestQuery.getString(END_TIME);

    ZonedDateTime startDateTime = getZonedDateTime(queryRequestStartTime);
    ZonedDateTime endDateTime =
        (queryRequestEndTime != null) ? getZonedDateTime(queryRequestEndTime) : null;


    if (DURING.equalsIgnoreCase(queryRequestTimeRelation)
        || BETWEEN.equalsIgnoreCase(queryRequestTimeRelation)) {
      validateTemporalPeriod(startDateTime, endDateTime);
    } else if (BEFORE.equalsIgnoreCase(queryRequestTimeRelation)) {
      queryRequestStartTime = startDateTime.minusDays(defaultDateLimit).toString();
      queryRequestEndTime = startDateTime.toString();
    } else if (AFTER.equalsIgnoreCase(queryRequestTimeRelation)) {
      queryRequestStartTime = startDateTime.toString();
      queryRequestEndTime = getEndDateForAfterQuery(startDateTime);
    } else {
      throw new ESQueryException("exception while parsing date/time");
    }

    final String startTime = queryRequestStartTime;
    final String endTime = queryRequestEndTime;

    Query temporalQuery = RangeQuery
        .of(r -> r
            .field("observationDateTime")
              .lte(JsonData.of(endTime))
              .gte(JsonData.of(startTime)))
          ._toQuery();


    List<Query> queryList = queryFilters.get(FilterType.FILTER);
    queryList.add(temporalQuery);
    return queryFilters;
  }
  
  public void addDefaultTemporalFilters(Map<FilterType, List<Query>> queryLists, JsonObject query) {
    String[] timeLimitConfig = query.getString(TIME_LIMIT).split(",");
    String deploymentType = timeLimitConfig[0];
    String dateToUseForDevDeployment = timeLimitConfig[1];
    if (PROD_INSTANCE.equalsIgnoreCase(deploymentType)) {
      addDefaultForProduction(queryLists);
    } else if (TEST_INSTANCE.equalsIgnoreCase(deploymentType)) {
      addDefaultForDev(queryLists, dateToUseForDevDeployment);
    }else {
      throw new ESQueryException("invalid timeLimit config passed");
    }
  }

  private void addDefaultForDev(Map<FilterType, List<Query>> queryLists,
      String dateToUseForDevDeployment) {
    ZonedDateTime endTime = getZonedDateTime(dateToUseForDevDeployment);
    ZonedDateTime startTime = endTime.minusDays(defaultDateLimit);
    //LOGGER.debug("startTim :{}, endTime : {} [default days : {}]",startTime,endTime,defaultDateLimit);
    Query temporalQuery = RangeQuery
        .of(r -> r
            .field("observationDateTime")
              .lte(JsonData.of(endTime.toString()))
              .gte(JsonData.of(startTime.toString())))
          ._toQuery();
    List<Query> queryList = queryLists.get(FilterType.FILTER);
    queryList.add(temporalQuery);
  }

  private void addDefaultForProduction(Map<FilterType, List<Query>> queryLists) {
    OffsetDateTime currentDateTime = OffsetDateTime.now().minusDays(defaultDateLimit);
    Query temporalQuery = RangeQuery
        .of(r -> r.field("observationDateTime").gte(JsonData.of(currentDateTime.toString())))
          ._toQuery();
    List<Query> queryList = queryLists.get(FilterType.FILTER);
    queryList.add(temporalQuery);
  }

  private String getEndDateForAfterQuery(ZonedDateTime startDateTime) {
    ZonedDateTime endDateTime;
    endDateTime = startDateTime.plusDays(defaultDateLimit);
    ZonedDateTime now = ZonedDateTime.now();
    long difference = endDateTime.compareTo(now);
    if (difference > 0) {
      return now.toString();
    } else {
      return endDateTime.toString();
    }
  }

  private void validateTemporalPeriod(ZonedDateTime startDateTime, ZonedDateTime endDateTime) {
    if (endDateTime == null) {
      throw new ESQueryException("No endDate[required mandatory field] provided for query");
    }

    if (startDateTime.isAfter(endDateTime)) {
      throw new ESQueryException("end date is before start date");
    }
  }

  private ZonedDateTime getZonedDateTime(String time) {
    try {
      return ZonedDateTime.parse(time);
    } catch (DateTimeParseException e) {
      throw new ESQueryException("exception while parsing date/time");
    }
  }

}
