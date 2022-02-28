package iudx.resource.server.apiserver.util;


public enum RequestType {
  ENTITY("entity"),
  TEMPORAL("temporal"),
  LATEST("latest"),
  ASYNC_SEARCH("async_search"),
  ASYNC_STATUS("async_status"),
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
