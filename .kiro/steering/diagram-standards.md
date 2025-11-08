---
title: Diagram Standards and Best Practices
category: documentation
last_updated: 2025-11-08
---

# Diagram Standards and Best Practices

## Overview

This document defines when and how to use different diagram formats in our documentation. Choosing the right format improves readability, maintainability, and GitHub rendering.

---

## Diagram Format Decision Tree

```mermaid
graph TD
    A[Need a Diagram?] --> B{What type?}
    B -->|Directory Structure| C[ASCII Tree]
    B -->|Git Branch Flow| D[ASCII Flow]
    B -->|Simple Flow/Architecture| E[Mermaid]
    B -->|Complex UML| F[PlantUML]
    B -->|Whiteboard/Sketch| G[Excalidraw]
    
    C --> H[Use ├── └── │]
    D --> I[Use │ ◄── branches]
    E --> J[Use ```mermaid]
    F --> K[Generate PNG/SVG]
    G --> L[Export to docs/diagrams/]
```

---

## When to Use Each Format

### ASCII Art

**Use For:**
- ✅ Directory/file structures
- ✅ Git branch workflows
- ✅ Simple hierarchical trees
- ✅ Quick inline examples

**Advantages:**
- Fast to create and edit
- No external tools needed
- Clear in plain text
- Version control friendly
- Works in all environments

**Disadvantages:**
- Limited visual complexity
- Hard to maintain for large diagrams
- No automatic rendering

**Example:**

```text
src/
├── domain/
│   ├── customer/
│   ├── order/
│   └── shared/
├── application/
└── infrastructure/
```

**When NOT to Use:**
- Complex flowcharts with multiple paths
- Architecture diagrams with many components
- Diagrams that need frequent updates

---

### Mermaid

**Use For:**
- ✅ Flowcharts and process flows
- ✅ Architecture diagrams
- ✅ Sequence diagrams
- ✅ State diagrams
- ✅ Entity relationship diagrams
- ✅ Gantt charts

**Advantages:**
- Native GitHub rendering
- Easy to update (text-based)
- Version control friendly
- Multiple diagram types
- Good for documentation

**Disadvantages:**
- Limited layout control
- Cannot handle very complex diagrams
- Styling options are limited

**Example:**

```mermaid
graph TD
    A[Start] --> B{Decision}
    B -->|Yes| C[Action 1]
    B -->|No| D[Action 2]
    C --> E[End]
    D --> E
```

**Best Practices:**
- Keep diagrams simple (< 20 nodes)
- Use descriptive node labels
- Group related nodes with subgraphs
- Use appropriate diagram type (graph, sequenceDiagram, etc.)

**File Location:**
- Inline in markdown: `docs/**/*.md`
- Standalone files: `docs/diagrams/mermaid/*.mmd`

---

### PlantUML

**Use For:**
- ✅ Detailed UML class diagrams
- ✅ Complex component diagrams
- ✅ Deployment diagrams
- ✅ Professional documentation
- ✅ Precise layout requirements

**Advantages:**
- Comprehensive UML support
- Fine-grained layout control
- Professional output quality
- Supports complex diagrams
- Good for architecture documentation

**Disadvantages:**
- Requires generation step
- Not rendered natively on GitHub
- Needs Java runtime
- Steeper learning curve

**Example:**

```plantuml
@startuml
class Order {
  - id: OrderId
  - items: List<OrderItem>
  + submit(): void
  + cancel(): void
}

class OrderItem {
  - productId: ProductId
  - quantity: int
}

Order "1" *-- "many" OrderItem
@enduml
```

**Best Practices:**
- Use for detailed technical diagrams
- Generate both PNG and SVG
- Store source in `docs/diagrams/viewpoints/`
- Store generated in `docs/diagrams/generated/`
- Reference PNG in markdown (better GitHub display)

**Generation:**
```bash
./scripts/generate-diagrams.sh --format=png
```

**File Organization:**
```text
docs/diagrams/
├── viewpoints/
│   ├── functional/*.puml
│   ├── information/*.puml
│   └── deployment/*.puml
└── generated/
    ├── functional/*.png
    ├── information/*.png
    └── deployment/*.png
```

---

### Excalidraw

**Use For:**
- ✅ Whiteboard-style sketches
- ✅ Brainstorming diagrams
- ✅ Hand-drawn look diagrams
- ✅ Collaborative design sessions
- ✅ Conceptual illustrations

**Advantages:**
- Intuitive drawing interface
- Hand-drawn aesthetic
- Great for collaboration
- Easy to create quickly
- Good for presentations

**Disadvantages:**
- Not text-based (harder to version)
- Requires export step
- Not suitable for precise diagrams
- Larger file sizes

**Best Practices:**
- Use for early-stage design
- Export to PNG/SVG for documentation
- Store source `.excalidraw` files
- Use for high-level concepts only

**File Organization:**
```text
docs/diagrams/excalidraw/
├── sources/
│   └── *.excalidraw
└── exports/
    └── *.png
```

**Workflow:**
1. Create diagram in Excalidraw
2. Save source to `docs/diagrams/excalidraw/sources/`
3. Export PNG to `docs/diagrams/excalidraw/exports/`
4. Reference PNG in markdown

---

## Conversion Guidelines

### ASCII to Mermaid

**Convert When:**
- Diagram has arrows (→, ↓) indicating flow
- Multiple paths or decision points
- Need better GitHub rendering
- Diagram is frequently updated

**Keep ASCII When:**
- Simple directory structures
- Git branch flows
- Hierarchical trees
- Quick inline examples

**Conversion Tool:**
```bash
python3 scripts/convert-ascii-to-mermaid.py docs/ .kiro/
```

### Mermaid to PlantUML

**Convert When:**
- Need precise layout control
- Diagram becomes too complex (> 20 nodes)
- Professional documentation required
- UML compliance needed

### PlantUML to Excalidraw

**Convert When:**
- Need hand-drawn aesthetic
- Presenting to non-technical audience
- Want more visual flexibility
- Creating marketing materials

---

## Diagram Maintenance

### Update Frequency

| Format | Update Effort | Best For |
|--------|--------------|----------|
| ASCII | Low | Frequently changing structures |
| Mermaid | Low | Evolving processes |
| PlantUML | Medium | Stable architecture |
| Excalidraw | High | One-time illustrations |

### Version Control

**Text-Based (ASCII, Mermaid, PlantUML):**
- ✅ Commit source files
- ✅ Easy to review changes
- ✅ Merge conflicts manageable
- ✅ History tracking clear

**Binary (Excalidraw exports, PNG):**
- ⚠️ Commit both source and exports
- ⚠️ Use Git LFS for large files
- ⚠️ Difficult to review changes
- ⚠️ Merge conflicts problematic

### Documentation Standards

**Every diagram should have:**
1. **Title**: Clear, descriptive name
2. **Context**: What does it show?
3. **Date**: When was it created/updated?
4. **Owner**: Who maintains it?
5. **Related Docs**: Links to related documentation

**Example:**

```markdown
## System Architecture Overview

**Purpose**: Shows the high-level architecture of the e-commerce platform

**Last Updated**: 2025-11-08

**Owner**: Architecture Team

```mermaid
graph TB
    Client --> API
    API --> Services
    Services --> Database
```

**Related Documentation:**
- [Deployment Viewpoint](../viewpoints/deployment/overview.md)
- [ADR-002: Hexagonal Architecture](../architecture/adrs/002-adopt-hexagonal-architecture.md)
```

---

## Quality Checklist

### Before Creating a Diagram

- [ ] Is a diagram really needed? (Can text explain it better?)
- [ ] What is the target audience? (Technical vs. non-technical)
- [ ] How often will it change? (Choose format accordingly)
- [ ] Where will it be used? (GitHub, presentations, print)

### After Creating a Diagram

- [ ] Is it clear and easy to understand?
- [ ] Are labels descriptive and consistent?
- [ ] Is the complexity appropriate? (Not too simple or complex)
- [ ] Is it properly documented? (Title, context, date)
- [ ] Is it in the right location?
- [ ] Does it render correctly on GitHub?

---

## Common Patterns

### Architecture Diagrams

**Use:** Mermaid for simple, PlantUML for complex

```mermaid
graph TB
    subgraph "Presentation Layer"
        UI[Web UI]
    end
    
    subgraph "Application Layer"
        API[REST API]
        Service[Business Services]
    end
    
    subgraph "Data Layer"
        DB[(Database)]
        Cache[(Cache)]
    end
    
    UI --> API
    API --> Service
    Service --> DB
    Service --> Cache
```

### Process Flows

**Use:** Mermaid

```mermaid
graph LR
    A[Receive Order] --> B{Validate}
    B -->|Valid| C[Process Payment]
    B -->|Invalid| D[Reject Order]
    C --> E{Payment OK?}
    E -->|Yes| F[Confirm Order]
    E -->|No| G[Cancel Order]
```

### Sequence Diagrams

**Use:** Mermaid

```mermaid
sequenceDiagram
    participant C as Client
    participant A as API
    participant S as Service
    participant D as Database
    
    C->>A: POST /orders
    A->>S: createOrder()
    S->>D: save()
    D-->>S: order
    S-->>A: order
    A-->>C: 201 Created
```

### Directory Structures

**Use:** ASCII

```text
project/
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   └── test/
│       ├── java/
│       └── resources/
├── docs/
└── scripts/
```

---

## Tools and Resources

### Mermaid
- **Live Editor**: https://mermaid.live/
- **Documentation**: https://mermaid.js.org/
- **VS Code Extension**: Mermaid Preview

### PlantUML
- **Live Editor**: http://www.plantuml.com/plantuml/
- **Documentation**: https://plantuml.com/
- **VS Code Extension**: PlantUML

### Excalidraw
- **Web App**: https://excalidraw.com/
- **VS Code Extension**: Excalidraw

### ASCII Art
- **ASCII Flow**: https://asciiflow.com/
- **Tree Generator**: `tree` command (macOS/Linux)

---

## Migration Strategy

### Existing Diagrams

1. **Audit**: Identify all diagrams in documentation
2. **Categorize**: Classify by type and complexity
3. **Prioritize**: Focus on frequently accessed docs
4. **Convert**: Use appropriate format
5. **Validate**: Ensure rendering and clarity
6. **Document**: Update references and metadata

### New Diagrams

1. **Plan**: Choose format based on guidelines
2. **Create**: Use recommended tools
3. **Review**: Check quality checklist
4. **Document**: Add context and metadata
5. **Commit**: Include source and generated files

---

## Related Documentation

- [Diagram Generation Standards](diagram-generation-standards.md) - PlantUML generation details
- [Development Standards](development-standards.md) - General documentation standards
- [Code Review Standards](code-review-standards.md) - Review process for diagrams

---

**Document Version**: 1.0  
**Last Updated**: 2025-11-08  
**Owner**: Documentation Team
