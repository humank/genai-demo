# 領域驅動設計模式

## 概覽

本目錄包含領域驅動設計（DDD）的戰術模式實現和最佳實踐。

## DDD 戰術模式

### 核心概念
- **聚合根 (Aggregate Root)** - 聚合的入口點和一致性邊界
- **實體 (Entity)** - 具有唯一標識的領域對象
- **值對象 (Value Object)** - 不可變的描述性對象
- **領域服務 (Domain Service)** - 不屬於特定實體的業務邏輯
- **領域事件 (Domain Event)** - 領域中發生的重要事件

### 實現指南
- 聚合設計原則
- 實體與值對象的區別
- 領域服務的使用時機
- 領域事件的發布和處理

## 相關文檔

- [領域驅動設計](../ddd-domain-driven-design.md)
- [領域事件指南](../../../../../.kiro/steering/domain-events.md)
- [SOLID 原則](../../solid-principles-and-design-patterns.md)

---

**維護者**: 開發團隊  
**最後更新**: 2025年1月21日
![Microservices Overview](../../../../diagrams/viewpoints/development/microservices-overview.puml)
![Microservices Overview](../../../../diagrams/viewpoints/development/architecture/microservices-overview.mmd)
