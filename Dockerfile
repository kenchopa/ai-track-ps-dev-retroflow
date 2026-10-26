# syntax=docker/dockerfile:1

# ---- deps: resolve Maven dependencies once, cached until pom.xml changes ----
FROM maven:3.9-eclipse-temurin-21 AS deps
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# ---- test: run with `docker compose run --rm test` ----
FROM deps AS test
COPY src ./src
CMD ["mvn", "-B", "test"]

# ---- build: package the jar (tests are run separately via the test target) ----
FROM deps AS build
COPY src ./src
RUN mvn -B -q -Dmaven.test.skip=true package \
 && java -Djarmode=tools -jar target/retroflow-*.jar extract --layers --launcher --destination target/extracted

# ---- runtime: slim JRE, non-root, layered for fast rebuilds ----
FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S retroflow && adduser -S retroflow -G retroflow
WORKDIR /app
COPY --from=build /workspace/target/extracted/dependencies/ ./
COPY --from=build /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/target/extracted/application/ ./
USER retroflow
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
HEALTHCHECK --interval=15s --timeout=3s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
