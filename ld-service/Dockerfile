# syntax=docker/dockerfile:1

# Build context is the REPO ROOT, not this directory (ld-service depends on the sibling modules
# and the parent pom):
#   docker build -f ld-service/Dockerfile -t ld-service:local .
# docker-compose.yml in this directory does exactly that.

FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
COPY . .
# Tests run in CI (mvn verify); the image build only needs the jar. The cache mount keeps
# ~/.m2 between builds so a rebuild doesn't re-download every dependency.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl ld-service -am -DskipTests package \
    && cp ld-service/target/ld-service-*.jar /build/app.jar

FROM eclipse-temurin:25-jre
# curl is only here for the HEALTHCHECK below (the JRE image doesn't ship it).
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app && useradd --system --gid app --create-home app
# The analysis writes its report/log files (immuno.log, *.csv, ...) to the working directory,
# so give it one the non-root user owns instead of the read-only /app. The reference-data cache
# directory is created here, owned by app, so a named volume mounted over it (docker-compose.yml)
# inherits that ownership -- otherwise Docker creates the volume root-owned and the non-root
# user can't write the cache.
WORKDIR /data
RUN mkdir -p /home/app/.dash-cache \
    && chown app:app /data /home/app/.dash-cache
COPY --from=build /build/app.jar /app/app.jar
USER app
EXPOSE 8080

# A frequencyFiles upload is parsed fully into heap at ~4-6x the file size (the ~1GB NMDP
# nine-locus release needs ~6g), so size the heap off the container's memory rather than a
# fixed -Xmx that fails on smaller machines. Override with JAVA_OPTS (e.g. "-Xmx6g"). Note
# Docker Desktop's VM defaults to a couple of GB: raise it in Settings > Resources for large uploads.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
