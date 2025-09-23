# 技術堆疊詳細說明

## 概覽

本目錄包含專案技術堆疊的詳細配置和使用指南。

## 技術堆疊架構

### 後端技術堆疊
```
┌─────────────────────────────────────┐
│           API Layer                 │
│    Spring Boot 3.4.5 + Java 21     │
├─────────────────────────────────────┤
│         Application Layer           │
│      Spring Framework 6.x          │
├─────────────────────────────────────┤
│          Domain Layer               │
│         Pure Java + DDD             │
├─────────────────────────────────────┤
│       Infrastructure Layer          │
│  Spring Data JPA + Hibernate       │
│      PostgreSQL + H2               │
└─────────────────────────────────────┘
```

### 前端技術堆疊
```
┌─────────────────────────────────────┐
│        CMC Management               │
│   Next.js 14 + React 18 + TS       │
├─────────────────────────────────────┤
│       Consumer App                  │
│      Angular 18 + TypeScript       │
├─────────────────────────────────────┤
│        UI Components                │
│     shadcn/ui + Radix UI           │
└─────────────────────────────────────┘
```

## 核心技術配置

### Spring Boot 配置
- **版本**: 3.4.5
- **Java**: 21 (LTS)
- **建構工具**: Gradle 8.x
- **打包**: JAR with embedded Tomcat

### 資料庫配置
- **開發環境**: H2 記憶體資料庫
- **測試環境**: H2 檔案資料庫
- **生產環境**: PostgreSQL 15+
- **遷移工具**: Flyway

### 監控和觀測
- **健康檢查**: Spring Boot Actuator
- **指標收集**: Micrometer + Prometheus
- **分散式追蹤**: AWS X-Ray
- **日誌**: Logback + Structured Logging

## 開發工具

### IDE 和編輯器
- **推薦**: IntelliJ IDEA Ultimate
- **替代**: VS Code + Java Extension Pack
- **配置**: EditorConfig, Checkstyle

### 版本控制
- **Git**: 分散式版本控制
- **GitHub**: 程式碼託管和協作
- **分支策略**: GitFlow

### CI/CD 工具
- **GitHub Actions**: 持續整合和部署
- **Docker**: 容器化
- **AWS CDK**: 基礎設施即程式碼

## 相關文檔

- [技術堆疊](../technology-stack.md)
- [開發環境設定](../../getting-started/)
- [建構和部署](../../build-system/)

---

**維護者**: 開發團隊  
**最後更新**: 2025年1月21日
![Microservices Overview](../../../../diagrams/viewpoints/development/microservices-overview.puml)
![Microservices Overview](../../../../diagrams/viewpoints/development/architecture/microservices-overview.mmd)