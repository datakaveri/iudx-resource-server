package iudx.resource.server.apiserver.query;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import io.vertx.core.MultiMap;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import iudx.resource.server.apiserver.util.Constants;

/**
 * NGSILDQueryParams Class to parse query parameters from HTTP request.
 */
public class NGSILDQueryParams {
  private static final Logger LOGGER = LoggerFactory.getLogger(NGSILDQueryParams.class);

  private List<URI> id;
  private List<String> type;
  private List<String> attrs;
  private String idPattern;
  private String textQuery;
  private GeoRelation geoRel;
  private String geometry;
  private String coordinates;
  private String geoProperty;
  private TemporalRelation temporalRelation;
  private DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME
      .withZone(ZoneId.systemDefault());

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
   * This method is used to initialize a NGSILDQueryParams object from multimap of
   * query parameters.
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
          this.attrs.addAll(Arrays.stream(entry.getValue().split(",")).collect(Collectors.toList()));
  
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
          this.temporalRelation.setTime(LocalDateTime.parse(entry.getValue(), formatter));
          break;
        }
        case Constants.NGSILDQUERY_ENDTIME: {
          this.temporalRelation.setEndTime(LocalDateTime.parse(entry.getValue(), formatter));
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
        default: {
          LOGGER.warn(Constants.MSG_INVALID_PARAM + ":" + entry.getKey());
        }
      }
    }
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

  public String getIdPattern() {
    return idPattern;
  }

  public void setIdPattern(String idPattern) {
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

}
