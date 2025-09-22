# 架構決策記錄 (ADR)

本目錄包含所有重要的架構決策記錄，遵循 ADR (Architecture Decision Records) 格式。

## ADR 清單

### 核心架構決策

- [ADR-001: DDD 戰術模式](../../../architecture/adrs/ADR-001-ddd-tactical-patterns.md)
- [ADR-002: 六角架構](../../../architecture/adrs/ADR-002-hexagonal-architecture.md)
- [ADR-003: 微服務架構](../../../architecture/adrs/ADR-003-microservices-architecture.md)
- [ADR-004: Saga 模式選擇](../../../architecture/adrs/ADR-004-saga-pattern-selection.md)

### 技術選擇決策

- Spring Boot 3.4.5 + Java 21 技術棧選擇
- PostgreSQL + H2 資料庫策略
- Next.js + Angular 前端架構
- AWS CDK 基礎設施即程式碼

## ADR 格式

每個 ADR 都遵循以下格式：

```markdown
# ADR-XXX: 決策標題

## 狀態
[提議中 | 已接受 | 已棄用 | 被 ADR-XXX 取代]

## 背景
描述需要做出決策的問題和背景。

## 決策
描述我們選擇的解決方案。

## 後果
描述決策的正面和負面影響。

## 相關決策
列出相關的其他 ADR。
```

## 建立新的 ADR

1. 複製 ADR 模板
2. 分配下一個 ADR 編號
3. 填寫所有必要章節
4. 提交 Pull Request 進行審查
5. 獲得批准後更新狀態為「已接受」

## 相關資源

- [架構總覽](../README.md)
- [設計原則](../design-principles.md)
- [Rozanski & Woods 方法論](../../../../.kiro/steering/rozanski-woods-architecture-methodology.md)

---

*ADR 是記錄重要架構決策的重要工具，幫助團隊理解為什麼做出特定的技術選擇。*