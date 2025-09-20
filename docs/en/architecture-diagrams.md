# GenAI Demo - Architecture Diagrams Documentation

## 📋 Overview

This document contains the complete architecture diagrams for the GenAI Demo project. These diagrams are automatically generated based on CDK code and showcase the system's AWS infrastructure and Domain-Driven Design (DDD) architecture.

## 🎨 Architecture Diagram List

### 📁 Complete Architecture Diagram Mapping Table

| Diagram Name | File Name | Description |
|-------------|-----------|-------------|
| System Architecture Diagram | `storage/1758271388722-qh0vw5v.json` | Basic system architecture diagram |
| User Registration Flow Diagram | `storage/1758271452950-pqpa620.json` | User registration business process |
| GenAI Demo - AWS CDK Architecture Diagram | `storage/1758272821927-c24lg7z.json` | Original CDK architecture diagram |
| GenAI Demo - Domain Event Architecture Flow Diagram | `storage/1758272891082-z23qvhs.json` | Domain Event processing flow |
| **AWS CDK Unified Architecture Diagram** | `storage/aws-cdk-unified-architecture-diagram.json` | **Unified Complete Architecture Diagram** ⭐ |
| Architecture Compliance Check - ArchUnit Rules | `storage/architecture-compliance-check-archunit-rules.json` | ArchUnit rules validation |
| Observability Requirements - Monitoring & Tracing | `storage/observability-requirements-monitoring-tracing.json` | Monitoring and tracing architecture |
| Four Architecture Perspectives Checklist | `storage/four-architecture-perspectives-checklist.json` | Four perspectives validation |
| Concurrency Strategy & Resilience Patterns | `storage/concurrency-strategy-resilience-patterns.json` | Concurrency and resilience patterns |
| Technology Evolution Standards & Version Management | `storage/technology-evolution-standards-version-management.json` | Technology evolution management |
| Rozanski & Woods Seven Viewpoints & Stakeholder Mapping | `storage/rozanski-woods-seven-viewpoints-stakeholder-mapping.json` | Seven viewpoints stakeholder mapping |
| Seven Architecture Viewpoints Detailed Focus Areas | `storage/seven-viewpoints-detailed-focus-areas.json` | Seven viewpoints focus areas |
| GenAI Demo Project Maturity Assessment & Recommendations | `storage/genai-demo-maturity-assessment-recommendations.json` | Project maturity assessment |
| GenAI Demo Architecture Improvement Action Plan | `storage/genai-demo-improvement-action-plan.json` | Architecture improvement action plan |
| GenAI Demo Seven Viewpoints Analysis & Roadmap | `storage/genai-demo-seven-viewpoints-analysis-roadmap.json` | In-depth analysis roadmap |
| GenAI Demo Technical Implementation Plan | `storage/genai-demo-technical-implementation-plan.json` | Technical implementation plan |

### 🎯 Recommended Usage

**Primary Architecture Diagram**: `AWS CDK Unified Architecture Diagram - GenAI Demo Complete System` (ID: `1758273710520-jghech8`)

- This is the most comprehensive architecture diagram, integrating all CDK stack components
- Includes complete architecture with network layer, application layer, database layer, security layer, etc.
- Shows the connection relationships and data flow between components

### 📋 Important Notes

⚠️ **File Naming Restrictions**: Due to Excalidraw MCP tool limitations, files must maintain their original ID format naming (e.g., `1758273710520-jghech8.json`) and cannot use custom file names. Renaming files will cause "Failed to load Document" errors.

## 🏗️ CDK Stack Architecture

### Core Infrastructure Stacks

1. **NetworkStack** - VPC, subnets, security group configuration
2. **SecurityStack** - KMS encryption, IAM roles and policies
3. **CoreInfrastructureStack** - Application Load Balancer, target groups

### Observability and Monitoring Stacks

4. **ObservabilityStack** - CloudWatch logs, dashboards
5. **AlertingStack** - SNS topics, alert configuration
6. **CostOptimizationStack** - AWS Budgets, cost alerts
7. **CrossRegionObservabilityStack** - Cross-region monitoring and log replication

### Data and Analytics Stacks

8. **AnalyticsStack** - S3 Data Lake, Kinesis Firehose, Glue, QuickSight
9. **MSKStack** - Apache Kafka cluster, configuration, monitoring

### High Availability Stacks

10. **Route53FailoverStack** - DNS failover, health checks

## 🌐 How to View Architecture Diagrams

### Method 1: View in Excalidraw

1. Open [Excalidraw.com](https://excalidraw.com)
2. Click "File" > "Open"
3. Copy and paste the JSON content below

### Method 2: Export Using MCP Tools

```bash
# Export as JSON format
mcp_excalidraw_export_to_json --id 1758272821927-c24lg7z

# Export as SVG format
mcp_excalidraw_export_to_svg --id 1758272821927-c24lg7z
```

## 📊 Architecture Features

### 🏛️ Infrastructure Features

- **Multi-tier Network Architecture**: Three-tier subnets (Public, Private, Database)
- **Comprehensive Security Protection**: KMS, IAM, Security Groups, WAF, CloudTrail, GuardDuty
- **Complete Observability**: CloudWatch, X-Ray, SNS alerts, cost monitoring
- **Data Analytics Pipeline**: S3 Data Lake, Kinesis Firehose, Glue, QuickSight

### 🔄 Domain-Driven Design Features

- **Aggregate Root Pattern**: Responsible for collecting and managing Domain Events
- **Event-Driven Architecture**: Uses MSK (Apache Kafka) to publish Domain Events
- **CQRS Pattern**: Command Query Responsibility Segregation
- **Event Sourcing**: Complete business history tracking
- **Cross-Aggregate Communication**: Loose coupling through Domain Events

### 🌍 Multi-Region Support

- **Disaster Recovery**: Cross-region data replication and failover
- **Route 53 Failover**: Automatic DNS switching
- **Cross-Region Observability**: Unified monitoring and log management

### 💰 Cost Optimization

- **AWS Budgets**: Automatic budget monitoring
- **Lifecycle Management**: S3 data automatic archiving
- **Resource Tagging**: Complete cost allocation tracking

## 🔧 Technology Stack

### Backend Technologies

- **Spring Boot 3.4.5** + **Java 21**
- **Spring Data JPA** + **Hibernate**
- **PostgreSQL** (production) / **H2** (development/testing)
- **Apache Kafka** (MSK) for Domain Events

### Frontend Technologies

- **Consumer App**: Angular 18 + TypeScript
- **CMC Management**: Next.js 14 + React 18 + TypeScript

### AWS Services

- **Compute**: ECS/EKS, Lambda
- **Network**: VPC, ALB, Route 53
- **Storage**: S3, RDS
- **Messaging**: MSK (Apache Kafka)
- **Monitoring**: CloudWatch, X-Ray
- **Security**: KMS, IAM, WAF, GuardDuty
- **Analytics**: Kinesis Firehose, Glue, Athena, QuickSight

## 📝 Update Notes

These architecture diagrams are automatically generated based on the following CDK code:

- Last updated: 2025-09-19
- CDK version: AWS CDK v2
- Based on actual TypeScript CDK code

To update architecture diagrams, please regenerate after CDK code changes.

## 🏗️ **Architecture Methodology Diagrams**

### Rozanski & Woods Architecture Methodology Visualization

Based on the `.kiro/steering/rozanski-woods-architecture-methodology.md` file, we have created a complete set of methodology diagrams:

#### 📊 **New Methodology Diagram Detailed Descriptions**

5. **Architecture Compliance Check Process** (`1758275485504-dbwdpv7`)
   - Shows the four major ArchUnit rule check categories
   - Domain layer dependency rules, Aggregate Root rules, event handler rules, Value Object rules
   - Compliance monitoring metrics: 100% coverage requirement

6. **Observability Requirements Architecture** (`1758275565208-3velqgl`)
   - Business metrics monitoring (required for each Aggregate Root)
   - Use case tracing (required for each application service)
   - Domain Event metrics (required for each event type)
   - Structured logging standards and alert configuration

7. **Four Architecture Perspectives Checklist** (`1758275636927-mu9pbco`)
   - Security perspective: Zero-trust architecture, principle of least privilege
   - Performance & scalability perspective: < 2s response time, ≥ 1000 req/s throughput
   - Availability & resilience perspective: ≥ 99.9% availability, ≤ 5 minutes RTO
   - Evolution perspective: Backward compatibility, version management strategy

8. **Concurrency Strategy & Resilience Patterns** (`1758275706782-36zkf1x`)
   - Concurrency strategy requirements: Event processing order, transaction boundaries, conflict handling
   - Circuit breaker pattern: CLOSED/OPEN/HALF_OPEN state management
   - Retry mechanism: Maximum 3 attempts, exponential backoff, jitter algorithm
   - Degradation strategy and dead letter queue handling

9. **Technology Evolution Standards & Version Management** (`1758275777304-9a6tabo`)
   - New technology introduction standards: Maturity assessment, team capability, risk control
   - Version upgrade requirements: Automated testing, test environment verification
   - Risk assessment matrix: Learning curve, performance impact, integration complexity
   - Migration strategy and rollback plan: ≤ 15 minutes rollback time

10. **Seven Viewpoints & Stakeholder Mapping** (`1758276726986-maiv8ad`)
    - Shows the main stakeholders corresponding to each architecture viewpoint
    - Annotates related roles from software development and business delivery perspectives
    - Includes complete seven viewpoints: Functional, Information, Concurrency, Development, Deployment, Operational, Context
    - Helps teams understand responsibility attribution for different viewpoints

11. **Seven Architecture Viewpoints Detailed Focus Areas** (`1758276802309-2o9w387`)
    - Detailed checklist based on current steering files
    - Specific focus areas and check items for each viewpoint
    - Includes corresponding tools and method recommendations
    - Provides complete architecture design guidelines

#### 🔄 **Methodology Application Flow**

```text
New Feature Development → Architecture Compliance Check → Observability Design → Four Perspectives Validation → Concurrency Resilience Design → Technology Evolution Assessment
     ↑                                                                                    ↓
     ←←←←←←←←←←←←←←←←←←← Continuous Improvement and Feedback ←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←
```

#### 🎯 **Methodology Diagram Purposes**

- **Architecture Compliance Check**: Ensure code complies with DDD and Hexagonal Architecture principles
- **Observability Design**: Establish complete monitoring, tracing, and alerting systems
- **Four Perspectives Validation**: Evaluate architecture from security, performance, availability, and evolution dimensions
- **Concurrency Resilience Design**: Implement circuit breaker, retry, degradation, and other resilience patterns
- **Technology Evolution Management**: Standardized technology selection and version upgrade processes

## 🔗 Related Documentation

- [CDK Deployment Guide](../infrastructure/README.md)
- [Multi-Region Architecture Documentation](../infrastructure/MULTI_REGION_ARCHITECTURE.md)
- [Security Implementation Guide](../infrastructure/SECURITY_IMPLEMENTATION.md)
- [Testing Guide](../infrastructure/TESTING_GUIDE.md)
- [Rozanski & Woods Architecture Methodology](../.kiro/steering/rozanski-woods-architecture-methodology.md)

## 🔍 **Latest Architecture Analysis Diagrams**

### 📈 **Project In-Depth Assessment Series**

14. **GenAI Demo Project Seven Viewpoints In-Depth Analysis & Improvement Roadmap** (`1758278799092-ft2juf7`)
    - In-depth analysis based on actual project code (13 bounded contexts, 143 Java test files, 103 infrastructure tests)
    - Detailed current state assessment and maturity scoring for seven viewpoints (overall 4.1/5.0)
    - 12-week detailed improvement roadmap, improving maturity from 4.1 to 4.7
    - Priority classification: 🚨 Context (2.0→4.0), 🔥 Concurrency (3.0→4.5), ⚡ Information & Operational (4.0→4.5)

15. **GenAI Demo Architecture Improvement Technical Implementation Detailed Plan** (`1758278894457-ehw2saj`)
    - Four-phase detailed technical implementation plan (2-4 weeks per phase)
    - Weekly specific tasks and responsible team assignments
    - Technical tools and implementation method guidance (EventStore, circuit breaker, monitoring, etc.)
    - Success metrics and milestone checkpoints

### 🎯 **Assessment Results Summary**

**Project Strengths** (⭐⭐⭐⭐⭐ Excellent Level):

- **Functional Viewpoint**: Complete DDD architecture, 13 bounded contexts
- **Development Viewpoint**: Hexagonal Architecture, 143 test files, ArchUnit compliance
- **Deployment Viewpoint**: AWS CDK v2, 6 coordinated stacks, 103 tests passed

**Areas for Improvement** (Urgent):

- **Context Viewpoint** (⭐⭐): System boundaries and external dependency mapping
- **Concurrency Viewpoint** (⭐⭐⭐): Concurrency strategy and resilience patterns

**Improvement Plan**: 12-week implementation plan, expected to improve overall maturity from 4.1 to 4.7 (excellent level)
