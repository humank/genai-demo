# Release Notes

This directory contains release notes for various system versions, documenting important updates, architectural changes, and feature implementations for each release.

## Release History

### [Test Code Quality Improvement and Refactoring - 2025-07-18](test-quality-improvement-2025-07-18.md)

Main Content:

- Established complete test utility infrastructure (data builders, scenario handlers, custom matchers)
- Refactored BDD step definitions, eliminating all conditional logic (if-else statements)
- Improved 3A structure of integration tests, splitting complex tests into independent test methods
- Established test classification and tagging system (@UnitTest, @IntegrationTest, @SlowTest, @BddTest)
- Significantly improved test code readability, maintainability, and reliability

### [Architecture Optimization and DDD Layering Implementation - 2025-06-08](architecture-optimization-2025-06-08.md)

Main Content:

- Resolved interface layer directly depending on domain layer issue
- Adjusted adapter package structure to correct positions
- Handled aggregate root inner class issues
- Implemented strict DDD layered architecture

### [Promotion Module Implementation and Architecture Optimization - 2025-05-21](promotion-module-implementation-2025-05-21.md)

Main Content:

- Implemented promotion feature module for e-commerce platform
- Convenience store voucher system, limited-time specials, limited-quantity specials, and other features
- Architecture optimization, reclassifying Voucher from value object to entity
- Implemented Specification interface for PromotionContext class

## Release Process

Each important update should create a new release note document in this directory, with naming format: `<topic>-<year>-<month>-<date>.md`.

Release notes should include the following content:

1. Business requirements overview
2. Technical implementation
3. Architecture changes
4. Technical details
5. Test coverage
6. Conclusion