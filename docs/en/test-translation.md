# Test Translation Feature

This is a test file to verify that the automatic translation feature works properly.

## Project Architecture

This project adopts hexagonal architecture. For detailed information, please refer to:

- [Architecture Overview](architecture-overview.md)
- [Design Guidelines](DesignGuideline.MD#tell-dont-ask-principle)
- [Release Notes](releases/README.md)

## Tech Stack

- **Core Framework**: Spring Boot 3.2.0
- **Build Tool**: Gradle 8.x
- **Testing Framework**: JUnit 5

## Diagram Examples

![Class Diagram](uml/class-diagram.svg)

## Code Examples

```java
// This is an example class
public class Order {
    private OrderId id;
    private OrderStatus status;
    
    // Process order
    public void process() {
        if (status == OrderStatus.CREATED) {
            status = OrderStatus.PROCESSING;
        }
    }
}
```

## Link Testing

- [Root README](../README.md)
- [UML Documentation](uml/README.md)
- [External Link](https://example.com)

This file contains various elements that need translation and link conversion.