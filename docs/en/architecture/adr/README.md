# Architecture Decision Records (ADRs)

This directory contains Architecture Decision Records (ADRs) for the GenAI Demo project, documenting all major architectural decisions made during the development process.

## ADR Index

### Software Architecture Decisions

- [ADR-001: DDD + Hexagonal Architecture Foundation](./ADR-001-ddd-hexagonal-architecture.md)
- [ADR-002: Bounded Context Design Strategy](./ADR-002-bounded-context-design.md)
- [ADR-003: Domain Events and CQRS Implementation](./ADR-003-domain-events-cqrs.md)
- [ADR-004: Spring Boot Profile Configuration Strategy](./ADR-004-spring-boot-profiles.md)

### Infrastructure Decisions

- [ADR-005: AWS CDK vs Terraform](./ADR-005-aws-cdk-vs-terraform.md)
- [ADR-006: Multi-Region Architecture Strategy](./ADR-006-multi-region-architecture.md)
- [ADR-007: EKS vs ECS vs Lambda](./ADR-007-container-orchestration.md)
- [ADR-008: Aurora Global Database vs RDS](./ADR-008-database-strategy.md)
- [ADR-009: MSK vs EventBridge for Event Streaming](./ADR-009-event-streaming-platform.md)

### Observability Decisions

- [ADR-010: Three Pillars Observability Integration](./ADR-010-observability-integration.md)
- [ADR-011: Cost-Optimized Logging Strategy](./ADR-011-logging-cost-optimization.md)
- [ADR-012: Multi-Environment Observability Strategy](./ADR-012-multi-env-observability.md)

### Deployment and GitOps Decisions

- [ADR-013: Blue-Green vs Canary Deployment Strategies](./ADR-013-deployment-strategies.md)
- [ADR-014: ArgoCD vs Flux for GitOps](./ADR-014-gitops-platform.md)
- [ADR-015: Automated Rollback Strategy](./ADR-015-automated-rollback.md)

### Well-Architected Framework Assessment

- [ADR-016: Well-Architected Framework Compliance](./ADR-016-well-architected-compliance.md)

## ADR Template

We use the [MADR (Markdown Architectural Decision Records)](https://adr.github.io/madr/) template for consistency. Each ADR should include:

1. **Title**: Short noun phrase
2. **Status**: Proposed, Accepted, Deprecated, Superseded
3. **Context**: Business objectives and technical constraints
4. **Decision**: The architectural decision made
5. **Consequences**: Positive and negative outcomes
6. **Alternatives Considered**: Other options evaluated
7. **Well-Architected Assessment**: Evaluation against AWS five pillars

## Decision Process

1. **Identify Decision**: Recognize when an architectural decision needs to be made
2. **Research Options**: Investigate alternatives and gather evidence
3. **Evaluate Trade-offs**: Assess options against business and technical criteria
4. **Document Decision**: Create ADR using the standard template
5. **Review and Approve**: Team review and stakeholder approval
6. **Implement and Monitor**: Execute decision and track outcomes

## Maintenance

- ADRs are immutable once accepted
- Updates require new ADRs that supersede previous ones
- Regular reviews ensure decisions remain relevant
- Link related ADRs for traceability

## Tools and Integration

- **MCP Tools**: Used for real-time AWS best practices validation
- **Well-Architected Tool**: Automated compliance checking
- **Cost Calculator**: Impact assessment for infrastructure decisions
- **Performance Benchmarks**: Quantitative validation of decisions
