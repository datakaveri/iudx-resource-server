package iudx.resource.server.apiserver.util;

public class Constants {

  // NGSI-LD endpoints
  public static final String NGSILD_BASE_PATH = "/ngsi-ld/v1";
  public static final String NGSILD_ENTITIES_URL = NGSILD_BASE_PATH + "/entities";
  public static final String NGSILD_TEMPORAL_URL = NGSILD_BASE_PATH + "/temporal/entities";
  public static final String NGSILD_SUBSCRIPTION_URL = NGSILD_BASE_PATH + "/subscription";

  // IUDX management endpoints
  public static final String IUDX_MANAGEMENT_URL = "/management";

  // ngsi-ld query paramaters
  public static final String NGSILDQUERY_ID = "id";
  public static final String NGSILDQUERY_TYPE = "type";
  public static final String NGSILDQUERY_COORDINATES = "coordinates";
  public static final String NGSILDQUERY_GEOMETRY = "geometry";
  public static final String NGSILDQUERY_ATTRIBUTE = "attrs";
  public static final String NGSILDQUERY_GEOREL = "georel";
  public static final String NGSILDQUERY_TIMEREL = "timerel";
  public static final String NGSILDQUERY_TIME = "time";
  public static final String NGSILDQUERY_ENDTIME = "endtime";
  public static final String NGSILDQUERY_Q = "q";
}
