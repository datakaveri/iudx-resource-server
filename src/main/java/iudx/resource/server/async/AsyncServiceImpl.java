package iudx.resource.server.async;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.async.util.Utilities;
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

import java.io.File;
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
	private String filePath;
	private final PostgresService pgService;
	private final S3FileOpsHelper s3FileOpsHelper;
	private final Utilities utilities;



	public AsyncServiceImpl(ElasticClient client, PostgresService pgService,
													S3FileOpsHelper s3FileOpsHelper, String timeLimit, String filePath) {
		this.client = client;
		this.pgService = pgService;
		this.s3FileOpsHelper = s3FileOpsHelper;
		this.timeLimit = timeLimit;
		this.filePath = filePath;
		this.utilities = new Utilities(pgService);
	}

	/**
	 * Performs a fetch from DB of the URL if the given requestID already exists
	 * @param requestID String received to identify incoming request
	 * @param sub String received to identify user
	 * @param scrollJson JsonObject received for scroll API flow
	 * @param handler Handler to return URL in case of success
	 *                and appropriate error message in case of failure
	 */

	@Override
	public AsyncService fetchURLFromDB(String requestID, String sub, JsonObject scrollJson, Handler<AsyncResult<JsonObject>> handler) {
		LOGGER.trace("Info: fetch URL started");

		ZonedDateTime zdt = ZonedDateTime.now();

		StringBuilder query = new StringBuilder(SELECT_S3_SEARCH_SQL
				.replace("$1", requestID));

		pgService.executeQuery(query.toString(), pgHandler -> {
			if(pgHandler.succeeded()) {
				if(pgHandler.result().getJsonArray("result").isEmpty()) {
					// write pending status to DB
					String searchID = UUID.randomUUID().toString();
					Future.future(future -> utilities.writeToDB(searchID, requestID));

					// if db result does not have a matching requestID, ONLY then ES scroll API is called
					scrollQuery(scrollJson, scrollHandler -> {
						if(scrollHandler.succeeded()) {
							JsonArray responseJson = new JsonArray()
									.add(new JsonObject().put("searchID",searchID));

							// respond with searchID for /async/status
							responseBuilder = new ResponseBuilder(SUCCESS).setTypeAndTitle(201).setMessage(responseJson);
							handler.handle(Future.succeededFuture(responseBuilder.getResponse()));

							Future.future(future -> uploadScrollResultToS3(scrollJson.getJsonArray(ID).getString(0)));
						} else {
							handler.handle(Future.failedFuture(scrollHandler.cause()));
						}
					});
				} else {
					JsonArray results = pgHandler.result().getJsonArray("result");
					String s3_url = checkExpiryAndUserID(results,zdt,sub);  // since request ID exists, get or generate the url based on user and/or the expiry of the url
					JsonArray responseJson = new JsonArray()
							.add(new JsonObject().put("file-download-url", s3_url));

					responseBuilder = new ResponseBuilder(SUCCESS).setTypeAndTitle(200).setMessage(responseJson);
					handler.handle(Future.succeededFuture(responseBuilder.getResponse()));
				}
			}
		});
		return null;
	}

	private Future<Void> uploadScrollResultToS3(String id) {
		Promise<Void> promise = Promise.promise();

		List<String> splitId = new LinkedList<>(Arrays.asList(id.split("/")));
		String objectKey = String.join("__", splitId);
		splitId.remove(splitId.size() - 1);
		final String searchIndex = String.join("__", splitId);
		String fileName = filePath
//				.concat("/")
//				.concat(searchIndex)
				.concat("/response.json");
		File file = new File(fileName);
		s3FileOpsHelper.s3Upload(file, objectKey);

		return promise.future();
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
				Future.future(future -> utilities.writeToDB(sub, result));
				return s3_url;
			} else if(userID.equalsIgnoreCase(sub) && !zdt.isBefore(expiry)) {
				String newS3_url = generateNewURL(object_id);
				// TODO: change expiry to long?
				String newExpiry = zdt.plusDays(1).toString();
				Future.future(future -> utilities.updateDBRecord(_id, newS3_url, newExpiry));
				return newS3_url;
			} else if(!userID.equalsIgnoreCase(sub) && !zdt.isBefore(expiry)) {
				String newS3_url = generateNewURL(object_id);
				// TODO: change expiry to long?
				String newExpiry = zdt.plusDays(1).toString();
				Future.future(future -> utilities.writeToDB(sub, newS3_url, newExpiry, result));
				return newS3_url;
			}
		}
		return null;
	}



	private String generateNewURL(String object_id) {

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

		QueryBuilder queryBuilder = utilities.getESquery1(request, true);

		handler.handle(Future.succeededFuture());
		Future.future(future -> client.scrollAsync(searchIndex, queryBuilder));
		return null;
	}


}
