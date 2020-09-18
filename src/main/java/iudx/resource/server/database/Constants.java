package iudx.resource.server.database;

public class Constants {
  /* General Purpose */
  static final String SEARCH_TYPE = "searchType";
  static final String ID = "id";
  //static final String RESOURCE_ID_KEY = "resource-id";
  static final String RESOURCE_ID_KEY = "id";
  /* Database */
  //static final String GEO_KEY = "geoJsonLocation";
  static final String GEO_KEY = "location";
  static final String GEO_CIRCLE = "circle";
  static final String GEO_BBOX = "envelope";
  static final String COORDINATES_KEY = "coordinates";
  static final String GEO_RELATION_KEY = "relation";
  static final String TYPE_KEY = "type";
  static final String GEO_SHAPE_KEY = "geo_shape";
  static final String GEO_RADIUS = "radius";
  static final String SHAPE_KEY = "shape";
  static final String QUERY_KEY = "query";
  static final String FILTER_KEY = "filter";
  static final String BOOL_KEY = "bool";
  static final String VARANASI_TEST_SEARCH_INDEX = "varanasi/_search";
  static final String VARANASI_TEST_COUNT_INDEX = "varanasi/_count";
  static final String VARANASI_LATEST_RESOURCE_INDEX = "varanasi-latest/_mget";
  static final String LATEST_RESOURCE_INDEX = "latest/_mget";
  static final String SOURCE_FILTER_KEY = "_source";
  static final String RANGE_KEY = "range";
  static final String TERM_KEY = "term";
  static final String TERMS_KEY = "terms";
  static final String FILTER_PATH = "filter_path";
  static final String FILTER_PATH_VAL = "took,hits.hits._source";
  static final String FILTER_PATH_VAL_LATEST = "docs._source";
  static final String SIZE_KEY = "size";
  static final String GREATER_THAN = "gt";
  static final String LESS_THAN = "lt";
  static final String GREATER_THAN_EQ = "gte";
  static final String LESS_THAN_EQ = "lte";
  static final String MUST_NOT = "must_not";
  static final String REQUEST_GET = "GET";
  static final String HITS = "hits";
  static final String SEARCH_KEY = "search";
  static final String ERROR = "Error";
  static final String COUNT = "count";
  static final String DOC_ID = "_id";
  static final String DOCS_KEY = "docs";
  static final String SEARCH_REQ_PARAM = "/_search";
  static final String COUNT_REQ_PARAM = "/_count";
  static final String TIME_FIELD_DB = "observationDateTime";
  /* Request Params */
  /* Temporal */
  static final String REQ_TIMEREL = "timerel";
  static final String TIME_KEY = "time";
  static final String END_TIME = "endtime";
  static final String DURING = "during";
  static final String AFTER = "after";
  static final String BEFORE = "before";
  /* Geo-Spatial */
  static final String LAT = "lat";
  static final String LON = "lon";
  static final String GEOMETRY = "geometry";
  static final String GEOREL = "georel";
  static final String WITHIN = "within";
  static final String POLYGON = "polygon";
  static final String LINESTRING = "linestring";
  static final String GEO_PROPERTY = "geoproperty";
  static final String BBOX = "bbox";
  /* Response Filter */
  static final String RESPONSE_ATTRS = "attrs";
  /* Attribute */
  static final String ATTRIBUTE_QUERY_KEY = "attr-query";
  static final String ATTRIBUTE_KEY = "attribute";
  static final String OPERATOR = "operator";
  static final String VALUE = "value";
  static final String VALUE_LOWER = "valueLower";
  static final String VALUE_UPPER = "valueUpper";
  static final String GREATER_THAN_OP = ">";
  static final String LESS_THAN_OP = "<";
  static final String GREATER_THAN_EQ_OP = ">=";
  static final String LESS_THAN_EQ_OP = "<=";
  static final String EQUAL_OP = "==";
  static final String NOT_EQUAL_OP = "!=";
  static final String BETWEEN_OP = "<==>";
  /* Errors */
  static final String INVALID_OPERATOR = "Invalid operator";
  static final String INVALID_SEARCH = "Invalid search request";
  static final String INVALID_DATE = "Invalid date format";
  static final String MISSING_ATTRIBUTE_FIELDS = "Missing attribute query fields";
  static final String MISSING_TEMPORAL_FIELDS = "Missing/Invalid temporal parameters";
  static final String MISSING_RESPONSE_FILTER_FIELDS = "Missing/Invalid responseFilter parameters";
  static final String MISSING_GEO_FIELDS = "Missing/Invalid geo parameters";
  static final String COORDINATE_MISMATCH = "Coordinate mismatch (Polygon)";
  static final String COUNT_UNSUPPORTED = "Count is not supported with filtering";
  static final String EMPTY_RESPONSE = "Empty response";
  static final String DB_ERROR = "DB request has failed";
  static final String DB_ERROR_2XX = "Status code is not 2xx";
  static final String ID_NOT_FOUND = "No id found";
  static final String EMPTY_RESOURCE_ID = "resource-id is empty";
  static final String SEARCHTYPE_NOT_FOUND = "No searchType found";
  /* Search Regex */
  static final String GEOSEARCH_REGEX = "(.*)geoSearch(.*)";
  static final String RESPONSE_FILTER_REGEX = "(.*)responseFilter(.*)";
  public static final String ATTRIBUTE_SEARCH_REGEX = "(.*)attributeSearch(.*)";
  public static final String TEMPORAL_SEARCH_REGEX = "(.*)temporalSearch(.*)";
  static final String LATEST_SEARCH = "latestSearch";
  /* Query templates */
  static final String GEO_SHAPE_QUERY =
      "{ \"geo_shape\": { \"$4\": { \"shape\": { \"type\": \"$1\", \"coordinates\": $2 },"
          + " \"relation\": \"$3\" } } }";
  static final String TIME_QUERY = "{\"range\":{\"observationDateTime\":{\"$1\":\"$2\"}}}";
  static final String TERM_QUERY = "{\"term\":{\"$1\":\"$2\"}}";
  static final String TERMS_QUERY = "{\"terms\":{\"$1\":$2}}";
  static final String RANGE_QUERY = "{\"range\":{\"$1\":{\"$2\":$3}}}";
  static final String RANGE_QUERY_BW = "{\"range\":{\"$1\":{\"$2\":$3,\"$4\":$5}}}";
  public static final String MUST_NOT_QUERY = "{\"must_not\":[$1]}";

}
