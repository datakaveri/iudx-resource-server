package iudx.resource.server.apiserver;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import iudx.resource.server.apiserver.util.Constants;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 * This class is used to validate NGSI-LD request and request parameters.
 *
 */
public class Validator {

  private static Set<String> validParams = new HashSet<String>();

  static {
    validParams.add(Constants.NGSILDQUERY_TYPE);
    validParams.add(Constants.NGSILDQUERY_ID);
    validParams.add(Constants.NGSILDQUERY_IDPATTERN);
    validParams.add(Constants.NGSILDQUERY_ATTRIBUTE);
    validParams.add(Constants.NGSILDQUERY_Q);
    validParams.add(Constants.NGSILDQUERY_GEOREL);
    validParams.add(Constants.NGSILDQUERY_GEOMETRY);
    validParams.add(Constants.NGSILDQUERY_COORDINATES);
    validParams.add(Constants.NGSILDQUERY_GEOPROPERTY);
    validParams.add(Constants.NGSILDQUERY_TIMEPROPERTY);
    validParams.add(Constants.NGSILDQUERY_TIME);
    validParams.add(Constants.NGSILDQUERY_TIMEREL);
    validParams.add(Constants.NGSILDQUERY_TIME);
    validParams.add(Constants.NGSILDQUERY_ENDTIME);
    // validParams.add("");
  }

  /**
   * Validate a http request.
   * 
   * @param parameterMap parameters map of request query
   * @param response     HttpServerResponse object
   */
  private static boolean validateParams(MultiMap parameterMap) {
    final List<Entry<String, String>> entries = parameterMap.entries();
    for (final Entry<String, String> entry : entries) {
      if (!validParams.contains(entry.getKey())) {
        return false;
      }
    }
    return true;
  }

  /**
   * validate request parameters.
   * 
   * @param paramsMap map of request parameters
   * @return Future future JsonObject
   */
  public static Future<Boolean> validate(MultiMap paramsMap) {
    Promise<Boolean> promise = Promise.promise();
    if (validateParams(paramsMap)) {
      promise.complete(true);
    } else {
      promise.fail(Constants.MSG_INVALID_PARAM);
    }
    return promise.future();
  }

}
