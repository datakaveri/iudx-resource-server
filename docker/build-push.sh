#!/bin/bash

MAJOR_RELEASE=2
MINOR_RELEASE=0
MONTH=6        # 'x'th month from last major.minor release
COMMIT_ID=`git log -1 --pretty=%h` # last commit-id in short form

# To be executed from project root
docker build -t dockerhub.iudx.io/iudx/rs-depl:$MAJOR_RELEASE.$MINOR_RELEASE.$MONTH-$COMMIT_ID -f docker/depl.dockerfile . && \
docker push dockerhub.iudx.io/iudx/rs-depl:$MAJOR_RELEASE.$MINOR_RELEASE.$MONTH-$COMMIT_ID

docker build -t dockerhub.iudx.io/iudx/rs-dev:$MAJOR_RELEASE.$MINOR_RELEASE.$MONTH-$COMMIT_ID -f docker/dev.dockerfile . && \
docker push dockerhub.iudx.io/iudx/rs-dev:$MAJOR_RELEASE.$MINOR_RELEASE.$MONTH-$COMMIT_ID
