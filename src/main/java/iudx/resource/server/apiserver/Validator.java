package iudx.resource.server.apiserver;

import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerResponse;
import iudx.resource.server.apiserver.response.ResponseType;
import iudx.resource.server.apiserver.response.RestResponse;

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
  public static void validate(MultiMap parameterMap, HttpServerResponse response) {
    final List<Entry<String, String>> entries = parameterMap.entries();
    for (final Entry<String, String> entry : entries) {
      if (!validParams.contains(entry.getKey())) {
        response.putHeader("content-type", "application/json")
            .setStatusCode(ResponseType.BadRequestData.getCode())
            .end(new RestResponse.Builder().withError(ResponseType.BadRequestData)
                .withMessage(entry.getKey() + " is not valid parameter.").build().toJsonString());
      }
    }

  }

}
