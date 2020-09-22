ARG VERSION="0.0.1-SNAPSHOT"

FROM maven:latest as dependencies

WORKDIR /usr/share/app
COPY pom.xml .
RUN mvn clean package

FROM dependencies as builder

WORKDIR /usr/share/app
COPY pom.xml .
COPY src src
RUN mvn clean package -Dmaven.test.skip=true


FROM openjdk:14-slim-buster

ARG VERSION
ENV JAR="iudx.resource.server-cluster-${VERSION}-fat.jar"

WORKDIR /usr/share/app
COPY --from=builder /usr/share/app/target/${JAR} ./fatjar.jar
