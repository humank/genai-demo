# 六角架構

## 概覽

本目錄包含六角架構（Hexagonal Architecture）的設計原則和實現指南。

## 六角架構原則

### 核心概念
- **領域核心** - 業務邏輯的核心，不依賴外部技術
- **埠 (Port)** - 定義與外部世界的介面
- **適配器 (Adapter)** - 實現埠的具體技術實現
- **依賴反轉** - 外部依賴內部，而非內部依賴外部

### 層級結構
```
interfaces/ → application/ → domain/ ← infrastructure/
```

### 實現指南
- 如何設計埠和適配器
- 依賴注入的最佳實踐
- 測試策略和 Mock 使用
- 與 DDD 的整合

## 相關文檔

- [六角架構](../hexagonal-architecture.md)
- [架構決策記錄 ADR-001](../../../../architecture/adr/ADR-001-ddd-hexagonal-architecture.md)
- [開發標準](../../../../../.kiro/steering/development-standards.md)

---

**維護者**: 開發團隊  
**最後更新**: 2025年1月21日
![Microservices Overview](../../../../diagrams/viewpoints/development/microservices-overview.puml)
![Microservices Overview](../../../../diagrams/viewpoints/development/architecture/microservices-overview.mmd)