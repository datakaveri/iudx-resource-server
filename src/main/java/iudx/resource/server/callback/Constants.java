package iudx.resource.server.callback;

public class Constants {
	
	public static final String QUEUE_NAME = "queueName";
	
	public static final String ERROR = "error";
	public static final String FAILURE = "failure";
	public static final String SUCCESS = "success";

	public static final String TAGS = "tags";
	public static final String TYPE = "type";
	public static final String TITLE = "title";
	public static final String DETAIL = "detail";

	public static final String MESSAGE = "Message";
	public static final String STATUS = "status";

	public static final String ROUTING_KEY = "Routing Key";
	public static final String ACKMODE = "ackmode";
	public static final String ENCODING = "encoding";

	public static final String URL = "url";
	public static final String CALLBACK_URL = "callbackurl";
	public static final String USER_NAME = "username";
	public static final String PASSWORD = "password";

	public static final String POOL_SIZE = "poolSize";

	public static final String CONTENT_TYPE = "content-type";
	public static final String APPLICATION_JSON = "application/json";

	public static final String CREATE = "create";
	public static final String UPDATE = "update";
	public static final String DELETE = "delete";
	public static final String TABLE_NAME = "tableName";
	public static final String OPERATION = "operation";

	public static final String COLON = " :: ";
	public static final String NEW_LINE = "\n";

	public static final String STATUS_CODE = "Status Code";
	public static final String RESPONSE_BODY = "Response Body";
	public static final String ERROR_MESSAGE = "Error Message";

	public static final String CALLBACK_URL_NOT_FOUND = "Callback Url not found";
	public static final String CALLBACK_SUCCESS = "Data Send to CallBackUrl Successfully";

	public static final String CONNECT_TO_CALLBACK_NOTIFICATION_QUEUE = "Connected to callback.notification queue";
	public static final String CONNECT_TO_CALLBACK_NOTIFICATION_QUEUE_FAIL = "Failed to connect with callback.notification queue";
	
	public static final String CONNECT_TO_CALLBACK_DATA_QUEUE = "Connected to callback.data queue";
	public static final String CONNECT_TO_CALLBACK_DATA_QUEUE_FAIL = "Failed to connect with callback.data queue";
	
	public static final String RABBITMQ_CONSUMER_CREATED = "RabbitMQ consumer created";
	public static final String CONSUME_QUEUE_MESSAGE_FAIL = "Failed to consume message from Queue";
	public static final String QUEUE_CONNECTION_FAIL = "rabbitmq client failed to create connection with Queue";
	public static final String MESSAGE_BODY_NULL = "Message body is NULL";
	public static final String GET_QUEUE_MESSAGE_FAIL = "Failed to get message from queue";
	
	public static final String DATABASE_QUERY_RESULT = "Database Query Result";
	public static final String DATABASE_QUERY_SUCCESS = "Database Query Successfully Done";
	public static final String DATABASE_QUERY_FAIL = "Database Query Failed";
	public static final String DATABASE_OPERATION_INVALID = "Invalid database operation. Operation should be one of [create or update or delete]";
	public static final String DATABASE_OPERATION_NOT_FOUND = "Database operation not found in message body";
	public static final String EXECUTING_SQL_QUERY = "Executing SQL Query on table";
	public static final String FETCH_DATA_FROM_DATABASE = "Fetching Data from DataBase.......!!!!";
	public static final String CACHE_UPDATE_SUCCESS = "Cache Updated Successfully";
	public static final String CACHE_DATA = "Cache Data";
	public static final String EXECUTE_QUERY_FAIL = "Failed to execute Query";
	public static final String CONNECT_DATABASE_FAIL = "Error in Connecting Database";
	public static final String CREATE_PG_CLIENT_OBJECT_FAIL =  "Failed to create pgClient for database query";
	public static final String ROWS = "rows";
	
	public static final String JSON_PARSE_EXCEPTION = "Failed to parse message body";
	public static final String CALLBACK_JSON_OBJECT = "callBackJsonObj";
	public static final String CURRENT_MESSAGE_JSON_OBJECT = "currentMessageJsonObj";
	public static final String CALLBACK_URL_RESPONSE = "CallbackUrl Response";
	public static final String CALLBACK_URL_RESPONSE_NULL = "CallbackUrl response is null";
	public static final String CALLBACK_URL_INVALID = "CallbackUrl is invalid";
	

	public static final String DATA_SEND_TO_CALLBACK_URL_SUCCESS = "Data Send to CallBackUrl Successfully";
	public static final String DATA_SEND_TO_CALLBACK_URL_FAIL  = "Failed to send data to callBackUrl";
	public static final String CREATE_CALLBACK_REQUEST_OBJECT_FAIL = "Failed to create request object for sending callback request";
	public static final String CONNECT_TO_CALLBACK_URL_FAIL  = "Failed to connect callbackUrl";
	public static final String NO_CALLBACK_URL_FOR_ROUTING_KEY = "No callBackUrl exist for routing key";
}
