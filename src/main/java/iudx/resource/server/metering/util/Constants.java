package iudx.resource.server.metering.util;

public class Constants {

  public static final String ID = "id";
  public static final String TIME = "time";
  public static final String EXCHANGE_NAME = "auditing";
  public static final String ROUTING_KEY = "#";
  /* Temporal */
  public static final String START_TIME = "startTime";
  public static final String END_TIME = "endTime";
  public static final String TIME_RELATION = "timeRelation";
  public static final String DURING = "during";
  public static final String BETWEEN = "between";
  public static final String CONSUMER = "consumer";
  public static final String WHERE = "where";

  /* Errors */
  public static final String SUCCESS = "Success";
  public static final String FAILED = "Failed";
  public static final String DETAIL = "detail";
  public static final String TITLE = "title";
  public static final String RESULTS = "results";
  public static final String STATUS = "status";
  public static final String TABLE_NAME = "tableName";

  /* Database */
  public static final String ERROR = "Error";
  public static final String QUERY_KEY = "query";
  public static final String TOTAL = "total";
  public static final String RESPONSE_ARRAY= "responseArray";
  public static final String TOTAL_HITS= "totalHits";
  public static final String TYPE_KEY = "type";
  public static final String PROVIDER_ID = "providerID";
  public static final String CONSUMER_ID = "consumerID";
  public static final String ENDPOINT = "endPoint";
  public static final String IID = "iid";
  public static final String RESOURCE_ID = "resourceId";
  public static final String PRIMARY_KEY= "primaryKey";
  public static final String EPOCH_TIME = "epochTime";
  public static final String ISO_TIME = "isoTime";
  public static final String ORIGIN = "origin";
  public static final String ORIGIN_SERVER = "rs-server";
  public static final String LATEST_ID = "latestId";
  public static final String LAST_ID= "lastId";

  /* Metering Service Constants*/
  public static final String TIME_RELATION_NOT_FOUND = "Time relation not found.";
  public static final String TIME_NOT_FOUND = "Time interval not found.";
  public static final String USERID_NOT_FOUND = "User Id not found.";
  public static final String INVALID_DATE_TIME = "invalid date-time";
  public static final String INVALID_PROVIDER_ID = "invalid provider id.";
  public static final String INVALID_PROVIDER_REQUIRED = "provider id required.";
  public static final String INVALID_DATE_DIFFERENCE =
      "Difference between dates cannot be greater than 14 days or less than 1 Minute.";
  public static final String RESPONSE_LIMIT_EXCEED = "Requested time range exceeds response limit";
  public static final String RESOURCE_QUERY = " and resourceId='$4'";

  public static final String CONSUMERID_TIME_INTERVAL_COUNT_QUERY =
      "SELECT count(*) FROM $0 where epochtime>=$1 and epochtime<=$2 and userid='$3'";

  public static final String PROVIDERID_TIME_INTERVAL_COUNT_QUERY =
      "SELECT count(*) FROM $0 where epochtime>=$1 and epochtime<=$2 and providerid='$3'";

  public static final String CONSUMERID_TIME_INTERVAL_READ_QUERY =
      "SELECT * FROM $0 where epochtime>=$1 and epochtime<=$2 and userid='$3'";

  public static final String PROVIDERID_TIME_INTERVAL_READ_QUERY =
      "SELECT * FROM $0 where epochtime>=$1 and epochtime<=$2 and providerid='$3'";

  public static final String ORDER_BY_AND_LIMIT= " ORDER BY ID LIMIT 999;";

  public static final String COUNT="count";
  public static final String API_QUERY = " and api='$5'";
  public static final String USER_ID_QUERY = " and userid='$6'";

  public static final String ID_QUERY=" id>'$7' and";
  public static final String API = "api";
  public static final String USER_ID = "userid";
  public static final StringBuilder COUNT_COLUMN = new StringBuilder("col0)");
  public static final StringBuilder RESOURCEID_COLUMN = new StringBuilder("resourceid)");
  public static final StringBuilder API_COLUMN = new StringBuilder("api)");
  public static final StringBuilder USERID_COLUMN = new StringBuilder("userid)");
  public static final StringBuilder TIME_COLUMN = new StringBuilder("isotime)");
  public static final StringBuilder RESPONSE_SIZE_COLUMN = new StringBuilder("size)");

  public static final StringBuilder ID_COLUMN = new StringBuilder("id)");
  public static final String MESSAGE = "message";

  public static final String MONTHLY_OVERVIEW_QUERY = "select to_char(date_trunc('month', time),'month') as month\n" +
          ",extract('year' from time) as year,\n" +
          "count(api) as count from auditing_rs\n" +
          "where time between "+ "'$0'" + " and " + "'$1'";
  public static final String MONTHLY_OVERVIEW_GROUPBY = "\ngroup by month,year\n" +
          "order by month";

  }
