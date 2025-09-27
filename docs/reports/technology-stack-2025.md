# Technology Stack Detailed Overview (January 2025)

## 🚀 Technology Stack Overview

The GenAI Demo project adopts a modern technology stack, combining the latest Java ecosystem, 
modern frontend frameworks, and enterprise-grade development tools.

## 🔧 Backend Technology Stack

### Core Frameworks

#### Spring Boot 3.5.5

- **Latest Stable Version**: Using the latest stable version of Spring Boot 3.x series
- **Spring Framework 6.x**: Based on Spring Framework 6.x
- **Native Compilation Support**: Supports GraalVM native image compilation
- **Enhanced Observability**: Built-in Micrometer and OpenTelemetry support

```gradle
implementation 'org.springframework.boot:spring-boot-starter-web'
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
implementation 'org.springframework.boot:spring-boot-starter-validation'
implementation 'org.springframework.boot:spring-boot-starter-actuator'
```

#### Java 21 LTS

- **Latest LTS Version**: Long-term support version, stable and reliable
- **Preview Features Enabled**: Using latest language features
- **Record Patterns**: Extensive use of Java Records to reduce boilerplate code
- **Pattern Matching**: Modern pattern matching syntax
- **Virtual Threads**: Lightweight concurrency handling (preview feature)

```java
// Java 21 Record Example
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        Objects.requireNonNull(amount, "Amount cannot be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
    }
}
```

### Build Tools

#### Gradle 8.x

- **Modern Build System**: More flexible and efficient than Maven
- **Kotlin DSL Support**: Type-safe build scripts
- **Incremental Compilation**: Improved build speed
- **Dependency Management**: Powerful dependency resolution and management

```gradle
java {
    sourceCompatibility = '21'
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType(JavaCompile).configureEach {
    options.compilerArgs += '--enable-preview'
    options.release = 21
}
```

### Data Persistence

#### H2 Database

- **In-Memory Database**: Fast development and testing
- **SQL Compatibility**: Standard SQL syntax support
- **Web Console**: Built-in database management interface
- **Zero Configuration**: No additional installation and configuration required

#### Flyway

- **Database Version Management**: Automated database migration
- **Version Control**: Database structure change tracking
- **Team Collaboration**: Ensures database structure consistency
- **Production Ready**: Supports production environment deployment

```sql
-- V1__Create_customer_table.sql
CREATE TABLE customers (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### API Documentation

#### SpringDoc OpenAPI 3

- **OpenAPI 3.0 Specification**: Industry standard API documentation format
- **Auto Generation**: Automatically generates documentation based on annotations
- **Swagger UI Integration**: Interactive API documentation interface
- **API Grouping**: Supports multiple API documentation management

```java
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order Management", description = "Order-related API endpoints")
public class OrderController {
    
    @PostMapping
    @Operation(summary = "Create Order", description = "Create a new order")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Order created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<CreateOrderResponse> createOrder(
        @RequestBody @Valid CreateOrderRequest request) {
        // Implementation logic
    }
}
```

### Testing Frameworks

#### JUnit 5

- **Modern Testing Framework**: More powerful and flexible than JUnit 4
- **Parameterized Tests**: Supports various parameterized testing methods
- **Dynamic Tests**: Runtime test case generation
- **Extension Model**: Flexible extension mechanism

```java
@Test
@DisplayName("Should collect domain event when creating order")
void should_collect_domain_event_when_creating_order() {
    // Given
    CustomerId customerId = CustomerId.of("CUST-001");
    List<OrderItem> items = List.of(
        new OrderItem(ProductId.of("PROD-001"), 1, Money.twd(999))
    );
    
    // When
    Order order = new Order(customerId, items);
    
    // Then
    assertThat(order.hasUncommittedEvents()).isTrue();
}
```

#### Cucumber 7

- **Behavior Driven Development**: BDD testing framework
- **Gherkin Syntax**: Business-readable test scenarios
- **Multi-language Support**: Supports Chinese test scenarios
- **Rich Reports**: HTML and JSON format reports

```gherkin
Feature: Order Processing
  As a customer
  I want to place orders
  So that I can purchase products

  Scenario: Successfully create order
    Given I am a registered customer "CUST-001"
    When I place an order with product "PROD-001" quantity 1
    Then the order should be created successfully
    And the order total should be 999
```

#### ArchUnit

- **Architecture Testing**: Ensures code follows architectural rules
- **Dependency Checking**: Validates inter-layer dependencies
- **Naming Conventions**: Checks class and method naming standards
- **DDD Compliance**: Validates DDD pattern implementation

```java
@Test
@DisplayName("Domain layer should not depend on infrastructure layer")
void domain_should_not_depend_on_infrastructure() {
    noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
        .check(classes);
}
```

#### Mockito

- **Mock Object Framework**: Create and manage mock objects
- **Behavior Verification**: Verify method calls and parameters
- **Stubbing**: Define mock object behavior
- **Spy Support**: Partial mocking of real objects

#### Allure 2

- **Test Reports**: Beautiful test report generation
- **Multi-format Support**: HTML, JSON and other formats
- **Historical Trends**: Test result history tracking
- **Rich Annotations**: Detailed test descriptions

### Development Tools

#### Lombok

- **Boilerplate Code Reduction**: Automatically generates getters, setters, etc.
- **Annotation Driven**: Annotation-based code generation
- **IDE Support**: Plugins available for mainstream IDEs
- **Compile-time Processing**: No runtime performance impact

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerJpaEntity {
    @Id
    private String id;
    private String name;
    private String email;
}
```

#### PlantUML

- **UML Diagram Generation**: Text-based UML diagrams
- **Multiple Diagram Types**: Class diagrams, sequence diagrams, activity diagrams, etc.
- **Version Control Friendly**: Text format convenient for version control
- **Automated Generation**: Can be integrated into build process

## 🌐 Frontend Technology Stack

### Core Frameworks

#### Next.js 14

- **React Framework**: Full-stack framework based on React
- **App Router**: Next-generation routing system
- **Server Components**: Server-side component support
- **Automatic Optimization**: Automatic code splitting and optimization
- **TypeScript Support**: Native TypeScript support

```typescript
// app/orders/page.tsx
export default async function OrdersPage() {
  const orders = await getOrders();
  
  return (
    <div className="container mx-auto p-4">
      <h1 className="text-2xl font-bold mb-4">Order Management</h1>
      <OrderList orders={orders} />
    </div>
  );
}
```

#### React 18

- **Concurrent Features**: Concurrent Features support
- **Suspense**: Data fetching and code splitting
- **Automatic Batching**: Performance optimization
- **Hooks**: Modern state management

#### TypeScript

- **Type Safety**: Compile-time type checking
- **IDE Support**: Excellent development experience
- **Refactoring Friendly**: Safe code refactoring
- **Team Collaboration**: Improves code maintainability

```typescript
interface Order {
  id: string;
  customerId: string;
  totalAmount: number;
  status: OrderStatus;
  items: OrderItem[];
}

type OrderStatus = 'PENDING' | 'CONFIRMED' | 'SHIPPED' | 'DELIVERED';
```

### Styling and UI

#### Tailwind CSS

- **Utility First**: Utility-first CSS framework
- **Responsive Design**: Built-in responsive support
- **Custom Themes**: Flexible theme configuration
- **Production Optimization**: Automatically removes unused styles

```tsx
<div className="bg-white shadow-md rounded-lg p-6 mb-4">
  <h2 className="text-xl font-semibold text-gray-800 mb-2">
    Order #{order.id}
  </h2>
  <p className="text-gray-600">
    Total: <span className="font-bold text-green-600">${order.totalAmount}</span>
  </p>
</div>
```

#### shadcn/ui

- **Modern Component Library**: Component library based on Radix UI
- **Customizable**: Fully customizable components
- **Accessibility Support**: Built-in accessibility features
- **TypeScript**: Complete TypeScript support

```tsx
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export function OrderCard({ order }: { order: Order }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Order #{order.id}</CardTitle>
      </CardHeader>
      <CardContent>
        <Button onClick={() => handleConfirm(order.id)}>
          Confirm Order
        </Button>
      </CardContent>
    </Card>
  );
}
```

### State Management

#### React Query (@tanstack/react-query)

- **Server State Management**: Specialized for handling server state
- **Caching Mechanism**: Intelligent data caching
- **Background Updates**: Automatic background data updates
- **Error Handling**: Built-in error handling mechanism

```typescript
function useOrders() {
  return useQuery({
    queryKey: ['orders'],
    queryFn: async () => {
      const response = await fetch('/api/orders');
      return response.json();
    },
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
}
```

#### Zustand

- **Lightweight State Management**: Simple global state management
- **TypeScript Support**: Complete type support
- **Middleware Support**: Rich middleware ecosystem
- **DevTools**: Developer tools support

```typescript
interface AppState {
  user: User | null;
  setUser: (user: User | null) => void;
  theme: 'light' | 'dark';
  toggleTheme: () => void;
}

const useAppStore = create<AppState>((set) => ({
  user: null,
  setUser: (user) => set({ user }),
  theme: 'light',
  toggleTheme: () => set((state) => ({ 
    theme: state.theme === 'light' ? 'dark' : 'light' 
  })),
}));
```

### Form Handling

#### React Hook Form

- **High Performance Forms**: Minimal re-renders
- **Validation Support**: Built-in and custom validation
- **TypeScript**: Complete type support
- **Easy to Use**: Clean API

```typescript
const { register, handleSubmit, formState: { errors } } = useForm<OrderForm>();

const onSubmit = (data: OrderForm) => {
  createOrder(data);
};

return (
  <form onSubmit={handleSubmit(onSubmit)}>
    <input
      {...register('customerName', { required: 'Customer name is required' })}
      placeholder="Customer Name"
    />
    {errors.customerName && (
      <span className="text-red-500">{errors.customerName.message}</span>
    )}
  </form>
);
```

#### Zod

- **Schema Validation**: TypeScript-first validation library
- **Type Inference**: Automatic type inference
- **Composable Validation**: Flexible validation rule composition
- **Error Handling**: Detailed error messages

```typescript
const orderSchema = z.object({
  customerName: z.string().min(1, 'Customer name is required'),
  email: z.string().email('Please enter a valid email'),
  items: z.array(z.object({
    productId: z.string(),
    quantity: z.number().min(1, 'Quantity must be greater than 0'),
  })).min(1, 'At least one item is required'),
});

type OrderForm = z.infer<typeof orderSchema>;
```

## 🐳 Containerization and Deployment

### Docker

- **Containerized Deployment**: Consistent runtime environment
- **ARM64 Optimization**: Supports Apple Silicon and AWS Graviton
- **Multi-stage Build**: Optimized image size
- **Health Checks**: Built-in health check mechanism

```dockerfile
FROM openjdk:21-jdk-slim as builder
WORKDIR /app
COPY . .
RUN ./gradlew build -x test

FROM openjdk:21-jre-slim
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Docker Compose

- **Multi-container Orchestration**: Manage multiple related containers
- **Network Configuration**: Automatic network configuration
- **Volume Management**: Data persistence
- **Environment Variables**: Flexible configuration management

```yaml
version: '3.8'
services:
  genai-demo:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - JAVA_OPTS=-Xms256m -Xmx512m
    healthcheck:
      test: ["CMD-SHELL", "wget --spider http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
```

## 🔧 Development Tools and Processes

### Version Control

- **Git**: Distributed version control
- **GitHub**: Code hosting and collaboration
- **Branching Strategy**: GitFlow or GitHub Flow

### IDE Support

- **IntelliJ IDEA**: Recommended Java IDE
- **VS Code**: Lightweight editor, suitable for frontend development
- **Plugin Support**: Lombok, Spring Boot, React and other plugins

### Code Quality

- **ESLint**: JavaScript/TypeScript code checking
- **Prettier**: Code formatting
- **SonarQube**: Code quality analysis (optional)

### CI/CD

- **GitHub Actions**: Automated build and deployment
- **Docker Hub**: Container image repository
- **Automated Testing**: Automatically run tests on every commit

## 📊 Technology Selection Rationale

### Backend Technology Selection

| Technology | Selection Reason | Alternatives |
|------------|------------------|--------------|
| Java 21 | Latest LTS, Record support | Java 17, Kotlin |
| Spring Boot 3.4.5 | Mature ecosystem, enterprise-grade | Quarkus, Micronaut |
| H2 Database | Fast development, zero configuration | PostgreSQL, MySQL |
| Gradle | Flexible build, good performance | Maven |
| JUnit 5 | Modern testing framework | TestNG |

### Frontend Technology Selection

| Technology | Selection Reason | Alternatives |
|------------|------------------|--------------|
| Next.js 14 | Full-stack framework, SEO friendly | Create React App, Vite |
| TypeScript | Type safety, development experience | JavaScript |
| Tailwind CSS | Utility first, rapid development | Bootstrap, Material-UI |
| React Query | Server state management expert | SWR, Apollo Client |
| Zustand | Lightweight, simple to use | Redux, Context API |

## 🚀 Performance Optimization

### Backend Optimization

- **JVM Tuning**: Memory and garbage collection optimization
- **Database Optimization**: Index and query optimization
- **Caching Strategy**: Redis caching (future plan)
- **Asynchronous Processing**: Event asynchronous processing

### Frontend Optimization

- **Code Splitting**: Automatic code splitting
- **Image Optimization**: Next.js image optimization
- **Caching Strategy**: React Query caching
- **Bundle Analysis**: Bundle size analysis and optimization

## 🔮 Technology Development Planning

### Short-term Plans (1-3 months)

- **Spring Boot Upgrade**: Upgrade to latest version
- **Performance Monitoring**: Add APM monitoring
- **Security Enhancement**: OAuth2 authentication and authorization

### Medium-term Plans (3-6 months)

- **Microservice Decomposition**: Split based on DDD boundaries
- **Event-Driven**: Complete event-driven architecture
- **API Gateway**: Unified API gateway

### Long-term Plans (6-12 months)

- **Cloud Native**: Kubernetes deployment
- **Service Mesh**: Istio service mesh
- **AI Integration**: Machine learning and AI features

This technology stack demonstrates best practices for modern enterprise application development, 
combining stability, performance, and development efficiency.
