package iudx.resource.server.cache.cacheImpl;

public enum CacheType {
  REVOKED_CLIENT("revoked_client"),
  UNIQUE_ATTRIBUTE("unique_attribute");

  String cacheName;

  CacheType(String name) {
    this.cacheName = name;
  }


}
