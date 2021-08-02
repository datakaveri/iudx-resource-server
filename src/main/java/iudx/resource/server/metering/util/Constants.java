package iudx.resource.server.metering.util;

public class Constants {

  public static final String ID = "id";
  /* Temporal */
  public static final String START_TIME = "startTime";
  public static final String END_TIME = "endtime";

  /* Errors */
  public static final String SUCCESS = "Success";
  public static final String FAILED = "Failed";
  public static final String EMPTY_RESPONSE = "Empty response";
  public static final String DETAIL = "detail";
  public static final String ERROR_TYPE = "type";
  public static final String TITLE = "title";
  public static final String RESULTS = "results";
  public static final String STATUS = "status";
  public static final String INDEX_NOT_FOUND = "index_not_found_exception";
  public static final String INVALID_RESOURCE_ID = "Invalid resource id";
  public static final String ROOT_CAUSE = "root_cause";
  public static final String REASON = "reason";

  /* Database */
  public static final String ERROR = "Error";
  public static final String QUERY_KEY = "query";
  public static final String COUNT = "count";
  public static final String TYPE_KEY = "type";
  public static final String FROM_KEY = "from";
  public static final String SIZE_KEY = "size";

  /* Metering Service Constants*/

  public static final String TIME_NOT_FOUND = "Time interval not found";
  public static final String EMAIL_ID = "emailId";
  public static final String INVALID_DATE_TIME = "invalid date-time";
  public static final String TIME_INTERVAL_QUERY =
      "SELECT count() FROM meteringtable where time>=$1 and time<=$2";
  public static final String EMAIL_QUERY = " and email='$3'";
  public static final String RESOURCE_QUERY = " and resourceId='$4'";
  public static final String API = "api";
  public static final String WRITE_QUERY =
      "INSERT INTO meteringtable (id,time,resourceid,api,email) VALUES ('$1',$2,'$3','$4','$5')";
  public static final String COLUMN_NAME = "(metering.meteringtable.col0)";

  public static final String MESSAGE = "message";
}
