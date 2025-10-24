# Build Process

> **Last Updated**: 2025-10-23  
> **Status**: Active  
> **Stakeholders**: Developers, Build Engineers, DevOps

## Overview

This document describes the build process, test execution strategy, and continuous integration pipeline for the Enterprise E-Commerce Platform. The system uses Gradle as the build tool with a comprehensive test strategy following the test pyramid approach.

## Build System

### Gradle

**Version**: 8.x (via Gradle Wrapper)

**Why Gradle?**
- Flexible and powerful build automation
- Excellent multi-project support
- Rich plugin ecosystem
- Kotlin DSL support
- Incremental builds for faster feedback

### Project Structure

```
project-root/
├── build.gradle                # Root build configuration
├── settings.gradle             # Project settings
├── gradle.properties           # Build properties
├── gradlew                     # Gradle wrapper (Unix)
├── gradlew.bat                 # Gradle wrapper (Windows)
├── app/
│   ├── build.gradle           # Application module build
│   └── src/
│       ├── main/
│       └── test/
└── gradle/
    ├── libs.versions.toml     # Version catalog
    └── wrapper/
        ├── gradle-wrapper.jar
        └── gradle-wrapper.properties
```

## Build Configuration

### Root build.gradle

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.4.5'
    id 'io.spring.dependency-management' version '1.1.4'
    id 'jacoco'
    id 'pmd'
    id 'checkstyle'
}

group = 'solid.humank'
version = '1.0.0-SNAPSHOT'
sourceCompatibility = '21'

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    
    // Database
    runtimeOnly 'com.h2database:h2'
    runtimeOnly 'org.postgresql:postgresql'
    
    // Testing
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'io.cucumber:cucumber-java:7.14.0'
    testImplementation 'io.cucumber:cucumber-spring:7.14.0'
    testImplementation 'io.cucumber:cucumber-junit-platform-engine:7.14.0'
    testImplementation 'com.tngtech.archunit:archunit-junit5:1.2.1'
}

test {
    useJUnitPlatform()
    finalizedBy jacocoTestReport
}

jacoco {
    toolVersion = "0.8.11"
}

jacocoTestReport {
    dependsOn test
    reports {
        xml.required = true
        html.required = true
    }
}
```

### Version Catalog (gradle/libs.versions.toml)

```toml
[versions]
spring-boot = "3.4.5"
java = "21"
cucumber = "7.14.0"
archunit = "1.2.1"
junit = "5.10.1"

[libraries]
spring-boot-starter-web = { module = "org.springframework.boot:spring-boot-starter-web" }
spring-boot-starter-data-jpa = { module = "org.springframework.boot:spring-boot-starter-data-jpa" }
spring-boot-starter-test = { module = "org.springframework.boot:spring-boot-starter-test" }
cucumber-java = { module = "io.cucumber:cucumber-java", version.ref = "cucumber" }
cucumber-spring = { module = "io.cucumber:cucumber-spring", version.ref = "cucumber" }
archunit-junit5 = { module = "com.tngtech.archunit:archunit-junit5", version.ref = "archunit" }

[plugins]
spring-boot = { id = "org.springframework.boot", version.ref = "spring-boot" }
spring-dependency-management = { id = "io.spring.dependency-management", version = "1.1.4" }
```

## Test Strategy

### Test Pyramid

The project follows the test pyramid approach:

```
        ╱╲
       ╱  ╲
      ╱ E2E ╲         5% - End-to-End Tests
     ╱──────╲        (~3s, ~500MB)
    ╱        ╲
   ╱Integration╲     15% - Integration Tests
  ╱────────────╲    (~500ms, ~50MB)
 ╱              ╲
╱  Unit Tests    ╲   80% - Unit Tests
╱────────────────╲  (~50ms, ~5MB)
```

### Test Classification

Tests are organized into three categories:

1. **Unit Tests** (80%)
   - Fast execution (< 50ms per test)
   - Low memory usage (< 5MB per test)
   - No external dependencies
   - Use `@ExtendWith(MockitoExtension.class)`

2. **Integration Tests** (15%)
   - Moderate execution (< 500ms per test)
   - Moderate memory usage (< 50MB per test)
   - Test with real infrastructure (H2 database)
   - Use `@DataJpaTest`, `@WebMvcTest`, etc.

3. **End-to-End Tests** (5%)
   - Slower execution (< 3s per test)
   - Higher memory usage (< 500MB per test)
   - Full Spring context
   - Use `@SpringBootTest`

### Test Tasks

#### Quick Test (Daily Development)

```gradle
tasks.register('quickTest', Test) {
    description = 'Fast unit tests for daily development (~2 minutes)'
    useJUnitPlatform {
        excludeTags 'integration', 'end-to-end', 'slow'
        includeTags 'unit'
    }
    maxHeapSize = '2g'
    maxParallelForks = Runtime.runtime.availableProcessors()
    forkEvery = 0  // No JVM restart for speed
}
```

**Usage**:
```bash
./gradlew quickTest
```

**When to use**: During active development for fast feedback

#### Unit Test

```gradle
tasks.register('unitTest', Test) {
    description = 'Fast unit tests (~5MB, ~50ms each)'
    useJUnitPlatform {
        excludeTags 'integration', 'end-to-end', 'slow'
        includeTags 'unit'
    }
    maxHeapSize = '2g'
    maxParallelForks = Runtime.runtime.availableProcessors()
    forkEvery = 0
    
    systemProperties = [
        'junit.jupiter.execution.timeout.default': '1m'
    ]
}
```

**Usage**:
```bash
./gradlew unitTest
```

#### Integration Test

```gradle
tasks.register('integrationTest', Test) {
    description = 'Integration tests (~50MB, ~500ms each)'
    useJUnitPlatform {
        includeTags 'integration'
        excludeTags 'end-to-end', 'slow'
    }
    maxHeapSize = '6g'
    minHeapSize = '2g'
    maxParallelForks = 1
    forkEvery = 5
    timeout = Duration.ofMinutes(30)
    
    jvmArgs += [
        '--enable-preview',
        '-XX:MaxMetaspaceSize=1g',
        '-XX:+UseG1GC',
        '-XX:+UseStringDeduplication'
    ]
    
    systemProperties = [
        'junit.jupiter.execution.timeout.default': '2m',
        'spring.profiles.active': 'test'
    ]
}
```

**Usage**:
```bash
./gradlew integrationTest
```

#### End-to-End Test

```gradle
tasks.register('e2eTest', Test) {
    description = 'End-to-end tests (~500MB, ~3s each)'
    useJUnitPlatform {
        includeTags 'end-to-end'
    }
    maxHeapSize = '8g'
    minHeapSize = '3g'
    maxParallelForks = 1
    forkEvery = 2
    timeout = Duration.ofHours(1)
    
    jvmArgs += [
        '--enable-preview',
        '-XX:MaxMetaspaceSize=2g',
        '-XX:+UseG1GC'
    ]
    
    systemProperties = [
        'junit.jupiter.execution.timeout.default': '5m',
        'spring.profiles.active': 'test'
    ]
}
```

**Usage**:
```bash
./gradlew e2eTest
```

#### Cucumber BDD Test

```gradle
tasks.register('cucumber', JavaExec) {
    description = 'Run BDD Cucumber tests'
    mainClass = 'io.cucumber.core.cli.Main'
    classpath = sourceSets.test.runtimeClasspath
    maxHeapSize = '4g'
    
    args = [
        '--plugin', 'pretty',
        '--plugin', 'html:build/reports/cucumber/cucumber-report.html',
        '--plugin', 'json:build/reports/cucumber/cucumber-report.json',
        '--glue', 'solid.humank.genaidemo.bdd',
        'src/test/resources/features'
    ]
}
```

**Usage**:
```bash
./gradlew cucumber
```

#### Pre-Commit Test

```gradle
tasks.register('preCommitTest', Test) {
    description = 'Pre-commit verification (< 5 minutes)'
    dependsOn unitTest, integrationTest
}
```

**Usage**:
```bash
./gradlew preCommitTest
```

#### Full Test

```gradle
tasks.register('fullTest', Test) {
    description = 'Complete test suite including E2E'
    dependsOn unitTest, integrationTest, e2eTest, cucumber
}
```

**Usage**:
```bash
./gradlew fullTest
```

## Code Quality Tasks

### Code Coverage (JaCoCo)

```gradle
jacoco {
    toolVersion = "0.8.11"
}

jacocoTestReport {
    dependsOn test
    
    reports {
        xml.required = true
        html.required = true
        csv.required = false
    }
    
    afterEvaluate {
        classDirectories.setFrom(files(classDirectories.files.collect {
            fileTree(dir: it, exclude: [
                '**/config/**',
                '**/dto/**',
                '**/*Entity.class'
            ])
        }))
    }
}

jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = 0.80  // 80% coverage required
            }
        }
    }
}
```

**Usage**:
```bash
# Generate coverage report
./gradlew test jacocoTestReport

# View report
open build/reports/jacoco/test/html/index.html

# Verify coverage threshold
./gradlew jacocoTestCoverageVerification
```

### Static Analysis (PMD)

```gradle
pmd {
    toolVersion = '6.55.0'
    consoleOutput = true
    ruleSetFiles = files("config/pmd/ruleset.xml")
    ruleSets = []
}

tasks.named('pmdMain') {
    reports {
        xml.required = true
        html.required = true
    }
}
```

**Usage**:
```bash
# Run PMD analysis
./gradlew pmdMain

# View report
open build/reports/pmd/main.html
```

### Code Style (Checkstyle)

```gradle
checkstyle {
    toolVersion = '10.12.5'
    configFile = file("config/checkstyle/checkstyle.xml")
}

tasks.named('checkstyleMain') {
    reports {
        xml.required = true
        html.required = true
    }
}
```

**Usage**:
```bash
# Run Checkstyle
./gradlew checkstyleMain

# View report
open build/reports/checkstyle/main.html
```

### Architecture Tests (ArchUnit)

```gradle
test {
    useJUnitPlatform {
        includeTags 'archunit'
    }
}

tasks.register('archUnit', Test) {
    description = 'Run architecture compliance tests'
    useJUnitPlatform {
        includeTags 'archunit'
    }
}
```

**Usage**:
```bash
# Run architecture tests
./gradlew archUnit
```

## Build Tasks

### Clean Build

```bash
# Clean and build
./gradlew clean build

# Clean only
./gradlew clean
```

### Compile

```bash
# Compile main sources
./gradlew compileJava

# Compile test sources
./gradlew compileTestJava
```

### Package

```bash
# Create executable JAR
./gradlew bootJar

# Output: build/libs/app-1.0.0-SNAPSHOT.jar
```

### Run Application

```bash
# Run with Gradle
./gradlew bootRun

# Run with specific profile
./gradlew bootRun --args='--spring.profiles.active=dev'

# Run JAR directly
java -jar build/libs/app-1.0.0-SNAPSHOT.jar
```

## Development Workflow

### Daily Development Cycle

```bash
# 1. Pull latest changes
git pull origin main

# 2. Run quick tests (2 minutes)
./gradlew quickTest

# 3. Make changes
# ... code changes ...

# 4. Run affected tests
./gradlew test --tests "*CustomerServiceTest"

# 5. Run quick tests again
./gradlew quickTest
```

### Pre-Commit Workflow

```bash
# 1. Run pre-commit tests (5 minutes)
./gradlew preCommitTest

# 2. Check code coverage
./gradlew jacocoTestReport

# 3. Run static analysis
./gradlew pmdMain checkstyleMain

# 4. Run architecture tests
./gradlew archUnit

# 5. Commit if all pass
git commit -m "feat: add customer feature"
```

### Pre-Push Workflow

```bash
# 1. Run full test suite
./gradlew fullTest

# 2. Build application
./gradlew clean build

# 3. Push if all pass
git push origin feature/customer-feature
```

## Continuous Integration

### GitHub Actions Workflow

```yaml
name: CI Build

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: gradle
    
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
    
    - name: Run unit tests
      run: ./gradlew unitTest
    
    - name: Run integration tests
      run: ./gradlew integrationTest
    
    - name: Run architecture tests
      run: ./gradlew archUnit
    
    - name: Generate coverage report
      run: ./gradlew jacocoTestReport
    
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v3
      with:
        files: ./build/reports/jacoco/test/jacocoTestReport.xml
    
    - name: Build application
      run: ./gradlew build
    
    - name: Upload build artifacts
      uses: actions/upload-artifact@v3
      with:
        name: app-jar
        path: build/libs/*.jar
```

### CI Pipeline Stages

```
┌─────────────┐
│   Checkout  │
└──────┬──────┘
       │
┌──────▼──────┐
│  Setup JDK  │
└──────┬──────┘
       │
┌──────▼──────┐
│ Unit Tests  │  (~2 minutes)
└──────┬──────┘
       │
┌──────▼──────────┐
│ Integration     │  (~5 minutes)
│ Tests           │
└──────┬──────────┘
       │
┌──────▼──────────┐
│ Architecture    │  (~1 minute)
│ Tests           │
└──────┬──────────┘
       │
┌──────▼──────────┐
│ Code Coverage   │  (~1 minute)
└──────┬──────────┘
       │
┌──────▼──────────┐
│ Static Analysis │  (~2 minutes)
└──────┬──────────┘
       │
┌──────▼──────────┐
│ Build & Package │  (~2 minutes)
└──────┬──────────┘
       │
┌──────▼──────────┐
│ Upload Artifacts│
└─────────────────┘
```

## Build Optimization

### Gradle Daemon

The Gradle daemon improves build performance:

```properties
# gradle.properties
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true
```

### Build Cache

Enable build cache for faster builds:

```gradle
buildCache {
    local {
        enabled = true
    }
}
```

### Incremental Compilation

```gradle
tasks.withType(JavaCompile) {
    options.incremental = true
}
```

### Parallel Test Execution

```gradle
test {
    maxParallelForks = Runtime.runtime.availableProcessors()
}
```

## Troubleshooting

### Common Issues

#### Out of Memory

**Problem**: `java.lang.OutOfMemoryError: Java heap space`

**Solution**:
```bash
# Increase heap size
./gradlew test -Xmx4g

# Or in gradle.properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
```

#### Test Failures

**Problem**: Tests fail intermittently

**Solution**:
```bash
# Run with more verbose output
./gradlew test --info

# Run specific test
./gradlew test --tests "*CustomerServiceTest.should_create_customer*"

# Clean and retry
./gradlew clean test
```

#### Slow Build

**Problem**: Build takes too long

**Solution**:
```bash
# Enable parallel execution
./gradlew build --parallel

# Use build cache
./gradlew build --build-cache

# Profile build
./gradlew build --profile
# View report: build/reports/profile/profile-*.html
```

#### Dependency Conflicts

**Problem**: Dependency resolution failures

**Solution**:
```bash
# View dependency tree
./gradlew dependencies

# View specific configuration
./gradlew dependencies --configuration runtimeClasspath

# Force dependency version
dependencies {
    implementation('com.example:library:1.0') {
        force = true
    }
}
```

## Best Practices

### 1. Run Tests Frequently

- Run `quickTest` during development
- Run `preCommitTest` before committing
- Run `fullTest` before pushing

### 2. Monitor Build Performance

- Keep build time under 15 minutes
- Optimize slow tests
- Use build cache

### 3. Maintain Test Quality

- Keep test coverage above 80%
- Fix flaky tests immediately
- Write meaningful test names

### 4. Keep Dependencies Updated

```bash
# Check for dependency updates
./gradlew dependencyUpdates

# Update Gradle wrapper
./gradlew wrapper --gradle-version=8.5
```

### 5. Use Gradle Wrapper

Always use `./gradlew` instead of `gradle` to ensure consistent builds across environments.

## Navigation

### Related Documents

- [← Dependency Rules](dependency-rules.md) - Architecture constraints
- [Overview](overview.md) - Development Viewpoint overview
- [Module Organization](module-organization.md) - Package structure

### Related Viewpoints

- [Deployment Viewpoint](../deployment/README.md) - Infrastructure and deployment

### Development Guides

- [Local Environment Setup](../../development/setup/local-environment.md)
- [Testing Strategy](../../development/testing/testing-strategy.md)
- [CI/CD Pipeline](../../development/workflows/ci-cd.md)

---

**Previous**: [← Dependency Rules](dependency-rules.md) | **Next**: [Development Viewpoint Diagrams →](README.md)

