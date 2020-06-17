package iudx.resource.server.databroker;

public class Constants {
  public static final String EXCHANGE_TYPE = "topic";
  /* queue additional parameters start */
  public static final long X_MESSAGE_TTL = 86400000; // 24hours
  public static final int X_MAXLENGTH = 100;
  public static final String X_QUEQUE_MODE = "lazy";
  public static final boolean EXCHANGE_DURABLE_TRUE = true;
  public static final boolean EXCHANGE_AUTODELETE_FALSE = false;
  /* queue additional parameters end */

  // Queue name constants
  public static final String QUEUE_DATA = "database";
  public static final String QUEUE_ADAPTOR_LOGS = "adaptorLogs";

  public static final int PASSWORD_LENGTH = 10;

}
