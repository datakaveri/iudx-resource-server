# Multiple Compose files overriding examples
## Use different config file i.e. config-x.json than the standard two
  -  The secrets directory structure will be :
     ```sh
     secrets/
     └── credentials
        ├── all-verticles-configs
        │   ├── config-depl.json
        │   ├── config-dev.json
        │   └── config-x.json
        ├── keystore.jks
     ```
   - Create a non-git versioned named `docker-compose.temp.yml` file with following contents:
        ```sh 
        version: '3.7'
        networks:
        rs-net:
            driver: bridge
        services:
        rs:
            image: iudx/rs-dev:latest
            volumes:
            - ./secrets/credentials/all-verticles-configs/config-x.json:/usr/share/app/secrets/credentials/all-verticles-configs/config.json
            ports:
            - "8080:80"
            networks: 
            - rs-net
        ```
   - Command to bring up the resource server container is :
        ```sh
        docker-compose -f docker-compose.yml -f docker-compose.temp.yml up -d 
        ```
## Binding the ports of clustered resource-container to host
   - Create a non-git versioned named `docker-compose.temp.yml` file with following contents:
        ```sh
        version: '3.7'
        networks:
        overlay-net:
            external: true      
            driver: overlay
        services:
        rs:
            image: iudx/rs-depl:latest
            volumes:
            - ./secrets/credentials/all-verticles-configs/config-depl.json:/usr/share/app/secrets/credentials/all-verticles-configs/config.json
            ports:
            - "80:80"
            - "9000:9000"
            networks: 
            - overlay-net

        ```
- Command to bring up the resource server container is :
        ```sh
        docker-compose -f docker-compose.yml -f docker-compose.temp.yml up -d 
        ```
## NOTE
- For overriding other miscellaneous configurations through multiple-compose files i.e. docker-compose.yml and docker-compose.temp.yml, refer [here](https://docs.docker.com/compose/extends/). 