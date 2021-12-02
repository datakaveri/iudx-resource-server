package iudx.resource.server.database.archives.elastic;

import static iudx.resource.server.database.archives.Constants.ATTRIBUTE_KEY;
import static iudx.resource.server.database.archives.Constants.ATTRIBUTE_QUERY_KEY;
import static iudx.resource.server.database.archives.Constants.BETWEEN_OP;
import static iudx.resource.server.database.archives.Constants.EQUAL_OP;
import static iudx.resource.server.database.archives.Constants.GREATER_THAN_EQ_OP;
import static iudx.resource.server.database.archives.Constants.GREATER_THAN_OP;
import static iudx.resource.server.database.archives.Constants.LESS_THAN_EQ_OP;
import static iudx.resource.server.database.archives.Constants.LESS_THAN_OP;
import static iudx.resource.server.database.archives.Constants.NOT_EQUAL_OP;
import static iudx.resource.server.database.archives.Constants.OPERATOR;
import static iudx.resource.server.database.archives.Constants.VALUE;
import static iudx.resource.server.database.archives.Constants.VALUE_LOWER;
import static iudx.resource.server.database.archives.Constants.VALUE_UPPER;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.database.archives.elastic.exception.ESQueryDecodeException;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

public class AttributeQueryParser implements QueryParser {

  private final BoolQueryBuilder builder;
  private final JsonObject json;

  public AttributeQueryParser(BoolQueryBuilder builder, JsonObject json) {
    this.builder = builder;
    this.json = json;
  }

  @Override
  public BoolQueryBuilder parse() {
    JsonArray attrQuery;

    if (json.containsKey(ATTRIBUTE_QUERY_KEY)) {
      attrQuery = json.getJsonArray(ATTRIBUTE_QUERY_KEY);

      /* Multi-Attribute */
      for (Object obj : attrQuery) {
        JsonObject attrObj = (JsonObject) obj;
        try {
          String attribute = attrObj.getString(ATTRIBUTE_KEY);
          String operator = attrObj.getString(OPERATOR);
          String attributeValue = attrObj.getString(VALUE);

          if (GREATER_THAN_OP.equalsIgnoreCase(operator)) {
            builder.filter(QueryBuilders.rangeQuery(attribute).gt(attributeValue));
          } else if (LESS_THAN_OP.equalsIgnoreCase(operator)) {
            builder.filter(QueryBuilders.rangeQuery(attribute).lt(attributeValue));
          } else if (GREATER_THAN_EQ_OP.equalsIgnoreCase(operator)) {
            builder.filter(QueryBuilders.rangeQuery(attribute).gte(attributeValue));
          } else if (LESS_THAN_EQ_OP.equalsIgnoreCase(operator)) {
            builder.filter(QueryBuilders.rangeQuery(attribute).lte(attributeValue));
          } else if (EQUAL_OP.equalsIgnoreCase(operator)) {
            builder.filter(QueryBuilders.termQuery(attribute, attributeValue));
          } else if (BETWEEN_OP.equalsIgnoreCase(operator)) {
            builder.filter(
                QueryBuilders.rangeQuery(attribute)
                    .gte(attrObj.getString(VALUE_LOWER))
                    .lte(attrObj.getString(VALUE_UPPER)));
          } else if (NOT_EQUAL_OP.equalsIgnoreCase(operator)) {
            builder.mustNot(QueryBuilders.termQuery(attribute, attributeValue));
          } else {
            throw new ESQueryDecodeException(
                ResponseUrn.INVALID_ATTR_PARAM, "invalid attribute operator");
          }
        } catch (NullPointerException e) {
          throw new ESQueryDecodeException(
              ResponseUrn.INVALID_ATTR_PARAM, "exception occurred at decoding attributes");
        }
      }
    }
    return builder;
  }
}
