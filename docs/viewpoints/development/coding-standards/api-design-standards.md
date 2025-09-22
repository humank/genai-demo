# REST API 設計指南

## API 設計原則

### RESTful 設計
- 使用 HTTP 動詞 (GET, POST, PUT, DELETE)
- 資源導向的 URL 設計
- 統一的回應格式
- 適當的 HTTP 狀態碼

### URL 設計規範

#### 基本 CRUD 操作
```
GET    /api/v1/customers                    # 取得客戶列表
GET    /api/v1/customers/{id}               # 取得特定客戶
POST   /api/v1/customers                    # 創建新客戶
PUT    /api/v1/customers/{id}               # 更新客戶（完整）
PATCH  /api/v1/customers/{id}               # 更新客戶（部分）
DELETE /api/v1/customers/{id}               # 刪除客戶
```

#### 巢狀資源
```
GET    /api/v1/customers/{id}/orders        # 取得客戶的訂單
POST   /api/v1/customers/{id}/orders        # 為客戶創建訂單
GET    /api/v1/customers/{id}/addresses     # 取得客戶的地址
POST   /api/v1/customers/{id}/addresses     # 為客戶添加地址
```

#### 動作端點（非 CRUD 操作）
```
POST   /api/v1/orders/{id}/cancel           # 取消訂單
POST   /api/v1/orders/{id}/ship             # 出貨訂單
POST   /api/v1/customers/{id}/activate      # 啟用客戶
POST   /api/v1/customers/{id}/suspend       # 暫停客戶
```

## 回應格式

### 成功回應
```json
{
  "status": "success",
  "data": {
    "id": "123",
    "name": "客戶名稱"
  },
  "meta": {
    "timestamp": "2025-01-21T10:00:00Z"
  }
}
```

### 錯誤回應
```json
{
  "status": "error",
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "輸入資料驗證失敗",
    "details": [
      {
        "field": "email",
        "message": "電子郵件格式不正確"
      }
    ]
  }
}
```

## 版本管理

### URL 版本控制
- 使用 `/api/v1/` 格式
- 向後相容性保證
- 版本廢棄通知機制

### 標頭版本控制
```http
Accept: application/vnd.api+json;version=1
API-Version: 1.0
```
