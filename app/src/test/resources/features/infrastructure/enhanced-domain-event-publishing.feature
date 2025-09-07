Feature: Enhanced Domain Event Publishing Strategy
  As a system architect
  I want profile-based domain event publishing
  So that I can use different event publishing strategies for development and production environments

  Background:
    Given the system is configured with enhanced domain event publishing

  Scenario: Development profile uses in-memory event publishing
    Given the system is running with "dev" profile
    When a domain event is published
    Then the event should be processed by InMemoryDomainEventPublisher
    And the event should be stored for testing purposes
    And the event should be published as Spring ApplicationEvent

  Scenario: Production profile uses Kafka event publishing
    Given the system is running with "production" profile
    When a domain event is published
    Then the event should be processed by KafkaDomainEventPublisher
    And the event should be sent to the appropriate Kafka topic
    And the event should include correlation ID for tracing

  Scenario: Transactional event publishing ensures consistency
    Given the system is configured for transactional event publishing
    When a domain event is published within a transaction
    Then the event should be processed after transaction commit
    And the event should not be processed if transaction rolls back

  Scenario: Retry mechanism handles temporary failures in production
    Given the system is running with "production" profile
    And Kafka is temporarily unavailable
    When a domain event is published
    Then the system should retry publishing with exponential backoff
    And the event should eventually be published when Kafka becomes available

  Scenario: Dead letter queue handles permanent failures
    Given the system is running with "production" profile
    And Kafka publishing fails permanently
    When a domain event is published
    And all retry attempts are exhausted
    Then the event should be sent to the dead letter queue
    And the failure should be logged for monitoring

  Scenario: Multiple events are published efficiently
    Given the system is configured with enhanced domain event publishing
    When multiple domain events are published together
    Then all events should be processed by the appropriate publisher
    And the events should maintain their order and consistency
    And the system should handle the batch efficiently

  Scenario: Event publishing handles null and empty inputs gracefully
    Given the system is configured with enhanced domain event publishing
    When null events or empty event lists are published
    Then the system should handle them gracefully without errors
    And no actual publishing should occur
    And appropriate warnings should be logged