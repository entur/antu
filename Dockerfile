FROM eclipse-temurin:17.0.4_8-jre-alpine as builder
COPY target/antu-*-SNAPSHOT.jar application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM eclipse-temurin:17.0.4_8-jre-alpine
RUN apk update && apk upgrade && apk add --no-cache tini
WORKDIR /deployments
RUN addgroup appuser && adduser --disabled-password appuser --ingroup appuser
USER appuser
COPY --from=builder dependencies/ ./
COPY --from=builder snapshot-dependencies/ ./
COPY --from=builder spring-boot-loader/ ./
COPY --from=builder application/ ./
ENTRYPOINT [ "/sbin/tini", "--", "java", "org.springframework.boot.loader.JarLauncher" ]
