# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Pragmatica Aether is a distributed runtime environment for Java that enables transformation of monolithic applications into distributed ones while maintaining simplicity. It uses a slice-based architecture where functionality is deployed as isolated units.

## Key Commands

### Building the Project
```bash
# Build all modules
./mvnw clean install

# Build without tests
./mvnw clean install -DskipTests

# Deploy to repository
./script/deploy.sh

# Check for dependency updates
./script/check-dependency-updates.sh

# Update Maven wrapper
./script/update-wrapper.sh
```

### Running Tests
```bash
# Run all tests
./mvnw test

# Run tests for specific module
./mvnw test -pl slice

# Run single test class
./mvnw test -Dtest=MavenLocalRepoLocatorTest

# Run with Java preview features (required)
./mvnw test -Dargline="--enable-preview"
```

## Architecture Overview

### Module Structure
- **slice-api**: Public API defining slice interfaces and entry points
- **slice**: Core implementation including classloaders, repositories, and deployment
- **node**: Runtime node with networking and serialization capabilities

### Core Concepts

1. **Slices**: Deployable units identified by Maven coordinates (GroupId:ArtifactId:Version)
2. **Entry Points**: Well-defined interfaces for slice interaction (must extend `SliceApi`)
3. **Repositories**: Systems for locating and retrieving artifacts (Maven-compatible)
4. **Isolated Class Loading**: Each slice runs in its own classloader to prevent conflicts

### Key Classes

- `SliceStore`: Manages loaded slices with concurrent access (slice/src/main/java/org/pragmatica/aether/slice/SliceStore.java)
- `SliceClassLoader`: Provides isolation between slices (slice/src/main/java/org/pragmatica/aether/slice/classloader/SliceClassLoader.java)
- `Repository`: Abstract artifact location/retrieval (slice/src/main/java/org/pragmatica/aether/slice/repository/Repository.java)
- `NodeDeploymentManager`: Handles cluster-wide deployment (node/src/main/java/org/pragmatica/aether/node/deployment/NodeDeploymentManager.java)

### Development Notes

- **Java 21 with preview features**: All code uses `--enable-preview` flag
- **Functional style**: Heavy use of Pragmatica-Lite functional programming patterns
- **Immutable data**: Prefer records and immutable collections
- **Error handling**: Use `Result<T>` monad for error propagation
- **Testing**: JUnit 5 with AssertJ assertions, Mockito for mocking, Awaitility for async tests

### Repository Access

This project depends on Pragmatica-Lite framework hosted on GitHub Packages. Ensure your Maven settings.xml has proper GitHub authentication configured for the repository at https://maven.pkg.github.com/siy/pragmatica-lite.