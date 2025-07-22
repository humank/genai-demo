<!-- This file is auto-translated from docs/en/UpgradeJava17to21.md -->
<!-- 此檔案由 docs/en/UpgradeJava17to21.md 自動翻譯而來 -->
<!-- Please use Kiro AI to complete the actual translation -->
<!-- 請使用 Kiro AI 完成實際翻譯 -->

# Java 17 to Java 21 Upgrade Improvement Guide

This document details the code improvements that can be adopted when upgrading a project from Java 17 to Java 21, focusing on how to leverage Java 21's new features to optimize Domain-Driven Design (DDD) implementation.

## Table of Contents

1. [Pattern Matching for instanceof](#1-pattern-matching-for-instanceof)
2. [Enhanced Switch Expressions](#2-enhanced-switch-expressions)
3. [String Templates](#3-string-templates)
4. [Record Types](#4-record-types)
5. [SequencedCollection Interface](#5-sequencedcollection-interface)
6. [Virtual Threads](#6-virtual-threads)
7. [Pattern Matching for Switch](#7-pattern-matching-for-switch)
8. [Functional Interfaces and Lazy Evaluation](#8-functional-interfaces-and-lazy-evaluation)
9. [Summary and DDD Compliance](#9-summary-and-ddd-compliance)

## 1. Pattern Matching for instanceof

### Before Improvement
```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Money money = (Money) o;
    return Objects.equals(amount, money.amount) && Objects.equals(currency, money.currency);
}
```

### After Improvement
```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (o instanceof Money money) {
        return Objects.equals(amount, money.amount) && Objects.equals(currency, money.currency);
    }
    return false;
}
```

### Benefits
- **Code Simplification**: Combines type checking and casting, reducing redundant code
- **Improved Safety**: Eliminates the risk of explicit type casting
- **Enhanced Readability**: Code is more intuitive with clearer intent
- **DDD Compliance**: Fully compliant with Value Object implementation requirements, maintaining immutability and equality comparison logic

## 2. Enhanced Switch Expressions

### Before Improvement
```java
public boolean canTransitionTo(OrderStatus targetStatus) {
    switch (this) {
        case CREATED:
            return targetStatus == SUBMITTED || targetStatus == PENDING || targetStatus == CANCELLED;
        case SUBMITTED:
            return targetStatus == PENDING || targetStatus == PAID || targetStatus == REJECTED || targetStatus == CANCELLED;
        case PENDING:
            return targetStatus == CONFIRMED || targetStatus == REJECTED || targetStatus == CANCELLED;
        case CONFIRMED:
            return targetStatus == PAID || targetStatus == CANCELLED;
        case PAID:
            return targetStatus == PROCESSING || targetStatus == CANCELLED;
        case PROCESSING:
            return targetStatus == SHIPPING || targetStatus == SHIPPED || targetStatus == CANCELLED;
        case SHIPPING:
            return targetStatus == SHIPPED || targetStatus == DELIVERED;
        case SHIPPED:
            return targetStatus == DELIVERED;
        case DELIVERED:
            return targetStatus == COMPLETED;
        case COMPLETED:
            return false; // Terminal state
        case CANCELLED:
            return false; // Terminal state
        case REJECTED:
            return false; // Terminal state
        case PAYMENT_FAILED:
            return targetStatus == SUBMITTED || targetStatus == PENDING || targetStatus == CANCELLED;
        default:
            return false;
    }
}
```

### After Improvement
```java
public boolean canTransitionTo(OrderStatus targetStatus) {
    return switch (this) {
        case CREATED -> targetStatus == SUBMITTED || targetStatus == PENDING || targetStatus == CANCELLED;
        case SUBMITTED -> targetStatus == PENDING || targetStatus == PAID || targetStatus == REJECTED || targetStatus == CANCELLED;
        case PENDING -> targetStatus == CONFIRMED || targetStatus == REJECTED || targetStatus == CANCELLED;
        case CONFIRMED -> targetStatus == PAID || targetStatus == CANCELLED;
        case PAID -> targetStatus == PROCESSING || targetStatus == CANCELLED;
        case PROCESSING -> targetStatus == SHIPPING || targetStatus == SHIPPED || targetStatus == CANCELLED;
        case SHIPPING -> targetStatus == SHIPPED || targetStatus == DELIVERED;
        case SHIPPED -> targetStatus == DELIVERED;
        case DELIVERED -> targetStatus == COMPLETED;
        case COMPLETED, CANCELLED, REJECTED -> false; // Terminal states
        case PAYMENT_FAILED -> targetStatus == SUBMITTED || targetStatus == PENDING || targetStatus == CANCELLED;
    };
}
```

### Benefits
- **Conciseness**: Uses arrow syntax (->) instead of verbose case and break statements
- **Expressiveness**: Can combine multiple case labels (e.g., `case COMPLETED, CANCELLED, REJECTED -> false;`)
- **Safety**: Switch expressions ensure all possible enum values are handled
- **DDD Compliance**: Enhances business rule expression for Value Objects (OrderStatus), making state transition logic clearer

## 3. String Templates

### New Feature
```java
// Using Java 21's String Templates
return STR."Amount: \{formatter.format(amount)} (\{currencyCode})";

// Multi-line text with templates
return STR."""
    Transaction Summary:
    Description: \{description}
    Amount: \{formattedAmount}
    Time: \{java.time.LocalDateTime.now()}
    """;
```

### Benefits
- **Readability**: More intuitive than traditional string concatenation or String.format()
- **Performance**: Compile-time processing avoids runtime string concatenation overhead
- **Expressiveness**: Can directly embed expressions in strings, reducing code volume
- **Multi-line Support**: Combined with text blocks, can create formatted complex output
- **DDD Compliance**: Enhances domain object presentation capabilities, making domain concept display more intuitive

### Implementation Example: MoneyFormatter Class
```java
public class MoneyFormatter {
    public static String format(Money money, Locale locale) {
        var currencyCode = money.getCurrency().getCurrencyCode();
        var amount = money.getAmount();
        var formatter = NumberFormat.getCurrencyInstance(locale);
        formatter.setCurrency(money.getCurrency());
        
        // Using Java 21's String Templates
        return STR."Amount: \{formatter.format(amount)} (\{currencyCode})";
    }
}
```

## 4. Record Types

### New Feature
```java
public record OrderSummary(
    String orderId,
    String customerId,
    OrderStatus status,
    Money totalAmount,
    Money effectiveAmount,
    int itemCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    // Compact constructor
    public OrderSummary {
        // Parameter validation
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID cannot be empty");
        }
        // Other validations...
    }
    
    // Business methods
    public boolean isCompleted() {
        return status == OrderStatus.COMPLETED || status == OrderStatus.DELIVERED;
    }
    
    public Money getDiscountAmount() {
        return totalAmount.subtract(effectiveAmount);
    }
}
```

### Benefits
- **Conciseness**: Automatically generates getter, equals, hashCode, and toString methods
- **Immutability**: Creates immutable objects by default, complying with Value Object design principles
- **Compact Constructor**: Allows parameter validation in constructor, ensuring object integrity
- **DDD Compliance**: Perfect for Value Objects or DTOs, maintaining immutability and value equality

## 5. SequencedCollection Interface

### New Feature
```java
public static <T> T getFirst(Collection<T> collection) {
    if (collection == null || collection.isEmpty()) {
        return null;
    }
    
    // Using Java 21's SequencedCollection interface
    if (collection instanceof List<T> list) {
        return list.getFirst();
    }
    
    return collection.iterator().next();
}

public static <T> List<T> reverse(List<T> list) {
    if (list == null || list.isEmpty()) {
        return List.of();
    }
    
    // Using Java 21's SequencedCollection interface
    return list.reversed();
}
```

### Benefits
- **Clear Semantics**: Methods like `getFirst()`, `getLast()`, `reversed()` are more intuitive than iterator or index operations
- **Code Simplification**: Reduces boilerplate code, making collection operations more concise
- **Performance Optimization**: Some implementations may provide more efficient operations
- **DDD Compliance**: Enhances domain service and repository implementation capabilities, making collection operations more intuitive

### Implementation Example: CollectionUtils Class
```java
public final class CollectionUtils {
    public static <T> List<T> filter(Collection<T> collection, Predicate<T> predicate) {
        if (collection == null || collection.isEmpty()) {
            return List.of();
        }
        
        // Using Java 21's enhanced Stream API functionality
        return collection.stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }
}
```

## 6. Virtual Threads

### New Feature
```java
// Using virtual thread executor
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    List<Future<?>> futures = new ArrayList<>();
    
    for (int i = 0; i < taskCount; i++) {
        futures.add(executor.submit(() -> {
            // Task logic
            Thread.sleep(sleepMillis);
            return STR."Virtual thread task completed: \{Thread.currentThread()}";
        }));
    }
    
    // Wait for all tasks to complete
    for (Future<?> future : futures) {
        future.get();
    }
}

// Directly creating virtual threads
Thread thread = Thread.ofVirtual()
        .name(name)
        .uncaughtExceptionHandler((t, e) -> 
            System.err.println(STR."Thread \{t.getName()} uncaught exception: \{e.getMessage()}"))
        .start(runnable);
```

### Benefits
- **High Concurrency**: Can create millions of virtual threads without exhausting system resources
- **Simplified Programming Model**: Write efficient asynchronous programs using synchronous code style
- **Reduced Context Switching Overhead**: Virtual thread switching cost is much lower than platform threads
- **Improved Resource Utilization**: More effective use of CPU and memory resources
- **DDD Compliance**: Enhances domain service and application service implementation capabilities, especially suitable for handling high-concurrency operation scenarios

### Implementation Example: VirtualThreadDemo Class
```java
public class VirtualThreadDemo {
    public static long runWithVirtualThreads(int taskCount, long sleepMillis) {
        Instant start = Instant.now();
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // Execute tasks...
        }
        
        Instant end = Instant.now();
        return Duration.between(start, end).toMillis();
    }
}
```

## 7. Pattern Matching for Switch

### New Feature
```java
public static String checkInputType(Object input) {
    return switch (input) {
        case null -> "Input is null";
        case String s when s.isBlank() -> "Input is blank string";
        case String s when isValidEmail(s) -> STR."Input is valid email address: \{s}";
        case String s -> STR."Input is regular string: \{s}";
        case Integer i -> STR."Input is integer: \{i}";
        case Double d -> STR."Input is double: \{d}";
        case Boolean b -> STR."Input is boolean: \{b}";
        case Object[] arr -> STR."Input is array with length: \{arr.length}";
        case java.util.List<?> list -> STR."Input is list with size: \{list.size()}";
        default -> STR."Input is other type: \{input.getClass().getSimpleName()}";
    };
}
```

### Benefits
- **Expressiveness**: Can directly perform type matching and conditional judgment in switch
- **Conciseness**: Reduces verbose if-else chains and type casting
- **Safety**: Compile-time type checking avoids runtime type errors
- **Readability**: Code structure is clearer with logical branches at a glance
- **DDD Compliance**: Enhances domain service and Specification implementation capabilities, making complex business rule expression more intuitive

### Implementation Example: StringPatternMatcher Class
```java
public class StringPatternMatcher {
    public static String checkInputType(Object input) {
        return switch (input) {
            case null -> "Input is null";
            case String s when s.isBlank() -> "Input is blank string";
            case String s when isValidEmail(s) -> STR."Input is valid email address: \{s}";
            // Other cases...
        };
    }
}
```

## 8. Functional Interfaces and Lazy Evaluation

### Before Improvement
```java
public static void checkArgument(boolean condition, String message) {
    if (!condition) {
        throw new IllegalArgumentException(message);
    }
}
```

### After Improvement
```java
public static void checkArgument(boolean condition, java.util.function.Supplier<String> messageSupplier) {
    if (!condition) {
        throw new IllegalArgumentException(messageSupplier.get());
    }
}
```

### Benefits
- **Lazy Evaluation**: Only computes error message when needed, improving performance
- **Dynamic Messages**: Can generate more specific error information based on runtime state
- **Code Organization**: Separates error message generation logic from checking logic
- **DDD Compliance**: Enhances parameter validation capabilities of domain entities and value objects, improving business rule expression

### Implementation Example: Preconditions Class
```java
public final class Preconditions {
    public static void checkArgument(boolean condition, java.util.function.Supplier<String> messageSupplier) {
        if (!condition) {
            throw new IllegalArgumentException(messageSupplier.get());
        }
    }
}
```

## 9. Summary and DDD Compliance

Java 21's new features not only improve code conciseness and readability but also better support Domain-Driven Design (DDD) implementation:

### Value Objects
- **Record Types**: Perfect for implementing immutable value objects
- **Pattern Matching for instanceof**: Simplifies equality comparison for value objects
- **String Templates**: Enhances string representation of value objects

### Entities
- **Pattern Matching for instanceof**: Simplifies equality comparison for entities
- **Functional Interfaces and Lazy Evaluation**: Enhances parameter validation and business rule expression

### Aggregate Roots
- **Virtual Threads**: Improves aggregate root's ability to handle concurrent operations
- **SequencedCollection Interface**: Simplifies aggregate root's collection management operations

### Domain Services
- **Pattern Matching for Switch**: Simplifies complex business rule expression
- **Enhanced Switch Expressions**: Makes state transitions and business logic clearer

### Specifications
- **Pattern Matching for Switch**: Enhances specification's conditional expression capabilities
- **Functional Interfaces**: Simplifies specification composition and reuse

### Repositories
- **Virtual Threads**: Improves concurrent performance of data access
- **SequencedCollection Interface**: Simplifies query result processing

### Domain Events
- **Virtual Threads**: Improves concurrent capability of event processing
- **String Templates**: Enhances event logging and debugging information expression

## Conclusion

Upgrading to Java 21 is not only technical progress but also an opportunity to enhance domain model expression capabilities. By properly utilizing new features, DDD implementation can become more concise, intuitive, and efficient while maintaining domain model purity and business expressiveness.

During the upgrade process, attention should be paid to maintaining the integrity of DDD tactical design patterns, ensuring that the use of new features enhances rather than undermines the domain model's expressive capabilities. Particularly for core DDD concepts like Value Objects and Entities, their immutability, equality comparison, and correct implementation of business rules should be ensured.

<!-- Translation placeholder - Use Kiro AI to translate this content -->
<!-- 翻譯佔位符 - 請使用 Kiro AI 翻譯此內容 -->
