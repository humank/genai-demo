# Flowchart Example: Order Processing

This example demonstrates a typical order processing flowchart using Mermaid.

## Simple Flowchart

```mermaid
graph TD
    Start[Order Received] --> Validate{Validate Order}
    Validate -->|Valid| CheckInventory[Check Inventory]
    Validate -->|Invalid| Reject[Reject Order]
    CheckInventory --> HasStock{Stock Available?}
    HasStock -->|Yes| ProcessPayment[Process Payment]
    HasStock -->|No| BackOrder[Create Back Order]
    ProcessPayment --> PaymentOK{Payment Success?}
    PaymentOK -->|Yes| Confirm[Confirm Order]
    PaymentOK -->|No| PaymentFailed[Payment Failed]
    Confirm --> Ship[Ship Order]
    Ship --> Complete[Order Complete]
    Reject --> NotifyCustomer[Notify Customer]
    PaymentFailed --> NotifyCustomer
    BackOrder --> NotifyCustomer
    NotifyCustomer --> End[End]
    Complete --> End
```

## Styled Flowchart with Colors

```mermaid
graph TD
    Start[Order Received] --> Validate{Validate Order}
    Validate -->|Valid| CheckInventory[Check Inventory]
    Validate -->|Invalid| Reject[Reject Order]
    CheckInventory --> HasStock{Stock Available?}
    HasStock -->|Yes| ProcessPayment[Process Payment]
    HasStock -->|No| BackOrder[Create Back Order]
    ProcessPayment --> PaymentOK{Payment Success?}
    PaymentOK -->|Yes| Confirm[Confirm Order]
    PaymentOK -->|No| PaymentFailed[Payment Failed]
    Confirm --> Ship[Ship Order]
    Ship --> Complete[Order Complete]
    
    style Start fill:#90EE90
    style Complete fill:#90EE90
    style Reject fill:#FFB6C1
    style PaymentFailed fill:#FFB6C1
    style Confirm fill:#87CEEB
    style Ship fill:#87CEEB
```

## Horizontal Flowchart

```mermaid
graph LR
    A[Customer] --> B[Submit Order]
    B --> C[Validate]
    C --> D[Process Payment]
    D --> E[Confirm]
    E --> F[Ship]
    F --> G[Deliver]
```

## Flowchart with Subgraphs

```mermaid
graph TB
    subgraph "Order Validation"
        A[Receive Order] --> B{Valid Format?}
        B -->|Yes| C{Items Available?}
        B -->|No| D[Reject]
    end
    
    subgraph "Payment Processing"
        C -->|Yes| E[Calculate Total]
        E --> F[Process Payment]
        F --> G{Payment OK?}
    end
    
    subgraph "Fulfillment"
        G -->|Yes| H[Reserve Inventory]
        H --> I[Generate Shipping Label]
        I --> J[Ship Order]
    end
    
    G -->|No| K[Cancel Order]
    D --> L[Notify Customer]
    K --> L
    J --> M[Complete]
```

## Use Cases

- **Process Documentation**: Document business processes
- **Algorithm Visualization**: Show algorithm flow
- **Decision Trees**: Visualize decision logic
- **Workflow Design**: Design and communicate workflows

## Tips

1. Use descriptive node labels
2. Keep decision nodes (diamonds) for yes/no questions
3. Use colors to highlight different states
4. Group related steps with subgraphs
5. Limit to 15-20 nodes per diagram for clarity
