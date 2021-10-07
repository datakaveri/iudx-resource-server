package iudx.resource.server.authenticator.authorization;

import java.util.stream.Stream;

public enum Api {
  ENTITIES("/ngsi-ld/v1/entities"),
  TEMPORAL("/ngsi-ld/v1/temporal"),
  ENTITY_OPERATION("/ngsi-ld/v1/entityoperations/query"),
  ENTITY_OPERATION_TEMPORAL("/ngsi-ld/v1/temporal/entityoperations/query"),
  SUBSCRIPTION("/ngsi-ld/v1/subscription"),
  INGESTION("/ngsi-ld/v1/ingestion"),
  ADMIN("/iudx/v1/management/resourceAttribute");
  


  private final String endpoint;

  Api(String endpoint) {
    this.endpoint = endpoint;
  }

  public String getApiEndpoint() {
    return this.endpoint;
  }

  public static Api fromEndpoint(final String endpoint) {
    return Stream.of(values())
        .filter(v -> v.endpoint.equalsIgnoreCase(endpoint))
        .findAny()
        .orElse(null);
  }

}
