package iudx.resource.server.databroker.util;

public enum PermissionOpType {

  ADD_READ("read"), 
  ADD_WRITE("write"),

  DELETE_READ("read"), 
  DELETE_WRITE("write");

  public String op;

  PermissionOpType(String op) {
    this.op = op;
  }
}
