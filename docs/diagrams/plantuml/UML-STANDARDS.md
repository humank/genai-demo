# UML 2.5 Standardization Guide

This document defines the standardization specifications for all UML diagrams in the project, based on UML 2.5 standards and the latest PlantUML syntax.

## Standardization Principles

### 1. Color Standards

All UML diagrams use a unified color scheme, defined in `uml-2.5-colors.puml`:

| Element Type | Color | Purpose |
|-------------|-------|---------|
| Aggregate Root | #LightSalmon | DDD aggregate root entities |
| Entity | #LightBlue | Objects with unique identity |
| Value Object | #LightGreen | Immutable value objects |
| Domain Event | #LightYellow | Business events |
| Service | #LightGray | Application and domain services |
| Repository | #LightCyan | Data access interfaces |
| Factory | #LightPink | Object creation factories |
| Strategy | #LightGoldenRodYellow | Business strategy patterns |
| Specification | #LightSkyBlue | Business specification patterns |

### 2. Syntax Standards

#### Class Diagram Standard Syntax

```plantuml
@startuml Class Diagram Title - UML 2.5 Standardized Version
!theme plain
!include uml-2.5-colors.puml

package "package.name" <<Rectangle>> {
  class ClassName <<Stereotype>> {
    -privateField: Type
    #protectedField: Type
    +publicField: Type
    --
    +publicMethod(param: Type): ReturnType
    -privateMethod(): void
    --
    +{static} staticMethod(): Type
  }
}
@enduml
```

#### Sequence Diagram Standard Syntax

```plantuml
@startuml Sequence Diagram Title - UML 2.5 Standardized Version
!theme plain
!include uml-2.5-colors.puml

actor "Actor" as Actor
participant "Controller" as Controller
participant "Service" as Service
participant "Repository" as Repository

Actor -> Controller: request(param: Type)
activate Controller

Controller -> Service: process(command: Command)
activate Service

Service -> Repository: save(entity: Entity)
activate Repository
Repository --> Service: result: Result
deactivate Repository

Service --> Controller: response: Response
deactivate Service

Controller --> Actor: result: Result
deactivate Controller
@enduml
```

### 3. Relationship Standards

| Relationship Type | UML Symbol | PlantUML Syntax | Description |
|------------------|------------|-----------------|-------------|
| Composition | ♦—— | `*--` | Strong ownership, lifecycle dependency |
| Aggregation | ◊—— | `o--` | Weak ownership, can exist independently |
| Association | ——> | `-->` | Reference relationship, knows about existence |
| Dependency | ····> | `..>` | Usage relationship, temporary dependency |
| Generalization | ——▷ | `--|>` | Inheritance relationship |
| Realization | ····▷ | `..|>` | Interface implementation relationship |

### 4. Multiplicity Notation

```plantuml
' Standard multiplicity notation
ClassA "1" *-- "*" ClassB : contains
ClassC "0..1" --> "1..*" ClassD : references
ClassE "1" o-- "0..5" ClassF : aggregates
```

## Diagram Type Standards

### 1. Class Diagram

**Purpose**: Show the static structure of the system, including classes, interfaces, and relationships

**Standard Elements**:
- Use `<<AggregateRoot>>` to mark aggregate roots
- Use `<<ValueObject>>` to mark value objects
- Use `<<DomainEvent>>` to mark domain events
- Use `<<Service>>` to mark service classes
- Use `<<Repository>>` to mark repository interfaces

**Example**: `class-diagram.puml`

### 2. Sequence Diagram

**Purpose**: Show the interaction sequence between objects

**Standard Elements**:
- Use `actor` for external participants
- Use `participant` for system components
- Use `activate`/`deactivate` for lifelines
- Use `note` to add important explanations

**Example**: `sequence-diagram.puml`

### 3. Domain Model Diagram

**Purpose**: Show the complete domain model structure

**Standard Elements**:
- Group by bounded contexts
- Clearly mark DDD tactical patterns
- Include complete relationships and multiplicities
- Add business rule explanations

**Example**: `domain-model-diagram.puml`

## Naming Conventions

### 1. File Naming

```
{diagram-type}-{context}-{version}.puml
```

Examples:
- `class-diagram-order-context.puml`
- `sequence-diagram-payment-flow.puml`
- `domain-model-complete.puml`

### 2. Class Naming

```plantuml
' Aggregate Root
class OrderAggregate <<AggregateRoot>>

' Value Object
class OrderId <<ValueObject>>

' Domain Event
class OrderCreatedEvent <<DomainEvent>>

' Service
class OrderApplicationService <<Service>>

' Repository
interface OrderRepository <<Repository>>
```

### 3. Method Naming

```plantuml
class Order {
  ' Query methods
  +getId(): OrderId
  +getStatus(): OrderStatus
  
  ' Command methods
  +addItem(item: OrderItem): void
  +submit(): void
  +cancel(): void
  
  ' Factory methods
  +{static} create(customerId: String): Order
}
```

## Best Practices

### 1. Diagram Organization

- **Single Responsibility**: Each diagram focuses on a specific perspective or use case
- **Appropriate Size**: Avoid overly complex diagrams, split into multiple diagrams when necessary
- **Consistency**: Use the same naming and styling across all diagrams

### 2. Annotations and Documentation

```plantuml
note right of Order
  <b>Order Aggregate Root</b>
  - Maintains order consistency
  - Publishes domain events
  - Implements business rules
end note
```

### 3. Legend Explanation

Each diagram should include a legend explaining the symbols and colors used:

```plantuml
legend right
  |= Element Type |= Color |= Description |
  | <back:#LightSalmon>   </back> | Aggregate Root | DDD aggregate root entities |
  | <back:#LightBlue>   </back> | Entity | Objects with unique identity |
  | <back:#LightGreen>   </back> | Value Object | Immutable value objects |
endlegend
```

## Tools and Automation

### 1. PlantUML Configuration

```bash
# Generate PNG images
java -jar tools-and-environment/plantuml.jar -tpng docs/diagrams/plantuml/*.puml

# Generate SVG images
java -jar tools/plantuml.jar -tsvg docs/diagrams/plantuml/*.puml

# Check syntax
java -jar tools/plantuml.jar -checkonly docs/diagrams/plantuml/*.puml
```

### 2. Automation Scripts

```bash
#!/bin/bash
# scripts/generate-diagrams.sh

echo "Generating standardized UML diagrams..."

# Generate all PlantUML diagrams
find docs/diagrams/plantuml -name "*.puml" -exec java -jar tools/plantuml.jar -tpng {} \;

echo "Diagram generation complete!"
```

### 3. Git Hook Integration

```bash
#!/bin/bash
# .git/hooks/pre-commit

# Check PlantUML syntax
java -jar tools/plantuml.jar -checkonly docs/diagrams/plantuml/*.puml

if [ $? -ne 0 ]; then
    echo "PlantUML syntax error, please fix before committing"
    exit 1
fi
```

## Quality Checklist

### Diagram Quality Check

- [ ] Uses standard color scheme
- [ ] Includes appropriate legend explanation
- [ ] Relationship markings are correct and complete
- [ ] Naming conventions are consistent
- [ ] Includes necessary annotations
- [ ] Diagram size is appropriate, not overly complex
- [ ] Syntax complies with UML 2.5 standards

### Technical Quality Check

- [ ] PlantUML syntax is correct
- [ ] Can successfully generate PNG/SVG
- [ ] File naming follows conventions
- [ ] Includes version and update date
- [ ] Related documentation is updated

## Related Resources

- PlantUML Official Documentation
- UML 2.5 Specification
- DDD Tactical Patterns
- [Project DDD Guide](../../design/ddd-guide.md)
- [Architecture Decision Records](../../architecture/adr/)