package iudx.resource.server.apiserver.query;

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * 
 * QueryMapper class to convert NGSILD query into json object for the purpose of
 * information exchange among different verticals.
 *
 */
public class QueryMapper {

	/**
	 * This method is used to create a json object from NGSILDQueryParams.
	 * 
	 * @param params NGSILD query request params
	 * @return JsonObject
	 */
	public JsonObject toJson(NGSILDQueryParams params) {
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
		if (params.getGeoRel() != null && (params.getCoordinates() != null || params.getGeometry() != null)) {
			if (params.getGeometry().equalsIgnoreCase("point")) {
				String coords[] = params.getCoordinates().replaceAll("\\[|\\]", "").split(",");
				json.put("lat", coords[0]);
				json.put("lon", coords[1]);
				json.put("radius", params.getGeoRel().getMaxDistance());
			} else if (params.getGeometry().equalsIgnoreCase("polygon")
					|| params.getGeometry().equalsIgnoreCase("linestring")) {
				json.put("geometry", params.getGeometry());
				json.put("coordinates", params.getCoordinates());
				json.put("georel", getOrDefault(params.getGeoRel().getRelation(), "within"));
				if (params.getGeoRel().getMaxDistance() != null) {
					json.put("distance", params.getGeoRel().getMaxDistance());
				} else if (params.getGeoRel().getMinDistance() != null) {
					json.put("distance", params.getGeoRel().getMinDistance());
				}
			} else if (params.getGeometry().equalsIgnoreCase("bbox")) {
				json.put("geometry", params.getGeometry());
				json.put("coordinates", params.getCoordinates());
				json.put("georel", getOrDefault(params.getGeoRel().getRelation(), "within"));
				if (params.getGeoRel().getMaxDistance() != null) {
					json.put("distance", params.getGeoRel().getMaxDistance());
				} else if (params.getGeoRel().getMinDistance() != null) {
					json.put("distance", params.getGeoRel().getMinDistance());
				}
			}
		}
		// TODO: timerel validations according to specifications.
		if (params.getTemporalRelation().getTemprel() != null && params.getTemporalRelation().getTime() != null) {
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
			JsonArray qArray = new JsonArray();
			String[] qterms = params.getQ().split(";");
			for (int i = 0; i < qterms.length; i++) {
				qArray.add(getQueryTerms(qterms[i]));
			}
			json.put("attr-query", qArray);
		}
		return json;
	}

	private <T> T getOrDefault(T value, T def) {
		return (value == null) ? def : value;
	}

	private JsonObject getQueryTerms(String queryTerms) {
		List<String> qTerms = new ArrayList<String>();
		JsonObject json = new JsonObject();
		int length = queryTerms.length();
		int startIndex = 0;
		boolean specialCharFound = false;
		char[] allowedSpecialCharacter = ">=<!".toCharArray();
		for (int i = 0; i < length; i++) {
			Character c = queryTerms.charAt(i);
			if (!(Character.isLetter(c) || Character.isDigit(c)) && !specialCharFound) {
				for (int j = 0; j < allowedSpecialCharacter.length; j++) {
					if (allowedSpecialCharacter[j] == c) {
						qTerms.add(queryTerms.substring(startIndex, i));
						json.put("attribute", queryTerms.substring(startIndex, i));
						startIndex = i;
						specialCharFound = true;
					} else {
						System.out.println("Ignore " + c.toString());
					}
				}
			} else {
				if (specialCharFound && (Character.isLetter(c) || Character.isDigit(c))) {
					qTerms.add(queryTerms.substring(startIndex, i));
					qTerms.add(queryTerms.substring(i));

					json.put("operator", queryTerms.substring(startIndex, i));
					json.put("value", queryTerms.substring(i));

					break;
				}
			}

		}
		return json;
	}

}
