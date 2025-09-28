# Tools and Environment

## Overview

This directory contains tools and environment configuration documentation within the development viewpoint, covering technology stack, development tools, and environment setup.

## Directory Structure

- **[technology-stack/](technology-stack/)** - Detailed technology stack descriptions and configurations

## Core Documentation

- **[Technology Stack](technology-stack.md)** - Complete technology stack description and configuration guide

## Technology Stack Overview

### Backend Technologies
- **Spring Boot 3.4.5** + Java 21 + Gradle 8.x
- **Spring Data JPA** + Hibernate + Flyway
- **H2** (dev/test) + PostgreSQL (production)
- **SpringDoc OpenAPI 3** + Swagger UI
- **Spring Boot Actuator** + AWS X-Ray + Micrometer

### Frontend Technologies
- **CMC Management**: Next.js 14 + React 18 + TypeScript
- **Consumer App**: Angular 18 + TypeScript
- **UI Components**: shadcn/ui + Radix UI

### Testing Frameworks
- **JUnit 5** + Mockito + AssertJ
- **Cucumber 7** (BDD) + Gherkin
- **ArchUnit** (Architecture Testing)

### Development Tools
- **IDE**: IntelliJ IDEA / VS Code
- **Version Control**: Git + GitHub
- **CI/CD**: GitHub Actions
- **Containerization**: Docker + Kubernetes
- **Cloud Platform**: AWS (CDK deployment)

## Environment Configuration

### Development Environment
- Java 21 JDK
- Node.js 18+
- Docker Desktop
- AWS CLI

### Test Environment
- H2 in-memory database
- TestContainers
- Mock external services

### Production Environment
- AWS EKS
- PostgreSQL RDS
- ElastiCache Redis
- CloudWatch monitoring

## Related Resources

- [Development Standards](../../../../.kiro/steering/development-standards.md)
- [Deployment Guide](../../deployment/)
- [API Documentation](../../../api/README.md)

---

**Maintainer**: Development Team  
**Last Updated**: January 21, 2025  
**Version**: 1.0
![Microservices Overview](../../../diagrams/viewpoints/development/microservices-overview.puml)
![Microservices Overview](../../../diagrams/viewpoints/development/architecture/microservices-overview.mmd)