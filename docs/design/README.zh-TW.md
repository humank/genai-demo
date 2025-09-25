# 📍 設計文檔已遷移

> **重要通知**: 設計相關文檔已遷移到新的 Development Viewpoint 架構模式中

## 🚀 新位置

所有設計模式和架構指南現在整合在 **[Development Viewpoint 架構模式](../viewpoints/development/architecture/)** 中，提供更系統化的架構設計指南。

## 📋 文檔遷移對照表

| 原始文檔 | 新位置 | 說明 |
|----------|--------|------|
| [ddd-guide.md](ddd-guide.md) | **DDD 戰術模式** | @AggregateRoot、@ValueObject、@DomainService 實作指南 |
| [design-principles.md](design-principles.md) | **SOLID 原則** | SOLID 原則和設計模式應用 |
| [refactoring-guide.md](refactoring-guide.md) | **重構策略** | 程式碼重構指南和最佳實踐 |

## 🏗️ 新的架構模式結構

### DDD 模式
- **戰術模式** - @AggregateRoot、@ValueObject、@Entity、@DomainService
- **領域事件** - Record 實作、事件收集與發布
- **聚合設計** - 聚合根設計原則

### 六角架構
- **Port-Adapter 模式** - 端口與適配器實作
- **依賴反轉** - 依賴反轉原則應用
- **分層設計** - 分層設計和邊界定義

### 微服務模式
- **API Gateway** - 路由、認證、限流配置
- **服務發現** - EKS 服務發現機制
- **斷路器模式** - 故障隔離和自動恢復

### Saga 模式
- **編排式 Saga** - 中央協調器模式
- **編舞式 Saga** - 事件驅動協調
- **訂單處理 Saga** - 實際業務流程範例

## 🎯 設計原則整合

### SOLID 原則
- **單一職責原則** - 實際程式碼範例
- **開放封閉原則** - 擴展性設計模式
- **依賴反轉原則** - 在六角架構中的應用

### 設計模式應用
- **Factory 和 Builder 模式** - 複雜物件創建
- **Strategy 和 Observer 模式** - 行為模式實現
- **Show Don't Ask 原則** - 物件行為封裝

## 🔗 快速導航

- **🏗️ 架構模式總覽** - 所有架構模式入口
- **📐 DDD 實踐** - 領域驅動設計
- **🔧 六角架構** - Port-Adapter 模式
- **🌐 微服務架構** - 分散式系統設計

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

**需要幫助？** 請參考 Development Viewpoint 架構指南 或查看 [專案文檔中心](../README.md)
