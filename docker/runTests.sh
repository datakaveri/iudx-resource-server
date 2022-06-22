#!/bin/bash

nohup mvn clean compile exec:java@resource-server & 
sleep 40
mvn clean test
cp -r target /tmp/test/
