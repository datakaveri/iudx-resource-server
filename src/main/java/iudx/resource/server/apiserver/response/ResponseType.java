package iudx.resource.server.apiserver.response;

/* TODO Need to add documentation. */
public enum ResponseType {
  Ok(200, "Ok"), Created(201, "created"), InvalidRequest(400, "Invalid request"),
  AuthenticationFailure(401, "Invalid credentials"), BadRequestData(400, "Bad Request Data"),
  TooComplexQuery(403, "Too complex query"), TooManyResults(403, "Too many results"),
  NotFound(404, "Not Found"), ResourceNotFound(404, "Resource not found"),
  MethodNotAllowed(405, "Method not allowed"), AlreadyExists(409, "Already exists"),
  LenghtRequired(411,
      "HTTP request provided by a client does not define the Content-Length HTTP header"),
  RequestEntityTooLarge(413, "HTTP input data stream is too large too many bytes"),
  UnsupportedMediaType(415, "Unsupported Media type"),
  OperationNotSupported(422, "Operation not supported"),
  UnprocessableEntity(422, "Unprocessable Entity"), InternalError(500, "Internal error");

  private final int code;
  private final String message;

  ResponseType(int code, String message) {
    this.code = code;
    this.message = message;
  }

  public int getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

  public String toString() {
    return "[" + code + " : " + message + " ]";
  }

}
