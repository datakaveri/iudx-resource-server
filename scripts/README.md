# Docker image of refresh script

```
# Build and push  docker image
docker build -t ghcr.io/datakaveri/rs-refresh-script:\<tag\> -f Dockerfile . && docker push ghcr.io/datakaveri/rs-refresh-script:\<tag\> 
```
# Deploy 
## Bring up container in Docker swarm overlay net

```
docker-compose -f up -d
```

## K8s Deployment

```
# create secret 
kubectl create secret generic rs-script-config --from-file=./script-config.json

# deploy 
kubectl apply -f Deployment.yaml
```  
