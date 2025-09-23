# Development Viewpoint Diagrams

This directory contains all diagrams related to the Development Viewpoint, organized by category to support the development process and architectural understanding.

## Directory Structure

```
docs/diagrams/viewpoints/development/
├── README.md                           # This file
├── architecture/                       # Architecture pattern diagrams
│   ├── microservices-overview.mmd     # Microservices architecture overview
│   ├── saga-orchestration.mmd         # Saga orchestration pattern
│   ├── distributed-system.mmd         # Distributed system architecture
│   └── circuit-breaker-pattern.mmd    # Circuit breaker pattern
├── workflows/                          # Development workflow diagrams
│   ├── development-workflow.mmd       # Complete development process
│   ├── tdd-cycle.mmd                  # TDD Red-Green-Refactor cycle
│   ├── bdd-process.mmd                # BDD process workflow
│   └── code-review-process.mmd        # Code review workflow
├── testing/                           # Testing-related diagrams
│   ├── test-pyramid.mmd               # Test pyramid strategy
│   └── performance-testing.mmd        # Performance testing architecture
├── infrastructure/                    # Infrastructure diagrams
│   ├── ci-cd-pipeline.mmd             # CI/CD pipeline workflow
│   └── monitoring-architecture.mmd    # Monitoring and observability
├── patterns/                          # Design pattern diagrams
├── hexagonal-architecture.mmd         # Hexagonal architecture (migrated)
└── ddd-layered-architecture.mmd       # DDD layered architecture (migrated)
```

## Core Architecture Diagrams

### Hexagonal Architecture
- **File**: `hexagonal-architecture.mmd`
- **Purpose**: Shows the ports and adapters pattern implementation
- **Key Elements**: Primary/Secondary adapters, Domain core, External systems
- **Usage**: Understanding system boundaries and dependency inversion

### DDD Layered Architecture
- **File**: `ddd-layered-architecture.mmd`
- **Purpose**: Illustrates Domain-Driven Design layered architecture
- **Key Elements**: UI, Application, Domain, Infrastructure layers
- **Usage**: Understanding DDD tactical patterns and layer dependencies

## Architecture Patterns

### Microservices Overview
- **File**: `architecture/microservices-overview.mmd`
- **Purpose**: Complete microservices architecture with all components
- **Key Elements**: Services, API Gateway, Data layer, Message layer
- **Usage**: Understanding distributed system design

### Saga Orchestration
- **File**: `architecture/saga-orchestration.mmd`
- **Purpose**: Shows saga pattern for distributed transaction management
- **Key Elements**: Orchestrator, Compensation flow, State management
- **Usage**: Understanding complex workflow coordination

### Distributed System
- **File**: `architecture/distributed-system.mmd`
- **Purpose**: Multi-region distributed system with resilience patterns
- **Key Elements**: Multi-region setup, Service mesh, Security, Monitoring
- **Usage**: Understanding enterprise-scale architecture

### Circuit Breaker Pattern
- **File**: `architecture/circuit-breaker-pattern.mmd`
- **Purpose**: Circuit breaker states and implementation
- **Key Elements**: Closed/Open/Half-Open states, Configuration, Fallback strategies
- **Usage**: Understanding resilience patterns

## Development Workflows

### Development Workflow
- **File**: `workflows/development-workflow.mmd`
- **Purpose**: Complete development lifecycle from feature to production
- **Key Elements**: BDD scenarios, TDD cycle, Quality gates, Deployment
- **Usage**: Understanding development process

### TDD Cycle
- **File**: `workflows/tdd-cycle.mmd`
- **Purpose**: Test-Driven Development Red-Green-Refactor cycle
- **Key Elements**: RED/GREEN/REFACTOR phases, Best practices, Tools
- **Usage**: Understanding TDD methodology

### BDD Process
- **File**: `workflows/bdd-process.mmd`
- **Purpose**: Behavior-Driven Development workflow
- **Key Elements**: Discovery, Gherkin scenarios, Step definitions
- **Usage**: Understanding BDD methodology

### Code Review Process
- **File**: `workflows/code-review-process.mmd`
- **Purpose**: Complete code review workflow
- **Key Elements**: Review types, Feedback categories, Quality gates
- **Usage**: Understanding code review standards

## Testing Strategy

### Test Pyramid
- **File**: `testing/test-pyramid.mmd`
- **Purpose**: Testing strategy with different test types
- **Key Elements**: Unit/Integration/E2E tests, Performance standards, Tools
- **Usage**: Understanding testing approach

### Performance Testing
- **File**: `testing/performance-testing.mmd`
- **Purpose**: Performance testing architecture and strategy
- **Key Elements**: Load/Stress/Spike testing, Monitoring, Thresholds
- **Usage**: Understanding performance validation

## Infrastructure

### CI/CD Pipeline
- **File**: `infrastructure/ci-cd-pipeline.mmd`
- **Purpose**: Complete continuous integration and deployment process
- **Key Elements**: Build stages, Quality gates, Deployment strategies
- **Usage**: Understanding delivery pipeline

### Monitoring Architecture
- **File**: `infrastructure/monitoring-architecture.mmd`
- **Purpose**: Comprehensive observability and monitoring strategy
- **Key Elements**: Metrics collection, Visualization, Alerting
- **Usage**: Understanding system observability

## Usage Guidelines

### Viewing Diagrams
- **GitHub**: Diagrams render automatically in GitHub's Mermaid support
- **Local**: Use Mermaid CLI or compatible viewers
- **Documentation**: Reference diagrams using relative paths

### Updating Diagrams
1. Edit the `.mmd` files directly
2. Follow Mermaid syntax guidelines
3. Test rendering before committing
4. Update documentation references if needed

### Adding New Diagrams
1. Choose appropriate subdirectory based on purpose
2. Use descriptive filenames with `.mmd` extension
3. Follow existing naming conventions
4. Update this README with new diagram information

## Related Documentation

- [Development Viewpoint Documentation](../../../viewpoints/development/README.md)
- [Architecture Documentation](../../../architecture/README.md)
- [Diagram Generation Standards](../../README.md)
- [Mermaid Syntax Guide](https://mermaid-js.github.io/mermaid/)

## Migration Notes

The following diagrams were migrated from `docs/diagrams/mermaid/` to this location:
- `hexagonal-architecture.md` → `hexagonal-architecture.mmd`
- `ddd-layered-architecture.md` → `ddd-layered-architecture.mmd`

All references have been updated to point to the new locations.