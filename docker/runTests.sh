#!/bin/bash

nohup mvn clean compile exec:java@resource-server & 
sleep 20
mvn clean test checkstyle:check pmd:check
cp -r target /tmp/test/
