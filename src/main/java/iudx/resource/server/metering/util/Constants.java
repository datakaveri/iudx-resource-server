package iudx.resource.server.metering.util;

public class Constants {

  public static final String ID = "id";
  /* Temporal */
  public static final String START_TIME = "startTime";
  public static final String END_TIME = "endTime";
  public static final String TIME_RELATION = "timeRelation";
  public static final String DURING = "during";

  /* Errors */
  public static final String SUCCESS = "Success";
  public static final String FAILED = "Failed";
  public static final String EMPTY_RESPONSE = "Empty response";
  public static final String DETAIL = "detail";
  public static final String TITLE = "title";
  public static final String RESULTS = "results";
  public static final String STATUS = "status";
  public static final String INVALID_RESOURCE_ID = "Invalid resource id";
  public static final String ROOT_CAUSE = "root_cause";
  public static final String REASON = "reason";

  /* Database */
  public static final String ERROR = "Error";
  public static final String QUERY_KEY = "query";
  public static final String TOTAL = "total";
  public static final String TYPE_KEY = "type";
  public static final String PROVIDER_ID = "providerID";
  public static final String CONSUMER_ID = "consumerID";
  public static final String ENDPOINT = "endPoint";
  public static final String IID = "iid";
  public static final String RESOURCE_ID = "resourceId";

  /* Metering Service Constants*/
  public static final String TIME_RELATION_NOT_FOUND = "Time relation not found.";
  public static final String TIME_NOT_FOUND = "Time interval not found.";
  public static final String USERID_NOT_FOUND = "User Id not found.";
  public static final String INVALID_DATE_TIME = "invalid date-time";
  public static final String INVALID_PROVIDER_ID = "invalid provider id.";
  public static final String INVALID_PROVIDER_REQUIRED = "provider id required.";
  public static final String INVALID_DATE_DIFFERENCE =
      "Difference between dates cannot be greater than 14 days or less than zero day.";
  public static final String RESOURCE_QUERY = " and resourceId='$4';";

  public static final String CONSUMERID_TIME_INTERVAL_COUNT_QUERY =
      "SELECT count() FROM rsauditingtable where epochtime>=$1 and epochtime<=$2 and userid='$3'";

  public static final String PROVIDERID_TIME_INTERVAL_COUNT_QUERY =
      "SELECT count() FROM rsauditingtable where epochtime>=$1 and epochtime<=$2 and providerid='$3'";

  public static final String CONSUMERID_TIME_INTERVAL_READ_QUERY =
      "SELECT * FROM rsauditingtable where epochtime>=$1 and epochtime<=$2 and userid='$3'";

  public static final String PROVIDERID_TIME_INTERVAL_READ_QUERY =
      "SELECT * FROM rsauditingtable where epochtime>=$1 and epochtime<=$2 and providerid='$3'";

  public static final String API_QUERY = " and api='$5'";
  public static final String USER_ID_QUERY = " and userid='$6'";

  public static final String API = "api";
  public static final String USER_ID = "userid";
  public static final String WRITE_QUERY =
      "INSERT INTO rsauditingtable (id,api,userid,epochtime,resourceid,isotime,providerid) VALUES ('$1','$2','$3',$4,'$5','$6','$7')";
  public static final String COUNT_COLUMN_NAME = "(metering.rsauditingtable.col0)";
  public static final String RESOURCEID_COLUMN_NAME = "(metering.rsauditingtable.resourceid)";
  public static final String API_COLUMN_NAME = "(metering.rsauditingtable.api)";
  public static final String USERID_COLUMN_NAME = "(metering.rsauditingtable.userid)";
  public static final String TIME_COLUMN_NAME = "(metering.rsauditingtable.isotime)";

  public static final String MESSAGE = "message";
}
