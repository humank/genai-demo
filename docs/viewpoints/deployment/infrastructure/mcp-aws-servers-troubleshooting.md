# AWS MCP Servers Troubleshooting Guide

> **Last Updated**: 2025-11-07  
> **Issue**: IAM, Lambda, and Pricing MCP servers connection timeout

---

## 🔴 Current Issue

The following AWS MCP servers are experiencing connection timeouts:

- ❌ `awslabs.iam-mcp-server`
- ❌ `awslabs.lambda-mcp-server`
- ❌ `awslabs.aws-pricing-mcp-server`

---

## 🔍 Diagnosis

### AWS Credentials Status

```bash
✅ AWS Profile: kim-sso
✅ Account: 584518143473
✅ Role: AWSAdministratorAccess
✅ Region: us-east-1 (default)
```

### Possible Causes

1. **Server Startup Time** ⏱️
   - AWS MCP servers may take longer to initialize
   - First-time package download via `uvx`
   - Network latency to AWS services

2. **Region Mismatch** 🌏
   - Config specifies: `ap-northeast-1`
   - AWS CLI default: `us-east-1`
   - May cause confusion or delays

3. **Package Installation Issues** 📦
   - `uvx` needs to download packages on first run
   - Network issues during download
   - Package version conflicts

4. **Timeout Settings** ⏰
   - Kiro's default MCP timeout may be too short
   - AWS API calls can be slow

---

## 🔧 Solutions

### Solution 1: Disable Problematic Servers (Quick Fix)

If you don't need these servers immediately, disable them:

**Edit `~/.kiro/settings/mcp.json`:**

```json
{
  "mcpServers": {
    "awslabs.lambda-mcp-server": {
      "disabled": true,  // Add this line
      // ... rest of config
    },
    "awslabs.iam-mcp-server": {
      "disabled": true,  // Add this line
      // ... rest of config
    },
    "awslabs.aws-pricing-mcp-server": {
      "disabled": true,  // Add this line
      // ... rest of config
    }
  }
}
```

**Then restart Kiro.**

---

### Solution 2: Pre-install Packages (Recommended)

Install the packages manually first to avoid timeout during Kiro startup:

```bash
# Install Lambda MCP server
uvx awslabs.lambda-mcp-server@latest --help

# Install IAM MCP server
uvx awslabs.iam-mcp-server@latest --help

# Install Pricing MCP server
uvx awslabs.aws-pricing-mcp-server@latest --help
```

This will:

- Download and cache the packages
- Verify they work with your AWS credentials
- Speed up Kiro startup

**Then restart Kiro.**

---

### Solution 3: Fix Region Configuration

Ensure consistent region configuration:

**Option A: Use us-east-1 (matches AWS CLI default)**

Edit `~/.kiro/settings/mcp.json`:

```json
{
  "mcpServers": {
    "awslabs.lambda-mcp-server": {
      "env": {
        "FASTMCP_LOG_LEVEL": "ERROR",
        "AWS_PROFILE": "kim-sso",
        "AWS_REGION": "us-east-1"  // Changed from ap-northeast-1
      }
    },
    "awslabs.iam-mcp-server": {
      "env": {
        "FASTMCP_LOG_LEVEL": "ERROR",
        "AWS_PROFILE": "kim-sso",
        "AWS_REGION": "us-east-1"  // Changed from ap-northeast-1
      }
    },
    "awslabs.aws-pricing-mcp-server": {
      "env": {
        "FASTMCP_LOG_LEVEL": "ERROR",
        "AWS_PROFILE": "kim-sso",
        "AWS_REGION": "us-east-1"  // Changed from ap-northeast-1
      }
    }
  }
}
```

**Option B: Keep ap-northeast-1 (if you need Tokyo region)**

Keep the config as is, but be aware:

- Lambda functions must exist in ap-northeast-1
- IAM is global, so region doesn't matter much
- Pricing API works globally

---

### Solution 4: Increase Logging for Debugging

Temporarily increase log level to see what's happening:

```json
{
  "mcpServers": {
    "awslabs.lambda-mcp-server": {
      "env": {
        "FASTMCP_LOG_LEVEL": "DEBUG",  // Changed from ERROR
        "AWS_PROFILE": "kim-sso",
        "AWS_REGION": "ap-northeast-1"
      }
    }
  }
}
```

Check Kiro logs to see detailed error messages.

---

### Solution 5: Test Servers Manually

Test each server independently to identify the issue:

```bash
# Test Lambda server
AWS_PROFILE=kim-sso AWS_REGION=ap-northeast-1 uvx awslabs.lambda-mcp-server@latest

# Test IAM server
AWS_PROFILE=kim-sso AWS_REGION=ap-northeast-1 uvx awslabs.iam-mcp-server@latest --readonly

# Test Pricing server
AWS_PROFILE=kim-sso AWS_REGION=ap-northeast-1 uvx awslabs.aws-pricing-mcp-server@latest
```

If any fail, you'll see the actual error message.

---

## 📋 Recommended Action Plan

### Step 1: Quick Fix (Immediate)

Disable the problematic servers to unblock your work:

```bash
# Edit global config
code ~/.kiro/settings/mcp.json

# Set disabled: true for:
# - awslabs.lambda-mcp-server
# - awslabs.iam-mcp-server  
# - awslabs.aws-pricing-mcp-server

# Restart Kiro
```

### Step 2: Investigate (When Time Permits)

1. **Pre-install packages**:

   ```bash
   uvx awslabs.lambda-mcp-server@latest --help
   uvx awslabs.iam-mcp-server@latest --help
   uvx awslabs.aws-pricing-mcp-server@latest --help
   ```

2. **Test manually** to see actual errors

3. **Check if you actually need these servers**:
   - Do you manage Lambda functions via Kiro?
   - Do you need IAM information in Kiro?
   - Do you need AWS pricing in Kiro?

### Step 3: Re-enable (If Needed)

Once packages are pre-installed and tested:

1. Set `disabled: false` in config
2. Restart Kiro
3. Verify connection

---

## 🎯 Minimal Working Configuration

If you don't need AWS resource management via MCP, here's a minimal config:

**Global Config** (`~/.kiro/settings/mcp.json`):

```json
{
  "mcpServers": {
    "github": {
      "command": "uvx",
      "args": [
        "mcp-proxy",
        "--transport", "streamablehttp",
        "--headers", "Authorization", "Bearer YOUR_TOKEN",
        "https://api.githubcopilot.com/mcp/"
      ],
      "disabled": false,
      "autoApprove": ["list_issues", "get_issue", "create_pull_request"]
    }
  }
}
```

**Project Config** (`.kiro/settings/mcp.json`):

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
      "autoApprove": ["search_aws_documentation", "read_documentation"]
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

This keeps:

- ✅ Documentation servers (aws-docs, aws-cdk)
- ✅ Pricing analysis (aws-pricing)
- ✅ Diagram creation (excalidraw)
- ✅ Time utilities (time)
- ✅ GitHub integration (github)

Removes:

- ❌ Lambda management (rarely needed in IDE)
- ❌ IAM management (rarely needed in IDE)

---

## 🔍 Debugging Commands

### Check Package Installation

```bash
# List installed uvx packages
ls ~/.local/share/uv/tools/

# Check if AWS MCP servers are installed
ls ~/.local/share/uv/tools/ | grep awslabs
```

### Test AWS Connectivity

```bash
# Test AWS CLI works
aws sts get-caller-identity --profile kim-sso

# Test Lambda access
aws lambda list-functions --profile kim-sso --region ap-northeast-1 --max-items 1

# Test IAM access
aws iam list-users --profile kim-sso --max-items 1
```

### Check Kiro Logs

Look for MCP-related errors in Kiro's output panel or logs.

---

## 📊 Server Priority Assessment

| Server | Priority | Use Case | Recommendation |
|--------|----------|----------|----------------|
| `aws-docs` | 🔴 High | Documentation lookup | ✅ Keep enabled |
| `aws-cdk` | 🔴 High | CDK development | ✅ Keep enabled |
| `aws-pricing` | 🟡 Medium | Cost analysis | ✅ Keep enabled |
| `excalidraw` | 🟡 Medium | Diagrams | ✅ Keep enabled |
| `time` | 🟡 Medium | Time operations | ✅ Keep enabled |
| `github` | 🟡 Medium | GitHub ops | ✅ Keep enabled |
| `lambda` | 🟢 Low | Lambda management | ⚠️ Disable if timeout |
| `iam` | 🟢 Low | IAM queries | ⚠️ Disable if timeout |

---

## ✅ Quick Fix Script

Save this as `fix-aws-mcp-servers.sh`:

```bash
#!/bin/bash

echo "🔧 Fixing AWS MCP Server Issues"
echo ""

# Backup
cp ~/.kiro/settings/mcp.json ~/.kiro/settings/mcp.json.backup.$(date +%Y%m%d_%H%M%S)
echo "✅ Backup created"

# Disable problematic servers
jq '.mcpServers["awslabs.lambda-mcp-server"].disabled = true |
    .mcpServers["awslabs.iam-mcp-server"].disabled = true |
    .mcpServers["awslabs.aws-pricing-mcp-server"].disabled = true' \
    ~/.kiro/settings/mcp.json > ~/.kiro/settings/mcp.json.tmp

mv ~/.kiro/settings/mcp.json.tmp ~/.kiro/settings/mcp.json

echo "✅ Disabled problematic AWS MCP servers"
echo ""
echo "📋 Next steps:"
echo "1. Restart Kiro"
echo "2. Verify other servers work"
echo "3. Optionally pre-install packages and re-enable"
```

Run with:

```bash
chmod +x fix-aws-mcp-servers.sh
./fix-aws-mcp-servers.sh
```

---

## 🎯 Recommended Solution

**For immediate productivity**:

1. Disable the three problematic servers
2. Keep the working servers (aws-docs, aws-cdk, aws-pricing in project, github in global)
3. Restart Kiro

**For long-term**:

1. Pre-install the packages when you have time
2. Test them manually
3. Re-enable only if you actually need them

Most developers don't need Lambda/IAM management directly in their IDE, so disabling them is perfectly fine.

---

**Related Documentation**:

- [MCP Cleanup Report](./mcp-cleanup-report.md)
- [MCP Server Analysis](./mcp-server-analysis.md)
