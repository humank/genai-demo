# GenAI Demo 腳本目錄

這個目錄包含了用於管理 GenAI Demo 全棧應用的各種腳本。

## 🚀 啟動腳本

### 全棧應用啟動

```bash
# 啟動所有服務（後端 + 前端）
./scripts/start-fullstack.sh
```

### 單獨服務啟動

```bash
# 只啟動後端 Spring Boot 應用
./scripts/start-backend.sh

# 只啟動 Consumer 前端 Angular 應用
./scripts/start-consumer-frontend.sh

# 只啟動 CMC 前端 Next.js 應用
./scripts/start-cmc-frontend.sh
```

## 🛑 停止腳本

### 全棧應用停止

```bash
# 停止所有服務
./scripts/stop-fullstack.sh

# 停止所有服務並清理日誌
./scripts/stop-fullstack.sh --clean-logs
```

### 單獨服務停止

```bash
# 只停止後端應用
./scripts/stop-backend.sh

# 只停止 Consumer 前端應用
./scripts/stop-consumer-frontend.sh

# 只停止 CMC 前端應用
./scripts/stop-cmc-frontend.sh
```

## 🔧 其他工具腳本

```bash
# API 測試腳本
./scripts/test-api.sh

# 驗證 Swagger UI 可用性
./scripts/verify-swagger-ui.sh

# 生成測試數據
python3 scripts/generate_data.py
```

## 📋 腳本功能說明

### start-fullstack.sh

- 檢查系統需求（Java, Node.js, npm, Angular CLI）
- 構建後端 Spring Boot 應用
- 安裝前端依賴
- 創建環境變數文件
- 啟動所有服務並監控狀態
- 提供完整的服務訪問地址

### stop-fullstack.sh

- 優雅停止所有服務
- 清理殘留進程
- 清理端口佔用
- 可選清理日誌文件

### 單獨服務腳本

- 提供更精細的服務控制
- 適合開發時只需要特定服務的場景
- 包含完整的錯誤處理和狀態檢查

## 🌐 服務端口配置

| 服務 | 端口 | 描述 | 啟動腳本 |
|------|------|------|----------|
| 後端 API | 8080 | Spring Boot 應用 | `./scripts/start-backend.sh` |
| Consumer 前端 | 3001 | Angular 應用 | `./scripts/start-consumer-frontend.sh` |
| CMC 前端 | 3002 | Next.js 應用 | `./scripts/start-cmc-frontend.sh` |

## 📝 日誌文件

所有服務的日誌都保存在 `logs/` 目錄下：

- `logs/backend.log` - 後端應用日誌
- `logs/consumer-frontend.log` - Consumer 前端日誌
- `logs/cmc-frontend.log` - CMC 前端日誌

## 🎯 服務說明

### 後端服務 (Spring Boot)

- **端口**: 8080
- **功能**: 提供 RESTful API，處理業務邏輯
- **訪問**: <http://localhost:8080>
- **API 文檔**: <http://localhost:8080/swagger-ui/index.html>

### Consumer 前端 (Angular)

- **端口**: 3001
- **功能**: 消費者端電商購物平台
- **技術棧**: Angular 18 + PrimeNG + Tailwind CSS
- **訪問**: <http://localhost:3001>

### CMC 前端 (Next.js)

- **端口**: 3002
- **功能**: 商務管理中心，後台管理系統
- **技術棧**: Next.js 14 + shadcn/ui + Tailwind CSS
- **訪問**: <http://localhost:3002>

## 🔍 故障排除

### 常見問題

1. **端口被佔用**

   ```bash
   # 檢查端口使用情況
   lsof -i:8080  # 後端
   lsof -i:3001  # Consumer 前端
   lsof -i:3002  # CMC 前端
   ```

2. **服務啟動失敗**

   ```bash
   # 查看詳細日誌
   tail -f logs/backend.log
   tail -f logs/consumer-frontend.log
   tail -f logs/cmc-frontend.log
   ```

3. **依賴安裝問題**

   ```bash
   # Consumer 前端 (Angular)
   cd consumer-frontend
   rm -rf node_modules package-lock.json
   npm install --legacy-peer-deps
   
   # CMC 前端 (Next.js)
   cd cmc-frontend
   rm -rf node_modules package-lock.json
   npm install
   ```

### 手動清理

如果腳本無法正常停止服務，可以手動清理：

```bash
# 殺死所有相關進程
pkill -f "spring-boot"
pkill -f "ng serve"
pkill -f "next-server"
pkill -f "npm run dev"

# 清理端口
lsof -ti:8080 | xargs kill -9
lsof -ti:3001 | xargs kill -9
lsof -ti:3002 | xargs kill -9
```

## 🛠️ 開發建議

### 開發場景建議

1. **全棧開發**

   ```bash
   ./scripts/start-fullstack.sh
   ```

2. **只開發後端 API**

   ```bash
   ./scripts/start-backend.sh
   ```

3. **只開發消費者前端**

   ```bash
   ./scripts/start-backend.sh      # 先啟動後端
   ./scripts/start-consumer-frontend.sh
   ```

4. **只開發管理後台**

   ```bash
   ./scripts/start-backend.sh      # 先啟動後端
   ./scripts/start-cmc-frontend.sh
   ```

5. **前端開發（不需要後端）**

   ```bash
   # 只啟動前端，使用 mock 數據
   ./scripts/start-consumer-frontend.sh
   # 或
   ./scripts/start-cmc-frontend.sh
   ```

### 最佳實踐

1. **開發時建議使用單獨啟動腳本**，這樣可以更快地重啟特定服務
2. **生產環境建議使用全棧啟動腳本**，確保所有服務協調啟動
3. **定期清理日誌文件**，避免佔用過多磁盤空間
4. **使用 `--clean-logs` 選項**在重新啟動前清理舊日誌

## 📚 相關文檔

- [專案 README](../README.md)
- [Docker 指南](../docs/deployment/docker-guide.md)
- [API 文檔](http://localhost:8080/swagger-ui/index.html)（服務啟動後可訪問）

## 🚀 快速開始

```bash
# 1. 啟動後端
./scripts/start-backend.sh

# 2. 啟動消費者前端
./scripts/start-consumer-frontend.sh

# 3. 啟動管理後台
./scripts/start-cmc-frontend.sh

# 4. 訪問應用
# Consumer: http://localhost:3001
# CMC: http://localhost:3002
# API: http://localhost:8080
```
