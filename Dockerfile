FROM eclipse-temurin:21-jdk AS build
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
ENV SERVER_PORT=8080
ENV TZ=Asia/Singapore
COPY --from=build /build/target/project-0.0.1-SNAPSHOT.jar app.jar
# A numeric account keeps the application away from root privileges.
USER 10001:10001
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=10 CMD curl -fsS http://localhost:8080${SERVER_SERVLET_CONTEXT_PATH:-}/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
