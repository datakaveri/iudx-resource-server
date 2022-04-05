package iudx.resource.server.database.elastic;

import static iudx.resource.server.database.archives.Constants.AFTER;
import static iudx.resource.server.database.archives.Constants.BEFORE;
import static iudx.resource.server.database.archives.Constants.BETWEEN;
import static iudx.resource.server.database.archives.Constants.DURING;
import static iudx.resource.server.database.archives.Constants.END_TIME;
import static iudx.resource.server.database.archives.Constants.REQ_TIMEREL;
import static iudx.resource.server.database.archives.Constants.TIME_KEY;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.database.elastic.exception.ESQueryDecodeException;

public class TemporalQueryParser implements QueryParser {


  private BoolQueryBuilder builder;
  private JsonObject json;

  public TemporalQueryParser(BoolQueryBuilder builder, JsonObject json) {
    this.builder = builder;
    this.json = json;
  }

  @Override
  public BoolQueryBuilder parse() {

    String timeRelation = json.getString(REQ_TIMEREL);
    String time = json.getString(TIME_KEY);
    ZonedDateTime zdt = null;

    String startTime = null;
    String endTime = null;

    try {
      zdt = ZonedDateTime.parse(time);
    } catch (DateTimeParseException e) {
      throw new ESQueryDecodeException("exception while parsing date/time");
    }

    if (DURING.equalsIgnoreCase(timeRelation) || BETWEEN.equalsIgnoreCase(timeRelation)) {
      ZonedDateTime endzdt;
      endTime = json.getString(END_TIME);
      startTime = time;
      try {
        endzdt = ZonedDateTime.parse(endTime);
      } catch (DateTimeParseException e) {
        throw new ESQueryDecodeException("exception while parsing date/time");
      }
      
      if (zdt.isAfter(endzdt)) {
        throw new ESQueryDecodeException("end date is before start date");
      }
      
    } else if (BEFORE.equalsIgnoreCase(timeRelation)) {
      zdt = ZonedDateTime.parse(time);
      startTime = zdt.minusDays(10).toString();
      endTime = time;
    } else if (AFTER.equalsIgnoreCase(timeRelation)) {
      zdt = ZonedDateTime.parse(time).plusDays(10);
      ZonedDateTime currentTime = ZonedDateTime.now();
      startTime = time;
      long difference = zdt.compareTo(currentTime);
      if (difference > 0) {
        endTime = currentTime.toString();
      } else {
        endTime = zdt.toString();
      }
    } else {
      throw new ESQueryDecodeException("exception while parsing date/time");
    }

    builder
        .filter(QueryBuilders.rangeQuery("observationDateTime")
            .lte(endTime)
            .gte(startTime));

    return builder;
  }

}
