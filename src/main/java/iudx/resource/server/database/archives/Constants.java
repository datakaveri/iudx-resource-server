package iudx.resource.server.database.archives;

public class Constants {
  /* General Purpose */
  public static final String SEARCH_TYPE = "searchType";
  public static final String ID = "id";
  public static final String RESOURCE_ID_KEY = "id";
  public static final String PROD_INSTANCE = "production";
  public static final String TEST_INSTANCE = "test";
  /* Database */
  public static final String GEO_KEY = "location";
  public static final String GEO_CIRCLE = "circle";
  public static final String GEO_BBOX = "envelope";
  public static final String COORDINATES_KEY = "coordinates";
  public static final String GEO_RELATION_KEY = "relation";
  public static final String TYPE_KEY = "type";
  public static final String GEO_SHAPE_KEY = "geo_shape";
  public static final String GEO_RADIUS = "radius";
  public static final String SHAPE_KEY = "shape";
  public static final String QUERY_KEY = "query";
  public static final String FILTER_KEY = "filter";
  public static final String BOOL_KEY = "bool";
  public static final String VARANASI_TEST_SEARCH_INDEX = "varanasi/_search";
  public static final String VARANASI_TEST_COUNT_INDEX = "varanasi/_count";
  public static final String LATEST_RESOURCE_INDEX = "latest/_mget";
  public static final String SOURCE_FILTER_KEY = "_source";
  public static final String RANGE_KEY = "range";
  public static final String TERM_KEY = "term";
  public static final String TERMS_KEY = "terms";
  public static final String FILTER_PATH = "filter_path";
  public static final String FILTER_PATH_VAL = "took,hits.hits._source";
  public static final String FILTER_PATH_VAL_LATEST = "docs._source";
  public static final String SIZE_KEY = "size";
  public static final String GREATER_THAN = "gt";
  public static final String LESS_THAN = "lt";
  public static final String GREATER_THAN_EQ = "gte";
  public static final String LESS_THAN_EQ = "lte";
  public static final String MUST_NOT = "must_not";
  public static final String REQUEST_GET = "GET";
  public static final String HITS = "hits";
  public static final String SEARCH_KEY = "search";
  public static final String ERROR = "Error";
  public static final String COUNT = "count";
  public static final String DOC_ID = "_id";
  public static final String DOCS_KEY = "docs";
  public static final String SEARCH_REQ_PARAM = "/_search";
  public static final String COUNT_REQ_PARAM = "/_count";
  public static final String COUNT_REQ_PARAM_WITHOUT_FILTER = "/_search?search_type=count";
  public static final String TIME_FIELD_DB = "observationDateTime";
  public static final String FROM_KEY = "from";
  /* Request Params */
  /* Temporal */
  public static final String REQ_TIMEREL = "timerel";
  public static final String TIME_KEY = "time";
  public static final String END_TIME = "endtime";
  public static final String DURING = "during";
  public static final String AFTER = "after";
  public static final String BEFORE = "before";
  public static final String TEQUALS = "tequals";
  public static final String TIME_LIMIT = "timeLimit";
  /* Geo-Spatial */
  public static final String LAT = "lat";
  public static final String LON = "lon";
  public static final String GEOMETRY = "geometry";
  public static final String GEOREL = "georel";
  public static final String WITHIN = "within";
  public static final String POLYGON = "polygon";
  public static final String LINESTRING = "linestring";
  public static final String GEO_PROPERTY = "geoproperty";
  public static final String BBOX = "bbox";
  /* Response Filter */
  public static final String RESPONSE_ATTRS = "attrs";
  /* Attribute */
  public static final String ATTRIBUTE_QUERY_KEY = "attr-query";
  public static final String ATTRIBUTE_KEY = "attribute";
  public static final String OPERATOR = "operator";
  public static final String VALUE = "value";
  public static final String VALUE_LOWER = "valueLower";
  public static final String VALUE_UPPER = "valueUpper";
  public static final String GREATER_THAN_OP = ">";
  public static final String LESS_THAN_OP = "<";
  public static final String GREATER_THAN_EQ_OP = ">=";
  public static final String LESS_THAN_EQ_OP = "<=";
  public static final String EQUAL_OP = "==";
  public static final String NOT_EQUAL_OP = "!=";
  public static final String BETWEEN_OP = "<==>";
  
  /*pagination*/
  public static final String PARAM_SIZE = "limit";
  public static final String PARAM_FROM = "offset";
  
  /* Errors */
  public static final String INVALID_OPERATOR = "Invalid operator";
  public static final String INVALID_SEARCH = "Invalid search request";
  public static final String INVALID_DATE = "Invalid date format";
  public static final String MISSING_ATTRIBUTE_FIELDS = "Missing attribute query fields";
  public static final String MISSING_TEMPORAL_FIELDS = "Missing/Invalid temporal parameters";
  public static final String MISSING_RESPONSE_FILTER_FIELDS =
      "Missing/Invalid responseFilter parameters";
  public static final String MISSING_GEO_FIELDS = "Missing/Invalid geo parameters";
  public static final String COORDINATE_MISMATCH = "Coordinate mismatch (Polygon)";
  public static final String COUNT_UNSUPPORTED = "Count is not supported with filtering";
  public static final String EMPTY_RESPONSE = "Empty response";
  public static final String DB_ERROR = "DB request has failed";
  public static final String DB_ERROR_2XX = "Status code is not 2xx";
  public static final String ID_NOT_FOUND = "No id found";
  public static final String EMPTY_RESOURCE_ID = "resource-id is empty";
  public static final String SEARCHTYPE_NOT_FOUND = "No searchType found";
  public static final String BAD_PARAMETERS = "Bad parameters";
  public static final String ERROR_TYPE = "type";
  public static final String SUCCESS = "Success";
  public static final String FAILED = "Failed";
  public static final String TITLE = "title";
  public static final String RESULTS = "results";
  public static final String DETAIL = "detail";
  public static final String ROOT_CAUSE = "root_cause";
  public static final String REASON = "reason";
  public static final String MALFORMED_ID = "Malformed Id ";
  public static final String STATUS = "status";
  public static final String INDEX_NOT_FOUND = "index_not_found_exception";
  public static final String INVALID_RESOURCE_ID = "Invalid resource id";
  /* Search Regex */
  public static final String GEOSEARCH_REGEX = "(.*)geoSearch(.*)";
  public static final String RESPONSE_FILTER_REGEX = "(.*)responseFilter(.*)";
  public static final String ATTRIBUTE_SEARCH_REGEX = "(.*)attributeSearch(.*)";
  public static final String TEMPORAL_SEARCH_REGEX = "(.*)temporalSearch(.*)";
  public static final String LATEST_SEARCH = "latestSearch";
  /* Query templates */
  public static final String GEO_SHAPE_QUERY =
      "{ \"geo_shape\": { \"$4\": { \"shape\": { \"type\": \"$1\", \"coordinates\": $2 },"
          + " \"relation\": \"$3\" } } }";
  public static final String TIME_QUERY = "{\"range\":{\"observationDateTime\":{\"$1\":\"$2\"}}}";
  public static final String TERM_QUERY = "{\"term\":{\"$1\":\"$2\"}}";
  public static final String TERMS_QUERY = "{\"terms\":{\"$1\":$2}}";
  public static final String RANGE_QUERY = "{\"range\":{\"$1\":{\"$2\":$3}}}";
  public static final String RANGE_QUERY_BW = "{\"range\":{\"$1\":{\"$2\":$3,\"$4\":$5}}}";
  public static final String MUST_NOT_QUERY = "{\"must_not\":[$1]}";
  /* Latest Data Params */
  public static final String OPTIONS_NOT_FOUND = "options not found";
  public static final String OPTIONS = "options";
  public static final String EMPTY_OPTIONS = "options is empty";
  public static final String ATTRIBUTE_LIST = "attributeList";
  public static final String DEFAULT_ATTRIBUTE = "_d";
  public static final String KEY = "key";
  public static final String PATH_PARAM = "pathParam";
  public static final String GROUP = "group";
  public static final String INVALID_LATEST_QUERY = "invalid latest params";
  public static final String ATTRIBUTE_LIST_NOT_FOUND = "key [attributeList] not found";
  public static final String REDIS_ERROR = "Redis Error!";
  public static final String INVALID_OPTIONS = "invalid options for latest";
  // needs modification depending on the actual error returned from Redis
  public static final String ID_NOT_PRESENT = "Not found";


  // pagination

  public static final int DEFAULT_SIZE_VALUE = 5000;
  public static final int DEFAULT_FROM_VALUE = 0;
  public static final String COUNT_MATCH_ALL_QUERY="{\"query\": { \"match_all\": {} }}";
}
