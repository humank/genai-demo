# 📍 設計文檔已遷移

> **重要通知**: 設計相關文檔已遷移到新的 Development Viewpoint 架構模式中

## 🚀 新位置

所有設計模式和架構指南現在整合在 **[Development Viewpoint 架構模式](../viewpoints/development/architecture/)** 中，提供更系統化的架構設計指南。

## 📋 文檔遷移對照表

| 原始文檔 | 新位置 | 說明 |
|----------|--------|------|
| [ddd-guide.md](ddd-guide.md) | **[DDD 戰術模式](../viewpoints/development/architecture/ddd-patterns/tactical-patterns.md)** | @AggregateRoot、@ValueObject、@DomainService 實作指南 |
| [design-principles.md](design-principles.md) | **[SOLID 原則](../viewpoints/development/architecture/design-principles/solid-principles.md)** | SOLID 原則和設計模式應用 |
| [refactoring-guide.md](refactoring-guide.md) | **[重構策略](../viewpoints/development/workflows/refactoring-strategy.md)** | 程式碼重構指南和最佳實踐 |

## 🏗️ 新的架構模式結構

### [DDD 模式](../viewpoints/development/architecture/ddd-patterns/)
- **[戰術模式](../viewpoints/development/architecture/ddd-patterns/tactical-patterns.md)** - @AggregateRoot、@ValueObject、@Entity、@DomainService
- **[領域事件](../viewpoints/development/architecture/ddd-patterns/domain-events.md)** - Record 實作、事件收集與發布
- **[聚合設計](../viewpoints/development/architecture/ddd-patterns/aggregate-design.md)** - 聚合根設計原則

### [六角架構](../viewpoints/development/architecture/hexagonal-architecture/)
- **[Port-Adapter 模式](../viewpoints/development/architecture/hexagonal-architecture/ports-adapters.md)** - 端口與適配器實作
- **[依賴反轉](../viewpoints/development/architecture/hexagonal-architecture/dependency-inversion.md)** - 依賴反轉原則應用
- **[分層設計](../viewpoints/development/architecture/hexagonal-architecture/layered-design.md)** - 分層設計和邊界定義

### [微服務模式](../viewpoints/development/architecture/microservices/)
- **[API Gateway](../viewpoints/development/architecture/microservices/api-gateway.md)** - 路由、認證、限流配置
- **[服務發現](../viewpoints/development/architecture/microservices/service-discovery.md)** - EKS 服務發現機制
- **[斷路器模式](../viewpoints/development/architecture/microservices/circuit-breaker.md)** - 故障隔離和自動恢復

### [Saga 模式](../viewpoints/development/architecture/saga-patterns/)
- **[編排式 Saga](../viewpoints/development/architecture/saga-patterns/orchestration.md)** - 中央協調器模式
- **[編舞式 Saga](../viewpoints/development/architecture/saga-patterns/choreography.md)** - 事件驅動協調
- **[訂單處理 Saga](../viewpoints/development/architecture/saga-patterns/order-processing-saga.md)** - 實際業務流程範例

## 🎯 設計原則整合

### [SOLID 原則](../viewpoints/development/architecture/design-principles/solid-principles.md)
- **單一職責原則** - 實際程式碼範例
- **開放封閉原則** - 擴展性設計模式
- **依賴反轉原則** - 在六角架構中的應用

### 設計模式應用
- **Factory 和 Builder 模式** - 複雜物件創建
- **Strategy 和 Observer 模式** - 行為模式實現
- **Show Don't Ask 原則** - 物件行為封裝

## 🔗 快速導航

- **[🏗️ 架構模式總覽](../viewpoints/development/architecture/README.md)** - 所有架構模式入口
- **[📐 DDD 實踐](../viewpoints/development/architecture/ddd-patterns/README.md)** - 領域驅動設計
- **[🔧 六角架構](../viewpoints/development/architecture/hexagonal-architecture/README.md)** - Port-Adapter 模式
- **[🌐 微服務架構](../viewpoints/development/architecture/microservices/README.md)** - 分散式系統設計

## 📅 遷移資訊

- **遷移日期**: 2025年1月21日
- **過渡期**: 2025年2月底前
- **舊文檔移除**: 2025年3月1日

## 💡 為什麼遷移？

1. **更完整的架構指南**: 整合了實際程式碼實作和最佳實踐
2. **實作導向**: 從理論轉向實際可執行的程式碼範例
3. **系統化組織**: 按照架構模式類型進行邏輯分組
4. **更好的維護性**: 減少內容重複，提高文檔品質

---

**需要幫助？** 請參考 [Development Viewpoint 架構指南](../viewpoints/development/architecture/README.md) 或查看 [專案文檔中心](../README.md)
