# ArchUnit Architecture Testing Recommendations

Based on the comprehensive analysis of your Java application, here are complete ArchUnit testing recommendations to protect DDD tactical design patterns and hexagonal architecture.

## 1. Layered Architecture Tests

### 1.1 Basic Layer Dependency Rules
```java
@Test
@DisplayName("Layered architecture should respect dependency direction")
void layered_architecture_should_respect_dependency_direction() {
    layeredArchitecture()
        .consideringAllDependencies()
        .layer("Interfaces").definedBy("..interfaces..")
        .layer("Application").definedBy("..application..")
        .layer("Domain").definedBy("..domain..")
        .layer("Infrastructure").definedBy("..infrastructure..")
        
        .whereLayer("Interfaces").mayNotBeAccessedByAnyLayer()
        .whereLayer("Application").mayOnlyBeAccessedByLayers("Interfaces")
        .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure")
        .whereLayer("Infrastructure").mayNotAccessAnyLayer()
        
        .check(classes);
}
```

### 1.2 Domain Layer Purity Check
```java
@Test
@DisplayName("Domain layer should not depend on external technical frameworks")
void domain_layer_should_not_depend_on_infrastructure_concerns() {
    noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAnyPackage(
            "org.springframework..",
            "javax.persistence..",
            "jakarta.persistence..",
            "org.hibernate..",
            "com.fasterxml.jackson.."
        )
        .check(classes);
}
```

## 2. DDD Tactical Patterns Tests

### 2.1 Aggregate Root Rules
```java
@Test
@DisplayName("Aggregate roots must be annotated with @AggregateRoot")
void aggregate_roots_should_be_annotated() {
    classes()
        .that().resideInAPackage("..domain.*.model.aggregate..")
        .should().beAnnotatedWith(AggregateRoot.class)
        .check(classes);
}

@Test
@DisplayName("Aggregate roots must implement AggregateRootInterface")
void aggregate_roots_should_implement_interface() {
    classes()
        .that().areAnnotatedWith(AggregateRoot.class)
        .should().implement(AggregateRootInterface.class)
        .check(classes);
}

@Test
@DisplayName("Only aggregate roots should be accessed from application layer")
void only_aggregate_roots_should_be_accessed_from_application_layer() {
    classes()
        .that().resideInAPackage("..application..")
        .should().onlyAccessClassesThat(
            resideInAnyPackage(
                "..domain.*.model.aggregate..",
                "..domain.*.repository..",
                "..domain.*.service..",
                "..domain.common..",
                "..application..",
                "java..",
                "org.springframework.."
            )
        )
        .check(classes);
}
```

### 2.2 Value Object Rules
```java
@Test
@DisplayName("Value objects must be annotated with @ValueObject")
void value_objects_should_be_annotated() {
    classes()
        .that().resideInAPackage("..domain.*.model.valueobject..")
        .should().beAnnotatedWith(ValueObject.class)
        .check(classes);
}

@Test
@DisplayName("Value objects should be records or enums")
void value_objects_should_be_records_or_enums() {
    classes()
        .that().areAnnotatedWith(ValueObject.class)
        .should().beRecords()
        .orShould().beEnums()
        .check(classes);
}

@Test
@DisplayName("Value objects should not have setter methods")
void value_objects_should_be_immutable() {
    noMethods()
        .that().areDeclaredInClassesThat().areAnnotatedWith(ValueObject.class)
        .should().haveNameMatching("set.*")
        .check(classes);
}
```

### 2.3 Domain Event Rules
```java
@Test
@DisplayName("Domain events must implement DomainEvent interface")
void domain_events_should_implement_domain_event_interface() {
    classes()
        .that().resideInAPackage("..domain.*.events..")
        .or().resideInAPackage("..domain.*.model.events..")
        .should().implement(DomainEvent.class)
        .check(classes);
}

@Test
@DisplayName("Domain events should be records")
void domain_events_should_be_records() {
    classes()
        .that().implement(DomainEvent.class)
        .should().beRecords()
        .check(classes);
}

@Test
@DisplayName("Domain events should be immutable")
void domain_events_should_be_immutable() {
    noMethods()
        .that().areDeclaredInClassesThat().implement(DomainEvent.class)
        .should().haveNameMatching("set.*")
        .check(classes);
}
```

### 2.4 Repository Rules
```java
@Test
@DisplayName("Repository interfaces should be in domain layer")
void repository_interfaces_should_be_in_domain_layer() {
    classes()
        .that().haveSimpleNameEndingWith("Repository")
        .and().areInterfaces()
        .should().resideInAPackage("..domain.*.repository..")
        .check(classes);
}

@Test
@DisplayName("Repository implementations should be in infrastructure layer")
void repository_implementations_should_be_in_infrastructure_layer() {
    classes()
        .that().haveSimpleNameEndingWith("RepositoryAdapter")
        .or().haveSimpleNameEndingWith("RepositoryImpl")
        .should().resideInAPackage("..infrastructure.*.persistence..")
        .check(classes);
}

@Test
@DisplayName("Repository implementations must implement domain repository interfaces")
void repository_implementations_should_implement_domain_interfaces() {
    classes()
        .that().resideInAPackage("..infrastructure.*.persistence..")
        .and().haveSimpleNameEndingWith("Adapter")
        .should().implementInterfacesThat().resideInAPackage("..domain.*.repository..")
        .check(classes);
}
```

### 2.5 Domain Service Rules
```java
@Test
@DisplayName("Domain services must be annotated with @DomainService")
void domain_services_should_be_annotated() {
    classes()
        .that().resideInAPackage("..domain.*.service..")
        .should().beAnnotatedWith(DomainService.class)
        .check(classes);
}

@Test
@DisplayName("Domain services should not depend on infrastructure")
void domain_services_should_not_depend_on_infrastructure() {
    noClasses()
        .that().areAnnotatedWith(DomainService.class)
        .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
        .check(classes);
}
```

### 2.6 Specification Rules
```java
@Test
@DisplayName("Specifications must be annotated with @Specification")
void specifications_should_be_annotated() {
    classes()
        .that().resideInAPackage("..domain.*.model.specification..")
        .should().beAnnotatedWith(Specification.class)
        .check(classes);
}

@Test
@DisplayName("Specifications must implement Specification interface")
void specifications_should_implement_specification_interface() {
    classes()
        .that().areAnnotatedWith(Specification.class)
        .should().implement("solid.humank.genaidemo.domain.common.specification.Specification")
        .check(classes);
}
```

### 2.7 Policy Rules
```java
@Test
@DisplayName("Policies must be annotated with @Policy")
void policies_should_be_annotated() {
    classes()
        .that().resideInAPackage("..domain.*.model.policy..")
        .should().beAnnotatedWith(Policy.class)
        .check(classes);
}

@Test
@DisplayName("Policies must implement DomainPolicy interface")
void policies_should_implement_domain_policy_interface() {
    classes()
        .that().areAnnotatedWith(Policy.class)
        .should().implement("solid.humank.genaidemo.domain.common.policy.DomainPolicy")
        .check(classes);
}
```

## 3. Hexagonal Architecture Tests

### 3.1 Port and Adapter Rules
```java
@Test
@DisplayName("Primary ports should be in application layer")
void primary_ports_should_be_in_application_layer() {
    classes()
        .that().haveSimpleNameEndingWith("UseCase")
        .or().haveSimpleNameEndingWith("Service")
        .and().areInterfaces()
        .and().resideInAPackage("..application..")
        .should().beInterfaces()
        .check(classes);
}

@Test
@DisplayName("Secondary ports should be in domain layer")
void secondary_ports_should_be_in_domain_layer() {
    classes()
        .that().areInterfaces()
        .and().resideInAPackage("..domain.*.repository..")
        .should().beInterfaces()
        .check(classes);
}

@Test
@DisplayName("Primary adapters should be in interfaces layer")
void primary_adapters_should_be_in_interfaces_layer() {
    classes()
        .that().haveSimpleNameEndingWith("Controller")
        .should().resideInAPackage("..interfaces.web..")
        .check(classes);
}

@Test
@DisplayName("Secondary adapters should be in infrastructure layer")
void secondary_adapters_should_be_in_infrastructure_layer() {
    classes()
        .that().haveSimpleNameEndingWith("Adapter")
        .or().haveSimpleNameEndingWith("Impl")
        .should().resideInAPackage("..infrastructure..")
        .check(classes);
}
```

### 3.2 Dependency Inversion Checks
```java
@Test
@DisplayName("Infrastructure should not be accessed by domain")
void infrastructure_should_not_be_accessed_by_domain() {
    noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
        .check(classes);
}

@Test
@DisplayName("Application should not depend on infrastructure implementations")
void application_should_not_depend_on_infrastructure_implementations() {
    noClasses()
        .that().resideInAPackage("..application..")
        .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
        .check(classes);
}
```

## 4. Naming Convention Tests

### 4.1 Package Naming Conventions
```java
@Test
@DisplayName("Aggregates should be in correct package")
void aggregates_should_be_in_correct_package() {
    classes()
        .that().areAnnotatedWith(AggregateRoot.class)
        .should().resideInAPackage("..domain.*.model.aggregate..")
        .check(classes);
}

@Test
@DisplayName("Controllers should end with Controller")
void controllers_should_have_correct_naming() {
    classes()
        .that().resideInAPackage("..interfaces.web..")
        .and().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
        .should().haveSimpleNameEndingWith("Controller")
        .check(classes);
}

@Test
@DisplayName("JPA entities should end with Entity and be in correct package")
void jpa_entities_should_have_correct_naming_and_location() {
    classes()
        .that().areAnnotatedWith("jakarta.persistence.Entity")
        .should().haveSimpleNameEndingWith("Entity")
        .andShould().resideInAPackage("..infrastructure.*.persistence.entity..")
        .check(classes);
}
```

### 4.2 Method Naming Conventions
```java
@Test
@DisplayName("Repository methods should follow naming conventions")
void repository_methods_should_follow_naming_conventions() {
    methods()
        .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Repository")
        .and().arePublic()
        .should().haveNameMatching("(find|save|delete|exists|count).*")
        .check(classes);
}

@Test
@DisplayName("Domain service methods should use business language")
void domain_service_methods_should_use_business_language() {
    noMethods()
        .that().areDeclaredInClassesThat().areAnnotatedWith(DomainService.class)
        .should().haveNameMatching("(get|set).*")
        .check(classes);
}
```

## 5. Technical Constraint Tests

### 5.1 Spring Annotation Usage Restrictions
```java
@Test
@DisplayName("Domain layer should not use Spring annotations")
void domain_layer_should_not_use_spring_annotations() {
    noClasses()
        .that().resideInAPackage("..domain..")
        .should().beAnnotatedWith("org.springframework.stereotype.Service")
        .orShould().beAnnotatedWith("org.springframework.stereotype.Component")
        .orShould().beAnnotatedWith("org.springframework.stereotype.Repository")
        .check(classes);
}

@Test
@DisplayName("Only infrastructure and interfaces should use JPA annotations")
void only_infrastructure_should_use_jpa_annotations() {
    classes()
        .that().areAnnotatedWith("jakarta.persistence.Entity")
        .or().areAnnotatedWith("jakarta.persistence.Table")
        .should().resideInAPackage("..infrastructure..")
        .check(classes);
}
```

### 5.2 Circular Dependency Check
```java
@Test
@DisplayName("Should not have circular dependencies")
void should_not_have_cycles() {
    slices()
        .matching("solid.humank.genaidemo.(*)..")
        .should().beFreeOfCycles()
        .check(classes);
}
```

## 6. Event Handling Tests

### 6.1 Event Publishing Rules
```java
@Test
@DisplayName("Only aggregate roots should publish domain events")
void only_aggregate_roots_should_publish_domain_events() {
    methods()
        .that().haveNameMatching(".*collectEvent.*")
        .should().beDeclaredInClassesThat().areAnnotatedWith(AggregateRoot.class)
        .check(classes);
}

@Test
@DisplayName("Event handlers should be in infrastructure layer")
void event_handlers_should_be_in_infrastructure_layer() {
    classes()
        .that().haveSimpleNameEndingWith("EventHandler")
        .should().resideInAPackage("..infrastructure.event.handler..")
        .check(classes);
}
```

## 7. Test Organization Recommendations

### 7.1 Test Class Structure
```java
// Recommended test class organization
@AnalyzeClasses(packages = "solid.humank.genaidemo")
public class ArchitectureTest {
    
    @Nested
    @DisplayName("Layered Architecture Tests")
    class LayeredArchitectureTest {
        // Layered architecture related tests
    }
    
    @Nested
    @DisplayName("DDD Tactical Patterns Tests")
    class DddTacticalPatternsTest {
        // DDD pattern related tests
    }
    
    @Nested
    @DisplayName("Hexagonal Architecture Tests")
    class HexagonalArchitectureTest {
        // Hexagonal architecture related tests
    }
    
    @Nested
    @DisplayName("Naming Convention Tests")
    class NamingConventionTest {
        // Naming convention related tests
    }
}
```

### 7.2 Custom ArchUnit Rules
```java
// Custom rule examples
public static final ArchRule AGGREGATE_ROOTS_SHOULD_BE_PROPERLY_DESIGNED = 
    classes()
        .that().areAnnotatedWith(AggregateRoot.class)
        .should().implement(AggregateRootInterface.class)
        .andShould().resideInAPackage("..domain.*.model.aggregate..")
        .andShould().notBeAnnotatedWith("org.springframework.stereotype.Component");

public static final ArchRule VALUE_OBJECTS_SHOULD_BE_IMMUTABLE = 
    classes()
        .that().areAnnotatedWith(ValueObject.class)
        .should().beRecords()
        .orShould().beEnums()
        .andShould().notHaveMethodsThat().haveNameMatching("set.*");
```

## 8. Implementation Recommendations

### 8.1 Gradle Configuration
```gradle
dependencies {
    testImplementation 'com.tngtech.archunit:archunit-junit5:1.0.1'
    testImplementation 'com.tngtech.archunit:archunit:1.0.1'
}

test {
    useJUnitPlatform()
    systemProperty 'archunit.freeze.store.default.path', 'build/archunit'
}
```

### 8.2 CI/CD Integration
- Include architecture tests in CI/CD pipeline
- Configure build failure on architecture violations
- Regularly review and update architecture rules

These ArchUnit tests will ensure your project consistently follows DDD tactical design patterns and hexagonal architecture principles, preventing architectural decay.
