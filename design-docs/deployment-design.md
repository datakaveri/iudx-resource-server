# Deploymemnt Design
## Main principles:
- Deploy the resource server in a secure fashion.
- To not edit the docker-compose/deployment files unnecessarily, i.e., git is the only source of truth.
## Four ways of deploying the resource server using docker & docker-swarm:
### 1) Non-Clustered and 2) Clustered one resource-server container
- At any given time only one service is up, hence only one service in compose-file and everything is named as rs/cat
- Two fundamentally different  way of setting up of resource server:
  1. Non-clustered vertx, local devlopment - docker-compose.dev.yml
  2. Clustered vertx - docker-compose.depl.yml
- A base docker-compose file that contains configuration common to both way of setup/deployment - env file,logging, command
- Use of non-git versioned/local directory called "secrets" to store the configs or credentials.
- Usage of environment file called '.rs-api.env' to adjust the environment variables such as LOG_LEVEL, java options etc.  without editing the variables in the compose files.
- Usage of multiple-compose files and its purpose, refer [here](https://docs.docker.com/compose/extends/).
### 3) Clustered one resource-server container using docker stack
- Docker stack facilitates centralised place of deployment i.e. from swarm manager node.
- This requires the docker image to be stored in registry.
- Usage of configs and secrets for api-docs and credentials.
- This is present in [iudx-deployment repo](https://github.com/datakaveri/iudx-deployment/tree/master/single-node/resource-server/apiserver).
### 4) Clustered multiple resource server container using docker stack
- Each verticle is containerised and deployed independently from the same image `rs-depl`, so that each of them can be scaled independently.
- The verticles are differntiated by config credentials passed.
- This requires the docker image to be stored in registry
- Usage of configs and secrets for api-docs and credentials
- This is present in [iudx-deployment repo](https://github.com/datakaveri/iudx-deployment/tree/master/cluster/resource-server/apiserver).


