package iudx.resource.server.metering.util;

import static iudx.resource.server.metering.util.Constants.DETAIL;
import static iudx.resource.server.metering.util.Constants.RESULTS;
import static iudx.resource.server.metering.util.Constants.SUCCESS;
import static iudx.resource.server.metering.util.Constants.TITLE;
import static iudx.resource.server.metering.util.Constants.TOTAL;
import static iudx.resource.server.metering.util.Constants.TOTAL_HITS;
import static iudx.resource.server.metering.util.Constants.TYPE_KEY;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.common.ResponseUrn;

public class ResponseBuilder {
  private final String status;
  private final JsonObject response;

  /** Initialise the object with Success or Failure. */
  public ResponseBuilder(String status) {
    this.status = status;
    response = new JsonObject();
  }

  public ResponseBuilder setTypeAndTitle(int statusCode) {

    if (200 == statusCode) {
      response.put(TYPE_KEY, ResponseUrn.SUCCESS_URN.getUrn());
      response.put(TITLE, SUCCESS);
    } else if (204 == statusCode) {
      response.put(TYPE_KEY, statusCode);
      response.put(TITLE, SUCCESS);
    } else {
      response.put(TYPE_KEY, statusCode);
      response.put(TITLE, ResponseUrn.BAD_REQUEST_URN.getUrn());
    }
    return this;
  }

  /** Overloaded methods for Error messages. */
  public ResponseBuilder setMessage(String error) {
    response.put(DETAIL, error);
    return this;
  }

  public ResponseBuilder setCount(int count) {
    response.put(RESULTS, new JsonArray().add(new JsonObject().put(TOTAL, count)));
    return this;
  }

  public ResponseBuilder setData(JsonArray jsonArray) {
    response.put(RESULTS, jsonArray);
    return this;
  }
  public ResponseBuilder setTotalHits(int totalHits) {
    response.put(TOTAL_HITS, totalHits);
    return this;
  }

  public JsonObject getResponse() {
    return response;
  }
}