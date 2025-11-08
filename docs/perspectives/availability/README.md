# Availability & Resilience Perspective

> **Status**: 📝 To be documented  
> **Last Updated**: 2025-01-17  
> **Owner**: SRE Lead

## Overview

The Availability & Resilience Perspective ensures the system remains operational and recovers quickly from failures.

## Key Concerns

- High availability (99.9% uptime)
- Fault tolerance patterns
- Disaster recovery
- Graceful degradation

## Quality Attribute Scenarios

### Scenario 1: Database Failover

- **Source**: Database server
- **Stimulus**: Primary database fails
- **Environment**: Production during business hours
- **Artifact**: Customer data service
- **Response**: System fails over to secondary database
- **Response Measure**: RTO ≤ 5 minutes, RPO ≤ 1 minute

## Availability Targets

- **Uptime**: 99.9% (8.76 hours downtime/year)
- **RTO**: 5 minutes
- **RPO**: 1 minute

## Affected Viewpoints

- [Deployment Viewpoint](../../viewpoints/deployment/README.md) - Multi-AZ deployment
- [Operational Viewpoint](../../viewpoints/operational/README.md) - Monitoring and recovery
- [Concurrency Viewpoint](../../viewpoints/concurrency/README.md) - Retry mechanisms

## Quick Links

- [Back to All Perspectives](../README.md)
- [Main Documentation](../../README.md)
