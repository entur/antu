FROM bellsoft/liberica-openjre-alpine:25.0.5.1 AS builder
WORKDIR /builder
COPY target/*-SNAPSHOT.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# Temurin JRE, no shell, no package manager. The pod securityContext sets the runtime uid, and the JVM handles
# SIGTERM itself as PID 1 for Spring's graceful shutdown.
FROM gcr.io/distroless/java25-debian13:nonroot
WORKDIR /deployments
COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./
# JVM options come from argument files the chart mounts from the JAVA_OPTS and JAVA_OPTS_EXTRA ConfigMap keys,
# since there is no shell to expand them. Not JDK_JAVA_OPTIONS: the launcher announces that on stderr at every
# start, Cloud Logging tags it ERROR, and the HPA restarts pods all day, so it drowns antu's real errors.
# Both files must exist: the launcher refuses to start on a missing one.
ENTRYPOINT [ "/usr/bin/java", "@/etc/jvm/java-opts", "@/etc/jvm/java-opts-extra", "-jar", "application.jar" ]
