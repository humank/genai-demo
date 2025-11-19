# Information Viewpoint

> **Status**: 📝 To be documented  
> **Last Updated**: 2025-01-17  
> **Owner**: Data Architect

## Overview

The Information Viewpoint describes how the system stores, manages, and distributes information across bounded contexts.

## Purpose

This viewpoint answers:

- What data does the system manage?
- How is data structured and related?
- Who owns which data?
- How does data flow through the system?

## Stakeholders

- **Primary**: Data architects, database administrators
- **Secondary**: Developers, architects

## Contents

### 📄 Documents

- [Overview](overview.md) - Data management approach
- [Domain Models](domain-models.md) - Entity relationships by bounded context
- [Data Ownership](data-ownership.md) - Data ownership and boundaries
- [Data Flow](data-flow.md) - How data moves between contexts

### 📊 Diagrams

- Entity-relationship diagrams for each bounded context
- Data flow diagrams
- Event flow diagrams

## Key Concepts

### Data Ownership

- Each bounded context owns its data
- Cross-context data access via domain events
- Eventual consistency between contexts

### Data Storage

- **Primary Database**: PostgreSQL (production)
- **Development/Test**: H2 in-memory database
- **Caching**: Redis for distributed caching

## Related Documentation

### Related Viewpoints

- [Functional Viewpoint](../functional/README.md) - Bounded contexts that own data
- [Concurrency Viewpoint](../concurrency/README.md) - Data consistency strategies

### Related Perspectives

- [Security Perspective](../../perspectives/security/README.md) - Data encryption and protection
- [Performance Perspective](../../perspectives/performance/README.md) - Database optimization

## Quick Links

- [Back to All Viewpoints](../README.md)
- [Main Documentation](../../README.md)

---

**Note**: This viewpoint is currently being documented.
