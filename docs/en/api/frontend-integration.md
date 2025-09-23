
# 前端 API 整合說明

This document說明了前端應用程式從使用寫死資料改為向後端發送 HTTP 請求的修改內容。

## Overview

### 1. API 服務層擴展 (`src/services/api.ts`)

新增了以下 API 服務：

- **統計服務** (`statsService`)
  - `getStats()` - 獲取系統總體統計
  - `getOrderStatusStats()` - 獲取訂單狀態分布
  - `getPaymentMethodStats()` - 獲取支付方式分布

- **活動記錄服務** (`activityService`)
  - `list(params)` - 獲取系統活動記錄

### 2. React Hooks 擴展 (`src/hooks/useApi.ts`)

新增了以下自定義 hooks：

- `useStats()` - 獲取系統統計數據
- `useOrderStatusStats()` - 獲取訂單狀態統計
- `usePaymentMethodStats()` - 獲取支付方式統計
- `useActivities(params)` - 獲取活動記錄

### 3. 頁面組件修改

#### 主頁面 (`src/app/page.tsx`)

- ✅ 移除寫死的統計數據
- ✅ 使用 `useStats()` 和 `useActivities()` hooks
- ✅ 根據 API 數據動態計算統計卡片內容
- ✅ 支持載入狀態顯示

#### 產品頁面 (`src/app/products/page.tsx`)

- ✅ 移除模擬產品數據
- ✅ 使用 `useProducts()` hook 獲取真實數據
- ✅ 支持分頁、搜尋和篩選
- ✅ 錯誤處理和載入狀態
- ✅ 動態統計計算

#### Customer頁面 (`src/app/customers/page.tsx`)

- ✅ 移除模擬Customer數據
- ✅ 使用 `useCustomers()` 和 `useStats()` hooks
- ✅ 支持分頁、搜尋和篩選
- ✅ 錯誤處理和載入狀態
- ✅ 動態統計計算

#### 活動時間軸組件 (`src/components/dashboard/ActivityTimeline.tsx`)

- ✅ 移除寫死的活動數據
- ✅ 支持從 props 接收活動數據
- ✅ 改善空狀態處理

### 4. 後端 API 控制器新增

#### 產品控制器 (`ProductController.java`)

- `GET /../api/products` - 獲取產品列表（支持分頁）
- `GET /api/products/{id}` - 獲取單個產品

#### Customer控制器 (`CustomerController.java`)

- `GET /../api/customers` - 獲取Customer列表（支持分頁）
- `GET /api/customers/{id}` - 獲取單個Customer

#### 活動記錄控制器 (`ActivityController.java`)

- `GET /../api/activities` - 獲取系統活動記錄

#### 統計控制器擴展 (`StatsController.java`)

- 已存在的統計 API 端點保持不變

## API 端點總覽

### 統計相關

```
GET /../api/stats                    # 系統總體統計
GET /api/stats/order-status       # 訂單狀態分布
GET /api/stats/payment-methods    # 支付方式分布
```

### 產品相關

```
GET /api/products                 # 產品列表（支持分頁）
GET /api/products/{id}            # 單個產品詳情
```

### Customer相關

```
GET /api/customers                # Customer列表（支持分頁）
GET /api/customers/{id}           # 單個Customer詳情
```

### 活動記錄

```
GET /api/activities               # 系統活動記錄
```

### 訂單相關（已存在）

```
GET /api/orders                   # 訂單列表
GET /api/orders/{id}              # 單個訂單詳情
POST /api/orders                  # 創建訂單
POST /api/orders/{id}/cancel      # 取消訂單
```

## 數據流程

1. **前端組件** 使用自定義 hooks
2. **自定義 hooks** 使用 React Query 管理狀態和快取
3. **API 服務層** 發送 HTTP 請求到後端
4. **後端控制器** 處理請求並返回數據
5. **數據庫** 提供真實的業務數據

## 特性改進

### 1. 錯誤處理

- 網路錯誤自動重試
- 用戶友好的錯誤訊息
- 載入狀態指示器

### 2. 效能優化

- React Query 自動快取
- 分頁載入減少數據量
- 防抖搜尋避免頻繁請求

### 3. 用戶體驗

- 載入骨架屏
- 實時數據更新
- 響應式設計

## Testing

使用提供的測試腳本驗證 API 整合：

```bash
./test-api.sh
```

## 啟動說明

1. 啟動後端服務：

```bash
./gradlew bootRun
```

2. 啟動前端服務：

```bash
cd cmc-frontend && npm run dev
```

3. 或使用全棧啟動腳本：

```bash
./start-fullstack.sh
```

## notes

1. **CORS 設定**：所有新的控制器都已配置 `@CrossOrigin(origins = "*")`
2. **數據生成**：後端控制器使用演算法生成模擬數據，確保一致性
3. **分頁支持**：所有列表 API 都支持分頁參數
4. **錯誤處理**：統一的錯誤響應格式
5. **快取Policy**：React Query 配置了適當的快取時間

## 未來改進

1. 添加更多篩選選項
2. 實現即時通知功能
3. 添加數據匯出功能
4. 改善搜尋功能（全文搜尋）
5. 添加批量操作功能
