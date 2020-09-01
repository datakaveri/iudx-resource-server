package iudx.resource.server.apiserver.query;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import iudx.resource.server.apiserver.util.Constants;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;



/**
 * NGSILDQueryParams Class to parse query parameters from HTTP request.
 */
public class NGSILDQueryParams {
  private static final Logger LOGGER = LogManager.getLogger(NGSILDQueryParams.class);

  private List<URI> id;
  private List<String> type;
  private List<String> attrs;
  private List<String> idPattern;
  private String textQuery;
  private GeoRelation geoRel;
  private String geometry;
  private String coordinates;
  private String geoProperty;
  private TemporalRelation temporalRelation;
  private String options;

  public NGSILDQueryParams() {}



  /**
   * constructor a NGSILDParams passing query parameters map.
   * 
   * @param paramsMap query paramater's map.
   */
  public NGSILDQueryParams(MultiMap paramsMap) {
    this.setGeoRel(new GeoRelation());
    this.setTemporalRelation(new TemporalRelation());
    this.create(paramsMap);
  }

  /**
   * constructor a NGSILDParams passing json.
   * 
   * @param json JsonObject of query.
   */
  public NGSILDQueryParams(JsonObject json) {
    this.setGeoRel(new GeoRelation());
    this.setTemporalRelation(new TemporalRelation());
    this.create(json);
  }

  /**
   * This method is used to initialize a NGSILDQueryParams object from multimap of query parameters.
   * 
   * @param paramsMap query paramater's map.
   */
  private void create(MultiMap paramsMap) {
    List<Entry<String, String>> entries = paramsMap.entries();
    for (final Entry<String, String> entry : entries) {
      switch (entry.getKey()) {
        case Constants.NGSILDQUERY_ID: {
          this.id = new ArrayList<URI>();
          String[] ids = entry.getValue().split(",");
          List<URI> uris = Arrays.stream(ids).map(e -> toUri(e)).collect(Collectors.toList());
          this.id.addAll(uris);
          break;
        }
        case Constants.NGSILDQUERY_ATTRIBUTE: {
          this.attrs = new ArrayList<String>();
          this.attrs
              .addAll(Arrays.stream(entry.getValue().split(",")).collect(Collectors.toList()));
          break;
        }
        case Constants.NGSILDQUERY_GEOREL: {
          String georel = entry.getValue();
          String[] values = georel.split(";");
          this.geoRel.setRelation(values[0]);
          if (values.length == 2) {
            String[] distance = values[1].split("=");
            if (distance[0].equalsIgnoreCase(Constants.NGSILDQUERY_MAXDISTANCE)) {
              this.geoRel.setMaxDistance(Double.parseDouble(distance[1]));
            } else if (distance[0].equalsIgnoreCase(Constants.NGSILDQUERY_MINDISTANCE)) {
              this.geoRel.setMinDistance(Double.parseDouble(distance[1]));
            }
          }
          break;
        }
        case Constants.NGSILDQUERY_GEOMETRY: {
          this.geometry = entry.getValue();
          break;
        }
        case Constants.NGSILDQUERY_COORDINATES: {
          this.coordinates = entry.getValue();
          break;
        }
        case Constants.NGSILDQUERY_TIMEREL: {
          this.temporalRelation.setTemprel(entry.getValue());
          break;
        }
        case Constants.NGSILDQUERY_TIME: {
          this.temporalRelation.setTime(entry.getValue());
          break;
        }
        case Constants.NGSILDQUERY_ENDTIME: {
          this.temporalRelation.setEndTime(entry.getValue());
          break;
        }
        case Constants.NGSILDQUERY_Q: {
          this.textQuery = entry.getValue();
          break;
        }
        case Constants.NGSILDQUERY_GEOPROPERTY: {
          this.geoProperty = entry.getValue();
          break;
        }
        case Constants.IUDXQUERY_OPTIONS: {
          this.options = entry.getValue();
          break;
        }
        default: {
          LOGGER.warn(Constants.MSG_INVALID_PARAM + ":" + entry.getKey());
          break;
        }
      }
    }
  }

  public static void main(String[] args) {
    JsonObject json = new JsonObject();
    json.put("type", "query").put("geoQ",
        new JsonObject()
            .put("geometry", "point")
            .put("coordinates", new JsonArray().add(25.319768).add(82.987988))
            .put("georel", "near;maxDistance=1000")
            .put("geoproperty", "geoJsonObject"))
            .put("entities", new JsonArray().add(new JsonObject().put("id",
            "rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live")));

    NGSILDQueryParams ng = new NGSILDQueryParams(json);
    System.out.println(ng.toString());
  }

  private void create(JsonObject requestJson) {
    LOGGER.info("create from json started");
    requestJson.forEach(entry -> {
      LOGGER.info("key ::" + entry.getKey() + " value :: " + entry.getValue());
      if (entry.getKey().equalsIgnoreCase(Constants.NGSILDQUERY_Q)) {
        this.textQuery = requestJson.getString(Constants.NGSILDQUERY_Q);
      } else if (entry.getKey().equalsIgnoreCase(Constants.NGSILDQUERY_ATTRIBUTE)) {
        this.attrs = new ArrayList<String>();
        this.attrs =
            Arrays.stream(entry.getValue().toString().split(",")).collect(Collectors.toList());
      } else if (entry.getKey().equalsIgnoreCase(Constants.NGSILDQUERY_TYPE)) {
        this.type = new ArrayList<String>();
        this.type =
            Arrays.stream(entry.getValue().toString().split(",")).collect(Collectors.toList());
      } else if (entry.getKey().equalsIgnoreCase("geoQ")) {
        JsonObject geoJson = requestJson.getJsonObject(entry.getKey());
        this.setGeometry(geoJson.getString("geometry"));
        this.setGeoProperty(geoJson.getString("geoproperty"));
        this.setCoordinates(geoJson.getJsonArray("coordinates").toString());
        if (geoJson.containsKey("georel")) {
          String georel = geoJson.getString("georel");
          String[] values = georel.split(";");
          this.geoRel.setRelation(values[0]);
          if (values.length == 2) {
            String[] distance = values[1].split("=");
            if (distance[0].equalsIgnoreCase(Constants.NGSILDQUERY_MAXDISTANCE)) {
              this.geoRel.setMaxDistance(Double.parseDouble(distance[1]));
            } else if (distance[0].equalsIgnoreCase(Constants.NGSILDQUERY_MINDISTANCE)) {
              this.geoRel.setMinDistance(Double.parseDouble(distance[1]));
            }
          }
        }
      } else if (entry.getKey().equalsIgnoreCase("temporalQ")) {
        JsonObject temporalJson = requestJson.getJsonObject(entry.getKey());
        this.temporalRelation.setTemprel(temporalJson.getString("timerel"));
        this.temporalRelation.setTime(temporalJson.getString("time"));
        this.temporalRelation.setEndTime(temporalJson.getString("endTime"));
      } else if (entry.getKey().equalsIgnoreCase("entities")) {
        JsonArray array = new JsonArray(entry.getValue().toString());
        Iterator<?> iter = array.iterator();
        while (iter.hasNext()) {
          this.id = new ArrayList<URI>();
          this.idPattern = new ArrayList<String>();
          JsonObject entity = (JsonObject) iter.next();
          System.out.println(entity);
          String id = entity.getString("id");
          String idPattern = entity.getString("idPattern");
          if (id != null) {
            this.id.add(toUri(id));
          }
          if (idPattern != null) {
            this.idPattern.add(idPattern);
          }
        }
      } else if (entry.getKey().equalsIgnoreCase(Constants.IUDXQUERY_OPTIONS)) {
        this.options = requestJson.getString(entry.getKey());
      }
    });
  }

  private URI toUri(String source) {
    URI uri = null;
    try {
      uri = new URI(source);
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
    return uri;
  }

  public List<URI> getId() {
    return id;
  }

  public void setId(List<URI> id) {
    this.id = id;
  }

  public List<String> getType() {
    return type;
  }

  public void setType(List<String> type) {
    this.type = type;
  }

  public List<String> getAttrs() {
    return attrs;
  }

  public void setAttrs(List<String> attrs) {
    this.attrs = attrs;
  }

  public List<String> getIdPattern() {
    return idPattern;
  }

  public void setIdPattern(List<String> idPattern) {
    this.idPattern = idPattern;
  }

  public String getQ() {
    return textQuery;
  }

  public void setQ(String textQuery) {
    this.textQuery = textQuery;
  }

  public GeoRelation getGeoRel() {
    return geoRel;
  }

  public void setGeoRel(GeoRelation geoRel) {
    this.geoRel = geoRel;
  }

  public String getGeometry() {
    return geometry;
  }

  public void setGeometry(String geometry) {
    this.geometry = geometry;
  }

  public String getCoordinates() {
    return coordinates;
  }

  public void setCoordinates(String coordinates) {
    this.coordinates = coordinates;
  }

  public String getGeoProperty() {
    return geoProperty;
  }

  public void setGeoProperty(String geoProperty) {
    this.geoProperty = geoProperty;
  }

  public TemporalRelation getTemporalRelation() {
    return temporalRelation;
  }

  public void setTemporalRelation(TemporalRelation temporalRelation) {
    this.temporalRelation = temporalRelation;
  }

  public String getOptions() {
    return options;
  }

  public void setOptions(String options) {
    this.options = options;
  }

}
