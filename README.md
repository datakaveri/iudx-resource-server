![IUDX](./docs/iudx.png)
# iudx-resource-server
The resource server is [IUDXs](https://iudx.org.in) data discovery, data publication and data subscription portal.
It allows data providers to publish their data *resources* in accordance to the IUDX vocabulary annotated meta-data document,  data subscribers to query and subscribe for data *resources* as per the consent of the provider.
The consumers can access data from the resource server using HTTPs and AMQPs.

<p align="center">
<img src="./docs/rs_overview.png">
</p>


## Features

- Provides data access from available resources using standard APIs, streaming subscriptions (AMQP) and/or callbacks
- Search and count APIs for searching through available data: Support for Spatial (Circle, Polygon, Bbox, Linestring), Temporal (Before, during, After) and Attribute searches
- Adaptor registration endpoints and streaming endpoints for data ingestion
- Integration with authorization server (token introspection) to serve private data as per the access control policies set by the provider
- Secure data access over TLS
- Scalable, service mesh architecture based implementation using open source components: Vert.X API framework, Elasticsearch/Logstash for database and RabbitMQ for data broker.
- Hazelcast and Zookeeper based cluster management and service discovery

## API Docs 
The api docs can be found [here](https://rs.iudx.org.in/apis).

## Get Started

### Prerequisite - Make configuration
1. Clone this repo and change directory:
   ```sh 
   git clone https://github.com/datakaveri/iudx-resource-server.git && cd iudx-resource-server
   ```
2. Make a config file based on the template in `example-credentials-environment/all-verticles-configs/config-dev.json` for non-clustered vertx and  `example-credentials-environment/all-verticles-configs/config-depl.json` for clustered vertx using hazelcast, zookeeper.
   - Generate a certificate using Lets Encrypt or other methods
   - Make a Java Keystore File and mention its path and password in the appropriate sections
   - Modify the database url and associated credentials in the appropriate sections
   - Populate secrets directory with following structure in the present directory:
      ```sh
      secrets/
      └── credentials
         ├── all-verticles-configs
         │   ├── config-depl.json (needed for clustered vertx setup all verticles  in one container)
         │   └── config-dev.json (needed for non-clustered vertx setup all verticles in one container/maven based setup)
         ├── keystore.jks
         └── one-verticle-configs (needed for clustered vertx in multi-container)
      ``` 
3. Populate .rs-api.env environment file based on template in `example-credentials-environment/example-evironment-file(.rs-api.env)` in the present directory
#### Note
1. DO NOT ADD actual config with credentials to `examples-credentials-enviroment/` directory (even in your local git clone!). 
2. If you would like to add your own config with differnt name than config-dev.json and config-depl.json, place in the `secrets/credentials/all-verticles-configs/` and follow the note sections of docker based and maven based setup.
3. Update all approriate configs in `examples-credentials-enviroment/` ONLY when there is addition of new config parameter options.
### Docker based
1. Install docker and docker-compose
2. Build the images 
   ```sh
    ./docker/build.sh
    ```
3. There are four ways of setting/deploying the resource server using docker:
   1. Non-Clustered setup with all verticles running in a single container: 
   - This needs no hazelcast, zookeeper, the deployment can be done on non-swarm too and suitable for development environment.
   - This makes use of iudx/rs-dev:latest image and config-dev.json present at `secrets/credentials/config-dev.json`
   ```sh 
   # Command to bring up the non-clustered resource-server container
   docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d
   # Command to bring down the non-clustered resource-server container
   docker-compose -f docker-compose.yml -f docker-compose.dev.yml down
   ```
   2. Clustered setup with all verticles running in a single container (using docker-compose): 
   - This needs following things:
      - Docker swarm and overlay network having a name 'overlay-net'. Refer [here](https://github.com/datakaveri/iudx-deployment/tree/master/docs/swarm-setup.md)
      - zookeeper running in Docker swarm and in the overlay network named 'overlay-net'. Refer [here](https://github.com/datakaveri/iudx-deployment/tree/master/single-node/zookeeper)
   - This makes use of iudx/rs-depl:latest image and config-depl.json present at `secrets/credentials/config-depl.json`
   - This is suitable for production/testing environment single node-setup.
     ```sh 
     # Command to bring up the clustered one resource-container
     docker-compose -f docker-compose.yml -f docker-compose.depl.yml up -d
     # Command to bring down the clustered one resource-container
     docker-compose -f docker-compose.yml -f docker-compose.depl.yml down
     ```
   3. [Clustered setup with all verticles running in a single container using docker stack](https://github.com/datakaveri/iudx-deployment/tree/master/single-node/resource-server/apiserver)
   4. [Clustered setup in multi-container using docker stack](https://github.com/datakaveri/iudx-deployment/tree/master/cluster/resource-server/apiserver)
#### Note   
1. If you want to try out or do temporary things, such as 
   - use different config file than the standard two
   - Binding the ports of clustered resource-container to host
   - other miscellaneous changes etc.<br>
Please use [this](readme/multiple-compose-files.md) technique of overriding/merging compose files (i.e. using non-git versioned docker-compose.temp.yml) file.
2. DO NOT MODIFY the git versioned docker compose files (even in your local git clone!) for these things.
3. Modify the git versioned compose files ONLY when the configuration is needed by all (or its for CI - can preferably name it as docker-compose.ci.yml) and commit and push to the repo.


### Maven based
1. Install java 13 and maven
2. Use the maven exec plugin based starter to start the server 
   ```sh 
   mvn clean compile exec:java@resource-server
   ```
#### Note
1. Privileged access maybe required to bring up the http server at port 80. 
2. Maven based setup by default uses `secrets/credentials/all-verticles-configs/config-dev.json` and is non-clustered setup of verticles.
3. If you want to use a different named config called `config-x`, need to place it at `secrets/credentials/all-verticles-configs/config-x.json` and use following command to bring it up:
   ```sh
   mvn clean compile  exec:java@resource-server -Dconfig-dev.file=config-x.json
   ```

### Testing

### Unit tests
1. Run the server through either docker, maven or redeployer
2. Run the unit tests and generate a surefire report 
   `mvn clean test-compile surefire:test surefire-report:report`
3. Reports are stored in `./target/`

### Integration tests
Integration tests are through Postman/Newman whose script can be found from [here](./src/test/resources/IUDX-Resource-Server-Release-v2.0.postman_collection.json).
1. Install prerequisites 
   - [postman](https://www.postman.com/) + [newman](https://www.npmjs.com/package/newman)
   - [newman reporter-htmlextra](https://www.npmjs.com/package/newman-reporter-htmlextra)
2. Example Postman environment can be found [here](./configs/postman-env.json)
3. Run the server through either docker, maven or redeployer
4. Run the integration tests and generate the newman report 
   `newman run <postman-collection-path> -e <postman-environment> --insecure -r htmlextra --reporter-htmlextra-export .`
5. Reports are stored in `./target/`

## Contributing
We follow Git Merge based workflow 
1. Fork this repo
2. Create a new feature branch in your fork. Multiple features must have a hyphen separated name, or refer to a milestone name as mentioned in Github -> Projects 
3. Commit to your fork and raise a Pull Request with upstream

## License
[MIT](./LICENSE.txt)
