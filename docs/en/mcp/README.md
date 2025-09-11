# MCP (Model Context Protocol) Integration Guide

## Overview

This project integrates multiple MCP servers to provide AI-assisted development capabilities, including AWS ecosystem support, GitHub integration, and intelligent development guidance.

## Integrated MCP Servers

### 🏗️ AWS Development Support

- **aws-docs**: AWS official documentation queries and best practices
- **aws-cdk**: CDK build guidance and Nag rule checking
- **aws-pricing**: AWS service pricing queries and cost estimation
- **aws-iam**: IAM user, role, and policy management

### 🐙 GitHub Integration

- **github**: Code review, issue tracking, and PR management

## Configuration

### User-Level Configuration (~/.kiro/settings/mcp.json)

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

## Usage Examples

### AWS Documentation Query

```
Search for AWS Lambda best practices
```

### CDK Guidance

```
Check if my CDK code complies with Nag rules
```

### Pricing Analysis

```
Query EC2 t3.medium pricing in us-east-1
```

### GitHub Integration

```
Create a PR to implement new feature
```

## Installation Requirements

Ensure `uv` and `uvx` are installed:

```bash
# macOS (using Homebrew)
brew install uv

# For other platforms, refer to: https://docs.astral.sh/uv/getting-started/installation/
```
