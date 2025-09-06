# AWS EKS 原生微服務架構

## 🏗️ 整體架構圖

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

## 🔧 核心 AWS 服務選擇

### 1. **API Gateway 替代方案：Application Load Balancer (ALB)**
```yaml
# 簡單且成本效益高的選擇
apiVersion: v1
kind: Service
metadata:
  name: genai-demo-alb
  annotations:
    service.beta.kubernetes.io/aws-load-balancer-type: "external"
    service.beta.kubernetes.io/aws-load-balancer-nlb-target-type: "ip"
    service.beta.kubernetes.io/aws-load-balancer-scheme: "internet-facing"
    # 路由規則
    alb.ingress.kubernetes.io/listen-ports: '[{"HTTP": 80}, {"HTTPS": 443}]'
    alb.ingress.kubernetes.io/ssl-redirect: '443'
spec:
  type: LoadBalancer
  ports:
  - port: 80
    targetPort: 8080
```

### 2. **配置管理：AWS Systems Manager Parameter Store**
```java
// 替代 Spring Cloud Config
@Configuration
public class AwsConfigurationManager {
    
    private final SsmClient ssmClient;
    
    @PostConstruct
    public void loadConfiguration() {
        // 載入應用配置
        loadDatabaseConfig();
        loadKafkaConfig();
        loadRedisConfig();
    }
    
    private void loadDatabaseConfig() {
        String dbUrl = getParameter("/genai-demo/database/url");
        String dbUsername = getParameter("/genai-demo/database/username");
        String dbPassword = getParameter("/genai-demo/database/password", true);
        
        System.setProperty("spring.datasource.url", dbUrl);
        System.setProperty("spring.datasource.username", dbUsername);
        System.setProperty("spring.datasource.password", dbPassword);
    }
    
    private String getParameter(String name, boolean decrypt) {
        GetParameterRequest request = GetParameterRequest.builder()
            .name(name)
            .withDecryption(decrypt)
            .build();
        return ssmClient.getParameter(request).parameter().value();
    }
}
```

### 3. **消息中間件：Amazon MSK (Managed Kafka)**
```yaml
# MSK 配置 (Terraform)
resource "aws_msk_cluster" "genai_demo_kafka" {
  cluster_name           = "genai-demo-kafka"
  kafka_version         = "2.8.1"
  number_of_broker_nodes = 3

  broker_node_group_info {
    instance_type   = "kafka.m5.large"
    ebs_volume_size = 100
    client_subnets = [
      aws_subnet.private_subnet_1.id,
      aws_subnet.private_subnet_2.id,
      aws_subnet.private_subnet_3.id,
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
}
```

### 4. **數據庫：Amazon RDS (PostgreSQL)**
```yaml
# 每個服務獨立的 RDS 實例
resource "aws_db_instance" "order_service_db" {
  identifier = "genai-demo-order-db"
  engine     = "postgres"
  engine_version = "14.9"
  instance_class = "db.t3.micro"
  
  allocated_storage     = 20
  max_allocated_storage = 100
  storage_encrypted     = true
  
  db_name  = "orderdb"
  username = "orderuser"
  password = var.db_password
  
  vpc_security_group_ids = [aws_security_group.rds.id]
  db_subnet_group_name   = aws_db_subnet_group.main.name
  
  backup_retention_period = 7
  backup_window          = "03:00-04:00"
  maintenance_window     = "sun:04:00-sun:05:00"
  
  skip_final_snapshot = false
  final_snapshot_identifier = "genai-demo-order-db-final-snapshot"
  
  tags = {
    Name = "GenAI Demo Order DB"
    Service = "order-service"
  }
}
```

### 5. **緩存：Amazon ElastiCache (Redis)**
```yaml
resource "aws_elasticache_subnet_group" "main" {
  name       = "genai-demo-cache-subnet"
  subnet_ids = [aws_subnet.private_subnet_1.id, aws_subnet.private_subnet_2.id]
}

resource "aws_elasticache_replication_group" "redis" {
  replication_group_id       = "genai-demo-redis"
  description                = "Redis cluster for GenAI Demo"
  
  node_type                  = "cache.t3.micro"
  port                       = 6379
  parameter_group_name       = "default.redis7"
  
  num_cache_clusters         = 2
  automatic_failover_enabled = true
  multi_az_enabled          = true
  
  subnet_group_name = aws_elasticache_subnet_group.main.name
  security_group_ids = [aws_security_group.redis.id]
  
  at_rest_encryption_enabled = true
  transit_encryption_enabled = true
  
  tags = {
    Name = "GenAI Demo Redis"
  }
}
```

## 📊 監控和可觀測性：AWS 原生方案

### 1. **CloudWatch + X-Ray 替代 Jaeger**
```java
@Configuration
public class AwsObservabilityConfig {
    
    @Bean
    public XRayTraceHandler xrayTraceHandler() {
        return XRayTraceHandler.builder()
            .withSegmentNamingStrategy(new DynamicSegmentNamingStrategy("genai-demo"))
            .build();
    }
    
    @Bean
    public CloudWatchMeterRegistry cloudWatchMeterRegistry() {
        return CloudWatchMeterRegistry.builder(CloudWatchConfig.DEFAULT)
            .cloudWatchClient(CloudWatchAsyncClient.create())
            .build();
    }
}

// 自定義指標
@Component
public class OrderMetrics {
    private final Counter orderCreatedCounter;
    private final Timer orderProcessingTimer;
    
    public OrderMetrics(MeterRegistry meterRegistry) {
        this.orderCreatedCounter = Counter.builder("orders.created")
            .description("Number of orders created")
            .register(meterRegistry);
            
        this.orderProcessingTimer = Timer.builder("orders.processing.time")
            .description("Order processing time")
            .register(meterRegistry);
    }
}
```

### 2. **日誌管理：CloudWatch Logs**
```yaml
# Fluent Bit DaemonSet for log collection
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: fluent-bit
  namespace: amazon-cloudwatch
spec:
  selector:
    matchLabels:
      name: fluent-bit
  template:
    metadata:
      labels:
        name: fluent-bit
    spec:
      containers:
      - name: fluent-bit
        image: amazon/aws-for-fluent-bit:stable
        env:
        - name: AWS_REGION
          value: "ap-northeast-1"
        - name: CLUSTER_NAME
          value: "genai-demo-cluster"
        - name: LOG_GROUP_NAME
          value: "/aws/eks/genai-demo/application"
```

## 🔐 安全性：AWS 原生方案

### 1. **服務間認證：IAM Roles for Service Accounts (IRSA)**
```yaml
# 每個服務的 IAM 角色
apiVersion: v1
kind: ServiceAccount
metadata:
  name: order-service-sa
  namespace: genai-demo
  annotations:
    eks.amazonaws.com/role-arn: arn:aws:iam::ACCOUNT:role/OrderServiceRole

---
# IAM 角色 (Terraform)
resource "aws_iam_role" "order_service_role" {
  name = "OrderServiceRole"
  
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRoleWithWebIdentity"
        Effect = "Allow"
        Principal = {
          Federated = aws_iam_openid_connect_provider.eks.arn
        }
        Condition = {
          StringEquals = {
            "${replace(aws_iam_openid_connect_provider.eks.url, "https://", "")}:sub": "system:serviceaccount:genai-demo:order-service-sa"
          }
        }
      }
    ]
  })
}

# 附加必要的權限
resource "aws_iam_role_policy" "order_service_policy" {
  name = "OrderServicePolicy"
  role = aws_iam_role.order_service_role.id
  
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ssm:GetParameter",
          "ssm:GetParameters",
          "ssm:GetParametersByPath"
        ]
        Resource = "arn:aws:ssm:*:*:parameter/genai-demo/order/*"
      },
      {
        Effect = "Allow"
        Action = [
          "rds:DescribeDBInstances"
        ]
        Resource = "*"
      }
    ]
  })
}
```

### 2. **網路安全：VPC + Security Groups**
```yaml
# Security Group for EKS nodes
resource "aws_security_group" "eks_nodes" {
  name_prefix = "genai-demo-eks-nodes"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port = 443
    to_port   = 443
    protocol  = "tcp"
    cidr_blocks = [aws_vpc.main.cidr_block]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# Security Group for RDS
resource "aws_security_group" "rds" {
  name_prefix = "genai-demo-rds"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.eks_nodes.id]
  }
}
```

## 🚀 部署策略

### 1. **藍綠部署使用 AWS Load Balancer Controller**
```yaml
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: order-service
spec:
  replicas: 5
  strategy:
    blueGreen:
      activeService: order-service-active
      previewService: order-service-preview
      autoPromotionEnabled: false
      scaleDownDelaySeconds: 30
      prePromotionAnalysis:
        templates:
        - templateName: success-rate
        args:
        - name: service-name
          value: order-service-preview
      postPromotionAnalysis:
        templates:
        - templateName: success-rate
        args:
        - name: service-name
          value: order-service-active
  selector:
    matchLabels:
      app: order-service
  template:
    metadata:
      labels:
        app: order-service
    spec:
      containers:
      - name: order-service
        image: ACCOUNT.dkr.ecr.REGION.amazonaws.com/genai-demo/order-service:latest
```

### 2. **CI/CD Pipeline 使用 AWS CodePipeline**
```yaml
# buildspec.yml
version: 0.2
phases:
  pre_build:
    commands:
      - echo Logging in to Amazon ECR...
      - aws ecr get-login-password --region $AWS_DEFAULT_REGION | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com
  build:
    commands:
      - echo Build started on `date`
      - echo Building the Docker image...
      - docker build -t $IMAGE_REPO_NAME:$IMAGE_TAG .
      - docker tag $IMAGE_REPO_NAME:$IMAGE_TAG $AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com/$IMAGE_REPO_NAME:$IMAGE_TAG
  post_build:
    commands:
      - echo Build completed on `date`
      - echo Pushing the Docker image...
      - docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com/$IMAGE_REPO_NAME:$IMAGE_TAG
      - echo Writing image definitions file...
      - printf '[{"name":"order-service","imageUri":"%s"}]' $AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com/$IMAGE_REPO_NAME:$IMAGE_TAG > imagedefinitions.json
artifacts:
  files:
    - imagedefinitions.json
    - k8s-manifests/*
```

## 💰 成本優化建議

### 1. **使用 Spot Instances**
```yaml
# EKS Node Group with Spot Instances
resource "aws_eks_node_group" "spot_nodes" {
  cluster_name    = aws_eks_cluster.main.name
  node_group_name = "genai-demo-spot-nodes"
  node_role_arn   = aws_iam_role.node_group.arn
  subnet_ids      = aws_subnet.private[*].id
  
  capacity_type = "SPOT"
  instance_types = ["t3.medium", "t3.large"]
  
  scaling_config {
    desired_size = 2
    max_size     = 10
    min_size     = 1
  }
}
```

### 2. **自動擴縮容**
```yaml
# Horizontal Pod Autoscaler
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

## 📋 總結

這個 AWS EKS 原生架構具有以下優勢：

1. **簡化複雜度**：避免 Kong、Istio 等複雜組件
2. **AWS 原生整合**：充分利用 AWS 服務生態
3. **成本效益**：使用 Spot Instances 和自動擴縮容
4. **安全性**：IRSA + VPC + Security Groups
5. **可觀測性**：CloudWatch + X-Ray 完整監控
6. **易於維護**：減少第三方依賴，降低運維複雜度

這個架構可以讓你專注於業務邏輯，而不是基礎設施的複雜性。
