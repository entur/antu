# Antu

[![CircleCI](https://circleci.com/gh/entur/antu/tree/main.svg?style=svg)](https://circleci.com/gh/entur/antu/tree/main)

Validate NeTEx datasets against
the [Nordic NeTEx Profile](https://enturas.atlassian.net/wiki/spaces/PUBLIC/pages/728891481/Nordic+NeTEx+Profile).

# Data flow

Antu receives NeTEx validation requests from [Marduk](https://github.com/entur/marduk).
The request refers to a given NeTEx codespace and a NeTEx dataset (zip archive) stored in a Google Cloud Storage
bucket.  
Antu extracts the individual PublicationDelivery files from the NeTEx archive and register a validation job for each of
them in a PubSub topic.  
The resulting workload is then split among the running Antu Kubernetes pods, processed asynchronously and in parallel.
Each validation job produces a JSON-serialized ValidationReport object.  
When all validation jobs are complete, the individual ValidationReports are combined in a single object and stored in
GCS under a unique report ID.  
Antu sends a message to a PubSub topic to notify Marduk that the validation is complete.

# Validation rules

Antu uses the [NeTEx validator library](https://github.com/entur/netex-validator-java) to execute a set of validation
rules on the NeTEx dataset.  
In addition to the default rules present in this library, Antu defines a set of rules that are specific to Entur and
relevant in a Norwegian context.  
This applies to validation against the [National Stop Register](https://stoppested.entur.org/) or against the
Organisation Register.

# API

Validation reports can be downloaded thanks to a REST API. Reports are identified by their unique report ID.  
The API is OAuth2-protected and access rights must be sufficient to access a given report.

# Kubernetes integration

Antu is designed so that the validation workload can be split evenly among single-core Kubernetes pods.  
This results in smaller pods, both in terms of CPU and memory consumption, which makes the Kubernetes scheduling process
more efficient.
The number of pods is adjusted dynamically thanks to a Horizontal Pod Autoscaler.

# Parallel processing

The Nordic NeTEx Profile mandates that datasets are delivered as single-line files within a zip archive with,
optionally, a set of "common files" that gather objects shared between lines.  
This means that validation of individual line files can be run in parallel and mostly independently of one another, with
the following exceptions:

* **validating references** from a line file to a shared object in a "common file" requires that common files are
  processed first, and line files afterwards, so that all shared ids can be collected before validating the line files.
* **validating NeTEx ids uniqueness** across the dataset needs to be synchronized.
  Antu uses distributed locks and distributed collections stored in Redis to ensure proper synchronization between
  concurrent jobs.

# Local environment configuration

A minimal local setup requires a Redis memory store, a Google PubSub emulator and access to the stop place
registry ([Baba](https://github.com/entur/tiamat)) and the organization registry.

## Redis memory store

Antu uses a memory store to store the cache of stop places and organizations, as well as temporary files created during
the validation process.  
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

The application.properties file used in unit tests src/test/resources/application.properties can be used as a
template.  
The Kubernetes configmap helm/antu/templates/configmap.yaml can also be used as a template.

## Starting the application locally

- Run `mvn package` to generate the Spring Boot jar.
- The application can be started with the following command line:  
  ```java -Xmx500m -Dspring.config.location=/path/to/application.properties -Dfile.encoding=UTF-8 -jar target/antu-0.0.1-SNAPSHOT.jar```

# Antu rule set

Antu comes with the following rule sets, depending on the validation profile used for validation:

### For validation Profile `Timetable`

| Sr. | Rule Code                                          |                                                      Rule Description                                                       |
|-----|----------------------------------------------------|:---------------------------------------------------------------------------------------------------------------------------:|
| 1   | NETEX_ID_4                                         |                                   Use of unapproved codespace. Approved codespaces are %s                                   |
| 2   | NETEX_ID_4W                                        |                                   Use of unapproved codespace. Approved codespaces are %s                                   |
| 3   | NETEX_ID_2                                         |                                               Invalid id structure on element                                               |
| 4   | NETEX_ID_3                                         |                                           Invalid structure on id %s. Expected %s                                           |
| 5   | NETEX_ID_8                                         |                                   Missing version attribute on elements with id attribute                                   |
| 6   | NETEX_ID_9                                         |                                  Missing version attribute on reference to local elements                                   |
| 7   | NETEX_ID_6                                         | Reference to %s is not allowed from element %s. Generally an element named XXXXRef may only reference elements if type XXXX |
| 8   | NETEX_ID_7                                         |                                               Invalid id structure on element                                               |
| 9   | NETEX_ID_5                                         |                                       Unresolved reference to external reference data                                       |
| 10  | NETEX_ID_1                                         |                                         Duplicate element identifiers across files                                          |
| 11  | NETEX_ID_10                                        |                                      Duplicate element identifiers across common files                                      |
| 12  | INVALID_TRANSPORT_MODE                             |                                                   Invalid transport mode                                                    |
| 13  | TIMETABLED_PASSING_TIME_INCONSISTENT_TIME          |                                    ServiceJourney has inconsistent TimetabledPassingTime                                    |
| 14  | TIMETABLED_PASSING_TIME_INCOMPLETE_TIME            |                                     ServiceJourney has incomplete TimetabledPassingTime                                     |
| 15  | TIMETABLED_PASSING_TIME_NON_INCREASING_TIME        |                                   ServiceJourney has non-increasing TimetabledPassingTime                                   |
| 16  | HIGH_SPEED                                         |                                              ServiceJourney has too high speed                                              |
| 17  | LOW_SPEED                                          |                                                ServiceJourney has low speed                                                 |
| 18  | WARNING_SPEED                                      |                                                ServiceJourney has high speed                                                |
| 19  | SAME_DEPARTURE_ARRIVAL_TIME                        |                                      Same departure/arrival time for consecutive stops                                      |
| 20  | CODESPACE                                          |              Codespace %s is not in the list of valid codespaces for this data space. Valid codespaces are %s               |
| 21  | VERSION_NON_NUMERIC                                |                                                  Non-numeric NeTEx version                                                  |
| 22  | JOURNEY_PATTERN_NO_BOARDING_ALLOWED_AT_LAST_STOP   |                                   Last StopPointInJourneyPattern must not allow boarding                                    |
| 23  | JOURNEY_PATTERN_NO_ALIGHTING_ALLOWED_AT_FIRST_STOP |                                  First StopPointInJourneyPattern must not allow alighting                                   |
| 23  | SAME_STOP_POINT_IN_JOURNEY_PATTERNS                |                                            JourneyPatterns have same StopPoints                                             |
| 24  | INVALID_NUMBER_OF_SERVICE_LINKS_IN_JOURNEY_PATTERN |                                      Invalid number of ServiceLinks in JourneyPattern                                       |

### For validation Profiles `TimetableFlexibleTransport` and `ImportTimetableFlexibleTransport`

| Sr. | Rule Code           |                                                      Rule Description                                                       |
|-----|---------------------|:---------------------------------------------------------------------------------------------------------------------------:|
| 1   | NETEX_FILE_NAME_1   |                                                      Invalid filename                                                       |
| 2   | NETEX_ID_4W         |                                   Use of unapproved codespace. Approved codespaces are %s                                   |
| 3   | NETEX_ID_2          |                                               Invalid id structure on element                                               |
| 4   | NETEX_ID_3          |                                           Invalid structure on id %s. Expected %s                                           |
| 5   | NETEX_ID_4          |                                   Use of unapproved codespace. Approved codespaces are %s                                   |
| 6   | NETEX_ID_8          |                                   Missing version attribute on elements with id attribute                                   |
| 7   | NETEX_ID_9          |                                  Missing version attribute on reference to local elements                                   |
| 8   | NETEX_ID_6          | Reference to %s is not allowed from element %s. Generally an element named XXXXRef may only reference elements if type XXXX |
| 9   | NETEX_ID_7          |                                               Invalid id structure on element                                               |
| 10  | NETEX_ID_5          |                                       Unresolved reference to external reference data                                       |
| 11  | NETEX_ID_1          |                                         Duplicate element identifiers across files                                          |
| 12  | NETEX_ID_10         |                                      Duplicate element identifiers across common files                                      |
| 13  | CODESPACE           |              Codespace %s is not in the list of valid codespaces for this data space. Valid codespaces are %s               |
| 14  | VERSION_NON_NUMERIC |                                                  Non-numeric NeTEx version                                                  |

### For validation Profile `TimetableFlexibleTransportMerging`

| Sr. | Rule Code   |                 Rule Description                  |
|-----|-------------|:-------------------------------------------------:|
| 1   | NETEX_ID_10 | Duplicate element identifiers across common files |
| 2   | NETEX_ID_1  |    Duplicate element identifiers across files     |

### For validation Profile `Stop`

| Sr. | Rule Code   |                                                      Rule Description                                                       |
|-----|-------------|:---------------------------------------------------------------------------------------------------------------------------:|
| 1   | NETEX_ID_4W |                                   Use of unapproved codespace. Approved codespaces are %s                                   |
| 2   | NETEX_ID_2  |                                               Invalid id structure on element                                               |
| 3   | NETEX_ID_3  |                                           Invalid structure on id %s. Expected %s                                           |
| 4   | NETEX_ID_4  |                                   Use of unapproved codespace. Approved codespaces are %s                                   |
| 5   | NETEX_ID_8  |                                   Missing version attribute on elements with id attribute                                   |
| 6   | NETEX_ID_9  |                                  Missing version attribute on reference to local elements                                   |
| 7   | NETEX_ID_6  | Reference to %s is not allowed from element %s. Generally an element named XXXXRef may only reference elements if type XXXX |
| 8   | NETEX_ID_7  |                                               Invalid id structure on element                                               |
| 9   | NETEX_ID_5  |                                       Unresolved reference to external reference data                                       |
| 10  | NETEX_ID_1  |                                         Duplicate element identifiers across files                                          |
| 11  | NETEX_ID_10 |                                      Duplicate element identifiers across common files                                      |
