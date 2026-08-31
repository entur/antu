FROM bellsoft/liberica-openjre-alpine:25.0.4.1 AS builder
WORKDIR /builder
COPY target/*-SNAPSHOT.jar application.jar
RUN java -Djarmode=tools  -jar application.jar extract --layers --destination extracted

FROM bellsoft/liberica-openjre-alpine:25.0.4.1
RUN apk update && apk upgrade && apk add --no-cache tini
WORKDIR /deployments
RUN addgroup appuser && adduser --disabled-password appuser --ingroup appuser
USER appuser
COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./
# JAVA_OPTS rather than JDK_JAVA_OPTIONS: the JVM announces the latter on stderr at every start, and
# Cloud Logging tags that ERROR. The HPA restarts pods all day, so it drowns antu's real errors.
ENTRYPOINT [ "/sbin/tini", "--", "sh", "-c", "exec java $JAVA_OPTS -jar application.jar" ]