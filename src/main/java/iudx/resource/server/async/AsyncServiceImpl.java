package iudx.resource.server.async;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.database.archives.ResponseBuilder;
import iudx.resource.server.database.archives.elastic.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static iudx.resource.server.database.archives.Constants.*;

/**
 * The Async Service Implementation.
 * <h1>Async Service Implementation</h1>
 * <p>
 *   The Async Service implementation in the IUDX Resource Server implements the definitions of the
 *   {@link iudx.resource.server.async.AsyncService}.
 * </p>
 *
 * @version 1.0
 * @since 2022-02-08
 */

public class AsyncServiceImpl implements AsyncService {

	private static final Logger  LOGGER = LogManager.getLogger(AsyncServiceImpl.class);
	private final ElasticClient client;
	private JsonObject query;
	private ResponseBuilder responseBuilder;
	private String timeLimit;

	public AsyncServiceImpl(ElasticClient client, String timeLimit) {
		this.client = client;
		this.timeLimit = timeLimit;
	}

	/**
	 * Performs a ElasticSearch scrolling search using the high level REST client.
	 *
	 * @param request JsonObject received from the AsyncRestApi
	 * @param handler Handler to return database response in case of success and appropriate error
	 *                message in case of failure
	 */
	@Override
	public AsyncService scrollQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
		LOGGER.trace("Info: scrollQuery; " + request.toString());
		request.put(SEARCH_KEY, true);
		request.put(TIME_LIMIT, "test,2020-10-22T00:00:00Z,10"); // TODO: what is time limit?
		request.put("isTest", true);
		if (!request.containsKey(ID)) {
			LOGGER.debug("Info: " + ID_NOT_FOUND);
			responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(ID_NOT_FOUND);
			handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
			return null;
		}

		if (request.getJsonArray(ID).isEmpty()) {
			LOGGER.debug("Info: " + EMPTY_RESOURCE_ID);
			responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400)
					.setMessage(EMPTY_RESOURCE_ID);
			handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
			return null;
		}

		if (!request.containsKey(SEARCH_TYPE)) {
			LOGGER.debug("Info: " + SEARCHTYPE_NOT_FOUND);
			responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400)
					.setMessage(SEARCHTYPE_NOT_FOUND);
			handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
			return null;
		}

		if (request.getJsonArray(ID).getString(0).split("/").length != 5) {
			LOGGER.error("Malformed ID: " + request.getJsonArray(ID).getString(0));
			responseBuilder =
					new ResponseBuilder(FAILED).setTypeAndTitle(400)
							.setMessage(MALFORMED_ID + request.getJsonArray(ID));
			handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
			return null;
		}

		List<String> splitId = new LinkedList<>(Arrays.asList(request.getJsonArray(ID)
				.getString(0).split("/")));
		splitId.remove(splitId.size() - 1);
		final String searchIndex = String.join("__", splitId);
		LOGGER.debug("Index name: " + searchIndex);

		try {
			query = new QueryDecoder().getESquery(request);
		} catch (Exception e) {
			responseBuilder =
					new ResponseBuilder(FAILED)
							.setTypeAndTitle(400)
							.setMessage(e.getMessage());
			handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
			return null;
		}

		if (query.containsKey(ERROR)) {
			LOGGER.error("Fail: Query returned with an error: " + query.getString(ERROR));
			responseBuilder =
					new ResponseBuilder(FAILED).setTypeAndTitle(400)
							.setMessage(query.getString(ERROR));
			handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
			return null;
		}

		LOGGER.info("Info: index: "+ searchIndex);
		LOGGER.info("Info: Query constructed: " + query.toString());

		QueryBuilder queryBuilder = getESquery1(request, true);

		Future.future(future -> client.scrollAsync(searchIndex, queryBuilder));
		responseBuilder = new ResponseBuilder(SUCCESS)
				.setTypeAndTitle(200)
						.setMessage(new JsonArray().add(new JsonObject().put("status","pending")));
		handler.handle(Future.succeededFuture(responseBuilder.getResponse()));
		return null;
	}

	public QueryBuilder getESquery1(JsonObject json,boolean scrollRequest) {
		LOGGER.debug(json);
		String searchType = json.getString(SEARCH_TYPE);
		Boolean isValidQuery = false;
		JsonObject elasticQuery = new JsonObject();

		boolean temporalQuery = false;

		JsonArray id = json.getJsonArray(ID);

		String timeLimit = json.getString(TIME_LIMIT).split(",")[1];
		int numDays = Integer.valueOf(json.getString(TIME_LIMIT).split(",")[2]);

		BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
		boolQuery.filter(QueryBuilders.termsQuery(ID, id.getString(0)));

		if (searchType.matches(GEOSEARCH_REGEX)) {
			boolQuery = new GeoQueryParser(boolQuery, json).parse();
			isValidQuery = true;
		}

		if (searchType.matches(TEMPORAL_SEARCH_REGEX) && json.containsKey(REQ_TIMEREL)
				&& json.containsKey(TIME_KEY)) {
			boolQuery = new TemporalQueryParser(boolQuery, json).parse();
			temporalQuery = true;
			isValidQuery = true;
		}

		if (searchType.matches(ATTRIBUTE_SEARCH_REGEX)) {
			boolQuery = new AttributeQueryParser(boolQuery, json).parse();
			isValidQuery = true;
		}

		JsonArray responseFilters = null;
		if (searchType.matches(RESPONSE_FILTER_REGEX)) {
			LOGGER.debug("Info: Adding responseFilter");
			isValidQuery = true;
			if (!json.getBoolean(SEARCH_KEY)) {
				return null;
			}
			if (json.containsKey(RESPONSE_ATTRS)) {
				responseFilters = json.getJsonArray(RESPONSE_ATTRS);
			} else {
				return null;
			}
		}

		/* checks if any valid search jsons have matched */
		if (!isValidQuery) {
			return null;
		} else {
			if (!temporalQuery && json.getJsonArray("applicableFilters").contains("TEMPORAL")) {
				if (json.getString(TIME_LIMIT).split(",")[0].equalsIgnoreCase(PROD_INSTANCE)) {
					boolQuery
							.filter(QueryBuilders.rangeQuery("observationDateTime")
									.gte("now-" + timeLimit + "d/d"));

				} else if (json.getString(TIME_LIMIT).split(",")[0].equalsIgnoreCase(TEST_INSTANCE)) {
					String endTime = json.getString(TIME_LIMIT).split(",")[1];
					ZonedDateTime endTimeZ = ZonedDateTime.parse(endTime);
					ZonedDateTime startTime = endTimeZ.minusDays(numDays);

					boolQuery
							.filter(QueryBuilders.rangeQuery("observationDateTime")
									.lte(endTime)
									.gte(startTime));

				}

			}
		}

//    elasticQuery.put(QUERY_KEY, new JsonObject(boolQuery.toString()));
//
//    if (responseFilters != null) {
//      elasticQuery.put(SOURCE_FILTER_KEY, responseFilters);
//    }

		return boolQuery;
	}
}
