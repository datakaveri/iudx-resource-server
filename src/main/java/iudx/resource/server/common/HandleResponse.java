package iudx.resource.server.common;

import static iudx.resource.server.apiserver.response.ResponseUtil.generateResponse;
import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.apiserver.util.Constants.JSON_TITLE;
import static iudx.resource.server.common.HttpStatusCode.BAD_REQUEST;
import static iudx.resource.server.common.HttpStatusCode.getByValue;
import static iudx.resource.server.common.ResponseUrn.BACKING_SERVICE_FORMAT_URN;
import static iudx.resource.server.common.ResponseUrn.fromCode;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HandleResponse {
  private static final Logger LOGGER = LogManager.getLogger(HandleResponse.class);

  public void handleResponse(HttpServerResponse response, Response respObject) {
    ResponseUrn urn = fromCode(respObject.getType());
    handleResponse(response, respObject, urn.getMessage());
  }

  private void handleResponse(HttpServerResponse response, Response respObject, String message) {
    HttpStatusCode httpCode = getByValue(respObject.getStatus());
    ResponseUrn urn = fromCode(respObject.getType());
    handleResponse(response, httpCode, urn, message);
  }

  public void handleResponse(HttpServerResponse response, HttpStatusCode code, ResponseUrn urn) {
    handleResponse(response, code, urn, code.getDescription());
  }

  public void handleResponse(
      HttpServerResponse response, HttpStatusCode statusCode, ResponseUrn urn, String message) {
    response
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(statusCode.getValue())
        .end(generateResponse(statusCode, urn, message).toString());
  }

  public void handleSuccessResponse(HttpServerResponse response, int statusCode, String result) {
    response.putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(statusCode).end(result);
  }

  public void processBackendResponse(HttpServerResponse response, String failureMessage) {
    LOGGER.debug("Info : " + failureMessage);
    try {
      JsonObject json = new JsonObject(failureMessage);
      int type = json.getInteger(JSON_TYPE);
      HttpStatusCode status = HttpStatusCode.getByValue(type);
      String urnTitle = json.getString(JSON_TITLE);
      ResponseUrn urn;
      if (urnTitle != null) {
        urn = fromCode(urnTitle);
      } else {
        urn = fromCode(type + "");
      }
      // return urn in body
      response
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(type)
          .end(generateResponse(status, urn).toString());
    } catch (DecodeException ex) {
      LOGGER.error("ERROR : Expecting Json from backend service [ jsonFormattingException ]");
      handleResponse(response, BAD_REQUEST, BACKING_SERVICE_FORMAT_URN);
    }
  }
}
