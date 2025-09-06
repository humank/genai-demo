<!-- This document needs manual translation from Chinese to English -->
<!-- 此文檔需要從中文手動翻譯為英文 -->

# API 版本管理策略

## 概述

本文檔定義了 GenAI Demo 專案的 API 版本管理策略，確保 API 的向後相容性和平滑升級。

## 版本控制方案

### 1. 版本號格式

採用語義化版本控制 (Semantic Versioning)：`MAJOR.MINOR.PATCH`

- **MAJOR**: 不相容的 API 變更
- **MINOR**: 向後相容的功能新增
- **PATCH**: 向後相容的問題修正

### 2. 版本控制方式

#### URL 路徑版本控制 (主要方式)

```
/api/v1/products
/api/v2/products
```

#### HTTP Header 版本控制 (備用方式)

```http
Accept: application/vnd.genaidemo.v1+json
API-Version: v1
```

## API 分組和版本

### 消費者 API (Consumer API)

- **基礎路徑**: `/api/consumer/v1/`
- **目標用戶**: 終端消費者
- **版本策略**: 嚴格向後相容，長期支援

```
/api/consumer/v1/products          # 商品瀏覽
/api/consumer/v1/shopping-cart     # 購物車
/api/consumer/v1/promotions        # 促銷活動
/api/consumer/v1/member            # 會員功能
/api/consumer/v1/reviews           # 商品評價
/api/consumer/v1/recommendations   # 推薦系統
/api/consumer/v1/notifications     # 通知系統
/api/consumer/v1/delivery-tracking # 配送追蹤
```

### 商務 API (Business API)

- **基礎路徑**: `/api/business/v1/`
- **目標用戶**: 商務管理人員
- **版本策略**: 快速迭代，定期升級

```
/api/business/v1/orders      # 訂單管理
/api/business/v1/products    # 商品管理
/api/business/v1/customers   # 客戶管理
/api/business/v1/inventory   # 庫存管理
/api/business/v1/pricing     # 定價管理
/api/business/v1/stats       # 統計分析
```

### 內部 API (Internal API)

- **基礎路徑**: `/api/internal/v1/`
- **目標用戶**: 內部系統整合
- **版本策略**: 靈活變更，內部協調

## 版本生命週期

### 1. 版本支援期

| 版本類型 | 支援期 | 棄用通知期 |
|---------|--------|-----------|
| 消費者 API | 24 個月 | 6 個月 |
| 商務 API | 12 個月 | 3 個月 |
| 內部 API | 6 個月 | 1 個月 |

### 2. 版本狀態

- **CURRENT**: 目前版本，積極開發和維護
- **SUPPORTED**: 支援版本，僅修復重大問題
- **DEPRECATED**: 棄用版本，計劃移除
- **RETIRED**: 已退役版本，不再支援

## 變更管理

### 1. 向後相容的變更 (MINOR/PATCH)

✅ **允許的變更**：

- 新增 API 端點
- 新增可選參數
- 新增回應欄位
- 修復錯誤
- 效能改善

### 2. 不相容的變更 (MAJOR)

❌ **需要新版本的變更**：

- 移除 API 端點
- 移除請求/回應欄位
- 變更欄位類型
- 變更錯誤碼
- 變更認證方式

### 3. 變更通知流程

1. **提案階段**: 在 GitHub Issues 中提出變更提案
2. **評估階段**: 技術團隊評估影響範圍
3. **通知階段**: 提前通知 API 使用者
4. **實施階段**: 發布新版本
5. **監控階段**: 監控使用情況和問題

## 實作細節

### 1. Spring Boot 配置

```java
@RestController
@RequestMapping("/api/consumer/v1")
@Tag(name = "Consumer API v1", description = "消費者 API 第一版")
public class ConsumerProductController {
    // 實作內容
}
```

### 2. OpenAPI 文檔配置

```yaml
openapi: 3.0.3
info:
  title: GenAI Demo API
  version: 2.0.0
  description: |
    GenAI Demo 電商平台 API
    
    ## 版本資訊
    - Consumer API: v1 (穩定版)
    - Business API: v1 (穩定版)
    - Internal API: v1 (開發版)
```

### 3. 版本檢測中介軟體

```java
@Component
public class ApiVersionInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, 
                           HttpServletResponse response, 
                           Object handler) {
        // 版本檢測和路由邏輯
        return true;
    }
}
```

## 監控和指標

### 1. 版本使用統計

- 各版本 API 調用次數
- 版本分布統計
- 棄用 API 使用情況

### 2. 效能監控

- 各版本回應時間
- 錯誤率統計
- 使用者滿意度

### 3. 告警設定

- 棄用 API 使用量異常
- 新版本錯誤率過高
- 版本遷移進度緩慢

## 最佳實踐

### 1. API 設計原則

- **一致性**: 保持 API 設計風格一致
- **可預測性**: API 行為應該可預測
- **文檔完整**: 提供完整的 API 文檔
- **測試覆蓋**: 確保充分的測試覆蓋

### 2. 版本遷移指南

- 提供詳細的遷移文檔
- 提供程式碼範例
- 提供遷移工具
- 提供技術支援

### 3. 溝通策略

- 定期發布版本更新通知
- 維護變更日誌
- 提供開發者論壇
- 舉辦技術分享會

## 相關文檔

- [API 文檔](./README.md)
- [變更日誌](../releases/)
- [SpringDoc 分組指南](./SPRINGDOC_GROUPING_GUIDE.md)
- [技術棧說明](../TECHNOLOGY_STACK_2025.md)
