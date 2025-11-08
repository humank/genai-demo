# Event Schemas

## Overview

This directory contains JSON Schema definitions for all domain events in the system. These schemas can be used for:

- **Event Validation**: Validate event payloads before publishing
- **Code Generation**: Generate event classes from schemas
- **Documentation**: Auto-generate event documentation
- **Contract Testing**: Verify event contracts between services

**Last Updated**: 2025-10-25

---

## Schema Organization

Schemas are organized by bounded context:

```text
schemas/
├── README.md (this file)
├── customer/
│   ├── CustomerCreatedEvent.json
│   ├── CustomerProfileUpdatedEvent.json
│   └── ...
├── order/
│   ├── OrderCreatedEvent.json
│   ├── OrderSubmittedEvent.json
│   └── ...
├── product/
│   ├── ProductCreatedEvent.json
│   └── ...
├── payment/
│   ├── PaymentProcessedEvent.json
│   └── ...
└── common/
    ├── DomainEvent.json (base schema)
    ├── Money.json
    ├── Address.json
    └── ...
```

---

## Schema Standards

### JSON Schema Version

All schemas use **JSON Schema Draft 2020-12**.

### Common Fields

All domain events must include these fields:

```json
{
  "eventId": "UUID",
  "eventType": "string",
  "occurredOn": "ISO 8601 datetime",
  "aggregateId": "string"
}
```

### Naming Conventions

- **Schema Files**: `{EventName}.json` (e.g., `CustomerCreatedEvent.json`)
- **Event Types**: PascalCase (e.g., `CustomerCreated`)
- **Field Names**: camelCase (e.g., `customerId`, `orderTotal`)

---

## Using Schemas

### Validation Example (Java)

```java
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;

public class EventValidator {
    
    private final JsonSchemaFactory factory = 
        JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    
    public void validateEvent(String eventJson, String schemaPath) {
        JsonSchema schema = factory.getSchema(
            getClass().getResourceAsStream(schemaPath)
        );
        
        Set<ValidationMessage> errors = schema.validate(
            new ObjectMapper().readTree(eventJson)
        );
        
        if (!errors.isEmpty()) {
            throw new EventValidationException(errors);
        }
    }
}
```

### Code Generation Example

```bash
# Using quicktype to generate Java classes from schemas
quicktype \
  --src schemas/customer/CustomerCreatedEvent.json \
  --lang java \
  --out src/main/java/events/CustomerCreatedEvent.java
```

---

## Schema Examples

### Base Domain Event Schema

See [common/DomainEvent.json](common/DomainEvent.json) for the base schema that all events extend.

### Customer Event Schema

See [customer/CustomerCreatedEvent.json](customer/CustomerCreatedEvent.json) for a complete example.

### Order Event Schema

See [order/OrderSubmittedEvent.json](order/OrderSubmittedEvent.json) for a complex event with nested objects.

---

## Schema Versioning

### Version Strategy

- **Major Version**: Breaking changes (field removal, type changes)
- **Minor Version**: Additive changes (new optional fields)
- **Patch Version**: Documentation updates, clarifications

### Version in Schema

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "https://api.example.com/schemas/v1/CustomerCreatedEvent.json",
  "version": "1.0.0",
  "title": "CustomerCreatedEvent"
}
```

### Backward Compatibility

- New fields must be optional
- Existing fields cannot be removed
- Field types cannot change
- Breaking changes require new event type

---

## Validation Tools

### Online Validators

- [JSON Schema Validator](https://www.jsonschemavalidator.net/)
- [JSON Schema Lint](https://jsonschemalint.com/)

### CLI Tools

```bash
# Install ajv-cli
npm install -g ajv-cli

# Validate event against schema
ajv validate \
  -s schemas/customer/CustomerCreatedEvent.json \
  -d examples/customer-created-example.json
```

### IDE Integration

- **VS Code**: Install "JSON Schema Validator" extension
- **IntelliJ IDEA**: Built-in JSON Schema support
- **Eclipse**: Install "JSON Editor Plugin"

---

## Schema Registry Integration

### Kafka Schema Registry

```java
@Configuration
public class SchemaRegistryConfig {
    
    @Bean
    public SchemaRegistryClient schemaRegistryClient() {
        return new CachedSchemaRegistryClient(
            "http://schema-registry:8081",
            100
        );
    }
    
    @Bean
    public KafkaAvroSerializer avroSerializer() {
        return new KafkaAvroSerializer(schemaRegistryClient());
    }
}
```

### Schema Evolution Rules

1. **Forward Compatible**: New consumers can read old data
2. **Backward Compatible**: Old consumers can read new data
3. **Full Compatible**: Both forward and backward compatible

---

## Testing with Schemas

### Contract Testing

```java
@Test
void customer_created_event_should_match_schema() {
    // Given
    CustomerCreatedEvent event = createTestEvent();
    String eventJson = objectMapper.writeValueAsString(event);
    
    // When
    Set<ValidationMessage> errors = validateAgainstSchema(
        eventJson,
        "schemas/customer/CustomerCreatedEvent.json"
    );
    
    // Then
    assertThat(errors).isEmpty();
}
```

### Property-Based Testing

```java
@Property
void all_customer_events_should_have_valid_structure(
    @ForAll("customerEvents") CustomerCreatedEvent event
) {
    String eventJson = objectMapper.writeValueAsString(event);
    assertThat(validateAgainstSchema(eventJson)).isEmpty();
}
```

---

## Related Documentation

- **Event Catalog**: [../event-catalog.md](../event-catalog.md)
- **Event Documentation**: [../README.md](../README.md)
- **Domain Events Guide**: `.kiro/steering/domain-events.md`

---

## Contributing

### Adding New Schemas

1. Create schema file in appropriate context directory
2. Extend base `DomainEvent.json` schema
3. Add examples in `examples/` directory
4. Update this README with new schema reference
5. Run validation tests

### Schema Review Checklist

- [ ] Follows JSON Schema Draft 2020-12
- [ ] Extends base DomainEvent schema
- [ ] All required fields documented
- [ ] Field descriptions are clear
- [ ] Examples provided
- [ ] Validation tests pass
- [ ] Backward compatibility maintained

---

**Document Version**: 1.0  
**Last Updated**: 2025-10-25  
**Owner**: Architecture Team
