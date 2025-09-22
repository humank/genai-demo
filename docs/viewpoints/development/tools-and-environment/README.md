# 工具鏈

本文檔描述 工具鏈 的配置和使用指南。

## 技術棧概覽

### 後端技術

- **Spring Boot 3.4.5**：應用框架
- **Java 21**：程式語言
- **Gradle 8.x**：建置工具

### 前端技術

- **Next.js 14 + React 18**：CMC 管理介面
- **Angular 18 + TypeScript**：消費者應用
- **shadcn/ui + Radix UI**：UI 組件

### 測試框架

- **JUnit 5**：單元測試
- **Mockito**：模擬框架
- **Cucumber 7**：BDD 測試

## 環境配置

### 開發環境

```bash
# 安裝 Java 21
sdk install java 21.0.1-tem

# 設定環境變數
export JAVA_HOME=$HOME/.sdkman/candidates/java/current
```

### IDE 配置

- IntelliJ IDEA 推薦設定
- VS Code 擴充套件
- Eclipse 配置指南

## 工具整合

### 版本控制

- Git 工作流程
- 分支策略
- 提交規範

### CI/CD

- GitHub Actions
- 自動化測試
- 部署流程

## 相關文檔

- [工具鏈總覽](../README.md)
- [環境設置](../getting-started/environment-setup.md)
- [技術棧詳細說明](technology-stack/README.md)

---

*本文檔遵循 [開發標準](../../../../.kiro/steering/development-standards.md)*