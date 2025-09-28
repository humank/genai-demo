# Event Storming Standardized Diagrams

This directory contains three-phase diagrams based on official Event Storming standards, using standardized colors and symbol conventions.

## Diagram Overview

### 1. Big Picture Event Storming

**File**: `big-picture-standardized.puml`

**Purpose**: Identify core business events, discover problem hotspots, find improvement opportunities

**Standard Colors**:
- 🟠 **Events** (#FFA500): Important business events occurring in the system
- 🟡 **Actors** (#FFD700): People or roles triggering events
- 🩷 **External Systems** (#FF69B4): External services integrated with the system
- 🔴 **Hotspots** (#FF0000): Problems or risk points that need resolution
- 🟢 **Opportunities** (#32CD32): System improvement and optimization opportunities

**Key Content**:
- Complete business process event chains
- Cross-system integration points
- Business risks and improvement opportunities
- Stakeholder interactions

### 2. Process Level Event Storming

**File**: `process-level-standardized.puml`

**Purpose**: Detailed command and event flows, aggregate boundaries and responsibilities, business policies and rules

**Standard Colors**:
- 🟠 **Events** (#FFA500): Results of business state changes
- 🔵 **Commands** (#1E90FF): Intentions that trigger business behaviors
- 🟡 **Aggregates** (#FFFF00): Entities that maintain business rules
- 🟢 **Read Models** (#32CD32): Information views for user queries
- 🟣 **Policies** (#800080): Business rules triggered by events
- 🟡 **Actors** (#FFD700): People or roles executing commands
- 🩷 **External Systems** (#FF69B4): Integrated external services

**Key Content**:
- Command to aggregate execution flows
- Event-driven business rules
- Read model projection strategies
- External system integration points

### 3. Design Level Event Storming

**File**: `design-level-standardized.puml`

**Purpose**: Complete bounded context design, aggregate boundaries and responsibility division, cross-context event integration

**Standard Colors**:
- 🟠 **Events** (#FFA500): Results of business state changes
- 🔵 **Commands** (#1E90FF): Intentions that trigger business behaviors
- 🟡 **Aggregates** (#FFFF00): Entities that maintain business rules and consistency
- 🟢 **Read Models** (#32CD32): Information views for user queries
- 🟣 **Policies** (#800080): Business rules triggered by events
- 🔷 **Services** (#ADD8E6): Coordinate aggregates and handle complex business logic
- 🟡 **Actors** (#FFD700): People or roles executing commands
- 🩷 **External Systems** (#FF69B4): Integrated external services

**Bounded Contexts**:
- 📦 **Order Context**
- 📦 **Payment Context**
- 📦 **Inventory Context**
- 📦 **Delivery Context**
- 📦 **Notification Context**
- 📦 **Customer Service Context**
- 📦 **Integration View Context**

**Key Content**:
- Complete bounded context boundaries
- Cross-context event integration
- Service and external system integration
- Read model projection strategies

## Connection Type Descriptions

| Connection Type | Color | Description |
|----------------|-------|-------------|
| Solid Arrow | Black | Command execution or event publishing |
| Thick Solid Line | Red | Cross-bounded context event integration |
| Dashed Line | Green | Event to read model projection |
| Thick Dashed Line | Purple | Cross-bounded context read model projection |
| Solid Line | Pink | External system integration |
| Dashed Arrow | Red | Potential problems or risk points |

## Usage Guide

### 1. Reference Standard Colors

Other Event Storming diagrams can reference standard color definitions:

```plantuml
!include event-storming-colors.puml
```

### 2. Diagram Generation

Use PlantUML to generate PNG images:

```bash
java -jar tools-and-environment/plantuml.jar -tpng docs/diagrams/plantuml/event-storming/*.puml
```

### 3. Diagram Updates

When business processes change, update in the following order:

1. **Big Picture**: Update core business events and hotspots
2. **Process Level**: Update commands, aggregates, and policies
3. **Design Level**: Update bounded contexts and service design

## Event Storming Best Practices

### Big Picture Phase
- Focus on business events, don't consider technical implementation too early
- Identify all stakeholders and external systems
- Mark problem hotspots and improvement opportunities
- Maintain high-level perspective, avoid too much detail

### Process Level Phase
- Clarify cause-and-effect relationships between commands and events
- Define aggregate boundaries and responsibilities
- Identify business policies and rules
- Design read models to support query requirements

### Design Level Phase
- Divide clear bounded contexts
- Design cross-context event integration
- Define service responsibilities and external system integration
- Consider non-functional requirements (performance, security, etc.)

## Related Documentation

- [Domain Events Implementation Guide](../../../viewpoints/information/domain-events.md)
- [DDD Tactical Patterns Implementation](../../../design/ddd-guide.md)
- [Bounded Context Design](../../../viewpoints/functional/bounded-contexts.md)
- **Domain Events Implementation Guide** (refer to internal project documentation)

## Tools and Resources

- **PlantUML**: Diagram generation tool
- **Event Storming Official Website**: https://www.eventstorming.com/
- **DDD Community Resources**: https://github.com/ddd-crew
- **PlantUML Syntax Reference**: https://plantuml.com/