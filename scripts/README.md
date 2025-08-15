# Scripts 目錄

此目錄包含專案的各種腳本檔案。

## 檔案說明

### 全棧應用腳本

- `start-fullstack.sh` - 啟動完整的前後端應用
- `stop-fullstack.sh` - 停止所有服務

### 測試和驗證腳本

- `test-api.sh` - API 測試腳本
- `verify-swagger-ui.sh` - Swagger UI 驗證腳本

### 資料生成腳本

- `generate_data.py` - 測試資料生成腳本

## 使用方式

```bash
# 啟動全棧應用
./scripts/start-fullstack.sh

# 停止所有服務
./scripts/stop-fullstack.sh

# 生成測試資料
python3 scripts/generate_data.py

# 測試 API
./scripts/test-api.sh

# 驗證 Swagger UI
./scripts/verify-swagger-ui.sh
```
