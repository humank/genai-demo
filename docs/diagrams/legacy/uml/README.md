# E-commerce System UML Documentation

This directory contains various UML diagrams for the e-commerce system, describing the system's architecture, design, and behavior.

## Diagram List

### Basic Diagrams

1. **Class Diagram (class-diagram.puml)**
   - Describes the main classes and their relationships in the system
   - Includes order, payment, pricing, and delivery aggregate roots, along with related entities, value objects, and domain services

2. **Object Diagram (object-diagram.puml)**
   - Shows instance relationships of the domain model
   - Includes concrete object instances such as orders, order items, payments, and deliveries

3. **Component Diagram (component-diagram.puml)**
   - Shows the main components of the system and their interactions
   - Based on hexagonal architecture, showing ports and adapters
   - Includes persistence adapters and external system adapters

4. **Deployment Diagram (deployment-diagram.puml)**
   - Describes the deployment architecture of the system
   - Includes servers, databases, message middleware, and external systems

5. **Package Diagram (package-diagram.puml)**
   - Shows the package structure and dependencies of the system
   - Divided into interface layer, application layer, domain layer, and infrastructure layer according to hexagonal architecture
   - Includes package structure for pricing and delivery modules

6. **Sequence Diagram (sequence-diagram.puml)**
   - Describes the main processes of order processing
   - Includes sequences for creating orders, processing payments, and adding order items

7. **Pricing Processing Sequence Diagram (pricing-sequence-diagram.puml)**
   - Describes the main processes of pricing processing
   - Includes creating pricing rules, updating commission rates, getting pricing rules for product categories, and calculating commissions

8. **Delivery Processing Sequence Diagram (delivery-sequence-diagram.puml)**
   - Describes the main processes of delivery processing
   - Includes creating deliveries, arranging deliveries, allocating delivery resources, updating delivery addresses, marking as delivered, etc.

9. **State Diagram (state-diagram.puml)**
   - Shows transitions between different states of orders
   - Includes sub-states and possible rollback paths

10. **Activity Diagram Overview (activity-diagram-overview.puml)**
    - High-level view of the main business processes in the e-commerce system
    - Includes interactions between customers, order system, payment system, and logistics system

11. **Activity Diagram Detail (activity-diagram-detail.puml)**
    - Detailed view of specific steps in order processing
    - Includes interactions between layers and event flows

12. **Use Case Diagram (use-case-diagram.puml)**
    - Describes the main functions and actors of the system
    - Distinguishes between core use cases and extended use cases

### Domain-Driven Design Diagrams

13. **Domain Model Diagram (domain-model-diagram.puml)**
    - Detailed view of aggregate roots, entities, value objects, and domain services in the system
    - Organized by domain context
    - Includes order, payment, pricing, and delivery aggregates

14. **Hexagonal Architecture Diagram (hexagonal-architecture-diagram.puml)**
    - Detailed view of the system's ports and adapters pattern
    - Includes driving adapters, application core, and driven adapters
    - Shows the role of application layer mappers

15. **DDD Layers Diagram (ddd-layers-diagram.puml)**
    - Shows dependency relationships and data flow in DDD layered architecture
    - Detailed explanation of responsibilities and components of each layer
    - Special emphasis on data transformation and mapper roles

16. **Saga Pattern Diagram (saga-pattern-diagram.puml)**
    - Shows distributed transaction processing flow
    - Includes normal flow and compensating transactions

17. **Bounded Context Diagram (bounded-context-diagram.puml)**
    - Shows relationships between different contexts in the system
    - Includes context mapping patterns

18. **Event Storming Diagrams (big-picture-exploration.puml, process-modeling.puml, design-level.puml)**
    - Shows commands, events, aggregate roots, policies, and read models in the system
    - Based on results from Event Storming workshops
    - **Big Picture Exploration**: Quickly understand the entire business domain
    - **Process Modeling**: Deep understanding of cause-and-effect relationships between events
    - **Design Level**: Provide detailed design for software implementation

### Advanced Architecture Diagrams

19. **CQRS Pattern Diagram (cqrs-pattern-diagram.puml)**
    - Shows Command and Query Responsibility Segregation pattern
    - Includes architecture for command side and query side

20. **Event Sourcing Diagram (event-sourcing-diagram.puml)**
    - Shows event storage and replay mechanisms
    - Includes how to build read models from events

21. **API Interface Diagram (api-interface-diagram.puml)**
    - Shows API interfaces provided by the system
    - Includes endpoints and data structures

22. **Data Model Diagram (data-model-diagram.puml)**
    - Shows the database model and relationships of the system
    - Includes tables, columns, and relationships

23. **Security Architecture Diagram (security-architecture-diagram.puml)**
    - Shows security mechanisms and authentication/authorization flows of the system
    - Includes security controls and monitoring

24. **Observability Diagram (observability-diagram.puml)**
    - Shows monitoring, logging, and observability architecture of the system
    - Includes metrics, logs, traces, and alerts

## How to View Diagrams

These diagrams are created using PlantUML and can be viewed in the following ways:

1. **Using PlantUML Online Service**:
   - Visit <http://www.plantuml.com/plantuml/uml/>
   - Copy and paste the .puml file content into the editor

2. **Using PlantUML Local Rendering**:
   - Use plantuml.jar in the project root directory
   - Execute command: `java -jar plantuml.jar docs/uml/diagram-name.puml`

3. **Using IDE Plugins**:
   - IDEs like IntelliJ IDEA, VS Code have PlantUML plugins
   - After installing plugins, you can preview directly in the IDE

## Diagram Update Guidelines

When updating these UML diagrams, please follow these principles:

1. Keep diagrams consistent with actual code
2. Use English naming and comments for better readability
3. Use colors and grouping appropriately to enhance visual effects
4. Add necessary comments to explain complex relationships or concepts
5. Record changes in this document after updates

## Recent Updates

- 2023-03-23: Initial version created
- 2024-05-10: Updated all diagrams to reflect latest system architecture
- 2024-05-10: Added new Domain-Driven Design diagrams
- 2024-05-10: Added advanced architecture diagrams
- 2024-05-10: Added object diagrams and activity diagrams
- 2024-06-08: Updated class diagram, component diagram, domain model diagram, hexagonal architecture diagram, and package diagram
- 2024-06-08: Added pricing processing sequence diagram, delivery processing sequence diagram, and DDD layers diagram

## Diagram Preview

To view diagrams, please use one of the methods above to render the .puml files. Here are previews of some example diagrams:

### Class Diagram

![Class Diagram](./class-diagram.svg)

### Object Diagram

![Object Diagram](./object-diagram.svg)

### Component Diagram

![Component Diagram](../../generated/legacy/.png)

### Deployment Diagram

![Deployment Diagram](./deployment-diagram.svg)

### Package Diagram

![Package Diagram](../../generated/legacy/.png)

### Sequence Diagram

![Sequence Diagram](../../generated/legacy/.png)

### Pricing Processing Sequence Diagram

![Pricing Processing Sequence Diagram](../../generated/legacy/.png)

### Delivery Processing Sequence Diagram

![Delivery Processing Sequence Diagram](../../generated/legacy/.png)

### State Diagram

![State Diagram](../../generated/legacy/.png)

### Activity Diagram Overview

![Activity Diagram Overview](../../generated/legacy/.png)

### Activity Diagram Detail

![Activity Diagram Detail](../../generated/legacy/.png)

### Use Case Diagram

![Use Case Diagram](../../generated/legacy/.png)

### Domain Model Diagram

![Domain Model Diagram](../../generated/legacy/.png)

### Hexagonal Architecture Diagram

![Hexagonal Architecture Diagram](../../generated/legacy/.png)

### DDD Layers Diagram

![DDD Layers Diagram](../../generated/legacy/DDD.png)

### Saga Pattern Diagram

![Saga Pattern Diagram](../../generated/legacy/saga.png)

### Bounded Context Diagram

![Bounded Context Diagram](../../generated/legacy/.png)

### Event Storming Diagram - Big Picture Exploration

![Event Storming Diagram - Big Picture Exploration](../../generated/legacy/big-picture-exploration.png)

### Event Storming Diagram - Process Modeling

![Event Storming Diagram - Process Modeling](../../generated/legacy/process-modeling.png)

### Event Storming Diagram - Design Level

![Event Storming Diagram - Design Level](../../generated/legacy/design-level.png)

### CQRS Pattern Diagram

![CQRS Pattern Diagram](../../generated/legacy/cqrs pattern diagram.png)

### Event Sourcing Diagram

![Event Sourcing Diagram](../../generated/legacy/event sourcing diagram.png)

### API Interface Diagram

![API Interface Diagram](../../generated/legacy/api interface diagram.png)

### Data Model Diagram

![Data Model Diagram](../../generated/legacy/data model diagram.png)

### Security Architecture Diagram

![Security Architecture Diagram](../../generated/legacy/security architecture diagram.png)

### Observability Diagram

![Observability Diagram](./observability-diagram.svg)