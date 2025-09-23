
# Guidelines

## 概述

Excalidraw MCP Server 是一個功能完整的 Model Context Protocol 服務器，讓 AI 助手能夠創建和管理 Excalidraw 圖表。本指南詳細說明如何安裝、配置和使用這個強大的可視化工具。

## 🎯 功能特色

### 基本繪圖功能
- ✅ **多種元素類型**：矩形、橢圓、菱形、箭頭、文字、線條、自由繪圖
- ✅ **豐富的樣式選項**：背景色、邊框色、線條寬度、透明度、字體大小
- ✅ **中文支援**：完美支援中文文字顯示

### 進階管理功能
- ✅ **元素管理**：創建、更新、刪除、查詢
- ✅ **批量操作**：一次創建多個元素，適合複雜圖表
- ✅ **元素分組**：群組管理和取消群組
- ✅ **對齊分佈**：左中右、上中下對齊，水平垂直分佈
- ✅ **鎖定功能**：鎖定/解鎖元素防止意外修改

### 技術特色
- ✅ **即時同步**：支援與畫布的即時同步（可選）
- ✅ **版本控制**：每個元素都有版本Tracing
- ✅ **錯誤處理**：完善的錯誤處理和Logging記錄
- ✅ **Resource管理**：場景、庫、主題、元素Resource存取

## Guidelines

### 方法 1：本地安裝（推薦）

```bash
# 1. 導航到專案根目錄
cd /path/to/genai-demo

# 2. 安裝 Excalidraw MCP Server
npm install mcp-excalidraw-server

# 3. 驗證安裝
ls node_modules/mcp-excalidraw-server/src/index.js

# Testing
echo '{"jsonrpc": "2.0", "id": 1, "method": "tools-and-environment/list", "params": {}}' | \
  node node_modules/mcp-excalidraw-server/src/index.js | head -3
```

### 方法 2：全域安裝

```bash
# 全域安裝
npm install -g mcp-excalidraw-server

# Testing
mcp-excalidraw-server --help
```

### 方法 3：使用 NPX（不推薦用於生產Environment）

```bash
# 使用 NPX 運行（較慢）
npx mcp-excalidraw-server --help
```

## ⚙️ 配置設定

### MCP 配置文件

確保 `.kiro/settings/mcp.json` 包含正確的 Excalidraw 配置：

```json
{
  "mcpServers": {
    "excalidraw": {
      "command": "node",
      "args": [
        "node_modules/mcp-excalidraw-server/src/index.js"
      ],
      "env": {
        "ENABLE_CANVAS_SYNC": "false"
      },
      "disabled": false,
      "autoApprove": [
        "create_element",
        "update_element",
        "delete_element",
        "query_elements",
        "get_resource",
        "group_elements",
        "ungroup_elements",
        "align_elements",
        "distribute_elements",
        "lock_elements",
        "unlock_elements",
        "batch_create_elements"
      ]
    }
  }
}
```

### Environment變數說明

| 變數 | 預設值 | 說明 |
|------|--------|------|
| `ENABLE_CANVAS_SYNC` | `true` | 是否啟用畫布同步功能 |
| `EXPRESS_SERVER_URL` | `http://localhost:3000` | 畫布服務器 URL |
| `DEBUG` | `false` | 是否啟用除錯Logging |

### 配置選項

#### 本地安裝配置（推薦）

```json
{
  "excalidraw": {
    "command": "node",
    "args": ["node_modules/mcp-excalidraw-server/src/index.js"],
    "env": {
      "ENABLE_CANVAS_SYNC": "false"
    }
  }
}
```

#### 全域安裝配置

```json
{
  "excalidraw": {
    "command": "mcp-excalidraw-server",
    "args": [],
    "env": {
      "ENABLE_CANVAS_SYNC": "false"
    }
  }
}
```

#### NPX 配置（不推薦）

```json
{
  "excalidraw": {
    "command": "npx",
    "args": ["mcp-excalidraw-server"],
    "env": {
      "ENABLE_CANVAS_SYNC": "false"
    }
  }
}
```

## 🎨 使用方法

### 在 Kiro IDE 中使用

#### 基本元素創建

```
Ask Kiro: "創建一個藍色的矩形，包含文字 '開始'"
Ask Kiro: "畫一個綠色的橢圓形"
Ask Kiro: "創建一個從 (100,100) 到 (200,100) 的箭頭"
```

#### 複雜圖表創建

```
Ask Kiro: "創建一個簡單的流程圖：
- 開始（綠色矩形）
- 處理（藍色矩形）
- 決策（黃色菱形）
- 結束（紅色矩形）
- 用箭頭連接它們"
```

#### 系統架構圖

```
Ask Kiro: "創建一個Microservices Architecture圖：
- API Gateway（藍色矩形）
- 用戶服務（綠色矩形）
- 訂單服務（綠色矩形）
- Repository（紫色橢圓）
- 用箭頭連接它們"
```

#### 批量創建元素

```
Ask Kiro: "批量創建以下元素：
1. 矩形 (50,50) 120x60 背景色 #c8e6c9 文字 '開始'
2. 箭頭 (170,80) 80x0
3. 矩形 (250,50) 120x60 背景色 #e3f2fd 文字 '處理'
4. 箭頭 (370,80) 80x0
5. 矩形 (450,50) 120x60 背景色 #ffcdd2 文字 '結束'"
```

### Tools

#### Tools

| 工具名稱 | 功能 | 參數 |
|----------|------|------|
| `create_element` | 創建單個元素 | type, x, y, width, height, text, colors |
| `update_element` | 更新現有元素 | id, 要更新的屬性 |
| `delete_element` | 刪除元素 | id |
| `query_elements` | 查詢元素 | type (可選), filter (可選) |

#### Tools

| 工具名稱 | 功能 | 參數 |
|----------|------|------|
| `batch_create_elements` | 批量創建元素 | elements 陣列 |

#### Tools

| 工具名稱 | 功能 | 參數 |
|----------|------|------|
| `group_elements` | 群組元素 | elementIds 陣列 |
| `ungroup_elements` | 取消群組 | groupId |
| `align_elements` | 對齊元素 | elementIds, alignment |
| `distribute_elements` | 分佈元素 | elementIds, direction |
| `lock_elements` | 鎖定元素 | elementIds 陣列 |
| `unlock_elements` | 解鎖元素 | elementIds 陣列 |

#### Tools

| 工具名稱 | 功能 | 參數 |
|----------|------|------|
| `get_resource` | 獲取Resource | resource (scene/library/theme/elements) |

### 元素類型和屬性

#### 支援的元素類型

| 類型 | 說明 | 特殊屬性 |
|------|------|----------|
| `rectangle` | 矩形 | width, height |
| `ellipse` | 橢圓 | width, height |
| `diamond` | 菱形 | width, height |
| `arrow` | 箭頭 | width, height (終點相對位置) |
| `text` | 文字 | text, fontSize, fontFamily |
| `line` | 線條 | width, height (終點相對位置) |
| `freedraw` | 自由繪圖 | points 陣列 |

#### 通用屬性

| 屬性 | 類型 | 預設值 | 說明 |
|------|------|--------|------|
| `x` | number | 必填 | X 座標 |
| `y` | number | 必填 | Y 座標 |
| `width` | number | 100 | 寬度 |
| `height` | number | 50 | 高度 |
| `backgroundColor` | string | transparent | 背景色 (hex) |
| `strokeColor` | string | #1e1e1e | 邊框色 (hex) |
| `strokeWidth` | number | 2 | 邊框寬度 |
| `opacity` | number | 100 | 透明度 (0-100) |
| `roughness` | number | 1 | 粗糙度 (0-2) |

#### 文字屬性

| 屬性 | 類型 | 預設值 | 說明 |
|------|------|--------|------|
| `text` | string | "" | 文字內容 |
| `fontSize` | number | 16 | 字體大小 |
| `fontFamily` | number | 1 | 字體家族 (1-4) |
| `textAlign` | string | center | 文字對齊 (left/center/right) |

## Examples

### Examples

```javascript
// 使用 batch_create_elements 創建完整流程圖
{
  "elements": [
    {
      "type": "rectangle",
      "x": 50,
      "y": 50,
      "width": 120,
      "height": 60,
      "backgroundColor": "#c8e6c9",
      "strokeColor": "#4caf50",
      "text": "開始"
    },
    {
      "type": "arrow",
      "x": 170,
      "y": 80,
      "width": 80,
      "height": 0,
      "strokeColor": "#666666"
    },
    {
      "type": "rectangle",
      "x": 250,
      "y": 50,
      "width": 120,
      "height": 60,
      "backgroundColor": "#e3f2fd",
      "strokeColor": "#2196f3",
      "text": "處理"
    },
    {
      "type": "arrow",
      "x": 370,
      "y": 80,
      "width": 80,
      "height": 0,
      "strokeColor": "#666666"
    },
    {
      "type": "rectangle",
      "x": 450,
      "y": 50,
      "width": 120,
      "height": 60,
      "backgroundColor": "#ffcdd2",
      "strokeColor": "#f44336",
      "text": "結束"
    }
  ]
}
```

### Examples

```javascript
{
  "elements": [
    {
      "type": "rectangle",
      "x": 100,
      "y": 50,
      "width": 150,
      "height": 60,
      "backgroundColor": "#e3f2fd",
      "text": "API Gateway"
    },
    {
      "type": "rectangle",
      "x": 50,
      "y": 150,
      "width": 120,
      "height": 60,
      "backgroundColor": "#e8f5e8",
      "text": "用戶服務"
    },
    {
      "type": "rectangle",
      "x": 200,
      "y": 150,
      "width": 120,
      "height": 60,
      "backgroundColor": "#e8f5e8",
      "text": "訂單服務"
    },
    {
      "type": "ellipse",
      "x": 125,
      "y": 250,
      "width": 120,
      "height": 60,
      "backgroundColor": "#f3e5f5",
      "text": "Repository"
    }
  ]
}
```

### Examples

```javascript
{
  "elements": [
    {
      "type": "rectangle",
      "x": 150,
      "y": 50,
      "width": 100,
      "height": 50,
      "backgroundColor": "#c8e6c9",
      "text": "開始"
    },
    {
      "type": "diamond",
      "x": 125,
      "y": 150,
      "width": 150,
      "height": 80,
      "backgroundColor": "#fff3e0",
      "text": "條件判斷"
    },
    {
      "type": "rectangle",
      "x": 50,
      "y": 280,
      "width": 100,
      "height": 50,
      "backgroundColor": "#e3f2fd",
      "text": "是"
    },
    {
      "type": "rectangle",
      "x": 250,
      "y": 280,
      "width": 100,
      "height": 50,
      "backgroundColor": "#ffebee",
      "text": "否"
    }
  ]
}
```

## Troubleshooting

### 常見問題和解決方案

#### 1. 模組找不到錯誤

```bash
Error: Cannot find module 'mcp-excalidraw-server'
```

**解決方案**：
```bash
# 確保套件已安裝
npm install mcp-excalidraw-server

# 檢查安裝位置
ls node_modules/mcp-excalidraw-server/
```

#### 2. 權限被拒絕

```bash
Error: EACCES: permission denied
```

**解決方案**：
```bash
# 檢查檔案權限
chmod +x node_modules/mcp-excalidraw-server/src/index.js

# 或重新安裝
rm -rf node_modules/mcp-excalidraw-server
npm install mcp-excalidraw-server
```

#### 3. Node.js 版本不相容

```bash
Error: Unsupported Node.js version
```

**解決方案**：
```bash
# 檢查 Node.js 版本（需要 v16+）
node --version

# 如果版本過舊，請升級 Node.js
# 從 https://nodejs.org/ 下載最新版本
```

#### 4. MCP 服務器無回應

**解決方案**：
```bash
# 檢查配置
cat .kiro/settings/mcp.json | jq '.mcpServers.excalidraw'

# Testing
node node_modules/mcp-excalidraw-server/src/index.js --help

# 重啟 Kiro IDE
```

#### 5. JSON 解析錯誤

```bash
Error: Unexpected token in JSON
```

**解決方案**：
```bash
# 檢查 MCP 配置文件語法
cat .kiro/settings/mcp.json | jq '.'

# 如果有語法錯誤，修正 JSON 格式
```

### 除錯模式

啟用除錯Logging：

```json
{
  "excalidraw": {
    "env": {
      "DEBUG": "true",
      "ENABLE_CANVAS_SYNC": "false"
    }
  }
}
```

### Testing

```bash
# Testing
echo '{"jsonrpc": "2.0", "id": 1, "method": "tools-and-environment/list", "params": {}}' | \
  node node_modules/mcp-excalidraw-server/src/index.js

# Testing
echo '{"jsonrpc": "2.0", "id": 1, "method": "tools/call", "params": {"name": "create_element", "arguments": {"type": "rectangle", "x": 100, "y": 100, "width": 200, "height": 100, "text": "測試"}}}' | \
  node node_modules/mcp-excalidraw-server/src/index.js
```

## 🚀 進階使用

### 自定義樣式

```javascript
// 創建具有自定義樣式的元素
{
  "type": "rectangle",
  "x": 100,
  "y": 100,
  "width": 200,
  "height": 100,
  "backgroundColor": "#e3f2fd",
  "strokeColor": "#1976d2",
  "strokeWidth": 3,
  "opacity": 80,
  "roughness": 0.5,
  "text": "自定義樣式",
  "fontSize": 18,
  "fontFamily": 2
}
```

### 元素分組和對齊

```javascript
// 1. 創建多個元素
// 2. 使用 group_elements 將它們分組
{
  "elementIds": ["element-1", "element-2", "element-3"]
}

// 3. 使用 align_elements 對齊元素
{
  "elementIds": ["element-1", "element-2", "element-3"],
  "alignment": "center"
}
```

### Templates

#### Templates

```javascript
{
  "elements": [
    {
      "type": "rectangle",
      "x": 100,
      "y": 100,
      "width": 200,
      "height": 120,
      "backgroundColor": "#f5f5f5",
      "strokeColor": "#333333",
      "text": "User\n---\n+id: String\n+name: String\n+email: String\n---\n+login()\n+logout()"
    }
  ]
}
```

#### Templates

```javascript
{
  "elements": [
    {
      "type": "ellipse",
      "x": 200,
      "y": 50,
      "width": 100,
      "height": 60,
      "backgroundColor": "#e3f2fd",
      "text": "路由器"
    },
    {
      "type": "rectangle",
      "x": 50,
      "y": 150,
      "width": 80,
      "height": 50,
      "backgroundColor": "#e8f5e8",
      "text": "PC1"
    },
    {
      "type": "rectangle",
      "x": 270,
      "y": 150,
      "width": 80,
      "height": 50,
      "backgroundColor": "#e8f5e8",
      "text": "PC2"
    }
  ]
}
```

## Resources

### 官方文檔

- [Excalidraw 官方網站](https://excalidraw.com/)
- [MCP 協議規範](https://modelcontextprotocol.io/)
- [Node.js 官方文檔](https://nodejs.org/docs/)

### 相關專案

- [mcp-excalidraw-server GitHub](https://github.com/yctimlin/mcp_excalidraw)
- [Excalidraw React 組件](https://github.com/excalidraw/excalidraw)
- [Model Context Protocol SDK](https://github.com/modelcontextprotocol/sdk)

### 內部文檔

- [MCP 整合指南](../../../infrastructure/docs/MCP_INTEGRATION_GUIDE.md)
- [架構文檔](../architecture/)
- [Deployment指南](../deployment/)

---

## 📞 支援和協助

### 獲得幫助

1. 檢查上述故障排除部分
2. 查看 MCP Integration Test報告
3. 諮詢 DevOps 團隊
4. 查閱 AWS 文檔（使用 AWS Docs MCP）

### 回報問題

如果遇到問題，請提供以下資訊：

- Node.js 版本 (`node --version`)
- NPM 版本 (`npm --version`)
- 錯誤訊息的完整輸出
- MCP 配置文件內容
- 重現問題的步驟

---

*文檔版本: 1.0*  
*最後更新: 2025-09-21*  
*維護者: DevOps 團隊*