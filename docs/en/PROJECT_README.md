# GenAI Demo - E-commerce Platform Demo Project

> **Language / 語言選擇**  
> 🇺🇸 **English**: You are reading the English version  
> 🇹🇼 **繁體中文**: [Chinese Documentation](../../README.md) | [Chinese Project README](../../README.md)

This is a full-stack e-commerce platform demo project based on Domain-Driven Design (DDD) and Hexagonal Architecture, demonstrating how to build a modern application with good architecture and testing practices.

## ✨ New Feature Highlights (v3.1.0 - January 2025)

### 🔍 Enterprise Observability System (NEW!)

- **Distributed Tracing**: Complete AWS X-Ray and Jaeger integration for cross-service request tracing
- **Structured Logging**: Unified log format, correlation ID management, and PII masking
- **Business Metrics**: CloudWatch custom metrics collection and cost optimization analysis
- **Enhanced Health Checks**: Multi-level health checks and automated recovery mechanisms
- **Security Monitoring**: Security event logging, compliance monitoring, and data retention policies
- **Cost Optimization**: Resource right-sizing analysis and cost tracking dashboard

### 🤖 AI-Powered Development with MCP Integration

- **AWS Ecosystem**: Complete AWS documentation, CDK guidance, and pricing analysis tools
- **Development Tools**: GitHub integration, IAM management, and automated code review
- **Intelligent Assistant**: MCP-based development assistant providing real-time technical guidance

### 🏗️ Cloud-Native Infrastructure

- **AWS CDK**: Complete infrastructure as code with multi-region deployment
- **Kubernetes**: EKS cluster configuration and GitOps workflows
- **CI/CD Pipeline**: GitHub Actions automated testing, building, and deployment
- **Security Compliance**: CDK Nag rule checking and Well-Architected Framework adherence

### 🛒 Consumer Features

- **Smart Shopping Cart**: Multi-promotion calculation and promotion rule engine support
- **Personalized Recommendations**: Product recommendation system based on purchase history and preferences
- **Member Loyalty System**: Complete loyalty point accumulation and redemption mechanism
- **Convenience Store Coupons**: Coupon purchase, usage, and management features
- **Real-time Delivery Tracking**: Real-time delivery status updates and route tracking
- **Product Review System**: Review submission, moderation, and statistical analysis

### 🏢 Business Features

- **Promotion Management**: Multiple promotion rules and coupon systems
- **Inventory Management**: Real-time inventory tracking and reservation mechanism
- **Order Processing**: Complete order lifecycle management
- **Statistical Analysis**: Sales data and performance metrics analysis
- **Cost Monitoring**: Real-time cost tracking and optimization recommendations

### 🔧 Technical Features

- **Complete API Documentation**: Interactive documentation based on OpenAPI 3.0 with API grouping support
- **Containerized Deployment**: ARM64 optimized Docker images
- **Lightweight Design**: Slim Docker images and in-memory database
- **Health Checks**: Complete application monitoring mechanism
- **DDD Architecture**: Complete Domain-Driven Design implementation including Aggregate Roots, Value Objects, Domain Events, Domain Services, Specification Pattern, Policy Pattern
- **Hexagonal Architecture**: Strict port and adapter separation ensuring business logic independence
- **Java Record Refactoring**: Value Objects and Domain Events implemented using Java 21 Records, Aggregate Roots using Interface + Annotation hybrid approach
- **Event-Driven Architecture**: Complete domain event collection, publishing, and processing mechanism
- **Test Coverage**: BDD tests, unit tests, integration tests, and architecture tests achieving 100% test pass rate
- **CI/CD Pipeline**: GitHub Actions automated testing, building, and deployment

## 🚀 Quick Start

### Memory and Performance Optimization (v3.0.1 Added)

This project has been optimized for memory usage during compilation and testing:

#### 🔧 Optimization Configuration

- **Memory Configuration**: Compilation and testing maximum heap memory increased to 4GB
- **Log Optimization**: Only ERROR level logs output during testing, significantly reducing output volume
- **JVM Optimization**: Using G1 garbage collector and string deduplication optimization
- **Parallel Processing**: Optimized Gradle parallel execution configuration

#### 🛠️ Optimized Execution Scripts

```bash
# Optimized compilation (reduced log output, increased memory)
./scripts/build-optimized.sh

# Optimized test execution (only show error logs)
./scripts/run-tests-optimized.sh

# Memory usage monitoring
./scripts/monitor-memory.sh

# System resource checking
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

## 🏗️ Project Architecture

### Backend Architecture (Hexagonal Architecture + DDD)

This project adopts Hexagonal Architecture (also known as Ports and Adapters Architecture) and Domain-Driven Design, dividing the application into the following main layers:

1. **Domain Layer**
   - Contains core business logic and rules
   - Does not depend on other layers
   - **Aggregate Roots**: Using `@AggregateRoot` annotation + `AggregateRootInterface` hybrid approach
   - **Value Objects**: Using `@ValueObject` annotated Java Record implementation
   - **Domain Events**: Using Java Record implementing `DomainEvent` interface
   - **Specification Pattern**: Using `@Specification` annotation for business rules implementation
   - **Policy Pattern**: Using `@Policy` annotation for business decision implementation
   - **Domain Services**: Using `@DomainService` annotation

2. **Application Layer**
   - Coordinates domain objects to complete user use cases
   - Only depends on the domain layer
   - Contains application services, DTOs, command and query objects
   - Responsible for domain event publishing and cross-aggregate operations
   - Responsible for data transformation between interface layer and domain layer

3. **Infrastructure Layer**
   - Provides technical implementation
   - Depends on the domain layer, implements interfaces defined by the domain layer
   - Contains repository implementations, external system adapters, ORM mappings, event handlers, etc.
   - Organized by function into sub-packages like persistence, event (event handling), and external (external systems)

## 📁 Project Directory Structure

```
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

4. **Interface Layer**
   - Handles user interactions
   - Only depends on the application layer, does not directly depend on the domain layer
   - Contains REST controllers, view models, request/response objects, etc.
   - Uses its own DTOs to interact with the application layer
   - Integrates OpenAPI 3.0 specification and Swagger UI

### Frontend Architecture (Modern Dual Frontend Architecture)

#### CMC Frontend (Next.js)

- **Framework**: Next.js 14.2.30 with App Router
- **Language**: TypeScript 5.5.4
- **Styling**: Tailwind CSS 3.4.9 + shadcn/ui component library
- **State Management**: Zustand 4.5.4 (global state) + React Query 5.51.23 (server state)
- **API Integration**: Axios 1.7.3 based type-safe API calls
- **Form Handling**: React Hook Form 7.52.2 + Zod 3.23.8 validation

#### Consumer Frontend (Angular)

- **Framework**: Angular 18.2.0
- **Language**: TypeScript 5.5.2
- **Styling**: Tailwind CSS 3.4.17 + PrimeNG 18.0.2 UI components
- **State Management**: RxJS 7.8.0 reactive programming
- **Testing**: Jasmine 5.1.0 + Karma 6.4.0

## 🆕 Latest Changes (January 2025)

### 🏗️ Significant Architecture Quality Improvement

- **Hexagonal Architecture Enhancement**: Strict port and adapter separation, architecture compliance improved from 8.5/10 to 9.5/10
- **DDD Practice Optimization**: Complete tactical pattern implementation including Specification Pattern and Policy Pattern
- **Java Record Refactoring**: 22 major Value Objects and Domain Events converted to Records, reducing 30-40% boilerplate code
- **Type Safety Enhancement**: Unified use of domain Value Objects, avoiding primitive type leakage

### 🧪 Test Quality Improvement

- **Test Stability**: Fixed all test compilation errors, achieving 100% pass rate for 272 tests
- **Architecture Testing**: Using ArchUnit to ensure DDD and Hexagonal Architecture compliance
- **BDD Testing**: Complete business process behavior-driven testing
- **Event Management**: Fixed aggregate root event collection mechanism, ensuring Domain Event correctness

### 🔧 Technical Modernization

- **Java 21 Upgrade**: Enabled preview features, using latest language features
- **Spring Boot 3.4.5**: Upgraded to latest stable version
- **Record Pattern**: Extensive use of Java Records to improve code conciseness
- **API Documentation**: Complete OpenAPI 3.0 specification and Swagger UI integration

### 📁 Project Structure Optimization

- **File Reorganization**: Organized scattered root directory files into corresponding functional directories
- **Docker Files**: Moved to `docker/` directory, including build and verification scripts
- **Deployment Files**: Moved to `deployment/` directory, including Kubernetes and EKS configuration
- **Script Files**: Moved to `scripts/` directory, including startup, testing, and data generation scripts
- **Tool Files**: Moved to `tools/` directory, including PlantUML and other development tools

## 🛠️ Technology Stack

### Backend Technologies

- **Core Framework**: Spring Boot 3.5.5
- **Programming Language**: Java 21 (preview features enabled)
- **Build Tool**: Gradle 8.x
- **Database**: H2 (development) + Flyway (migration management)
- **API Documentation**: SpringDoc OpenAPI 3 + Swagger UI
- **Testing Frameworks**:
  - JUnit 5 - Unit testing
  - Cucumber 7.15.0 - BDD testing
  - ArchUnit 1.3.0 - Architecture testing
  - Mockito 5.8.0 - Mock objects
  - Allure 2.22.1 - Test reporting and visualization
- **Other Tools**:
  - Lombok 1.18.38 - Reduce boilerplate code
  - PlantUML - UML diagram generation

### Frontend Technologies

#### CMC Frontend (Next.js)

- **Framework**: Next.js 14.2.30, React 18.3.1
- **Language**: TypeScript 5.5.4
- **Styling**: Tailwind CSS 3.4.9, PostCSS
- **UI Components**: shadcn/ui, Radix UI, Lucide Icons 0.424.0
- **State Management**: Zustand 4.5.4, React Query 5.51.23
- **Form Handling**: React Hook Form 7.52.2, Zod 3.23.8
- **HTTP Client**: Axios 1.7.3
- **Development Tools**: ESLint, Prettier, Playwright (E2E testing)

#### Consumer Frontend (Angular)

- **Framework**: Angular 18.2.0
- **Language**: TypeScript 5.5.2
- **Styling**: Tailwind CSS 3.4.17, PrimeNG 18.0.2
- **UI Components**: PrimeNG, PrimeIcons 7.0.0
- **State Management**: RxJS 7.8.0
- **Testing Framework**: Jasmine 5.1.0, Karma 6.4.0

## 📊 Data and API

### Database Initialization

The project uses Flyway for database version management, including rich business test data:

- **100+ Product Inventory Records** - Covering electronics, clothing, home goods, etc.
- **Complete Order Process Data** - Orders, order items, payment records
- **Taiwan Localized Data** - Real addresses, Traditional Chinese product names
- **Multiple Payment Methods** - Credit cards, digital wallets, bank transfers, cash on delivery
- **Independent Product Table** - Supporting complete product lifecycle management

### API Documentation and Endpoints

#### 📖 Swagger UI Documentation

- **Swagger UI**: <http://localhost:8080/swagger-ui/index.html>
- **OpenAPI Specification**: <http://localhost:8080/v3/api-docs>
- **API Grouping**:
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

# Health Check
GET /actuator/health              # Application health status

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
  - Product Editing Feature - Support for name, description, price, category modification
  - Inventory Adjustment Feature - Support for adding, reducing, setting inventory quantities
  - Product Deletion Feature - Safe product deletion operations
- **Customer Management** (`/customers`) - Customer information management

### Product Management Features

- ✏️ **Product Editing** - Complete product information editing interface
  - Product name and description modification
  - Price and currency settings (TWD, USD, EUR)
  - Product category management (Electronics, Clothing, Food, etc.)
- 📦 **Inventory Management** - Flexible inventory adjustment system
  - Set Inventory - Directly set inventory quantity
  - Add Inventory - Restock inventory
  - Reduce Inventory - Handle damage or returns
  - Adjustment Reason Recording - Complete inventory change tracking
- 🗑️ **Product Deletion** - Safe product deletion feature
  - Confirmation dialog to prevent accidental deletion
  - Automatic product list updates

### UI/UX Features

- 🎨 Modern design system (shadcn/ui + Tailwind CSS)
- 📱 Fully responsive design
- 🌙 Dark/light theme support
- ⚡ Real-time data updates (React Query)
- 🔄 Loading states and error handling
- 📊 Data visualization charts
- 🎯 Intuitive operation interface
- 📝 Form validation and user feedback
- 🔔 Toast notification system

## 📚 Documentation

> **Documentation Center**: [docs/README.md](docs/README.md) - Complete documentation navigation and categorization

The project includes rich documentation organized by function:

### 🎯 Quick Navigation

- **👨‍💼 Project Manager**: [Project Summary 2025](docs/reports/project-summary-2025.md) | [Architecture Overview](docs/diagrams/mermaid/architecture-overview.md)
- **🏗️ Architect**: [Architecture Documentation](docs/architecture/) | [Diagram Documentation](docs/diagrams/) | [Design Documentation](docs/design/)
- **👨‍💻 Developer**: [Development Guide](docs/development/) | [API Documentation](docs/api/) | [Development Instructions](docs/development/instructions.md)
- **🚀 DevOps**: [Deployment Documentation](docs/deployment/) | [Docker Guide](docs/deployment/docker-guide.md)

### 📊 Core Diagrams (Mermaid - Direct GitHub Display)

- [🏗️ System Architecture Overview](docs/diagrams/mermaid/architecture-overview.md) - Complete system architecture diagram
- [🔵 Hexagonal Architecture](docs/diagrams/mermaid/hexagonal-architecture.md) - Ports and adapters architecture
- [🏛️ DDD Layered Architecture](docs/diagrams/mermaid/ddd-layered-architecture.md) - Domain-Driven Design layering
- [⚡ Event-Driven Architecture](docs/diagrams/mermaid/event-driven-architecture.md) - Event processing mechanism
- [🔌 API Interaction Diagram](docs/diagrams/mermaid/api-interactions.md) - API call relationships

### 📋 Detailed UML Diagrams (PlantUML)

- **Structure Diagrams**: Class diagrams, object diagrams, component diagrams, deployment diagrams, package diagrams, composite structure diagrams
- **Behavior Diagrams**: Use case diagrams, activity diagrams, state diagrams
- **Interaction Diagrams**: Sequence diagrams, communication diagrams, interaction overview diagrams, timing diagrams
- **Event Storming**: Big Picture, Process Level, Design Level

### 🏆 Core Reports (January 2025 Update)

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

Using ArchUnit 1.3.0 to ensure code follows predefined architectural rules:

- **DddEntityRefactoringArchitectureTest** - Ensures DDD entity refactoring complies with architectural specifications
- **Hexagonal Architecture Compliance** - Ensures port and adapter separation
- **DDD Tactical Patterns** - Ensures correct use of Aggregate Roots, Value Objects, Domain Events, Specification Pattern, Policy Pattern
- **Package Structure Standards** - Ensures package structure complies with DDD layered architecture
- **Annotation Validation** - Ensures correct use of `@AggregateRoot`, `@ValueObject`, `@Specification`, `@Policy`, etc.

### BDD Testing

Using Cucumber 7.15.0 for Behavior-Driven Development testing, covering:

- **Consumer Features** - Shopping journey, shopping cart management
- **Customer Management** - Member system, loyalty points, member discounts
- **Order Management** - Order aggregate root, order workflow
- **Inventory Management** - Inventory management
- **Payment Processing** - Payment aggregate root, payment discounts
- **Logistics Delivery** - Delivery management, delivery system
- **Notification Service** - Notification management, notification service
- **Promotion Activities** - Coupon system, flash sales, convenience store coupons, add-on activities, gift activities
- **Product Management** - Product search, product combinations
- **Pricing Management** - Commission rates
- **Complete Workflow** - End-to-end business processes

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

- Class diagrams, object diagrams, component diagrams, deployment diagrams
- Sequence diagrams (order processing, pricing processing, delivery processing)
- State diagrams, activity diagrams
- Domain model diagrams, hexagonal architecture diagrams, DDD layered architecture diagrams

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
- **Consumer Frontend**: <http://localhost:3001> (development mode)
- **CMC Frontend**: <http://localhost:3002> (development mode)
- **Backend API**: <http://localhost:8080>
- **Swagger UI**: <http://localhost:8080/swagger-ui/index.html>
- **OpenAPI Specification**: <http://localhost:8080/v3/api-docs>
- **H2 Console**: <http://localhost:8080/h2-console>

---

## 🆕 Latest Updates (January 2025)

### 🧪 Test Quality Improvement (2025-01-21)

- ✅ **Fixed Aggregate Root Tests** - Resolved `CustomerAggregateRootTest` event count inconsistency issue
- ✅ **Event Management Optimization** - Improved event generation logic in `updateProfile` method
- ✅ **Test Stability** - Ensured all 272 tests pass, achieving 100% success rate
- ✅ **Domain Event Correctness** - Fixed aggregate root state tracker event collection mechanism

### Complete OpenAPI Documentation System Implementation

- ✅ **Complete OpenAPI 3.0 Specification** - Industry-standard API documentation
- ✅ **Swagger UI Integration** - Interactive API documentation interface
- ✅ **API Group Management** - Public API, internal API, management endpoint grouping
- ✅ **Standardized Error Responses** - Unified error handling format
- ✅ **Complete Schema Annotations** - Detailed request/response model documentation
- ✅ **Multi-environment Configuration** - Development, testing, staging, production environment configuration

### Complete Product Management System Implementation

- ✅ **Complete Product CRUD Operations** - Create, Read, Update, Delete
- ✅ **Independent Product Data Table** - Separated product management from inventory system
- ✅ **Inventory Adjustment Features** - Support for adding, reducing, setting inventory
- ✅ **Complete Frontend-Backend Integration** - Seamless React + Spring Boot integration
- ✅ **DDD Architecture Implementation** - Following Domain-Driven Design principles
- ✅ **Hexagonal Architecture** - Clear port and adapter separation

### Technical Improvements

- 🔧 **Spring Boot Upgrade** - Upgraded to Spring Boot 3.5.5 latest version
- 🔧 **Dependency Version Updates** - Updated all major dependencies to latest stable versions
- 🔧 **JPA Entity Refactoring** - Optimized database mapping and query performance
- 🔧 **API Error Handling** - Comprehensive error handling and user feedback
- 🔧 **Frontend State Management** - React Query 5.51.23 for data synchronization
- 🔧 **Type Safety** - TypeScript 5.5.4 complete type definitions
- 🔧 **API Documentation Automation** - SpringDoc 2.2.0 automatic OpenAPI specification generation
- 🔧 **Test Quality Assurance** - Fixed domain event management, ensuring test stability

## 📈 Project Statistics

- **Total Lines of Code**: 200,000+ lines (including complete DDD, Hexagonal Architecture, and observability implementation)
- **Test Coverage**: 272 tests, 100% pass rate
- **Business Data**: 131 complete business records
- **API Endpoints**: 35+ RESTful APIs (including cost optimization and monitoring endpoints)
- **UI Components**: 25+ reusable components (modern React + Angular ecosystem)
- **Documentation Pages**: 80+ detailed documentation (including architecture, design, implementation, and observability guides)
- **Database Migrations**: 22 Flyway migration scripts
- **Architecture Compliance**: 9.5/10 (Hexagonal Architecture) + 9.5/10 (DDD Practices)
- **Technology Stack Versions**: Java 21 + Spring Boot 3.4.5 + Next.js 14.2.30 + Angular 18.2.0
- **Infrastructure**: AWS CDK + Kubernetes + GitHub Actions CI/CD
- **Observability**: X-Ray + CloudWatch + Jaeger + Prometheus

## 🏆 Project Features

This project demonstrates best practices for modern enterprise application development:

### 🎯 Architectural Excellence

- **Hexagonal Architecture**: Strict port and adapter separation, complete business logic independence
- **DDD Tactical Patterns**: Complete implementation of Aggregate Roots, Value Objects, Domain Events, Specification Pattern, Policy Pattern
- **Java Records**: Extensive use of Java 21 Records to reduce boilerplate code and improve code quality

### 🧪 Test-Driven Development

- **BDD + TDD**: Behavior-Driven Development combined with Test-Driven Development
- **Architecture Testing**: ArchUnit ensures architectural compliance
- **100% Test Pass Rate**: All 272 tests pass, ensuring code quality

### 🚀 Modern Technology Stack

- **Java 21**: Using latest LTS version and preview features
- **Spring Boot 3.5.5**: Latest stable version
- **Next.js 14.2.30**: Modern frontend framework
- **Angular 18.2.0**: Modern consumer frontend
- **Docker Containerization**: ARM64 optimized deployment

This project is not only a fully functional e-commerce platform but also a best practice example demonstrating how to achieve clear architectural separation, complete test coverage, enterprise-grade observability, and excellent user experience in complex business scenarios. The project adopts the latest technology stack and architectural patterns, including 200,000+ lines of high-quality code, 272 test cases, 22 database migration scripts, complete AWS infrastructure code, and 80+ detailed documentation, making it an ideal reference for learning modern enterprise application development and DevOps practices.

### 🌟 Project Highlights

- **🏗️ Enterprise Architecture**: DDD + Hexagonal Architecture + Event-Driven Design
- **🔍 Complete Observability**: Distributed tracing + structured logging + business metrics
- **🤖 AI-Assisted Development**: MCP integration providing intelligent development guidance
- **☁️ Cloud-Native Deployment**: AWS CDK + Kubernetes + GitOps
- **🧪 Test-Driven**: BDD + TDD + architecture testing, 100% pass rate
- **📊 Cost Optimization**: Real-time cost tracking and resource right-sizing recommendations
