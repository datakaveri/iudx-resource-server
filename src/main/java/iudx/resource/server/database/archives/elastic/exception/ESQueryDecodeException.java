package iudx.resource.server.database.archives.elastic.exception;

import iudx.resource.server.common.ResponseUrn;
import org.apache.http.HttpStatus;

public class ESQueryDecodeException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final int statusCode = HttpStatus.SC_BAD_REQUEST;
  private final ResponseUrn urn;
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
