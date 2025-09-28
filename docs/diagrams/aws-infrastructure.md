# AWS Infrastructure Architecture

This document shows the complete AWS infrastructure architecture for the GenAI Demo project, including all CDK stacks, services, and environment configurations.

## AWS Infrastructure Overview

```mermaid
graph TB
    subgraph "AWS CDK Stacks" as CDK
        MAIN_STACK[Main Infrastructure Stack]
        OBS_STACK[Observability Stack]
        DATA_STACK[Data Analytics Stack]
    end
    
    subgraph "Networking & Security" as NETWORK
        VPC[VPC<br/>Multi-AZ Setup]
        ALB[Application Load Balancer<br/>SSL Termination]
        WAF[AWS WAF<br/>Security Rules]
        SG[Security Groups<br/>Network ACLs]
    end
    
    subgraph "Container Platform" as CONTAINER
        EKS[Amazon EKS<br/>Kubernetes 1.28]
        ECR[Amazon ECR<br/>Container Registry]
        FARGATE[Fargate Nodes<br/>Serverless Compute]
        GRAVITON[Graviton3 Nodes<br/>ARM64 Instances]
    end
    
    subgraph "Data Services" as DATA
        RDS[RDS PostgreSQL<br/>Multi-AZ, Encrypted]
        MSK[Amazon MSK<br/>Kafka Cluster]
        REDIS[ElastiCache Redis<br/>Session & Cache]
        S3[S3 Buckets<br/>Data Lake & Backups]
    end
    
    subgraph "Observability Services" as OBS
        CW[CloudWatch<br/>Logs & Metrics]
        XRAY[AWS X-Ray<br/>Distributed Tracing]
        OPENSEARCH[OpenSearch Service<br/>Log Analytics]
        PROMETHEUS[Managed Prometheus<br/>Metrics Storage]
    end
    
    subgraph "Analytics & BI" as ANALYTICS
        FIREHOSE[Kinesis Data Firehose<br/>Data Streaming]
        GLUE[AWS Glue<br/>Data Catalog & ETL]
        ATHENA[Amazon Athena<br/>Serverless Queries]
        QUICKSIGHT[QuickSight<br/>Business Intelligence]
    end
    
    subgraph "Configuration & Secrets" as CONFIG
        SSM[Systems Manager<br/>Parameter Store]
        SECRETS[Secrets Manager<br/>Database Credentials]
        IAM[IAM Roles & Policies<br/>IRSA for EKS]
    end
    
    subgraph "Monitoring & Alerting" as MONITORING
        SNS[SNS Topics<br/>Alert Notifications]
        LAMBDA[Lambda Functions<br/>Custom Metrics & Automation]
        EVENTBRIDGE[EventBridge<br/>Event Routing]
    end
    
    %% CDK Deployment Flow
    CDK -->|Deploys| NETWORK
    CDK -->|Deploys| CONTAINER
    CDK -->|Deploys| DATA
    CDK -->|Deploys| OBS
    CDK -->|Deploys| ANALYTICS
    CDK -->|Deploys| CONFIG
    CDK -->|Deploys| MONITORING
    
    %% Network Flow
    ALB -->|Routes to| EKS
    WAF -->|Protects| ALB
    VPC -->|Contains| EKS
    VPC -->|Contains| RDS
    VPC -->|Contains| MSK
    VPC -->|Contains| REDIS
    
    %% Container Platform
    EKS -->|Pulls images| ECR
    EKS -->|Runs on| FARGATE
    EKS -->|Runs on| GRAVITON
    
    %% Data Flow
    EKS -->|Connects to| RDS
    EKS -->|Publishes to| MSK
    EKS -->|Caches in| REDIS
    MSK -->|Streams to| FIREHOSE
    
    %% Observability Flow
    EKS -->|Logs to| CW
    EKS -->|Traces to| XRAY
    CW -->|Streams to| OPENSEARCH
    CW -->|Archives to| S3
    
    %% Analytics Flow
    FIREHOSE -->|Stores in| S3
    S3 -->|Catalogs with| GLUE
    GLUE -->|Queries via| ATHENA
    ATHENA -->|Visualizes in| QUICKSIGHT
    OPENSEARCH -->|Dashboards in| QUICKSIGHT
    
    %% Configuration
    EKS -->|Reads config| SSM
    EKS -->|Gets secrets| SECRETS
    IAM -->|Authorizes| EKS
    
    %% Monitoring
    CW -->|Triggers| SNS
    LAMBDA -->|Processes| EVENTBRIDGE
    EVENTBRIDGE -->|Routes events| SNS
    
    %% Environment-specific Resources
    subgraph "Development Environment" as DEV_ENV
        DEV_EKS[EKS Dev Cluster<br/>t3.medium nodes]
        DEV_RDS[RDS Dev Instance<br/>db.t3.micro]
        DEV_REDIS[ElastiCache Dev<br/>cache.t3.micro]
    end
    
    subgraph "Production Environment" as PROD_ENV
        PROD_EKS[EKS Prod Cluster<br/>m6g.large nodes]
        PROD_RDS[RDS Prod Cluster<br/>Multi-AZ, Read Replicas]
        PROD_MSK[MSK Prod Cluster<br/>3 brokers, Multi-AZ]
        PROD_REDIS[ElastiCache Prod<br/>Cluster mode, Multi-AZ]
    end
    
    CDK -.->|Profile: dev| DEV_ENV
    CDK -.->|Profile: production| PROD_ENV
    
    classDef cdk fill:#ff9800,stroke:#e65100,stroke-width:3px
    classDef network fill:#2196f3,stroke:#0d47a1,stroke-width:2px
    classDef container fill:#4caf50,stroke:#1b5e20,stroke-width:2px
    classDef data fill:#9c27b0,stroke:#4a148c,stroke-width:2px
    classDef observability fill:#ff5722,stroke:#bf360c,stroke-width:2px
    classDef analytics fill:#607d8b,stroke:#263238,stroke-width:2px
    classDef config fill:#795548,stroke:#3e2723,stroke-width:2px
    classDef monitoring fill:#e91e63,stroke:#880e4f,stroke-width:2px
    classDef environment fill:#8bc34a,stroke:#33691e,stroke-width:2px
    
    class CDK cdk
    class VPC,ALB,WAF,SG network
    class EKS,ECR,FARGATE,GRAVITON container
    class RDS,MSK,REDIS,S3 data
    class CW,XRAY,OPENSEARCH,PROMETHEUS observability
    class FIREHOSE,GLUE,ATHENA,QUICKSIGHT analytics
    class SSM,SECRETS,IAM config
    class SNS,LAMBDA,EVENTBRIDGE monitoring
    class DEV_EKS,DEV_RDS,DEV_REDIS,PROD_EKS,PROD_RDS,PROD_MSK,PROD_REDIS environment
```

## Architecture Description

### CDK Stack Structure

1. **Main Infrastructure Stack**: Core infrastructure including networking, container platform, and data services
2. **Observability Stack**: Observability services including monitoring, logging, and tracing
3. **Data Analytics Stack**: Data analytics services including data lake and business intelligence

### Core Service Components

#### Networking and Security Layer
- **VPC**: Multi-AZ setup providing network isolation
- **Application Load Balancer**: SSL termination and traffic routing
- **AWS WAF**: Web application firewall providing security rules
- **Security Groups**: Network access control lists

#### Container Platform
- **Amazon EKS**: Kubernetes 1.28 cluster management
- **Amazon ECR**: Container image registry
- **Fargate**: Serverless compute nodes
- **Graviton3**: ARM64 high-performance instances

#### Data Services
- **RDS PostgreSQL**: Multi-AZ, encrypted relational database
- **Amazon MSK**: Kafka cluster for event streaming
- **ElastiCache Redis**: Session and cache storage
- **S3**: Data lake and backup storage

### Environment Configuration

#### Development Environment
- **EKS**: t3.medium nodes, suitable for development and testing
- **RDS**: db.t3.micro instance, cost-optimized
- **Redis**: cache.t3.micro, basic caching needs

#### Production Environment
- **EKS**: m6g.large nodes, high-performance ARM64 instances
- **RDS**: Multi-AZ cluster with read replicas
- **MSK**: 3 brokers, multi-AZ setup
- **Redis**: Cluster mode, multi-AZ high availability

## Related Documentation

- **[Deployment Viewpoint](../viewpoints/deployment/README.md)** - Deployment strategies and environment management
- **[Infrastructure as Code](../viewpoints/deployment/infrastructure-as-code.md)** - AWS CDK practices
- **[Observability Deployment](../viewpoints/deployment/observability-deployment.md)** - Monitoring system deployment

## Static Diagrams

If the above Mermaid diagram cannot be displayed properly, please view the static SVG version:

![AWS Infrastructure Architecture](aws_infrastructure.svg)

---

**Maintenance Note**: This diagram is automatically updated as the infrastructure evolves, ensuring it reflects the latest AWS architecture state.