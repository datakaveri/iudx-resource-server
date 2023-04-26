package iudx.resource.server.cache.cachelmpl;

public enum CacheType {
  REVOKED_CLIENT("revoked_client"),
  UNIQUE_ATTRIBUTE("unique_attribute"),
  CATALOGUE_CACHE("catalogue_cache");

  String cacheName;

  CacheType(String name) {
    this.cacheName = name;
  }


}
