# Context Viewpoint

> **Status**: 📝 To be documented  
> **Last Updated**: 2025-01-17  
> **Owner**: Architect / Product Manager

## Overview

The Context Viewpoint describes the system's relationships with its environment, including external systems, stakeholders, and constraints.

## Purpose

This viewpoint answers:

- What are the system boundaries?
- What external systems does it interact with?
- Who are the stakeholders?
- What are the external constraints?

## Stakeholders

- **Primary**: Business analysts, architects
- **Secondary**: Compliance officers, product managers

## Contents

### 📄 Documents

- [Overview](overview.md) - System context and boundaries
- [Scope & Boundaries](scope-and-boundaries.md) - What's in scope and out of scope
- [External Systems](external-systems.md) - Third-party integrations
- [Stakeholders](stakeholders.md) - Stakeholder map and concerns

### 📊 Diagrams

- System context diagram
- External integrations diagram
- Stakeholder map

## Key Concepts

### System Scope

**In Scope**:

- Customer management
- Order processing
- Payment processing
- Inventory management
- Product catalog
- Promotions and pricing

**Out of Scope**:

- Warehouse management (external system)
- Shipping logistics (third-party)
- Accounting (separate system)

### External Systems

- **Payment Gateway**: Stripe
- **Email Service**: SendGrid
- **Shipping Provider**: FedEx
- **Analytics**: Google Analytics

### Stakeholders

- **Business**: Product owners, marketing team, finance team
- **Technical**: Development team, operations team, security team
- **External**: Customers, partners, regulators

## Related Documentation

### Related Viewpoints

- [Functional Viewpoint](../functional/README.md) - Internal capabilities
- [Deployment Viewpoint](../deployment/README.md) - External service connections

### Related Perspectives

- [Security Perspective](../../perspectives/security/README.md) - External system security
- [Location Perspective](../../perspectives/location/README.md) - Geographic constraints

### Related Guides

- [API Integration Guide](../../api/integration/README.md)

## Quick Links

- [Back to All Viewpoints](../README.md)
- [Main Documentation](../../README.md)
