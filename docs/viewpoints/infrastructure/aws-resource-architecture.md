# Infrastructure Viewpoint - AWS Resource Architecture Design

**Document Version**: 1.0  
**Last Updated**: September 28, 2025 11:12 PM (Taipei Time)  
**Author**: Architecture Team  
**Status**: Active

## 📋 Table of Contents

- Overview
- Architecture Design Principles
- AWS Resource Architecture
- Network Architecture
- Compute Resources
- Data Storage
- Messaging Services
- Monitoring and Observability
- Cost Optimization
- Architecture Diagrams

## Overview

GenAI Demo adopts a Multi-Region Active-Active architecture deployed on AWS cloud platform, primarily serving users in Taiwan and the Asia-Pacific region. The system design follows the five pillars of the AWS Well-Architected Framework, ensuring high availability, security, performance, cost-effectiveness, and operational excellence.

### Core Design Objectives

- **High Availability**: 99.9% availability target with cross-region failover support
- **Scalability**: Support 10x traffic growth with auto-scaling mechanisms
- **Security**: Zero-trust architecture with end-to-end encryption
- **Cost Effectiveness**: Pay-as-you-go with resource optimization
- **Operational Excellence**: Automated deployment with comprehensive monitoring

## Architecture Design Principles

### 1. Cloud-Native Design

```yaml
Containerization: 
  - Applications: Docker containers
  - Orchestration: Amazon EKS (Kubernetes)
  - Images: Amazon ECR

Microservices Architecture:
  - Domain-Driven Design (DDD)
  - Event-driven architecture
  - API-First design

Serverless Services:
  - Lambda: Automation tasks
  - EventBridge: Event routing
  - Step Functions: Workflows
```

### 2. Multi-Region Architecture

```yaml
Primary Region: ap-east-2 (Taipei)
  - Purpose: Primary service region
  - Users: Taiwan, Hong Kong, Southeast Asia
  - Services: Complete service stack

Secondary Region: ap-northeast-1 (Tokyo)
  - Purpose: Disaster recovery, read replicas
  - Users: Japan, Korea
  - Services: Read services, failover
```

### 3. Infrastructure as Code (IaC)

```yaml
Tool: AWS CDK (TypeScript)
Version Control: Git
Deployment: CI/CD Pipeline
Testing: Unit tests + Integration tests
```

## AWS Resource Architecture

### Overall Architecture Overview

```mermaid
graph TB
    subgraph "Internet"
        User[Users]
        DNS[Route 53]
    end
    
    subgraph "ap-east-2 (Taipei) - Primary Region"
        subgraph "Network Layer"
            VPC1[VPC 10.0.0.0/16]
            ALB1[Application Load Balancer]
            NAT1[NAT Gateway]
        end
        
        subgraph "Compute Layer"
            EKS1[EKS Cluster]
            Nodes1[Worker Nodes]
            Pods1[Application Pods]
        end
        
        subgraph "Data Layer"
            Aurora1[Aurora Global - Writer]
            Redis1[ElastiCache Redis]
            MSK1[MSK Kafka]
            Glue1[Glue Data Catalog]
        end
        
        subgraph "Monitoring"
            CW1[CloudWatch]
            XRay1[X-Ray]
            Grafana1[Managed Grafana]
        end
    end
    
    subgraph "ap-northeast-1 (Tokyo) - Secondary Region"
        subgraph "Network Layer DR"
            VPC2[VPC 10.1.0.0/16]
            ALB2[Application Load Balancer]
        end
        
        subgraph "Compute Layer DR"
            EKS2[EKS Cluster]
            Nodes2[Worker Nodes]
        end
        
        subgraph "Data Layer DR"
            Aurora2[Aurora Global - Reader]
            Redis2[ElastiCache Redis]
            MSK2[MSK Kafka]
            Glue2[Glue Data Catalog DR]
        end
    end
    
    User --> DNS
    DNS --> ALB1
    DNS -.-> ALB2
    ALB1 --> EKS1
    ALB2 --> EKS2
    EKS1 --> Aurora1
    EKS2 --> Aurora2
    Aurora1 -.-> Aurora2
    Aurora1 --> Glue1
    Aurora2 --> Glue2
    Glue1 -.-> Glue2
    
    style VPC1 fill:#e1f5fe
    style VPC2 fill:#fff3e0
    style Aurora1 fill:#c8e6c9
    style Aurora2 fill:#ffcdd2
```

### CDK Stack Architecture

```mermaid
graph TD
    subgraph "CDK Application"
        App[CDK App]
        
        subgraph "Foundation Stacks"
            Network[NetworkStack]
            Security[SecurityStack]
        end
        
        subgraph "Core Stacks"
            EKS[EKSStack]
            RDS[RdsStack]
            Cache[ElastiCacheStack]
            MSK[MSKStack]
        end
        
        subgraph "Platform Stacks"
            Observability[ObservabilityStack]
            Alerting[AlertingStack]
            Core[CoreStack]
        end
        
        subgraph "Optional Stacks"
            Analytics[AnalyticsStack]
            DR[DisasterRecoveryStack]
        end
    end
    
    App --> Network
    App --> Security
    Network --> EKS
    Network --> RDS
    Network --> Cache
    Network --> MSK
    Security --> EKS
    Security --> RDS
    EKS --> Observability
    RDS --> Observability
    Observability --> Alerting
    Core --> Analytics
    
    style Network fill:#e3f2fd
    style Security fill:#fce4ec
    style EKS fill:#e8f5e8
    style RDS fill:#fff3e0
```

## Network Architecture

### VPC Design

```mermaid
graph TB
    subgraph "VPC 10.0.0.0/16"
        subgraph "Availability Zone A"
            PubA[Public Subnet<br/>10.0.0.0/24]
            PrivA[Private Subnet<br/>10.0.2.0/24]
            DBA[DB Subnet<br/>10.0.4.0/28]
        end
        
        subgraph "Availability Zone B"
            PubB[Public Subnet<br/>10.0.1.0/24]
            PrivB[Private Subnet<br/>10.0.3.0/24]
            DBB[DB Subnet<br/>10.0.5.0/28]
        end
        
        subgraph "Network Components"
            IGW[Internet Gateway]
            NAT[NAT Gateway]
            ALB[Application Load Balancer]
        end
        
        subgraph "Security Groups"
            ALBSG[ALB Security Group]
            AppSG[App Security Group]
            DBSG[DB Security Group]
        end
    end
    
    Internet --> IGW
    IGW --> PubA
    IGW --> PubB
    PubA --> NAT
    NAT --> PrivA
    NAT --> PrivB
    PubA --> ALB
    PubB --> ALB
    ALB --> PrivA
    ALB --> PrivB
    PrivA --> DBA
    PrivB --> DBB
    ALB -.-> ALBSG
    PrivA -.-> AppSG
    DBA -.-> DBSG
    
    style PubA fill:#e1f5fe
    style PubB fill:#e1f5fe
    style PrivA fill:#e8f5e8
    style PrivB fill:#e8f5e8
    style DBA fill:#fff3e0
    style DBB fill:#fff3e0
```

### Network Security Design

```yaml
Security Group Rules:
  ALB Security Group:
    Inbound:
      - Port 80 (HTTP): 0.0.0.0/0
      - Port 443 (HTTPS): 0.0.0.0/0
    Outbound:
      - Port 8080: App Security Group

  App Security Group:
    Inbound:
      - Port 8080: ALB Security Group
      - Port 22: Bastion Security Group (management)
    Outbound:
      - Port 443: 0.0.0.0/0 (AWS APIs)
      - Port 5432: DB Security Group
      - Port 6379: Redis Security Group
      - Port 9092: MSK Security Group

  DB Security Group:
    Inbound:
      - Port 5432: App Security Group
    Outbound: None
```

## Compute Resources

### Amazon EKS Architecture

```mermaid
graph TB
    subgraph "EKS Control Plane"
        API[Kubernetes API Server]
        ETCD[etcd]
        Scheduler[kube-scheduler]
        Controller[kube-controller-manager]
    end
    
    subgraph "EKS Data Plane"
        subgraph "Managed Node Group"
            Node1[Worker Node 1<br/>t3.medium]
            Node2[Worker Node 2<br/>t3.medium]
            Node3[Worker Node 3<br/>t3.large]
        end
        
        subgraph "Application Pods"
            Pod1[genai-demo-app-1]
            Pod2[genai-demo-app-2]
            Pod3[genai-demo-app-3]
        end
        
        subgraph "System Pods"
            DNS[CoreDNS]
            Proxy[kube-proxy]
            CNI[AWS VPC CNI]
        end
        
        subgraph "Add-ons"
            KEDA[KEDA Operator]
            HPA[HPA Controller]
            CA[Cluster Autoscaler]
            CSI[EBS CSI Driver]
        end
    end
    
    subgraph "AWS Services"
        ECR[Elastic Container Registry]
        ELB[Elastic Load Balancer]
        EBS[Elastic Block Store]
    end
    
    API --> Node1
    API --> Node2
    API --> Node3
    Node1 --> Pod1
    Node2 --> Pod2
    Node3 --> Pod3
    Pod1 --> ECR
    Pod2 --> ECR
    Pod3 --> ECR
    ELB --> Pod1
    ELB --> Pod2
    ELB --> Pod3
    Node1 --> EBS
    Node2 --> EBS
    Node3 --> EBS
    
    style API fill:#e3f2fd
    style Node1 fill:#e8f5e8
    style Node2 fill:#e8f5e8
    style Node3 fill:#e8f5e8
    style Pod1 fill:#fff3e0
    style Pod2 fill:#fff3e0
    style Pod3 fill:#fff3e0
```

### Compute Resource Configuration

```yaml
EKS Cluster:
  Version: Kubernetes 1.28
  Endpoint: Private
  Logging: API, Audit, Authenticator, ControllerManager, Scheduler

Managed Node Groups:
  Min Nodes: 2
  Max Nodes: 10
  Desired Nodes: 2-3 (environment dependent)
  Instance Types: 
    - t3.medium (development)
    - t3.large (production)
  AMI: Amazon Linux 2
  Disk: 20GB GP3

Pod Specifications:
  CPU Request: 100m
  CPU Limit: 500m
  Memory Request: 128Mi
  Memory Limit: 512Mi

Auto Scaling:
  HPA: CPU 70%, Memory 80%
  KEDA: Custom metrics (thread pool, queue length)
  Cluster Autoscaler: Node utilization 70%
```

## Data Storage

### Database Architecture (Aurora Global)

```mermaid
graph TB
    subgraph "ap-east-2 (Primary Region)"
        subgraph "Aurora Cluster Primary"
            Writer1[Writer Instance<br/>db.r6g.large]
            Reader1[Reader Instance<br/>db.r6g.large]
        end
        
        subgraph "Backup"
            Backup1[Automated Backup<br/>30-day retention]
            Snapshot1[Manual Snapshots]
        end
    end
    
    subgraph "ap-northeast-1 (Secondary Region)"
        subgraph "Aurora Cluster Secondary"
            Reader2[Reader Instance<br/>db.r6g.large]
            Reader3[Reader Instance<br/>db.r6g.medium]
        end
        
        subgraph "Backup"
            Backup2[Cross-Region Backup]
        end
    end
    
    subgraph "Applications"
        App1[EKS Pods - Taipei]
        App2[EKS Pods - Tokyo]
    end
    
    App1 --> Writer1
    App1 --> Reader1
    App2 --> Reader2
    App2 --> Reader3
    Writer1 -.-> Reader2
    Writer1 --> Backup1
    Backup1 -.-> Backup2
    
    style Writer1 fill:#c8e6c9
    style Reader1 fill:#e1f5fe
    style Reader2 fill:#e1f5fe
    style Reader3 fill:#e1f5fe
    style Backup1 fill:#fff3e0
    style Backup2 fill:#ffcdd2
```

### Data Storage Configuration

```yaml
Aurora PostgreSQL:
  Engine Version: 15.4
  Instance Types:
    Production: db.r6g.large
    Staging: db.r6g.medium
    Development: db.t3.medium
  Storage:
    Type: Aurora Storage
    Encryption: AES-256 (KMS)
    Auto Scaling: Enabled
  Backup:
    Automated Backup: 30 days (Production), 7 days (Development)
    Snapshots: Weekly manual snapshots
    Cross-Region Replication: Enabled (Production)
  Performance:
    Performance Insights: Enabled
    Enhanced Monitoring: Enabled

ElastiCache Redis:
  Version: 7.0
  Node Types:
    Production: cache.r6g.large
    Staging: cache.r6g.medium
    Development: cache.t3.micro
  Configuration:
    Replication Group: 3 nodes
    Multi-AZ: Enabled
    Automatic Failover: Enabled
  Security:
    Transit Encryption: TLS
    At-Rest Encryption: AES-256
    Authentication: AUTH token
```

## Messaging Services

### MSK Kafka Architecture

```mermaid
graph TB
    subgraph "MSK Cluster"
        subgraph "Broker Nodes"
            Broker1[Kafka Broker 1<br/>kafka.m5.large<br/>AZ-1]
            Broker2[Kafka Broker 2<br/>kafka.m5.large<br/>AZ-2]
            Broker3[Kafka Broker 3<br/>kafka.m5.large<br/>AZ-3]
        end
        
        subgraph "Zookeeper"
            ZK1[Zookeeper 1]
            ZK2[Zookeeper 2]
            ZK3[Zookeeper 3]
        end
        
        subgraph "Topics"
            Topic1[genai-demo-events-prod<br/>Partitions: 6<br/>Replication: 3]
            Topic2[genai-demo-deadletter<br/>Partitions: 3<br/>Replication: 3]
        end
    end
    
    subgraph "Producers"
        App1[Application Pods]
        Lambda1[Lambda Functions]
    end
    
    subgraph "Consumers"
        App2[Event Processors]
        Analytics[Analytics Pipeline]
    end
    
    Broker1 --> ZK1
    Broker2 --> ZK2
    Broker3 --> ZK3
    App1 --> Topic1
    Lambda1 --> Topic1
    Topic1 --> App2
    Topic1 --> Analytics
    Topic2 --> App2
    
    style Broker1 fill:#e8f5e8
    style Broker2 fill:#e8f5e8
    style Broker3 fill:#e8f5e8
    style Topic1 fill:#fff3e0
    style Topic2 fill:#ffcdd2
```

## Monitoring and Observability

### Monitoring Architecture

```mermaid
graph TB
    subgraph "Data Sources"
        App[Application Metrics]
        EKS[EKS Metrics]
        RDS[RDS Metrics]
        Redis[Redis Metrics]
        MSK[MSK Metrics]
        ALB[ALB Metrics]
    end
    
    subgraph "Collection Layer"
        CWAgent[CloudWatch Agent]
        ContainerInsights[Container Insights]
        XRay[X-Ray Daemon]
        Prometheus[Prometheus]
    end
    
    subgraph "Storage Layer"
        CloudWatch[CloudWatch Metrics]
        XRayService[X-Ray Service]
        LogGroups[CloudWatch Logs]
    end
    
    subgraph "Visualization Layer"
        CWDashboard[CloudWatch Dashboard]
        Grafana[Managed Grafana]
        XRayConsole[X-Ray Console]
    end
    
    subgraph "Alerting Layer"
        Alarms[CloudWatch Alarms]
        SNS[SNS Topics]
        Email[Email Notifications]
        Slack[Slack Integration]
    end
    
    App --> CWAgent
    EKS --> ContainerInsights
    App --> XRay
    EKS --> Prometheus
    CWAgent --> CloudWatch
    ContainerInsights --> CloudWatch
    XRay --> XRayService
    Prometheus --> CloudWatch
    CloudWatch --> CWDashboard
    CloudWatch --> Grafana
    XRayService --> XRayConsole
    CloudWatch --> Alarms
    Alarms --> SNS
    SNS --> Email
    SNS --> Slack
    
    style CloudWatch fill:#e1f5fe
    style Grafana fill:#e8f5e8
    style Alarms fill:#ffcdd2
```

## Cost Optimization

### Cost Structure Analysis

```mermaid
pie title Monthly Cost Distribution (Production)
    "EKS + EC2" : 35
    "RDS Aurora" : 25
    "ElastiCache" : 15
    "MSK" : 10
    "Data Transfer" : 8
    "CloudWatch" : 4
    "Other Services" : 3
```

### Cost Optimization Strategies

```yaml
Compute Resource Optimization:
  EKS:
    - Spot Instances: 30% of nodes use Spot
    - Auto Scaling: Based on actual load
    - Resource Request Optimization: Avoid over-provisioning
  Lambda:
    - ARM Graviton2: 20% cost savings
    - Memory Optimization: Based on actual usage

Storage Optimization:
  RDS:
    - Reserved Instances: 1-year term 40% savings
    - Storage Auto Scaling: Avoid over-provisioning
    - Read Replicas: Create only when needed
  S3:
    - Intelligent Tiering: Automatic cost optimization
    - Lifecycle Policies: Automatic archiving

Network Optimization:
  - CloudFront: Reduce Data Transfer costs
  - VPC Endpoints: Avoid NAT Gateway fees
  - Intra-Region Communication: Minimize cross-region traffic
```

## Architecture Diagrams

### Complete System Architecture Diagram

```mermaid
graph TB
    subgraph "User Layer"
        Web[Web Browser]
        Mobile[Mobile App]
        API_Client[API Client]
    end
    
    subgraph "CDN & DNS"
        CF[CloudFront]
        R53[Route 53]
    end
    
    subgraph "ap-east-2 (Primary Region)"
        subgraph "Network Layer"
            VPC1[VPC]
            ALB1[ALB]
            NAT1[NAT Gateway]
        end
        
        subgraph "Application Layer"
            EKS1[EKS Cluster]
            subgraph "Pods"
                App1[genai-demo-app]
                Sidecar1[X-Ray Sidecar]
            end
        end
        
        subgraph "Data Layer"
            Aurora1[Aurora Writer]
            Redis1[Redis Primary]
            MSK1[MSK Cluster]
        end
        
        subgraph "Monitoring Layer"
            CW1[CloudWatch]
            XRay1[X-Ray]
        end
    end
    
    subgraph "ap-northeast-1 (Disaster Recovery)"
        VPC2[VPC]
        ALB2[ALB]
        EKS2[EKS Cluster]
        Aurora2[Aurora Reader]
        Redis2[Redis Replica]
    end
    
    subgraph "Global Services"
        IAM[IAM]
        KMS[KMS]
        Secrets[Secrets Manager]
        ECR[ECR]
    end
    
    Web --> CF
    Mobile --> CF
    API_Client --> R53
    CF --> R53
    R53 --> ALB1
    R53 -.-> ALB2
    ALB1 --> EKS1
    EKS1 --> App1
    App1 --> Sidecar1
    App1 --> Aurora1
    App1 --> Redis1
    App1 --> MSK1
    App1 --> CW1
    Sidecar1 --> XRay1
    Aurora1 -.-> Aurora2
    Redis1 -.-> Redis2
    App1 --> IAM
    App1 --> KMS
    App1 --> Secrets
    EKS1 --> ECR
    
    style Web fill:#e3f2fd
    style EKS1 fill:#e8f5e8
    style Aurora1 fill:#c8e6c9
    style Redis1 fill:#fff3e0
    style CW1 fill:#e1f5fe
```

---

**Document Status**: ✅ Complete  
**Next Step**: Review [Security Viewpoint](../security/iam-permissions-architecture.md) for IAM permissions architecture  
**Related Documents**: 
- [Security Viewpoint - IAM Permissions Architecture](../security/iam-permissions-architecture.md)
- [Deployment Viewpoint - Deployment Architecture](../deployment/deployment-architecture.md)
- [Operational Viewpoint - DNS Resolution and Disaster Recovery](../operational/dns-disaster-recovery.md)