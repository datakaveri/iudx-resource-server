package iudx.resource.server.common;

public enum BroadcastEventType {

  CREATE("create"),
  UPDATE("update"),
  DELETE("delete");

  public final String eventType;

  BroadcastEventType(String eventType) {
    this.eventType = eventType;
  }
  
}
