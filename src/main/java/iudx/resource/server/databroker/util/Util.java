package iudx.resource.server.databroker.util;

import static iudx.resource.server.databroker.util.Constants.DETAIL;
import static iudx.resource.server.databroker.util.Constants.TITLE;
import static iudx.resource.server.databroker.util.Constants.TYPE;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.codec.digest.DigestUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Util {
  private static final Logger LOGGER = LogManager.getLogger(Util.class);

  /**
   * encode string using URLEncoder's encode method.
   * 
   * @param value which is a String
   * @return encoded_value which is a String
   **/
  public static String encodedValue(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  /**
   * This method is as simple as but it can have more sophisticated encryption logic.
   * 
   * @param plainUserName which is a String
   * @return encodedValue which is a String
   **/
  public static String getSha(String plainUserName) {
    String encodedValue = null;
    try {
      encodedValue = DigestUtils.sha1Hex(plainUserName);
    } catch (Exception e) {
      LOGGER.error("Unable to encode username using SHA" + e.getLocalizedMessage());
    }
    return encodedValue;
  }

  /**
   * This method generate random alphanumeric password of given PASSWORD_LENGTH.
   **/
  public static String generateRandomPassword() {
    // It is simple one. here we may have strong algorithm for password generation.
    return org.apache.commons.lang3.RandomStringUtils.random(Constants.PASSWORD_LENGTH, true, true);
  }

  public static Supplier<String> randomPassword = () -> org.apache.commons.lang3.RandomStringUtils
      .random(Constants.PASSWORD_LENGTH, true, true);

  /**
   * TODO This method checks the if for special characters other than hyphen, A-Z, a-z and 0-9.
   **/

  public static boolean isValidID(String id) {
    /* Check if id contains any special character */
    Pattern allowedPattern = Pattern.compile("[^-_.a-z0-9 ]", Pattern.CASE_INSENSITIVE);
    Matcher isInvalid = allowedPattern.matcher(id);
    return !isInvalid.find();
    /*
     * if (isInvalid.find()) { LOGGER.info("Invalid ID" + id); return false; } else {
     * LOGGER.info("Valid ID" + id); return true; }
     */
  }

  public static Predicate<String> isValidId=(id)->{
    Pattern allowedPattern = Pattern.compile("[^-_.a-z0-9 ]", Pattern.CASE_INSENSITIVE);
    Matcher isInvalid = allowedPattern.matcher(id);
    return !isInvalid.find();
  };


  public static BinaryOperator<JsonArray> bindingMergeOperator = (key1, key2) -> {
    JsonArray mergedArray = new JsonArray();
    mergedArray.clear().addAll(((JsonArray) key1)).addAll(((JsonArray) key2));
    return mergedArray;
  };

  public static JsonObject getResponseJson(int type, String title, String detail) {
    JsonObject json = new JsonObject();
    json.put(TYPE, type);
    json.put(TITLE, title);
    json.put(DETAIL, detail);
    return json;
  }
}