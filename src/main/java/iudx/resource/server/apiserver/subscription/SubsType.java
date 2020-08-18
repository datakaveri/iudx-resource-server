package iudx.resource.server.apiserver.subscription;

public enum SubsType {
  CALLBACK("callback"), 
  STREAMING("streaming");

  private final String subType;

  SubsType(String subType) {
    this.subType = subType;
  }

  public String getMessage() {
    return subType;
  }

}
