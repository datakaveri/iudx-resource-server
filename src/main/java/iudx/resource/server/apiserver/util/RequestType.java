package iudx.resource.server.apiserver.util;


public enum RequestType {
  ENTITY("entity"),
  TEMPORAL("temporal"),
  LATEST("latest"),
  POST_TEMPORAL("post_temporal_schema.json"),
  POST_ENTITIES("post_entities_schema.json"),
  SUBSCRIPTION("subscription_schema.json");

  private String filename;

  public String getFilename() {
    return this.filename;
  }

  private RequestType(String fileName) {
    this.filename = fileName;
  }
}
