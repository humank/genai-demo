# Technology Stack Detailed Description

## Overview

This directory contains detailed configurations and usage guides for the project's technology stack.

## Technology Stack Architecture

### Backend Technology Stack
```
┌─────────────────────────────────────┐
│           API Layer                 │
│    Spring Boot 3.4.5 + Java 21     │
├─────────────────────────────────────┤
│         Application Layer           │
│      Spring Framework 6.x          │
├─────────────────────────────────────┤
│          Domain Layer               │
│         Pure Java + DDD             │
├─────────────────────────────────────┤
│       Infrastructure Layer          │
│  Spring Data JPA + Hibernate       │
│      PostgreSQL + H2               │
└─────────────────────────────────────┘
```

### Frontend Technology Stack
```
┌─────────────────────────────────────┐
│        CMC Management               │
│   Next.js 14 + React 18 + TS       │
├─────────────────────────────────────┤
│       Consumer App                  │
│      Angular 18 + TypeScript       │
├─────────────────────────────────────┤
│        UI Components                │
│     shadcn/ui + Radix UI           │
└─────────────────────────────────────┘
```

## Core Technology Configuration

### Spring Boot Configuration
- **Version**: 3.4.5
- **Java**: 21 (LTS)
- **Build Tool**: Gradle 8.x
- **Packaging**: JAR with embedded Tomcat

### Database Configuration
- **Development Environment**: H2 in-memory database
- **Test Environment**: H2 file database
- **Production Environment**: PostgreSQL 15+
- **Migration Tool**: Flyway

### Monitoring and Observability
- **Health Checks**: Spring Boot Actuator
- **Metrics Collection**: Micrometer + Prometheus
- **Distributed Tracing**: AWS X-Ray
- **Logging**: Logback + Structured Logging

## Development Tools

### IDE and Editors
- **Recommended**: IntelliJ IDEA Ultimate
- **Alternative**: VS Code + Java Extension Pack
- **Configuration**: EditorConfig, Checkstyle

### Version Control
- **Git**: Distributed version control
- **GitHub**: Code hosting and collaboration
- **Branching Strategy**: GitFlow

### CI/CD Tools
- **GitHub Actions**: Continuous integration and deployment
- **Docker**: Containerization
- **AWS CDK**: Infrastructure as Code

## Related Documentation

- [Technology Stack](../technology-stack.md)
- [Development Environment Setup](../../getting-started/)
- [Build and Deployment](../../build-system/)

---

**Maintainer**: Development Team  
**Last Updated**: January 21, 2025
![Microservices Overview](../../../../diagrams/viewpoints/development/microservices-overview.puml)
![Microservices Overview](../../../../diagrams/viewpoints/development/architecture/microservices-overview.mmd)