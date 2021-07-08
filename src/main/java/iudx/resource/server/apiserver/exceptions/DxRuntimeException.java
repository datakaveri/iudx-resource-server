package iudx.resource.server.apiserver.exceptions;

import iudx.resource.server.apiserver.response.ResponseUrn;

public class DxRuntimeException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private int statusCode;
  private ResponseUrn urn;
  private String message;

  public DxRuntimeException(int statusCode, ResponseUrn urn) {
    super();
    this.statusCode = statusCode;
    this.urn = urn;
  }

  public DxRuntimeException(int statusCode, ResponseUrn urn, String message) {
    super(message);
    this.statusCode = statusCode;
    this.urn = urn;
    this.message = message;
  }

  public DxRuntimeException(int statusCode, ResponseUrn urn, Throwable cause) {
    super(cause);
    this.statusCode = statusCode;
    this.urn = urn;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public ResponseUrn getUrn() {
    return urn;
  }

  public String getMessage() {
    return message;
  }



}
