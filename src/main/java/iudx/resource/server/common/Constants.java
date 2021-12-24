package iudx.resource.server.common;

public class Constants {
  
  /** service proxy addresses **/
  public static String PG_SERVICE_ADD="iudx.rs.pgsql.service";
  


  /* Broadcast exchanges and queues */
  public static String TOKEN_INVALID_EX = "rs-token-invalidation";
  public static String TOKEN_INVALID_EX_ROUTING_KEY = "rs-token-invalidation";

  public static String UNIQUE_ATTR_EX = "latest-data-unique-attributes";
  public static String UNIQUE_ATTR_EX_ROUTING_KEY = "unique-attribute";

}
