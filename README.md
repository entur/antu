# Antu
[![CircleCI](https://circleci.com/gh/entur/antu/tree/main.svg?style=svg)](https://circleci.com/gh/entur/antu/tree/main)

Validate NeTEx datasets against the [Nordic NeTEx Profile](https://enturas.atlassian.net/wiki/spaces/PUBLIC/pages/728891481/Nordic+NeTEx+Profile).

# Data flow
Antu receives NeTEx validation requests from [Marduk](https://github.com/entur/marduk).
The request refers to a given NeTEx codespace and a NeTEx dataset (zip archive) stored in a Google Cloud Storage bucket.  
Antu extracts the individual PublicationDelivery files from the NeTEx archive and register a validation job for each of them in a PubSub topic.  
The resulting workload is then split among the running Antu Kubernetes pods, processed asynchronously and in parallel.
Each validation job produces a JSON-serialized ValidationReport object.  
When all validation jobs are complete, the individual ValidationReports are combined in a single object and stored in GCS under a unique report ID.  
Antu sends a message to a PubSub topic to notify Marduk that the validation is complete.

# Validation rules
Antu uses the [NeTEx validator library](https://github.com/entur/netex-validator-java) to execute a set of validation rules on the NeTEx dataset.  
In addition to the default rules present in this library, Antu defines a set of rules that are specific to Entur and relevant in a Norwegian context.  
This applies to validation against the [National Stop Register](https://stoppested.entur.org/) or against the Organisation Register.

# API
Validation reports can be downloaded thanks to a REST API. Reports are identified by their unique report ID.  
The API is OAuth2-protected and access rights must be sufficient to access a given report.

# Kubernetes integration
Antu is designed so that the validation workload can be split evenly among single-core Kubernetes pods.  
This results in smaller pods, both in terms of CPU and memory consumption, which makes the Kubernetes scheduling process more efficient.
The number of pods is adjusted dynamically thanks to a Horizontal Pod Autoscaler.

# Parallel processing
The Nordic NeTEx Profile mandates that datasets are delivered as single-line files within a zip archive with, optionally, a set of "common files" that gather objects shared between lines.  
This means that validation of individual line files can be run in parallel and mostly independently of one another, with the following exceptions:
 * **validating references** from a line file to a shared object in a "common file" requires that common files are processed first, and line files afterwards, so that all shared ids can be collected before validating the line files. 
 * **validating NeTEx ids uniqueness** across the dataset needs to be synchronized.
Antu uses distributed locks and distributed collections stored in Redis to ensure proper synchronization between concurrent jobs.

# Local environment configuration

A minimal local setup requires a Redis memory store, a Google PubSub emulator and access to the stop place registry ([Baba](https://github.com/entur/tiamat)) and the organization registry.

## Redis memory store
Antu uses a memory store to store the cache of stop places and organizations, as well as temporary files created during the validation process.  
A Docker Redis memory store instance can be used for local testing:
```
docker run -p 6379:6379 --name redis-antu redis:6
```

## Google PubSub emulator
See https://cloud.google.com/pubsub/docs/emulator for details on how to install the Google PubSub emulator.  
The emulator is started with the following command:
```
gcloud beta emulators pubsub start
```
and will listen by default on port 8085.

The emulator port must be set in the Spring Boot application.properties file as well:

```
spring.cloud.gcp.pubsub.emulatorHost=localhost:8085
camel.component.google-pubsub.endpoint=localhost:8085
```

## Access to the stop place registry
Access to the stop place registry is configured in the Spring Boot application.properties file:
```
antu.stop.registry.id.url=https://tiamat
```

## Access to the organization registry
Access to the organization registry is configured in the Spring Boot application.properties file:
```
antu.organisation.registry.url=https://org-reg
```

## Spring boot configuration file
The application.properties file used in unit tests src/test/resources/application.properties can be used as a template.  
The Kubernetes configmap helm/antu/templates/configmap.yaml can also be used as a template.

## Starting the application locally
- Run `mvn package` to generate the Spring Boot jar.
- The application can be started with the following command line:  
  ```java -Xmx500m -Dspring.config.location=/path/to/application.properties -Dfile.encoding=UTF-8 -jar target/antu-0.0.1-SNAPSHOT.jar```

