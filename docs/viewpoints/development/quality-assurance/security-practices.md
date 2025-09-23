# 安全實作指南

## 認證與授權

### JWT 實作
- Token 有效期管理
- Refresh Token 機制
- 安全的 Token 儲存

### 角色權限控制
```java
@PreAuthorize("hasRole('ADMIN') or hasPermission(#customerId, 'Customer', 'READ')")
public Customer getCustomer(@PathVariable String customerId) {
    return customerService.findById(customerId);
}
```

## 資料保護

### 敏感資料加密
- 資料庫欄位加密
- 傳輸過程加密 (HTTPS)
- 密碼雜湊處理

### 輸入驗證
- SQL 注入防護
- XSS 攻擊防護
- CSRF 保護機制

## 安全標頭

### HTTP 安全標頭
```yaml
security:
  headers:
    frame-options: DENY
    content-type-options: nosniff
    xss-protection: "1; mode=block"
    strict-transport-security: "max-age=31536000; includeSubDomains"
```

## 監控與稽核

### 安全事件記錄
- 登入失敗記錄
- 權限違規記錄
- 異常存取記錄

### 威脅偵測
- 異常行為偵測
- 暴力攻擊防護
- 速率限制機制
