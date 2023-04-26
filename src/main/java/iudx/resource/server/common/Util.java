package iudx.resource.server.common;

import static iudx.resource.server.apiserver.util.Constants.APP_NAME_REGEX;
import static iudx.resource.server.apiserver.util.Constants.MSG_INVALID_NAME;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import java.util.regex.Pattern;

public class Util {

  /**
   * validate if name passes the regex test for IUDX queue,exchage name.
   *
   * @param name name(queue,exchange)
   * @return Future true if name matches the regex else false
   */
  public static Future<Boolean> isValidName(String name) {
    Promise<Boolean> promise = Promise.promise();
    if (Pattern.compile(APP_NAME_REGEX).matcher(name).matches()) {
      promise.complete(true);
    } else {
      promise.fail(MSG_INVALID_NAME);
    }
    return promise.future();
  }
}
