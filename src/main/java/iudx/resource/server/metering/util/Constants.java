package iudx.resource.server.metering.util;

public class Constants {

  public static final String ID = "id";
  /* Temporal */
  public static final String START_TIME = "startTime";
  public static final String END_TIME = "endtime";
  public static final String TIME_RELATION = "timeRelation";
  public static final String TIME_REL = "timerel";

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

  /* Metering Service Constants*/
  public static final String TIME_RELATION_NOT_FOUND = "Time relation not found.";
  public static final String TIME_NOT_FOUND = "Time interval not found.";
  public static final String USERID_NOT_FOUND = "User Id not found.";
  public static final String INVALID_DATE_TIME = "invalid date-time";
  public static final String INVALID_DATE_DIFFERENCE =
      "Difference between dates cannot be greater than 14 days or less than zero day.";
  public static final String RESOURCE_QUERY = " and resourceId='$4';";

  public static final String USERID_TIME_INTERVAL_COUNT_QUERY =
      "SELECT count() FROM auditing_table where time>=$1 and time<=$2 and userid='$3'";

  public static final String USERID_TIME_INTERVAL_READ_QUERY =
      "SELECT * FROM auditing_table where time>=$1 and time<=$2 and userid='$3'";
  public static final String API_QUERY = " and api='$5'";

  public static final String API = "api";
  public static final String USER_ID = "userid";
  public static final String WRITE_QUERY =
      "INSERT INTO auditing_table (id,api,userid,time,resourceid,isotime) VALUES ('$1','$2','$3',$4,'$5','$6')";
  public static final String COUNT_COLUMN_NAME = "(metering.auditing_table.col0)";
  public static final String RESOURCEID_COLUMN_NAME = "(metering.auditing_table.resourceid)";
  public static final String API_COLUMN_NAME = "(metering.auditing_table.api)";
  public static final String USERID_COLUMN_NAME = "(metering.auditing_table.userid)";
  public static final String TIME_COLUMN_NAME = "(metering.auditing_table.isotime)";

  public static final String MESSAGE = "message";
}
