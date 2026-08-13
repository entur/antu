# Antu

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
Responses are gzipped when the caller sends `Accept-Encoding: gzip`, through `server.compression`.

# Kubernetes integration

Antu is designed so that the validation workload splits evenly across many small pods rather than a few large
ones, which makes Kubernetes scheduling more efficient. Each pod validates one file at a time
(`jobConsumers`), so heap is bounded per pod and throughput comes from adding pods.

A Horizontal Pod Autoscaler adjusts the pod count from the `AntuJobQueue` backlog and from CPU. It scales up
immediately, because a dataset fans out into its line files all at once and the work is already queued, and
down slowly, because stopping a pod mid-validation costs the shutdown drain and possibly a revalidated file.
Both thresholds live in `helm/antu/values.yaml` under `horizontalPodAutoscaler`, and the HPA acts on whichever
of them asks for more pods.

# Deployment

EnTur deploys Antu using [Harness](https://app.harness.io/ng/account/8VwWgE0WRK67_PWDpkooNA/all/cd/orgs/entur/projects/ror/services/antu)

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

# Coordination

`docs/camel-removal.md` records what the move off Apache Camel changed, which behaviours were kept
identical on purpose, and what to watch when it deploys. Its *Distributed correctness* section is the one to
read before changing how the pipeline coordinates: with no aggregator and no leader on the critical path,
every step runs on an arbitrary pod against at-least-once delivery, and a check followed by an act is not a
guard.

Every step of a validation ends by putting the next job on `AntuJobQueue`, and any pod may pick it up. Two points in the
pipeline have to wait for the whole dataset:

* line files may not be validated until every common file is,
* the individual reports may not be merged until every file has produced one.

Both are counted in Redis: each file records its arrival in a set, and the caller that both completes the set and wins an
atomic claim on it starts the next step. Recording the same file twice is harmless, which is what makes this safe under
PubSub redelivery.

The work that must not run on more than one pod at a time, the stop place changelog consumer, priming the stop place
cache and deciding when the caches are refreshed, is guarded by a leader election, also a lease in Redis.

`docs/camel-migration-learnings.md` is the transferable version of the same material, written for whoever
migrates another service off Camel.

# Local environment configuration

Antu builds and runs on **JDK 25**. The Maven enforcer rejects anything older, so `./mvnw` on an older JDK
fails at `validate` with `Detected JDK ... is not in the allowed range [25,)` rather than anything more
helpful.

A minimal local setup requires a Redis memory store, a Google PubSub emulator and access to the stop place
registry ([Tiamat](https://github.com/entur/tiamat)) and the organization registry.

The whole pipeline can also be run locally with docker-compose or a local Kubernetes cluster from the
[marduk-pipeline](https://github.com/entur/marduk-pipeline) repository, which is usually less work than wiring
the dependencies up by hand.

## Redis memory store

Antu uses a memory store to store the cache of stop places and organizations, as well as temporary files created during
the validation process.  
A Docker Redis memory store instance can be used for local testing:

```
docker run -p 6379:6379 --name redis-antu redis:7-alpine
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
spring.cloud.gcp.pubsub.emulator-host=localhost:8085
```

### Additional instructions for Mac OS when validating large datasets

When validating large datasets, Google PubSub emulator may start to fail processing messages due to Mac OS limitations
on the number of ephemeral ports available for outgoing connections.

This limit can be configured by setting the following values in `/etc/sysctl.conf` (if file does not exist, create it):

```
net.inet.ip.portrange.first=32768
net.inet.ip.portrange.last=65535
```

Mac will need to be restarted for changes to take effect.

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

- Run `./mvnw package` to generate the Spring Boot jar. Requires JDK 25.
- The application can be started with the following command line:  
  ```java -Xmx500m -Dspring.config.location=/path/to/application.properties -Dfile.encoding=UTF-8 -jar target/antu-*.jar```

# Antu rule set

Which rules run depends on the validation profile the request asks for. This list is maintained by hand; the
authoritative source is the validator wiring in `config/TimetableDataValidatorConfig.java`,
`config/StopPlaceDataValidatorConfig.java` and `config/flex/`. Add a validator, add its codes here.

## Profile `Timetable`

| Rule code | Description |
|---|---|
| NETEX_ID_1 | Duplicate element identifiers across files |
| NETEX_ID_2 | Invalid id structure on element |
| NETEX_ID_3 | Invalid structure on id %s. Expected %s |
| NETEX_ID_4 | Use of unapproved codespace. Approved codespaces are %s |
| NETEX_ID_4W | Use of unapproved codespace. Approved codespaces are %s |
| NETEX_ID_5 | Unresolved reference to external reference data |
| NETEX_ID_6 | Reference to %s is not allowed from element %s. Generally an element named XXXXRef may only reference elements of type XXXX |
| NETEX_ID_7 | Invalid id structure on element |
| NETEX_ID_8 | Missing version attribute on elements with id attribute |
| NETEX_ID_9 | Missing version attribute on reference to local elements |
| NETEX_ID_10 | Duplicate element identifiers across common files |
| CODESPACE | Codespace %s is not in the list of valid codespaces for this data space. Valid codespaces are %s |
| VERSION_NON_NUMERIC | Non-numeric NeTEx version |
| INVALID_TRANSPORT_MODE | Invalid transport mode |
| TIMETABLED_PASSING_TIME_INCONSISTENT_TIME | ServiceJourney has inconsistent TimetabledPassingTime |
| TIMETABLED_PASSING_TIME_INCOMPLETE_TIME | ServiceJourney has incomplete TimetabledPassingTime |
| TIMETABLED_PASSING_TIME_NON_INCREASING_TIME | ServiceJourney has non-increasing TimetabledPassingTime |
| HIGH_SPEED | ServiceJourney has too high speed |
| WARNING_SPEED | ServiceJourney has high speed |
| LOW_SPEED | ServiceJourney has low speed |
| SAME_DEPARTURE_ARRIVAL_TIME | Same departure/arrival time for consecutive stops |
| JOURNEY_PATTERN_NO_BOARDING_ALLOWED_AT_LAST_STOP | Last StopPointInJourneyPattern must not allow boarding |
| JOURNEY_PATTERN_NO_ALIGHTING_ALLOWED_AT_FIRST_STOP | First StopPointInJourneyPattern must not allow alighting |
| SAME_STOP_POINT_IN_JOURNEY_PATTERNS | JourneyPatterns have same StopPoints |
| INVALID_NUMBER_OF_SERVICE_LINKS_IN_JOURNEY_PATTERN | Invalid number of ServiceLinks in JourneyPattern |
| SAME_QUAY_REF_IN_CONSECUTIVE_STOP_POINTS_IN_JOURNEY_PATTERN | Same quay refs in consecutive stop points in journey pattern |

### Interchange rules, also profile `Timetable`

| Rule code | Description |
|---|---|
| DUPLICATE_INTERCHANGES | Duplicate interchanges found at %s |
| MISSING_FROM_SERVICE_JOURNEY_IN_INTERCHANGE | Mandatory field FromJourneyRef is missing in ServiceJourneyInterchange |
| MISSING_TO_SERVICE_JOURNEY_IN_INTERCHANGE | Mandatory field ToJourneyRef is missing in ServiceJourneyInterchange |
| MISSING_FROM_STOP_POINT_IN_INTERCHANGE | Mandatory field FromPointRef is missing in ServiceJourneyInterchange |
| MISSING_TO_STOP_POINT_IN_INTERCHANGE | Mandatory field ToPointRef is missing in ServiceJourneyInterchange |
| FROM_POINT_REF_IN_INTERCHANGE_IS_NOT_PART_OF_FROM_JOURNEY_REF | FromPointRef in interchange is not a part of FromJourneyRef |
| TO_POINT_REF_IN_INTERCHANGE_IS_NOT_PART_OF_TO_JOURNEY_REF | ToPointRef in interchange is not a part of ToJourneyRef |
| DISTANCE_BETWEEN_STOP_POINTS_IN_INTERCHANGE_IS_MORE_THAN_MAX_LIMIT | Distance between stop points in interchange is more than maximum limit |
| DISTANCE_BETWEEN_STOP_POINTS_IN_INTERCHANGE_IS_MORE_THAN_WARNING_LIMIT | Distance between stop points in interchange is more than warning limit |

Three interchange validators are behind feature flags and are **off unless the environment enables them**
(see `helm/antu/templates/configmap.yaml`):

| Rule code | Description | Flag |
|---|---|---|
| RULE_SERVICE_JOURNEYS_HAS_TOO_LONG_WAITING_TIME_WARNING | Interchange waiting time is longer than expected | `interchange-waiting-time-validation-enabled` |
| RULE_NO_INTERCHANGE_POSSIBLE | Feeder and consumer vehicle journeys have no interchange possibilities | `interchange-waiting-time-validation-enabled` |
| INTERCHANGE_ALIGHTING_NOT_ALLOWED_FOR_ALIGHTING_STOP | Alighting is not allowed at the interchange alighting stop | `interchange-alighting-and-boarding-validation-enabled` |
| INTERCHANGE_BOARDING_NOT_ALLOWED_FOR_BOARDING_STOP | Boarding is not allowed at the interchange boarding stop | `interchange-alighting-and-boarding-validation-enabled` |
| RULE_NON_EXISTING_SERVICE_JOURNEY_REF | ServiceJourneyInterchange %s refers to non-existing service journey %s | `interchange-service-journey-references-exist-validator-enabled` |
| RULE_NON_EXISTING_STOP_POINT_REF | ServiceJourneyInterchange %s refers to non-existing scheduled stop point %s | `interchange-service-journey-references-exist-validator-enabled` |

## Profiles `TimetableFlexibleTransport` and `ImportTimetableFlexibleTransport`

| Rule code | Description |
|---|---|
| NETEX_FILE_NAME_1 | Invalid filename |
| NETEX_ID_1 | Duplicate element identifiers across files |
| NETEX_ID_2 | Invalid id structure on element |
| NETEX_ID_3 | Invalid structure on id %s. Expected %s |
| NETEX_ID_4 | Use of unapproved codespace. Approved codespaces are %s |
| NETEX_ID_4W | Use of unapproved codespace. Approved codespaces are %s |
| NETEX_ID_5 | Unresolved reference to external reference data |
| NETEX_ID_6 | Reference to %s is not allowed from element %s. Generally an element named XXXXRef may only reference elements of type XXXX |
| NETEX_ID_7 | Invalid id structure on element |
| NETEX_ID_8 | Missing version attribute on elements with id attribute |
| NETEX_ID_9 | Missing version attribute on reference to local elements |
| NETEX_ID_10 | Duplicate element identifiers across common files |
| CODESPACE | Codespace %s is not in the list of valid codespaces for this data space. Valid codespaces are %s |
| VERSION_NON_NUMERIC | Non-numeric NeTEx version |
| INVALID_FLEXIBLE_AREA | Invalid flexible area |

## Profile `TimetableFlexibleTransportMerging`

| Rule code | Description |
|---|---|
| NETEX_ID_1 | Duplicate element identifiers across files |
| NETEX_ID_10 | Duplicate element identifiers across common files |

## Profile `Stop`

| Rule code | Description |
|---|---|
| NETEX_ID_1 | Duplicate element identifiers across files |
| NETEX_ID_2 | Invalid id structure on element |
| NETEX_ID_3 | Invalid structure on id %s. Expected %s |
| NETEX_ID_4 | Use of unapproved codespace. Approved codespaces are %s |
| NETEX_ID_4W | Use of unapproved codespace. Approved codespaces are %s |
| NETEX_ID_5 | Unresolved reference to external reference data |
| NETEX_ID_6 | Reference to %s is not allowed from element %s. Generally an element named XXXXRef may only reference elements of type XXXX |
| NETEX_ID_7 | Invalid id structure on element |
| NETEX_ID_8 | Missing version attribute on elements with id attribute |
| NETEX_ID_9 | Missing version attribute on reference to local elements |
| NETEX_ID_10 | Duplicate element identifiers across common files |
