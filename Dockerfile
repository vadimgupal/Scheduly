FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY dto/pom.xml dto/pom.xml
COPY core/pom.xml core/pom.xml
COPY bot/pom.xml bot/pom.xml

COPY dto dto
COPY core core
COPY bot bot

ARG MODULE

RUN mvn -B \
    -Dmaven.wagon.http.retryHandler.count=5 \
    -Dmaven.wagon.httpconnectionManager.ttlSeconds=120 \
    -pl ${MODULE} -am clean package -DskipTests

RUN JAR_FILE=$(find ${MODULE}/target -maxdepth 1 -type f -name "*.jar" ! -name "original-*.jar" | head -n 1) && \
    echo "Found jar: $JAR_FILE" && \
    cp "$JAR_FILE" /tmp/app.jar


FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

COPY --from=build /tmp/app.jar /app/app.jar

EXPOSE 8080 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]