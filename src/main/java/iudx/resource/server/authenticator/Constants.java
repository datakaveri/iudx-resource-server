package iudx.resource.server.authenticator;

import java.time.temporal.ChronoUnit;
import java.util.List;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class Constants {
  public static final String CONFIG_FILE = "config.properties";
  public static final String KEYSTORE_PATH = "keystore";
  public static final String KEYSTORE_PASSWORD = "keystorePassword";
  public static final String AUTH_SERVER_HOST = "authServerHost";
  public static final String AUTH_CERTINFO_PATH = "/auth/v1/certificate-info";
  public static final String PUBLIC_TOKEN = "public";
  public static final List<String> OPEN_ENDPOINTS =
      List.of(
          "/ngsi-ld/v1/temporal/entities",
          "/ngsi-ld/v1/entities",
          "/ngsi-ld/v1/entityOperations/query",
          "/ngsi-ld/v1/temporal/entityOperations/query",
          "/ngsi-ld/v1/async/status",
          "/ngsi-ld/v1/consumer/audit",
          "/ngsi-ld/v1/async/search");
  public static final String AUTH_TIP_PATH = "/auth/v1/token/introspect";
  public static final List<String> CLOSED_ENDPOINTS =
      List.of("/ngsi-ld/v1/ingestion", "/ngsi-ld/v1/subscription");
  public static final List<String> MANAGEMENT_ENDPOINTS =
      List.of("/management/queue", "/management/exchange", "/management/vhost");
  public static final String ADAPTER_ENDPOINT = "/ngsi-ld/v1/ingestion";
  public static final String SUBSCRIPTION_ENDPOINT = "/ngsi-ld/v1/subscription";
  public static final String MANAGEMENT_ENDPOINT = "/management/*";
  public static final long CACHE_TIMEOUT_AMOUNT = 30;
  public static final ChronoUnit TIP_CACHE_TIMEOUT_UNIT = ChronoUnit.MINUTES;
  public static final String CAT_RSG_PATH = "/iudx/cat/v1/search";
  public static final String CAT_ITEM_PATH = "/iudx/cat/v1/item";
  public static final String SERVER_MODE = "serverMode";
  public static final String JSON_USERID = "userid";
  public static final String JSON_IID = "iid";
  public static final String JSON_CONSUMER = "consumer";
  public static final String JSON_PROVIDER = "provider";
  public static final String JSON_TEST_CONSUMER = "vasanth.rajaraman@datakaveri.org";
  public static final String JSON_TEST_PROVIDER_SHA =
      "datakaveri.org/3ae88e7ba006938ffc38203d326b1711b892226d";
  public static final String JSON_IUDX_ADMIN_SHA =
      "datakaveri.org/27E503DA0BDDA6EFAE3A52B3EF423C1F9005657A";
  public static final JsonObject JSON_PUBLIC_TIP_RESPONSE =
      new JsonObject()
          .put("consumer", "public.data1@iudx.org")
          .put(
              "request",
              new JsonArray().add(new JsonObject().put("id", "iudx.org/public/server/group/*")))
          .put("apis", new JsonArray().add("/iudx/public"));
  public static final String JSON_PUBLIC_CONSUMER = "public.data1@iudx.org";
  public static final String JSON_EXPIRY = "expiry";

  public static final String REVOKED_CLIENT_SQL = "SELECT * FROM revoked_tokens WHERE _id='$1'";
}
