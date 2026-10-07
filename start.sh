#!/bin/bash

cd Backend

chmod +x mvnw

./mvnw clean package -DskipTests

java -jar target/ilussioness-backend-0.0.1-SNAPSHOT.jar