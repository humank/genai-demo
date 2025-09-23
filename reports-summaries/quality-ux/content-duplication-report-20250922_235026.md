# Content Duplication Detection Report
Generated: Mon Sep 22 23:50:26 CST 2025
Threshold: 80.0%

## Summary
- Total duplicates found: 4
- Full document duplicates: 0
- Section duplicates: 4

## Detailed Findings

### Duplicate 1: Section
**Similarity**: 81.58%
**File 1**: `docs/viewpoints/development/coding-standards.md`
**File 2**: `docs/viewpoints/development/api/rest-api-design.md`

**Section 1 Preview**: #### URL 命名標準
```
GET    /../api/v1/customers                    # 列出客戶
GET    /api/v1/customers/{id...
**Section 2 Preview**: ### URL 設計規範
```
GET    /api/v1/customers           # 取得客戶列表
GET    /api/v1/customers/{id}      # 取得...

---

### Duplicate 2: Section
**Similarity**: 93.58%
**File 1**: `docs/viewpoints/development/testing/tdd-bdd-testing.md`
**File 2**: `docs/viewpoints/development/tools-and-environment/technology-stack.md`

**Section 1 Preview**: #### 基本語法結構

```gherkin
Feature: 客戶管理
  作為系統管理員
  我想要管理客戶資料
  以便提供更好的服務

  Background:
    Given 系統已...
**Section 2 Preview**: # src/test/resources/features/customer-management.feature
Feature: 客戶管理
  作為系統管理員
  我想要管理客戶資料
  以便提供...

---

### Duplicate 3: Section
**Similarity**: 89.58%
**File 1**: `docs/viewpoints/development/tools-and-environment/technology-stack.md`
**File 2**: `docs/viewpoints/development/quality-assurance/quality-assurance.md`

**Section 1 Preview**: #### Micrometer + Prometheus 配置

```java
@Configuration
public class MetricsConfiguration {
    
   ...
**Section 2 Preview**: #### Micrometer 配置

```java
@Configuration
public class MetricsConfiguration {
    
    @Bean
    pu...

---

### Duplicate 4: Section
**Similarity**: 91.06%
**File 1**: `docs/viewpoints/development/tools-and-environment/technology-stack.md`
**File 2**: `docs/viewpoints/development/build-system/build-deployment.md`

**Section 1 Preview**: # Management endpoints
management:
  endpoints:
    web:
      exposure:
        include: health,inf...
**Section 2 Preview**: # application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,met...

---

## Recommendations

1. **Review High Similarity Content**: Examine content with >90% similarity
2. **Consolidate Duplicates**: Merge similar sections where appropriate
3. **Create Cross-References**: Use links instead of duplicating content
4. **Establish Single Source of Truth**: Designate authoritative sources for common topics
