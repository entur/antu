FROM adoptopenjdk/openjdk11:alpine-jre
WORKDIR /deployments
COPY target/antu-*-SNAPSHOT.jar antu.jar
RUN addgroup appuser && adduser --disabled-password appuser --ingroup appuser
USER appuser
CMD java $JAVA_OPTIONS -jar antu.jar
