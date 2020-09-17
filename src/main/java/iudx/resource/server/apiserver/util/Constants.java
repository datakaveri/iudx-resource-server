package iudx.resource.server.apiserver.util;

public class Constants {

  // date-time format
  public static final String APP_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss[.SSSSSS]'Z'";
  public static final String APP_NAME_REGEX = "[a-zA-Z0-9._\\-]*$";

  public static final String APP_TEST_NAME = "vasanth";
  public static final String APP_TEST_CONSUMER = "vasanth@iudx.org";
  public static final String API_ENDPOINT = "apiEndpoint";
  public static final String API_METHOD = "method";
  public static final String ID = "id";
  
  // config
  public static final String CONFIG_FILE = "config.properties";
  
  // NGSI-LD endpoints
  public static final String NGSILD_BASE_PATH = "/ngsi-ld/v1";
  public static final String NGSILD_ENTITIES_URL = NGSILD_BASE_PATH + "/entities";
  public static final String NGSILD_TEMPORAL_URL = NGSILD_BASE_PATH + "/temporal/entities";
  public static final String NGSILD_SUBSCRIPTION_URL = NGSILD_BASE_PATH + "/subscription";
  public static final String NGSILD_POST_QUERY_PATH = NGSILD_BASE_PATH + "/entityOperations/query";

  // IUDX management endpoints
  public static final String IUDX_MANAGEMENT_URL = "/management";
  public static final String IUDX_ADAPTOR_URL = "/iudx/v1";
  public static final String IUDX_MANAGEMENT_EXCHANGE_URL = IUDX_MANAGEMENT_URL + "/exchange";
  public static final String IUDX_MANAGEMENT_QUEUE_URL = IUDX_MANAGEMENT_URL + "/queue";
  public static final String IUDX_MANAGEMENT_BIND_URL = IUDX_MANAGEMENT_URL + "/bind";
  public static final String IUDX_MANAGEMENT_UNBIND_URL = IUDX_MANAGEMENT_URL + "/unbind";
  public static final String IUDX_MANAGEMENT_VHOST_URL = IUDX_MANAGEMENT_URL + "/vhost";
  public static final String IUDX_MANAGEMENT_ADAPTER_URL = IUDX_ADAPTOR_URL + "/adapter";

  /** API Documentation endpoint */
  public static final String ROUTE_STATIC_SPEC = "/apis/spec";
  public static final String ROUTE_DOC = "/apis";

  /** Accept Headers and CORS */
  public static final String MIME_APPLICATION_JSON = "application/json";
  public static final String MIME_TEXT_HTML = "text/html";

  // ngsi-ld/IUDX query paramaters
  public static final String NGSILDQUERY_ID = "id";
  public static final String NGSILDQUERY_IDPATTERN = "idpattern";
  public static final String NGSILDQUERY_TYPE = "type";
  public static final String NGSILDQUERY_COORDINATES = "coordinates";
  public static final String NGSILDQUERY_GEOMETRY = "geometry";
  public static final String NGSILDQUERY_ATTRIBUTE = "attrs";
  public static final String NGSILDQUERY_GEOREL = "georel";
  public static final String NGSILDQUERY_TIMEREL = "timerel";
  public static final String NGSILDQUERY_TIME = "time";
  public static final String NGSILDQUERY_ENDTIME = "endtime";
  public static final String NGSILDQUERY_Q = "q";
  public static final String NGSILDQUERY_GEOPROPERTY = "geoproperty";
  public static final String NGSILDQUERY_TIMEPROPERTY = "timeproperty";
  public static final String NGSILDQUERY_MAXDISTANCE = "maxdistance";
  public static final String NGSILDQUERY_MINDISTANCE = "mindistance";
  public static final String IUDXQUERY_OPTIONS = "options";
  public static final String NGSILDQUERY_ENTITIES = "entities";
  public static final String NGSILDQUERY_GEOQ = "geoQ";
  public static final String NGSILDQUERY_TEMPORALQ = "temporalQ";
  public static final String NGSILDQUERY_TIME_PROPERTY = "timeProperty";
  
  // Header params
  public static final String HEADER_TOKEN = "token";
  public static final String HEADER_HOST = "Host";
  public static final String HEADER_ACCEPT = "Accept";
  public static final String HEADER_CONTENT_LENGTH = "Content-Length";
  public static final String HEADER_CONTENT_TYPE = "Content-Type";
  public static final String HEADER_ORIGIN = "Origin";
  public static final String HEADER_REFERER = "Referer";
  public static final String HEADER_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
  public static final String HEADER_OPTIONS = "options";

  public static final String COUNT_HEADER = "Count";
  public static final String PUBLIC_TOKEN = "public";

  // request/response params
  public static final String CONTENT_TYPE = "content-type";
  public static final String APPLICATION_JSON = "application/json";
  public static final String SUBSCRIPTION_ID = "subscriptionID";
  public static final String EXCHANGE_ID = "exId";

  // json fields
  public static final String JSON_INSTANCEID = "instanceID";
  public static final String JSON_CONSUMER = "consumer";
  public static final String JSON_PROVIDER = "provider";
  public static final String JSON_TYPE = "type";
  public static final String JSON_NAME = "name";
  public static final String JSON_ENTITIES = "entities";
  public static final String JSON_ID = "id";
  public static final String JSON_ATTRIBUTE_FILTER = "attrs";
  public static final String JSON_NEAR = "near";
  public static final String JSON_LAT = "lat";
  public static final String JSON_LON = "lon";
  public static final String JSON_RADIUS = "radius";
  public static final String JSON_GEOMETRY = "geometry";
  public static final String JSON_COORDINATES = "coordinates";
  public static final String JSON_GEOREL = "georel";
  public static final String JSON_WITHIN = "within";
  public static final String JSON_MAXDISTANCE = "maxdistance";
  public static final String JSON_MINDISTANCE = "mindistance";
  public static final String JSON_DURING = "during";
  public static final String JSON_TIME = "time";
  public static final String JSON_ENDTIME = "endtime";
  public static final String JSON_TIMEREL = "timerel";
  public static final String JSON_ATTR_QUERY = "attr-query";
  public static final String JSON_GEOPROPERTY = "geoproperty";
  public static final String JSON_ATTRIBUTE = "attribute";
  public static final String JSON_OPERATOR = "operator";
  public static final String JSON_VALUE = "value";
  public static final String JSON_TITLE = "title";
  public static final String JSON_DETAIL = "detail";
  public static final String JSON_EXCHANGE_NAME = "exchangeName";
  public static final String JSON_QUEUE_NAME = "queueName";
  public static final String JSON_VHOST_NAME = "vHostName";
  public static final String JSON_VHOST = "vHost";
  public static final String JSON_VHOST_ID = "vhostId";
  public static final String JSON_DOMAIN = "domain";
  public static final String JSON_USERSHA = "userSHA";
  public static final String JSON_ALIAS = "alias";
  public static final String JSON_STREAMING_TYPE = "streaming";
  public static final String JSON_EXCHANGE = "exchange";
  public static final String JSON_QUEUE = "queue";
  public static final String JSON_USERNAME = "username";
  public static final String JSON_APIKEY = "apiKey";
  public static final String JSON_STATUS = "status";
  public static final String JSON_STATUS_HEARTBEAT = "heartbeat";
  public static final String JSON_STATUS_SERVERISSUE = "Server Issue";
  public static final String JSON_STATUS_DATAISSUE = "Server Issue";
  public static final String JSON_STREAMING_NAME = "test-streaming-name";
  public static final String JSON_SUBS_ID = "subscriptionID";
  public static final String JSON_COUNT = "Count";
  public static final String JSON_URL = "url";
  public static final String JSON_METHOD = "method";
  public static final String JSON_PASSWORD = "password";
  public static final String JSON_RESOURCE_SERVER = "resourceServer";
  public static final String JSON_RESOURCE_GROUP = "resourceGroup";
  public static final String JSON_RESOURCE_NAME = "resourceName";
  
  // searchtype
  public static final String JSON_SEARCH_TYPE = "searchType";
  public static final String JSON_TEMPORAL_SEARCH = "temporalSearch_";
  public static final String JSON_GEO_SEARCH = "geoSearch_";
  public static final String JSON_RESPONSE_FILTER_SEARCH = "responseFilter_";
  public static final String JSON_ATTRIBUTE_SEARCH = "attributeSearch_";
  public static final String JSON_LATEST_SEARCH = "latestSearch_";

  // Geometry
  public static final String GEOM_POINT = "point";
  public static final String GEOM_POLYGON = "polygon";
  public static final String GEOM_LINESTRING = "linestring";

  // subscription
  public static final String SUBSCRIPTION = "subscription";
  public static final String SUB_TYPE = "subscriptionType";
  public static final String SUB_STREAMING = "streaming";
  public static final String SUB_CALLBACK = "callback";
  public static final String SUB_STREAMING_URL = "streamingURL";


  // messages (Error, Exception, messages..)
  public static final String MSG_INVALID_PARAM = "Invalid parameter in request.";
  public static final String MSG_PARAM_DECODE_ERROR = "Error while decoding query params.";
  public static final String MSG_INVALID_EXCHANGE_NAME = "Invalid exchange name";
  public static final String MSG_INVALID_QUEUE_NAME = "Invalid queue name";
  public static final String MSG_INVALID_VHOST_NAME = "Invalid vhost name";
  public static final String MSG_INVALID_NAME = "Invalid name.";
  public static final String MSG_FAILURE = "failure";
  public static final String MSG_FAILURE_NO_VHOST = "No vhosts found";
  public static final String MSG_FAILURE_VHOST_EXIST = "vHost already exists";
  public static final String MSG_FAILURE_EXCHANGE_NOT_FOUND = "Exchange not found";
  public static final String MSG_FAILURE_QUEUE_NOT_EXIST = "Queue does not exist";
  public static final String MSG_FAILURE_QUEUE_EXIST = "Queue already exists";
  public static final String MSG_EXCHANGE_EXIST = "Exchange already exists";
  public static final String MSG_SUB_TYPE_NOT_FOUND = "Subscription type not present in body";
  public static final String MSG_SUB_INVALID_TOKEN = "Invalid/no token found in header";

  // results
  public static final String SUCCCESS = "success";

}
