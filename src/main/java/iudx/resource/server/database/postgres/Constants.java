package iudx.resource.server.database.postgres;

public class Constants {

  public static String INSERT_REVOKE_TOKEN_SQL =
      "INSERT INTO revoked_tokens(client_id,rs_url,token,expiry) VALUES('$1','$2','$3','$4')";

  public static String INSERT_UNIQUE_ATTR_SQL =
      "INSERT INTO unique_attributes(resource_id,unique_attribute) VALUES($1,$2)";
}
