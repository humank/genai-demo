# DDD + Hexagonal Architecture: From Problem Domain to Solution Domain (genai-demo Reference)

> This document clearly illustrates the progression from **Subdomain** ➜ **Bounded Context** ➜ **Hexagonal Architecture (Ports & Adapters)**:
> 1) Event Storming Big Picture, 2) Subdomain ➜ Context Mapping, 3) Context Map, 4) Representative Sequence Diagrams, 5) Aggregates & Invariants, 6) Event Contracts (Published Language), 7) Hex Diagrams & Port/Adapter, 8) Modularization & Test Governance (ArchUnit).

---

## 0. Scope and Actors
urns.
- **Actors**: Customers, Customer Service, Warehouse Staff, Finance, Risk Management.
- **External Systems**: Payment Service Providers (PSP), Carriers, Tax/Accounting, KYL.

---

)
> Scenario-based illustration follrnal`.

### 1.1 Ordering & Payment Authorization
aid
flowchart LR
h Ordering
    C1["Coer"]
    A1 --> Ed")
  end

  E1 --> P1{"Policy: When OrderPlac"}
  P1 ent"]

  subgraph Payments
    C2 --> A2["Aggregate: Payment"]
")
    A2 --> E3("Evenlined")
  end

  E2 --> P2{"Policy: When PaymentAuthorized"}
  P2 
ned"}
  P3 --> C4["Command: CancelOrder"]

  subgraph Inventory
    C3 --> A3["Aggregate: Reservation"]
served")
    A3 --> E5("Event
  end

  E4 --> P4{"Policy: When InventoryReserved"}
  P4 ent"]

  classDef command fill:#fff,stroke:#333,stro:1px
  classDef agg fill:#eef,stroke:#335
83
  classDef policy fill:#ffd,stroke:#aa3
  class C1,C2,C3,C4,C5 command
  class A1,A2,A3 agg
  class E1,E2,E3,E4,E5 event
  class P1,P2,P3,P4 policy
```

### 1.2 Fulfillment, Shipp
```

  subgraph Fulfillment

    A4 -->d")
    C7["Comm> A4
    A4 --> E7("Event: pped")
  end

  E7 --> P5{"Policy: When OrderShip
  P5 --> C8["Command: NotifyCustomer


    C8 --> A5["Aggregate: NotificationJo]
    A5 --> E8("Event: CustomerNotified")
d
```

### 1.3 Settlement, Reconciliation & Ref
```memaid
floart LR
t
    C9["Command: CapturePayment"] --> A2["Pa
ed")
    C10["C"]
    A6 --> E")
  end

  subgraph Returns
    C11["Command: RequestReturn"] --> A1["Order"]
    A1 --> E11("Event: ReturnRequested")
    C
")
  end
```

---

## 2.ny)

 |
|--|

| Payments | Authorization, capture, reconciliation,
|
| Fulfillment | Allocation, shipping, reverse logistics | **Fulfillment Orchestration**, **Carrier Integration** | Mult
| Customer | Idencts |

> Decision Criteria: Consistency rule differences, language dialects, read/write separation, compliance boundaries, external integrations, evolution rates, organizational boundaries, regional regulations, event storming clustering.

---

ap
> Relationship patterns annotated: `PL`=Published Language, `ACL`=Anti-Corruption Layer, `OH`=Open Host Service, `C/S`=Customer/Supplier, `CF`=Conformist.

```

  OM["Order Manag
OM
  OM -->|"PL: OrderCancelled"| PA

  OM -->|"on"]
  INV --ck"| OM

  OM -->|"PL: AllocateFulfillment"| FUL["Fulfn"]
  FUL -->|"PL: FulfillmentAllocat OM

  FUL -->|"ACL & OH"| CAR["Carrier Integration"]
  PA -->|"ACL"| TOK["Tokenization (PCI CDE)"]
iation"]

  OM -->|"CF"| NOTIF["Notification"]
`

---

s

###n)

seqam
ber
  participant C as Customer

  participant OMS as OM.Domain

  particip

  C->>API: P
  API->>OMS: PlaceOrder(cmd
  OMS-->>API: OrderPlaced(evt)
  API-->>PAY: AuthorizePayment)
  PAY-->>API: PaymentAuthorized(evt)
  API-->>INV: ReserveInventory(cmd)
)
  API-->>C: 201 Created {order
```

### 4.2 Ship & Capture (Shipping anttlement)
```mermaid
sequenceDiagram
  autonumber
  participant FUL as Fulfillment.Orch
  prier



  CAR-->>F
  FUL-->>PAY: Cd)
  PAY-->>FULvt)
```

---

## 5. Aggregates & Invariants

### 5.1 Order Management
- **Order** (Aggregate Root)
  -ariants:

   odified;
.

sk
- **Payment** (AR)
ts:
    1) Authorization cannot ated;
    2) After `C;
    3) Card numbers must never be stored (Token only).

### 5.3 Inventory - Reservation
(AR)
  - Invariants:
cated;
    2) Reservation
    3) Automati.

### 5.4 Fulfillment Orchestration
- **FulfillmentOrder** (AR)
apture`.

---

## 6. Event Con Language)
> Represented using JSON Schema/examples. In practice, recommendol.

### 6.1 `OrderPlaced` (Published by OM)
on
{
Placed",
  "version": 1,
  "orderId": "ORD-2025-0001",
T-1001",
  "
": 200.0,
  "occurredAt": "2025-08-28T10:02:00Z"

```

### 6.2 `PaymentAuthorized` (Published 
```json
{
 
  "version": 2,
  "orderId": "O01",
  "paymentId": "PAY-8899",
  "authCode": "A1B2C3",
  "amount": 200.0,
  "occurredAt": "02:03Z"
}
`


```json

  "even",
 on": 1,
  "orderId": "ORD-2025-0001",
  "reservationsty": 2}],
  "expireAt": "2025-08-28T10:Z"
}
```

---

## dapters)

### 7.1 Order Management (Example)
`mermaid
flowchaB
 ation
    PI["port.in: PlaceOrderUseC"]
    PO["service"]
    PO --> PI
    PO -->|"calls"| PDO["port.out: Pricing/Inveateways"]
  end

  sDomain
r")]
   
R
    PO --> DS
  end


    AIN["a"]
    AOUT1["alient"]
    AOUT2["adapter.outent"]
    AOUT3["adapter.out: OrderReposit
  end

  AIN --> PI
  PDO1
2
  PDO --> AOUT3
```

### 7.2 Payme
```mermaid
flowc

    PI2["port.in: Authoriment"]
    S2["service: PaymentAppService"]
    S2 --> PI2
    S2 --> PO2["port.out: PSPGateway, Tok
  end

in
    AR2[("Agyment")]
    RS["DomainSn"]
    S2 --> AR2
    S2 --> RS
  end

  subgraph Infrastructure
]
    OUT21[CL)"]
    OUT22["a
    OUT23["adapter.outy"]
  end

  IN2 --> PI2
  PO2 --> OUT21
  PO2
OUT23
```

---

## 8. Modular
```

  ├─ order-management
  │   ├─ domain
  │   ├─ application
  │   └─ infrastructure
  ├─ payments-auth
  │  domain
on
  │   └─ infrure
  ├─ payments-sent
  ├─ inventory-vation
  ├─ fulfillmenation
  └s/Id)
``
> *ng.

---

## 9. Arsion)
> Pl.

```java
// 9.1 Prohibit Appl
ArchRuleDefinition.noCl
  .that().resideIn)
  .should().dep;

// 9.2 Domain must not structure
ArchRuleDefinition.noClasses()
  .that().resideInAPackage")
  .should().dependOnClassesThat()
  .resideInAnyPackage("solid.humank.genaidemo" + ".application..", "solid.humanture..");

er
ArchRuleDefinition.noClasses()
")
  .")
")
  .orShould().accessClassesThat().haveName("r");

// 9.4 Only Application can be annotated with @Transactional (or restrict via whitst)
es()
  .that
  .or().resideInAPackage("solid.humank.genaidemo" + ".i
  .should().beAnnotatedWith("o);

// 9.5 Prohibit field injection
s()
  .should().beAnnotatedWith("org.springframework.beans.factd");
```

---

Strategy
1) **First establish single context main flow** (Order + Auth + Reserv.
2) **Observe pressure points**
3) **Use events as contracts** (PL), use ACL across contexts to avoid semantic leakage.
4) **Team alignment**: One stream-aligned team.

---

## 11. Q CLI Agent Context Integration
- Place this document at `docs**`.
- Point custom Agent's `resources` to this file, allowing Q to ref

> The above visual draft can be directly used as workshop material (Event Storming ➜ Conn.



---

s)



l)
1. **Aggregate Root as single entry point**, state changes only through AR public behrs.
2. **Invariant constraints** must be maintained "atomically" within AR, prohibited from being scattered in application layer.
3. **Specification** expresses can/cannot with composable boolean rules; **Policy** dec.
4. **State machine whitelist**: Illegal transitions throw domain exceptions.

6. ).

8. **Application service vs Domain service**: .
ns.
10. **Repository only for AR**; returns aggregate slices sufficient to maintain invariants.
11. **Prohibit field injection**; use constructor injection to enhance testability.
).
13. **Domain independent of Spring** (avoid technical framework pollution of model).

---

### 12.2 ArchUnit Tests (New)

> New file: `ExtendedArchitectureRulesTest.java`

```java
e;

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
  v{

    ArchRuleDefinition.noClasses()

        .should().beAnnotatedWith("org.springframework.transaction.annotatiol")
        .because("@Transactional should be contr
);
  }

t
  void forbid_field_injection_autowired() {
    JavaClasses classes = new ClassFileImporter().importPack
    ArchRuleDefinition.noFields()
        .should().beAnnotatedWith("org.springframed")
        .because("Prohibit field injection, please use
        .check(classes);
  }

Test
  void domain_should_not_depend_on_spework() {
    JavaClasses classes = new ClassFileImporter().importPackagSE);

       omain..")
        .should().dependOnClassesThat().resid)
        .because("Domain model should be framework-agnostic, keep it pu
        .check(classes);
  }

  @Test
  void jpa_entity_should() {
   E);
") {
      @nts) {
        boolean isEntity = clazz.isAnnotatety")
                        || clazz.isAnnotatedWith("javax.persistence.Entity");
        if (!isEntity) return;
        boolean hasVersion = clazz.getFields().stream().anyMatch(f ->
            f.isAnnotatedWith("jakarta.persistence.Version") || f.isAnnotatedWion"));
        if (!hasVersion) {
   z,
);
       
      }
    };
    ArchRuleDefinition.classes().sses);
  }

  @Test
  void controller_shouldy() {
   BASE);
()
       )
        .or().haveSimpleNameEndingWith("Controller")
        .should().accessClassesThat().haveSimpleNameEndingWith("Reposit")
        .orShould().accessClassesThat().haveName("jakarta.persistence.EntityManagr")
        .orShould().accessClassesThat().haveName("javax.persistence.EntityMar")
        .because("Adapter-in should interact with data access through applicat)
        .check(classes);
  }
}
```

> If you already have `LayerDependencyTest / AdaptersAndCtwork.

---

### 12eminder)
Add ArchUnit and JUnit5 dependencies to `app/build.gradle`:
```e
cies {
  testI)
  testImplementation "org.junit.jupiter:junit-jupiter"
  testImplementation "com.tngtech.archunit:archunit-junit5:1.3.0"
}

test { useJUnitPlatform() }
```

---

### 12.4 Q CLI Agent (Re)
Ple
```
filva
les.md
```
ponding.


---

## 12. Aggregate and Business Strict Rules (Actionable Checklist + Test Templates)

> This section converts the specifications that need to be strictly implemented at the aggregate/business logic layer into checkable rules and ArchUnit test skeletons, already replaced with your base package: `solid.humank.genaidemo`.

### 12.1 Specification Summary (Essential)
1. **Aggregate Root as single entry point**, state changes only through AR public behaviors.
2. **Invariant constraints** must be maintained "atomically" within AR, prohibited from being scattered in application layer.
3. **Specification** expresses can/cannot with composable boolean rules; **Policy** decides how to do it.
4. **State machine whitelist**: Illegal transitions throw domain exceptions.
5. **Cross-aggregate references by ID only**; no external calls within AR.
6. **Single transaction modifies single aggregate**; default optimistic locking (`@Version`).
7. **Domain events** recorded only after invariants are established and state is confirmed; sent via Outbox after commit.
8. **Application service vs Domain service**: Process coordination in application layer; core rules in AR/domain services.
9. **Validation layering**: Interface/application does input format checking; business rules in AR/VO/specifications.
10. **Repository only for AR**; returns aggregate slices sufficient to maintain invariants.
11. **Prohibit field injection**; use constructor injection to enhance testability.
12. **@Transactional whitelist**: Only allow application service/adapter-in boundaries (adjustable per project).
13. **Domain independent of Spring** (avoid technical framework pollution of model).

---

### 12.2 ArchUnit Tests (New)
> Recommended path: `app/src/test/java/solid/humank/genaidemo/architecture/`  
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
        .because("Domain model should be framework-agnostic, keep it pure")
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
```
file://app/src/test/java/solid/humank/genaidemo/architecture/**/*.java
file://docs/ddd-hex-rules.md
```
This way Q will reference specifications and test files when answering.