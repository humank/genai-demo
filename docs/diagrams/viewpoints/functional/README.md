# Functional Viewpoint - Generated Diagrams

This directory contains automatically generated PlantUML diagrams based on the analysis of the project's DDD code structure and BDD feature files.

## Generated on: 2025-09-21

## DDD Code Analysis Results

### Overview
- **Total Java files analyzed**: 630
- **Domain classes found**: 116
- **Application services found**: 13
- **Repositories found**: 97
- **Controllers found**: 17
- **Domain events found**: 59
- **Bounded contexts identified**: 13

### Bounded Contexts
Customer, Delivery, Inventory, Notification, Observability, Order, Payment, Pricing, Product, Promotion, Review, Seller, ShoppingCart

## BDD Feature Analysis Results

### Overview
- **Feature files analyzed**: 27
- **Scenarios found**: 225
- **Business events extracted**: 51
- **User journeys identified**: 99
- **Bounded contexts from features**: 11
- **Actors identified**: 55

### Bounded Contexts from Features
Consumer, Customer, Inventory, Logistics, Notification, Order, Payment, Pricing, Product, Promotion, Workflow

## Generated Diagrams

### System Overview Diagrams
1. **system-overview.mmd** - Complete system architecture overview showing 7-layer architecture with external systems, API gateway, application services, domain layer, event-driven architecture, infrastructure, observability platform, and deployment platform

![系統概覽](system-overview.svg)

### Domain Model Diagrams (from DDD Code)
1. **domain-model-overview.puml** - Complete domain model overview with all aggregates, entities, and value objects
   !\1

2. **hexagonal-architecture-overview.puml** - Hexagonal architecture showing domain core, application layer, infrastructure layer, and interface layer
   !\1

3. **application-services-overview.puml** - Application services grouped by bounded context
   !\1

4. **infrastructure-layer-overview.puml** - Infrastructure layer components including repositories
   !\1

5. **bounded-contexts-overview.puml** - High-level bounded context relationships
   !\1

6. **domain-events-flow.puml** - Domain events flow between aggregates
   !\1

### Aggregate Details (per Bounded Context)
- **customer-aggregate-details.puml** - Customer aggregate detailed design
- **order-aggregate-details.puml** - Order aggregate detailed design
- **product-aggregate-details.puml** - Product aggregate detailed design
- **inventory-aggregate-details.puml** - Inventory aggregate detailed design
- **payment-aggregate-details.puml** - Payment aggregate detailed design
- **notification-aggregate-details.puml** - Notification aggregate detailed design
- **delivery-aggregate-details.puml** - Delivery aggregate detailed design
- **promotion-aggregate-details.puml** - Promotion aggregate detailed design
- **review-aggregate-details.puml** - Review aggregate detailed design
- **seller-aggregate-details.puml** - Seller aggregate detailed design
- **shoppingcart-aggregate-details.puml** - Shopping cart aggregate detailed design
- **pricing-aggregate-details.puml** - Pricing aggregate detailed design
- **observability-aggregate-details.puml** - Observability aggregate detailed design

### Business Process Diagrams (from BDD Features)
1. **event-storming-big-picture.puml** - Event Storming Big Picture with standard colors (events in orange, actors in yellow, commands in blue)
2. **event-storming-process-level.puml** - Event Storming Process Level showing detailed business processes
3. **business-process-flows.puml** - Business process flows as activity diagrams
4. **user-journey-overview.puml** - User journey overview showing actors and their interactions
5. **bdd-features-overview.puml** - Overview of all BDD features grouped by bounded context

## Analysis Summaries

### DDD Analysis Summary
- **File**: `analysis-summary.json`
- **Contains**: Detailed breakdown of all domain classes, application services, repositories, controllers, and events with their packages and metadata

### BDD Analysis Summary
- **File**: `bdd-analysis-summary.json`
- **Contains**: Detailed breakdown of all features, scenarios, business events, user journeys, and actors extracted from feature files

## Key Insights

### Architecture Patterns Identified
1. **Hexagonal Architecture**: Clear separation between domain core, application services, infrastructure, and interfaces
2. **Domain-Driven Design**: Well-defined aggregates, entities, value objects, and domain events
3. **Event-Driven Architecture**: Rich domain events supporting business process automation
4. **CQRS Pattern**: Separate read and write models evident in repository patterns

### Business Capabilities Identified
1. **Customer Management**: Customer registration, profile management, membership levels, reward points
2. **Order Processing**: Order creation, item management, submission, cancellation
3. **Product Catalog**: Product management, bundling, pricing, inventory
4. **Payment Processing**: Multiple payment methods, installments, discounts
5. **Promotion Engine**: Coupons, flash sales, gift with purchase, tiered discounts
6. **Delivery Management**: Multiple delivery methods, tracking, address management
7. **Notification System**: Multi-channel notifications, preferences, templates
8. **Review System**: Product reviews, moderation, responses

### Cross-Cutting Concerns
1. **Observability**: Comprehensive analytics, performance monitoring, data retention
2. **Security**: Authentication, authorization, data protection
3. **Performance**: Load testing, monitoring, optimization
4. **Infrastructure**: CI/CD, disaster recovery, multi-region deployment

## Usage

These diagrams can be used with PlantUML to generate visual representations:

```bash
# Generate PNG from PlantUML
plantuml -tpng docs/diagrams/viewpoints/functional/*.puml

# Generate SVG from PlantUML
plantuml -tsvg docs/diagrams/viewpoints/functional/*.puml
```

## Regeneration

To regenerate these diagrams after code changes:

```bash
# Analyze DDD code and generate domain diagrams
python3 scripts/analyze-ddd-code.py app/src/main/java/solid/humank/genaidemo docs/diagrams/viewpoints/functional

# Analyze BDD features and generate business process diagrams
python3 scripts/analyze-bdd-features.py app/src/test/resources/features docs/diagrams/viewpoints/functional
```

## Next Steps

1. **PNG Generation**: Convert PlantUML files to PNG for documentation embedding
2. **Mermaid Conversion**: Create Mermaid versions for GitHub direct display
3. **Excalidraw Concepts**: Generate conceptual diagrams using Excalidraw MCP
4. **Documentation Integration**: Embed diagrams in viewpoint documentation
5. **Automation**: Set up Kiro hooks for automatic regeneration on code changes