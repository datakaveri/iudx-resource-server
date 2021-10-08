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

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class AttributeQueryParser implements QueryParser {

  private BoolQueryBuilder builder;
  private JsonObject json;

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
        String attrElasticQuery = "";

        try {
          String attribute = attrObj.getString(ATTRIBUTE_KEY);
          String operator = attrObj.getString(OPERATOR);
          String attributeValue = attrObj.getString(VALUE);

          if (GREATER_THAN_OP.equalsIgnoreCase(operator)) {
            // attrElasticQuery = RANGE_QUERY.replace("$1", attribute).replace("$2", GREATER_THAN)
            // .replace("$3", attributeValue);
            builder.filter(QueryBuilders.rangeQuery(attribute).gt(attributeValue));

          } else if (LESS_THAN_OP.equalsIgnoreCase(operator)) {
            // attrElasticQuery = RANGE_QUERY.replace("$1", attribute).replace("$2", LESS_THAN)
            // .replace("$3", attributeValue);
            builder.filter(QueryBuilders.rangeQuery(attribute).lt(attributeValue));
          } else if (GREATER_THAN_EQ_OP.equalsIgnoreCase(operator)) {
            // attrElasticQuery = RANGE_QUERY.replace("$1", attribute).replace("$2", GREATER_THAN_EQ)
            // .replace("$3", attributeValue);
            builder.filter(QueryBuilders.rangeQuery(attribute).gte(attributeValue));
          } else if (LESS_THAN_EQ_OP.equalsIgnoreCase(operator)) {
            // attrElasticQuery = RANGE_QUERY.replace("$1", attribute).replace("$2", LESS_THAN_EQ)
            // .replace("$3", attributeValue);
            builder.filter(QueryBuilders.rangeQuery(attribute).lte(attributeValue));
          } else if (EQUAL_OP.equalsIgnoreCase(operator)) {
            // attrElasticQuery = TERM_QUERY.replace("$1", attribute).replace("$2", attributeValue);
            builder.filter(QueryBuilders.termQuery(attribute, attributeValue));
          } else if (BETWEEN_OP.equalsIgnoreCase(operator)) {
            // attrElasticQuery = RANGE_QUERY_BW.replace("$1", attribute)
            // .replace("$2", GREATER_THAN_EQ).replace("$3", attrObj.getString(VALUE_LOWER))
            // .replace("$4", LESS_THAN_EQ).replace("$5", attrObj.getString(VALUE_UPPER));
            builder.filter(QueryBuilders.rangeQuery(attribute)
                .gte(attrObj.getString(VALUE_LOWER))
                .lte(attrObj.getString(VALUE_UPPER)));
          } else if (NOT_EQUAL_OP.equalsIgnoreCase(operator)) {
            builder.mustNot(QueryBuilders.termQuery(attribute, attributeValue));
          } else {
            // TODO: throw runtime exception
          }

          if (!attrElasticQuery.isBlank()) {
            // filterQuery.add(new JsonObject(attrElasticQuery));
          }

        } catch (NullPointerException e) {
          // TODO: throw runtime exception
        }
      }
    }
    return builder;
  }

  public static void main(String[] args) {
    AttributeQueryParser attrQueryParser = new AttributeQueryParser(new BoolQueryBuilder(), new JsonObject(
        "{\"id\":[\"iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood/FWR055\"],\"attr-query\":[{\"attribute\":\"referenceLevel\",\"operator\":\">\",\"value\":\"15.0\"}],\"searchType\":\"latestSearch_attributeSearch\",\"instanceID\":\"localhost:8443\",\"applicableFilters\":[\"ATTR\",\"TEMPORAL\"]}"));
    BoolQueryBuilder bool = attrQueryParser.parse();
    System.out.println(bool.toString());
  }

}
