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

- Establish complete testing support tool infrastructure (data builders, scenario handlers, custom matchers)
- Refactor BDD step definitions, eliminate all conditional logic (if-else statements)
- Improve 3A structure of integration tests, split complex tests into independent test methods
- Establish test classification and tagging system (@UnitTest, @IntegrationTest, @SlowTest, @BddTest)
- Significantly improve test code readability, maintainability, and reliability

### [Architecture Optimization and DDD Layering Implementation - 2025-06-08](architecture-optimization-2025-06-08.md)

Main Content:

- Resolve interface layer directly depending on domain layer issues
- Adjust adapter package structure to correct positions
- Handle aggregate root inner class issues
- Implement strict DDD layered architecture

### [Promotion Module Implementation and Architecture Optimization - 2025-05-21](promotion-module-implementation-2025-05-21.md)

Main Content:

- Implement promotion functionality module for e-commerce platform
- Convenience store coupon system, limited-time specials, limited-quantity specials, etc.
- Architecture optimization, reclassifying Voucher from value object to entity
- Implement Specification interface for PromotionContext class

## Release Process

Each important update should create a new release note document in this directory, with naming format: `<topic>-<year>-<month>-<date>.md`.

Release notes should include the following content:

1. Business requirements overview
2. Technical implementation
3. Architectural changes
4. Technical details
5. Test coverage
6. Conclusion