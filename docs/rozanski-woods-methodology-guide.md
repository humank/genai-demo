# Rozanski & Woods Architecture Methodology Guide

> **Complete Guide to Viewpoints and Perspectives for Software Architecture Documentation**

## 📚 Table of Contents

- [Introduction](#introduction)
- [Methodology Overview](#methodology-overview)
- [Viewpoints (System Structure)](#viewpoints-system-structure)
- [Perspectives (Quality Attributes)](#perspectives-quality-attributes)
- [How to Use This Guide](#how-to-use-this-guide)
- [Practical Examples](#practical-examples)
- [References](#references)

---

## Introduction

### What is Rozanski & Woods Methodology

The Rozanski & Woods methodology is a comprehensive approach to software architecture documentation that separates concerns into:

1. **Viewpoints** - Describe **WHAT** the system is and **HOW** it's structured
2. **Perspectives** - Describe **QUALITY ATTRIBUTES** that cut across multiple viewpoints

### Why Use This Methodology

**Benefits:**

- ✅ **Systematic Coverage**: Ensures no architectural aspect is overlooked
- ✅ **Stakeholder Communication**: Different viewpoints for different audiences
- ✅ **Quality Focus**: Perspectives ensure quality attributes are addressed
- ✅ **Maintainability**: Clear structure makes documentation easier to maintain
- ✅ **Traceability**: Links requirements to architecture decisions

**When to Use:**

- Designing new systems
- Documenting existing systems
- Architecture reviews
- Stakeholder communication
- Compliance and audit requirements

---

## Methodology Overview

### The Two-Dimensional Model

Rozanski & Woods 方法論使用二維模型來完整描述軟體架構：

```mermaid
graph TB
    subgraph VP["📐 VIEWPOINTS (結構維度 - Structure Dimension)"]
        V1[Functional<br/>功能視角<br/>系統做什麼]
        V2[Information<br/>資訊視角<br/>資料如何組織]
        V3[Concurrency<br/>並發視角<br/>並發處理]
        V4[Development<br/>開發視角<br/>程式碼組織]
        V5[Deployment<br/>部署視角<br/>如何部署]
        V6[Operational<br/>運維視角<br/>如何運行]
    end
    
    subgraph PS["🎯 PERSPECTIVES (品質維度 - Quality Dimension)"]
        P1[Security<br/>安全性<br/>如何保護]
        P2[Performance<br/>效能<br/>多快]
        P3[Availability<br/>可用性<br/>多穩定]
        P4[Evolution<br/>演進性<br/>如何變更]
        P5[Scalability<br/>擴展性<br/>如何擴展]
    end
    
    P1 -.應用於.-> V1
    P1 -.應用於.-> V2
    P1 -.應用於.-> V3
    P1 -.應用於.-> V4
    P1 -.應用於.-> V5
    P1 -.應用於.-> V6
    
    P2 -.應用於.-> V1
    P2 -.應用於.-> V2
    P2 -.應用於.-> V3
    
    P3 -.應用於.-> V5
    P3 -.應用於.-> V6
    
    style VP fill:#e1f5ff
    style PS fill:#fff4e1
```

### Viewpoints vs Perspectives：核心差異

#### 📐 Viewpoints (視角) - "結構維度"

**定義**：描述系統的**結構和組織方式**，回答"系統是什麼"和"如何構建"

**特性**：
- 🏗️ **結構性 (Structural)**：關注系統的靜態和動態結構
- 📦 **模組化 (Modular)**：每個視角獨立描述系統的一個面向
- 👥 **利害關係人導向 (Stakeholder-oriented)**：不同視角服務不同的利害關係人
- 🎯 **具體 (Concrete)**：描述具體的元件、介面、部署等

**回答的問題**：
- **Functional**: 系統提供什麼功能？
- **Information**: 資料如何儲存和流動？
- **Concurrency**: 如何處理並發？
- **Development**: 程式碼如何組織？
- **Deployment**: 如何部署到環境？
- **Operational**: 如何監控和維運？

**範例**：
```
Functional Viewpoint 描述：
- 客戶管理模組提供註冊、登入、個人資料管理功能
- 訂單模組提供下單、查詢、取消功能
- 這些模組透過 REST API 互動
```

---

#### 🎯 Perspectives (觀點) - "品質維度"

**定義**：描述系統的**品質屬性**，回答"系統有多好"和"如何確保品質"

**特性**：
- 🌐 **橫切性 (Cross-cutting)**：跨越所有視角，影響整個系統
- 📊 **品質導向 (Quality-oriented)**：關注非功能性需求
- 🎚️ **可量化 (Measurable)**：通常有明確的指標和目標
- 🔄 **持續性 (Continuous)**：需要在整個開發過程中持續關注

**回答的問題**：
- **Security**: 系統夠安全嗎？如何防護？
- **Performance**: 系統夠快嗎？能承受多少負載？
- **Availability**: 系統夠穩定嗎？故障如何恢復？
- **Evolution**: 系統容易修改嗎？如何演進？
- **Scalability**: 系統能擴展嗎？如何擴展？

**範例**：
```
Security Perspective 應用：
- Functional: 實作認證授權功能
- Information: 加密敏感資料
- Deployment: 配置防火牆和網路隔離
- Operational: 監控安全事件和異常登入
```

---

### 互補關係：如何協同工作

#### 🔄 Viewpoints 提供結構，Perspectives 確保品質

```mermaid
graph LR
    A[Functional Viewpoint<br/>定義登入功能] --> B[Security Perspective<br/>確保登入安全]
    B --> C[實作方案<br/>JWT + MFA + 加密]
    
    D[Deployment Viewpoint<br/>定義部署架構] --> E[Availability Perspective<br/>確保高可用]
    E --> F[實作方案<br/>多區域 + 負載均衡]
    
    style A fill:#e1f5ff
    style D fill:#e1f5ff
    style B fill:#fff4e1
    style E fill:#fff4e1
    style C fill:#d4edda
    style F fill:#d4edda
```

#### 📋 互補性範例

| Viewpoint | + Perspective | = 實作決策 |
|-----------|---------------|-----------|
| **Functional**<br/>客戶註冊功能 | **Security**<br/>保護個資 | 密碼加密 (bcrypt)<br/>Email 驗證<br/>CAPTCHA 防機器人 |
| **Information**<br/>訂單資料模型 | **Performance**<br/>快速查詢 | 資料庫索引<br/>快取策略<br/>讀寫分離 |
| **Deployment**<br/>容器化部署 | **Scalability**<br/>自動擴展 | Kubernetes HPA<br/>服務網格<br/>無狀態設計 |
| **Concurrency**<br/>並發處理 | **Availability**<br/>容錯處理 | 樂觀鎖<br/>重試機制<br/>斷路器模式 |

#### 🎯 實際應用流程

```mermaid
sequenceDiagram
    participant A as 架構師
    participant V as Viewpoints<br/>(結構設計)
    participant P as Perspectives<br/>(品質檢查)
    participant D as 設計決策
    
    A->>V: 1. 設計系統結構
    Note over V: 定義功能模組<br/>資料模型<br/>部署架構
    
    A->>P: 2. 應用品質觀點
    Note over P: 檢查安全性<br/>效能<br/>可用性
    
    P->>V: 3. 發現問題
    Note over P,V: 效能瓶頸<br/>安全漏洞<br/>擴展限制
    
    V->>D: 4. 調整設計
    Note over D: 加入快取<br/>加密機制<br/>負載均衡
    
    D->>P: 5. 驗證品質
    Note over P: 確認符合<br/>品質目標
    
    P->>A: 6. 完成設計
```

---

### 為什麼需要兩個維度？

#### ❌ 只用 Viewpoints 的問題

```
只描述結構 → 可能忽略品質屬性
- 功能完整但效能差
- 部署架構清楚但不安全
- 程式碼組織良好但難以擴展
```

#### ❌ 只用 Perspectives 的問題

```
只關注品質 → 缺乏具體實作指引
- 知道要安全但不知道如何實作
- 知道要高效能但不知道架構如何設計
- 知道要可擴展但不知道如何部署
```

#### ✅ 兩者結合的優勢

```
結構 + 品質 = 完整的架構設計
- 清楚的系統結構
- 明確的品質目標
- 具體的實作方案
- 可驗證的設計決策
```

---

### 實務建議

#### 📝 文檔組織建議

```
docs/
├── viewpoints/
│   ├── functional.md          # 功能視角
│   ├── information.md         # 資訊視角
│   ├── deployment.md          # 部署視角
│   └── ...
├── perspectives/
│   ├── security.md            # 安全觀點
│   ├── performance.md         # 效能觀點
│   └── ...
└── decisions/
    ├── ADR-001-auth.md        # 結合兩者的決策
    └── ADR-002-cache.md
```

#### 🔍 審查檢查清單

**Viewpoint 檢查**：
- [ ] 所有主要功能都有文檔？
- [ ] 資料模型完整定義？
- [ ] 部署架構清楚？
- [ ] 開發指引明確？

**Perspective 檢查**：
- [ ] 安全需求都滿足？
- [ ] 效能目標都達成？
- [ ] 可用性要求都實現？
- [ ] 系統可演進和擴展？

**整合檢查**：
- [ ] 每個 Viewpoint 都考慮了相關 Perspectives？
- [ ] 每個 Perspective 都應用到了相關 Viewpoints？
- [ ] 設計決策有明確的品質目標？

---

## Viewpoints (System Structure)

Viewpoints describe the **structure and organization** of the system from different angles.

### 1. Functional Viewpoint

**Purpose:** Describes the system's functional elements, their responsibilities, and interactions

**Key Questions:**

- What are the main functional capabilities?
- How do functional elements interact?
- What are the key use cases?
- What interfaces does the system expose?

**What to Document:**

#### 1.1 Functional Elements

```markdown
## Example Structure

### Customer Management Module

- **Responsibilities**: 
  - Customer registration and authentication
  - Profile management
  - Membership level tracking
  
- **Provided Services**:
  - `POST /api/v1/customers` - Create customer
  - `GET /api/v1/customers/{id}` - Retrieve customer
  - `PUT /api/v1/customers/{id}` - Update customer
  
- **Dependencies**:
  - Email Service (for notifications)
  - Authentication Service (for login)
  - Payment Service (for membership upgrades)

```

#### 1.2 System Capabilities

- Core business functions
- Supporting functions
- Integration points with external systems

#### 1.3 Use Cases & Scenarios

```gherkin
Feature: Customer Registration
  Scenario: Successful registration
    Given a new customer with valid email
    When they submit registration form
    Then account should be created
    And welcome email should be sent
```

#### 1.4 Functional Architecture Diagram

- Component diagram showing functional modules
- Sequence diagrams for key workflows
- Use case diagrams

**Stakeholders:** Business analysts, product managers, developers

---

### 2. Information Viewpoint

**Purpose:** Describes how the system stores, manages, and distributes information

**Key Questions:**

- What data does the system manage?
- How is data structured and related?
- How does data flow through the system?
- Who owns which data?

**What to Document:**

#### 2.1 Data Models

```markdown
## Domain Model Example

### Customer Entity

- CustomerId (PK)
- Name
- Email (unique)
- MembershipLevel
- RegistrationDate

### Relationships

- Customer 1 ──── * Order
- Order 1 ──── * OrderItem
- Order 1 ──── 1 Payment

```

#### 2.2 Data Ownership

```markdown
## Data Ownership by Bounded Context

### Customer Context (Owner)

- Customer profile data
- Authentication credentials
- Membership information

### Order Context (Owner)

- Order details and status
- Order history

### Shared Data (Read-only copies)

- Order Context maintains customer name/email (eventual consistency)

```

#### 2.3 Data Flow

- How data moves between components
- Data transformation points
- Data validation rules

#### 2.4 Data Lifecycle

- Creation, update, deletion policies
- Archival and retention policies
- GDPR compliance (right to be forgotten)

#### 2.5 Data Quality & Integrity

- Validation rules
- Consistency guarantees
- Conflict resolution strategies

**Stakeholders:** Database administrators, data architects, developers

---

### 3. Concurrency Viewpoint

**Purpose:** Describes how the system handles concurrent and parallel operations

**Key Questions:**

- What operations can run in parallel?
- How is concurrency managed?
- What are the synchronization mechanisms?
- How are race conditions prevented?

**What to Document:**

#### 3.1 Concurrency Model

```markdown
## Concurrency Strategy

### Synchronous Operations

- Customer registration (immediate response)
- Payment processing (transactional)
- Order validation (must complete before payment)

### Asynchronous Operations

- Email notifications (fire-and-forget)
- Analytics collection (eventual consistency)
- Report generation (background jobs)

### Parallel Operations

- Product search across categories
- Inventory check across warehouses

```

#### 3.2 Process/Thread Structure

- Application processes and their responsibilities
- Thread pools and their configurations
- Message consumers and their concurrency

#### 3.3 Synchronization Mechanisms

```java
// Example: Distributed locking
@Transactional
public void reserveInventory(String productId, int quantity) {
    RLock lock = redissonClient.getLock("inventory:lock:" + productId);
    try {
        if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
            // Critical section
        }
    } finally {
        lock.unlock();
    }
}
```

#### 3.4 State Management

- Stateless vs stateful components
- Shared state management
- State consistency strategies

#### 3.5 Deadlock Prevention

- Lock ordering rules
- Timeout mechanisms
- Deadlock detection strategies

**Stakeholders:** Developers, performance engineers, architects

---

### 4. Development Viewpoint

**Purpose:** Describes the code organization, build process, and development environment

**Key Questions:**

- How is the code organized?
- What are the module dependencies?
- How is the system built and tested?
- What tools do developers need?

**What to Document:**

#### 4.1 Module Organization

```text
app/
├── domain/              # Domain layer (no external dependencies)
│   ├── customer/       # Customer bounded context
│   ├── order/          # Order bounded context
│   └── product/        # Product bounded context
├── application/        # Application services (use cases)
├── infrastructure/     # Infrastructure adapters
└── interfaces/         # API controllers, event handlers
```

#### 4.2 Dependency Rules

```markdown
## Layer Dependencies

- Domain layer: No dependencies on other layers
- Application layer: Depends only on domain
- Infrastructure layer: Depends on domain (via interfaces)
- Interface layer: Depends on application

## Prohibited Dependencies

- ❌ Domain → Infrastructure
- ❌ Domain → Application
- ❌ Circular dependencies between modules

```

#### 4.3 Build Process

```bash
# Build pipeline
./gradlew clean build          # Compile and package
./gradlew test                 # Run unit tests
./gradlew integrationTest      # Run integration tests
./gradlew bootJar              # Create executable JAR
./gradlew bootBuildImage       # Create Docker image
```

#### 4.4 Code Standards

- Naming conventions
- Code style guidelines
- Code review checklist
- Quality gates (coverage, complexity)

#### 4.5 Development Environment

- Required tools and versions
- Local setup instructions
- Debugging configuration

**Stakeholders:** Developers, build engineers, DevOps

---

### 5. Deployment Viewpoint

**Purpose:** Describes how the system is deployed to hardware and network infrastructure

**Key Questions:**

- What hardware/cloud resources are needed?
- How is the network configured?
- What is the deployment process?
- How does the system scale?

**What to Document:**

#### 5.1 Physical Architecture

```markdown
## AWS Infrastructure

### Compute

- EKS Cluster (Kubernetes 1.28)
- Node Group: t3.large (2 vCPU, 8 GB RAM)
- Auto-scaling: 3-10 nodes

### Database

- RDS PostgreSQL 15
- Instance: db.r6g.xlarge (4 vCPU, 32 GB RAM)
- Multi-AZ: Yes

### Cache

- ElastiCache Redis 7
- Node: cache.r6g.large (2 vCPU, 13 GB RAM)

```

#### 5.2 Network Architecture

```text
VPC: 10.0.0.0/16
├── Public Subnets (ALB, NAT Gateway)
│   ├── 10.0.1.0/24 (AZ-1)
│   ├── 10.0.2.0/24 (AZ-2)
│   └── 10.0.3.0/24 (AZ-3)
├── Private Subnets (Application)
│   ├── 10.0.11.0/24 (AZ-1)
│   ├── 10.0.12.0/24 (AZ-2)
│   └── 10.0.13.0/24 (AZ-3)
└── Private Subnets (Data)
    ├── 10.0.21.0/24 (AZ-1)
    ├── 10.0.22.0/24 (AZ-2)
    └── 10.0.23.0/24 (AZ-3)
```

#### 5.3 Deployment Process

- CI/CD pipeline
- Deployment strategy (rolling, blue-green, canary)
- Rollback procedures

#### 5.4 Environment Configuration

- Development, staging, production environments
- Configuration management
- Secrets management

#### 5.5 Scaling Strategy

- Horizontal pod autoscaling
- Cluster autoscaling
- Database scaling (read replicas)

**Stakeholders:** DevOps engineers, infrastructure architects, operations

---

### 6. Operational Viewpoint

**Purpose:** Describes how the system is installed, migrated, operated, and supported

**Key Questions:**

- How is the system installed?
- How is it monitored?
- How are backups performed?
- What are the operational procedures?

**What to Document:**

#### 6.1 Installation & Configuration

```bash
# Installation steps

1. Deploy infrastructure (CDK)
2. Configure kubectl
3. Install application (Helm)
4. Verify installation (smoke tests)

```

#### 6.2 Monitoring & Alerting

```markdown
## Key Metrics

### Business Metrics

- Orders per minute
- Revenue per hour
- Conversion rate

### Technical Metrics

- API response time (p50, p95, p99)
- Error rate (4xx, 5xx)
- Database query time
- Cache hit rate

### Alerts

- High error rate (> 10 errors in 5 min) → Critical
- High response time (p95 > 2s for 5 min) → Warning
- Database connections (> 90) → Critical

```

#### 6.3 Backup & Recovery

- Backup schedule and retention
- Recovery procedures
- RTO (Recovery Time Objective)
- RPO (Recovery Point Objective)

#### 6.4 Operational Procedures

- Startup and shutdown procedures
- Upgrade procedures
- Incident response runbooks
- Troubleshooting guides

#### 6.5 Support & Maintenance

- Log aggregation and analysis
- Performance tuning
- Capacity planning
- Patch management

**Stakeholders:** Operations team, SRE, support engineers

---

### 7. Context Viewpoint

**Purpose:** Describes the system's relationships with its environment

**Key Questions:**

- What are the system boundaries?
- What external systems does it interact with?
- Who are the stakeholders?
- What are the external constraints?

**What to Document:**

#### 7.1 System Scope & Boundaries

```markdown
## System Context

### In Scope

- Customer management
- Order processing
- Payment processing
- Inventory management

### Out of Scope

- Warehouse management (external system)
- Shipping logistics (third-party)
- Accounting (separate system)

```

#### 7.2 External Entities

```markdown
## External Systems

### Payment Gateway (Stripe)

- Protocol: REST API over HTTPS
- Authentication: API Key
- Data Exchange: JSON
- SLA: 99.9% uptime

### Email Service (SendGrid)

- Protocol: SMTP / REST API
- Authentication: API Key
- Rate Limit: 100 emails/second

### Shipping Provider (FedEx)

- Protocol: SOAP Web Service
- Authentication: OAuth 2.0
- Data Exchange: XML

```

#### 7.3 Stakeholders

```markdown
## Stakeholder Map

### Business Stakeholders

- Product Owner: Feature prioritization
- Marketing Team: Campaign requirements
- Finance Team: Reporting requirements

### Technical Stakeholders

- Development Team: Implementation
- Operations Team: Deployment and monitoring
- Security Team: Security compliance

### External Stakeholders

- Customers: End users
- Partners: Integration requirements
- Regulators: Compliance requirements

```

#### 7.4 External Constraints

- Regulatory requirements (GDPR, PCI-DSS)
- Organizational policies
- Technology standards
- Budget constraints

#### 7.5 Integration Patterns

- API integration (REST, GraphQL)
- Event-driven integration (Kafka)
- Batch integration (file transfer)
- Database integration (shared database)

**Stakeholders:** Business analysts, architects, compliance officers

---

## Perspectives (Quality Attributes)

Perspectives describe **quality attributes** that cut across multiple viewpoints.

### 1. Security Perspective

**Purpose:** Ensure the system is protected from malicious attacks and unauthorized access

**Key Questions:**

- How is authentication handled?
- How is authorization enforced?
- How is sensitive data protected?
- How are security threats mitigated?

**What to Document:**

#### 1.1 Authentication & Authorization

```markdown
## Authentication Strategy

### JWT-Based Authentication

- Token validity: 1 hour
- Refresh token: 24 hours
- Algorithm: HS512

### Authorization Model

- Role-Based Access Control (RBAC)
- Roles: ADMIN, USER, GUEST
- Permissions: READ, WRITE, DELETE

```

#### 1.2 Data Protection

```markdown
## Encryption

### Data in Transit

- TLS 1.3 for all external communication
- mTLS for service-to-service communication

### Data at Rest

- Database: AES-256 encryption
- Sensitive fields: Application-level encryption
- Secrets: AWS Secrets Manager

```

#### 1.3 Security Controls

- Input validation and sanitization
- SQL injection prevention
- XSS prevention
- CSRF protection
- Rate limiting

#### 1.4 Security Monitoring

- Failed login attempts tracking
- Suspicious activity detection
- Security event logging
- Vulnerability scanning

#### 1.5 Compliance

- GDPR compliance (data privacy)
- PCI-DSS compliance (payment data)
- SOC 2 compliance (security controls)

**Applied to Viewpoints:**

- Functional: Authentication/authorization features
- Information: Data encryption, access control
- Deployment: Network security, firewalls
- Operational: Security monitoring, incident response

---

### 2. Performance & Scalability Perspective

**Purpose:** Ensure the system meets performance requirements and can scale

**Key Questions:**

- What are the response time requirements?
- How many concurrent users can it support?
- How does it scale under load?
- What are the bottlenecks?

**What to Document:**

#### 2.1 Performance Requirements

```markdown
## Performance Targets

### API Response Time

- Critical APIs: ≤ 500ms (p95)
- Business APIs: ≤ 1000ms (p95)
- Reporting APIs: ≤ 3000ms (p95)

### Throughput

- Peak load: 1000 requests/second
- Sustained load: 500 requests/second

### Database

- Simple queries: ≤ 10ms (p95)
- Complex queries: ≤ 100ms (p95)

```

#### 2.2 Scalability Strategy

```markdown
## Horizontal Scaling

### Application Tier

- Min replicas: 3
- Max replicas: 10
- Scale trigger: CPU > 70%

### Database Tier

- Read replicas: 2
- Connection pooling: 20 connections per instance

```

#### 2.3 Performance Optimization

- Caching strategy (Redis)
- Database indexing
- Query optimization
- Asynchronous processing
- CDN for static content

#### 2.4 Performance Testing

- Load testing scenarios
- Stress testing limits
- Performance benchmarks
- Capacity planning

**Applied to Viewpoints:**

- Functional: Async operations, caching
- Information: Database optimization, indexing
- Concurrency: Parallel processing
- Deployment: Auto-scaling configuration

---

### 3. Availability & Resilience Perspective

**Purpose:** Ensure the system remains operational and recovers from failures

**Key Questions:**

- What is the uptime requirement?
- How does it handle component failures?
- What is the disaster recovery plan?
- How quickly can it recover?

**What to Document:**

#### 3.1 Availability Requirements

```markdown
## Availability Targets

### Service Level Objectives (SLO)

- Availability: 99.9% (8.76 hours downtime/year)
- RTO (Recovery Time Objective): 5 minutes
- RPO (Recovery Point Objective): 1 minute

```

#### 3.2 Fault Tolerance

```markdown
## Resilience Patterns

### Circuit Breaker

- Failure threshold: 5 failures in 10 seconds
- Open state duration: 30 seconds
- Half-open test requests: 3

### Retry Mechanism

- Max retries: 3
- Backoff: Exponential (1s, 2s, 4s)
- Retry on: Transient errors only

```

#### 3.3 High Availability Design

- Multi-AZ deployment
- Load balancing
- Health checks
- Graceful degradation
- Fallback mechanisms

#### 3.4 Disaster Recovery

```markdown
## DR Strategy

### Backup

- Database: Automated daily backups
- Retention: 30 days
- Cross-region replication: Yes

### Recovery Procedures

1. Detect failure (monitoring alerts)
2. Assess impact (runbook)
3. Execute recovery (automated failover)
4. Verify recovery (smoke tests)

```

**Applied to Viewpoints:**

- Deployment: Multi-AZ, redundancy
- Operational: Monitoring, alerting, runbooks
- Concurrency: Timeout handling, retries

---

### 4. Evolution Perspective

**Purpose:** Ensure the system can adapt to future changes

**Key Questions:**

- How easy is it to add new features?
- How can technology be upgraded?
- How is backward compatibility maintained?
- How is technical debt managed?

**What to Document:**

#### 4.1 Extensibility

```markdown
## Extension Points

### Plugin Architecture

- New payment methods via PaymentProvider interface
- New notification channels via NotificationChannel interface
- New pricing strategies via PricingStrategy interface

```

#### 4.2 Technology Evolution

```markdown
## Upgrade Strategy

### Framework Upgrades

- Spring Boot: Upgrade every 6 months
- Java: Upgrade every 2 years
- Dependencies: Monthly security updates

### Migration Path

1. Test in development environment
2. Deploy to staging
3. Run regression tests
4. Gradual rollout to production

```

#### 4.3 API Versioning

```markdown
## API Version Management

### Versioning Strategy

- URL versioning: /api/v1/, /api/v2/
- Maintain 2 versions simultaneously
- Deprecation period: 6 months

### Backward Compatibility

- Additive changes only in minor versions
- Breaking changes require new major version

```

#### 4.4 Refactoring Strategy

- Technical debt tracking
- Refactoring priorities
- Code quality metrics
- Continuous improvement

**Applied to Viewpoints:**

- Development: Modular architecture, clean code
- Functional: Plugin architecture, extension points
- Information: Schema evolution, data migration

---

### 5. Accessibility Perspective

**Purpose:** Ensure the system is usable by all users, including those with disabilities

**Key Questions:**

- Can users with disabilities use the system?
- Does it meet accessibility standards?
- Is the API easy to use?
- Is documentation clear?

**What to Document:**

#### 5.1 UI Accessibility

```markdown
## WCAG 2.1 Compliance

### Level AA Requirements

- Color contrast ratio: ≥ 4.5:1
- Keyboard navigation: Full support
- Screen reader: ARIA labels
- Focus indicators: Visible

```

#### 5.2 API Usability

```markdown
## API Design Principles

### RESTful Design

- Consistent naming conventions
- Proper HTTP methods and status codes
- Clear error messages
- Comprehensive documentation

### Error Handling
```json
{
  "errorCode": "CUSTOMER_NOT_FOUND",
  "message": "Customer with ID 123 not found",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/v1/customers/123"
}
```text

```

#### 5.3 Documentation

- API documentation (OpenAPI/Swagger)
- User guides
- Developer guides
- Troubleshooting guides

**Applied to Viewpoints:**

- Functional: User interface design
- Operational: Clear error messages, logs

---

### 6. Development Resource Perspective

**Purpose:** Ensure efficient use of development resources

**Key Questions:**

- What skills are required?
- What tools are needed?
- How is knowledge transferred?
- How is productivity measured?

**What to Document:**

#### 6.1 Team Structure
```markdown
## Team Organization

### Backend Team (5 developers)

- Skills: Java, Spring Boot, PostgreSQL, AWS
- Responsibilities: API development, business logic

### Frontend Team (3 developers)

- Skills: React, TypeScript, Next.js
- Responsibilities: UI/UX implementation

### DevOps Team (2 engineers)

- Skills: AWS, Kubernetes, Terraform, CI/CD
- Responsibilities: Infrastructure, deployment

```

#### 6.2 Required Skills

- Programming languages (Java 21, TypeScript)
- Frameworks (Spring Boot, React)
- Cloud platforms (AWS)
- Tools (Git, Docker, Kubernetes)

#### 6.3 Development Tools

```markdown
## Toolchain

### Development

- IDE: IntelliJ IDEA / VS Code
- Version Control: Git + GitHub
- Build: Gradle 8.x

### Testing

- Unit: JUnit 5, Mockito
- Integration: Testcontainers
- BDD: Cucumber

### CI/CD

- Pipeline: GitHub Actions
- Deployment: ArgoCD
- Monitoring: CloudWatch, Grafana

```

#### 6.4 Knowledge Management

- Code documentation
- Architecture decision records (ADRs)
- Onboarding guides
- Pair programming
- Code reviews

**Applied to Viewpoints:**

- Development: Build tools, code standards
- Operational: Runbooks, troubleshooting guides

---

### 7. Internationalization Perspective

**Purpose:** Ensure the system supports multiple languages and regions

**Key Questions:**

- What languages are supported?
- How are dates/times/currencies handled?
- How is content localized?
- What are the cultural considerations?

**What to Document:**

#### 7.1 Language Support

```markdown
## Supported Languages

### Phase 1 (Launch)

- English (US)
- Traditional Chinese (Taiwan)
- Simplified Chinese (China)

### Phase 2 (6 months)

- Japanese
- Korean

```

#### 7.2 Localization

```markdown
## Localization Strategy

### Text Translation

- i18n framework: Spring MessageSource
- Translation files: messages_en.properties, messages_zh_TW.properties
- Fallback: English

### Date/Time

- Format: ISO 8601
- Timezone: User's local timezone
- Display: Localized format (MM/DD/YYYY vs DD/MM/YYYY)

### Currency

- Storage: USD (base currency)
- Display: User's local currency
- Exchange rates: Daily update

```

#### 7.3 Cultural Adaptation

- Color meanings (red = luck in China, danger in US)
- Icon appropriateness
- Content sensitivity
- Legal requirements per region

**Applied to Viewpoints:**

- Functional: Multi-language UI
- Information: Unicode support, locale data
- Deployment: Region-specific deployments

---

### 8. Location Perspective

**Purpose:** Ensure the system serves users across different geographic locations

**Key Questions:**

- Where are users located?
- How is latency minimized?
- Where is data stored?
- How is data replicated?

**What to Document:**

#### 8.1 Geographic Distribution

```markdown
## Multi-Region Deployment

### Primary Region: US East (N. Virginia)

- Application servers
- Primary database
- Main user base: North America

### Secondary Region: EU West (Ireland)

- Application servers
- Read replica database
- Main user base: Europe

### Tertiary Region: AP Southeast (Singapore)

- Application servers
- Read replica database
- Main user base: Asia Pacific

```

#### 8.2 Data Residency

```markdown
## Data Location Requirements

### GDPR Compliance

- EU customer data: Stored in EU region only
- Data transfer: Prohibited outside EU

### China Data Localization

- China customer data: Stored in China region
- Separate deployment in China cloud

```

#### 8.3 Latency Optimization

```markdown
## Performance by Region

### CDN Strategy

- Static content: CloudFront edge locations
- API Gateway: Regional endpoints
- Database: Read replicas in each region

### Target Latency

- Same region: < 50ms
- Cross region: < 200ms
- Global average: < 150ms

```

#### 8.4 Disaster Recovery

- Cross-region replication
- Failover procedures
- Data consistency across regions

**Applied to Viewpoints:**

- Deployment: Multi-region infrastructure
- Information: Data replication, consistency
- Operational: Regional monitoring

---

## How to Use This Guide

### For New Projects

**Step 1: Start with Context Viewpoint**

- Define system boundaries
- Identify stakeholders
- Document external systems

**Step 2: Define Functional Viewpoint**

- Identify bounded contexts
- Define use cases
- Design functional architecture

**Step 3: Design Information Viewpoint**

- Create domain model
- Define data ownership
- Plan data flow

**Step 4: Address Other Viewpoints**

- Concurrency (if needed)
- Development (always)
- Deployment (always)
- Operational (always)

**Step 5: Apply Perspectives**

- Security (always)
- Performance (always)
- Availability (always)
- Evolution (always)
- Others (as needed)

### For Existing Projects

**Step 1: Assess Current Documentation**

- What viewpoints are covered?
- What perspectives are addressed?
- What gaps exist?

**Step 2: Prioritize Gaps**

- Critical: Security, Availability
- Important: Performance, Evolution
- Nice-to-have: Accessibility, Internationalization

**Step 3: Document Incrementally**

- Start with most critical viewpoint/perspective
- Add one section at a time
- Review and refine

### Documentation Templates

Each viewpoint/perspective document should include:

```markdown
# [Viewpoint/Perspective Name]

## Overview

- Purpose of this viewpoint/perspective
- Key stakeholders

## Current State

- What exists today

## Concerns & Requirements

- What needs to be addressed

## Design Decisions

- How concerns are addressed
- Rationale for decisions

## Diagrams

- Visual representations

## Risks & Trade-offs

- Known limitations
- Mitigation strategies

## Related Documents

- Links to other viewpoints/perspectives

```

---

## Practical Examples

### Example 1: E-Commerce Platform

**Functional Viewpoint:**

- Customer Management
- Product Catalog
- Order Processing
- Payment Processing

**Security Perspective Applied:**

- Customer Management: JWT authentication, password hashing
- Payment Processing: PCI-DSS compliance, encryption
- All modules: HTTPS, input validation

### Example 2: Microservices Architecture

**Development Viewpoint:**

- Service per bounded context
- Shared libraries for common code
- Independent deployment

**Evolution Perspective Applied:**

- API versioning strategy
- Service contract testing
- Backward compatibility rules

### Example 3: Global SaaS Application

**Deployment Viewpoint:**

- Multi-region AWS deployment
- Regional databases
- Global load balancing

**Location Perspective Applied:**

- Data residency compliance
- CDN for static content
- Regional failover

---

## References

### Books

- **"Software Systems Architecture"** by Nick Rozanski and Eoin Woods (2nd Edition)
  - The definitive guide to this methodology

### Online Resources

- [Rozanski & Woods Website](http://www.viewpoints-and-perspectives.info/)
- [Architecture Viewpoints](https://en.wikipedia.org/wiki/4%2B1_architectural_view_model)

### Related Methodologies

- **C4 Model**: Complementary approach for diagrams
- **Arc42**: Alternative documentation template
- **ISO/IEC/IEEE 42010**: International standard for architecture description

### Project-Specific Documents

- [Development Standards](../.kiro/steering/development-standards.md)
- [Security Standards](../.kiro/steering/security-standards.md)
- [Performance Standards](../.kiro/steering/performance-standards.md)
- [Rozanski & Woods Architecture Methodology](../.kiro/steering/rozanski-woods-architecture-methodology.md)

---

## Conclusion

The Rozanski & Woods methodology provides a comprehensive framework for documenting software architecture. By systematically addressing all viewpoints and perspectives, you ensure:

✅ **Complete Coverage**: No architectural aspect is overlooked  
✅ **Stakeholder Alignment**: Each stakeholder gets relevant information  
✅ **Quality Assurance**: Quality attributes are explicitly addressed  
✅ **Maintainability**: Clear structure makes updates easier  
✅ **Traceability**: Links requirements to architecture to implementation  

**Remember:**

- Not every project needs all viewpoints/perspectives
- Start with the most critical ones
- Document incrementally
- Keep documentation up-to-date
- Use diagrams to complement text

**Next Steps:**

1. Identify which viewpoints/perspectives are most critical for your project
2. Create documentation structure
3. Start documenting incrementally
4. Review and refine regularly

---

*Last Updated: 2025-01-17*  
*Version: 1.0*  
*Maintained by: Architecture Team*
