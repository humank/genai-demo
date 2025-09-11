# MCP (Model Context Protocol) 整合指南

## 概覽

本專案整合了多個 MCP 伺服器，提供 AI 輔助開發功能，包含 AWS 生態系統支援、GitHub 整合和智能開發指導。

## 已整合的 MCP 伺服器

### 🏗️ AWS 開發支援

- **aws-docs**: AWS 官方文檔查詢和最佳實踐
- **aws-cdk**: CDK 構建指導和 Nag 規則檢查
- **aws-pricing**: AWS 服務定價查詢和成本估算
- **aws-iam**: IAM 用戶、角色和政策管理

### 🐙 GitHub 整合

- **github**: 程式碼審查、問題追蹤和 PR 管理

## 配置說明

### 用戶級配置 (~/.kiro/settings/mcp.json)

```json
{
  "mcpServers": {
    "aws-docs": {
      "command": "uvx",
      "args": ["awslabs.aws-documentation-mcp-server@latest"],
      "env": {
        "FASTMCP_LOG_LEVEL": "ERROR"
      },
      "disabled": false,
      "autoApprove": []
    },
    "aws-cdk": {
      "command": "uvx", 
      "args": ["awslabs.aws-cdk-mcp-server@latest"],
      "env": {
        "FASTMCP_LOG_LEVEL": "ERROR"
      },
      "disabled": false,
      "autoApprove": []
    },
    "aws-pricing": {
      "command": "uvx",
      "args": ["awslabs.aws-pricing-mcp-server@latest"], 
      "env": {
        "FASTMCP_LOG_LEVEL": "ERROR"
      },
      "disabled": false,
      "autoApprove": []
    },
    "aws-iam": {
      "command": "uvx",
      "args": ["awslabs.aws-iam-mcp-server@latest"],
      "env": {
        "FASTMCP_LOG_LEVEL": "ERROR"
      },
      "disabled": false,
      "autoApprove": []
    },
    "github": {
      "command": "uvx",
      "args": ["github-mcp-server@latest"],
      "env": {
        "GITHUB_PERSONAL_ACCESS_TOKEN": "${GITHUB_TOKEN}"
      },
      "disabled": false,
      "autoApprove": []
    }
  }
}
```

## 使用範例

### AWS 文檔查詢

```
搜尋 AWS Lambda 最佳實踐
```

### CDK 指導

```
檢查我的 CDK 程式碼是否符合 Nag 規則
```

### 定價分析

```
查詢 EC2 t3.medium 在 us-east-1 的價格
```

### GitHub 整合

```
建立一個 PR 來實現新功能
```

## 安裝需求

確保已安裝 `uv` 和 `uvx`：

```bash
# macOS (使用 Homebrew)
brew install uv

# 其他平台請參考: https://docs.astral.sh/uv/getting-started/installation/
```
