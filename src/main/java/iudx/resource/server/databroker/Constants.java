package iudx.resource.server.databroker;

public class Constants {
  public static final String EXCHANGE_TYPE = "topic";
  /* queue additional parameters start */
  public static final long X_MESSAGE_TTL = 86400000; // 24hours
  public static final int X_MAXLENGTH = 100;
  public static final String X_QUEQUE_MODE = "lazy";
  /* queue additional parameters end */
  public static final int PASSWORD_LENGTH = 10;
}
