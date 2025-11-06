# Antu - Claude AI Assistant Guide

This document provides context and guidance for AI assistants working with the Antu codebase.

## Project Overview

Antu is a NeTEx dataset validation service built by Entur for validating public transportation data against the Nordic NeTEx Profile. It processes validation requests asynchronously using a distributed architecture.

## Architecture

### Core Components
- **Language**: Java 17
- **Framework**: Spring Boot with Apache Camel
- **Build Tool**: Maven
- **Message Queue**: Google Cloud PubSub
- **Cache/Coordination**: Redis (Redisson)
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
- `netex-validator-java` (v11.0.3): Core validation library
- `netex-parser-java` (v3.1.63): NeTEx XML parsing
- `camel-spring-boot` (v4.10.7): Integration framework
- `redisson` (v3.52.0): Distributed data structures
- `entur-helpers` (v5.47.0): Entur-specific utilities

## Project Structure

```
antu/
├── src/main/java/no/entur/antu/
│   ├── routes/          # Camel routes for message processing
│   ├── validation/      # Custom validation rules
│   ├── repository/      # Data access layer
│   └── rest/            # REST API controllers
├── src/test/
├── api/                 # API specifications
├── helm/                # Kubernetes deployment configs
├── terraform/           # Infrastructure as code
└── pom.xml              # Maven build configuration
```

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
mvn clean package
```

### Running Tests
```bash
mvn test
```

### Running Locally
Requirements:
- Redis server (port 6379)
- Google PubSub emulator (port 8085)
- Application properties file configured

```bash
docker run -p 6379:6379 --name redis-antu redis:6
gcloud beta emulators pubsub start
java -Xmx500m -Dspring.config.location=/path/to/application.properties -jar target/antu-0.0.1-SNAPSHOT.jar
```

### Code Formatting
Uses Prettier for Java:
```bash
mvn prettier:write  # Format code
mvn prettier:check  # Check formatting
```

## Important Considerations

### Parallel Processing
- Individual line files can be validated in parallel
- Common files must be processed before line files
- Redis distributed locks coordinate cross-file validations
- NeTEx ID uniqueness validation is synchronized across pods

### Testing
- Unit tests use embedded Redis
- Integration tests use Testcontainers with GCloud emulator
- Test resources in `src/test/resources/application.properties`

### External Dependencies
- **Tiamat**: National Stop Place Registry
- **Organization Registry**: Authority/operator data
- **Marduk**: Orchestration service

## Code Style

- Follow existing patterns in the codebase
- Prettier handles formatting automatically on `mvn validate`
- Use Spring dependency injection
- Camel routes for async processing
- Prefer functional programming style where appropriate

## Key Files to Review

- `pom.xml`: Dependencies and build configuration
- `src/test/resources/application.properties`: Local config template
- `helm/antu/templates/configmap.yaml`: Production config template
- `README.md`: User-facing documentation

## Common Pitfalls

1. **Mac OS PubSub limits**: May need to increase ephemeral port range for large datasets
2. **Memory constraints**: Tests run with `-Xms500m -Xmx500m -Xss512k`
3. **Redis serialization**: Uses Kryo for distributed data structures
4. **NeTEx file structure**: Single-line files in zip with optional common files

## Getting Help

- Check existing tests for usage examples
- Refer to netex-validator-java library documentation
- Review Camel documentation for routing patterns
- Consult Nordic NeTEx Profile specification

## Making Changes

1. Always run existing tests first to establish baseline
2. Make minimal, surgical changes
3. Ensure tests pass after changes
4. Let Prettier handle formatting
5. Update this file if architecture changes significantly
