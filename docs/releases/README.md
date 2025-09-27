# Release Notes

This directory contains release notes for various system versions, documenting important updates, architectural changes, and feature implementations for each release.

## Release History

### [Project Restructure and API Grouping Optimization - 2025-01-15](2025-01-15-project-restructure-and-api-grouping.md)

Main Content:

- Project file structure reorganization, organizing scattered root directory files into functional directories
- API grouping strategy redesign, intelligent grouping based on DDD and user roles
- OpenAPI tag optimization, using Chinese tags to improve user experience
- Docker containerization optimization, ARM64 native support and performance tuning
- Domain model enhancement, implementing complete DDD architecture and testing system

### [Test Code Quality Improvement and Refactoring - 2025-07-18](test-quality-improvement-2025-07-18.md)

Main Content:

- Established complete test utility infrastructure (data builders, scenario processors, custom matchers)
- Refactored BDD step definitions, eliminating all conditional logic (if-else statements)
- Improved integration test 3A structure, splitting complex tests into independent test methods
- Established test categorization and tagging system (@UnitTest, @IntegrationTest, @SlowTest, @BddTest)
- Significantly improved test code readability, maintainability, and reliability

### [Architecture Optimization and DDD Layering Implementation - 2025-06-08](architecture-optimization-2025-06-08.md)

Main Content:

- Resolved interface layer direct dependency on domain layer issues
- Adjusted adapter package structure to correct positions
- Handled aggregate root inner class issues
- Implemented strict DDD layered architecture

### [Promotion Module Implementation and Architecture Optimization - 2025-05-21](promotion-module-implementation-2025-05-21.md)

Main Content:

- Implemented e-commerce platform promotion functionality module
- Convenience store voucher system, flash sales, limited quantity sales features
- Architecture optimization, reclassifying Voucher from value object to entity
- Implemented PromotionContext class Specification interface

## Release Process

Each important update should create a new release note document in this directory, with naming format: `<topic>-<year>-<month>-<date>.md`.

Release notes should include the following content:

1. Business requirements overview
2. Technical implementation
3. Architectural changes
4. Technical details
5. Test coverage
6. Conclusion
