# 工具與環境

## 概覽

本目錄包含開發視點中的工具和環境配置相關文檔，涵蓋了技術堆疊、開發工具和環境設定。

## 目錄結構

- **[technology-stack/](technology-stack/)** - 技術堆疊詳細說明和配置

## 核心文檔

- **[技術堆疊](technology-stack.md)** - 完整的技術堆疊說明和配置指南

## 技術堆疊概覽

### 後端技術
- **Spring Boot 3.4.5** + Java 21 + Gradle 8.x
- **Spring Data JPA** + Hibernate + Flyway
- **H2** (開發/測試) + PostgreSQL (生產)
- **SpringDoc OpenAPI 3** + Swagger UI
- **Spring Boot Actuator** + AWS X-Ray + Micrometer

### 前端技術
- **CMC 管理系統**: Next.js 14 + React 18 + TypeScript
- **消費者應用**: Angular 18 + TypeScript
- **UI 元件**: shadcn/ui + Radix UI

### 測試框架
- **JUnit 5** + Mockito + AssertJ
- **Cucumber 7** (BDD) + Gherkin
- **ArchUnit** (架構測試)

### 開發工具
- **IDE**: IntelliJ IDEA / VS Code
- **版本控制**: Git + GitHub
- **CI/CD**: GitHub Actions
- **容器化**: Docker + Kubernetes
- **雲端平台**: AWS (CDK 部署)

## 環境配置

### 開發環境
- Java 21 JDK
- Node.js 18+
- Docker Desktop
- AWS CLI

### 測試環境
- H2 記憶體資料庫
- TestContainers
- Mock 外部服務

### 生產環境
- AWS EKS
- PostgreSQL RDS
- ElastiCache Redis
- CloudWatch 監控

## 相關資源

- [開發標準](../../../../.kiro/steering/development-standards.md)
- [部署指南](../../deployment/)
- [API 文檔](../../../api/README.md)

---

**維護者**: 開發團隊  
**最後更新**: 2025年1月21日  
**版本**: 1.0
![Microservices Overview](../../../diagrams/viewpoints/development/microservices-overview.puml)
![Microservices Overview](../../../diagrams/viewpoints/development/architecture/microservices-overview.mmd)
