package iudx.resource.server.database.archives.elastic;

import static iudx.resource.server.database.archives.Constants.AFTER;
import static iudx.resource.server.database.archives.Constants.BEFORE;
import static iudx.resource.server.database.archives.Constants.DURING;
import static iudx.resource.server.database.archives.Constants.END_TIME;
import static iudx.resource.server.database.archives.Constants.REQ_TIMEREL;
import static iudx.resource.server.database.archives.Constants.TIME_KEY;

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import io.vertx.core.json.JsonObject;

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
      // TODO : throw RuntimeException
    }

    if (DURING.equalsIgnoreCase(timeRelation)) {
      ZonedDateTime endzdt;
      endTime = json.getString(END_TIME);
      startTime = time;
      endzdt = ZonedDateTime.parse(endTime);

      if (zdt.isAfter(endzdt)) {
        // TODO: throw Runtime exception
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
      // TODO : throw Runtime exception.
    }

    builder
        .filter(QueryBuilders.rangeQuery("observationDateTime")
            .lte(endTime)
            .gte(startTime));

    return builder;
  }


  public static void main(String[] args) {
    TemporalQueryParser temporalQueryParser = new TemporalQueryParser(new BoolQueryBuilder(), new JsonObject(
        "{\"id\":[\"iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood/FWR055\"],\"time\":\"2020-10-18T14:20:01Z\",\"timerel\":\"after\",\"searchType\":\"temporalSearch\",\"instanceID\":\"localhost:8443\"}"));
    BoolQueryBuilder bool = temporalQueryParser.parse();
    System.out.println(bool.toString());
  }

}
