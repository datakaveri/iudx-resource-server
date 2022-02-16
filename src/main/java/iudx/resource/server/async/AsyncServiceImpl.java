package iudx.resource.server.async;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.database.archives.ResponseBuilder;
import iudx.resource.server.database.archives.elastic.ElasticClient;
import iudx.resource.server.database.archives.elastic.QueryDecoder;
import iudx.resource.server.database.archives.elastic.GeoQueryParser;
import iudx.resource.server.database.archives.elastic.AttributeQueryParser;
import iudx.resource.server.database.archives.elastic.TemporalQueryParser;
import iudx.resource.server.database.postgres.PostgresService;
import iudx.resource.server.database.archives.S3FileOpsHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import java.net.URL;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static iudx.resource.server.apiserver.util.Constants.USER_ID;
import static iudx.resource.server.database.archives.Constants.SUCCESS;
import static iudx.resource.server.database.archives.Constants.SEARCH_KEY;
import static iudx.resource.server.database.archives.Constants.TIME_LIMIT;
import static iudx.resource.server.database.archives.Constants.ID;
import static iudx.resource.server.database.archives.Constants.ID_NOT_FOUND;
import static iudx.resource.server.database.archives.Constants.FAILED;
import static iudx.resource.server.database.archives.Constants.EMPTY_RESOURCE_ID;
import static iudx.resource.server.database.archives.Constants.SEARCH_TYPE;
import static iudx.resource.server.database.archives.Constants.SEARCHTYPE_NOT_FOUND;
import static iudx.resource.server.database.archives.Constants.MALFORMED_ID;
import static iudx.resource.server.database.archives.Constants.GEOSEARCH_REGEX;
import static iudx.resource.server.database.archives.Constants.TEMPORAL_SEARCH_REGEX;
import static iudx.resource.server.database.archives.Constants.REQ_TIMEREL;
import static iudx.resource.server.database.archives.Constants.TIME_KEY;
import static iudx.resource.server.database.archives.Constants.ATTRIBUTE_SEARCH_REGEX;
import static iudx.resource.server.database.archives.Constants.ERROR;
import static iudx.resource.server.database.archives.Constants.RESPONSE_FILTER_REGEX;
import static iudx.resource.server.database.archives.Constants.RESPONSE_ATTRS;
import static iudx.resource.server.database.archives.Constants.PROD_INSTANCE;
import static iudx.resource.server.database.archives.Constants.TEST_INSTANCE;
import static iudx.resource.server.database.postgres.Constants.SELECT_S3_SEARCH_SQL;
import static iudx.resource.server.database.postgres.Constants.INSERT_S3_PENDING_SQL;
import static iudx.resource.server.database.postgres.Constants.INSERT_S3_READY_SQL;
import static iudx.resource.server.database.postgres.Constants.UPDATE_S3_URL_SQL;


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
	private final PostgresService pgService;



	public AsyncServiceImpl(ElasticClient client, PostgresService pgService, String timeLimit) {
		this.client = client;
		this.pgService = pgService;
		this.timeLimit = timeLimit;
	}

	/**
	 * Performs a fetch from DB of the URL if the given requestID already exists
	 * @param routingContext Routing Context instance received from AsyncRestApi
	 * @param handler Handler to return URL in case of success
	 *                and appropriate error message in case of failure
	 */

	@Override
	public AsyncService fetchURLFromDB(RoutingContext routingContext, JsonObject scrollJson, Handler<AsyncResult<JsonObject>> handler) {
		LOGGER.trace("Info: fetch URL started");

		ZonedDateTime zdt = ZonedDateTime.now();
		String sub = ((JsonObject) routingContext.data().get("authInfo")).getString(USER_ID); // get sub from AuthHandler result
		String requestID = UUID.fromString(routingContext.request().absoluteURI()).toString(); // generate UUID from the absolute URI of the HTTP Request

		StringBuilder query = new StringBuilder(SELECT_S3_SEARCH_SQL
				.replace("$1", requestID));

		pgService.executeQuery(query.toString(), pgHandler -> {
			if(pgHandler.succeeded()) {
				if(pgHandler.result().getJsonArray("result").isEmpty()) {
					// if db result does not have a matching requestID, ONLY then ES scroll API is called
					scrollQuery(scrollJson, scrollHandler -> {
						if(scrollHandler.succeeded()) {
							String searchID = UUID.randomUUID().toString();
							JsonArray responseJson = new JsonArray()
									.add(new JsonObject().put("searchID",searchID));

							// write pending status to DB
							Future.future(future -> writeToDB(searchID, requestID));

							// respond with searchID for /async/status
							responseBuilder = new ResponseBuilder(SUCCESS).setTypeAndTitle(201).setMessage(responseJson);
							handler.handle(Future.succeededFuture(responseBuilder.getResponse()));
						} else {
							handler.handle(Future.failedFuture(scrollHandler.cause()));
						}
					});
				} else {
					JsonArray results = pgHandler.result().getJsonArray("result");
					String s3_url = checkExpiryAndUserID(results,zdt,sub);
					JsonArray responseJson = new JsonArray()
							.add(new JsonObject().put("file-download-url", s3_url));

					responseBuilder = new ResponseBuilder(SUCCESS).setTypeAndTitle(200).setMessage(responseJson);
					handler.handle(Future.succeededFuture(responseBuilder.getResponse()));
				}
			}
		});
		return null;
	}

	private String checkExpiryAndUserID(JsonArray results, ZonedDateTime zdt, String sub) {

		for(Object json : results) {
			JsonObject result = (JsonObject) json;
			String _id = result.getString("_id");
			String userID = result.getString("user_id");
			ZonedDateTime expiry = ZonedDateTime.parse(result.getString("expiry"));
			String s3_url = result.getString("s3_url");
			String object_id = result.getString("object_id");

			if(userID.equalsIgnoreCase(sub) && zdt.isBefore(expiry)) {
				return s3_url;
			} else if(!userID.equalsIgnoreCase(sub) && zdt.isBefore(expiry)) {
				Future.future(future -> writeToDB(sub, result));
				return s3_url;
			} else if(userID.equalsIgnoreCase(sub) && !zdt.isBefore(expiry)) {
				String newS3_url = generateNewURL(object_id);
				// TODO: change expiry to long?
				String newExpiry = zdt.plusDays(1).toString();
				Future.future(future -> updateDBRecord(_id, newS3_url, newExpiry));
				return newS3_url;
			} else if(!userID.equalsIgnoreCase(sub) && !zdt.isBefore(expiry)) {
				String newS3_url = generateNewURL(object_id);
				// TODO: change expiry to long?
				String newExpiry = zdt.plusDays(1).toString();
				Future.future(future -> writeToDB(sub, newS3_url, newExpiry, result));
				return newS3_url;
			}
		}
		return null;
	}

	private Future<Void> writeToDB(StringBuilder query) {
		Promise<Void> promise = Promise.promise();

		pgService.executeQuery(query.toString(), pgHandler -> {
			if(pgHandler.succeeded()) {
				promise.complete();
			} else {
				LOGGER.error("Insert/update into DB failed");
				promise.fail("Insert/update fail");
			}
		});
		return promise.future();
	}

	private Future<Void> writeToDB(String searchID, String requestID) {

		StringBuilder query = new StringBuilder(INSERT_S3_PENDING_SQL
				.replace("$1",UUID.randomUUID().toString())
				.replace("$2", searchID)
				.replace("$3", requestID)
				.replace("$4", "Pending"));

		return writeToDB(query);
	}

	private Future<Void> writeToDB(String sub, JsonObject result) {

		StringBuilder query = new StringBuilder(INSERT_S3_READY_SQL
				.replace("$1",UUID.randomUUID().toString())
				.replace("$2",UUID.randomUUID().toString())
				.replace("$3",result.getString("request_id"))
				.replace("$4",result.getString("status"))
				.replace("$5",result.getString("s3_url"))
				.replace("$6", result.getString("expiry"))
				.replace("$7",sub)
				.replace("$8",result.getString("object_id")));

		return writeToDB(query);
	}

	private Future<Void> writeToDB(String sub, String s3_url,String expiry, JsonObject result) {

		result.remove("s3_url");
		result.remove("expiry");
		result.put("s3_url",s3_url);
		result.put("expiry",expiry);

		return writeToDB(sub,result);
	}

	private Future<Void> updateDBRecord(String _id, String s3_url, String expiry) {

		StringBuilder query = new StringBuilder(UPDATE_S3_URL_SQL
				.replace("$1",s3_url)
				.replace("$2",expiry)
				.replace("$3",_id));

		return writeToDB(query);
	}

	private String generateNewURL(String object_id) {
		S3FileOpsHelper s3FileOpsHelper = new S3FileOpsHelper();

		long expiry = TimeUnit.DAYS.toMillis(1);
		URL s3_url  = s3FileOpsHelper.generatePreSignedUrl(expiry,object_id);
		return s3_url.toString();
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

		handler.handle(Future.succeededFuture());
		Future.future(future -> client.scrollAsync(searchIndex, queryBuilder));
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
