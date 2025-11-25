---
title: "Compute Infrastructure"
type: "deployment-viewpoint"
category: "infrastructure"
status: "active"
last_updated: "2025-10-23"
owner: "DevOps Team"
---

# Compute Infrastructure

> **Service**: Amazon EKS (Elastic Kubernetes Service)

## Cluster Configuration

**Cluster Details**:
- **Name**: `ecommerce-platform-prod`
- **Version**: Kubernetes 1.28
- **Region**: us-east-1
- **Availability Zones**: us-east-1a, us-east-1b, us-east-1c
- **Endpoint**: Private and public access enabled
- **Logging**: Control plane logging enabled (API, audit, authenticator, controller manager, scheduler)

**Control Plane**:
- **Managed by AWS**: Automatic updates and patching
- **High Availability**: Multi-AZ deployment by default
- **API Server**: Load balanced across multiple AZs
- **etcd**: Managed by AWS with automatic backups

## Node Groups

### General Purpose Node Group

**Configuration**:
```yaml
Name: general-purpose-nodes
Instance Type: t3.large
  - vCPU: 2
  - Memory: 8 GiB
  - Network: Up to 5 Gbps
  - EBS Bandwidth: Up to 2,780 Mbps

Capacity:
  Desired: 5 nodes
  Minimum: 3 nodes
  Maximum: 10 nodes

Disk:
  Type: gp3
  Size: 100 GB
  IOPS: 3000
  Throughput: 125 MB/s

AMI: Amazon EKS optimized Amazon Linux 2
```

**Use Cases**:
- Stateless microservices (Order, Customer, Product services)
- API gateways
- Background workers

### Memory Optimized Node Group

**Configuration**:
```yaml
Name: memory-optimized-nodes
Instance Type: r5.xlarge
  - vCPU: 4
  - Memory: 32 GiB
  - Network: Up to 10 Gbps
  - EBS Bandwidth: Up to 4,750 Mbps

Capacity:
  Desired: 3 nodes
  Minimum: 2 nodes
  Maximum: 5 nodes

Disk:
  Type: gp3
  Size: 200 GB
  IOPS: 3000
  Throughput: 125 MB/s

AMI: Amazon EKS optimized Amazon Linux 2
```

**Use Cases**:
- Data-intensive services (Analytics, Reporting)
- In-memory caching services
- Search and indexing services

## Namespace Organization

```yaml
# System namespaces
- kube-system          # Kubernetes system components
- kube-public          # Public cluster information
- kube-node-lease      # Node heartbeat data

# Application namespaces (by bounded context)
- customer-context     # Customer service and related components
- order-context        # Order service and related components
- product-context      # Product service and related components
- payment-context      # Payment service and related components
- inventory-context    # Inventory service and related components
- notification-context # Notification service and related components

# Infrastructure namespaces
- monitoring           # Prometheus, Grafana
- logging              # Fluentd, Elasticsearch
- ingress-nginx        # Ingress controller
```

## Auto-Scaling Configuration

**Cluster Autoscaler**:
- Scale up when pods are pending for > 30 seconds
- Scale down when node utilization < 50% for > 10 minutes
- Respect pod disruption budgets
- Never scale below minimum node count

**Horizontal Pod Autoscaler (HPA)**:
- Scales pods based on CPU/Memory utilization
- Custom metrics support (e.g., request rate)
