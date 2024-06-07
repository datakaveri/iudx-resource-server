package iudx.resource.server.apiserver.subscription;

public enum SubsType {
  STREAMING("STREAMING");

  public final String type;

  SubsType(String type) {
    this.type = type;
  }
}
