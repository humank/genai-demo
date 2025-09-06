# Technology Stack

## Backend Stack

### Core Framework
- **Spring Boot 3.4.5** - Main application framework
- **Java 21** - Programming language with preview features enabled
- **Gradle 8.x** - Build system and dependency management

### Database & Persistence
- **H2 Database** - In-memory database for development
- **Spring Data JPA** - Data access layer
- **Flyway** - Database migration management
- **Hibernate** - ORM implementation

### API & Documentation
- **SpringDoc OpenAPI 3** - API documentation generation
- **Swagger UI** - Interactive API documentation interface
- **Spring Boot Actuator** - Health checks and monitoring endpoints

### Testing Framework
- **JUnit 5** - Unit testing framework
- **Cucumber 7** - BDD testing with Gherkin syntax
- **ArchUnit** - Architecture testing to enforce DDD patterns
- **Mockito** - Mocking framework
- **Allure 2** - Test reporting and visualization
- **Spring Boot Test** - Integration testing support

### Development Tools
- **Lombok** - Reduces boilerplate code
- **PlantUML** - UML diagram generation

## Frontend Stack

### CMC Frontend (Next.js)
- **Next.js 14** - React framework with App Router
- **React 18** - UI library
- **TypeScript** - Type-safe JavaScript
- **Tailwind CSS** - Utility-first CSS framework
- **shadcn/ui** - Modern UI component library
- **Radix UI** - Headless UI primitives
- **React Query (@tanstack/react-query)** - Server state management
- **Zustand** - Client state management
- **React Hook Form** - Form handling
- **Zod** - Schema validation
- **Axios** - HTTP client

### Consumer Frontend (Angular)
- **Angular 18** - Frontend framework
- **TypeScript** - Type-safe JavaScript
- **Tailwind CSS** - Styling framework

## Infrastructure & Deployment

### Containerization
- **Docker** - Containerization platform
- **Docker Compose** - Multi-container orchestration
- **ARM64 optimization** - Optimized for Apple Silicon and AWS Graviton3

### Cloud & Deployment
- **Kubernetes** - Container orchestration
- **AWS EKS** - Managed Kubernetes service
- **AWS Graviton3** - ARM-based compute instances

## Common Commands

### Backend Development
```bash
# Build the application
./gradlew build

# Run the application
./gradlew bootRun

# Run all tests
./gradlew runAllTests

# Run tests with Allure report
./gradlew runAllTestsWithReport

# Run only unit tests
./gradlew test

# Run only BDD tests
./gradlew cucumber

# Run architecture tests
./gradlew testArchitecture

# Clean build
./gradlew clean build
```

### Frontend Development (CMC)
```bash
# Install dependencies
cd cmc-frontend && npm install

# Start development server
npm run dev

# Build for production
npm run build

# Run tests
npm test

# Type checking
npm run type-check

# Linting
npm run lint
```

### Full Stack Development
```bash
# Start all services (backend + frontends)
./scripts/start-fullstack.sh

# Stop all services
./scripts/stop-fullstack.sh

# Test API endpoints
./scripts/test-api.sh
```

### Docker Operations
```bash
# Build ARM64 optimized image
./docker/docker-build.sh

# Start with Docker Compose
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down

# Verify deployment
./docker/verify-deployment.sh
```

### Testing Commands
```bash
# Generate test data
python3 scripts/generate_data.py

# Verify Swagger UI
./scripts/verify-swagger-ui.sh
```

## Development Ports

- **Backend API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **H2 Console**: http://localhost:8080/h2-console
- **Health Check**: http://localhost:8080/actuator/health
- **CMC Frontend**: http://localhost:3002
- **Consumer Frontend**: http://localhost:3001

## Build Configuration

### Java Compilation
- Java 21 with preview features enabled
- Gradle configuration cache disabled for compatibility
- Parallel builds enabled for performance

### Test Configuration
- Allure reporting integrated
- Cucumber HTML reports generated
- Architecture tests enforce DDD patterns
- BDD tests use Gherkin syntax for business scenarios
