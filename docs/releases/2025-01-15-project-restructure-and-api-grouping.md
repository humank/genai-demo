# Project Restructure and API Grouping Optimization - 2025-01-15

## 📋 Change Overview

This update focuses on project structure reorganization and API grouping strategy optimization to improve project maintainability and API documentation user experience.

## 🔄 Major Changes

### 1. Project File Structure Reorganization

#### 📁 New Directory Structure

```
genai-demo/
├── docker/                 # Docker-related files
│   ├── docker-build.sh    # ARM64 image build script
│   ├── verify-deployment.sh # Deployment verification script
│   └── postgres/           # PostgreSQL initialization scripts
├── deployment/             # Deployment-related files
│   ├── k8s/               # Kubernetes configurations
│   ├── deploy-to-eks.sh   # EKS deployment script
│   └── aws-eks-architecture.md
├── scripts/                # Various script files
│   ├── start-fullstack.sh # Start full-stack application
│   ├── stop-fullstack.sh  # Stop all services
│   ├── test-api.sh        # API testing script
│   ├── verify-swagger-ui.sh # Swagger UI verification
│   └── generate_data.py   # Test data generation
├── tools-and-environment/  # Development tools
│   └── plantuml.jar       # UML diagram generation tool
└── docs/                   # Project documentation (expanded)
    ├── api/               # API-related documentation
    ├── releases/          # Version release records
    └── ...
```

#### 🗂️ Moved Files

- **Docker-related**: `docker-build.sh`, `verify-deployment.sh` → `docker/`
- **Deployment-related**: `deploy-to-eks.sh`, `k8s/`, `aws-eks-architecture.md` → `deployment/`
- **Script files**: `start-fullstack.sh`, `stop-fullstack.sh`, `test-api.sh`, `verify-swagger-ui.sh`, `generate_data.py` → `scripts/`
- **Tool files**: `plantuml.jar` → `tools-and-environment/`
- **Documentation files**: Various `.md` files → `docs/`

### 2. API Grouping Strategy Redesign

#### 🎯 DDD and User Role-Based Grouping

**Old Grouping Approach**:

- `public-api`: All public APIs
- `internal-api`: Internal APIs
- `management`: Management endpoints

**New Grouping Approach**:

- `customer-api`: Customer APIs (for end customers)
- `operator-api`: Operations Management APIs (for platform operators)
- `system-api`: System Management APIs (for system administrators)

#### 📊 Detailed Grouping Description

##### Customer API (`customer-api`)

**Target Users**: End Customers
**Included Paths**:

- `/api/products/**` - Product browsing
- `/api/orders/**` - Personal order queries
- `/api/payments/**` - Payment processing
- `/api/consumer/**` - Consumer functions
- `/api/shopping-cart/**` - Shopping cart
- `/api/promotions/**` - Promotional activities
- `/api/vouchers/**` - Vouchers
- `/api/reviews/**` - Product reviews
- `/api/recommendations/**` - Personalized recommendations
- `/api/notifications/**` - Personal notifications
- `/api/delivery-tracking/**` - Delivery tracking

##### Operations Management API (`operator-api`)

**Target Users**: Platform Operators/Admins
**Included Paths**:

- `/api/customers/**` - Customer management
- `/api/orders/**` - Platform-wide order management
- `/api/products/**` - Product management (CRUD)
- `/api/inventory/**` - Inventory management
- `/api/pricing/**` - Pricing strategies
- `/api/payments/**` - Payment management
- `/api/activities/**` - System activity records
- `/api/stats/**` - Statistical reports
- `/api/admin/**` - Admin-specific functions

##### System Management API (`system-api`)

**Target Users**: System Administrators, DevOps
**Included Paths**:

- `/api/internal/**` - Internal system integration
- `/api/management/**` - System management functions
- `/actuator/**` - Spring Boot Actuator

### 3. OpenAPI Tag Optimization

#### 🏷️ New Tag System

**Customer Tags** (Chinese naming for better intuition):

- Product Browsing, Shopping Cart, Order Query, Payment Processing
- Promotional Activities, Product Reviews, Personalized Recommendations
- Notification Center, Delivery Tracking

**Operations Management Tags**:

- Customer Management, Order Management, Product Management
- Inventory Management, Pricing Management, Payment Management
- Statistical Reports, Activity Records

**System Management Tags**:

- System Monitoring, Internal Integration

### 4. Domain Model Enhancement

#### 🏗️ DDD Architecture Enhancement

- **Promotion Rules Engine**: Complete implementation of sealed interface promotion rules system
- **Shopping Cart Aggregate**: Implementation of complete shopping cart business logic and discount calculation
- **Customer Aggregate**: Enhanced customer model supporting loyalty points and notification preferences
- **Order Aggregate**: Improved order lifecycle management and state transitions

#### 🧪 Testing System Establishment

- **BDD Testing**: Implementation of behavior-driven testing for consumer shopping processes
- **Architecture Testing**: Using ArchUnit to ensure DDD architecture compliance
- **Integration Testing**: Complete API endpoint test coverage
- **Unit Testing**: Unit tests for domain logic and business rules

## 🔧 Technical Improvements

### Docker Optimization

- **ARM64 Native Support**: Optimized for Apple Silicon and ARM64 servers
- **Multi-stage Build**: Minimized final image size
- **JVM Tuning**: Container environment-specific JVM parameters
- **Health Checks**: Complete application monitoring mechanisms

### Development Experience Enhancement

- **Directory Documentation**: Each new directory includes README.md descriptions
- **Script Execution Permissions**: Automatic setting of execution permissions for all scripts
- **Path Reference Updates**: Updated file path references in all documentation

## 📱 Usage Updates

### New Script Paths

```bash
# Docker-related
./docker/docker-build.sh
./docker/verify-deployment.sh

# Full-stack application
./scripts/start-fullstack.sh
./scripts/stop-fullstack.sh

# Testing and verification
./scripts/test-api.sh
./scripts/verify-swagger-ui.sh

# Data generation
python3 scripts/generate_data.py

# Deployment
./deployment/deploy-to-eks.sh

# Tools
java -jar tools-and-environment/plantuml.jar docs/uml/*.puml
```

### API Documentation Access

```bash
# Customer API documentation
http://localhost:8080/swagger-ui/index.html?configUrl=/v3/api-docs/swagger-config#/customer-api

# Operations Management API documentation  
http://localhost:8080/swagger-ui/index.html?configUrl=/v3/api-docs/swagger-config#/operator-api

# System Management API documentation
http://localhost:8080/swagger-ui/index.html?configUrl=/v3/api-docs/swagger-config#/system-api
```

## 🎯 Impact and Benefits

### Development Efficiency Improvement

- **Clear Directory Structure**: Developers can quickly find relevant files
- **Functional Grouping**: Related functions centrally managed, reducing maintenance costs
- **Documentation Completeness**: Each directory has detailed usage instructions

### API User Experience Improvement

- **Role-oriented Grouping**: Different users only see relevant APIs
- **Chinese Tags**: More intuitive API categorization and descriptions
- **Smart Path Matching**: Automatically categorizes APIs into correct groups

### Architecture Quality Enhancement

- **DDD Compliance**: Architecture tests ensure design principles
- **Test Coverage**: Multi-level testing strategy ensures code quality
- **Containerization Optimization**: ARM64 native support and performance tuning

## 🔄 Migration Guide

### Impact on Existing Users

1. **Script Path Changes**: Need to update path references in CI/CD scripts
2. **API Documentation Access**: Can use new grouped URLs for better experience
3. **Development Tools**: PlantUML and other tool paths have changed

### Recommended Migration Steps

1. Update path references in local scripts
2. Rebuild Docker images to get latest optimizations
3. Use new API grouping URLs to access documentation
4. Check path references in CI/CD processes

## 📚 Related Documentation

- [Docker Deployment Guide](../deployment/README.md)
- API Version Management Strategy
- Project Directory Structure Description
- [Development Environment Setup](../development/getting-started.md)

---

**Release Date**: 2025-01-15  
**Version**: v2.0.0  
**Impact Scope**: Project structure, API documentation, development tools
