# Amazon MSK Integration Guide

This guide provides comprehensive information about the Amazon MSK (Managed Streaming for Apache Kafka) integration for the GenAI Demo application.

## Overview

The MSK integration provides a robust, scalable messaging infrastructure for domain events in the GenAI Demo application. It supports:

- **Domain Event Publishing**: Reliable publishing of domain events from aggregates
- **Event-Driven Architecture**: Decoupled communication between bounded contexts
- **Scalable Processing**: Horizontal scaling of event consumers
- **Monitoring & Observability**: Comprehensive monitoring and alerting
- **Security**: IAM-based authentication and encryption

## Architecture

### MSK Cluster Configuration

The MSK cluster is configured with the following features:

- **Multi-AZ Deployment**: Brokers distributed across multiple availability zones
- **Encryption**: Data encrypted at rest (KMS) and in transit (TLS)
- **Authentication**: IAM-based authentication for secure access
- **Monitoring**: Enhanced monitoring with Prometheus metrics
- **Logging**: CloudWatch Logs integration for broker logs

### Topic Strategy

Domain events are organized into topics following this naming convention:

```
{project}.{environment}.{domain}.{event-type}
```

Examples:

- `genai-demo.production.customer.created`
- `genai-demo.production.order.confirmed`
- `genai-demo.production.payment.processed`

## Infrastructure Components

### 1. MSK Cluster

```typescript
// Core cluster configuration
const cluster = new msk.CfnCluster(this, 'MSKCluster', {
    clusterName: `${projectName}-${environment}-msk`,
    kafkaVersion: '2.8.1',
    numberOfBrokerNodes: numberOfBrokers,
    
    // Broker configuration
    brokerNodeGroupInfo: {
        instanceType: brokerInstanceType,
        clientSubnets: privateSubnets,
        securityGroups: [mskSecurityGroup],
        storageInfo: {
            ebsStorageInfo: {
                volumeSize: storageSize
            }
        }
    },
    
    // Security configuration
    encryptionInfo: {
        encryptionAtRest: {
            dataVolumeKmsKeyId: kmsKey.keyId
        },
        encryptionInTransit: {
            clientBroker: 'TLS',
            inCluster: true
        }
    },
    
    // Authentication
    clientAuthentication: {
        sasl: {
            iam: {
                enabled: true
            }
        }
    }
});
```

### 2. Security Configuration

#### Security Groups

- **MSK Security Group**: Allows Kafka traffic from EKS cluster
  - Port 9092: Kafka plaintext (internal only)
  - Port 9094: Kafka TLS
  - Port 9096: Kafka SASL_SSL
  - Port 2181: Zookeeper
  - Port 2182: Zookeeper TLS

#### IAM Policies

- **Cluster Access**: Connect, describe, alter cluster
- **Topic Management**: Create, delete, alter, describe topics
- **Data Access**: Read and write data to topics
- **Consumer Groups**: Manage consumer group membership

### 3. Monitoring and Alerting

#### CloudWatch Alarms

- **CPU Utilization**: Alert when > 80%
- **Memory Utilization**: Alert when > 85%
- **Disk Utilization**: Alert when > 80%
- **Producer Request Rate**: Alert when > 1000 req/sec
- **Consumer Lag**: Alert when > 60 seconds

#### Metrics Collection

- **Enhanced Monitoring**: Per-broker and per-topic metrics
- **Prometheus Integration**: JMX and Node exporter enabled
- **Custom Metrics**: Domain events processing metrics

## Environment-Specific Configuration

### Development Environment

```json
{
  "msk": {
    "broker-instance-type": "kafka.t3.small",
    "number-of-brokers": 1,
    "storage-size": 20
  }
}
```

### Production Environment

```json
{
  "msk": {
    "broker-instance-type": "kafka.m5.large",
    "number-of-brokers": 3,
    "storage-size": 100
  }
}
```

## Spring Boot Integration

### Dependencies

Add the following dependencies to your `build.gradle`:

```gradle
dependencies {
    implementation 'org.springframework.kafka:spring-kafka'
    implementation 'software.amazon.msk:aws-msk-iam-auth:1.1.6'
    implementation 'org.apache.kafka:kafka-clients:3.4.0'
}
```

### Configuration

The application uses profile-based configuration:

#### Development Profile (`application-dev.yml`)

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    # Simple configuration for local development
```

#### Production Profile (`application-production.yml`)

```yaml
spring:
  kafka:
    bootstrap-servers: ${MSK_BOOTSTRAP_SERVERS}
    security:
      protocol: SASL_SSL
    sasl:
      mechanism: AWS_MSK_IAM
      jaas:
        config: software.amazon.msk.auth.iam.IAMLoginModule required;
```

### Domain Event Publishing

```java
@Component
@Profile("production")
public class KafkaDomainEventPublisher implements DomainEventPublisher {
    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;
    
    @Override
    public void publish(DomainEvent event) {
        String topic = getTopicForEvent(event);
        kafkaTemplate.send(topic, event.getAggregateId(), event);
    }
    
    private String getTopicForEvent(DomainEvent event) {
        return String.format("%s.%s.%s", 
            topicPrefix, 
            getDomainFromEvent(event),
            event.getEventType().toLowerCase()
        );
    }
}
```

### Event Consumption

```java
@Component
@Profile("production")
public class CustomerEventHandler {
    
    @KafkaListener(topics = "#{topicPrefix}.customer.created")
    public void handleCustomerCreated(CustomerCreatedEvent event) {
        // Process customer created event
        log.info("Processing customer created event: {}", event.getCustomerId());
        
        // Update read models, send notifications, etc.
        customerReadModelService.createCustomerReadModel(event);
        notificationService.sendWelcomeEmail(event);
    }
}
```

## Deployment Guide

### 1. Deploy Infrastructure

```bash
# Deploy the MSK stack
cd infrastructure
cdk deploy GenAIDemoInfrastructureStack-MSKStack --profile production

# Verify deployment
aws msk list-clusters --region ap-northeast-1
```

### 2. Create Kafka Topics

```bash
# Use the provided script to create domain event topics
./scripts/create-kafka-topics.sh production ap-northeast-1

# Verify topics were created
./scripts/create-kafka-topics.sh --list production ap-northeast-1
```

### 3. Configure Application

```bash
# Set environment variables for the application
export MSK_BOOTSTRAP_SERVERS=$(aws cloudformation describe-stacks \
    --stack-name genai-demo-production-MSKStack \
    --region ap-northeast-1 \
    --query 'Stacks[0].Outputs[?OutputKey==`MSKBootstrapServersIAM`].OutputValue' \
    --output text)

export SPRING_PROFILES_ACTIVE=production,msk
```

### 4. Deploy Application

```bash
# Deploy to EKS cluster
kubectl apply -f k8s/production/

# Verify application is connected to MSK
kubectl logs -f deployment/genai-demo-app | grep -i kafka
```

## Monitoring and Troubleshooting

### Health Checks

The application provides several health check endpoints:

```bash
# Overall application health
curl http://localhost:8080/actuator/health

# Kafka-specific health
curl http://localhost:8080/actuator/health/kafka

# Kafka metrics
curl http://localhost:8080/actuator/metrics/kafka.producer.record-send-total
```

### Common Issues and Solutions

#### 1. Connection Issues

**Problem**: Application cannot connect to MSK cluster

**Solution**:

```bash
# Check security group rules
aws ec2 describe-security-groups --group-ids sg-xxxxx

# Verify IAM permissions
aws sts get-caller-identity
aws kafka describe-cluster --cluster-arn arn:aws:kafka:...
```

#### 2. Authentication Issues

**Problem**: SASL authentication failures

**Solution**:

```bash
# Verify IAM role has MSK permissions
aws iam get-role-policy --role-name genai-demo-production-eks-node-role --policy-name MSKAccess

# Check MSK cluster policy
aws kafka get-cluster-policy --cluster-arn arn:aws:kafka:...
```

#### 3. Topic Creation Issues

**Problem**: Topics not being created automatically

**Solution**:

```bash
# Create topics manually
./scripts/create-kafka-topics.sh production ap-northeast-1

# Check topic configuration
kafka-topics.sh --bootstrap-server $MSK_BOOTSTRAP_SERVERS \
    --command-config client.properties \
    --describe --topic genai-demo.production.customer.created
```

### Monitoring Dashboards

#### CloudWatch Dashboard

Key metrics to monitor:

- **Cluster Metrics**: CPU, Memory, Disk utilization
- **Topic Metrics**: Messages in/out, partition count
- **Consumer Metrics**: Lag, throughput
- **Producer Metrics**: Send rate, error rate

#### Grafana Dashboard

If using Prometheus integration:

- **JMX Metrics**: Broker-level JVM metrics
- **Node Metrics**: System-level metrics
- **Application Metrics**: Custom domain event metrics

## Security Best Practices

### 1. Network Security

- MSK cluster deployed in private subnets
- Security groups restrict access to EKS cluster only
- No public access to brokers

### 2. Authentication and Authorization

- IAM-based authentication (no username/password)
- Fine-grained permissions per service
- Regular rotation of IAM credentials

### 3. Encryption

- Data encrypted at rest using KMS
- Data encrypted in transit using TLS
- Client-broker and inter-broker encryption

### 4. Monitoring and Auditing

- All API calls logged via CloudTrail
- Enhanced monitoring enabled
- Security events sent to SNS for alerting

## Performance Tuning

### Producer Configuration

```yaml
spring:
  kafka:
    producer:
      # Throughput optimization
      batch-size: 16384
      linger-ms: 5
      compression-type: snappy
      
      # Reliability optimization
      acks: all
      retries: 2147483647
      enable-idempotence: true
```

### Consumer Configuration

```yaml
spring:
  kafka:
    consumer:
      # Throughput optimization
      fetch-min-size: 1
      fetch-max-wait: 500
      max-poll-records: 500
      
      # Reliability optimization
      enable-auto-commit: false
      isolation-level: read_committed
```

### Broker Configuration

Key settings in MSK configuration:

- `num.partitions=6`: Default partition count
- `default.replication.factor=3`: Replication for durability
- `min.insync.replicas=2`: Minimum replicas for writes
- `log.retention.hours=168`: 7-day retention
- `compression.type=snappy`: Compression for efficiency

## Cost Optimization

### Development Environment

- Single broker (t3.small)
- Minimal storage (20GB)
- Shorter retention (7 days)

### Production Environment

- Multiple brokers (m5.large)
- Adequate storage (100GB)
- Longer retention (30 days)
- Reserved instances for cost savings

### Monitoring Costs

- CloudWatch billing alerts configured
- Cost allocation tags applied
- Regular cost reviews and optimization

## Disaster Recovery

### Multi-Region Setup

- Primary region: ap-east-2 (Taiwan)
- Secondary region: ap-northeast-1 (Tokyo)
- Cross-region replication using MirrorMaker 2.0

### Backup Strategy

- Topic configuration backed up
- Consumer offset management
- Schema registry backup (if used)

### Failover Procedures

1. Monitor primary region health
2. Redirect traffic to secondary region
3. Update DNS records
4. Verify data consistency
5. Plan failback procedures

## Conclusion

The MSK integration provides a robust, scalable, and secure messaging infrastructure for the GenAI Demo application. By following this guide, you can successfully deploy, configure, and operate the MSK cluster for production workloads.

For additional support or questions, refer to the AWS MSK documentation or contact the development team.
