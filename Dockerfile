# syntax=docker/dockerfile:1
FROM maven:3.8.3-openjdk-16-slim AS build
RUN mkdir /project
COPY . /project
WORKDIR /project
RUN mvn clean package -DskipTests

FROM openjdk:16-alpine
RUN apk add dumb-init
RUN mkdir /app
RUN addgroup --system javauser && adduser -S -s /bin/false -G javauser javauser
COPY --from=build /project/target/beets-1.0-SNAPSHOT-jar-with-dependencies.jar /app/beets.jar
WORKDIR /app
RUN chown -R javauser:javauser /app
USER javauser
CMD "dumb-init" "java" "-jar" "beets.jar" "/run/beets_discord_token"
