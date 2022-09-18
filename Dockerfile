# syntax=docker/dockerfile:1
FROM maven:3.8.3-openjdk-16-slim AS build
RUN mkdir /project
COPY . /project
WORKDIR /project
RUN mvn clean package -DskipTests

FROM openjdk:16-slim
RUN apt-get update
RUN apt-get install build-essential -y
RUN mkdir /app
COPY --from=build /project/target/beets-1.0-SNAPSHOT-jar-with-dependencies.jar /app/beets.jar
WORKDIR /app
CMD "java" "-jar" "beets.jar" "/run/beets_discord_token"
