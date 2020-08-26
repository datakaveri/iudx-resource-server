package iudx.resource.server.apiserver;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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
    validParams.add(Constants.NGSILDQUERY_ENTITIES);
    validParams.add(Constants.NGSILDQUERY_GEOQ);
    validParams.add(Constants.NGSILDQUERY_TEMPORALQ);
    // Need to check with the timeProperty in Post Query property for NGSI-LD release v1.3.1
    validParams.add(Constants.NGSILDQUERY_TIME_PROPERTY);
    
    //for IUDX count query
    validParams.add(Constants.IUDXQUERY_OPTIONS);
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
      System.out.println(entry.getKey());
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

  /**
   * validate request parameters.
   * 
   * @param JsonObject requestJson of request parameters
   * @return Future future JsonObject
   */
  public static Future<Boolean> validate(JsonObject requestJson) {
    Promise<Boolean> promise = Promise.promise();
    MultiMap paramsMap = MultiMap.caseInsensitiveMultiMap();

    requestJson.forEach(entry -> {
      if (entry.getKey().equalsIgnoreCase("geoQ") || entry.getKey().equalsIgnoreCase("temporalQ")) {
        JsonObject innerObject = (JsonObject) entry.getValue();
        paramsMap.add(entry.getKey().toString(), entry.getValue().toString());
        innerObject.forEach(innerentry -> {
          paramsMap.add(innerentry.getKey().toString(), innerentry.getValue().toString());
        });
      } else if (entry.getKey().equalsIgnoreCase("entities")) {
        paramsMap.add(entry.getKey().toString(), entry.getValue().toString());
        JsonArray array = (JsonArray) entry.getValue();
        JsonObject innerObject = array.getJsonObject(0);
        innerObject.forEach(innerentry -> {
          paramsMap.add(innerentry.getKey().toString(), innerentry.getValue().toString());
        });
      } else {
        paramsMap.add(entry.getKey().toString(), entry.getValue().toString());
      }

    });
    if (validateParams(paramsMap)) {
      promise.complete(true);
    } else {
      promise.fail(Constants.MSG_INVALID_PARAM);
    }
    return promise.future();
  }
}
