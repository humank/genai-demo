# Legacy Diagrams

## Overview

This directory preserves historical diagrams from before the project restructuring, serving as reference and
comparison material. These diagrams document the project's evolution and provide the foundation for the new
Rozanski & Woods architecture documentation.

## Migration Mapping

### Correspondence from Legacy Diagrams to New Architecture Viewpoints

| Legacy Diagram Location | New Location | Viewpoint/Perspective | Description |
|------------------------|--------------|----------------------|-------------|
| `legacy-uml/domain-model-diagram.puml` | `viewpoints/functional/domain-model.mmd` | Functional Viewpoint | Domain model redesign |
| `legacy-uml/hexagonal-architecture-diagram.puml` | `viewpoints/development/hexagonal-architecture.mmd` | Development Viewpoint | Hexagonal architecture modernization |
| `legacy-uml/event-storming-diagram.puml` | `viewpoints/information/event-storming-*.puml` | Information Viewpoint | Split into three levels |
| `legacy-uml/observability-diagram.puml` | `viewpoints/operational/monitoring-architecture.mmd` | Operational Viewpoint | Observability architecture |
| `legacy-uml/security-architecture-diagram.puml` | `perspectives/security/security-architecture.puml` | Security Perspective | Security architecture update |
| `legacy-uml/deployment-diagram.puml` | `viewpoints/deployment/deployment-diagram.puml` | Deployment Viewpoint | Deployment architecture modernization |

## Legacy Diagram Categories

### UML Diagrams

- **Class Diagrams**: Domain model and system design class diagrams
- **Sequence Diagrams**: Business process and system interaction sequence diagrams
- **Component Diagrams**: System component and dependency diagrams
- **Deployment Diagrams**: System deployment and infrastructure diagrams

### Architecture Diagrams

- **Layered Architecture Diagrams**: Traditional layered architecture design
- **Hexagonal Architecture Diagrams**: Early hexagonal architecture implementation
- **Event-Driven Architecture Diagrams**: Event-driven system design
- **Microservices Architecture Diagrams**: Microservices decomposition and design

### Business Process Diagrams

- **Event Storming**: Event storming analysis results
- **Use Case Diagrams**: System use cases and actors
- **Activity Diagrams**: Business processes and decision flows
- **State Diagrams**: Entity state transitions

## Reasons for Preservation

### Historical Reference

- **Evolution Trajectory**: Record the evolution process of system architecture
- **Design Decisions**: Preserve the background and rationale of historical design decisions
- **Learning Resources**: Serve as learning and reference materials for architecture design

### Comparative Analysis

- **Improvement Comparison**: Comparative analysis of old and new architectures
- **Best Practices**: Identify and summarize best practices in architecture design
- **Avoid Repetition**: Avoid repeating historical design mistakes

### Compliance Requirements

- **Document Retention**: Meet compliance requirements for project document retention
- **Audit Trail**: Provide complete audit trail for design changes
- **Knowledge Management**: Manage and transfer organizational knowledge assets

## Usage Guidelines

### Consulting Legacy Diagrams

1. **Define Purpose**: Clearly define the purpose of consulting legacy diagrams
2. **Find Correspondence**: Use the mapping table to find relevant legacy diagrams
3. **Comparative Analysis**: Compare with new architecture diagrams
4. **Extract Value**: Extract valuable design ideas and experiences

### Referencing Legacy Diagrams

- **Clear Annotation**: Clearly annotate as legacy version when referencing
- **Explain Context**: Explain the temporal and design context of legacy diagrams
- **Comparison Explanation**: Explain differences and improvements from current version

### Maintenance Principles

- **Read-Only Preservation**: Legacy diagrams are preserved as read-only data
- **No Updates**: No content updates to legacy diagrams
- **Regular Checks**: Regularly check integrity and accessibility of legacy diagrams

## Diagram Tool Evolution

### Tool Changes

- **Early Stage**: Mainly used PlantUML and manual drawing
- **Current**: Mermaid (GitHub native) + PlantUML (detailed design) + Excalidraw (conceptual design)
- **Future**: Consider introducing more automated diagram generation tools

### Format Evolution

- **Standardization**: From multiple formats to standardized formats
- **Automation**: From manual maintenance to automated generation
- **Integration**: Deep integration with development processes

## Access Instructions

### File Structure

```text
legacy/
├── uml/                    # UML diagrams
│   ├── class-diagrams/     # Class diagrams
│   ├── sequence-diagrams/  # Sequence diagrams
│   ├── component-diagrams/ # Component diagrams
│   └── deployment-diagrams/ # Deployment diagrams
├── architecture/           # Architecture diagrams
│   ├── layered/           # Layered architecture
│   ├── hexagonal/         # Hexagonal architecture
│   └── event-driven/      # Event-driven architecture
└── business/              # Business process diagrams
    ├── event-storming/    # Event storming
    ├── use-cases/         # Use case diagrams
    └── workflows/         # Workflows
```

### Access Permissions

- **Read Access**: All team members have read access
- **Modification Restrictions**: Modification of legacy diagram content is prohibited
- **Backup Protection**: Regular backups to prevent accidental loss

## Historical Context

### Architecture Evolution Phases

#### Phase 1: Initial Design (2024 Q1-Q2)

- **Monolithic Approach**: Single application architecture
- **Basic UML**: Simple class and sequence diagrams
- **Manual Documentation**: Hand-crafted diagrams and documentation
- **Limited Tooling**: Basic PlantUML usage

#### Phase 2: Service Decomposition (2024 Q2-Q3)

- **Microservices Transition**: Breaking down monolithic structure
- **Event-Driven Patterns**: Introduction of event-driven architecture
- **Advanced UML**: Complex interaction and component diagrams
- **Tool Diversification**: Multiple diagramming tools

#### Phase 3: DDD Implementation (2024 Q3-Q4)

- **Domain-Driven Design**: Strategic and tactical DDD patterns
- **Hexagonal Architecture**: Port-Adapter pattern implementation
- **Event Sourcing**: Event-based persistence patterns
- **Comprehensive Documentation**: Full architecture documentation

#### Phase 4: Rozanski & Woods Adoption (2025+)

- **Viewpoint-Based Architecture**: Structured architecture documentation
- **Quality Attribute Scenarios**: Systematic quality requirements
- **Automated Generation**: Tool-assisted diagram generation
- **Continuous Evolution**: Living architecture documentation

### Technology Stack Evolution

#### Diagramming Tools

- **Legacy**: PlantUML only, manual exports
- **Transition**: PlantUML + Mermaid combination
- **Current**: Mermaid primary, PlantUML for complex UML, Excalidraw for collaboration
- **Future**: AI-assisted diagram generation, live data integration

#### Documentation Formats

- **Legacy**: Standalone diagram files, limited integration
- **Transition**: Embedded diagrams in documentation
- **Current**: Integrated documentation system with cross-references
- **Future**: Interactive, searchable diagram ecosystem

## Migration Benefits

### Improved Organization

- **Structured Approach**: Rozanski & Woods methodology provides clear structure
- **Viewpoint Separation**: Clear separation of concerns across viewpoints
- **Quality Focus**: Explicit quality attribute scenarios
- **Stakeholder Alignment**: Better alignment with stakeholder needs

### Enhanced Maintainability

- **Living Documentation**: Documentation that evolves with the system
- **Automated Validation**: Consistency checks and validation
- **Version Control**: Proper versioning and change tracking
- **Tool Integration**: Better integration with development tools

### Better Collaboration

- **Shared Understanding**: Common vocabulary and structure
- **Role Clarity**: Clear responsibilities for different viewpoints
- **Review Process**: Structured review and approval processes
- **Knowledge Transfer**: Easier onboarding and knowledge sharing

## Related Resources

- [New Architecture Viewpoint Diagrams](../viewpoints/README.md) - New diagrams based on Rozanski & Woods
- [Architecture Perspective Diagrams](../perspectives/README.md) - Quality attribute related diagrams
- [Architecture Evolution Log](../../ARCHITECTURE_EVOLUTION_LOG.md) - Detailed record of architecture evolution

## Support and Maintenance

### Legacy Diagram Support

- **Read-Only Access**: Legacy diagrams are preserved for reference only
- **No Active Maintenance**: Content is not updated or maintained
- **Historical Context**: Provided for understanding evolution and decisions
- **Migration Assistance**: Help available for understanding legacy content

### Migration Support

- **Mapping Documentation**: Clear mapping from legacy to new structure
- **Transition Guides**: Step-by-step migration instructions
- **Tool Support**: Assistance with new tooling and processes
- **Training Resources**: Educational materials for new approaches

---

**Last Updated**: January 22, 2025  
**Maintainer**: Architecture Team  
**Note**: Diagrams in this directory are legacy versions for reference only. Please use the new architecture viewpoint diagrams for development work.

**Quick Navigation**:

- [Current Architecture](../../viewpoints/) - Active architecture documentation
- [Migration Guide](../../ARCHITECTURE_MIGRATION_GUIDE.md) - How to migrate from legacy
- [Tool Documentation](../README.md) - Current diagramming tools and standards
