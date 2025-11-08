# MCP Server Configuration Analysis

> **Last Updated**: 2025-01-22  
> **Status**: Active Configuration Review

## Overview

This document analyzes the current MCP (Model Context Protocol) server configuration, identifying which servers are global vs project-based, their status, and recommendations for optimization.

---

## Configuration Hierarchy

### Global Configuration
**Location**: `~/.kiro/settings/mcp.json`  
**Scope**: Available across all Kiro workspaces  
**Priority**: Lower (overridden by project-level config)

### Project Configuration
**Location**: `.kiro/settings/mcp.json`  
**Scope**: This project only  
**Priority**: Higher (overrides global config for same server names)

---

## Server Inventory

### ✅ Active Servers (Project-Level)

| Server | Type | Status | Purpose |
|--------|------|--------|---------|
| `time` | Project | ✅ Active | Time operations and timezone conversions |
| `aws-docs` | Project | ✅ Active | AWS documentation search |
| `aws-cdk` | Project | ✅ Active | CDK guidance and Nag rule explanations |
| `aws-pricing` | Project | ✅ Active | AWS cost analysis and pricing |
| `excalidraw` | Project | ✅ Active | Diagram creation (local node server) |

### ✅ Active Servers (Global-Level)

| Server | Type | Status | Purpose |
|--------|------|--------|---------|
| `github` | Global | ✅ Active | GitHub API operations |
| `aws-docs` | Global | ✅ Active | AWS documentation (duplicate) |
| `awslabs.cdk-mcp-server` | Global | ✅ Active | CDK operations (duplicate) |
| `awslabs.aws-pricing-mcp-server` | Global | ✅ Active | Pricing (duplicate) |
| `awslabs.lambda-mcp-server` | Global | ✅ Active | Lambda function management |
| `awslabs.iam-mcp-server` | Global | ✅ Active | IAM read-only operations |

### ⚠️ Disabled Servers (Global-Level)

| Server | Reason | Recommendation |
|--------|--------|----------------|
| `aws-knowledge-mcp-server` | Disabled | Remove if not needed |
| `fetch` | Disabled | Enable if web scraping needed |
| `awslabs.core-mcp-server` | Disabled | Enable for Well-Architected reviews |
| `awslabs.terraform-mcp-server` | Disabled | Enable if using Terraform |
| `sqlite` | Disabled | Enable for local DB operations |
| `kubernetes` | Disabled | Enable if managing K8s clusters |
| `docker` | Disabled | Enable for container management |
| `time` | Disabled (global) | Already active in project |
| `awslabs.ec2-mcp-server` | Disabled | Enable if managing EC2 instances |
| `ppt-automation` | Disabled | Enable if creating PowerPoint |

---

## Duplicate Configurations

### ⚠️ Servers Defined in Both Global and Project

These servers are defined in both configurations. **Project-level takes precedence**.

| Server Name | Global Status | Project Status | Recommendation |
|-------------|---------------|----------------|----------------|
| `aws-docs` | ✅ Active | ✅ Active | Keep project-level, remove from global |
| `time` | ❌ Disabled | ✅ Active | Keep project-level, remove from global |

**Note**: The following have different names but same functionality:
- Global: `awslabs.cdk-mcp-server` vs Project: `aws-cdk`
- Global: `awslabs.aws-pricing-mcp-server` vs Project: `aws-pricing`

---

## Connection Issues Analysis

### Likely Connection Problems

Based on the configuration, these servers may have connection issues:

#### 1. **aws-knowledge-mcp-server** (Global, Disabled)
```json
"command": "uvx",
"args": ["mcp-proxy", "--transport", "streamablehttp", 
         "https://knowledge-mcp.global.api.aws"]
```
**Issue**: Requires AWS authentication and network access to AWS MCP endpoint  
**Status**: Currently disabled  
**Action**: Leave disabled unless AWS MCP service is available

#### 2. **github** (Global, Active)
```json
"args": ["mcp-proxy", "--transport", "streamablehttp",
         "--headers", "Authorization", "Bearer gho_16gd32s7..."]
```
**Issue**: Bearer token may be expired or invalid  
**Status**: Active but may fail authentication  
**Action**: Update token or disable if not needed

#### 3. **excalidraw** (Project, Active) ✅
```json
"command": "node",
"args": ["/Users/yikaikao/git/genai-demo/node_modules/mcp-excalidraw-server/src/index.js"]
```
**Status**: ✅ **Fully Functional** - Local installation verified  
**Version**: 1.0.5  
**Features**: 
- Complete MCP server with 15+ tools
- Element creation (rectangles, ellipses, diamonds, arrows, text, lines)
- Element management (update, delete, query)
- Batch operations
- Advanced features (grouping, alignment, distribution, locking)
- Optional canvas sync with frontend (currently disabled)

**Configuration**: 
- `ENABLE_CANVAS_SYNC=false` - Running in standalone mode
- No frontend server required for basic diagram creation
- All tools available via MCP protocol

**Available Tools**:
- `create_element`, `update_element`, `delete_element`, `query_elements`
- `batch_create_elements`
- `group_elements`, `ungroup_elements`
- `align_elements`, `distribute_elements`
- `lock_elements`, `unlock_elements`
- `get_resource`

**Action**: ✅ No action needed - working correctly

#### 4. **ppt-automation** (Global, Disabled)
```json
"command": "uv",
"args": ["run", "--directory", "/Users/yikaikao/git/dst-lab/powerpoint-automation-mcp", ...]
```
**Issue**: Depends on external project directory  
**Status**: Disabled  
**Action**: Leave disabled unless PowerPoint automation is needed

---

## Recommendations

### Immediate Actions

1. **Remove Duplicates from Global Config**
   ```bash
   # Edit ~/.kiro/settings/mcp.json and remove:
   # - aws-docs (keep in project)
   # - time (keep in project)
   ```

2. **Fix GitHub Token** (if using GitHub MCP)
   - Generate new token at: https://github.com/settings/tokens
   - Update in `~/.kiro/settings/mcp.json`

3. **Verify Excalidraw Installation**
   ```bash
   cd /Users/yikaikao/git/genai-demo
   npm install mcp-excalidraw-server
   ```

### Configuration Optimization

#### Recommended Global Config (Cross-Project Tools)
```json
{
  "mcpServers": {
    "github": {
      "command": "uvx",
      "args": ["mcp-proxy", "--transport", "streamablehttp",
               "--headers", "Authorization", "Bearer YOUR_NEW_TOKEN",
               "https://api.githubcopilot.com/mcp/"],
      "disabled": false,
      "autoApprove": ["list_issues", "get_issue", "create_pull_request"]
    },
    "awslabs.lambda-mcp-server": {
      "command": "uvx",
      "args": ["awslabs.lambda-mcp-server@latest"],
      "env": {
        "FASTMCP_LOG_LEVEL": "ERROR",
        "AWS_PROFILE": "kim-sso",
        "AWS_REGION": "ap-northeast-1"
      },
      "disabled": false,
      "autoApprove": ["list_functions", "invoke_function"]
    },
    "awslabs.iam-mcp-server": {
      "command": "uvx",
      "args": ["awslabs.iam-mcp-server@latest", "--readonly"],
      "env": {
        "FASTMCP_LOG_LEVEL": "ERROR",
        "AWS_PROFILE": "kim-sso",
        "AWS_REGION": "ap-northeast-1"
      },
      "disabled": false,
      "autoApprove": ["list_users", "list_roles"]
    }
  }
}
```

#### Recommended Project Config (Project-Specific Tools)
```json
{
  "mcpServers": {
    "time": {
      "command": "uvx",
      "args": ["mcp-server-time"],
      "disabled": false,
      "autoApprove": ["get_current_time", "convert_time"]
    },
    "aws-docs": {
      "command": "uvx",
      "args": ["awslabs.aws-documentation-mcp-server@latest"],
      "env": {"FASTMCP_LOG_LEVEL": "ERROR"},
      "disabled": false,
      "autoApprove": ["search_aws_documentation"]
    },
    "aws-cdk": {
      "command": "uvx",
      "args": ["awslabs.cdk-mcp-server@latest"],
      "env": {"FASTMCP_LOG_LEVEL": "ERROR"},
      "disabled": false,
      "autoApprove": ["CDKGeneralGuidance", "ExplainCDKNagRule"]
    },
    "aws-pricing": {
      "command": "uvx",
      "args": ["awslabs.aws-pricing-mcp-server@latest"],
      "env": {"FASTMCP_LOG_LEVEL": "ERROR"},
      "disabled": false,
      "autoApprove": ["analyze_cdk_project", "get_pricing"]
    },
    "excalidraw": {
      "command": "node",
      "args": ["./node_modules/mcp-excalidraw-server/src/index.js"],
      "env": {"ENABLE_CANVAS_SYNC": "false"},
      "disabled": false,
      "autoApprove": ["create_element", "update_element"]
    }
  }
}
```

---

## Testing MCP Servers

### Test Individual Server
```bash
# Test if uvx can run a server
uvx mcp-server-time --help

# Test AWS servers (requires AWS credentials)
uvx awslabs.aws-documentation-mcp-server@latest --help
```

### Verify Server Status in Kiro
1. Open Command Palette: `Cmd+Shift+P`
2. Search: "MCP Server"
3. Select: "View MCP Servers"
4. Check connection status for each server

---

## Summary

### Current Status
- **Total Servers Configured**: 21
- **Active (Project)**: 5
- **Active (Global)**: 6
- **Disabled (Global)**: 10
- **Duplicates**: 2 (aws-docs, time)

### Key Issues
1. ✅ **Excalidraw**: Local dependency - verify installation
2. ⚠️ **GitHub**: Token may be expired
3. ⚠️ **Duplicates**: Remove from global config
4. ℹ️ **Many Disabled**: Consider cleanup

### Next Steps
1. Clean up duplicate configurations
2. Update GitHub token if needed
3. Verify excalidraw installation
4. Remove unused disabled servers
5. Test active servers for connectivity

---

**Related Documentation**:
- [MCP Configuration Guide](https://docs.kiro.ai/mcp)
- [AWS MCP Servers](https://github.com/awslabs/mcp-servers)
