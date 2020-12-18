#!/bin/bash

nohup mvn clean compile test-compile exec:java@resource-server & 
sleep 20
mvn clean test
