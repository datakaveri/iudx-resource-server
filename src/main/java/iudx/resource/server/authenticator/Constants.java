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
    public static final List<String> OPEN_ENDPOINTS = List.of("/ngsi-ld/v1/temporal/entities", "/ngsi-ld/v1/entities");
    public static final String AUTH_TIP_PATH = "/auth/v1/token/introspect";
    public static final long TIP_CACHE_TIMEOUT_AMOUNT = 30;
    public static final ChronoUnit TIP_CACHE_TIMEOUT_UNIT = ChronoUnit.MINUTES;
}
