# DDD + Hexagonal Architecture: From Problem Domain to Solution Domain (genai-demo Reference)

> This document clearly illustrates "**Subdomain** ➜ **Bounded Context** ➜ **Hexagonal Architecture (Ports & Adapters)**" in one go:
>
> 1) Event Storming Big Picture, 2) Subdomain ➜ Context Mapping, 3) Context Map, 4) Representative Sequence Diagrams, 5) Aggregates & Invariants, 6) Event Contracts (Published Language), 7) Hex Diagrams & Port/Adapter, 8) Modularization & Test Governance (ArchUnit).

---

## 0. Scope and Personas

- **Business Goals**: Support ordering, payment authorization, inventory reservation, shipping fulfillment, settlement & reconciliation, returns.
- **Personas**: Customer, Customer Service, Warehouse, Finance, Risk Control.
- **External Systems**: Payment Service Providers (PSP), Carriers, Tax/Accounting, KYC/AML.

---

## 1. Event Storming (Big Picture)

> Scenario-based illustration following "Order ➜ Authorization ➜ Reservation ➜ Fulfillment ➜ Shipping ➜ Settlement" main flow. Color semantics indicated by text: `Command`, `Aggregate`, `Event`, `Policy`, `External`.

### 1.1 Ordering & Payment Authorization

```mermaid
flowchart LR
  subgraph Ordering
    C1["Command: PlaceOrder"] -->|"validates cart, pricing"| A1["Aggregate: Order"]
    A1 --> E1("Event: OrderPlaced")
  end

  E1 --> P1{"Policy: When OrderPlaced"}
  P1 --> C2["Command: AuthorizePayment"]

  subgraph Payments
    C2 --> A2["Aggregate: Payment"]
    A2 --> E2("Event: PaymentAuthorized")
    A2 --> E3("Event: PaymentDeclined")
  end

  E2 --> P2{"Policy: When PaymentAuthorized"}
  P2 --> C3["Command: ReserveInventory"]
  E3 --> P3{"Policy: When PaymentDeclined"}
  P3 --> C4["Command: CancelOrder"]

  subgraph Inventory
    C3 --> A3["Aggregate: Reservation"]
    A3 --> E4("Event: InventoryReserved")
    A3 --> E5("Event: InventoryOutOfStock")
  end

  E4 --> P4{"Policy: When InventoryReserved"}
  P4 --> C5["Command: AllocateFulfillment"]

  classDef command fill:#fff,stroke:#333,stroke-width:1px
  classDef agg fill:#eef,stroke:#335
  classDef event fill:#efe,stroke:#383
  classDef policy fill:#ffd,stroke:#aa3
  class C1,C2,C3,C4,C5 command
  class A1,A2,A3 agg
  class E1,E2,E3,E4,E5 event
  class P1,P2,P3,P4 policy
```

### 1.2 Fulfillment, Shipping, Notification

```mermaid
flowchart LR
  subgraph Fulfillment
    C6["Command: AllocateFulfillment"] --> A4["Aggregate: FulfillmentOrder"]
    A4 --> E6("Event: FulfillmentAllocated")
    C7["Command: ShipOrder"] --> A4
    A4 --> E7("Event: OrderShipped")
  end

  E7 --> P5{"Policy: When OrderShipped"}
  P5 --> C8["Command: NotifyCustomer"]

  subgraph Notification
    C8 --> A5["Aggregate: NotificationJob"]
    A5 --> E8("Event: CustomerNotified")
  end
```

### 1.3 Settlement, Reconciliation & Refunds

```mermaid
flowchart LR
  subgraph Settlement
    C9["Command: CapturePayment"] --> A2["Payment"]
    A2 --> E9("Event: PaymentCaptured")
    C10["Command: StartReconciliation"] --> A6["Aggregate: ReconciliationBatch"]
    A6 --> E10("Event: ReconciliationCompleted")
  end

  subgraph Returns
    C11["Command: RequestReturn"] --> A1["Order"]
    A1 --> E11("Event: ReturnRequested")
    C12["Command: ApproveRefund"] --> A2
    A2 --> E12("Event: RefundIssued")
  end
```

---

## 2. Subdomain ➜ Bounded Context (When One-to-Many)

| Subdomain | Typical Upstream/Downstream Capabilities | Suggested Bounded Contexts | Why Split (One-to-Many Signals) |
|---|---|---|---|
| Ordering | Create/manage orders, cancel, modify | **Order Management** | Different semantics and consistency with Fulfillment/Payment; order state machine separated from shipping/payment flows |
| Payments | Authorization, capture, reconciliation, refunds, disputes | **Auth & Risk**, **Settlement & Reconciliation**, **Dispute/Chargeback**, **Tokenization** | Different consistency and timing (seconds vs. next-day batch), compliance zones (PCI CDE), process/external protocol differences |
| Inventory | Inventory view, reservation, replenishment | **Reservation**, **StockLedger** | Different transaction boundaries and read/write models, reservation and ledger evolve at different rates |
| Fulfillment | Allocation, shipping, reverse logistics | **Fulfillment Orchestration**, **Carrier Integration** | Multiple Carrier integration (protocol/SLA differences) and different internal operation models |
| Customer | Identity, consent/preferences | **Identity/Profile**, **Consent/Privacy** | Different regulatory/data levels, semantic conflicts |

> Decision criteria: Consistency rule differences, language dialects, read/write separation, compliance boundaries, external integration, evolution rates, organizational boundaries, regional regulations, event storming clustering.

---

## 3. Context Map

> Annotate relationship types: `PL`=Published Language, `ACL`=Anti-Corruption Layer, `OH`=Open Host Service, `C/S`=Customer/Supplier, `CF`=Conformist.

```mermaid
graph LR
  OM["Order Management"] -->|"PL: OrderPlaced"| PA["Payments - Auth & Risk"]
  PA -->|"PL: PaymentAuthorized/Declined"| OM
  OM -->|"PL: OrderCancelled"| PA

  OM -->|"PL: OrderPlaced"| INV["Inventory - Reservation"]
  INV -->|"PL: InventoryReserved/OutOfStock"| OM

  OM -->|"PL: AllocateFulfillment"| FUL["Fulfillment Orchestration"]
  FUL -->|"PL: FulfillmentAllocated/Shipped"| OM

  FUL -->|"ACL & OH"| CAR["Carrier Integration"]
  PA -->|"ACL"| TOK["Tokenization (PCI CDE)"]
  PA -->|"PL"| SET["Settlement & Reconciliation"]

  OM -->|"CF"| NOTIF["Notification"]
```

---

## 4. Representative Sequence Diagrams

### 4.1 Place Order (with Authorization & Reservation)

```mermaid
sequenceDiagram
  autonumber
  participant C as Customer
  participant API as OM.API (Open Host)
  participant OMS as OM.Domain
  participant PAY as Payments.Auth
  participant INV as Inventory.Reservation

  C->>API: POST /orders (cart)
  API->>OMS: PlaceOrder(cmd)
  OMS-->>API: OrderPlaced(evt)
  API-->>PAY: AuthorizePayment(cmd)
  PAY-->>API: PaymentAuthorized(evt)
  API-->>INV: ReserveInventory(cmd)
  INV-->>API: InventoryReserved(evt)
  API-->>C: 201 Created {orderId}
```

### 4.2 Ship & Capture (Shipping & Settlement)

```mermaid
sequenceDiagram
  autonumber
  participant FUL as Fulfillment.Orch
  participant CAR as Carrier
  participant PAY as Payments.Settlement

  FUL->>CAR: CreateShipment(cmd)
  CAR-->>FUL: ShipmentCreated(evt)
  FUL-->>PAY: CapturePayment(cmd)
  PAY-->>FUL: PaymentCaptured(evt)
```

---

## 5. Aggregates & Invariants

### 5.1 Order Management

- **Order**(Aggregate Root)
  - Invariants:
    1) `Order` cannot ship before `PENDING` status;
    2) `CANCELLED` cannot be modified;
    3) Must have `PaymentAuthorized` & `InventoryReserved` to enter `READY_TO_SHIP`.

### 5.2 Payments - Auth & Risk

- **Payment**(AR)
  - Invariants:
    1) Authorization cannot be duplicated;
    2) After `CAPTURED`, no state reversal except refunds;
    3) Card numbers never stored (only tokens).

### 5.3 Inventory - Reservation

- **Reservation**(AR)
  - Invariants:
    1) Reservation for same `orderId` cannot be duplicated;
    2) Reservation must be less than or equal to available stock;
    3) Automatic release on timeout.

### 5.4 Fulfillment Orchestration

- **FulfillmentOrder**(AR)
  - Invariants: Must be `Allocated` to `Ship`; must be `Shipped` to trigger `Capture`.

---

## 6. Event Contracts (Published Language)

> Represented using JSON Schema/examples. In practice, recommend using **schema registry** for version control.

### 6.1 `OrderPlaced` (Published by OM)

```json
{
  "event": "OrderPlaced",
  "version": 1,
  "orderId": "ORD-2025-0001",
  "customerId": "CUST-1001",
  "lines": [{"sku": "SKU-1", "qty": 2, "unitPrice": 100.0}],
  "total": 200.0,
  "occurredAt": "2025-01-28T10:02:00Z"
}
```

### 6.2 `PaymentAuthorized` (Published by Payments.Auth)

```json
{
  "event": "PaymentAuthorized",
  "version": 2,
  "orderId": "ORD-2025-0001",
  "paymentId": "PAY-8899",
  "authCode": "A1B2C3",
  "amount": 200.0,
  "occurredAt": "2025-01-28T10:02:03Z"
}
```

### 6.3 `InventoryReserved` (Published by Inventory.Reservation)

```json
{
  "event": "InventoryReserved",
  "version": 1,
  "orderId": "ORD-2025-0001",
  "reservations": [{"sku": "SKU-1", "qty": 2}],
  "expireAt": "2025-01-28T10:32:03Z"
}
```

---

## 7. Hexagonal Architecture Views (Ports & Adapters)

### 7.1 Order Management (Example)

```mermaid
flowchart TB
  subgraph Application
    PI["port.in: PlaceOrderUseCase"]
    PO["service: OrderApplicationService"]
    PO --> PI
    PO -->|"calls"| PDO["port.out: Pricing/Inventory/Payment Gateways"]
  end

  subgraph Domain
    AR[("Aggregate: Order")]
    DS["DomainService: OrderPolicy"]
    PO --> AR
    PO --> DS
  end

  subgraph Infrastructure
    AIN["adapter.in: REST Controller"]
    AOUT1["adapter.out: PaymentClient"]
    AOUT2["adapter.out: InventoryClient"]
    AOUT3["adapter.out: OrderRepository"]
  end

  AIN --> PI
  PDO --> AOUT1
  PDO --> AOUT2
  PDO --> AOUT3
```

### 7.2 Payments.Auth (with ACL & Tokenization)

```mermaid
flowchart TB
  subgraph Application
    PI2["port.in: AuthorizePayment"]
    S2["service: PaymentAppService"]
    S2 --> PI2
    S2 --> PO2["port.out: PSPGateway, TokenService"]
  end

  subgraph Domain
    AR2[("Aggregate: Payment")]
    RS["DomainService: RiskDecision"]
    S2 --> AR2
    S2 --> RS
  end

  subgraph Infrastructure
    IN2["adapter.in: REST/Webhook"]
    OUT21["adapter.out: PSPClient (ACL)"]
    OUT22["adapter.out: TokenizationClient"]
    OUT23["adapter.out: PaymentRepository"]
  end

  IN2 --> PI2
  PO2 --> OUT21
  PO2 --> OUT22
  PO2 --> OUT23
```

---

## 8. Modularization Recommendations (Gradle Multi-Module)

```text
app/
  ├─ order-management
  │   ├─ domain
  │   ├─ application
  │   └─ infrastructure
  ├─ payments-auth
  │   ├─ domain
  │   ├─ application
  │   └─ infrastructure
  ├─ payments-settlement
  ├─ inventory-reservation
  ├─ fulfillment-orchestration
  └─ shared-kernel (only truly cross-domain common Domain Primitives/Events/Id)
```

> **Shared Kernel** strictly controlled: only place cross-context stable and common **Domain Primitives** (like Money, Quantity, Id). Avoid putting business models to prevent coupling.

---

## 9. ArchUnit Governance (Extended Version)

> Place in each module's `src/test/java/.../architecture`, with JUnit5.

```java
// 9.1 Prohibit Application depending on Infrastructure
ArchRuleDefinition.noClasses()
  .that().resideInAPackage("solid.humank.genaidemo" + ".application..")
  .should().dependOnClassesThat().resideInAnyPackage("solid.humank.genaidemo" + ".infrastructure..");

// 9.2 Domain must not depend on Application/Infrastructure
ArchRuleDefinition.noClasses()
  .that().resideInAPackage("solid.humank.genaidemo" + ".domain..")
  .should().dependOnClassesThat()
  .resideInAnyPackage("solid.humank.genaidemo" + ".application..", "solid.humank.genaidemo" + ".infrastructure..");

// 9.3 Controllers must not directly touch Repository or EntityManager
ArchRuleDefinition.noClasses()
  .that().resideInAnyPackage("solid.humank.genaidemo" + ".infrastructure.adapter.in..")
  .or().haveSimpleNameEndingWith("Controller")
  .should().accessClassesThat().haveSimpleNameEndingWith("Repository")
  .orShould().accessClassesThat().haveName("javax.persistence.EntityManager");

// 9.4 Only Application can be annotated with @Transactional (or whitelist approach)
ArchRuleDefinition.noClasses()
  .that().resideInAPackage("solid.humank.genaidemo" + ".domain..")
  .or().resideInAPackage("solid.humank.genaidemo" + ".infrastructure..")
  .should().beAnnotatedWith("org.springframework.transaction.annotation.Transactional");

// 9.5 Prohibit field injection
ArchRuleDefinition.noFields()
  .should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired");
```

---

## 10. Splitting & Evolution Strategy

1. **First run single context main flow** (Order + Auth + Reservation + Fulfillment).
2. **Observe pressure points** (consistency/language/compliance/vendors), correspondingly split out `payments-settlement`, `carrier-integration` etc. contexts.
3. **Use events as contracts** (PL), use ACL across contexts to avoid semantic leakage.
4. **Team alignment**: one stream-aligned team owns one context; set platform/enabling teams at high architectural threshold points for support.

---

## 11. Q CLI Agent Context Links

- Place this document at `docs/en/ddd-hex-rules.md`, add ArchUnit tests from `architecture-tests/**`.
- Point custom Agent's `resources` to this file, so Q references specifications and event contracts when answering.

> The above visual draft can be directly used as workshop material (Event Storming ➜ Context Map ➜ Hex/Ports ➜ Test Governance). If you want me to auto-generate ArchUnit test filenames and Gradle module skeleton based on your actual package base, tell me the base package and I'll replace and output a ready-to-run version.

---

## 12. Aggregate & Business Strict Rules (Actionable Checklist + Test Templates)

> This section converts the specifications that need strict implementation in aggregate/business logic layers into checkable rules and ArchUnit test skeletons, already replaced with your base package: `solid.humank.genaidemo`.

### 12.1 Specification Summary (Essential)

1. **Aggregate root single entry point**, state only changes through AR public behaviors.
2. **Invariant constraints** must be maintained "atomically" within AR, prohibited from scattering in application layer.
3. **Specification** expresses composable boolean rules for can/cannot; **Policy** decides how to do.
4. **State machine whitelist**: illegal transitions throw domain exceptions.
5. **Cross-aggregate only by ID reference**; no external calls within AR.
6. **Single transaction only changes single aggregate**; default optimistic locking (`@Version`).
7. **Domain events** only recorded after invariants hold and state established; sent via Outbox after commit.
8. **Application service vs Domain service**: process coordination in application layer; core rules in AR/domain service.
9. **Validation layering**: interface/application does input format checking; business rules in AR/VO/specification.
10. **Repository only for AR**; returns sufficient aggregate slices to maintain invariants.
11. **Prohibit field injection**; use constructor injection to enhance testability.
12. **@Transactional whitelist**: only allow application service/adapter-in boundaries (adjustable per project).
13. **Domain independent of Spring** (avoid technical framework pollution of model).

---

### 12.2 ArchUnit Tests (Additional)

> Suggested path: `app/src/test/java/solid/humank/genaidemo/architecture/`  
> New file: `ExtendedArchitectureRulesTest.java`

```java
package solid.humank.genaidemo.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.core.domain.JavaClass;
import org.junit.jupiter.api.Test;

class ExtendedArchitectureRulesTest {
  private static final String BASE = "solid.humank.genaidemo";

  @Test
  void transactional_annotation_whitelist() {
    JavaClasses classes = new ClassFileImporter().importPackages(BASE);
    ArchRuleDefinition.noClasses()
        .that().resideInAnyPackage(BASE + ".domain..", BASE + ".infrastructure..")
        .should().beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
        .because("@Transactional should be controlled by application layer/adapter-in, avoid polluting domain/infrastructure")
        .check(classes);
  }

  @Test
  void forbid_field_injection_autowired() {
    JavaClasses classes = new ClassFileImporter().importPackages(BASE);
    ArchRuleDefinition.noFields()
        .should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
        .because("Prohibit field injection, please use constructor injection")
        .check(classes);
  }

  @Test
  void domain_should_not_depend_on_spring_framework() {
    JavaClasses classes = new ClassFileImporter().importPackages(BASE);
    ArchRuleDefinition.noClasses()
        .that().resideInAPackage(BASE + ".domain..")
        .should().dependOnClassesThat().resideInAnyPackage("org.springframework..")
        .because("Domain model should be framework-agnostic, keep pure")
        .check(classes);
  }

  @Test
  void jpa_entity_should_have_version_field_for_optimistic_lock() {
    JavaClasses classes = new ClassFileImporter().importPackages(BASE);
    var mustHaveVersion = new ArchCondition<JavaClass>("have a @Version field") {
      @Override public void check(JavaClass clazz, ConditionEvents events) {
        boolean isEntity = clazz.isAnnotatedWith("jakarta.persistence.Entity")
                        || clazz.isAnnotatedWith("javax.persistence.Entity");
        if (!isEntity) return;
        boolean hasVersion = clazz.getFields().stream().anyMatch(f ->
            f.isAnnotatedWith("jakarta.persistence.Version") || f.isAnnotatedWith("javax.persistence.Version"));
        if (!hasVersion) {
          events.add(SimpleConditionEvent.violated(clazz,
              clazz.getName() + " should declare an @Version field for optimistic locking"));
        }
      }
    };
    ArchRuleDefinition.classes().should(mustHaveVersion).check(classes);
  }

  @Test
  void controller_should_not_access_repository_or_entitymanager_directly() {
    JavaClasses classes = new ClassFileImporter().importPackages(BASE);
    ArchRuleDefinition.noClasses()
        .that().resideInAnyPackage(BASE + ".infrastructure.adapter.in..")
        .or().haveSimpleNameEndingWith("Controller")
        .should().accessClassesThat().haveSimpleNameEndingWith("Repository")
        .orShould().accessClassesThat().haveName("jakarta.persistence.EntityManager")
        .orShould().accessClassesThat().haveName("javax.persistence.EntityManager")
        .because("Adapter-in should interact with data access through application ports")
        .check(classes);
  }
}
```

> If you already have `LayerDependencyTest / AdaptersAndControllersTest / ValueObjectImmutabilityTest` from Chapter 2, this file serves as **extended rules**; combining all four can form a more complete governance network.

---

### 12.3 Gradle Dependencies (Reminder)

Add ArchUnit and JUnit5 dependencies to `app/build.gradle`:

```gradle
dependencies {
  testImplementation platform("org.junit:junit-bom:5.10.2")
  testImplementation "org.junit.jupiter:junit-jupiter"
  testImplementation "com.tngtech.archunit:archunit-junit5:1.3.0"
}

test { useJUnitPlatform() }
```

---

### 12.4 Q CLI Agent (Resource Enhancement)

Please ensure your Agent JSON (`java-ddd-hex-genaidemo.json`) includes in `resources`:

```text
file://app/src/test/java/solid/humank/genaidemo/architecture/**/*.java
file://docs/en/ddd-hex-rules.md
```

This way Q will reference specifications and test files when answering.
