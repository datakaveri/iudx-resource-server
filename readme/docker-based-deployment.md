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
Please use [this](multiple-compose-files.md) technique of overriding/merging compose files (i.e. using non-git versioned docker-compose.temp.yml) file.
2. DO NOT MODIFY the git versioned docker compose files (even in your local git clone!) for these things.
3. Modify the git versioned compose files ONLY when the configuration is needed by all (or its for CI - can preferably name it as docker-compose.ci.yml) and commit and push to the repo.
