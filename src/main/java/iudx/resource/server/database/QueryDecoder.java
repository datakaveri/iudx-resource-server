package iudx.resource.server.database;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.database.ElasticClient;

public class QueryDecoder {
	
	private static final Logger LOGGER = LogManager.getLogger(QueryDecoder.class);

	/**
	 * Decodes and constructs ElasticSearch Search/Count query based on the
	 * parameters passed in the request.
	 * 
	 * @param request Json object containing various fields related to query-type.
	 * @return JsonObject which contains fully formed ElasticSearch query.
	 */
	public JsonObject queryDecoder(JsonObject request) {
		String searchType = request.getString(Constants.SEARCH_TYPE);
		Boolean match = false;
		JsonObject elasticQuery = new JsonObject();
		JsonObject boolObject = new JsonObject().put(Constants.BOOL_KEY, new JsonObject());
		JsonArray id = request.getJsonArray(Constants.ID);
		JsonArray filterQuery = new JsonArray();
		JsonObject termQuery = new JsonObject().put(Constants.TERMS_KEY,
				new JsonObject().put(Constants.RESOURCE_ID_KEY + ".keyword", id));

		filterQuery.add(termQuery);
		/* TODO: Pagination for large result set */
		if (request.containsKey("search") && request.getBoolean("search")) {
			elasticQuery.put(Constants.SIZE_KEY, 10);
		}
		/* Geo-Spatial Search */
		if (searchType.matches("(.*)geoSearch(.*)")) {
			match = true;
			LOGGER.info("In geoSearch block---------");
			JsonObject shapeJson = new JsonObject();
			JsonObject geoSearch = new JsonObject();
			String relation;
			JsonArray coordinates;
			if (request.containsKey(Constants.LON) && request.containsKey(Constants.LAT)
					&& request.containsKey(Constants.GEO_RADIUS)) {
				double lat = request.getDouble(Constants.LAT);
				double lon = request.getDouble(Constants.LON);
				double radius = request.getDouble(Constants.GEO_RADIUS);
				relation = request.containsKey(Constants.GEOREL) ? request.getString(Constants.GEOREL)
						: Constants.WITHIN;
				shapeJson
						.put(Constants.SHAPE_KEY,
								new JsonObject().put(Constants.TYPE_KEY, Constants.GEO_CIRCLE)
										.put(Constants.COORDINATES_KEY, new JsonArray().add(lon).add(lat))
										.put(Constants.GEO_RADIUS, radius + "m"))
						.put(Constants.GEO_RELATION_KEY, relation);
			} else if (request.containsKey(Constants.GEOMETRY)
					&& (request.getString(Constants.GEOMETRY).equalsIgnoreCase(Constants.POLYGON)
							|| request.getString(Constants.GEOMETRY).equalsIgnoreCase(Constants.LINESTRING))
					&& request.containsKey(Constants.GEOREL) && request.containsKey(Constants.COORDINATES_KEY)
					&& request.containsKey(Constants.GEO_PROPERTY)) {
				String geometry = request.getString(Constants.GEOMETRY);
				relation = request.getString(Constants.GEOREL);
				coordinates = new JsonArray(request.getString(Constants.COORDINATES_KEY));
				int length = coordinates.getJsonArray(0).size();
				if (geometry.equalsIgnoreCase(Constants.POLYGON)
						&& !coordinates.getJsonArray(0).getJsonArray(0).getDouble(0)
								.equals(coordinates.getJsonArray(0).getJsonArray(length - 1).getDouble(0))
						&& !coordinates.getJsonArray(0).getJsonArray(0).getDouble(1)
								.equals(coordinates.getJsonArray(0).getJsonArray(length - 1).getDouble(1))) {
					return new JsonObject().put("Error", Constants.COORDINATE_MISMATCH);

				}
				shapeJson.put(Constants.SHAPE_KEY,
						new JsonObject().put(Constants.TYPE_KEY, geometry).put(Constants.COORDINATES_KEY, coordinates))
						.put(Constants.GEO_RELATION_KEY, relation);
			} else if (request.containsKey(Constants.GEOMETRY)
					&& request.getString(Constants.GEOMETRY).equalsIgnoreCase(Constants.BBOX)
					&& request.containsKey(Constants.GEOREL) && request.containsKey(Constants.COORDINATES_KEY)
					&& request.containsKey(Constants.GEO_PROPERTY)) {
				relation = request.getString(Constants.GEOREL);
				coordinates = new JsonArray(request.getString(Constants.COORDINATES_KEY));
				shapeJson = new JsonObject();
				shapeJson
						.put(Constants.SHAPE_KEY,
								new JsonObject().put(Constants.TYPE_KEY, Constants.GEO_BBOX)
										.put(Constants.COORDINATES_KEY, coordinates))
						.put(Constants.GEO_RELATION_KEY, relation);

			} else {
				return new JsonObject().put("Error", Constants.MISSING_GEO_FIELDS);
			}
			geoSearch.put(Constants.GEO_SHAPE_KEY, new JsonObject().put(Constants.GEO_KEY, shapeJson));
			filterQuery.add(geoSearch);
		}
		/* Response Filtering */
		if (searchType.matches("(.*)responseFilter(.*)")) {
			match = true;
			LOGGER.info("In responseFilter block---------");
			if (!request.getBoolean("search")) {
				return new JsonObject().put("Error", Constants.COUNT_UNSUPPORTED);
			}
			if (request.containsKey(Constants.RESPONSE_ATTRS)) {
				JsonArray sourceFilter = request.getJsonArray(Constants.RESPONSE_ATTRS);
				elasticQuery.put(Constants.SOURCE_FILTER_KEY, sourceFilter);
			} else {
				return new JsonObject().put("Error", Constants.MISSING_RESPONSE_FILTER_FIELDS);
			}
		}
		/* Temporal Search */
		if (searchType.matches("(.*)temporalSearch(.*)") && request.containsKey(Constants.REQ_TIMEREL)
				&& request.containsKey("time")) {
			match = true;
			LOGGER.info("In temporalSearch block---------");
			String timeRelation = request.getString(Constants.REQ_TIMEREL);
			String time = request.getString(Constants.TIME_KEY);
			/* check if the time is valid based on the format. Supports both UTC and IST. */
			try {
				DateFormat formatTimeUtc = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
				DateFormat formatTimeIst = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
				formatTimeUtc.setTimeZone(TimeZone.getTimeZone("UTC"));
				formatTimeIst.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
				Date parsedDateTimeUtc = formatTimeUtc.parse(time);
				Date parsedDateTimeIst = formatTimeIst.parse(time);
				LOGGER.info("Requested date: #UTC- " + formatTimeUtc.format(parsedDateTimeUtc) + "\n#IST- "
						+ formatTimeIst.format(parsedDateTimeIst));
			} catch (ParseException e) {
				e.printStackTrace();
				return new JsonObject().put("Error", Constants.INVALID_DATE);
			}
			JsonObject rangeTimeQuery = new JsonObject();
			if (Constants.DURING.equalsIgnoreCase(timeRelation)) {
				String endTime = request.getString(Constants.END_TIME);
				rangeTimeQuery.put(Constants.RANGE_KEY, new JsonObject().put(Constants.TIME_KEY,
						new JsonObject().put(Constants.GREATER_THAN_EQ, time).put(Constants.LESS_THAN_EQ, endTime)));
			} else if (Constants.BEFORE.equalsIgnoreCase(timeRelation)) {
				rangeTimeQuery.put(Constants.RANGE_KEY,
						new JsonObject().put(Constants.TIME_KEY, new JsonObject().put(Constants.LESS_THAN, time)));
			} else if (Constants.AFTER.equalsIgnoreCase(timeRelation)) {
				rangeTimeQuery.put(Constants.RANGE_KEY,
						new JsonObject().put(Constants.TIME_KEY, new JsonObject().put(Constants.GREATER_THAN, time)));
			} else if ("tequals".equalsIgnoreCase(timeRelation)) {
				rangeTimeQuery.put(Constants.TERM_KEY, new JsonObject().put(Constants.TIME_KEY, time));
			} else {
				return new JsonObject().put("Error", Constants.MISSING_TEMPORAL_FIELDS);
			}
			filterQuery.add(rangeTimeQuery);
		}
		/* Attribute Search */
		if (searchType.matches("(.*)attributeSearch(.*)")) {
			match = true;
			JsonArray attrQuery;
			LOGGER.info("In attributeFilter block---------");
			if (request.containsKey(Constants.ATTRIBUTE_QUERY_KEY)) {
				attrQuery = request.getJsonArray(Constants.ATTRIBUTE_QUERY_KEY);
				/* Multi-Attribute */
				for (Object obj : attrQuery) {
					JsonObject attrObj = (JsonObject) obj;
					JsonObject attrElasticQuery = new JsonObject();
					try {
						String attribute = attrObj.getString(Constants.ATTRIBUTE_KEY);
						String operator = attrObj.getString(Constants.OPERATOR);
						if (Constants.GREATER_THAN_OP.equalsIgnoreCase(operator)) {
							attrElasticQuery.put(Constants.RANGE_KEY, new JsonObject().put(attribute, new JsonObject()
									.put(Constants.GREATER_THAN, Double.valueOf(attrObj.getString(Constants.VALUE)))));
							filterQuery.add(attrElasticQuery);
						} else if (Constants.LESS_THAN_OP.equalsIgnoreCase(operator)) {
							attrElasticQuery.put(Constants.RANGE_KEY, new JsonObject().put(attribute, new JsonObject()
									.put(Constants.LESS_THAN, Double.valueOf(attrObj.getString(Constants.VALUE)))));
							filterQuery.add(attrElasticQuery);
						} else if (Constants.GREATER_THAN_EQ_OP.equalsIgnoreCase(operator)) {
							attrElasticQuery.put(Constants.RANGE_KEY,
									new JsonObject().put(attribute, new JsonObject().put(Constants.GREATER_THAN_EQ,
											Double.valueOf(attrObj.getString(Constants.VALUE)))));
							filterQuery.add(attrElasticQuery);
						} else if (Constants.LESS_THAN_EQ_OP.equalsIgnoreCase(operator)) {
							attrElasticQuery.put(Constants.RANGE_KEY, new JsonObject().put(attribute, new JsonObject()
									.put(Constants.LESS_THAN_EQ, Double.valueOf(attrObj.getString(Constants.VALUE)))));
							filterQuery.add(attrElasticQuery);
						} else if (Constants.EQUAL_OP.equalsIgnoreCase(operator)) {
							attrElasticQuery.put(Constants.TERM_KEY, new JsonObject().put(attribute,
									Double.valueOf(attrObj.getString(Constants.VALUE))));
							filterQuery.add(attrElasticQuery);
						} else if (Constants.BETWEEN_OP.equalsIgnoreCase(operator)) {
							attrElasticQuery
									.put(Constants.RANGE_KEY,
											new JsonObject()
													.put(attribute,
															new JsonObject()
																	.put(Constants.GREATER_THAN_EQ,
																			Double.valueOf(attrObj
																					.getString(Constants.VALUE_LOWER)))
																	.put(Constants.LESS_THAN_EQ, Double.valueOf(attrObj
																			.getString(Constants.VALUE_UPPER)))));
							filterQuery.add(attrElasticQuery);
						} else if (Constants.NOT_EQUAL_OP.equalsIgnoreCase(operator)) {
							attrElasticQuery.put(Constants.TERM_KEY, new JsonObject().put(attribute,
									Double.valueOf(attrObj.getString(Constants.VALUE))));
							boolObject.getJsonObject(Constants.BOOL_KEY).put(Constants.MUST_NOT, attrElasticQuery);

							/*
							 * TODO: Need to understand operator parameter of JsonObject from the APIServer
							 * would look like.
							 */
							// else if ("like".equalsIgnoreCase(operator)) {}
						} else {
							return new JsonObject().put("Error", Constants.INVALID_OPERATOR);
						}
					} catch (NullPointerException e) {
						e.printStackTrace();
						return new JsonObject().put("Error", Constants.MISSING_ATTRIBUTE_FIELDS);
					}
				}
			}
		}
		/* checks if any valid search requests have matched */
		if (!match) {
			return new JsonObject().put("Error", Constants.INVALID_SEARCH);
		} else {
			/* return fully formed elastic query */
			boolObject.getJsonObject(Constants.BOOL_KEY).put(Constants.FILTER_KEY, filterQuery);
			return elasticQuery.put(Constants.QUERY_KEY, boolObject);
		}
	}
}
