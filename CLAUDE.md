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

### Key Dependencies
- `netex-validator-java`: Core validation library
- `netex-parser-java`: NeTEx XML parsing
- `redisson`: Distributed data structures
- `entur-helpers`: Entur-specific utilities, including `AbstractEnturGooglePubSubConsumer`

Versions live in pom.xml; do not trust versions written in this file.

## Project Structure

```
antu/
├── src/main/java/no/entur/antu/
│   ├── job/             # Job model (sealed AntuJob), dispatch, MDC
│   ├── pubsub/          # PubSub consumers, publisher, wire codec
│   ├── pipeline/        # The validation steps and the barriers between them
│   ├── leader/          # Redis lease leader election
│   ├── validation/      # Custom validation rules
│   ├── rest/            # Spring MVC controllers
│   ├── config/          # Spring configuration
│   ├── stop/            # Stop place registry and changelog
│   └── services/        # Blob store access
├── src/test/
├── helm/                # Kubernetes deployment configs
├── terraform/           # Infrastructure as code
└── pom.xml              # Maven build configuration
```

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

1. **Timetable**: Standard timetable data validation (~25 rules)
2. **TimetableFlexibleTransport**: Flexible transport services (~15 rules)
3. **ImportTimetableFlexibleTransport**: Import variant of flexible transport
4. **TimetableFlexibleTransportMerging**: Merging validation (~2 rules)
5. **Stop**: Stop place data validation (~11 rules)

## Common Tasks

### Building the Project
```bash
./mvnw clean package
```

### Running Tests
```bash
./mvnw test
```

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

## Code Style

- Follow existing patterns in the codebase
- Prettier handles formatting automatically on `./mvnw validate`
- Use Spring dependency injection
- Prefer functional programming style where appropriate

## Key Files to Review

- `docs/camel-removal.md`: what the move off Camel changed, and the behaviour differences it introduced. Read its
  *Distributed correctness* and *Shutdown* sections before changing anything under `pipeline/` or `leader/`: they
  are the rules that keep at-least-once delivery across arbitrary pods from producing duplicate or contradictory
  terminal statuses, and several of them were arrived at by getting it wrong first
- `pom.xml`: Dependencies and build configuration
- `src/test/resources/application.properties`: Local config template
- `helm/antu/templates/configmap.yaml`: Production config template
- `README.md`: User-facing documentation

## Common Pitfalls

1. **Mac OS PubSub limits**: May need to increase ephemeral port range for large datasets
2. **Memory constraints**: Tests run with `-Xms500m -Xmx500m -Xss512k`
3. **Redis serialization**: Uses Kryo for distributed data structures
4. **NeTEx file structure**: Single-line files in zip with optional common files
5. **Common file ordering is not a thing you can rely on**: `DatasetSplitter` queues common files in reverse name
   order, which puts `_stops.xml` ahead of `_shared_data.xml`, but that orders the *queue* only. Jobs carry no
   ordering key, so delivery order is not publish order, and every pod pulls at once. Measured on a 24 common file
   dataset across ten pods: the file queued first started 19.5 s after another and finished last of the 24. It still
   validated clean, as did two other multi-common-file datasets, so this is a documented non-guarantee rather than a
   known bug. The real guarantee is the `COMMON_FILES_VALIDATED` barrier: no line file runs until every common file
   has. Do not add logic that assumes one common file was validated before another.

## Getting Help

- Check existing tests for usage examples
- Refer to netex-validator-java library documentation
- Consult Nordic NeTEx Profile specification

## Making Changes

1. Always run existing tests first to establish baseline
2. Make minimal, surgical changes
3. Ensure tests pass after changes
4. Let Prettier handle formatting
5. Update this file if architecture changes significantly
