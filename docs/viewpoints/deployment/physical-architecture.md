---
title: "Physical Architecture"
viewpoint: "Deployment"
status: "active"
last_updated: "2025-11-25"
stakeholders: ["Architects", "DevOps Engineers", "Operations Team"]
---

# Physical Architecture

> **Viewpoint**: Deployment
> **Purpose**: Document the detailed configuration of physical infrastructure components
> **Audience**: Architects, DevOps Engineers, Operations Team

## Overview

This document serves as an index for the detailed specifications of the E-Commerce Platform's physical infrastructure. The infrastructure is built on AWS and organized into compute, database, caching, and messaging layers.

![AWS Infrastructure](../../diagrams/generated/deployment/aws-infrastructure.png)

*Figure 1: AWS infrastructure architecture showing multi-AZ deployment across us-east-1*

## Infrastructure Components

### [Compute Infrastructure](infrastructure/compute.md)

Detailed configuration for the Kubernetes (EKS) clusters, including:
- Control Plane configuration
- Node Group specifications (General Purpose & Memory Optimized)
- Namespace organization
- Pod resource quotas and limits
- Auto-scaling policies (Cluster Autoscaler & HPA)

[View Compute Details](infrastructure/compute.md)

### [Database Infrastructure](infrastructure/database.md)

Specifications for the relational database layer (Amazon RDS), including:
- Primary instance configuration (PostgreSQL)
- Read replica setup for scalability
- Storage configuration and IOPS
- Backup strategies and maintenance windows
- Database parameter tuning

[View Database Details](infrastructure/database.md)

### [Cache Infrastructure](infrastructure/cache.md)

Configuration for the distributed caching layer (Amazon ElastiCache Redis), including:
- Cluster mode and sharding configuration
- Node types and capacity
- Redis parameter tuning
- Persistence settings (AOF)
- Backup and maintenance policies

[View Cache Details](infrastructure/cache.md)

### [Messaging Infrastructure](infrastructure/messaging.md)

Details for the event streaming platform (Amazon MSK / Kafka), including:
- Broker configuration and sizing
- Topic specifications (partitions, replication)
- Storage and retention policies
- Security settings (Encryption, ACLs)
- Monitoring and alerting rules

[View Messaging Details](infrastructure/messaging.md)

## Resource Summary

### Compute Resources

| Component | Instance Type | Count | vCPU | Memory | Storage |
|-----------|--------------|-------|------|--------|---------|
| EKS General Nodes | t3.large | 3-10 | 6-20 | 24-80 GB | 300-1000 GB |
| EKS Memory Nodes | r5.xlarge | 2-5 | 8-20 | 64-160 GB | 400-1000 GB |
| RDS Primary | db.r5.xlarge | 1 | 4 | 32 GB | 500-2000 GB |
| RDS Replicas | db.r5.large | 2 | 4 | 32 GB | 1000 GB |
| Redis Nodes | cache.r5.large | 6 | 12 | 78 GB | N/A |
| Kafka Brokers | kafka.m5.large | 3 | 6 | 24 GB | 3000 GB |

### Network Bandwidth

| Component | Network Performance |
|-----------|-------------------|
| EKS Nodes (t3.large) | Up to 5 Gbps |
| EKS Nodes (r5.xlarge) | Up to 10 Gbps |
| RDS (db.r5.xlarge) | Up to 10 Gbps |
| Redis (cache.r5.large) | Up to 10 Gbps |
| Kafka (kafka.m5.large) | Up to 10 Gbps |

### Cost Estimate (Monthly)

| Component | Configuration | Estimated Cost |
|-----------|--------------|----------------|
| EKS Control Plane | 1 cluster | $150 |
| EKS Nodes (General) | 5 x t3.large | $400 |
| EKS Nodes (Memory) | 3 x r5.xlarge | $600 |
| RDS Primary | 1 x db.r5.xlarge | $600 |
| RDS Replicas | 2 x db.r5.large | $600 |
| ElastiCache | 6 x cache.r5.large | $900 |
| MSK | 3 x kafka.m5.large | $600 |
| EBS Storage | ~5 TB | $500 |
| Data Transfer | Estimated | $200 |
| **Total** | | **~$4,550** |

*Note: Costs are estimates and may vary based on actual usage, region, and AWS pricing changes.*

## Related Documentation

- [Deployment Overview](overview.md) - High-level deployment architecture
- [Network Architecture](network-architecture.md) - VPC and network configuration
- [Deployment Process](deployment-process.md) - CI/CD and deployment procedures

---

**Document Version**: 2.0
**Last Updated**: 2025-11-25
**Owner**: DevOps Team
