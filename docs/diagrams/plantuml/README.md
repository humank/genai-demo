# E-commerce System UML Documentation

This directory contains various UML diagrams for the e-commerce system, describing the system's architecture, design, and behavior.

## Diagram Catalog

### Basic Diagrams

1. **Class Diagram (class-diagram.puml)**
   - Describes the main classes and their relationships in the system
   - Includes Order, Payment, Pricing, and Delivery aggregate roots, along with related entities, value objects, and domain services

2. **Object Diagram (object-diagram.puml)**
   - Shows instance relationships of the domain model
   - Includes concrete object instances such as orders, order items, payments, and deliveries

3. **Component Diagram (component-diagram.puml)**
   - Shows the main components of the system and their interactions
   - Based on hexagonal architecture, showing ports and adapters
   - Includes persistence adapters and external system adapters

4. **Deployment Diagram (deployment-diagram.puml)**
   - Describes the system's deployment architecture
   - Includes servers, databases, message middleware, and external systems

5. **Package Diagram (package-diagram.puml)**
   - Shows the system's package structure and dependencies
   - Divided according to hexagonal architecture into interface, application, domain, and infrastructure layers
   - Includes package structure for pricing and delivery modules

6. **Sequence Diagram (sequence-diagram.puml)**
   - Describes the main processes of order processing
   - Includes sequences for creating orders, processing payments, and adding order items

7. **Pricing Processing Sequence Diagram (pricing-sequence-diagram.puml)**
   - Describes the main processes of pricing processing
   - Includes creating pricing rules, updating commission rates, getting pricing rules for product categories, and calculating commissions

8. **Delivery Processing Sequence Diagram (delivery-sequence-diagram.puml)**
   - Describes the main processes of delivery processing
   - Includes creating deliveries, scheduling deliveries, allocating delivery resources, updating delivery addresses, marking as delivered, and other operations

9. **State Diagram (state-diagram.puml)**
   - Shows transitions between different order states
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
    - Includes Order, Payment, Pricing, and Delivery aggregates

14. **Hexagonal Architecture Diagram (hexagonal-architecture-diagram.puml)**
    - Detailed view of the system's ports and adapters pattern
    - Includes driving adapters, application core, and driven adapters
    - Shows the role of application layer mappers

15. **DDD Layers Diagram (ddd-layers-diagram.puml)**
    - Shows dependency relationships and data flow in DDD layered architecture
    - Detailed explanation of responsibilities and components in each layer
    - Special emphasis on data transformation and mapper roles

16. **Saga Pattern Diagram (saga-pattern-diagram.puml)**
    - Shows distributed transaction processing flow
    - Includes normal flow and compensating transactions

17. **Bounded Context Diagram (bounded-context-diagram.puml)**
    - Shows relationships between different contexts in the system
    - Includes context mapping patterns

18. **Event Storming Diagrams (big-picture-exploration.puml, process-modeling.puml, design-level.puml)**
    - Shows commands, events, aggregate roots, policies, and read models in the system
    - Based on event storming workshop results
    - **Big Picture Exploration**: Quickly understand the entire business domain
    - **Process Modeling**: Deep understanding of causal relationships between events
    - **Design Level**: Detailed design for software implementation

### Advanced Architecture Diagrams

19. **CQRS Pattern Diagram (cqrs-pattern-diagram.puml)**
    - Shows Command and Query Responsibility Segregation pattern
    - Includes command-side and query-side architecture

20. **Event Sourcing Diagram (event-sourcing-diagram.puml)**
    - Shows event storage and replay mechanisms
    - Includes how to build read models from events

21. **API Interface Diagram (api-interface-diagram.puml)**
    - Shows API interfaces exposed by the system
    - Includes endpoints and data structures

22. **Data Model Diagram (data-model-diagram.puml)**
    - Shows the system's database model and relationships
    - Includes tables, columns, and relationships

23. **Security Architecture Diagram (security-architecture-diagram.puml)**
    - Shows the system's security mechanisms and authentication/authorization flows
    - Includes security controls and monitoring

24. **Observability Architecture Diagram (observability-diagram.puml)**
    - Shows the system's monitoring, logging, and observability architecture
    - Includes metrics, logs, traces, and alerts

## How to View Diagrams

These diagrams are created using PlantUML and can be viewed through the following methods:

1. **Using PlantUML Online Service**:
   - Visit <http://www.plantuml.com/plantuml/uml/>
   - Copy the .puml file content and paste it into the editor

2. **Using PlantUML Local Rendering**:
   - Use plantuml.jar in the project root directory
   - Execute command: `java -jar plantuml.jar docs/uml/diagram-name.puml`

3. **Using IDE Plugins**:
   - IDEs like IntelliJ IDEA, VS Code have PlantUML plugins
   - After installing the plugin, you can preview directly in the IDE

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

## Diagram Categories by Purpose

### System Understanding
- **[Class Diagram](class-diagram.puml)** - Core domain model structure
- **[Domain Model Diagram](domain-model-diagram.puml)** - DDD tactical patterns
- **[Bounded Context Diagram](bounded-context-diagram.puml)** - Strategic design

### Architecture Documentation
- **[Hexagonal Architecture Diagram](hexagonal-architecture-diagram.puml)** - Port-Adapter pattern
- **[Component Diagram](component-diagram.puml)** - System components
- **[Package Diagram](package-diagram.puml)** - Code organization

### Process Documentation
- **[Sequence Diagrams](sequence-diagram.puml)** - Interaction flows
- **[Activity Diagrams](activity-diagram-overview.puml)** - Business processes
- **[State Diagram](state-diagram.puml)** - State transitions

### Event Storming Results
- **[Big Picture Exploration](big-picture-exploration.puml)** - Domain overview
- **[Process Modeling](process-modeling.puml)** - Event causality
- **[Design Level](design-level.puml)** - Implementation design

### Advanced Patterns
- **[CQRS Pattern](cqrs-pattern-diagram.puml)** - Command-Query separation
- **[Event Sourcing](event-sourcing-diagram.puml)** - Event-based persistence
- **[Saga Pattern](saga-pattern-diagram.puml)** - Distributed transactions

## Diagram Generation and Maintenance

### Automated Generation
- **Build Integration**: Diagrams are generated during the build process
- **CI/CD Pipeline**: Automatic validation and generation in continuous integration
- **Version Control**: All diagram sources are version controlled
- **Change Detection**: Automatic detection of diagram changes

### Quality Assurance
- **Consistency Checks**: Ensure diagrams match implementation
- **Style Validation**: Verify adherence to visual standards
- **Link Validation**: Check all diagram references
- **Performance Optimization**: Optimize for fast rendering

### Maintenance Schedule
- **Weekly**: Check for outdated diagrams
- **Monthly**: Update diagrams based on code changes
- **Quarterly**: Comprehensive review and cleanup
- **Release**: Update all diagrams for major releases

## Integration with Documentation

### Cross-References
- **Architecture Documentation**: Links to architecture viewpoints
- **Design Documentation**: References in design guides
- **API Documentation**: Integration with API specifications
- **Development Guides**: Referenced in development workflows

### Export Formats
- **PNG**: For documentation embedding
- **SVG**: For scalable web display
- **PDF**: For print documentation
- **Interactive**: For online exploration

## Tool Configuration

### PlantUML Settings
```plantuml
!theme plain
skinparam backgroundColor white
skinparam classBackgroundColor lightblue
skinparam classBorderColor black
skinparam arrowColor black
```

### IDE Integration
- **IntelliJ IDEA**: PlantUML Integration plugin
- **VS Code**: PlantUML extension
- **Eclipse**: PlantUML plugin
- **Vim**: PlantUML syntax highlighting

### Build System Integration
```gradle
task generateDiagrams(type: Exec) {
    commandLine 'java', '-jar', 'plantuml.jar', 'docs/diagrams/plantuml/*.puml'
}
```

## Troubleshooting

### Common Issues
1. **Rendering Problems**: Check PlantUML syntax and dependencies
2. **Performance Issues**: Simplify complex diagrams or split into multiple views
3. **Export Issues**: Verify output format and file permissions
4. **Version Conflicts**: Ensure PlantUML version compatibility

### Support Resources
- **PlantUML Documentation**: <https://plantuml.com/>
- **Syntax Reference**: <https://plantuml.com/guide>
- **Community Forum**: <https://forum.plantuml.net/>
- **GitHub Issues**: <https://github.com/plantuml/plantuml/issues>

---

**Diagram Maintainer**: Development Team  
**Last Updated**: January 22, 2025  
**Next Review**: April 22, 2025  
**PlantUML Version**: 1.2024.x

**Quick Navigation**:
- [Domain Models](domain-models/) - Core business entities
- [Event Storming](event-storming/) - Event-driven design
- [Architecture](../mermaid/architecture/) - System architecture (Mermaid)
- [Legacy Diagrams](../legacy/) - Historical reference
