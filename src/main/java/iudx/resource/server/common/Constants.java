package iudx.resource.server.common;

public class Constants {

  /** service proxy addresses **/
  public static final String PG_SERVICE_ADDRESS = "iudx.rs.pgsql.service";
  public static final String CACHE_SERVICE_ADDRESS = "iudx.rs.cache.service";
  public static final String LATEST_SERVICE_ADDRESS = "iudx.rs.latest.service";
  public static final String AUTH_SERVICE_ADDRESS = "iudx.rs.authentication.service";
  public static final String ASYNC_SERVICE_ADDRESS = "iudx.rs.async.service";
  public static final String DATABASE_SERVICE_ADDRESS = "iudx.rs.database.service";
  public static final String BROKER_SERVICE_ADDRESS = "iudx.rs.broker.service";



  /* Broadcast exchanges and queues */
  public static String TOKEN_INVALID_EX = "rs-token-invalidation";
  public static String TOKEN_INVALID_EX_ROUTING_KEY = "rs-token-invalidation";
  public static String TOKEN_INVALID_Q = "invalid-tokens";

  public static String UNIQUE_ATTR_EX = "latest-data-unique-attributes";
  public static String UNIQUE_ATTR_EX_ROUTING_KEY = "unique-attribute";
  public static String UNIQUE_ATTR_Q = "unique-attribute";


  // postgres queries
  public static String SELECT_REVOKE_TOKEN_SQL = "SELECT * FROM revoked_tokens";
  public static String SELECT_UNIQUE_ATTRIBUTE = "SELECT * from unique_attributes";

}
