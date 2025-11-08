# MCP Configuration Cleanup Recommendations

> **Last Updated**: 2025-01-22  
> **Status**: Ready for Execution

## 🎯 Quick Summary

Your MCP configuration has:
- ✅ **5 working project servers** (time, aws-docs, aws-cdk, aws-pricing, excalidraw)
- ✅ **6 working global servers** (github, aws-docs, cdk, pricing, lambda, iam)
- ⚠️ **2 duplicates** (aws-docs, time)
- ⚠️ **10 disabled servers** in global config (taking up space)

---

## 📋 Recommended Actions

### Step 1: Backup Configurations ✅

```bash
# Backup global config
cp ~/.kiro/settings/mcp.json ~/.kiro/settings/mcp.json.backup.$(date +%Y%m%d)

# Backup project config
cp .kiro/settings/mcp.json .kiro/settings/mcp.json.backup.$(date +%Y%m%d)
```

### Step 2: Remove Duplicates from Global Config

**Duplicates to Remove from `~/.kiro/settings/mcp.json`:**

1. **`aws-docs`** - Already active in project config
2. **`time`** - Already active in project config (global version is disabled anyway)

**Why**: Project-level config takes precedence, so these global entries are unused.

**Note about Time Server**: 
- The `time` MCP server provides timezone conversions and time formatting
- However, Kiro can also get current time via system commands (`date`)
- **Recommendation**: Keep `time` server in project config for advanced time operations
  - Timezone conversions
  - Time formatting in different formats
  - Time difference calculations
  - Multiple timezone support

### Step 3: Clean Up Disabled Servers (Optional)

**Disabled servers in global config that can be removed:**

| Server | Reason to Remove |
|--------|------------------|
| `aws-knowledge-mcp-server` | Requires AWS MCP service access (not available) |
| `fetch` | Not being used |
| `awslabs.core-mcp-server` | Not being used |
| `awslabs.terraform-mcp-server` | Not using Terraform in this project |
| `sqlite` | Not using SQLite |
| `kubernetes` | Not managing K8s from MCP |
| `docker` | Not managing Docker from MCP |
| `awslabs.ec2-mcp-server` | Not managing EC2 from MCP |
| `ppt-automation` | Depends on external project |

**Keep these disabled servers if you might use them later:**
- `kubernetes`, `docker`, `sqlite` - Useful for infrastructure management
- `awslabs.ec2-mcp-server` - Useful for AWS EC2 management

### Step 4: Fix GitHub Token (If Using GitHub MCP)

The GitHub token in global config may be expired:
```json
"Bearer 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"
```

**Action**:
1. Generate new token: https://github.com/settings/tokens
2. Update in `~/.kiro/settings/mcp.json`
3. Or disable the server if not needed

---

## 🔧 Manual Cleanup Steps

### Option A: Conservative Cleanup (Recommended)

Only remove duplicates, keep disabled servers for future use.

**Edit `~/.kiro/settings/mcp.json`:**

```bash
# Open in editor
code ~/.kiro/settings/mcp.json

# Remove these two entries:
# 1. "aws-docs": { ... }
# 2. "time": { ... }
```

### Option B: Aggressive Cleanup

Remove duplicates AND all disabled servers.

**Edit `~/.kiro/settings/mcp.json`:**

Remove these entries:
- `aws-docs`
- `time`
- `aws-knowledge-mcp-server`
- `fetch`
- `awslabs.core-mcp-server`
- `awslabs.terraform-mcp-server`
- `sqlite`
- `kubernetes`
- `docker`
- `awslabs.ec2-mcp-server`
- `ppt-automation`

---

## 📊 Recommended Final Configuration

### Global Config (`~/.kiro/settings/mcp.json`)

**Keep only these active servers:**

```json
{
  "mcpServers": {
    "github": {
      "command": "uvx",
      "args": [
        "mcp-proxy",
        "--transport", "streamablehttp",
        "--headers", "Authorization", "Bearer YOUR_NEW_TOKEN",
        "https://api.githubcopilot.com/mcp/"
      ],
      "disabled": false,
      "autoApprove": [
        "list_issues", "get_issue", "get_issue_comments",
        "create_pull_request", "get_me", "get_pull_request",
        "search_pull_requests", "update_pull_request",
        "search_repositories", "get_file_contents", "create_issue"
      ]
    },
    "awslabs.cdk-mcp-server": {
      "command": "uvx",
      "args": ["awslabs.cdk-mcp-server@latest"],
      "env": {"FASTMCP_LOG_LEVEL": "ERROR"},
      "disabled": false,
      "autoApprove": [
        "CDKGeneralGuidance",
        "ExplainCDKNagRule",
        "CheckCDKNagSuppressions"
      ]
    },
    "awslabs.aws-pricing-mcp-server": {
      "command": "uvx",
      "args": ["awslabs.aws-pricing-mcp-server@latest"],
      "env": {"FASTMCP_LOG_LEVEL": "ERROR"},
      "disabled": false,
      "autoApprove": [
        "analyze_cdk_project",
        "get_pricing",
        "generate_cost_report"
      ]
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
      "autoApprove": [
        "list_functions",
        "invoke_function",
        "get_function_info",
        "update_function_code"
      ]
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
      "autoApprove": [
        "list_users",
        "list_roles",
        "get_user_policies",
        "get_role_policies"
      ]
    }
  }
}
```

### Project Config (`.kiro/settings/mcp.json`)

**Keep as is - already optimal:**

```json
{
  "mcpServers": {
    "time": {
      "command": "uvx",
      "args": ["mcp-server-time"],
      "env": {},
      "disabled": false,
      "autoApprove": [
        "get_current_time",
        "get_timezone",
        "convert_time",
        "format_time",
        "calculate_time_difference"
      ]
    },
    "aws-docs": {
      "command": "uvx",
      "args": ["awslabs.aws-documentation-mcp-server@latest"],
      "env": {"FASTMCP_LOG_LEVEL": "ERROR"},
      "disabled": false,
      "autoApprove": [
        "search_aws_documentation",
        "get_aws_service_info",
        "search_documentation",
        "read_documentation"
      ]
    },
    "aws-cdk": {
      "command": "uvx",
      "args": ["awslabs.cdk-mcp-server@latest"],
      "env": {"FASTMCP_LOG_LEVEL": "ERROR"},
      "disabled": false,
      "autoApprove": [
        "CDKGeneralGuidance",
        "ExplainCDKNagRule",
        "CheckCDKNagSuppressions"
      ]
    },
    "aws-pricing": {
      "command": "uvx",
      "args": ["awslabs.aws-pricing-mcp-server@latest"],
      "env": {"FASTMCP_LOG_LEVEL": "ERROR"},
      "disabled": false,
      "autoApprove": [
        "analyze_cdk_project",
        "get_pricing",
        "generate_cost_report",
        "get_pricing_service_codes"
      ]
    },
    "excalidraw": {
      "command": "node",
      "args": [
        "/Users/yikaikao/git/genai-demo/node_modules/mcp-excalidraw-server/src/index.js"
      ],
      "env": {"ENABLE_CANVAS_SYNC": "false"},
      "disabled": false,
      "autoApprove": [
        "create_element", "update_element", "delete_element",
        "query_elements", "get_resource", "group_elements",
        "ungroup_elements", "align_elements", "distribute_elements",
        "lock_elements", "unlock_elements", "batch_create_elements"
      ]
    }
  }
}
```

---

## ✅ Verification Steps

After cleanup:

1. **Restart Kiro**
   - Close and reopen Kiro to reload MCP configuration

2. **Check MCP Server Status**
   - Open Command Palette: `Cmd+Shift+P`
   - Search: "MCP Server"
   - Select: "View MCP Servers"
   - Verify all servers show "Connected"

3. **Test Key Servers**
   ```
   Ask Kiro to:
   - "What time is it?" (tests time server)
   - "Search AWS docs for Lambda" (tests aws-docs)
   - "Create a simple diagram" (tests excalidraw)
   ```

---

## 📈 Expected Results

**Before Cleanup:**
- Total servers: 21
- Active: 11
- Disabled: 10
- Duplicates: 2

**After Conservative Cleanup:**
- Total servers: 19
- Active: 11
- Disabled: 8
- Duplicates: 0

**After Aggressive Cleanup:**
- Total servers: 11
- Active: 11
- Disabled: 0
- Duplicates: 0

---

## 🔄 Rollback Plan

If something goes wrong:

```bash
# Restore global config
cp ~/.kiro/settings/mcp.json.backup.YYYYMMDD ~/.kiro/settings/mcp.json

# Restore project config
cp .kiro/settings/mcp.json.backup.YYYYMMDD .kiro/settings/mcp.json

# Restart Kiro
```

---

## 📝 Notes

- **Excalidraw**: ✅ Fully functional, no changes needed
- **GitHub**: ⚠️ Update token if using GitHub features
- **AWS Servers**: ✅ All working with kim-sso profile
- **Duplicates**: Safe to remove from global config

**Recommendation**: Start with **Conservative Cleanup** to minimize risk.
