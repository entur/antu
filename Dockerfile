FROM adoptopenjdk/openjdk11:alpine-jre as builder
COPY target/antu-*-SNAPSHOT.jar application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM adoptopenjdk/openjdk11:alpine-jre
RUN apk add --no-cache tini
WORKDIR /deployments
RUN addgroup appuser && adduser --disabled-password appuser --ingroup appuser
RUN mkdir /deployments/truststore && chown appuser:appuser /deployments/truststore
USER appuser
COPY --from=builder dependencies/ ./
COPY --from=builder snapshot-dependencies/ ./
COPY --from=builder spring-boot-loader/ ./
COPY --from=builder application/ ./
ENTRYPOINT [ "/sbin/tini", "--", "java", "org.springframework.boot.loader.JarLauncher" ]
