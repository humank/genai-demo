# Saga 模式

## 概覽

本目錄包含 Saga 模式的實現指南，用於管理微服務架構中的分散式事務。

## Saga 模式類型

### 編排式 Saga (Choreography)
- **去中心化** - 每個服務知道下一步該做什麼
- **事件驅動** - 透過領域事件協調流程
- **鬆耦合** - 服務間沒有直接依賴

### 協調式 Saga (Orchestration)
- **中心化** - 由協調器管理整個流程
- **明確控制** - 清楚的流程控制和錯誤處理
- **易於監控** - 集中的狀態管理

## 實現策略

### 補償動作設計
- **冪等性** - 補償動作可以安全重複執行
- **可逆性** - 每個步驟都有對應的補償動作
- **順序性** - 補償動作按相反順序執行

### 錯誤處理
- **重試機制** - 暫時性錯誤的重試策略
- **熔斷器** - 防止級聯失敗
- **死信佇列** - 處理無法恢復的錯誤

## 技術實現

### Spring Boot 實現
```java
@Component
public class OrderProcessingSaga {
    
    @SagaOrchestrationStart
    public void processOrder(OrderCreatedEvent event) {
        // 開始 Saga 流程
    }
    
    @SagaOrchestrationParticipant
    public void reserveInventory(ReserveInventoryCommand command) {
        // 庫存預留步驟
    }
    
    @SagaOrchestrationParticipant
    public void processPayment(ProcessPaymentCommand command) {
        // 付款處理步驟
    }
}
```

## 相關文檔

- [領域事件](../../../../../.kiro/steering/domain-events.md)
- [微服務架構](../microservices/)
- [事件驅動架構](../../../../diagrams/mermaid/event-driven-architecture.md)

---

**維護者**: 開發團隊  
**最後更新**: 2025年1月21日
![Microservices Overview](../../../../diagrams/viewpoints/development/microservices-overview.puml)
![Microservices Overview](../../../../diagrams/viewpoints/development/architecture/microservices-overview.mmd)
