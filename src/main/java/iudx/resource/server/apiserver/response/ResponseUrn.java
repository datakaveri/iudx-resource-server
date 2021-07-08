package iudx.resource.server.apiserver.response;

import java.util.stream.Stream;

public enum ResponseUrn {

  SUCCESS("urn:dx:rs:success", "successful operations"),
  INVALID_PARAM("urn:dx:rs:invalidParamameter", "Invalid parameter passed"),
  INVALID_GEO_REL("urn:dx:rs:invalidGeoRel", "Invalid geo relation value"),
  INVALID_TEMPORAL_PARAM("urn:dx:rs:invalidTemporalParam", "Invalid temporal parameter"),
  INVALIA_TEMPORAL_REL("urn:dx:rs:invalidTemporalRelationParam", "Invalid temporal param value"),
  INVALID_TEMPORAL_DATE_FORMAT("urn:dx:rs:invalidTemporalDateFormat", "Invalid date format"),
  INVALID_GEO_PARAM("urn:dx:rs:invalidGeoParam", "Invalid geo param"),
  INVALID_GEO_VALUE("urn:dx:rs:invalidGeoValue", "Invalid geo param value"),
  INVALID_ATTR_PARAM("urn:dx:rs:invalidAttributeParam", "Invalid attribute param"),
  INVALID_ATTR_VALUE("urn:dx:rs:invalidAttributeValue", "Invalid attribute value"),
  INVALID_OPERATION("urn:dx:rs:invalidOperation", "Invalid operation"),
  UNAUTHORIZED_ENDPOINT("urn:dx:rs:unauthorizedEndpoint", "Access to endpoint is not available"),
  UNAUTHORIZED_RESOURCE("urn,dx:rs:unauthorizedResource", "Access to resource is not available"),
  EXPIRED_TOKEN("urn:dx:rs:expiredAuthorizationToken", "Token has expired"),
  MISSING_TOKEN("urn:dx:rs:missingAuthorizationToken", "Token needed and not present"),
  INVALID_TOKEN("urn:dx:rs:invalidAuthorizationToken", "Token is invalid"),
  RESOURCE_NOT_FOUND("urn:dx:rs:resourceNotFound", "Document of given id does not exist"),



  LIMIT_EXCEED("urn:dx:rs:requestLimitExceeded", "Operation exceeds the degault value of limit"),


  // extra urn
  BACKING_SERVICE_FORMAT("urn:dx:rs:backend", "format error from backing service [cat,auth etc.]"),

  YET_NOT_IMPLEMENTED("urn:dx:rs:general", "urn yet not implemented in backend verticle.");



  private final String urn;
  private final String message;

  ResponseUrn(String urn, String message) {
    this.urn = urn;
    this.message = message;
  }

  public String getUrn() {
    return urn;
  }

  public String getMessage() {
    return message;
  }

  public static ResponseUrn fromCode(final String urn) {
    return Stream.of(values())
        .filter(v -> v.urn.equalsIgnoreCase(urn))
        .findAny()
        .orElse(YET_NOT_IMPLEMENTED); // if backend service dont respond with urn
  }

  public String toString() {
    return "[" + urn + " : " + message + " ]";
  }

}
