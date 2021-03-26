#!/bin/bash

nohup mvn clean compile exec:java@resource-server & 
sleep 20
mvn clean test -Dtest=AuthenticationServiceTest,DatabaseServiceTest,DataBrokerServiceTest,AdapterEntitiesTest
cp -r target /tmp/test/
