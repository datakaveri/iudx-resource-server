#!/bin/bash

MAJOR_RELEASE=3
MINOR_RELEASE=0
PR_NO=`git ls-remote upstream  'pull/*/head' | grep -F -f <(git log --no-merges -1 --pretty=%h) | awk -F'/' '{print $3}'` # last closed PR, when built from master branch

# To be executed from project root
docker build -t dockerhub.iudx.io/iudx/rs-depl:$MAJOR_RELEASE.$MINOR_RELEASE-$PR_NO -f docker/depl.dockerfile . && \
docker push dockerhub.iudx.io/iudx/rs-depl:$MAJOR_RELEASE.$MINOR_RELEASE-$PR_NO

docker build -t dockerhub.iudx.io/iudx/rs-dev:$MAJOR_RELEASE.$MINOR_RELEASE-$PR_NO -f docker/dev.dockerfile . && \
docker push dockerhub.iudx.io/iudx/rs-dev:$MAJOR_RELEASE.$MINOR_RELEASE-$PR_NO
