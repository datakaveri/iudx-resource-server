SETUP GUIDE
----

This document contains the installation and configuration processes
of the external modules of each Verticle in IUDX Resource Server.

<p align="center">
<img src="./readme/images/rs-architecture.png">
</p>

The Resource Server connects with various external dependencies namely
- `ELK` stack : used to capture and query temporal and spatial data.
- `PostgreSQL` :  used to store and query data related to
  - Token Invalidation
  - Query and status information of Async APIs
  - Subscription Status
  - Unique attributes
- `ImmuDB` : used to store metering information
- `RabbitMQ` : used to
  - publish and subscribe data
  - registration of publishers and subscribers
  - broadcast token invalidation info
- `Redis` : used to query latest data
- `AWS S3` : used to upload files for async search

The Resource Server also connects with various DX dependencies namely
- Authorization Server : used to download the certificate for token decoding
- Catalogue Server : used to download the list of resources, access policies and query types supported on a resource.

## Setting up ELK for IUDX Resource Server
- Refer to the docker files available [here](https://github.com/datakaveri/iudx-deployment/blob/master/Docker-Swarm-deployment/single-node/elk) to setup ELK stack

**Note** : Access to HTTP APIs for search functionality should be configured with TLS and RBAC privileges

In order to connect to the appropriate Elasticsearch database, required information such as databaseIP,databasePort etc. should be updated in the DatabaseVerticle and AsyncVerticle modules available in [config-example.json](configs/config-example.json).

**DatabaseVerticle**
```
{
    "id": "iudx.resource.server.database.archives.DatabaseVerticle",
    "isWorkerVerticle": false,
    "verticleInstances": <num-of-verticle-instance>,
    "databaseIP": "localhost",
    "databasePort": <port-number>,
    "dbUser": <username-for-es>,
    "dbPassword": <password-for-es>,
    "timeLimit": "test,2020-10-22T00:00:00Z,10"
}
```

**AsyncVerticle**
```
{
    "id": "iudx.resource.server.database.async.AsyncVerticle",
    "isWorkerVerticle":true,
    "threadPoolName":<name-of-the-thread-pool>,
    "threadPoolSize":<thread-pool-size,
    "verticleInstances": <number-of-verticle-instances>,
    "databaseIP": "localhost",
    "databasePort": <port-number>,
    "dbUser": <username-for-db>,
    "dbPassword": <password-for-db>,
    "filePath": <path/to/file>,
    "bucketName": <aws-s3-bucket-name>
}
```

----

## Setting up PostgreSQL for IUDX Resource Server
-  Refer to the docker files available [here](https://github.com/datakaveri/iudx-deployment/blob/master/Docker-Swarm-deployment/single-node/postgres) to setup PostgreSQL

**Note** : PostgresQL database should be configured with a RBAC user having CRUD privileges

In order to connect to the appropriate Postgres database, required information such as databaseIP,databasePort etc. should be updated in the PostgresVerticle and DataBrokerVerticle modules available in [config-example.json](configs/config-example.json).

**PostgresVerticle**
```
{
    "id": "iudx.resource.server.database.postgres.PostgresVerticle",
    "isWorkerVerticle":false,
    "verticleInstances": <num-of-verticle-instances>,
    "databaseIp": "localhost",
    "databasePort": <port-number>,
    "databaseName": <database-name>,
    "databaseUserName": <username-for-psql>,
    "databasePassword": <password-for-psql>,
    "poolSize": <pool-size>
}
```

**DataBrokerVerticle**
```
{
    id": "iudx.resource.server.databroker.DataBrokerVerticle",
    "isWorkerVerticle":false,
    "verticleInstances": <num-of-verticle-instances>,
    "dataBrokerIP": "localhost",
    "dataBrokerPort": <port-number>,
    "dataBrokerVhost": <vHost-name>,
    "dataBrokerUserName": <username-for-rmq>,
    "dataBrokerPassword": <password-for-rmq>,
    "dataBrokerManagementPort": <management-port-number>,
    "connectionTimeout": <time-in-milliseconds>,
    "requestedHeartbeat": <time-in-seconds>,
    "handshakeTimeout": <time-in-milliseconds>,
    "requestedChannelMax": <num-of-max-channels>,
    "networkRecoveryInterval": <time-in-milliseconds>,
    "automaticRecoveryEnabled": "true",
    "postgresDatabaseIP": "localhost",
    "postgresDatabasePort": <postgres-port-number>,
    "postgresDatabaseName": <postgres-database-name>,
    "postgresDatabaseUserName": <username-for-postgres-db>,
    "postgresDatabasePassword": <password-for-postgres-db>,
    "postgrespoolSize": <postgres-pool-size>,
    "brokerAmqpIp": "localhost",
    "brokerAmqpPort": <amqp-port-number>
}
```


#### Schemas for PostgreSQL tables in IUDX Resource Server
1. Token Invalidation Table Schema
```
CREATE TABLE IF NOT EXISTS revoked_tokens
(
   _id uuid NOT NULL,
   expiry timestamp with time zone NOT NULL,
   created_at timestamp without time zone NOT NULL,
   modified_at timestamp without time zone NOT NULL,
   CONSTRAINT revoke_tokens_pk PRIMARY KEY (_id)
);
```

2. Asynchronous query and status information table schema
```

CREATE TABLE IF NOT EXISTS s3_upload_url
(
   _id uuid NOT NULL,
   search_id uuid NOT NULL,
   request_id TEXT NOT NULL,
   status Query_Progress NOT NULL,
   progress numeric(5,2),
   s3_url varchar,
   expiry timestamp without time zone,
   user_id varchar,
   object_id varchar,
   created_at timestamp without time zone NOT NULL,
   modified_at timestamp without time zone NOT NULL,
   CONSTRAINT upload_url_pk PRIMARY KEY (_id)
);
```
3. Subscription Table
```
CREATE TABLE IF NOT EXISTS subscriptions
(
   _id varchar NOT NULL,
   _type sub_type NOT NULL,
   queue_name varchar NOT NULL,
   entity varchar NOT NULL,
   expiry timestamp without time zone NOT NULL,
   created_at timestamp without time zone NOT NULL,
   modified_at timestamp without time zone NOT NULL,
   CONSTRAINT sub_pk PRIMARY KEY
   (
      queue_name,
      entity
   )
);
```

4. Unique Attributes Table
```
CREATE TABLE IF NOT EXISTS unique_attributes
(
   _id uuid DEFAULT uuid_generate_v4 () NOT NULL,
   resource_id varchar NOT NULL,
   unique_attribute varchar NOT NULL,
   created_at timestamp without time zone NOT NULL,
   modified_at timestamp without time zone NOT NULL,
   CONSTRAINT unique_attrib_pk PRIMARY KEY (_id),
   CONSTRAINT resource_id_unique UNIQUE (resource_id)
);
```

5. Streaming User Info Table
```
CREATE TABLE IF NOT EXISTS databroker 
(
   username character varying (255) NOT NULL,
   password character varying (50) NOT NULL,
   created_at timestamp without time zone NOT NULL,
   modified_at timestamp without time zone NOT NULL,
   CONSTRAINT databroker_pkey PRIMARY KEY (username),
   CONSTRAINT databroker_username_key UNIQUE (username)
);
```
----

## Setting up ImmuDB for IUDX Resource Server
- Refer to the docker files available [here](https://github.com/datakaveri/iudx-deployment/blob/master/Docker-Swarm-deployment/single-node/immudb) to setup ImmuDB.

In order to connect to the appropriate ImmuDB database, required information such as meteringDatabaseIP,meteringDatabasePort etc. should be updated in the MeteringVerticle module available in [config-example.json](configs/config-example.json).

**MeteringVerticle**

```
{
    "id": "iudx.resource.server.metering.MeteringVerticle",
    "isWorkerVerticle":false,
    "verticleInstances": <num-of-verticle-instances>,
    "meteringDatabaseIP": "localhost",
    "meteringDatabasePort": <port-number>,
    "meteringDatabaseName": <database-name>,
    "meteringDatabaseUserName": <username-for-immudb>,
    "meteringDatabasePassword": <password-for-immudb>,
    "meteringPoolSize": <pool-size>
}
```

**Metering Table Schema**
```
CREATE TABLE IF NOT EXISTS rsauditingtable
(
    id uuid NOT NULL,
    api varchar NOT NULL,
    userid varchar NOT NULL,
    epochtime integer NOT NULL,
    resourceid varchar NOT NULL,
    isotime timestamp with timezone NOT NULL,
    providerid varchar NOT NULL,
    CONSTRAINT metering_pk PRIMARY KEY (id)
);
```

----

## Setting up RabbitMQ for IUDX Resource Server
- Refer to the docker files available [here](https://github.com/datakaveri/iudx-deployment/blob/master/Docker-Swarm-deployment/single-node/databroker) to setup RMQ.


In order to connect to the appropriate RabbitMQ instance, required information such as dataBrokerIP,dataBrokerPort etc. should be updated in the DataBrokerVerticle module available in [config-example.json](configs/config-example.json).

**DataBrokerVerticle**
```
{
    id": "iudx.resource.server.databroker.DataBrokerVerticle",
    "isWorkerVerticle":false,
    "verticleInstances": <num-of-verticle-instances>,
    "dataBrokerIP": "localhost",
    "dataBrokerPort": <port-number>,
    "dataBrokerVhost": <vHost-name>,
    "dataBrokerUserName": <username-for-rmq>,
    "dataBrokerPassword": <password-for-rmq>,
    "dataBrokerManagementPort": <management-port-number>,
    "connectionTimeout": <time-in-milliseconds>,
    "requestedHeartbeat": <time-in-seconds>,
    "handshakeTimeout": <time-in-milliseconds>,
    "requestedChannelMax": <num-of-max-channels>,
    "networkRecoveryInterval": <time-in-milliseconds>,
    "automaticRecoveryEnabled": "true",
    "postgresDatabaseIP": "localhost",
    "postgresDatabasePort": <postgres-port-number>,
    "postgresDatabaseName": <postgres-database-name>,
    "postgresDatabaseUserName": <username-for-postgres-db>,
    "postgresDatabasePassword": <password-for-postgres-db>,
    "postgrespoolSize": <postgres-pool-size>,

    "brokerAmqpIp": "localhost",
    "brokerAmqpPort": <amqp-port-number>
}
```

----

## Setting up Redis for IUDX Resource Server
- Refer to the docker files available [here](https://github.com/datakaveri/iudx-deployment/blob/master/Docker-Swarm-deployment/single-node/redis)


In order to connect to the appropriate Redis instance, required information such as redisHost,redisPort etc. should be updated in the LatestVerticle module available in [config-example.json](configs/config-example.json).

**LatestVerticle**
```
   "id": "iudx.resource.server.database.latest.LatestVerticle",
   "isWorkerVerticle":false,
    "verticleInstances": <num-of-verticle-instances>,
    "redisMode": <mode>,
    "redisUsername": <username-for-redis>,
    "redisPassword": <password-for-redis>,
    "redisMaxPoolSize": <pool-size>,
    "redisMaxPoolWaiting": <max-pool-waiting>,
    "redisMaxWaitingHandlers": <max-waiting-handlers>,
    "redisPoolRecycleTimeout": <recycle-timeout-in milliseconds>,
    "redisHost": "localhost",
    "redisPort": <port-number>,
}
```
----

## Setting up AWS S3 for IUDX Resource Server

In order to connect to AWS S3,
- Create AWS account
- From the AWS Console, create a new user with permission set to `AmazonS3FullAccess`.
- Download the credentials `AWS_ACCESS_KEY_ID` , `AWS_SECRET_ACCESS_KEY` and add them to the environment variables
- Create a new s3 bucket and add it's name to the AsyncVerticle module available in [config-example.json](configs/config-example.json)
- 


**AsyncVerticle**
```
{
    "id": "iudx.resource.server.database.async.AsyncVerticle",
    "isWorkerVerticle":true,
    "threadPoolName":<name-of-the-thread-pool>,
    "threadPoolSize":<thread-pool-size,
    "verticleInstances": <number-of-verticle-instances>,
    "databaseIP": "localhost",
    "databasePort": <port-number>,
    "dbUser": <username-for-db>,
    "dbPassword": <password-for-db>,
    "filePath": <path/to/file>,
    "bucketName": <aws-s3-bucket-name>
}
```

## Connecting with DX Catalogue Server

In order to connect to the DX catalogue server, required information such as catServerHost,catServerPort etc. should be updated in the AuthenticationVerticle and ApiServerVerticle modules availabe in [config-example.json](configs/config-example.json).

**AuthenticationVerticle**
```
{
    "id": "iudx.resource.server.authenticator.AuthenticationVerticle",
    "isWorkerVerticle":false,
    "verticleInstances": <number-of-verticle-instances,
    "audience": <resource-server-host>,
    "keystore": <path/to/keystore.jks>,
    "keystorePassword": <password-for-keystore>,
    "authServerHost": <auth-server-host>,
    "catServerHost": <catalogue-server-host>,
    "catServerPort": <catalogue-server-port>,
    "jwtIgnoreExpiry": <true | false>
}
```

**ApiServerVerticle**
```
{
    "id": "iudx.resource.server.apiserver.ApiServerVerticle",
    "isWorkerVerticle":false,
    "ssl": true,
    "keystore": <path/to/keystore.jks>,
    "keystorePassword": <password-for-keystore>,
    "httpPort": <port-to-listen>,
    "verticleInstances": <number-of-verticle-instances>,
    "catServerHost": <catalogue-server-host>,
    "catServerPort": <catalogue-server-port>
}
```

## Connecting with DX Authorization Server

In order to connect to the DX authentication server, required information such as authServerHost should be updated in the AuthenticationVerticle module availabe in [config-example.json](configs/config-example.json).

**AuthenticationVerticle**
```
{
    "id": "iudx.resource.server.authenticator.AuthenticationVerticle",
    "isWorkerVerticle":false,
    "verticleInstances": <number-of-verticle-instances,
    "audience": <resource-server-host>,
    "keystore": <path/to/keystore.jks>,
    "keystorePassword": <password-for-keystore>,
    "authServerHost": <auth-server-host>,
    "catServerHost": <catalogue-server-host>,
    "catServerPort": <catalogue-server-port>,
    "jwtIgnoreExpiry": <true | false>
}
```
