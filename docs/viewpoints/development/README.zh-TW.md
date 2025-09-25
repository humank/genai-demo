# Development Viewpoint (繁體中文版)

> **注意**: 此文件需要翻譯。原始英文版本請參考對應的英文文件。

# Development Viewpoint

## Overview

The Development Viewpoint provides comprehensive development guidelines and best practices, integrating all development patterns, technology stacks, and toolchains in the project. This viewpoint follows the Rozanski & Woods architecture methodology, providing unified development standards for developers, architects, and technical teams.

## Introduction

The Development Viewpoint is a core component of software architecture that defines how to build, test, and maintain high-quality software systems. This viewpoint covers the complete development lifecycle from code writing to deployment, ensuring teams can collaborate in a consistent and efficient manner.

### Core Values
- **Consistency**: Unified development standards and practices
- **Quality**: Ensure code quality through TDD/BDD and code reviews
- **Efficiency**: Improve development efficiency through automated tools and processes
- **Maintainability**: Clear architectural design and documentation
- **Collaboration**: Promote team knowledge sharing and collaboration

### Scope of Application
This viewpoint applies to all team members involved in software development, including:
- Software Development Engineers
- Architects and Technical Leaders
- DevOps Engineers
- Test Engineers
- Product Managers and Project Managers

## 🚀 Quick Start

### 🎯 Getting Started
- [📚 Quick Start Guide](getting-started.md) - Complete beginner's guide including environment setup, project structure, and first contribution

### 🏗️ Core Concepts
- [🏗️ Architecture Design](architecture/) - DDD, Hexagonal Architecture, Microservices, Saga Pattern
- [📋 Coding Standards](coding-standards.md) - Java, Frontend, API Design and Documentation Standards
- [🧪 Testing Strategy](testing/) - TDD, BDD, Performance Testing, Architecture Testing

## 🏗️ Architecture and Design Patterns

### DDD Domain-Driven Design
- 🎯 DDD Domain-Driven Design - Complete DDD implementation guide
  - @AggregateRoot Aggregate Roots - Event collection and management
  - @ValueObject Value Objects - Record implementation pattern
  - @Entity Entities - Business logic encapsulation
  - @DomainService Domain Services - Cross-aggregate business logic
  - 📡 Domain Events - Record implementation, event collection and publishing

### Hexagonal Architecture
- 🔵 Hexagonal Architecture - Complete hexagonal architecture guide
  - Hexagonal Architecture Overview - Core concepts and architectural principles
  - 🔌 Port-Adapter Pattern - Port and adapter design
  - 🔄 Dependency Inversion - Application of dependency inversion principle
  - 📚 Layered Design - Clear layer responsibility division

### Microservices Architecture
- 🌐 Microservices Architecture - Microservices design and implementation
  - Microservices design principles
  - 🚪 API Gateway configuration
  - 🔍 Service Discovery implementation
  - ⚖️ Load Balancing strategies
  - 🔧 Circuit Breaker pattern

### Saga Pattern
- 🎭 Saga Pattern - Distributed transaction processing
  - Saga Pattern Overview
  - 🎼 Orchestration-based Saga implementation
  - 💃 Choreography-based Saga design
  - 🛒 Order Processing Saga example
  - 💳 Payment Saga flow

## 🧪 Testing and Quality Assurance

### TDD Test-Driven Development & BDD Behavior-Driven Development
- 🧪 TDD & BDD Complete Guide - Test-Driven Development and Behavior-Driven Development
  - 🔴🟢🔵 Red-Green-Refactor cycle
  - 🏗️ Test Pyramid - Unit, Integration, End-to-End testing
  - ⚡ Unit Testing Patterns - Test builders and naming conventions
  - 📝 Gherkin Syntax - BDD scenario description language
  - 📋 Given-When-Then pattern
  - 🎬 Feature file writing guide
  - 🎯 Scenario design principles
  - 🔗 Integration testing strategy
  - ⚡ Performance Testing - @TestPerformanceExtension
  - 🏛️ Architecture Testing - ArchUnit rules
  - 🤖 Test Automation - CI/CD integration

## 🛠️ Technology Stack and Toolchain

### Environment Management and Profile Architecture
- 🎯 [Profile Management Strategy](profile-management.md) - Three-stage Profile architecture guide
  - 🏠 Local Profile - Local development environment (H2 + Redis)
  - 🧪 Test Profile - CI/CD testing environment (minimal configuration)
  - 🎭 Staging Profile - AWS pre-production environment (complete simulation)
  - 🚀 Production Profile - AWS production environment (enterprise-grade)
  - 🗄️ Database Strategy - JPA + Flyway integrated management
  - 📊 [Profile Dependencies Matrix](../../PROFILE_DEPENDENCIES_MATRIX.md)
  - 🔧 [Database Configuration Matrix](../../DATABASE_CONFIGURATION_MATRIX.md)

### Complete Technology Stack Guide
- 🛠️ Technology Stack and Toolchain - Complete technology stack integration guide
  - ☕ Spring Boot 3.4.5 + Java 21 + Gradle 8.x - Backend core technology
  - 🗄️ PostgreSQL + H2 + Flyway - Database technology stack
  - 📊 Spring Boot Actuator + AWS X-Ray - Monitoring and tracing
  - ⚛️ Next.js 14 + React 18 - CMC management interface
  - 🅰️ Angular 18 + TypeScript - Consumer application
  - 🎨 shadcn/ui + Radix UI - UI component library
  - 🧪 JUnit 5 + Mockito + AssertJ - Testing framework
  - 🥒 Cucumber 7 + Gherkin - BDD testing
  - ☁️ AWS CDK + TypeScript - Infrastructure as Code
  - 🐳 EKS + MSK + Route 53 - AWS cloud services
  - 🔧 Build and Deployment - Gradle, CI/CD, Quality Assurance

## 🔧 Build and Deployment

### Complete Build and Deployment Guide
- 🔧 Build and Deployment - Complete build and deployment guide
  - 🐘 Gradle Configuration - Basic configuration, build tasks, Wrapper setup
  - 📦 Multi-Module Setup - Project structure, sub-module configuration
  - 📚 Dependency Management - Version catalogs, dependency strategies
  - 🚀 CI/CD Integration - GitHub Actions, Docker, deployment automation
  - Deployment Strategies - Environment configuration, deployment scripts, health checks
  - Performance Optimization - Build performance, application performance
  - Monitoring and Logging - Application monitoring, log configuration

### Quality Assurance
- 🔍 Quality Assurance - Complete quality assurance guide
  - 👀 Code Review - Review process, checklists, feedback guidelines
  - 🔍 Static Analysis - SonarQube, Checkstyle, SpotBugs
  - 🔒 Security Scanning - OWASP, dependency checks, secure coding
  - 📊 Performance Monitoring - Micrometer, business metrics, performance testing
  - Quality Gates and Automation - Quality standards, automated checks

## 🔄 Workflow and Collaboration

### Complete Workflow and Collaboration Guide
- 🔄 Workflow and Collaboration - Complete workflow guide
  - 🔄 Development Workflow - Requirements analysis, design, BDD, TDD, review
  - 🚀 Release Process - Version control, release branches, deployment pipeline
  - 🔥 Hotfix Process - Emergency fixes, decision matrix
  - ♻️ Refactoring Strategy - Safe refactoring, refactoring checklist
  - 🤝 Team Collaboration - Communication principles, meeting management, knowledge sharing
  - Collaboration Tools - Project management, communication tools
  - 📊 Metrics and Improvement - Development metrics, continuous improvement

## 📊 Related Diagrams

### Architecture Diagrams
- [🔵 Hexagonal Architecture Diagram](../../diagrams/viewpoints/development/architecture/hexagonal-architecture.mmd)
- [🏛️ DDD Layered Architecture](../../diagrams/viewpoints/development/architecture/ddd-layered-architecture.mmd)
- [🌐 Microservices Architecture](../../diagrams/viewpoints/development/architecture/microservices-overview.mmd)
![Microservices Overview](../../diagrams/viewpoints/development/architecture/microservices-overview.mmd)
![Microservices Overview](../../diagrams/viewpoints/development/microservices-overview.puml)
- [🎭 Saga Orchestration Pattern](../../diagrams/viewpoints/development/architecture/saga-orchestration.mmd)

### Process Flow Diagrams
- [🔄 Development Workflow](../../diagrams/viewpoints/development/workflows/development-workflow.mmd)
- [🔴🟢🔵 TDD Cycle](../../diagrams/viewpoints/development/workflows/tdd-cycle.mmd)
- [📝 BDD Process](../../diagrams/viewpoints/development/workflows/bdd-process.mmd)
- [👀 Code Review Process](../../diagrams/viewpoints/development/workflows/code-review-process.mmd)

## 🎯 SOLID Principles and Design Patterns

- [🎯 SOLID Principles and Design Patterns](solid-principles-and-design-patterns.md) - Complete SOLID principles and design patterns guide

### SOLID Principles
- 📏 Single Responsibility Principle (SRP) - A class should have only one reason to change
- 🔓 Open-Closed Principle (OCP) - Open for extension, closed for modification
- 🔄 Liskov Substitution Principle (LSP) - Subtypes must be substitutable for their base types
- 🔌 Interface Segregation Principle (ISP) - Clients should not be forced to depend on interfaces they don't use
- 🔄 Dependency Inversion Principle (DIP) - Depend on abstractions, not concrete implementations

### Design Patterns
- 🏭 Factory Pattern - Create objects without specifying their concrete classes
- 🔨 Builder Pattern - Construct complex objects step by step
- 📋 Strategy Pattern - Define a family of algorithms and make them interchangeable
- 👁️ Observer Pattern - Define one-to-many dependency between objects
- 🙈 Tell, Don't Ask - Tell objects what to do, don't ask for their state

## 📚 Learning Paths

### Beginner Path
1. [📚 Quick Start](getting-started.md)
2. [☕ Java Coding Standards](coding-standards.md#java-coding-standards)
3. 🧪 Unit Testing Basics
4. 🏗️ Basic Architecture Concepts

### Intermediate Developer Path
1. 🎯 DDD Tactical Patterns
2. 🔵 Hexagonal Architecture Implementation
3. 🔴🟢🔵 TDD Practice
4. 📝 BDD Scenario Design

### Senior Architect Path
1. 🌐 Microservices Design
2. 🎭 Saga Pattern Implementation
3. 🔧 Distributed System Patterns
4. 📊 System Monitoring and Observability

## 🔗 Related Resources

### Internal Links
- [📋 Functional Viewpoint](../functional/README.md) - Functional requirements and business logic
- [📊 Information Viewpoint](../information/README.md) - Data models and information flow
- [⚡ Concurrency Viewpoint](../concurrency/README.md) - Concurrent processing and event-driven architecture
- [🌐 Context Viewpoint](../context/README.md) - System boundaries and external integration
- [🚀 Deployment Viewpoint](../deployment/README.md) - Deployment and infrastructure

### External Resources
- [Rozanski & Woods Architecture Viewpoints](https://www.viewpoints-and-perspectives.info/)
- [Domain-Driven Design Reference](https://domainlanguage.com/ddd/reference/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [AWS CDK Documentation](https://docs.aws.amazon.com/cdk/)

---

**Last Updated**: January 21, 2025  
**Maintainer**: Development Team  
**Version**: 1.0  
**Status**: Active

> 💡 **Tip**: This is an actively maintained document. If you find any issues or have suggestions for improvement, please contact us through GitHub Issues or reach out to the development team directly.

---
*此文件由自動翻譯系統生成，可能需要人工校對。*
