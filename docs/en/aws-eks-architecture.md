# AWS EKS Native Microservices Architecture

## 🏗️ Overall Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    AWS Cloud                                │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐    ┌──────────────────────────────────┐ │
│  │   Route 53      │    │        CloudFront                │ │
│  │   (DNS)         │    │        (CDN)                     │ │
│  └─────────────────┘    └──────────────────────────────────┘ │
│           │                           │                      │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │              Application Load Balancer                  │ │
│  │              (ALB with WAF)                             │ │
│  └─────────────────────────────────────────────────────────┘ │
│           │                                                  │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │                    EKS Cluster                          │ │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐ │ │
│  │  │Order Service│ │Payment Svc  │ │Inventory Service    │ │ │
│  │  └─────────────┘ └─────────────┘ └─────────────────────┘ │ │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐ │ │
│  │  │Pricing Svc  │ │Delivery Svc │ │Notification Service │ │ │
│  │  └─────────────┘ └─────────────┘ └─────────────────────┘ │ │
│  └─────────────────────────────────────────────────────────┘ │
│           │                                                  │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │                 Data & Messaging Layer                  │ │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐ │ │
│  │  │   RDS       │ │   MSK       │ │    ElastiCache      │ │ │
│  │  │(PostgreSQL) │ │  (Kafka)    │ │     (Redis)         │ │ │
│  │  └─────────────┘ └─────────────┘ └─────────────────────┘ │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                              │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │              Supporting Services                         │ │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐ │ │
│  │  │Parameter    │ │   ECR       │ │    CloudWatch       │ │ │
│  │  │Store        │ │(Container   │ │   (Monitoring)      │ │ │
│  │  │(Config)     │ │Registry)    │ │                     │ │ │
│  │  └─────────────┘ └─────────────┘ └─────────────────────┘ │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## 🔧 Core AWS Service Selection

### 1. **API Gateway Alternative: Application Load Balancer (ALB)**

**Why ALB over API Gateway:**
- **Cost Efficiency**: ALB is significantly cheaper for high-traffic applications
- **Native EKS Integration**: Seamless integration with EKS through AWS Load Balancer Controller
- **Advanced Routing**: Path-based and host-based routing capabilities
- **WebSocket Support**: Native support for real-time communications

```yaml
# ALB Ingress Configuration
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: microservices-ingress
  annotations:
    kubernetes.io/ingress.class: alb
    alb.ingress.kubernetes.io/scheme: internet-facing
    alb.ingress.kubernetes.io/target-type: ip
    alb.ingress.kubernetes.io/wafv2-acl-arn: arn:aws:wafv2:region:account:webacl/name
spec:
  rules:
  - host: api.example.com
    http:
      paths:
      - path: /api/orders
        pathType: Prefix
        backend:
          service:
            name: order-service
            port:
              number: 8080
      - path: /api/payments
        pathType: Prefix
        backend:
          service:
            name: payment-service
            port:
              number: 8080
```

### 2. **Container Orchestration: Amazon EKS**

**EKS Advantages:**
- **Managed Control Plane**: AWS manages Kubernetes master nodes
- **AWS Integration**: Native integration with AWS services
- **Security**: IAM integration and Pod Security Standards
- **Scalability**: Auto Scaling Groups and Horizontal Pod Autoscaler

```yaml
# EKS Cluster Configuration
apiVersion: eksctl.io/v1alpha5
kind: ClusterConfig

metadata:
  name: microservices-cluster
  region: us-west-2
  version: "1.28"

nodeGroups:
  - name: worker-nodes
    instanceType: t3.medium
    desiredCapacity: 3
    minSize: 1
    maxSize: 10
    volumeSize: 20
    ssh:
      allow: true
    iam:
      withAddonPolicies:
        autoScaler: true
        cloudWatch: true
        ebs: true
        efs: true
        albIngress: true

addons:
  - name: vpc-cni
  - name: coredns
  - name: kube-proxy
  - name: aws-ebs-csi-driver
```

### 3. **Data Layer: Amazon RDS + Amazon MSK + ElastiCache**

#### **Amazon RDS (PostgreSQL)**
- **Multi-AZ Deployment**: High availability with automatic failover
- **Read Replicas**: Scale read operations
- **Automated Backups**: Point-in-time recovery

```yaml
# RDS Configuration (Terraform)
resource "aws_db_instance" "microservices_db" {
  identifier = "microservices-db"
  engine     = "postgres"
  engine_version = "15.4"
  instance_class = "db.t3.micro"
  allocated_storage = 20
  storage_encrypted = true
  
  db_name  = "microservices"
  username = "admin"
  password = var.db_password
  
  vpc_security_group_ids = [aws_security_group.rds.id]
  db_subnet_group_name   = aws_db_subnet_group.main.name
  
  backup_retention_period = 7
  backup_window          = "03:00-04:00"
  maintenance_window     = "sun:04:00-sun:05:00"
  
  skip_final_snapshot = true
  deletion_protection = false
  
  tags = {
    Name = "microservices-database"
  }
}
```

#### **Amazon MSK (Managed Kafka)**
- **Event Streaming**: Reliable event-driven communication
- **Managed Service**: AWS handles Kafka cluster management
- **Integration**: Native integration with EKS and other AWS services

```yaml
# MSK Configuration
resource "aws_msk_cluster" "microservices_kafka" {
  cluster_name           = "microservices-kafka"
  kafka_version          = "2.8.1"
  number_of_broker_nodes = 3

  broker_node_group_info {
    instance_type   = "kafka.t3.small"
    ebs_volume_size = 20
    client_subnets = [
      aws_subnet.private_a.id,
      aws_subnet.private_b.id,
      aws_subnet.private_c.id,
    ]
    security_groups = [aws_security_group.msk.id]
  }

  encryption_info {
    encryption_at_rest_kms_key_id = aws_kms_key.msk.arn
    encryption_in_transit {
      client_broker = "TLS"
      in_cluster    = true
    }
  }

  logging_info {
    broker_logs {
      cloudwatch_logs {
        enabled   = true
        log_group = aws_cloudwatch_log_group.msk.name
      }
    }
  }
}
```

#### **ElastiCache (Redis)**
- **Caching Layer**: Improve application performance
- **Session Storage**: Distributed session management
- **Real-time Analytics**: Fast data access patterns

### 4. **Service Mesh: AWS App Mesh (Optional)**

For advanced microservices communication:

```yaml
# App Mesh Virtual Service
apiVersion: appmesh.k8s.aws/v1beta2
kind: VirtualService
metadata:
  name: order-service
  namespace: microservices
spec:
  awsName: order-service
  provider:
    virtualRouter:
      virtualRouterRef:
        name: order-service-router
```

## 🚀 Microservices Deployment Strategy

### 1. **Service-Specific Configurations**

#### **Order Service**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
  namespace: microservices
spec:
  replicas: 3
  selector:
    matchLabels:
      app: order-service
  template:
    metadata:
      labels:
        app: order-service
    spec:
      serviceAccountName: order-service-sa
      containers:
      - name: order-service
        image: your-account.dkr.ecr.us-west-2.amazonaws.com/order-service:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "aws"
        - name: DB_HOST
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: host
        - name: KAFKA_BROKERS
          valueFrom:
            configMapKeyRef:
              name: kafka-config
              key: brokers
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 5
          periodSeconds: 5
```

### 2. **Configuration Management**

#### **AWS Systems Manager Parameter Store Integration**
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
data:
  application.yml: |
    spring:
      cloud:
        aws:
          paramstore:
            enabled: true
            prefix: /microservices
            profile-separator: _
```

#### **Secrets Management**
```yaml
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: aws-secrets-manager
spec:
  provider:
    aws:
      service: SecretsManager
      region: us-west-2
      auth:
        jwt:
          serviceAccountRef:
            name: external-secrets-sa
---
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: db-credentials
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: aws-secrets-manager
    kind: SecretStore
  target:
    name: db-credentials
    creationPolicy: Owner
  data:
  - secretKey: username
    remoteRef:
      key: microservices/database
      property: username
  - secretKey: password
    remoteRef:
      key: microservices/database
      property: password
```

## 📊 Monitoring and Observability

### 1. **CloudWatch Integration**

```yaml
# CloudWatch Agent Configuration
apiVersion: v1
kind: ConfigMap
metadata:
  name: cwagentconfig
  namespace: amazon-cloudwatch
data:
  cwagentconfig.json: |
    {
      "logs": {
        "metrics_collected": {
          "kubernetes": {
            "cluster_name": "microservices-cluster",
            "metrics_collection_interval": 60
          }
        }
      },
      "metrics": {
        "namespace": "CWAgent",
        "metrics_collected": {
          "cpu": {
            "measurement": ["cpu_usage_idle", "cpu_usage_iowait"],
            "metrics_collection_interval": 60
          },
          "disk": {
            "measurement": ["used_percent"],
            "metrics_collection_interval": 60,
            "resources": ["*"]
          },
          "mem": {
            "measurement": ["mem_used_percent"],
            "metrics_collection_interval": 60
          }
        }
      }
    }
```

### 2. **Distributed Tracing with X-Ray**

```java
// Spring Boot Configuration
@Configuration
public class XRayConfig {
    
    @Bean
    public Filter TracingFilter() {
        return new AWSXRayServletFilter("order-service");
    }
    
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
            .filter(new TracingWebClientFilter())
            .build();
    }
}
```

## 🔒 Security Best Practices

### 1. **IAM Roles for Service Accounts (IRSA)**

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: order-service-sa
  namespace: microservices
  annotations:
    eks.amazonaws.com/role-arn: arn:aws:iam::ACCOUNT:role/OrderServiceRole
```

### 2. **Network Security**

```yaml
# Network Policy
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: order-service-netpol
  namespace: microservices
spec:
  podSelector:
    matchLabels:
      app: order-service
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: ingress-nginx
    ports:
    - protocol: TCP
      port: 8080
  egress:
  - to:
    - namespaceSelector:
        matchLabels:
          name: microservices
    ports:
    - protocol: TCP
      port: 8080
  - to: []
    ports:
    - protocol: TCP
      port: 5432  # PostgreSQL
    - protocol: TCP
      port: 9092  # Kafka
```

## 💰 Cost Optimization

### 1. **Spot Instances for Non-Critical Workloads**

```yaml
nodeGroups:
  - name: spot-workers
    instanceTypes: ["t3.medium", "t3.large"]
    spot: true
    desiredCapacity: 2
    minSize: 0
    maxSize: 10
```

### 2. **Horizontal Pod Autoscaler**

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: order-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: order-service
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

## 🚀 CI/CD Pipeline

### 1. **GitHub Actions with EKS**

```yaml
name: Deploy to EKS
on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    
    - name: Configure AWS credentials
      uses: aws-actions/configure-aws-credentials@v2
      with:
        aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
        aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
        aws-region: us-west-2
    
    - name: Login to Amazon ECR
      uses: aws-actions/amazon-ecr-login@v1
    
    - name: Build and push Docker image
      run: |
        docker build -t $ECR_REGISTRY/$ECR_REPOSITORY:$GITHUB_SHA .
        docker push $ECR_REGISTRY/$ECR_REPOSITORY:$GITHUB_SHA
    
    - name: Deploy to EKS
      run: |
        aws eks update-kubeconfig --region us-west-2 --name microservices-cluster
        kubectl set image deployment/order-service order-service=$ECR_REGISTRY/$ECR_REPOSITORY:$GITHUB_SHA
        kubectl rollout status deployment/order-service
```

## 📈 Performance Benchmarks

### Expected Performance Metrics:
- **Response Time**: < 100ms for 95% of requests
- **Throughput**: 10,000+ requests per second
- **Availability**: 99.99% uptime
- **Auto-scaling**: Scale from 2 to 50 pods in < 2 minutes

### Cost Estimates (Monthly):
- **EKS Cluster**: $73 (control plane)
- **EC2 Instances**: $150-300 (3-6 t3.medium nodes)
- **RDS**: $50-100 (db.t3.micro with Multi-AZ)
- **MSK**: $200-400 (3 kafka.t3.small brokers)
- **ALB**: $20-30
- **Total**: ~$500-900/month for production workload

This architecture provides a robust, scalable, and cost-effective foundation for microservices deployment on AWS, leveraging native AWS services for optimal performance and integration.