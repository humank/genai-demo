# Viewpoint Diagrams

## Overview

This directory contains diagram resources based on Rozanski & Woods' seven architectural viewpoints, using different diagram tools to express different levels of architectural information.

## Diagram Tool Strategy

### Tool Selection Principles

| Tool | Best Use Cases | GitHub Display | Advantages | When to Use |
|------|---------------|----------------|------------|-------------|
| **Mermaid** | System architecture, flowcharts, sequence diagrams | ✅ Direct display | Concise syntax, version control friendly | Overview diagrams, flowcharts |
| **PlantUML** | Detailed UML diagrams, complex class diagrams | ❌ Requires PNG conversion | Powerful features, UML standard | Detailed design diagrams |
| **Excalidraw** | Conceptual design, brainstorming | ❌ Requires PNG conversion | Intuitive and easy to use, hand-drawn style | Concept diagrams, sketches |

### Diagram Hierarchy

1. **Overview Level (Mermaid)**: Overall system architecture and main component relationships
2. **Detailed Level (PlantUML)**: Specific class diagrams, sequence diagrams, component diagrams
3. **Conceptual Level (Excalidraw)**: Conceptual design and stakeholder communication

## Viewpoint Diagram Catalog

### [Functional Viewpoint Diagrams](functional/README.md)
- **domain-model.mmd**: Domain model overview diagram (Mermaid)
- **bounded-contexts.puml**: Bounded context detailed diagram (PlantUML)
- **use-cases.puml**: Use case diagram (PlantUML)
- **aggregates.puml**: Aggregate root design diagram (PlantUML)

### [Information Viewpoint Diagrams](README.md)
- **data-model.puml**: Data model diagram (PlantUML)
- **event-storming-big-picture.puml**: Event Storming Big Picture (PlantUML)
- **event-storming-process-level.puml**: Event Storming Process Level (PlantUML)
- **event-storming-design-level.puml**: Event Storming Design Level (PlantUML)
- **information-flow.mmd**: Information flow diagram (Mermaid)

### [Concurrency Viewpoint Diagrams](README.md)
- **event-driven-architecture.mmd**: Event-driven architecture diagram (Mermaid)
- **async-processing.puml**: Asynchronous processing diagram (PlantUML)
- **concurrency-patterns.puml**: Concurrency patterns diagram (PlantUML)

### [Development Viewpoint Diagrams](README.md)
- **hexagonal-architecture.mmd**: Hexagonal architecture diagram (Mermaid)
- **module-dependencies.puml**: Module dependency diagram (PlantUML)
- **build-pipeline.mmd**: Build pipeline diagram (Mermaid)

### [Deployment Viewpoint Diagrams](README.md)
- **infrastructure.mmd**: Infrastructure overview diagram (Mermaid)
- **deployment-diagram.puml**: Deployment diagram (PlantUML)
- **network-topology.puml**: Network topology diagram (PlantUML)

### [Operational Viewpoint Diagrams](README.md)
- **monitoring-architecture.mmd**: Monitoring architecture diagram (Mermaid)
- **observability.puml**: Observability architecture diagram (PlantUML)
- **incident-response.mmd**: Incident response flow diagram (Mermaid)

## Diagram Naming Conventions

### File Naming Format
```
{viewpoint-name}/{diagram-type}-{specific-content}.{extension}
```

### Examples
- `functional/domain-model.mmd` - Functional viewpoint domain model Mermaid diagram
- `information/event-storming-big-picture.puml` - Information viewpoint Event Storming PlantUML diagram
- `deployment/infrastructure.mmd` - Deployment viewpoint infrastructure Mermaid diagram

## Diagram Maintenance Guidelines

### Automated Generation
- **PlantUML**: Use GitHub Actions to automatically generate PNG
- **Excalidraw**: Use MCP service for generation and conversion
- **Mermaid**: GitHub native support, no additional processing needed

### Version Control
- Diagram source files (`.mmd`, `.puml`, `.excalidraw`) included in version control
- Generated PNG files also included in version control for offline viewing
- Use meaningful commit messages to describe diagram changes

### Quality Standards
- Diagrams must have clear titles and descriptions
- Use consistent colors and styles
- Ensure diagram readability at different sizes
- Regularly check diagram consistency with actual system

## Related Resources

- [Architectural Perspective Diagrams](../perspectives/README.md) - Cross-viewpoint quality attribute diagrams
- [Legacy Diagrams](../legacy/README.md) - Preserved historical diagrams
- [Diagram Tools Usage Guide](../diagram-tools-guide.md) - Diagram tool usage guidelines

---

**Last Updated**: January 21, 2025  
**Maintainer**: Architecture Team