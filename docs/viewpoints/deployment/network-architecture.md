---
title: "Network Architecture"
viewpoint: "Deployment"
status: "active"
last_updated: "2025-10-23"
stakeholders: ["Architects", "DevOps Engineers", "Security Team", "Network Engineers"]
---

# Network Architecture

> **Viewpoint**: Deployment  
> **Purpose**: Document the network topology, VPC configuration, and security controls  
> **Audience**: Architects, DevOps Engineers, Security Team, Network Engineers

## Overview

This document describes the network architecture of the E-Commerce Platform on AWS, including VPC design, subnet organization, routing, and security group configurations.

## VPC Configuration

### Primary VPC

**VPC Details**:

```yaml
Name: ecommerce-platform-prod-vpc
Region: us-east-1
CIDR Block: 10.0.0.0/16

  - Total IPs: 65,536
  - Usable IPs: 65,531

DNS Settings:
  DNS Resolution: Enabled
  DNS Hostnames: Enabled

Tenancy: Default
IPv6: Not enabled
```

**VPC Components**:

- **Internet Gateway**: For public subnet internet access
- **NAT Gateways**: 3 (one per AZ) for private subnet internet access
- **VPC Endpoints**: For AWS service access without internet
- **Flow Logs**: Enabled for network traffic monitoring

## Subnet Organization

### Subnet Strategy

The VPC is divided into **public** and **private** subnets across 3 Availability Zones for high availability.

```text
VPC: 10.0.0.0/16
├── Public Subnets (for internet-facing resources)
│   ├── us-east-1a: 10.0.1.0/24  (256 IPs)
│   ├── us-east-1b: 10.0.2.0/24  (256 IPs)
│   └── us-east-1c: 10.0.3.0/24  (256 IPs)
│
├── Private Subnets - Application (for EKS nodes)
│   ├── us-east-1a: 10.0.10.0/23 (512 IPs)
│   ├── us-east-1b: 10.0.12.0/23 (512 IPs)
│   └── us-east-1c: 10.0.14.0/23 (512 IPs)
│
├── Private Subnets - Database (for RDS, ElastiCache)
│   ├── us-east-1a: 10.0.20.0/24 (256 IPs)
│   ├── us-east-1b: 10.0.21.0/24 (256 IPs)
│   └── us-east-1c: 10.0.22.0/24 (256 IPs)
│
└── Private Subnets - Messaging (for MSK)
    ├── us-east-1a: 10.0.30.0/24 (256 IPs)
    ├── us-east-1b: 10.0.31.0/24 (256 IPs)
    └── us-east-1c: 10.0.32.0/24 (256 IPs)
```

### Public Subnets

**Purpose**: Host internet-facing resources

**Resources**:

- Application Load Balancer (ALB)
- NAT Gateways
- Bastion hosts (if needed)

**Configuration**:

```yaml
Public Subnet (us-east-1a):
  CIDR: 10.0.1.0/24
  Availability Zone: us-east-1a
  Auto-assign Public IP: Enabled
  Route Table: Public Route Table
  
Routes:

  - Destination: 0.0.0.0/0

    Target: Internet Gateway (igw-xxx)

  - Destination: 10.0.0.0/16

    Target: Local
```

### Private Subnets - Application

**Purpose**: Host EKS worker nodes and application pods

**Resources**:

- EKS worker nodes
- Application pods
- Internal load balancers

**Configuration**:

```yaml
Private Subnet - Application (us-east-1a):
  CIDR: 10.0.10.0/23
  Availability Zone: us-east-1a
  Auto-assign Public IP: Disabled
  Route Table: Private Route Table (AZ-1a)
  
Routes:

  - Destination: 0.0.0.0/0

    Target: NAT Gateway (nat-1a)

  - Destination: 10.0.0.0/16

    Target: Local

  - Destination: s3-prefix-list

    Target: VPC Endpoint (vpce-s3)
```

### Private Subnets - Database

**Purpose**: Host database and caching services

**Resources**:

- RDS instances (primary and replicas)
- ElastiCache Redis clusters

**Configuration**:

```yaml
Private Subnet - Database (us-east-1a):
  CIDR: 10.0.20.0/24
  Availability Zone: us-east-1a
  Auto-assign Public IP: Disabled
  Route Table: Private Route Table (Database)
  
Routes:

  - Destination: 10.0.0.0/16

    Target: Local
  # No internet access for security
```

### Private Subnets - Messaging

**Purpose**: Host Kafka brokers

**Resources**:

- MSK Kafka brokers
- Zookeeper nodes (managed by MSK)

**Configuration**:

```yaml
Private Subnet - Messaging (us-east-1a):
  CIDR: 10.0.30.0/24
  Availability Zone: us-east-1a
  Auto-assign Public IP: Disabled
  Route Table: Private Route Table (Messaging)
  
Routes:

  - Destination: 10.0.0.0/16

    Target: Local
  # No internet access for security
```

## Route Tables

### Public Route Table

**Associated Subnets**: All public subnets

**Routes**:

```yaml
Destination         Target              Purpose
10.0.0.0/16        local               VPC internal traffic
0.0.0.0/0          igw-xxx             Internet access
```

### Private Route Tables (per AZ)

**Private Route Table - AZ 1a**:

```yaml
Associated Subnets: 

  - Private-App-1a (10.0.10.0/23)

Routes:
Destination         Target              Purpose
10.0.0.0/16        local               VPC internal traffic
0.0.0.0/0          nat-1a              Internet access via NAT
s3-prefix-list     vpce-s3             S3 access via VPC endpoint
dynamodb-prefix    vpce-dynamodb       DynamoDB access via VPC endpoint
```

**Private Route Table - Database**:

```yaml
Associated Subnets:

  - Private-DB-1a (10.0.20.0/24)
  - Private-DB-1b (10.0.21.0/24)
  - Private-DB-1c (10.0.22.0/24)

Routes:
Destination         Target              Purpose
10.0.0.0/16        local               VPC internal traffic only
# No internet access for security
```

**Private Route Table - Messaging**:

```yaml
Associated Subnets:

  - Private-Msg-1a (10.0.30.0/24)
  - Private-Msg-1b (10.0.31.0/24)
  - Private-Msg-1c (10.0.32.0/24)

Routes:
Destination         Target              Purpose
10.0.0.0/16        local               VPC internal traffic only
# No internet access for security
```

## NAT Gateways

### Configuration

**NAT Gateway per AZ** (for high availability):

```yaml
NAT Gateway - AZ 1a:
  Name: ecommerce-nat-1a
  Subnet: Public-1a (10.0.1.0/24)
  Elastic IP: eipalloc-xxx (static public IP)
  
NAT Gateway - AZ 1b:
  Name: ecommerce-nat-1b
  Subnet: Public-1b (10.0.2.0/24)
  Elastic IP: eipalloc-yyy
  
NAT Gateway - AZ 1c:
  Name: ecommerce-nat-1c
  Subnet: Public-1c (10.0.3.0/24)
  Elastic IP: eipalloc-zzz
```

**Purpose**:

- Allow private subnet resources to access internet
- Download software updates and patches
- Access external APIs and services
- Each AZ uses its own NAT Gateway for fault isolation

## VPC Endpoints

### Gateway Endpoints

**S3 Endpoint**:

```yaml
Type: Gateway
Service: com.amazonaws.us-east-1.s3
Route Tables: All private route tables
Policy: Full access to specific S3 buckets

Purpose:

  - Access S3 without internet gateway
  - Reduce data transfer costs
  - Improve security

```

**DynamoDB Endpoint** (if used):

```yaml
Type: Gateway
Service: com.amazonaws.us-east-1.dynamodb
Route Tables: All private route tables
Policy: Full access
```

### Interface Endpoints

**ECR Endpoints** (for EKS):

```yaml
# ECR API Endpoint
Service: com.amazonaws.us-east-1.ecr.api
Subnets: Private-App subnets (all AZs)
Security Group: vpce-ecr-sg
Private DNS: Enabled

# ECR Docker Endpoint
Service: com.amazonaws.us-east-1.ecr.dkr
Subnets: Private-App subnets (all AZs)
Security Group: vpce-ecr-sg
Private DNS: Enabled
```

**CloudWatch Logs Endpoint**:

```yaml
Service: com.amazonaws.us-east-1.logs
Subnets: Private-App subnets (all AZs)
Security Group: vpce-logs-sg
Private DNS: Enabled

Purpose:

  - Send logs without internet access
  - Reduce data transfer costs

```

## Security Groups

### ALB Security Group

**Name**: `ecommerce-alb-sg`

**Purpose**: Control traffic to Application Load Balancer

**Inbound Rules**:

```yaml

- Protocol: TCP

  Port: 443 (HTTPS)
  Source: 0.0.0.0/0
  Description: Allow HTTPS from internet

- Protocol: TCP

  Port: 80 (HTTP)
  Source: 0.0.0.0/0
  Description: Allow HTTP (redirect to HTTPS)
```

**Outbound Rules**:

```yaml

- Protocol: TCP

  Port: 8080
  Destination: eks-node-sg
  Description: Forward to EKS nodes
```

### EKS Node Security Group

**Name**: `ecommerce-eks-node-sg`

**Purpose**: Control traffic to/from EKS worker nodes

**Inbound Rules**:

```yaml

- Protocol: TCP

  Port: 8080
  Source: alb-sg
  Description: Allow traffic from ALB

- Protocol: All

  Source: eks-node-sg
  Description: Allow pod-to-pod communication

- Protocol: TCP

  Port: 443
  Source: eks-control-plane-sg
  Description: Allow from EKS control plane

- Protocol: TCP

  Port: 10250
  Source: eks-control-plane-sg
  Description: Kubelet API
```

**Outbound Rules**:

```yaml

- Protocol: All

  Destination: 0.0.0.0/0
  Description: Allow all outbound (via NAT Gateway)
```

### RDS Security Group

**Name**: `ecommerce-rds-sg`

**Purpose**: Control access to RDS database

**Inbound Rules**:

```yaml

- Protocol: TCP

  Port: 5432 (PostgreSQL)
  Source: eks-node-sg
  Description: Allow from EKS nodes only

- Protocol: TCP

  Port: 5432
  Source: bastion-sg (if exists)
  Description: Allow from bastion for admin access
```

**Outbound Rules**:

```yaml

- Protocol: TCP

  Port: 5432
  Destination: rds-sg
  Description: Allow replication between RDS instances
```

### ElastiCache Security Group

**Name**: `ecommerce-redis-sg`

**Purpose**: Control access to Redis cluster

**Inbound Rules**:

```yaml

- Protocol: TCP

  Port: 6379 (Redis)
  Source: eks-node-sg
  Description: Allow from EKS nodes only
```

**Outbound Rules**:

```yaml

- Protocol: TCP

  Port: 6379
  Destination: redis-sg
  Description: Allow replication between Redis nodes
```

### MSK Security Group

**Name**: `ecommerce-msk-sg`

**Purpose**: Control access to Kafka brokers

**Inbound Rules**:

```yaml

- Protocol: TCP

  Port: 9092 (Kafka plaintext)
  Source: eks-node-sg
  Description: Allow from EKS nodes

- Protocol: TCP

  Port: 9094 (Kafka TLS)
  Source: eks-node-sg
  Description: Allow TLS from EKS nodes

- Protocol: TCP

  Port: 2181 (Zookeeper)
  Source: msk-sg
  Description: Allow Zookeeper communication
```

**Outbound Rules**:

```yaml

- Protocol: All

  Destination: msk-sg
  Description: Allow broker-to-broker communication
```

### VPC Endpoint Security Groups

**ECR Endpoint Security Group**:

```yaml
Name: ecommerce-vpce-ecr-sg

Inbound:

  - Protocol: TCP

    Port: 443
    Source: eks-node-sg
    Description: Allow HTTPS from EKS nodes

Outbound:

  - Protocol: TCP

    Port: 443
    Destination: 0.0.0.0/0
    Description: Allow to ECR service
```

## Network ACLs

### Default Network ACL

**Strategy**: Use Security Groups as primary security control, keep NACLs permissive

**Configuration**:

```yaml
Inbound Rules:

  - Rule: 100

    Protocol: All
    Port: All
    Source: 0.0.0.0/0
    Action: Allow

Outbound Rules:

  - Rule: 100

    Protocol: All
    Port: All
    Destination: 0.0.0.0/0
    Action: Allow
```

### Custom Network ACLs (Optional)

Can be implemented for additional security layer if needed:

```yaml
# Example: Database Subnet NACL
Inbound Rules:

  - Rule: 100

    Protocol: TCP
    Port: 5432
    Source: 10.0.10.0/23  # App subnet
    Action: Allow
  
  - Rule: 200

    Protocol: TCP
    Port: 5432
    Source: 10.0.12.0/23  # App subnet
    Action: Allow
  
  - Rule: *

    Protocol: All
    Port: All
    Source: 0.0.0.0/0
    Action: Deny

Outbound Rules:

  - Rule: 100

    Protocol: TCP
    Port: 1024-65535  # Ephemeral ports
    Destination: 10.0.0.0/16
    Action: Allow
  
  - Rule: *

    Protocol: All
    Port: All
    Destination: 0.0.0.0/0
    Action: Deny
```

## Network Flow

### Inbound Traffic Flow

```text
Internet
  ↓
Internet Gateway
  ↓
Application Load Balancer (Public Subnet)
  ↓ (Security Group: alb-sg → eks-node-sg)
EKS Ingress Controller (Private Subnet)
  ↓ (Pod Network)
Application Pods (Private Subnet)
  ↓ (Security Group: eks-node-sg → rds-sg)
RDS Database (Private Subnet - Database)
```

### Outbound Traffic Flow

```text
Application Pods (Private Subnet)
  ↓ (Security Group: eks-node-sg)
NAT Gateway (Public Subnet)
  ↓
Internet Gateway
  ↓
Internet
```

### Internal Service Communication

```text
Order Service Pod
  ↓ (Pod Network)
Kafka Producer
  ↓ (Security Group: eks-node-sg → msk-sg)
MSK Broker (Private Subnet - Messaging)
  ↓ (Kafka Replication)
Other MSK Brokers
  ↓ (Kafka Consumer)
Inventory Service Pod
```

## DNS Configuration

### Route 53 Configuration

**Hosted Zone**:

```yaml
Domain: ecommerce-platform.com
Type: Public Hosted Zone

Records:

  - Name: api.ecommerce-platform.com

    Type: A (Alias)
    Target: ALB DNS name
    Routing: Simple
  
  - Name: www.ecommerce-platform.com

    Type: A (Alias)
    Target: CloudFront distribution
    Routing: Simple
```

**Internal DNS**:

```yaml
# VPC DNS Resolution
Domain: internal.ecommerce-platform.com
Type: Private Hosted Zone
Associated VPCs: ecommerce-platform-prod-vpc

Records:

  - Name: rds.internal.ecommerce-platform.com

    Type: CNAME
    Target: RDS endpoint
  
  - Name: redis.internal.ecommerce-platform.com

    Type: CNAME
    Target: ElastiCache configuration endpoint
  
  - Name: kafka.internal.ecommerce-platform.com

    Type: CNAME
    Target: MSK bootstrap servers
```

## Network Monitoring

### VPC Flow Logs

**Configuration**:

```yaml
Log Destination: CloudWatch Logs
Log Group: /aws/vpc/ecommerce-platform-prod
Traffic Type: ALL (Accept and Reject)
Log Format: Default
Aggregation Interval: 1 minute

Retention: 7 days
```

**Use Cases**:

- Troubleshoot connectivity issues
- Monitor traffic patterns
- Detect security threats
- Compliance auditing

### CloudWatch Metrics

**Network Metrics**:

```yaml
Monitored Metrics:

  - NAT Gateway: BytesInFromSource, BytesOutToDestination
  - ALB: ActiveConnectionCount, TargetResponseTime
  - VPC Endpoints: BytesIn, BytesOut
  - Network Interfaces: NetworkIn, NetworkOut

Alarms:

  - NAT Gateway data transfer > 100 GB/hour
  - ALB unhealthy target count > 0
  - VPC endpoint errors > 10/minute

```

## Network Security Best Practices

### Implemented Controls

1. **Defense in Depth**:
   - Multiple security layers (Security Groups, NACLs, WAF)
   - Principle of least privilege for all rules
   - Regular security group audits

2. **Network Segmentation**:
   - Separate subnets for different tiers
   - No direct internet access for databases
   - Isolated messaging layer

3. **Encryption**:
   - TLS 1.2+ for all traffic
   - VPC endpoints for AWS service access
   - Encrypted EBS volumes

4. **Monitoring**:
   - VPC Flow Logs enabled
   - CloudWatch alarms for anomalies
   - Regular security assessments

## Related Documentation

- [Deployment Overview](overview.md) - High-level deployment architecture
- [Physical Architecture](physical-architecture.md) - Infrastructure component details
- [Security Perspective](../../perspectives/security/overview.md) - Security controls and policies

---

**Document Version**: 1.0  
**Last Updated**: 2025-10-23  
**Owner**: Network Engineering Team
