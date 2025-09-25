# 技術棧與工具鏈

## 概述

本文檔提供完整的技術棧與工具鏈指南，包含後端技術、前端技術、測試框架、基礎設施、建置與部署、品質保證等所有技術組件的整合配置。

## 🛠️ 後端技術

### ☕ Spring Boot 3.4.5 + Java 21 + Gradle 8.x

#### 核心配置

```gradle
// build.gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.4.5'
    id 'io.spring.dependency-management' version '1.1.6'
    id 'org.graalvm.buildtools.native' version '0.10.3'
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    // Spring Boot Starters
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    
    // Database
    runtimeOnly 'org.postgresql:postgresql'
    runtimeOnly 'com.h2database:h2'
    implementation 'org.flywaydb:flyway-core'
    
    // Documentation
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0'
    
    // Monitoring
    implementation 'io.micrometer:micrometer-registry-prometheus'
    implementation 'com.amazonaws:aws-xray-recorder-sdk-spring:2.15.1'
}
```#### Java
 21 特性應用

```java
// 使用 Records 作為 DTO
public record CustomerDto(
    String id,
    String name,
    String email,
    LocalDateTime createdAt
) {
    public static CustomerDto from(Customer customer) {
        return new CustomerDto(
            customer.getId(),
            customer.getName(),
            customer.getEmail(),
            customer.getCreatedAt()
        );
    }
}

// 使用 Pattern Matching for Switch
public class OrderStatusHandler {
    
    public String getStatusMessage(OrderStatus status) {
        return switch (status) {
            case PENDING -> "訂單處理中";
            case CONFIRMED -> "訂單已確認";
            case SHIPPED -> "訂單已發貨";
            case DELIVERED -> "訂單已送達";
            case CANCELLED -> "訂單已取消";
        };
    }
}

// 使用 Text Blocks
public class SqlQueries {
    
    public static final String FIND_CUSTOMERS_WITH_ORDERS = """
        SELECT c.id, c.name, c.email, COUNT(o.id) as order_count
        FROM customers c
        LEFT JOIN orders o ON c.id = o.customer_id
        WHERE c.created_at >= ?
        GROUP BY c.id, c.name, c.email
        HAVING COUNT(o.id) > 0
        ORDER BY order_count DESC
        """;
}

// 使用 Virtual Threads (Project Loom)
@Configuration
@EnableAsync
public class AsyncConfiguration {
    
    @Bean(name = "virtualThreadExecutor")
    public TaskExecutor virtualThreadExecutor() {
        return new TaskExecutor() {
            @Override
            public void execute(Runnable task) {
                Thread.ofVirtual().start(task);
            }
        };
    }
}
```

### 🗄️ PostgreSQL + H2 + Flyway

#### 資料庫配置

```yaml
# application.yml
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/genaidemo}
    username: ${DATABASE_USERNAME:genaidemo}
    password: ${DATABASE_PASSWORD:password}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      max-lifetime: 1200000
      connection-timeout: 20000
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        use_sql_comments: true
        jdbc:
          batch_size: 25
        order_inserts: true
        order_updates: true
  
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

#### Flyway 遷移範例

```sql
-- V1__Create_customers_table.sql
CREATE TABLE customers (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_customers_email ON customers(email);
CREATE INDEX idx_customers_status ON customers(status);
CREATE INDEX idx_customers_created_at ON customers(created_at);
```

### 📊 Spring Boot Actuator + AWS X-Ray

#### 監控配置

```yaml
# Management endpoints
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
  metrics:
    export:
      prometheus:
        enabled: true

# AWS X-Ray 配置
aws:
  xray:
    tracing-name: genai-demo
    sampling-rate: 0.1
```

```java
// X-Ray 整合
@Configuration
public class XRayConfiguration {
    
    @Bean
    public Filter TracingFilter() {
        return new AWSXRayServletFilter("genai-demo");
    }
    
    @Bean
    public WebMvcConfigurer xrayWebConfig() {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new XRayInterceptor());
            }
        };
    }
}
```

## 🎨 前端技術

### ⚛️ Next.js 14 + React 18

#### 專案結構

```
cmc-management/
├── app/                    # App Router (Next.js 14)
│   ├── (dashboard)/       # Route Groups
│   │   ├── customers/
│   │   ├── orders/
│   │   └── analytics/
│   ├── ../api/               # API Routes
│   ├── globals.css
│   ├── layout.tsx
│   └── page.tsx
├── components/            # Reusable Components
│   ├── ui/               # shadcn/ui components
│   ├── forms/
│   ├── charts/
│   └── layout/
├── lib/                  # Utilities
├── hooks/                # Custom Hooks
├── types/                # TypeScript Types
└── public/               # Static Assets
```

#### 核心配置

```json
// package.json
{
  "name": "cmc-management",
  "version": "1.0.0",
  "dependencies": {
    "next": "14.2.15",
    "react": "^18.3.1",
    "react-dom": "^18.3.1",
    "@radix-ui/react-dialog": "^1.1.2",
    "@radix-ui/react-dropdown-menu": "^2.1.2",
    "@radix-ui/react-select": "^2.1.2",
    "class-variance-authority": "^0.7.1",
    "clsx": "^2.1.1",
    "lucide-react": "^0.451.0",
    "tailwind-merge": "^2.5.4",
    "tailwindcss-animate": "^1.0.7",
    "@tanstack/react-query": "^5.59.16",
    "@tanstack/react-table": "^8.20.5",
    "react-hook-form": "^7.53.1",
    "@hookform/resolvers": "^3.9.1",
    "zod": "^3.23.8",
    "axios": "^1.7.7"
  }
}
```

#### React 18 特性應用

```typescript
// 使用 Concurrent Features
import { Suspense, lazy, startTransition } from 'react';
import { ErrorBoundary } from 'react-error-boundary';

// Lazy Loading Components
const CustomerList = lazy(() => import('./components/CustomerList'));
const OrderAnalytics = lazy(() => import('./components/OrderAnalytics'));

// 使用 Suspense 和 Error Boundaries
export default function Dashboard() {
  return (
    <ErrorBoundary fallback={<ErrorFallback />}>
      <Suspense fallback={<LoadingSpinner />}>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <CustomerList />
          <OrderAnalytics />
        </div>
      </Suspense>
    </ErrorBoundary>
  );
}

// 使用 startTransition 優化用戶體驗
function SearchComponent() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  
  const handleSearch = (newQuery: string) => {
    setQuery(newQuery);
    
    // 使用 startTransition 標記非緊急更新
    startTransition(() => {
      searchCustomers(newQuery).then(setResults);
    });
  };
  
  return (
    <div>
      <input
        value={query}
        onChange={(e) => handleSearch(e.target.value)}
        placeholder="搜索客戶..."
      />
      <CustomerResults results={results} />
    </div>
  );
}
```

### 🅰️ Angular 18 + TypeScript

#### 專案結構

```
consumer-app/
├── src/
│   ├── app/
│   │   ├── core/              # Core Services
│   │   ├── shared/            # Shared Components
│   │   ├── features/          # Feature Modules
│   │   │   ├── products/
│   │   │   ├── orders/
│   │   │   └── profile/
│   │   ├── app.component.ts
│   │   ├── app.config.ts      # Angular 18 Standalone Config
│   │   └── app.routes.ts
│   ├── assets/
│   ├── environments/
│   └── main.ts
```

#### Angular 18 配置

```typescript
// app.config.ts (Angular 18 Standalone Configuration)
import { ApplicationConfig, importProvidersFrom } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { errorInterceptor } from './core/interceptors/error.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(
      withInterceptors([authInterceptor, errorInterceptor])
    ),
    provideAnimations(),
    importProvidersFrom(MatSnackBarModule)
  ]
};
```

#### Angular 18 特性應用

```typescript
// 使用 Standalone Components
@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatButtonModule],
  template: `
    <div class="product-grid">
      @for (product of products(); track product.id) {
        <mat-card class="product-card">
          <mat-card-header>
            <mat-card-title>{{ product.name }}</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <p>{{ product.description }}</p>
            <p class="price">{{ product.price | currency }}</p>
          </mat-card-content>
          <mat-card-actions>
            <button mat-button (click)="addToCart(product)">
              加入購物車
            </button>
          </mat-card-actions>
        </mat-card>
      }
    </div>
  `
})
export class ProductListComponent {
  private productService = inject(ProductService);
  private cartService = inject(CartService);
  
  // 使用 Signals
  products = signal<Product[]>([]);
  
  constructor() {
    // 使用 effect 響應式更新
    effect(() => {
      this.loadProducts();
    });
  }
  
  private async loadProducts() {
    try {
      const products = await this.productService.getProducts();
      this.products.set(products);
    } catch (error) {
      console.error('Failed to load products:', error);
    }
  }
  
  addToCart(product: Product) {
    this.cartService.addItem(product);
  }
}
```

### 🎨 shadcn/ui + Radix UI

#### 安裝與配置

```bash
# 安裝 shadcn/ui
npx shadcn-ui@latest init

# 添加組件
npx shadcn-ui@latest add button
npx shadcn-ui@latest add dialog
npx shadcn-ui@latest add form
npx shadcn-ui@latest add table
```

#### 組件使用範例

```typescript
// 使用 shadcn/ui 組件
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"

export function CreateCustomerDialog() {
  const form = useForm<CreateCustomerFormData>({
    resolver: zodResolver(createCustomerSchema),
    defaultValues: {
      name: "",
      email: "",
      phone: "",
    },
  })

  return (
    <Dialog>
      <DialogTrigger asChild>
        <Button>創建客戶</Button>
      </DialogTrigger>
      <DialogContent className="sm:max-w-[425px]">
        <DialogHeader>
          <DialogTitle>創建新客戶</DialogTitle>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="name"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>姓名</FormLabel>
                  <FormControl>
                    <Input placeholder="請輸入客戶姓名" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="email"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>電子郵件</FormLabel>
                  <FormControl>
                    <Input placeholder="請輸入電子郵件" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <Button type="submit" className="w-full">
              創建客戶
            </Button>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  )
}
```

## 🧪 測試框架

### 🧪 JUnit 5 + Mockito + AssertJ

#### 測試依賴配置

```gradle
dependencies {
    // JUnit 5
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.1'
    testImplementation 'org.junit.jupiter:junit-jupiter-params:5.10.1'
    
    // Mockito
    testImplementation 'org.mockito:mockito-core:5.8.0'
    testImplementation 'org.mockito:mockito-junit-jupiter:5.8.0'
    
    // AssertJ
    testImplementation 'org.assertj:assertj-core:3.24.2'
    
    // Spring Boot Test
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
    
    // Testcontainers
    testImplementation 'org.testcontainers:junit-jupiter:1.19.3'
    testImplementation 'org.testcontainers:postgresql:1.19.3'
}
```

#### 整合測試範例

```java
// JUnit 5 + Mockito + AssertJ 整合
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
    
    @Mock
    private CustomerRepository customerRepository;
    
    @Mock
    private EmailService emailService;
    
    @InjectMocks
    private CustomerService customerService;
    
    @Test
    @DisplayName("應該成功創建客戶並發送歡迎郵件")
    void should_create_customer_and_send_welcome_email() {
        // Given - 使用 AssertJ 的流暢 API
        CreateCustomerCommand command = new CreateCustomerCommand(
            "John Doe",
            "john@example.com",
            "password123"
        );
        
        Customer expectedCustomer = Customer.builder()
            .id("CUST-001")
            .name("John Doe")
            .email("john@example.com")
            .build();
        
        when(customerRepository.save(any(Customer.class)))
            .thenReturn(expectedCustomer);
        
        // When
        Customer result = customerService.createCustomer(command);
        
        // Then - 使用 AssertJ 進行斷言
        assertThat(result)
            .isNotNull()
            .satisfies(customer -> {
                assertThat(customer.getId()).isEqualTo("CUST-001");
                assertThat(customer.getName()).isEqualTo("John Doe");
                assertThat(customer.getEmail()).isEqualTo("john@example.com");
            });
        
        // 驗證 Mock 互動
        verify(customerRepository).save(argThat(customer -> 
            customer.getName().equals("John Doe") &&
            customer.getEmail().equals("john@example.com")
        ));
        
        verify(emailService).sendWelcomeEmail(
            eq("john@example.com"),
            eq("John Doe")
        );
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"", " ", "invalid-email"})
    @DisplayName("應該拒絕無效的電子郵件地址")
    void should_reject_invalid_email_addresses(String invalidEmail) {
        // Given
        CreateCustomerCommand command = new CreateCustomerCommand(
            "John Doe",
            invalidEmail,
            "password123"
        );
        
        // When & Then
        assertThatThrownBy(() -> customerService.createCustomer(command))
            .isInstanceOf(InvalidEmailException.class)
            .hasMessageContaining("Invalid email format");
    }
}
```

### 🥒 Cucumber 7 + Gherkin

#### Cucumber 配置

```gradle
dependencies {
    // Cucumber
    testImplementation 'io.cucumber:cucumber-java:7.18.1'
    testImplementation 'io.cucumber:cucumber-junit-platform-engine:7.18.1'
    testImplementation 'io.cucumber:cucumber-spring:7.18.1'
}

test {
    useJUnitPlatform()
    systemProperty 'cucumber.junit-platform.naming-strategy', 'long'
}
```

#### Cucumber 整合

```java
// Cucumber Spring 配置
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class CucumberSpringConfiguration {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @LocalServerPort
    private int port;
    
    @BeforeAll
    static void setUp() {
        // 全局測試設置
    }
}

// Cucumber Step Definitions
@Component
public class CustomerStepDefinitions {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    private ResponseEntity<CustomerResponse> lastResponse;
    private CreateCustomerRequest customerRequest;
    
    @Given("一個有效的客戶資料")
    public void a_valid_customer_data() {
        customerRequest = new CreateCustomerRequest(
            "John Doe",
            "john@example.com",
            "password123"
        );
    }
    
    @When("提交客戶創建請求")
    public void submit_customer_creation_request() {
        lastResponse = restTemplate.postForEntity(
            "/../api/v1/customers",
            customerRequest,
            CustomerResponse.class
        );
    }
    
    @Then("應該成功創建客戶")
    public void should_successfully_create_customer() {
        assertThat(lastResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(lastResponse.getBody())
            .isNotNull()
            .satisfies(customer -> {
                assertThat(customer.getName()).isEqualTo("John Doe");
                assertThat(customer.getEmail()).isEqualTo("john@example.com");
            });
    }
}
```

#### Cucumber Feature 文件

Cucumber 使用 Gherkin 語法編寫 BDD 測試場景。詳細的 Gherkin 語法和範例請參考：
- [BDD 實踐指南](../testing/bdd-practices/README.md)
- [TDD/BDD 測試](../testing/tdd-bdd-testing.md)
    And 錯誤訊息應該包含 "<error_message>"

    Examples:
      | name     | email           | status | error_message    |
      |          | john@email.com  | 400    | Name is required |
      | John Doe |                 | 400    | Email is required|
      | John Doe | invalid-email   | 400    | Invalid email    |
```

## ☁️ 基礎設施

### ☁️ AWS CDK + TypeScript

#### CDK 專案結構

```
infrastructure/
├── lib/
│   ├── stacks/
│   │   ├── network-stack.ts
│   │   ├── database-stack.ts
│   │   ├── application-stack.ts
│   │   └── monitoring-stack.ts
│   ├── constructs/
│   │   ├── vpc-construct.ts
│   │   ├── rds-construct.ts
│   │   └── ecs-construct.ts
│   └── infrastructure-app.ts
├── bin/
│   └── infrastructure.ts
├── test/
├── cdk.json
├── package.json
└── tsconfig.json
```

#### CDK Stack 範例

```typescript
// lib/stacks/application-stack.ts
import * as cdk from 'aws-cdk-lib';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as elbv2 from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import { Construct } from 'constructs';

export class ApplicationStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    // VPC
    const vpc = new ec2.Vpc(this, 'GenAiDemoVpc', {
      maxAzs: 2,
      natGateways: 1,
    });

    // ECS Cluster
    const cluster = new ecs.Cluster(this, 'GenAiDemoCluster', {
      vpc,
      containerInsights: true,
    });

    // Task Definition
    const taskDefinition = new ecs.FargateTaskDefinition(this, 'GenAiDemoTaskDef', {
      memoryLimitMiB: 2048,
      cpu: 1024,
    });

    // Container
    const container = taskDefinition.addContainer('GenAiDemoContainer', {
      image: ecs.ContainerImage.fromRegistry('your-account.dkr.ecr.region.amazonaws.com/genai-demo:latest'),
      environment: {
        SPRING_PROFILES_ACTIVE: 'production',
        DATABASE_URL: 'jdbc:postgresql://your-rds-endpoint:5432/genaidemo',
      },
      logging: ecs.LogDrivers.awsLogs({
        streamPrefix: 'genai-demo',
      }),
    });

    container.addPortMappings({
      containerPort: 8080,
      protocol: ecs.Protocol.TCP,
    });

    // ECS Service
    const service = new ecs.FargateService(this, 'GenAiDemoService', {
      cluster,
      taskDefinition,
      desiredCount: 2,
      assignPublicIp: false,
    });

    // Application Load Balancer
    const alb = new elbv2.ApplicationLoadBalancer(this, 'GenAiDemoALB', {
      vpc,
      internetFacing: true,
    });

    const listener = alb.addListener('GenAiDemoListener', {
      port: 80,
      defaultAction: elbv2.ListenerAction.fixedResponse(404),
    });

    listener.addTargets('GenAiDemoTargets', {
      port: 8080,
      targets: [service],
      healthCheckPath: '/actuator/health',
      healthCheckIntervalDuration: cdk.Duration.seconds(30),
    });

    // Output
    new cdk.CfnOutput(this, 'LoadBalancerDNS', {
      value: alb.loadBalancerDnsName,
    });
  }
}
```

### 🐳 EKS + MSK + Route 53

#### EKS 配置

```typescript
// lib/stacks/eks-stack.ts
import * as eks from 'aws-cdk-lib/aws-eks';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as iam from 'aws-cdk-lib/aws-iam';

export class EksStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    // VPC
    const vpc = ec2.Vpc.fromLookup(this, 'VPC', {
      vpcName: 'GenAiDemoVpc',
    });

    // EKS Cluster
    const cluster = new eks.Cluster(this, 'GenAiDemoEksCluster', {
      version: eks.KubernetesVersion.V1_28,
      vpc,
      defaultCapacity: 0, // 使用 managed node groups
    });

    // Managed Node Group
    cluster.addNodegroupCapacity('GenAiDemoNodeGroup', {
      instanceTypes: [new ec2.InstanceType('t3.medium')],
      minSize: 1,
      maxSize: 10,
      desiredSize: 2,
      amiType: eks.NodegroupAmiType.AL2_X86_64,
    });

    // Service Account for AWS Load Balancer Controller
    const albServiceAccount = cluster.addServiceAccount('AWSLoadBalancerController', {
      name: 'aws-load-balancer-controller',
      namespace: 'kube-system',
    });

    // Install AWS Load Balancer Controller
    cluster.addHelmChart('AWSLoadBalancerController', {
      chart: 'aws-load-balancer-controller',
      repository: 'https://aws.github.io/eks-charts',
      namespace: 'kube-system',
      values: {
        clusterName: cluster.clusterName,
        serviceAccount: {
          create: false,
          name: 'aws-load-balancer-controller',
        },
      },
    });
  }
}
```

#### MSK 配置

```typescript
// lib/stacks/msk-stack.ts
import * as msk from 'aws-cdk-lib/aws-msk';
import * as ec2 from 'aws-cdk-lib/aws-ec2';

export class MskStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const vpc = ec2.Vpc.fromLookup(this, 'VPC', {
      vpcName: 'GenAiDemoVpc',
    });

    // MSK Cluster
    const mskCluster = new msk.Cluster(this, 'GenAiDemoMskCluster', {
      clusterName: 'genai-demo-kafka',
      kafkaVersion: msk.KafkaVersion.V2_8_1,
      vpc,
      instanceType: ec2.InstanceType.of(ec2.InstanceClass.T3, ec2.InstanceSize.SMALL),
      numberOfBrokerNodes: 3,
      ebsStorageInfo: {
        volumeSize: 100,
      },
      encryptionInTransit: {
        clientBroker: msk.ClientBrokerEncryption.TLS,
        enableInCluster: true,
      },
      clientAuthentication: msk.ClientAuthentication.sasl({
        scram: true,
      }),
    });

    // Output
    new cdk.CfnOutput(this, 'MskClusterArn', {
      value: mskCluster.clusterArn,
    });
  }
}
```

## 🔧 建置與部署

### 建置系統

#### 🐘 Gradle 配置

```gradle
// settings.gradle
rootProject.name = 'genai-demo'

include 'backend'
include 'shared'

// 多模組設置
project(':backend').projectDir = file('backend')
project(':shared').projectDir = file('shared')
```

#### 📦 多模組設置

```gradle
// backend/build.gradle
dependencies {
    implementation project(':shared')
    
    // 其他依賴...
}

// shared/build.gradle
dependencies {
    api 'org.springframework.boot:spring-boot-starter-validation'
    api 'com.fasterxml.jackson.core:jackson-databind'
    
    // 共享依賴...
}
```

#### 📚 依賴管理

```gradle
// gradle/libs.versions.toml
[versions]
spring-boot = "3.4.5"
java = "21"
junit = "5.10.1"
mockito = "5.8.0"
assertj = "3.24.2"

[libraries]
spring-boot-starter-web = { module = "org.springframework.boot:spring-boot-starter-web", version.ref = "spring-boot" }
spring-boot-starter-data-jpa = { module = "org.springframework.boot:spring-boot-starter-data-jpa", version.ref = "spring-boot" }
junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
mockito-core = { module = "org.mockito:mockito-core", version.ref = "mockito" }
assertj-core = { module = "org.assertj:assertj-core", version.ref = "assertj" }

[bundles]
spring-boot = ["spring-boot-starter-web", "spring-boot-starter-data-jpa"]
testing = ["junit-jupiter", "mockito-core", "assertj-core"]

[plugins]
spring-boot = { id = "org.springframework.boot", version.ref = "spring-boot" }
```

#### 🚀 CI/CD 整合

```yaml
# .github/workflows/ci.yml
name: CI/CD Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  backend-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      
      - name: Cache Gradle packages
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
      
      - name: Run backend tests
        run: ./gradlew test integrationTest
      
      - name: Generate test report
        run: ./gradlew jacocoTestReport
      
      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v4

  frontend-test:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        app: [cmc-management, consumer-app]
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: ${{ matrix.app }}/package-lock.json
      
      - name: Install dependencies
        run: npm ci
        working-directory: ${{ matrix.app }}
      
      - name: Run tests
        run: npm run test:ci
        working-directory: ${{ matrix.app }}
      
      - name: Build application
        run: npm run build
        working-directory: ${{ matrix.app }}

  deploy:
    needs: [backend-test, frontend-test]
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v4
      
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ap-northeast-1
      
      - name: Deploy to AWS
        run: |
          cd infrastructure
          npm ci
          npx cdk deploy --all --require-approval never
```

## 🔍 品質保證

### 👀 程式碼審查

#### 程式碼審查檢查清單

```markdown
## 程式碼審查檢查清單

### 功能性
- [ ] 程式碼正確實現需求
- [ ] 邊界條件處理適當
- [ ] 錯誤處理完整
- [ ] 輸入驗證充分

### 程式碼品質
- [ ] 程式碼清晰易讀
- [ ] 命名有意義
- [ ] 方法長度適中
- [ ] 複雜度可接受

### 架構設計
- [ ] 遵循 DDD 原則
- [ ] 依賴方向正確
- [ ] 分層清晰
- [ ] 介面設計合理

### 測試
- [ ] 單元測試覆蓋充分
- [ ] 測試案例有意義
- [ ] 測試命名清晰
- [ ] 測試獨立性

### 安全性
- [ ] 輸入驗證和清理
- [ ] 認證和授權
- [ ] 敏感資料保護
- [ ] SQL 注入防護
```

### 🔍 靜態分析

#### SonarQube 配置

```gradle
plugins {
    id 'org.sonarqube' version '4.4.1.3373'
    id 'jacoco'
}

sonar {
    properties {
        property "sonar.projectKey", "genai-demo"
        property "sonar.organization", "your-org"
        property "sonar.host.url", "https://sonarcloud.io"
        property "sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/test/jacocoTestReport.xml"
        property "sonar.java.source", "21"
        property "sonar.exclusions", "**/generated/**,**/build/**"
    }
}

jacoco {
    toolVersion = "0.8.11"
}

jacocoTestReport {
    dependsOn test
    reports {
        xml.required = true
        html.required = true
    }
}
```

#### Checkstyle 配置

```gradle
plugins {
    id 'checkstyle'
}

checkstyle {
    toolVersion = '10.12.7'
    configFile = file("config/checkstyle/checkstyle.xml")
    maxErrors = 0
    maxWarnings = 0
}

checkstyleMain {
    source = 'src/main/java'
}

checkstyleTest {
    source = 'src/test/java'
}
```

### 🔒 安全掃描

#### OWASP Dependency Check

```gradle
plugins {
    id 'org.owasp.dependencycheck' version '9.0.7'
}

dependencyCheck {
    format = 'ALL'
    suppressionFile = 'config/dependency-check-suppressions.xml'
    failBuildOnCVSS = 7.0
    
    analyzers {
        assemblyEnabled = false
        nuspecEnabled = false
        nugetconfEnabled = false
    }
}
```

#### SpotBugs 配置

```gradle
plugins {
    id 'com.github.spotbugs' version '6.0.7'
}

spotbugs {
    ignoreFailures = false
    showStackTraces = true
    showProgress = true
    effort = 'max'
    reportLevel = 'low'
    
    excludeFilter = file('config/spotbugs/spotbugs-exclude.xml')
}

spotbugsMain {
    reports {
        html {
            required = true
            outputLocation = file("$buildDir/reports/spotbugs/main/spotbugs.html")
        }
        xml {
            required = false
        }
    }
}
```

### 📊 效能監控

#### Micrometer + Prometheus 配置

```java
@Configuration
public class MetricsConfiguration {
    
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags(
            "application", "genai-demo",
            "environment", getEnvironment()
        );
    }
    
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
    
    @Bean
    public CountedAspect countedAspect(MeterRegistry registry) {
        return new CountedAspect(registry);
    }
}

// 業務指標
@Component
public class BusinessMetrics {
    
    private final Counter customerCreatedCounter;
    private final Timer orderProcessingTimer;
    private final Gauge activeUsersGauge;
    
    public BusinessMetrics(MeterRegistry meterRegistry) {
        this.customerCreatedCounter = Counter.builder("customers.created")
            .description("Number of customers created")
            .register(meterRegistry);
            
        this.orderProcessingTimer = Timer.builder("orders.processing.time")
            .description("Order processing time")
            .register(meterRegistry);
            
        this.activeUsersGauge = Gauge.builder("users.active")
            .description("Number of active users")
            .register(meterRegistry, this, BusinessMetrics::getActiveUserCount);
    }
    
    public void recordCustomerCreated() {
        customerCreatedCounter.increment();
    }
    
    public Timer.Sample startOrderProcessing() {
        return Timer.start(meterRegistry);
    }
    
    private double getActiveUserCount() {
        // 實際的活躍用戶計算邏輯
        return userService.getActiveUserCount();
    }
}
```

## 🔗 技術棧整合最佳實踐

### 1. 開發環境統一

```bash
# 使用 Docker Compose 統一開發環境
version: '3.8'
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: genaidemo
      POSTGRES_USER: genaidemo
      POSTGRES_PASSWORD: password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  backend:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: development
      DATABASE_URL: jdbc:postgresql://postgres:5432/genaidemo
    depends_on:
      - postgres
      - redis

volumes:
  postgres_data:
```

### 2. 代碼品質整合

```gradle
// 代碼品質任務
task qualityCheck {
    dependsOn 'test', 'jacocoTestReport', 'checkstyleMain', 'checkstyleTest', 'spotbugsMain', 'dependencyCheckAnalyze'
    description = 'Run all quality checks'
    group = 'verification'
}

// 品質門檻
jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = 0.80
            }
        }
    }
}
```

### 3. 監控整合

```yaml
# docker-compose.monitoring.yml
version: '3.8'
services:
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3001:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana-storage:/var/lib/grafana

volumes:
  grafana-storage:
```

## 最佳實踐總結

### 1. 技術選擇原則
- **成熟穩定**: 選擇經過驗證的技術棧
- **社群支持**: 活躍的社群和豐富的文檔
- **團隊熟悉度**: 考慮團隊的技術背景
- **長期維護**: 技術的長期發展前景

### 2. 整合策略
- **統一標準**: 建立統一的開發標準和規範
- **自動化**: 盡可能自動化重複性工作
- **監控覆蓋**: 全面的監控和日誌記錄
- **持續改進**: 定期評估和優化技術棧

### 3. 品質保證
- **多層測試**: 單元、整合、端到端測試
- **代碼審查**: 強制性的代碼審查流程
- **靜態分析**: 自動化的代碼品質檢查
- **安全掃描**: 定期的安全漏洞掃描

---

**相關文檔**
- DDD 領域驅動設計
- 六角架構
- 測試驅動開發