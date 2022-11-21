package iudx.resource.server.database.elastic;

import static iudx.resource.server.database.archives.Constants.*;
import java.util.List;
import java.util.Map;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.json.JsonData;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.database.elastic.exception.ESQueryException;

public class AttributeQueryFiltersDecorator implements ElasticsearchQueryDecorator {


  private Map<FilterType, List<Query>> queryFilters;
  private JsonObject requestQuery;

  public AttributeQueryFiltersDecorator(Map<FilterType, List<Query>> queryFilters,
      JsonObject requestQuery) {
    this.queryFilters = queryFilters;
    this.requestQuery = requestQuery;
  }

  @Override
  public Map<FilterType, List<Query>> add() {
    JsonArray attrQuery;

    if (!requestQuery.containsKey(ATTRIBUTE_QUERY_KEY)) {
      return queryFilters;
    }


    attrQuery = requestQuery.getJsonArray(ATTRIBUTE_QUERY_KEY);
    for (Object obj : attrQuery) {
      JsonObject attrObj = (JsonObject) obj;
      Query attrRangeQuery;
      try {
        String attribute = attrObj.getString(ATTRIBUTE_KEY);
        String operator = attrObj.getString(OPERATOR);
        String attributeValue = attrObj.getString(VALUE);

        if (GREATER_THAN_OP.equalsIgnoreCase(operator)) {

          attrRangeQuery = RangeQuery
              .of(query -> query.field(attribute).gt(JsonData.of(attributeValue)))
                ._toQuery();

          List<Query> queryList = queryFilters.get(FilterType.FILTER);
          queryList.add(attrRangeQuery);

        } else if (LESS_THAN_OP.equalsIgnoreCase(operator)) {

          attrRangeQuery = RangeQuery
              .of(query -> query.field(attribute).lt(JsonData.of(attributeValue)))
                ._toQuery();

          List<Query> queryList = queryFilters.get(FilterType.FILTER);
          queryList.add(attrRangeQuery);

        } else if (GREATER_THAN_EQ_OP.equalsIgnoreCase(operator)) {

          attrRangeQuery = RangeQuery
              .of(query -> query.field(attribute).gte(JsonData.of(attributeValue)))
                ._toQuery();

          List<Query> queryList = queryFilters.get(FilterType.FILTER);
          queryList.add(attrRangeQuery);

        } else if (LESS_THAN_EQ_OP.equalsIgnoreCase(operator)) {

          attrRangeQuery = RangeQuery
              .of(query -> query.field(attribute).lte(JsonData.of(attributeValue)))
                ._toQuery();

          List<Query> queryList = queryFilters.get(FilterType.FILTER);
          queryList.add(attrRangeQuery);

        } else if (EQUAL_OP.equalsIgnoreCase(operator)) {

          Query termQuery =
              TermQuery.of(query -> query.field(attribute).value(attributeValue))._toQuery();

          List<Query> queryList = queryFilters.get(FilterType.FILTER);
          queryList.add(termQuery);

        } else if (BETWEEN_OP.equalsIgnoreCase(operator)) {
          JsonData gteField = JsonData.of(attrObj.getString(VALUE_LOWER));
          JsonData lteField = JsonData.of(attrObj.getString(VALUE_UPPER));

          attrRangeQuery =
              RangeQuery.of(query -> query.field(attribute).gte(gteField).lte(lteField))._toQuery();

          List<Query> queryList = queryFilters.get(FilterType.FILTER);
          queryList.add(attrRangeQuery);

        } else if (NOT_EQUAL_OP.equalsIgnoreCase(operator)) {

          Query termQuery =
              TermQuery.of(query -> query.field(attribute).value(attributeValue))._toQuery();

          List<Query> queryList = queryFilters.get(FilterType.MUST_NOT);
          queryList.add(termQuery);

        } else {
          throw new ESQueryException(ResponseUrn.INVALID_ATTR_PARAM_URN,
              "invalid attribute operator");
        }
      } catch (ESQueryException e) {
        throw e;
      } catch (Exception e) {
        throw new ESQueryException(ResponseUrn.INVALID_ATTR_PARAM_URN,
            "exception occured at decoding attributes");
      }
    }
    return queryFilters;
  }

}
