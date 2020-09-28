package iudx.resource.server.authenticator;

import java.time.temporal.ChronoUnit;
import java.util.List;

public class Constants {
  public static final String CONFIG_FILE = "config.properties";
  public static final String KEYSTORE_PATH = "keystore";
  public static final String KEYSTORE_PASSWORD = "keystorePassword";
  public static final String AUTH_SERVER_HOST = "authServerHost";
  public static final String AUTH_CERTINFO_PATH = "/auth/v1/certificate-info";
  public static final String PUBLIC_TOKEN = "public";
  public static final List<String> OPEN_ENDPOINTS =
      List.of("/ngsi-ld/v1/temporal/entities", "/ngsi-ld/v1/entities", "/ngsi-ld/v1/entityOperations/query");
  public static final String AUTH_TIP_PATH = "/auth/v1/token/introspect";
  public static final List<String> CLOSED_ENDPOINTS =
      List.of("/iudx/v1/adapter", "/ngsi-ld/v1/subscription");
  public static final List<String> MANAGEMENT_ENDPOINTS =
      List.of("/management/queue", "/management/exchange", "/management/vhost");
  public static final String ADAPTER_ENDPOINT = "/iudx/v1/adapter";
  public static final String SUBSCRIPTION_ENDPOINT = "/ngsi-ld/v1/subscription";
  public static final String MANAGEMENT_ENDPOINT = "/management/*";
  public static final long TIP_CACHE_TIMEOUT_AMOUNT = 30;
  public static final ChronoUnit TIP_CACHE_TIMEOUT_UNIT = ChronoUnit.MINUTES;
  public static final String CAT_RSG_PATH = "/iudx/cat/v1/search";
  public static final String SERVER_MODE = "serverMode";
  public static final String JSON_CONSUMER = "consumer"; 
  public static final String JSON_PROVIDER = "provider"; 
  public static final String JSON_TEST_CONSUMER = "vasanth.rajaraman@datakaveri.org";
  public static final String JSON_TEST_PROVIDER_SHA = "datakaveri.org/3ae88e7ba006938ffc38203d326b1711b892226d";
  public static final String JSON_IUDX_ADMIN_SHA = "datakaveri.org/27E503DA0BDDA6EFAE3A52B3EF423C1F9005657A";
  
}
