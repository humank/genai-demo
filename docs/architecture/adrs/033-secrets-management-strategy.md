---
adr_number: 033
title: "Secrets Management Strategy"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [014, 016, 007]
affected_viewpoints: ["deployment", "operational"]
affected_perspectives: ["security", "availability"]
---

# ADR-033: Secrets Management Strategy

## Status

**Accepted** - 2025-10-25

## Context

### Problem Statement

The Enterprise E-Commerce Platform requires secure management of sensitive configuration data including:
- Database credentials (PostgreSQL, Redis)
- API keys (payment gateways, email services, SMS providers)
- Encryption keys (JWT signing keys, data encryption keys)
- Third-party service credentials
- OAuth client secrets
- Certificate private keys

We need a secrets management solution that:
- Stores secrets securely with encryption at rest
- Provides fine-grained access control
- Enables automatic secret rotation
- Supports audit logging for secret access
- Integrates with AWS services and Kubernetes
- Enables secrets versioning and rollback
- Supports disaster recovery and high availability

### Business Context

**Business Drivers**:
- Security compliance (PCI-DSS, GDPR, SOC 2)
- Prevent credential leakage and data breaches
- Regulatory audit requirements
- Zero-trust security model
- Expected growth from 10K to 1M+ users

**Constraints**:
- Must integrate with AWS infrastructure
- Must support Kubernetes (EKS) workloads
- Performance: Secret retrieval < 100ms
- Budget: $500/month for secrets management
- Must support automatic rotation without downtime

### Technical Context

**Current State**:
- AWS EKS for container orchestration
- Spring Boot microservices
- AWS CDK for infrastructure as code
- Multiple environments (dev, staging, production)

**Requirements**:
- Centralized secrets storage
- Encryption at rest and in transit
- Fine-grained IAM-based access control
- Automatic secret rotation
- Audit logging (CloudTrail integration)
- Versioning and rollback capability
- High availability (99.9%+)

## Decision Drivers

1. **Security**: Industry-standard encryption and access control
2. **Integration**: Seamless AWS and Kubernetes integration
3. **Automation**: Automatic rotation without manual intervention
4. **Auditability**: Complete audit trail for compliance
5. **Performance**: Fast secret retrieval (< 100ms)
6. **Cost**: Within budget constraints
7. **Operational**: Minimal operational overhead
8. **Reliability**: High availability and disaster recovery

## Considered Options

### Option 1: AWS Secrets Manager

**Description**: Fully managed secrets management service by AWS

**Pros**:
- ✅ Fully managed (no infrastructure to maintain)
- ✅ Native AWS integration (RDS, EKS, Lambda)
- ✅ Automatic rotation for RDS, Redshift, DocumentDB
- ✅ Encryption with AWS KMS
- ✅ Fine-grained IAM access control
- ✅ CloudTrail audit logging
- ✅ Versioning and rollback
- ✅ Cross-region replication
- ✅ High availability (99.99% SLA)
- ✅ Kubernetes integration via External Secrets Operator

**Cons**:
- ⚠️ Cost: $0.40 per secret per month + $0.05 per 10K API calls
- ⚠️ AWS vendor lock-in
- ⚠️ Limited to 65,536 bytes per secret

**Cost**: $400/month (estimated for 1000 secrets + API calls)

**Risk**: **Low** - Proven AWS service

### Option 2: AWS Systems Manager Parameter Store

**Description**: Hierarchical storage for configuration data and secrets

**Pros**:
- ✅ Free for standard parameters (up to 10K)
- ✅ Native AWS integration
- ✅ KMS encryption for SecureString
- ✅ IAM access control
- ✅ CloudTrail audit logging
- ✅ Versioning support
- ✅ Parameter hierarchies

**Cons**:
- ❌ No automatic rotation (manual implementation required)
- ❌ Limited to 8KB per parameter (standard) or 4KB (advanced)
- ❌ Advanced parameters cost $0.05 per parameter per month
- ❌ Higher throughput tier costs extra
- ❌ Less feature-rich than Secrets Manager

**Cost**: $50/month (for advanced parameters)

**Risk**: **Medium** - Requires custom rotation logic

### Option 3: HashiCorp Vault

**Description**: Self-managed secrets management platform

**Pros**:
- ✅ Cloud-agnostic (no vendor lock-in)
- ✅ Advanced features (dynamic secrets, PKI, encryption as a service)
- ✅ Fine-grained policies
- ✅ Automatic rotation support
- ✅ Audit logging
- ✅ Multi-cloud support
- ✅ Active community

**Cons**:
- ❌ Self-managed infrastructure (high operational overhead)
- ❌ High availability setup complex
- ❌ Requires dedicated team for operations
- ❌ Additional infrastructure costs (EC2, storage, backups)
- ❌ Learning curve for team
- ❌ Maintenance burden (upgrades, patches)

**Cost**: $2,000/month (infrastructure + operations)

**Risk**: **High** - Operational complexity

### Option 4: Kubernetes Secrets (Native)

**Description**: Built-in Kubernetes secrets management

**Pros**:
- ✅ No additional cost
- ✅ Native Kubernetes integration
- ✅ Simple to use

**Cons**:
- ❌ Base64 encoding only (not encrypted by default)
- ❌ No automatic rotation
- ❌ Limited access control
- ❌ No audit logging
- ❌ Secrets stored in etcd (security concerns)
- ❌ Not suitable for production

**Cost**: $0

**Risk**: **High** - Insufficient security

## Decision Outcome

**Chosen Option**: **AWS Secrets Manager with External Secrets Operator for Kubernetes**

### Rationale

AWS Secrets Manager was selected for the following reasons:

1. **Fully Managed**: No infrastructure to maintain, reduces operational overhead
2. **Automatic Rotation**: Built-in rotation for RDS, custom rotation via Lambda
3. **Security**: KMS encryption, IAM access control, CloudTrail auditing
4. **Integration**: Native AWS integration, Kubernetes via External Secrets Operator
5. **Reliability**: 99.99% SLA, cross-region replication
6. **Compliance**: Meets PCI-DSS, GDPR, SOC 2 requirements
7. **Cost-Effective**: $400/month within budget, no infrastructure costs
8. **Versioning**: Automatic versioning and rollback capability

**Architecture**:

```
┌─────────────────────────────────────────────────────────────┐
│                     AWS Secrets Manager                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ DB Credentials│  │  API Keys    │  │ JWT Keys     │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
│         ▲                  ▲                  ▲              │
│         │ KMS Encryption   │                  │              │
│         └──────────────────┴──────────────────┘              │
└─────────────────────────────────────────────────────────────┘
                           │
          ┌────────────────┼────────────────┐
          │                │                │
          ▼                ▼                ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ Spring Boot  │  │   Lambda     │  │     EKS      │
│ (Direct SDK) │  │ (Rotation)   │  │ (Ext Secrets)│
└──────────────┘  └──────────────┘  └──────────────┘
```

**Secret Categories**:

| Category | Storage | Rotation | Access Method |
|----------|---------|----------|---------------|
| Database Credentials | Secrets Manager | Automatic (30 days) | Spring Boot SDK |
| API Keys | Secrets Manager | Manual/Lambda | Spring Boot SDK |
| JWT Signing Keys | Secrets Manager | Manual (90 days) | Spring Boot SDK |
| Kubernetes Secrets | Secrets Manager | Sync via External Secrets | K8s Secret |
| Certificates | Secrets Manager | Manual/ACM | Spring Boot SDK |

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | Medium | Need to use Secrets Manager SDK | Training, code examples, libraries |
| Operations Team | Low | Managed service, minimal ops | Monitoring dashboards, runbooks |
| Security Team | Positive | Enhanced secrets security | Regular audits |
| Infrastructure Team | Medium | CDK integration required | CDK constructs, documentation |
| End Users | None | Transparent to users | N/A |

### Impact Radius

**Selected Impact Radius**: **System**

Affects:
- All microservices (secret retrieval)
- Infrastructure code (CDK)
- Kubernetes deployments (External Secrets)
- CI/CD pipelines (secret injection)
- Monitoring and alerting

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Secret retrieval latency | Low | Medium | Caching, connection pooling, retry logic |
| AWS Secrets Manager outage | Very Low | High | Cross-region replication, fallback to cached secrets |
| Cost overrun | Medium | Low | Monitor API calls, implement caching, set budget alerts |
| Rotation failures | Low | High | Automated testing, rollback capability, alerts |
| Misconfigured IAM policies | Medium | High | Least privilege, automated policy validation, regular audits |

**Overall Risk Level**: **Low**

## Implementation Plan

### Phase 1: Infrastructure Setup (Week 1)

- [x] Create AWS Secrets Manager secrets for each environment
- [x] Configure KMS encryption keys
- [x] Set up IAM roles and policies (least privilege)
- [x] Enable CloudTrail logging for Secrets Manager
- [x] Configure cross-region replication (Tokyo backup)
- [x] Set up budget alerts

### Phase 2: Application Integration (Week 2)

- [x] Add AWS Secrets Manager SDK to Spring Boot
- [x] Implement secrets retrieval service
- [x] Add caching layer (5-minute TTL)
- [x] Update application configuration
- [x] Implement graceful degradation (cached secrets)
- [x] Add health checks for secrets access

### Phase 3: Kubernetes Integration (Week 3)

- [x] Install External Secrets Operator on EKS
- [x] Configure IRSA (IAM Roles for Service Accounts)
- [x] Create SecretStore and ExternalSecret resources
- [x] Migrate existing K8s secrets to Secrets Manager
- [x] Test secret synchronization
- [x] Update deployment manifests

### Phase 4: Automatic Rotation (Week 4)

- [x] Enable automatic rotation for RDS credentials
- [x] Create Lambda functions for custom rotation
- [x] Implement rotation for API keys
- [x] Implement rotation for JWT signing keys
- [x] Test rotation without downtime
- [x] Set up rotation monitoring and alerts

### Phase 5: Migration and Testing (Week 5)

- [x] Migrate secrets from environment variables
- [x] Migrate secrets from config files
- [x] Remove hardcoded secrets from code
- [x] Integration testing
- [x] Security testing (penetration testing)
- [x] Disaster recovery testing

### Rollback Strategy

**Trigger Conditions**:
- Secret retrieval failures > 5%
- Performance degradation > 200ms
- Rotation failures causing outages
- Cost exceeding budget by > 50%

**Rollback Steps**:
1. Revert to environment variables (temporary)
2. Investigate and fix Secrets Manager integration
3. Re-deploy with fixes
4. Gradually migrate back to Secrets Manager

**Rollback Time**: < 1 hour

## Monitoring and Success Criteria

### Success Metrics

- ✅ Secret retrieval latency < 100ms (95th percentile)
- ✅ Secret retrieval success rate > 99.9%
- ✅ Zero secret leakage incidents
- ✅ Automatic rotation success rate 100%
- ✅ Audit log completeness 100%
- ✅ Cost within budget ($500/month)

### Monitoring Plan

**CloudWatch Metrics**:
- `secrets.retrieval.time` (histogram)
- `secrets.retrieval.success` (count)
- `secrets.retrieval.failure` (count)
- `secrets.rotation.success` (count)
- `secrets.rotation.failure` (count)
- `secrets.cache.hit_rate` (gauge)

**Alerts**:
- Secret retrieval failure rate > 1% for 5 minutes
- Secret retrieval latency > 200ms for 5 minutes
- Rotation failures
- Unauthorized secret access attempts
- Cost exceeding budget

**Security Monitoring**:
- Secret access patterns (CloudTrail)
- Unauthorized access attempts
- Secret version changes
- IAM policy changes
- Rotation failures

**Review Schedule**:
- Daily: Check secret access metrics
- Weekly: Review CloudTrail audit logs
- Monthly: Secret rotation verification
- Quarterly: Security audit and IAM policy review

## Consequences

### Positive Consequences

- ✅ **Enhanced Security**: KMS encryption, IAM access control
- ✅ **Automatic Rotation**: Reduces manual effort and human error
- ✅ **Auditability**: Complete audit trail for compliance
- ✅ **Reliability**: 99.99% SLA, cross-region replication
- ✅ **Integration**: Seamless AWS and Kubernetes integration
- ✅ **Versioning**: Easy rollback to previous secret versions
- ✅ **Operational**: Fully managed, minimal overhead
- ✅ **Compliance**: Meets PCI-DSS, GDPR, SOC 2

### Negative Consequences

- ⚠️ **Cost**: $400/month for secrets storage and API calls
- ⚠️ **Vendor Lock-in**: AWS dependency
- ⚠️ **Latency**: Small overhead for secret retrieval (mitigated by caching)
- ⚠️ **Complexity**: Additional integration code required

### Technical Debt

**Identified Debt**:
1. No multi-cloud secrets management (acceptable for AWS-only deployment)
2. Manual rotation for some secrets (acceptable initially)
3. No secrets scanning in CI/CD (should be added)

**Debt Repayment Plan**:
- **Q2 2026**: Implement secrets scanning in CI/CD pipeline
- **Q3 2026**: Automate rotation for all secrets
- **Q4 2026**: Evaluate multi-cloud secrets management if needed

## Related Decisions

- [ADR-014: JWT-Based Authentication Strategy](014-jwt-based-authentication-strategy.md) - JWT key management
- [ADR-016: Data Encryption Strategy](016-data-encryption-strategy.md) - Encryption key management
- [ADR-007: Use AWS CDK for Infrastructure](007-use-aws-cdk-for-infrastructure.md) - Infrastructure provisioning

## Notes

### Secrets Manager Implementation

```java
@Configuration
public class SecretsManagerConfiguration {
    
    @Bean
    public SecretsManagerClient secretsManagerClient() {
        return SecretsManagerClient.builder()
            .region(Region.AP_NORTHEAST_1)
            .build();
    }
    
    @Bean
    public SecretsService secretsService(SecretsManagerClient client) {
        return new SecretsService(client);
    }
}

@Service
public class SecretsService {
    
    private final SecretsManagerClient client;
    private final Cache<String, String> secretsCache;
    
    public SecretsService(SecretsManagerClient client) {
        this.client = client;
        this.secretsCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(100)
            .build();
    }
    
    public String getSecret(String secretName) {
        return secretsCache.get(secretName, key -> {
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                .secretId(secretName)
                .build();
            
            GetSecretValueResponse response = client.getSecretValue(request);
            return response.secretString();
        });
    }
    
    public <T> T getSecretAsObject(String secretName, Class<T> clazz) {
        String secretJson = getSecret(secretName);
        return objectMapper.readValue(secretJson, clazz);
    }
}

// Usage in application
@Configuration
public class DatabaseConfiguration {
    
    @Autowired
    private SecretsService secretsService;
    
    @Bean
    public DataSource dataSource() {
        DatabaseCredentials creds = secretsService.getSecretAsObject(
            "prod/database/credentials",
            DatabaseCredentials.class
        );
        
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(creds.getJdbcUrl());
        config.setUsername(creds.getUsername());
        config.setPassword(creds.getPassword());
        
        return new HikariDataSource(config);
    }
}
```

### External Secrets Operator Configuration

```yaml
# SecretStore - connects to AWS Secrets Manager
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: aws-secrets-manager
  namespace: production
spec:
  provider:
    aws:
      service: SecretsManager
      region: ap-northeast-1
      auth:
        jwt:
          serviceAccountRef:
            name: external-secrets-sa

---
# ExternalSecret - syncs secret from AWS to K8s
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: database-credentials
  namespace: production
spec:
  refreshInterval: 5m
  secretStoreRef:
    name: aws-secrets-manager
    kind: SecretStore
  target:
    name: database-credentials
    creationPolicy: Owner
  data:
    - secretKey: username
      remoteRef:
        key: prod/database/credentials
        property: username
    - secretKey: password
      remoteRef:
        key: prod/database/credentials
        property: password
```

### AWS CDK Infrastructure

```typescript
// CDK construct for secrets
export class SecretsStack extends Stack {
  constructor(scope: Construct, id: string, props?: StackProps) {
    super(scope, id, props);
    
    // Database credentials with automatic rotation
    const dbSecret = new secretsmanager.Secret(this, 'DatabaseCredentials', {
      secretName: 'prod/database/credentials',
      description: 'PostgreSQL database credentials',
      generateSecretString: {
        secretStringTemplate: JSON.stringify({ username: 'admin' }),
        generateStringKey: 'password',
        excludePunctuation: true,
        passwordLength: 32,
      },
    });
    
    // Enable automatic rotation
    dbSecret.addRotationSchedule('RotationSchedule', {
      automaticallyAfter: Duration.days(30),
      rotationLambda: new lambda.Function(this, 'RotationFunction', {
        runtime: lambda.Runtime.PYTHON_3_11,
        handler: 'index.handler',
        code: lambda.Code.fromAsset('lambda/rotation'),
      }),
    });
    
    // JWT signing key
    const jwtSecret = new secretsmanager.Secret(this, 'JWTSigningKey', {
      secretName: 'prod/jwt/signing-key',
      description: 'JWT signing key (RS256)',
      generateSecretString: {
        excludePunctuation: true,
        passwordLength: 64,
      },
    });
    
    // Cross-region replication
    new secretsmanager.CfnReplicaSecret(this, 'JWTSecretReplica', {
      secretId: jwtSecret.secretArn,
      replicaRegions: [
        { region: 'ap-northeast-1' }, // Tokyo backup
      ],
    });
  }
}
```

### Secret Rotation Lambda

```python
import boto3
import json

def handler(event, context):
    """
    Lambda function for rotating API keys
    """
    service_client = boto3.client('secretsmanager')
    
    # Get secret metadata
    token = event['Token']
    step = event['Step']
    secret_arn = event['SecretId']
    
    if step == 'createSecret':
        # Generate new secret
        new_secret = generate_new_api_key()
        service_client.put_secret_value(
            SecretId=secret_arn,
            ClientRequestToken=token,
            SecretString=json.dumps(new_secret),
            VersionStages=['AWSPENDING']
        )
        
    elif step == 'setSecret':
        # Update external service with new key
        update_external_service(new_secret)
        
    elif step == 'testSecret':
        # Test new secret
        test_api_key(new_secret)
        
    elif step == 'finishSecret':
        # Mark new version as current
        service_client.update_secret_version_stage(
            SecretId=secret_arn,
            VersionStage='AWSCURRENT',
            MoveToVersionId=token,
            RemoveFromVersionId=get_current_version(secret_arn)
        )
```

### Secret Naming Convention

Format: `{environment}/{service}/{secret-type}`

Examples:
- `prod/database/credentials` - Production database credentials
- `prod/jwt/signing-key` - JWT signing key
- `prod/api/payment-gateway` - Payment gateway API key
- `prod/api/email-service` - Email service API key
- `staging/database/credentials` - Staging database credentials

### IAM Policy Example

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue",
        "secretsmanager:DescribeSecret"
      ],
      "Resource": [
        "arn:aws:secretsmanager:ap-northeast-1:*:secret:prod/database/*",
        "arn:aws:secretsmanager:ap-northeast-1:*:secret:prod/jwt/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "kms:Decrypt"
      ],
      "Resource": "arn:aws:kms:ap-northeast-1:*:key/*",
      "Condition": {
        "StringEquals": {
          "kms:ViaService": "secretsmanager.ap-northeast-1.amazonaws.com"
        }
      }
    }
  ]
}
```

---

**Document Status**: ✅ Accepted  
**Last Reviewed**: 2025-10-25  
**Next Review**: 2026-01-25 (Quarterly)
