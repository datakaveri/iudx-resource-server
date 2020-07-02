package iudx.resource.server.apiserver;

import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;

/**
 * This class is used to validate NGSI-LD request and request parameters.
 *
 */
public class Validator {

  private static Set<String> validParams = new HashSet<String>();

  static {
    validParams.add("type");
    validParams.add("id");
    validParams.add("idpattern");
    validParams.add("attrs");
    validParams.add("q");
    validParams.add("georel");
    validParams.add("geometry");
    validParams.add("coordinates");
    validParams.add("geoproperty");
    validParams.add("timeproperty");
    validParams.add("time");
    validParams.add("timerel");
    validParams.add("time");
    validParams.add("endtime");
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
        /*
         * response.putHeader("content-type", "application/json")
         * .setStatusCode(ResponseType.BadRequestData.getCode()) .end(new
         * RestResponse.Builder().withError(ResponseType.BadRequestData)
         * .withMessage(entry.getKey() +
         * " is not valid parameter.").build().toJsonString());
         *
         */
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
      promise.fail("invalid parameter");
    }
    return promise.future();
  }

}
