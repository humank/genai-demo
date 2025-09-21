
# MCP 設置檢查清單

## 📋 新團隊成員 MCP 設置檢查清單

使用此檢查清單確保 MCP (Model Context Protocol) 服務器正確安裝和配置。

### Requirements

- [ ] **Node.js v16+** 已安裝
  ```bash
  node --version  # 應該顯示 v16.0.0 或更高版本
  ```

- [ ] **NPM** 已安裝並可用
  ```bash
  npm --version
  ```

- [ ] **UV 套件管理器** 已安裝
  ```bash
  uv --version
  # 如果未安裝: brew install uv (macOS) 或 pip install uv
  ```

- [ ] **AWS CLI** 已安裝（可選，用於 AWS MCP 工具）
  ```bash
  aws --version
  ```

### ✅ 專案設置檢查

- [ ] **專案根目錄** 確認
  ```bash
  pwd  # 應該在 genai-demo 專案根目錄
  ls   # 應該看到 package.json, .kiro/ 等文件
  ```

- [ ] **package.json** 存在
  ```bash
  ls package.json
  ```

### ✅ Excalidraw MCP 服務器安裝

- [ ] **安裝 mcp-excalidraw-server**
  ```bash
  npm install mcp-excalidraw-server
  ```

- [ ] **驗證安裝**
  ```bash
  ls node_modules/mcp-excalidraw-server/src/index.js
  # 應該顯示文件存在
  ```

- [ ] **測試服務器**
  ```bash
  # Testing
  echo '{"jsonrpc": "2.0", "id": 1, "method": "tools/list", "params": {}}' | \
    node /ABSOLUTE/PATH/TO/PROJECT/node_modules/mcp-excalidraw-server/src/index.js | head -3
  # 應該返回 JSON 響應
  
  # 或使用修正腳本
  ./scripts/fix-excalidraw-path.sh
  ```

### ✅ MCP 配置檢查

- [ ] **MCP 配置文件存在**
  ```bash
  ls .kiro/settings/mcp.json
  ```

- [ ] **配置文件格式正確**
  ```bash
  cat .kiro/settings/mcp.json | jq '.'
  # 應該顯示格式化的 JSON（需要安裝 jq）
  ```

- [ ] **Excalidraw 服務器已配置**
  ```bash
  cat .kiro/settings/mcp.json | jq '.mcpServers.excalidraw'
  # 應該顯示 excalidraw 配置，並使用絕對路徑
  ```

- [ ] **修正路徑問題（如果需要）**
  ```bash
  # 如果遇到路徑問題，使用修正腳本
  ./scripts/fix-excalidraw-path.sh
  ```

- [ ] **所有必需的 MCP 服務器已配置**
  ```bash
  cat .kiro/settings/mcp.json | jq '.mcpServers | keys'
  # 應該包含: ["aws-cdk", "aws-docs", "aws-pricing", "excalidraw", "time"]
  ```

### ✅ AWS 配置檢查（可選）

- [ ] **AWS 憑證已配置**
  ```bash
  aws configure list
  ```

- [ ] **AWS 配置文件設置**
  ```bash
  export AWS_PROFILE=kim-sso
  export AWS_REGION=ap-northeast-1
  ```

- [ ] **AWS 憑證測試**
  ```bash
  aws sts get-caller-identity
  # 應該返回用戶身份信息
  ```

### Testing

- [ ] **Time MCP 服務器**
  ```bash
  uvx mcp-server-time --help
  # 應該顯示幫助信息
  ```

- [ ] **AWS Docs MCP 服務器**
  ```bash
  uvx awslabs.aws-documentation-mcp-server@latest --help
  # 應該顯示幫助信息
  ```

- [ ] **AWS CDK MCP 服務器**
  ```bash
  uvx awslabs.cdk-mcp-server@latest --help
  # 應該顯示幫助信息
  ```

- [ ] **AWS Pricing MCP 服務器**
  ```bash
  uvx awslabs.aws-pricing-mcp-server@latest --help
  # 應該顯示幫助信息
  ```

### ✅ Kiro IDE 整合

- [ ] **重啟 Kiro IDE**
  - 關閉 Kiro IDE
  - 重新啟動 Kiro IDE
  - 等待 MCP 服務器載入

- [ ] **測試 Excalidraw MCP**
  ```
  在 Kiro 中詢問: "創建一個簡單的矩形，包含文字 'Hello MCP'"
  ```

- [ ] **測試 Time MCP**
  ```
  在 Kiro 中詢問: "現在東京是幾點？"
  ```

- [ ] **測試 AWS Docs MCP**（如果已配置 AWS）
  ```
  在 Kiro 中詢問: "搜尋 AWS Lambda Best Practice文檔"
  ```

### ✅ 功能驗證

- [ ] **Excalidraw 圖表創建**
  - 能夠創建基本形狀（矩形、圓形、箭頭）
  - 能夠添加文字
  - 能夠設置顏色和樣式

- [ ] **批量元素創建**
  ```
  在 Kiro 中詢問: "創建一個簡單的流程圖，包含開始、處理、結束三個步驟"
  ```

- [ ] **時間轉換功能**
  ```
  在 Kiro 中詢問: "將下午 2 點 EST 轉換為台灣時間"
  ```

### Troubleshooting

如果遇到問題，檢查以下項目：

- [ ] **檔案權限**
  ```bash
  chmod +x node_modules/mcp-excalidraw-server/src/index.js
  ```

- [ ] **Node.js 版本相容性**
  ```bash
  node --version  # 必須是 v16.0.0 或更高
  ```

- [ ] **NPM 快取清理**
  ```bash
  npm cache clean --force
  ```

- [ ] **UV 快取清理**
  ```bash
  uv cache clean
  ```

- [ ] **修正路徑問題**
  ```bash
  # 如果看到 "Cannot find module '/node_modules/...'" 錯誤
  ./scripts/fix-excalidraw-path.sh
  ```

- [ ] **重新安裝 Excalidraw MCP**
  ```bash
  rm -rf node_modules/mcp-excalidraw-server
  npm install mcp-excalidraw-server
  ```

### ✅ 自動化設置（推薦）

- [ ] **使用自動化腳本**
  ```bash
  ./scripts/setup-mcp-servers.sh
  ```

- [ ] **檢查設置報告**
  ```bash
  cat mcp-setup-report.txt
  ```

## Standards

當所有檢查項目完成後，您應該能夠：

1. ✅ 在 Kiro IDE 中創建 Excalidraw 圖表
2. ✅ 使用時間轉換功能
3. ✅ 查詢 AWS 文檔（如果已配置）
4. ✅ 獲得 CDK Best Practicerecommendations（如果已配置）
5. ✅ 進行成本分析（如果已配置）

## 📚 相關文檔

- [MCP 整合指南](docs/en/infrastructure/docs/MCP_INTEGRATION_GUIDE.md)
- [Excalidraw MCP 使用指南](excalidraw-mcp-usage-guide.md)
- \1

## 🆘 獲得幫助

如果遇到問題：

1. 📖 查看故障排除部分
2. 🔍 檢查 MCP Integration Test報告
3. 👥 諮詢 DevOps 團隊
4. 📝 提供詳細的錯誤信息和Environment詳情

---

**檢查清單完成日期**: ___________  
**檢查者**: ___________  
**版本**: 1.0  
**最後更新**: 2025-09-21