Feature: Multi-Environment Integration Testing
  As a DevOps engineer
  I want to validate multi-environment configurations work correctly
  So that I can ensure consistent behavior across development, staging, and production

  Background:
    Given the multi-environment configuration system is active

  @integration @environment @profiles
  Scenario: Spring Boot profile-based configuration validation
    Given the application supports multiple Spring profiles
    When deployed with "dev" profile
    Then H2 in-memory database should be configured and accessible
    And InMemoryDomainEventPublisher should handle domain events
    And lightweight observability stack should be active
    And development-specific configurations should be applied
    When deployed with "production" profile
    Then PostgreSQL database connection should be established
    And KafkaDomainEventPublisher should publish events to MSK
    And full AWS observability stack should be operational
    And production security configurations should be enforced

  @integration @database @migration
  Scenario: Database configuration and migration across environments
    Given database migration system is configured
    When running in development environment
    Then H2 database should be initialized with development schema
    And Flyway should execute H2-specific migration scripts
    And database health checks should pass
    And sample data should be loaded for development
    When running in production environment
    Then PostgreSQL connection should be established with production settings
    And Flyway should execute PostgreSQL-specific migration scripts
    And database performance should meet production requirements
    And backup and recovery procedures should be validated

  @integration @events @messaging
  Scenario: Event publishing strategy validation across environments
    Given domain event publishing is configured per environment
    When a domain event is generated in development
    Then InMemoryDomainEventPublisher should process the event locally
    And event should be available for immediate testing and debugging
    And no external messaging infrastructure should be required
    When a domain event is generated in production
    Then KafkaDomainEventPublisher should publish to MSK cluster
    And event should be delivered with transactional consistency
    And retry mechanisms should handle temporary failures
    And dead letter queue should capture permanent failures

  @integration @infrastructure @cdk
  Scenario: AWS CDK infrastructure deployment validation
    Given AWS CDK infrastructure is defined and deployable
    When deploying to development environment
    Then lightweight infrastructure should be provisioned (t3.medium, single AZ)
    And development-specific resource configurations should be applied
    And cost optimization should prioritize development efficiency
    When deploying to production environment
    Then production-grade infrastructure should be provisioned (m6g.large, multi-AZ)
    And high availability and disaster recovery should be configured
    And security and compliance requirements should be enforced
    And monitoring and alerting should be comprehensive

  @integration @networking @security
  Scenario: Network and security configuration validation
    Given network security is configured per environment
    When infrastructure is deployed
    Then VPC should be created with appropriate CIDR ranges
    And security groups should enforce least privilege access
    And SSL/TLS certificates should be properly configured
    And WAF should protect production endpoints
    And network ACLs should restrict unauthorized access
    And VPC Flow Logs should capture network traffic for security analysis

  @integration @monitoring @alerting
  Scenario: Environment-specific monitoring and alerting
    Given monitoring is configured per environment
    When system is operational in development
    Then basic health checks should be active
    And development-friendly logging should be enabled
    And local monitoring tools should be accessible
    When system is operational in production
    Then comprehensive monitoring should be active across all components
    And production alerting should notify on-call teams
    And SLA monitoring should track performance against targets
    And automated remediation should handle common issues

  @integration @secrets @configuration
  Scenario: Secrets and configuration management validation
    Given secrets management is configured per environment
    When application starts in any environment
    Then environment-specific secrets should be loaded from AWS Secrets Manager
    And configuration should be validated for completeness and correctness
    And sensitive information should never be logged or exposed
    And configuration changes should be applied without service restart where possible
    And audit trails should track all configuration access and changes

  @integration @backup @recovery
  Scenario: Backup and recovery procedures validation
    Given backup and recovery is configured per environment
    When data is generated in the system
    Then automated backups should be created according to environment policy
    And backup integrity should be validated regularly
    And point-in-time recovery should be available for production
    And disaster recovery procedures should be tested monthly
    And recovery time objectives (RTO) should be met during tests
    And recovery point objectives (RPO) should be validated

  @integration @scaling @performance
  Scenario: Auto-scaling and performance validation
    Given auto-scaling is configured per environment
    When system load increases
    Then horizontal pod autoscaling should add replicas based on CPU/memory
    And database connections should scale appropriately
    And observability infrastructure should scale with application load
    And performance should remain within acceptable limits
    And cost should scale proportionally with usage
    And scaling events should be logged and monitored

  @integration @compliance @audit
  Scenario: Compliance and audit trail validation
    Given compliance requirements are configured
    When system operations are performed
    Then all API calls should be logged to CloudTrail
    And data access should be tracked and auditable
    And security events should be captured and analyzed
    And compliance reports should be generated automatically
    And audit logs should be tamper-proof and retained per policy
    And regulatory requirements should be continuously validated
