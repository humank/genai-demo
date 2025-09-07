Feature: Multi-Environment Database Configuration
  As a developer
  I want the application to automatically configure the correct database based on the active profile
  So that I can develop locally with H2 and deploy to production with PostgreSQL

  Background:
    Given the application is configured with profile-based database selection

  Scenario: Development profile uses H2 in-memory database
    Given the application is running with "dev" profile
    When the database configuration is initialized
    Then the system should use H2 in-memory database
    And the migration path should be "classpath:db/migration/h2"
    And the database should be accessible for development
    And H2 console should be enabled

  Scenario: Production profile uses PostgreSQL database
    Given the application is running with "production" profile
    When the database configuration is initialized
    Then the system should use PostgreSQL database
    And the migration path should be "classpath:db/migration/postgresql"
    And HikariCP connection pooling should be configured
    And H2 console should be disabled

  Scenario: Test profile uses H2 in-memory database
    Given the application is running with "test" profile
    When the database configuration is initialized
    Then the system should use H2 in-memory database
    And the migration path should be "classpath:db/migration/h2"
    And the database should support test isolation

  Scenario: Database connectivity validation succeeds
    Given the application is running with "dev" profile
    When the database configuration is validated
    Then the connectivity test should pass
    And the basic query execution should succeed
    And the database health status should be "UP"

  Scenario: Database migration paths are correctly configured
    Given the application is running with "dev" profile
    When Flyway migration is configured
    Then the migration location should point to H2-specific scripts
    And baseline on migrate should be enabled for development
    And clean should be allowed for development

  Scenario: Production database has security configurations
    Given the application is running with "production" profile
    When the database configuration is validated
    Then clean should be disabled for production safety
    And baseline on migrate should be disabled
    And connection pooling should be optimized for production

  Scenario: Database health monitoring provides detailed information
    Given the application is running with "dev" profile
    When the database health is checked
    Then the health status should include database type
    And the health status should include connection time
    And the health status should include query performance metrics
    And the health status should include migration information

  Scenario: Environment variable validation for production
    Given the application is running with "production" profile
    When the database configuration is validated
    Then DB_HOST environment variable should be required
    And DB_NAME environment variable should be required
    And DB_USERNAME environment variable should be required
    And invalid port numbers should be rejected

  Scenario: Flyway migration validation
    Given the application is running with "dev" profile
    When the migration status is checked
    Then pending migrations should be identified
    And failed migrations should be detected
    And current migration version should be reported

  Scenario: Database performance validation
    Given the application is running with "dev" profile
    When the database performance is validated
    Then query execution time should be measured
    And transaction performance should be tested
    And connection time should be within acceptable limits