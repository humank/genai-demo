# Event Storming - Process Modeling (繁體中文版)

> **注意**: 此文件需要翻譯。原始英文版本請參考對應的英文文件。

# Event Storming - Process Modeling

## Core Business Processes

### 1. Customer Onboarding Process

```mermaid
graph TD
    A[Customer Visits Site] --> B{First Time Visitor?}
    B -->|Yes| C[Registration Form]
    B -->|No| D[Login Process]
    
    C --> E[Submit Registration]
    E --> F[Email Verification Sent]
    F --> G{Email Verified?}
    G -->|Yes| H[Account Activated]
    G -->|No| I[Resend Verification]
    I --> G
    
    H --> J[Welcome Email Sent]
    J --> K[Profile Setup]
    K --> L[Customer Registered Event]
    
    D --> M[Authentication]
    M --> N{Login Success?}
    N -->|Yes| O[Customer Logged In]
    N -->|No| P[Login Failed]
    P --> D
    
    L --> Q[Customer Ready to Shop]
    O --> Q
    
    classDef start fill:#4caf50,stroke:#2e7d32,stroke-width:2px
    classDef process fill:#2196f3,stroke:#1565c0,stroke-width:2px
    classDef decision fill:#ff9800,stroke:#ef6c00,stroke-width:2px
    classDef event fill:#9c27b0,stroke:#6a1b9a,stroke-width:2px
    classDef end fill:#f44336,stroke:#c62828,stroke-width:2px
    
    class A start
    class C,E,F,H,J,K,D,M,O process
    class B,G,N decision
    class L event
    class Q end
```

### 2. Order Processing Workflow

```mermaid
graph TD
    A[Customer Browses Products] --> B[Add to Cart]
    B --> C[Cart Updated Event]
    C --> D{Continue Shopping?}
    D -->|Yes| A
    D -->|No| E[Proceed to Checkout]
    
    E --> F[Apply Promotions]
    F --> G[Promotion Applied Event]
    G --> H[Calculate Total]
    H --> I[Create Order]
    I --> J[Order Created Event]
    
    J --> K[Reserve Inventory]
    K --> L{Inventory Available?}
    L -->|Yes| M[Inventory Reserved Event]
    L -->|No| N[Inventory Insufficient Event]
    N --> O[Notify Customer]
    O --> P[Update Cart]
    P --> D
    
    M --> Q[Process Payment]
    Q --> R{Payment Success?}
    R -->|Yes| S[Payment Processed Event]
    R -->|No| T[Payment Failed Event]
    T --> U[Release Inventory]
    U --> V[Inventory Released Event]
    V --> W[Notify Customer]
    W --> E
    
    S --> X[Confirm Order]
    X --> Y[Order Confirmed Event]
    Y --> Z[Schedule Delivery]
    Z --> AA[Delivery Scheduled Event]
    
    classDef start fill:#4caf50,stroke:#2e7d32,stroke-width:2px
    classDef process fill:#2196f3,stroke:#1565c0,stroke-width:2px
    classDef decision fill:#ff9800,stroke:#ef6c00,stroke-width:2px
    classDef event fill:#9c27b0,stroke:#6a1b9a,stroke-width:2px
    classDef error fill:#f44336,stroke:#c62828,stroke-width:2px
    
    class A start
    class B,E,F,H,I,K,O,P,Q,U,W,X,Z process
    class D,L,R decision
    class C,G,J,M,N,S,T,V,Y,AA event
    class N,T error
```

### 3. Delivery and Fulfillment Process

```mermaid
graph TD
    A[Order Confirmed] --> B[Prepare Shipment]
    B --> C[Package Items]
    C --> D[Generate Shipping Label]
    D --> E[Assign Carrier]
    E --> F[Delivery Scheduled Event]
    
    F --> G[Ship Package]
    G --> H[Order Shipped Event]
    H --> I[Tracking Number Generated]
    I --> J[Customer Notified]
    
    J --> K[Delivery in Transit]
    K --> L[Delivery Started Event]
    L --> M[Out for Delivery]
    M --> N{Delivery Attempt}
    
    N -->|Successful| O[Package Delivered]
    N -->|Failed| P[Delivery Failed Event]
    P --> Q[Reschedule Delivery]
    Q --> M
    
    O --> R[Delivery Completed Event]
    R --> S[Customer Confirmation]
    S --> T{Confirmed by Customer?}
    T -->|Yes| U[Delivery Confirmed Event]
    T -->|No| V[Auto-confirm after 24h]
    V --> U
    
    U --> W[Update Order Status]
    W --> X[Trigger Review Request]
    X --> Y[Review Requested Event]
    
    classDef start fill:#4caf50,stroke:#2e7d32,stroke-width:2px
    classDef process fill:#2196f3,stroke:#1565c0,stroke-width:2px
    classDef decision fill:#ff9800,stroke:#ef6c00,stroke-width:2px
    classDef event fill:#9c27b0,stroke:#6a1b9a,stroke-width:2px
    classDef error fill:#f44336,stroke:#c62828,stroke-width:2px
    classDef end fill:#795548,stroke:#5d4037,stroke-width:2px
    
    class A start
    class B,C,D,E,G,I,J,K,M,O,Q,S,V,W,X process
    class N,T decision
    class F,H,L,P,R,U,Y event
    class P error
    class Y end
```

### 4. Customer Loyalty and Rewards Process

```mermaid
graph TD
    A[Order Delivered] --> B[Calculate Reward Points]
    B --> C[Points Calculation Policy]
    C --> D[Base Points + Bonus Points]
    D --> E[Reward Points Earned Event]
    
    E --> F[Update Customer Balance]
    F --> G[Customer Spending Updated Event]
    G --> H[Check Loyalty Tier]
    H --> I{Tier Upgrade Eligible?}
    
    I -->|Yes| J[Upgrade Loyalty Tier]
    I -->|No| K[Maintain Current Tier]
    
    J --> L[Loyalty Status Updated Event]
    K --> M[Continue Current Benefits]
    
    L --> N[Send Tier Upgrade Notification]
    M --> O[Regular Loyalty Benefits]
    N --> P[Update Customer Profile]
    O --> P
    
    P --> Q[Personalized Offers]
    Q --> R[Promotion Eligibility Check]
    R --> S[Targeted Marketing Event]
    
    %% Parallel process for review collection
    A --> T[Review Request Timer]
    T --> U[Send Review Request]
    U --> V[Review Requested Event]
    V --> W{Review Submitted?}
    W -->|Yes| X[Review Submitted Event]
    W -->|No| Y[Follow-up Reminder]
    Y --> Z[Review Reminder Sent Event]
    
    X --> AA[Review Bonus Points]
    AA --> BB[Additional Points Earned Event]
    BB --> F
    
    classDef start fill:#4caf50,stroke:#2e7d32,stroke-width:2px
    classDef process fill:#2196f3,stroke:#1565c0,stroke-width:2px
    classDef decision fill:#ff9800,stroke:#ef6c00,stroke-width:2px
    classDef event fill:#9c27b0,stroke:#6a1b9a,stroke-width:2px
    classDef policy fill:#607d8b,stroke:#37474f,stroke-width:2px
    classDef end fill:#795548,stroke:#5d4037,stroke-width:2px
    
    class A start
    class B,D,F,H,J,K,N,M,P,Q,R,T,U,Y,AA process
    class I,W decision
    class E,G,L,S,V,X,Z,BB event
    class C policy
    class S end
```

## Cross-Context Event Flows

### Order-to-Payment Flow

```mermaid
sequenceDiagram
    participant Order as Order Context
    participant Payment as Payment Context
    participant Inventory as Inventory Context
    participant Customer as Customer Context
    
    Note over Order: Order Creation Process
    Order->>Inventory: Check Availability
    Inventory-->>Order: Availability Confirmed
    
    Order->>Order: Create Order
    Order->>Payment: Payment Requested Event
    
    Note over Payment: Payment Processing
    Payment->>Payment: Validate Payment Method
    Payment->>Payment: Process Payment
    
    alt Payment Success
        Payment-->>Order: Payment Processed Event
        Order->>Inventory: Reserve Inventory
        Inventory-->>Order: Inventory Reserved Event
        Order->>Order: Confirm Order
        Order->>Customer: Order Confirmed Event
    else Payment Failure
        Payment-->>Order: Payment Failed Event
        Order->>Customer: Payment Failed Notification
        Order->>Order: Cancel Order
    end
```

### Delivery-to-Loyalty Flow

```mermaid
sequenceDiagram
    participant Delivery as Delivery Context
    participant Order as Order Context
    participant Customer as Customer Context
    participant Loyalty as Loyalty Context
    
    Note over Delivery: Delivery Completion
    Delivery->>Delivery: Complete Delivery
    Delivery->>Order: Delivery Completed Event
    
    Order->>Order: Update Order Status
    Order->>Customer: Order Delivered Event
    
    Note over Customer: Loyalty Processing
    Customer->>Loyalty: Calculate Rewards
    Loyalty->>Loyalty: Apply Loyalty Policy
    Loyalty-->>Customer: Reward Points Earned Event
    
    Customer->>Customer: Update Customer Profile
    Customer->>Customer: Check Tier Eligibility
    
    alt Tier Upgrade
        Customer->>Loyalty: Loyalty Status Updated Event
        Loyalty-->>Customer: Tier Benefits Activated
    else No Upgrade
        Customer->>Customer: Maintain Current Status
    end
```

## Error Handling and Compensation

### Payment Failure Compensation

```mermaid
graph TD
    A[Payment Failed Event] --> B[Release Reserved Inventory]
    B --> C[Inventory Released Event]
    C --> D[Notify Customer]
    D --> E[Send Payment Retry Options]
    E --> F{Customer Action}
    
    F -->|Retry Payment| G[New Payment Attempt]
    F -->|Change Payment Method| H[Update Payment Info]
    F -->|Cancel Order| I[Cancel Order Process]
    
    G --> J[Payment Requested Event]
    H --> J
    I --> K[Order Cancelled Event]
    
    J --> L{Payment Success?}
    L -->|Yes| M[Continue Order Process]
    L -->|No| A
    
    K --> N[Full Compensation Complete]
    M --> O[Order Processing Resumed]
    
    classDef error fill:#f44336,stroke:#c62828,stroke-width:2px
    classDef compensation fill:#ff9800,stroke:#ef6c00,stroke-width:2px
    classDef decision fill:#2196f3,stroke:#1565c0,stroke-width:2px
    classDef event fill:#9c27b0,stroke:#6a1b9a,stroke-width:2px
    classDef success fill:#4caf50,stroke:#2e7d32,stroke-width:2px
    
    class A,K error
    class B,C,D,E,G,H,I compensation
    class F,L decision
    class C,J,K event
    class M,N,O success
```


---
*此文件由自動翻譯系統生成，可能需要人工校對。*
