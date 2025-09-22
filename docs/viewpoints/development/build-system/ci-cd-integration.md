# 🚀 CI/CD 整合

本文檔描述 🚀 CI/CD 整合 的配置和使用方法。

## 建置系統概覽

### 技術棧

- **Gradle 8.x**：建置工具
- **Java 21**：開發語言
- **Spring Boot 3.4.5**：應用框架

### 建置目標

- 快速建置和測試
- 一致的開發環境
- 自動化品質檢查

## 配置指南

### Gradle 配置

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.4.5'
    id 'io.spring.dependency-management' version '1.1.4'
}

java {
    sourceCompatibility = '21'
}
```

### 依賴管理

- 使用 Gradle 版本目錄
- 統一管理依賴版本
- 定期更新依賴

## 建置任務

### 常用命令

```bash
# 編譯專案
./gradlew build

# 執行測試
./gradlew test

# 執行應用
./gradlew bootRun
```

## 相關文檔

- [建置系統總覽](../README.md)
- [開發環境設置](../getting-started/environment-setup.md)
- [CI/CD 整合](ci-cd-integration.md)

---

*本文檔遵循 [開發標準](../../../../.kiro/steering/development-standards.md)*