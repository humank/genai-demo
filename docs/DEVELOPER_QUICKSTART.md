# Developer Quick Start Guide

## 🎯 5-Minute Quick Start

### 1. Environment Check

```bash
# Check required tools
java --version    # Requires 21+
node --version    # Requires 18+
npm --version
git --version
```

### 2. Project Setup

```bash
# Clone project
git clone https://github.com/humank/genai-demo.git
cd genai-demo

# Install root dependencies
npm install

# Backend setup
cd app
./gradlew build

# Frontend setup
cd ../consumer-frontend
npm install

cd ../cmc-frontend
npm install
```

### 3. Start Development Environment

```bash
# Terminal 1: Backend
cd app
./gradlew bootRun

# Terminal 2: Consumer frontend
cd consumer-frontend
npm start

# Terminal 3: Management frontend (optional)
cd cmc-frontend
npm run dev
```

### 4. Verify Installation

- Backend API: <http://localhost:8080/actuator/health>
- Consumer Frontend: <http://localhost:4200>
- Management Frontend: <http://localhost:3000>
- API Documentation: <http://localhost:8080/swagger-ui.html>

## 🏗️ Development Workflow

### New Feature Development

1. **Create Feature Branch**

   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Follow DDD Architecture**
   - Domain Layer: Business logic and rules
   - Application Layer: Use case coordination
   - Infrastructure Layer: Technical implementation
   - Interface Layer: API and UI

3. **Test-Driven Development**

   ```bash
   # Unit tests (fast feedback)
   ./gradlew unitTest
   
   # Integration tests (pre-commit)
   ./gradlew integrationTest
   ```

4. **Code Quality Checks**

   ```bash
   # Backend code checks
   ./gradlew check
   
   # Frontend code checks
   npm run lint
   ```

### Common Development Commands

```bash
# Backend development
./gradlew bootRun                    # Start application
./gradlew test                       # Run tests
./gradlew build                      # Build project
./gradlew clean build               # Clean and rebuild

# Frontend development (Angular)
npm start                           # Development server
npm run build                       # Production build
npm run test                        # Run tests
npm run lint                        # Code checks

# Frontend development (Next.js)
npm run dev                         # Development server
npm run build                       # Production build
npm run start                       # Production server
```

## 🧪 Testing Strategy

### Test Pyramid

- **Unit Tests (80%)**: Fast, isolated business logic tests
- **Integration Tests (15%)**: Component interaction tests
- **End-to-End Tests (5%)**: Complete user flow tests

### Test Classification

```bash
# By speed
./gradlew quickTest              # < 2 minutes, daily development
./gradlew preCommitTest          # < 5 minutes, pre-commit
./gradlew fullTest               # < 30 minutes, pre-release

# By type
./gradlew unitTest               # Unit tests
./gradlew integrationTest        # Integration tests
./gradlew e2eTest               # End-to-end tests
```

## 📊 Observability Development

### Current Status

- ✅ **Basic Monitoring**: Spring Boot Actuator
- ✅ **Structured Logging**: Unified format and correlation IDs
- ✅ **Frontend Tracking**: User behavior analysis
- 🚧 **WebSocket**: Frontend ready, backend planned
- 🚧 **Analytics**: Partial API available

### Adding Monitoring Metrics

```java
// Business metrics example
@Component
public class OrderMetrics {
    private final Counter ordersCreated;
    
    public OrderMetrics(MeterRegistry registry) {
        this.ordersCreated = Counter.builder("orders.created")
            .description("Total orders created")
            .register(registry);
    }
    
    public void recordOrderCreated() {
        ordersCreated.increment();
    }
}
```

### Structured Logging

```java
// Using structured logging
log.info("Order processed successfully", 
    kv("orderId", order.getId()),
    kv("customerId", order.getCustomerId()),
    kv("amount", order.getTotalAmount()));
```

## 🔧 Development Tool Configuration

### IDE Setup (Recommended)

- **IntelliJ IDEA**: Complete Java and Spring Boot support
- **VS Code**: Lightweight, suitable for frontend development
- **Kiro IDE**: AI-assisted development and code review

### Useful Plugins

- **SonarLint**: Code quality checks
- **GitLens**: Git history and blame tracking
- **Spring Boot Tools**: Spring Boot development support
- **Angular Language Service**: Angular development support

## 🐛 Common Issue Resolution

### Backend Issues

1. **Port Conflicts**

   ```bash
   # Find process using port
   lsof -i :8080
   # Or change port
   ./gradlew bootRun --args='--server.port=8081'
   ```

2. **Database Connection Issues**

   ```bash
   # Check H2 console
   http://localhost:8080/h2-console
   ```

### Frontend Issues

1. **Dependency Conflicts**

   ```bash
   rm -rf node_modules package-lock.json
   npm install
   ```

2. **Compilation Errors**

   ```bash
   # Angular
   ng build --verbose
   
   # Next.js
   npm run build -- --debug
   ```

## 📚 Learning Resources

### Architecture and Design

- DDD Practice Guide
- Hexagonal Architecture Guide
- Event-Driven Design

### Development Standards

- **Code Review Standards** (See project internal documentation)
- **Development Standards** (See project internal documentation)
- **Security Standards** (See project internal documentation)

### API Documentation

- Backend API
- Frontend Component Library

---

**Quick Help**: Check [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) or Troubleshooting Documentation
