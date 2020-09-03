#!/bin/bash

# To be executed from project root
docker build -t iudx/rs-db:latest -f docker/db.dockerfile .
docker build -t iudx/rs-auth:latest -f docker/auth.dockerfile .
docker build -t iudx/rs-api:latest -f docker/api.dockerfile .
docker build -t iudx/rs-broker:latest -f docker/broker.dockerfile .
docker build -t iudx/rs-call:latest -f docker/call.dockerfile .
docker build -t iudx/rs-all:latest -f docker/all.dockerfile .
docker build -t iudx/rs-dev:latest -f docker/dev.dockerfile .
