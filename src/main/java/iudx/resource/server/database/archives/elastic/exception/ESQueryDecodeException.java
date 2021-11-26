package iudx.resource.server.database.archives.elastic.exception;

import org.apache.http.HttpStatus;

import iudx.resource.server.common.ResponseUrn;


public class ESQueryDecodeException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final int statusCode = HttpStatus.SC_BAD_REQUEST;
  private final iudx.resource.server.common.ResponseUrn urn;
  private final String message;

  public ESQueryDecodeException(final String message) {
    super();
    this.urn = ResponseUrn.BAD_REQUEST_URN;
    this.message = message;
  }

  public ESQueryDecodeException(final ResponseUrn urn, final String message) {
    super(message);
    this.urn = urn;
    this.message = message;
  }

  public String getMessage() {
    return message;
  }
}
