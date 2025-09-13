# GenAI Demo - Enterprise E-commerce Platform Demonstration Project

> **Language / 語言選擇**  
> 🇺🇸 **English**: You are reading the English version  
> 🇹🇼 **繁體中文**: [Chinese Documentation](../../README.md) | [Chinese Project README](../../README.md)

A full-stack e-commerce platform based on DDD + Hexagonal Architecture, integrating enterprise-level observability, AI-assisted development, and cloud-native deployment in a modern application.

## 🌟 Project Highlights

### 🏗️ Enterprise-Level Architecture Design

- **DDD + Hexagonal Architecture**: Aggregate Root + Value Object + Domain Event + Specification Pattern + Policy Pattern
- **Event-Driven Design**: Complete event collection, publishing, and processing mechanisms
- **Java 21 Record**: Reduces 30-40% boilerplate code, improves type safety

### 📊 Complete Observability System (v3.1.0 NEW!)

- **Distributed Tracing**: AWS X-Ray + Jaeger cross-service request tracing
- **Structured Logging**: Unified format + Correlation ID + PII masking
- **Business Metrics**: CloudWatch custom metrics + Prometheus endpoints
- **Cost Optimization**: Resource right-sizing analysis + Real-time cost tracking

### 🤖 AI-Assisted Development (MCP Integration)

- **AWS Ecosystem**: Documentation queries + CDK guidance + Pricing analysis + IAM management
- **GitHub Integration**: Code review + Issue tracking + PR management
- **Intelligent Assistant**: Well-Architected reviews + Architecture decision support

### 🛒 Dual Frontend Business Features

**Consumer Side**: Smart shopping cart + Personalized recommendations + Member rewards + Delivery tracking  
**Business Side**: Promotion management + Inventory management + Order processing + Statistical analysis

### 🧪 Testing & Quality Assurance

- **Test-Driven**: BDD + TDD + Architecture tests, 272 tests 100% pass
- **Architecture Compliance**: 9.5/10 (Hexagonal Architecture) + 9.5/10 (DDD Practices)
- **Cloud-Native Deployment**: AWS CDK + Kubernetes + GitOps

## 🚀 Quick Start

### Memory and Performance Optimization (v3.0.1 New)

This project has been optimized for memory usage during compilation and testing:

#### 🔧 Optimization Configuration

- **Memory Configuration**: Compilation and testing maximum heap memory increased to 4GB
- **Log Optimization**: Only ERROR level logs output during testing, significantly reducing output volume
- **JVM Optimization**: Uses G1 garbage collector and string deduplication optimization
- **Parallel Processing**: Optimized Gradle parallel execution configuration

#### 🛠️ Optimized Execution Scripts

```bash
# Optimized compilation (reduced log output, increased memory)
./scripts/build-optimized.sh

# Optimized test execution (only show error logs)
./scripts/run-tests-optimized.sh

# Memory usage monitoring
./scripts/monitor-memory.sh

# System resource check
./scripts/check-system-resources.sh
```

#### 🔍 Observability Endpoints

```bash
# Application monitoring
curl http://localhost:8080/actuator/health     # Health check
curl http://localhost:8080/actuator/metrics    # Application metrics
curl http://localhost:8080/actuator/info       # Application info

# Cost optimization API
curl http://localhost:8080/api/cost-optimization/recommendations  # Cost recommendations
curl http://localhost:8080/api/cost-optimization/analysis         # Cost analysis
```

### Method 1: Docker Containerized Deployment (Recommended)

```bash
# Build ARM64 optimized images
./docker/docker-build.sh

# Start containerized environment
docker-compose up -d

# Check service status
docker-compose ps

# Stop all services
docker-compose down
```

**Service Endpoints:**

- 🌐 **API Documentation**: <http://localhost:8080/swagger-ui/index.html>
- 🏥 **Health Check**: <http://localhost:8080/actuator/health>
- 📊 **Application Metrics**: <http://localhost:8080/actuator/metrics>
- 💰 **Cost Optimization**: <http://localhost:8080/api/cost-optimization/recommendations>
- 🗄️ **H2 Database Console**: <http://localhost:8080/h2-console>

### Method 2: Local Development Environment

```bash
# Start complete frontend and backend applications
./scripts/start-fullstack.sh

# Stop all services
./scripts/stop-fullstack.sh
```

### Method 3: Start Services Individually

```bash
# Start backend only (Spring Boot)
./gradlew :app:bootRun

# Start frontend only (Next.js)
cd cmc-frontend && npm run dev
```

## 🏗️ Architecture Design

### Hexagonal Architecture + DDD Layers

```text
interfaces/ → application/ → domain/ ← infrastructure/
```

- **Domain Layer**: Business logic + Aggregate Root + Value Object + Domain Event + Specification Pattern
- **Application Layer**: Use case coordination + Event publishing + Cross-aggregate operations
- **Infrastructure Layer**: Persistence + External systems + Event handling
- **Interface Layer**: REST API + OpenAPI 3.0 + Swagger UI

## 📁 Project Directory Structure

```text
genai-demo/
├── app/                    # Main application
│   ├── src/main/java/      # Java source code
│   └── src/test/java/      # Test code
├── cmc-frontend/           # Next.js 14.2.30 frontend application (CMC)
├── consumer-frontend/      # Angular 18.2.0 frontend application (Consumer)
├── deployment/             # Deployment related files
│   ├── k8s/               # Kubernetes configuration
│   └── deploy-to-eks.sh   # EKS deployment script
├── docker/                 # Docker related files
│   ├── docker-build.sh    # Image build script
│   └── verify-deployment.sh # Deployment verification script
├── docs/                   # Project documentation
│   ├── api/               # API documentation
│   ├── en/                # English documentation
│   ├── architecture/      # Architecture documentation
│   ├── diagrams/          # Diagram documentation (Mermaid + PlantUML)
│   └── reports/           # Project reports
├── scripts/                # Various script files
│   ├── start-fullstack.sh # Start full-stack application
│   └── stop-fullstack.sh  # Stop all services
├── tools/                  # Development tools
│   └── plantuml.jar       # UML diagram generation tool
├── docker-compose.yml      # Docker Compose configuration
├── Dockerfile             # Docker image definition
└── README.md              # Project documentation
```

### Dual Frontend Architecture

**CMC Management Side (Next.js 14.2.30)**  
TypeScript + Tailwind CSS + shadcn/ui + Zustand + React Query

**Consumer Side (Angular 18.2.0)**  
TypeScript + Tailwind CSS + PrimeNG + RxJS + Jasmine

## 🆕 Version Updates (v3.2.0 - September 2025)

### 🚀 Major New Features

- ✅ **Production-Ready Observability**: Complete production environment testing strategy and industry best practices guide
- ✅ **Architecture Decision Records**: Complete Chinese and English ADR documentation, recording all important architecture decisions
- ✅ **Test System Optimization**: 568 tests 100% pass, removed impractical BDD tests
- ✅ **Documentation System Improvement**: 67-page observability guide, covering script-based testing to disaster recovery

### 📈 Architecture & Quality Improvements

- 🏗️ **Architecture Decision Records**: 7 complete ADR documents covering all important architecture decisions
- 🔧 **Test Strategy Optimization**: From theoretical BDD to practical production environment testing methods
- 🧪 **Test Stability**: 568 tests 100% pass rate, zero failed tests
- 📚 **Documentation Internationalization**: Complete Chinese and English documentation system, supporting multilingual teams

## 🛠️ Technology Stack

### Backend Technologies

- **Core Framework**: Spring Boot 3.4.5
- **Programming Language**: Java 21 (with preview features enabled)
- **Build Tool**: Gradle 8.x
- **Database**: H2 (development) + PostgreSQL (production) + Flyway (migration management)
- **API Documentation**: SpringDoc OpenAPI 3 + Swagger UI
- **Observability**:
  - Micrometer - Metrics collection
  - AWS X-Ray - Distributed tracing
  - Logback - Structured logging
  - Spring Boot Actuator - Health checks
- **Testing Framework**:
  - JUnit 5 - Unit testing
  - Cucumber 7.15.0 - BDD testing
  - ArchUnit 1.3.0 - Architecture testing
  - Mockito 5.8.0 - Mock objects
  - Allure 2.22.1 - Test reporting and visualization
- **Other Tools**:
  - Lombok 1.18.38 - Reduce boilerplate code
  - PlantUML - UML diagram generation

### Frontend Technologies

**CMC Management Side**: Next.js 14.2.30 + TypeScript + Tailwind + shadcn/ui + Zustand + React Query  
**Consumer Side**: Angular 18.2.0 + TypeScript + Tailwind + PrimeNG + RxJS + Jasmine

## 📊 Data & API

### Database Initialization

The project uses Flyway for database version management, including rich business test data:

- **100+ Product inventory records** - Covering electronics, clothing, home goods, etc.
- **Complete order process data** - Orders, order items, payment records
- **Taiwan localized data** - Real addresses, Traditional Chinese product names
- **Multiple payment methods** - Credit cards, digital wallets, bank transfers, cash on delivery
- **Independent product table** - Supporting complete product lifecycle management

### API Documentation & Endpoints

#### 📖 Swagger UI Documentation

- **Swagger UI**: <http://localhost:8080/swagger-ui/index.html>
- **OpenAPI Specification**: <http://localhost:8080/v3/api-docs>
- **API Groups**:
  - Public API: `/v3/api-docs/public-api`
  - Internal API: `/v3/api-docs/internal-api`
  - Management Endpoints: `/v3/api-docs/management`

#### 🔧 Main API Endpoints

```bash
# Product Management API
GET /api/products                 # Product list (with pagination support)
GET /api/products/{productId}     # Get single product
PUT /api/products/{productId}     # Update product information
DELETE /api/products/{productId}  # Delete product
POST /api/products                # Create new product

# Inventory Management API
GET /api/inventory/{productId}    # Get product inventory
POST /api/inventory/{productId}/adjust  # Adjust inventory
POST /api/inventory/{productId}/reserve # Reserve inventory
POST /api/inventory/{productId}/release # Release inventory

# Order Management API
GET /api/orders                   # Order list
GET /api/orders/{orderId}         # Get single order
POST /api/orders                  # Create new order
PUT /api/orders/{orderId}         # Update order

# Payment Management API
POST /api/payments                # Create payment
GET /api/payments/{paymentId}     # Get payment details
PUT /api/payments/{paymentId}     # Update payment status

# Cost Optimization API
GET /api/cost-optimization/recommendations  # Get cost optimization recommendations
GET /api/cost-optimization/analysis         # Get cost analysis report
POST /api/cost-optimization/right-sizing    # Execute resource right-sizing analysis

# Customer Management API
GET /api/customers                # Customer list
GET /api/customers/{customerId}   # Get customer details

# Pricing Management API
GET /api/pricing/rules            # Get pricing rules
POST /api/pricing/rules           # Create pricing rules
PUT /api/pricing/commission-rates # Update commission rates

# Data Statistics API
GET /api/stats                    # Overall data statistics
GET /api/stats/order-status       # Order status distribution
GET /api/stats/payment-methods    # Payment method distribution
GET /api/stats/database           # Database statistics

# Activity Log API
GET /api/activities               # System activity logs

# Health Check & Monitoring
GET /actuator/health              # Application health status
GET /actuator/metrics             # Application metrics
GET /actuator/info                # Application information
GET /actuator/prometheus          # Prometheus metrics

# H2 Database Console
http://localhost:8080/h2-console  # Database management interface
```

## 📱 Frontend Features

### Main Pages

- **Dashboard** (`/`) - System overview and statistics
- **Order Management** (`/orders`) - Order list and details
  - Order Details Page (`/orders/[orderId]`) - Complete order information display
- **Product Management** (`/products`) - Product display and inventory
  - Product Details Page (`/products/[productId]`) - Detailed product information and operations
  - Product Edit Function - Support name, description, price, category modification
  - Inventory Adjustment Function - Support add, reduce, set inventory quantity
  - Product Delete Function - Safe product deletion operation
- **Customer Management** (`/customers`) - Customer information management

### Product Management Features

- ✏️ **Product Editing** - Complete product information editing interface
  - Product name, description modification
  - Price and currency settings (TWD, USD, EUR)
  - Product category management (Electronics, Clothing, Food, etc.)
- 📦 **Inventory Management** - Flexible inventory adjustment system
  - Set Inventory - Directly set inventory quantity
  - Add Inventory - Restock inventory
  - Reduce Inventory - Handle damage or returns
  - Adjustment Reason Recording - Complete inventory change tracking
- 🗑️ **Product Deletion** - Safe product deletion function
  - Confirmation dialog to prevent accidental deletion
  - Automatic product list updates

### UI/UX Features

- 🎨 Modern design system (shadcn/ui + Tailwind CSS)
- 📱 Fully responsive design
- 🌙 Dark/Light theme support
- ⚡ Real-time data updates (React Query)
- 🔄 Loading states and error handling
- 📊 Data visualization charts
- 🎯 Intuitive operation interface
- 📝 Form validation and user feedback
- 🔔 Toast notification system

## 📚 Documentation

> **Documentation Center**: [docs/README.md](docs/README.md) - Complete documentation navigation and categorization

The project includes rich documentation, organized by functional categories:

### 🎯 Quick Navigation

- **👨‍💼 Project Manager**: [Project Summary 2025](docs/reports/project-summary-2025.md) | [Architecture Overview](docs/diagrams/mermaid/architecture-overview.md)
- **🏗️ Architect**: [Architecture Decision Records](docs/architecture/adr/) | [Architecture Documentation](docs/architecture/) | [Diagram Documentation](docs/diagrams/)
- **👨‍💻 Developer**: [Development Guide](docs/development/) | [API Documentation](docs/api/) | [Development Instructions](docs/development/instructions.md)
- **🚀 DevOps**: [Deployment Documentation](docs/deployment/) | [Docker Guide](docs/deployment/docker-guide.md)
- **🔍 Observability**: [Production Environment Testing Guide](docs/observability/production-observability-testing-guide.md) | [Observability System](docs/observability/)
- **🤖 MCP Integration**: [MCP Guide](docs/mcp/) | [AI-Assisted Development](docs/mcp/README.md)

### 📊 Core Diagrams (Mermaid - Direct GitHub Display)

- [🏗️ System Architecture Overview](docs/diagrams/mermaid/architecture-overview.md) - Complete system architecture diagram
- [🔵 Hexagonal Architecture](docs/diagrams/mermaid/hexagonal-architecture.md) - Ports and Adapters architecture
- [🏛️ DDD Layered Architecture](docs/diagrams/mermaid/ddd-layered-architecture.md) - Domain-Driven Design layers
- [⚡ Event-Driven Architecture](docs/diagrams/mermaid/event-driven-architecture.md) - Event processing mechanisms
- [🔌 API Interaction Diagram](docs/diagrams/mermaid/api-interactions.md) - API call relationships

### 📋 Detailed UML Diagrams (PlantUML)

- **Structural Diagrams**: Class diagrams, Object diagrams, Component diagrams, Deployment diagrams, Package diagrams, Composite structure diagrams
- **Behavioral Diagrams**: Use case diagrams, Activity diagrams, State diagrams
- **Interaction Diagrams**: Sequence diagrams, Communication diagrams, Interaction overview diagrams, Timing diagrams
- **Event Storming**: Big Picture, Process Level, Design Level

### 🏆 Core Reports (September 2025 Update)

- [📋 Project Summary Report 2025](docs/reports/project-summary-2025.md) - Complete project achievements and technical highlights summary
- [🏗️ Architecture Excellence Report 2025](docs/reports/architecture-excellence-2025.md) - Detailed architecture assessment and best practices analysis
- [🚀 Technology Stack Detailed Description 2025](docs/reports/technology-stack-2025.md) - Complete technology selection and implementation details
- [📝 Documentation Cleanup Report 2025](docs/reports/documentation-cleanup-2025.md) - Documentation reorganization and optimization records

### 🛠️ Diagram Generation Tools

```bash
# Generate all PlantUML diagrams
./scripts/generate-diagrams.sh

# Generate specific diagrams
./scripts/generate-diagrams.sh domain-model-class-diagram.puml

# Validate diagram syntax
./scripts/generate-diagrams.sh --validate
```

## 🧪 Testing

### Run All Tests

```bash
./gradlew runAllTests                    # Run all tests (272 tests)
./gradlew runAllTestsWithReport         # Run tests and generate Allure report
./gradlew runAllTestsComplete           # Run complete test suite
```

### Run Specific Test Types

```bash
./gradlew test                          # Unit tests (JUnit 5)
./gradlew cucumber                      # BDD tests (Cucumber 7.15.0)
./gradlew testArchitecture             # Architecture tests (ArchUnit 1.3.0)
```

### Test Reports

- **Cucumber HTML Report**: `app/build/reports/cucumber/cucumber-report.html`
- **JUnit HTML Report**: `app/build/reports/tests/test/index.html`
- **Allure Report**: `app/build/reports/allure-report/allureReport/index.html`
- **Allure Results Directory**: `app/build/allure-results/`

### Architecture Testing

Using ArchUnit 1.3.0 to ensure code follows predefined architecture rules:

- **DddEntityRefactoringArchitectureTest** - Ensures DDD entity refactoring complies with architecture specifications
- **Hexagonal Architecture Compliance** - Ensures ports and adapters separation
- **DDD Tactical Patterns** - Ensures correct use of Aggregate Root, Value Object, Domain Event, Specification Pattern, Policy Pattern
- **Package Structure Standards** - Ensures package structure follows DDD layered architecture
- **Annotation Validation** - Ensures correct use of `@AggregateRoot`, `@ValueObject`, `@Specification`, `@Policy` annotations

### BDD Testing

Using Cucumber 7.15.0 for Behavior-Driven Development testing, covering:

- **Consumer Features** (Consumer) - Shopping journey, shopping cart management
- **Customer Management** (Customer) - Member system, loyalty points, member discounts
- **Order Management** (Order) - Order aggregate root, order workflow
- **Inventory Management** (Inventory) - Inventory management
- **Payment Processing** (Payment) - Payment aggregate root, payment discounts
- **Logistics Delivery** (Logistics) - Delivery management, delivery system
- **Notification Service** (Notification) - Notification management, notification service
- **Promotion Activities** (Promotion) - Coupon system, flash sales, convenience store coupons, add-on activities, gift activities
- **Product Management** (Product) - Product search, product combinations
- **Pricing Management** (Pricing) - Commission rates
- **Complete Workflow** (Workflow) - End-to-end business processes

## 🔧 Development Tools

### Data Generation

```bash
python3 scripts/generate_data.py       # Generate large amounts of test data
```

### Service Management

```bash
./scripts/start-fullstack.sh           # Start full-stack application
./scripts/stop-fullstack.sh            # Stop all services
```

### Frontend Development

#### CMC Frontend (Next.js)

```bash
cd cmc-frontend
npm install                             # Install dependencies
npm run dev                            # Development mode (http://localhost:3002)
npm run build                          # Production build
npm run lint                           # Code linting
npm run type-check                     # TypeScript type checking
npm test                               # Run tests
npm run test:e2e                       # E2E tests (Playwright)
```

#### Consumer Frontend (Angular)

```bash
cd consumer-frontend
npm install                             # Install dependencies
npm start                              # Development mode (http://localhost:3001)
npm run build                          # Production build
npm test                               # Run tests (Jasmine + Karma)
```

## 🎯 UML Diagrams

This project uses PlantUML to generate various UML diagrams:

- Class diagrams, Object diagrams, Component diagrams, Deployment diagrams
- Sequence diagrams (Order processing, Pricing processing, Delivery processing)
- State diagrams, Activity diagrams
- Domain model diagrams, Hexagonal architecture diagrams, DDD layered architecture diagrams

See [Diagram Documentation](docs/diagrams/README.md) for more information.

## 🚨 Common Issues

### Configuration Cache Issues

```bash
./gradlew --no-configuration-cache <task>
```

### Allure Report Issues

```bash
./gradlew clean
./gradlew runAllTestsWithReport
```

### Frontend Dependency Issues

```bash
cd cmc-frontend
rm -rf node_modules package-lock.json
npm install
```

## 🤝 Contributing

Pull Requests and Issues for improvement suggestions are welcome.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🔗 Related Links

- **DeepWiki Integration**: [![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/humank/genai-demo)
- **Consumer Frontend**: <http://localhost:3001> (Development mode)
- **CMC Frontend**: <http://localhost:3002> (Development mode)
- **Backend API**: <http://localhost:8080>
- **Swagger UI**: <http://localhost:8080/swagger-ui/index.html>
- **OpenAPI Specification**: <http://localhost:8080/v3/api-docs>
- **H2 Console**: <http://localhost:8080/h2-console>

---

## 📊 Project Statistics & Value

### 📈 Core Data

- **Code Scale**: 200,000+ lines of high-quality code
- **Test Quality**: 568 tests, 100% pass rate
- **API Coverage**: 35+ RESTful API endpoints
- **UI Components**: 25+ reusable components (React + Angular)
- **Documentation Completeness**: 100+ detailed documentation pages, including 67-page production environment guide
- **Architecture Decisions**: 7 complete ADR documents covering all important architecture decisions
- **Database**: 131 business records + 22 Flyway migration scripts

### 🏆 Technical Value

- **Architecture Excellence**: DDD + Hexagonal Architecture + Event-Driven Design, complete ADR documentation
- **Quality Assurance**: Practical testing strategy + Production environment best practices + Complete architecture test coverage
- **Modern Technology Stack**: Java 21 + Spring Boot 3.4.5 + Next.js 14.2.30 + Angular 18.2.0
- **Enterprise Features**: Production-ready observability + AI-assisted development + Cloud-native deployment
- **Best Practices**: Industry-standard testing methods + Complete documentation system, suitable for learning modern enterprise application development
