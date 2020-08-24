package iudx.resource.server.databroker.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class Util {
  private static final Logger LOGGER = LoggerFactory.getLogger(Util.class);

  /**
   * encode string using URLEncoder's encode method.
   * 
   * @param value which is a String
   * @return encoded_value which is a String
   **/
  public static String encodedValue(String value) {
    String encodedVhost = null;
    try {
      encodedVhost = URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
    } catch (UnsupportedEncodingException ex) {
      LOGGER.error("Error in encode vhost name :" + ex.getCause());
    }
    return encodedVhost;
  }

}
