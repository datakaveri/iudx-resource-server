package iudx.resource.server.common;

public enum Api {
  
  //ngsi-ld endpoints
  NGSILD_BASE("/ngsi-ld/v1"),
  INGESTION("/ingestion"),
  
  //subscription endponts
  SUBSCRIPTION("/subscription"),

  //management endpoints
  MANAGEMENT("/management"),
  EXCHANGE("/exchange"),
  QUEUE("/queue"),
  BIND("/bind"),
  UNBIND("/unbind"),
  VHOST("/vhost"),
  RESET_PWD("/user/resetPassword"),

  ADMIN("/admin"),
  REVOKE_TOKEN("/revoketoken"),
  RESOURCE_ATTRIBS("/resourceattribute");

  
  public final String path;

  Api(String path) {
    this.path = path;
  }
}
