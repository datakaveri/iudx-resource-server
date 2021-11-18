package iudx.resource.server.apiserver;

public enum Api {
  REVOKE_TOKEN("/revoketoken");

  private String path;

  public String getPath() {
    return this.path;
  }

  Api(String path) {
    this.path = path;
  }
}
