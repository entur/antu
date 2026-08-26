# Antu - Claude AI Assistant Guide

This document provides context and guidance for AI assistants working with the Antu codebase.

## Project Overview

Antu is a NeTEx dataset validation service built by Entur for validating public transportation data against the Nordic NeTEx Profile. It processes validation requests asynchronously using a distributed architecture.

## Architecture

### Core Components
- **Language**: Java 25
- **Framework**: Spring Boot (no integration framework; PubSub consumers come from `entur-google-pubsub`)
- **Build Tool**: Maven, parent `org.entur.ror:superpom` (sets the Spring Boot version)
- **Message Queue**: Google Cloud PubSub
- **Cache/Coordination**: Redis (Redisson) - report-scoped caches, pipeline barriers, leader election
- **Storage**: Google Cloud Storage
- **Deployment**: Kubernetes with HPA

### Data Flow
1. Receives validation requests from Marduk via PubSub
2. Extracts PublicationDelivery files from NeTEx archives (zip)
3. Registers validation jobs in PubSub topic
4. Distributes workload across Kubernetes pods
5. Validates files in parallel (common files first, then line files)
6. Aggregates validation reports
7. Stores final report in GCS
8. Notifies Marduk via PubSub

### How a validation runs

`ValidationRequestConsumer` reads a request off `AntuNetexValidationQueue`, `ValidationInitializer` derives the report id
and notifies STARTED, and from there every step ends by putting the next `AntuJob` on `AntuJobQueue`. `JobQueueConsumer`
picks jobs up on any pod and `JobDispatcher` switches over the sealed job type:

| Job | Step |
| --- | --- |
| `SplitDataset` | `DatasetSplitter` - explode the archive into the memory store, create the common file jobs |
| `ValidateFile` | `NetexFileValidator` - validate one file, record its report, report to the barriers |
| `CreateLineFileJobs` | `DatasetSplitter` - the common files barrier opened, create the line file jobs |
| `AggregateReports` | `ReportAggregator` - merge the per file reports |
| `ValidateDataset` | `DatasetValidator` - run the validators that need the whole dataset |
| `CompleteValidation` | `ValidationCompleter` - publish the report, notify the client, clean up |
| `Refresh*Cache` | `StopPlaceCacheRefresher` / `OrganisationAliasCacheRefresher` |

The ack deadline is managed by the PubSub streaming pull client, which extends it for up to an hour while a message is
being processed. Nothing in the application touches it.

## Validation Profiles

Antu supports multiple validation profiles with different rule sets:

1. **Timetable**: standard timetable data, including the interchange rules
2. **TimetableFlexibleTransport**: flexible transport services
3. **ImportTimetableFlexibleTransport**: import variant of flexible transport
4. **TimetableFlexibleTransportMerging**: merging validation
5. **Stop**: stop place data

`README.md` lists the rule codes per profile. Which validators a profile runs is wired in
`config/TimetableDataValidatorConfig.java`, `config/StopPlaceDataValidatorConfig.java` and `config/flex/`,
and a few interchange validators are behind feature flags that default to off.

## Common Tasks

### Building the Project
Requires JDK 25; the enforcer fails the build on anything older.
```bash
./mvnw clean package
```

### Running Tests
```bash
./mvnw test
```
Use `clean` after changing a `static final` constant: those inline into call sites and an incremental build
can pass against the old value.

### Running Locally
Requirements:
- Redis server (port 6379)
- Google PubSub emulator (port 8085)
- Application properties file configured

```bash
docker run -p 6379:6379 --name redis-antu redis:7-alpine
gcloud beta emulators pubsub start
java -Xmx500m -Dspring.config.location=/path/to/application.properties -jar target/antu-*.jar
```

### Code Formatting
Uses Prettier for Java:
```bash
./mvnw prettier:write  # Format code
./mvnw prettier:check  # Check formatting
```

## Important Considerations

### Parallel Processing
- Individual line files can be validated in parallel
- Common files must be processed before line files
- Redis distributed locks coordinate cross-file validations
- NeTEx ID uniqueness validation is synchronized across pods

### Testing
- `AntuPipelineTestBase` runs a whole dataset validation in one JVM against embedded Redis and an in-memory blob store,
  with the job queue drained synchronously by `RecordingJobQueue`. No emulator, no polling for a result.
- `PubSubWiringTest` is the only test that starts a PubSub emulator, and it only checks the transport.
- `TestApp` repeats the filters `@SpringBootApplication` would contribute. Dropping `TypeExcludeFilter` there makes every
  `@TestConfiguration` under `no.entur.antu` apply to every test that boots it, so one test's doubles silently replace
  another test's beans. Import the configurations you need instead.
- Test resources in `src/test/resources/application.properties`

### External Dependencies
- **Tiamat**: National Stop Place Registry
- **Organization Registry**: Authority/operator data
- **Marduk**: Orchestration service

## Key Files to Review

- `docs/camel-migration-learnings.md`: the transferable lessons from removing Camel, for whoever migrates
  another service off it. Also the record of what this repo's own design doc originally got wrong about
  library behaviour, which is the most useful caution to carry into writing the next one
- `docs/camel-removal.md`: what the move off Camel changed, and the behaviour differences it introduced. Read its
  *Distributed correctness* and *Shutdown* sections before changing anything under `pipeline/` or `leader/`: they
  are the rules that keep at-least-once delivery across arbitrary pods from producing duplicate or contradictory
  terminal statuses, and several of them were arrived at by getting it wrong first
- `pom.xml`: Dependencies and build configuration
- `src/test/resources/application.properties`: Local config template
- `helm/antu/templates/configmap.yaml`: Production config template
- `README.md`: User-facing documentation

## Common Pitfalls

1. **JDK 25 is required**: the Maven enforcer rejects anything older, and the failure at `validate` names the
   JDK range rather than the cause. Set `JAVA_HOME` before `./mvnw`.
2. **Mac OS PubSub limits**: May need to increase ephemeral port range for large datasets
3. **Memory constraints**: Tests run with `-Xms500m -Xmx500m -Xss512k`
4. **Redis values are a wire format**: two versions of antu read the same Redis during a rollout, and the
   Kryo codec writes fields positionally with no schema header. Adding a field to a cached class makes every
   entry written by the other version unreadable, and a read path that scans a whole hash then throws on the
   first one. Change the shape, change the cache name: `validationProgressCache.v2` is versioned for exactly
   this, and `ValidationStateSerializationTest` fails if you change one without the other.
5. **`RLocalCachedMap.get()` is served from the JVM-local cache**, and holding a distributed lock does not
   flush it. Invalidation is pub/sub only and `reconnectionStrategy` defaults to `NONE`, so a stale entry can
   outlive a reconnect. Never read one for a read-modify-write; read a plain `RMap` view of the same hash
   instead, as `RedisNetexIdRepository.addSharedNetexIds` does. Iteration does go to Redis, so two reads of
   the same map can disagree.
6. **`static final String` values are inlined into call sites**, so changing a constant and running `mvn test`
   can pass against stale bytecode in the referencing classes. Verify constant changes with `mvn clean
   verify`.
7. **NeTEx file structure**: Single-line files in zip with optional common files
8. **Common file ordering is not a thing you can rely on**: `DatasetSplitter` queues common files in reverse name
   order, which puts `_stops.xml` ahead of `_shared_data.xml`, but that orders the *queue* only. Jobs carry no
   ordering key, so delivery order is not publish order, and every pod pulls at once. Measured on a 24 common file
   dataset across ten pods: the file queued first started 19.5 s after another and finished last of the 24. It still
   validated clean, as did two other multi-common-file datasets, so this is a documented non-guarantee rather than a
   known bug. The real guarantee is the `COMMON_FILES_VALIDATED` barrier: no line file runs until every common file
   has. Do not add logic that assumes one common file was validated before another.

## Making Changes

- Run the tests first, so a failure afterwards is attributable.
- Prettier runs in `validate` and gates the build; let `./mvnw prettier:write` do the formatting.
- Changing anything under `pipeline/` or `leader/`: read *Distributed correctness* in
  `docs/camel-removal.md` first. Every step runs on an arbitrary pod against at-least-once delivery, so a
  check followed by an act is not a guard, and a terminal status must have exactly one exit.
- A test for a concurrency fix has to be run against the unfixed code. Several here passed without their
  fix until that was checked; see *Testing* in `docs/camel-migration-learnings.md` for the two shapes that
  fail silently.
- Update this file when the architecture moves, and `docs/camel-removal.md` when the behaviour does.
