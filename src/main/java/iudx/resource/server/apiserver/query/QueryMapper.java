package iudx.resource.server.apiserver.query;

import java.util.Arrays;
import java.util.List;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * QueryMapper class to convert NGSILD query into json object for the purpose of
 * information exchange among different verticals.
 *
 */
public class QueryMapper {

  /**
   * This method is used to create a json object from NGSILDQueryParams.
   * 
   * @param params     A map of query parameters passed.
   * @param isTemporal flag indicating whether temporal or not.
   * @return JsonObject result.
   */
  public JsonObject toJson(NGSILDQueryParams params, boolean isTemporal) {
    JsonObject json = new JsonObject();

    if (params.getId() != null) {
      JsonArray jsonArray = new JsonArray();
      params.getId().forEach(s -> jsonArray.add(s.toString()));
      json.put("id", jsonArray);
    }
    if (params.getAttrs() != null) {
      JsonArray jsonArray = new JsonArray();
      params.getAttrs().forEach(attribute -> jsonArray.add(attribute));
      json.put("attribute-filter", jsonArray);
    }
    // TODO : geometry/georel validations according to specifications
    if (params.getGeoRel() != null
        && (params.getCoordinates() != null || params.getGeometry() != null)) {
      if (params.getGeometry().equalsIgnoreCase("point")
          && params.getGeoRel().getRelation().equals("near")
          && params.getGeoRel().getMaxDistance() != null) {
        String[] coords = params.getCoordinates().replaceAll("\\[|\\]", "").split(",");
        json.put("lat", Double.parseDouble(coords[0]));
        json.put("lon", Double.parseDouble(coords[1]));
        json.put("radius", params.getGeoRel().getMaxDistance());
      } else {
        json.put("geometry", params.getGeometry());
        json.put("coordinates", params.getCoordinates());
        json.put("georel", getOrDefault(params.getGeoRel().getRelation(), "within"));
        if (params.getGeoRel().getMaxDistance() != null) {
          json.put("maxdistance", params.getGeoRel().getMaxDistance());
        } else if (params.getGeoRel().getMinDistance() != null) {
          json.put("mindistance", params.getGeoRel().getMinDistance());
        }
      }
    }
    // TODO: timerel validations according to specifications.
    if (isTemporal && params.getTemporalRelation().getTemprel() != null
        && params.getTemporalRelation().getTime() != null) {
      if (params.getTemporalRelation().getTemprel().equalsIgnoreCase("between")) {
        json.put("time", params.getTemporalRelation().getTime().toString());
        json.put("endtime", params.getTemporalRelation().getEndTime().toString());
        json.put("timerel", params.getTemporalRelation().getTemprel());
      } else {
        json.put("time", params.getTemporalRelation().getTime().toString());
        json.put("timerel", params.getTemporalRelation().getTemprel());
      }

    }
    if (params.getQ() != null) {
      JsonArray query = new JsonArray();
      String[] qterms = params.getQ().split(";");
      for (String term : qterms) {
        query.add(getQueryTerms(term));
      }
      json.put("attr-query", query);
    }
    return json;
  }

  private <T> T getOrDefault(T value, T def) {
    return (value == null) ? def : value;
  }

  JsonObject getQueryTerms(String queryTerms) {
    JsonObject json = new JsonObject();
    int length = queryTerms.length();
    List<Character> allowedSpecialCharacter = Arrays.asList('>', '=', '<', '!');
    int startIndex = 0;
    boolean specialCharFound = false;
    for (int i = 0; i < length; i++) {
      Character c = queryTerms.charAt(i);
      if (!(Character.isLetter(c) || Character.isDigit(c)) && !specialCharFound) {
        if (allowedSpecialCharacter.contains(c)) {
          json.put("attribute", queryTerms.substring(startIndex, i));
          startIndex = i;
          specialCharFound = true;
        } else {
          System.out.println("Ignore " + c.toString());
        }
      } else {
        if (specialCharFound && (Character.isLetter(c) || Character.isDigit(c))) {
          json.put("operator", queryTerms.substring(startIndex, i));
          json.put("value", queryTerms.substring(i));
          break;
        }
      }

    }
    return json;
  }

}
