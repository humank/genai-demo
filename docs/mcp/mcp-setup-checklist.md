# MCP Setup Checklist

## 📋 New Team Member MCP Setup Checklist

Use this checklist to ensure MCP (Model Context Protocol) servers are properly installed and configured.

### ✅ Prerequisites Check

- [ ] **Node.js v16+** installed
  ```bash
  node --version  # Should show v16.0.0 or higher
  ```

- [ ] **NPM** installed and available
  ```bash
  npm --version
  ```

- [ ] **UV package manager** installed
  ```bash
  uv --version
  # If not installed: brew install uv (macOS) or pip install uv
  ```

- [ ] **AWS CLI** installed (optional, for AWS MCP tools)
  ```bash
  aws --version
  ```

### ✅ Project Setup Check

- [ ] **Project root directory** confirmed
  ```bash
  pwd  # Should be in genai-demo project root
  ls   # Should see package.json, .kiro/ etc.
  ```

- [ ] **package.json** exists
  ```bash
  ls package.json
  ```

### ✅ Excalidraw MCP Server Installation

- [ ] **Install mcp-excalidraw-server**
  ```bash
  npm install mcp-excalidraw-server
  ```

- [ ] **Verify installation**
  ```bash
  ls node_modules/mcp-excalidraw-server/src/index.js
  # Should show file exists
  ```

- [ ] **Test server**
  ```bash
  # Test with absolute path
  echo '{"jsonrpc": "2.0", "id": 1, "method": "tools-and-environment/list", "params": {}}' | \
    node /ABSOLUTE/PATH/TO/PROJECT/node_modules/mcp-excalidraw-server/src/index.js | head -3
  # Should return JSON response
  
  # Or use fix script
  ./scripts/fix-excalidraw-path.sh
  ```

### ✅ MCP Configuration Check

- [ ] **MCP configuration file exists**
  ```bash
  ls .kiro/settings/mcp.json
  ```

- [ ] **Configuration file format is correct**
  ```bash
  cat .kiro/settings/mcp.json | jq '.'
  # Should display formatted JSON (requires jq)
  ```

- [ ] **Excalidraw server configured**
  ```bash
  cat .kiro/settings/mcp.json | jq '.mcpServers.excalidraw'
  # Should show excalidraw configuration with absolute path
  ```

- [ ] **Fix path issues (if needed)**
  ```bash
  # If encountering path issues, use fix script
  ./scripts/fix-excalidraw-path.sh
  ```

- [ ] **All required MCP servers configured**
  ```bash
  cat .kiro/settings/mcp.json | jq '.mcpServers | keys'
  # Should include: ["aws-cdk", "aws-docs", "aws-pricing", "excalidraw", "time"]
  ```

### ✅ AWS Configuration Check (Optional)

- [ ] **AWS credentials configured**
  ```bash
  aws configure list
  ```

- [ ] **AWS profile settings**
  ```bash
  export AWS_PROFILE=kim-sso
  export AWS_REGION=ap-northeast-1
  ```

- [ ] **AWS credentials test**
  ```bash
  aws sts get-caller-identity
  # Should return user identity information
  ```

### ✅ MCP Server Testing

- [ ] **Time MCP Server**
  ```bash
  uvx mcp-server-time --help
  # Should show help information
  ```

- [ ] **AWS Docs MCP Server**
  ```bash
  uvx awslabs.aws-documentation-mcp-server@latest --help
  # Should show help information
  ```

- [ ] **AWS CDK MCP Server**
  ```bash
  uvx awslabs.cdk-mcp-server@latest --help
  # Should show help information
  ```

- [ ] **AWS Pricing MCP Server**
  ```bash
  uvx awslabs.aws-pricing-mcp-server@latest --help
  # Should show help information
  ```

### ✅ Kiro IDE Integration

- [ ] **Restart Kiro IDE**
  - Close Kiro IDE
  - Restart Kiro IDE
  - Wait for MCP servers to load

- [ ] **Test Excalidraw MCP**
  ```
  Ask Kiro: "Create a simple rectangle with text 'Hello MCP'"
  ```

- [ ] **Test Time MCP**
  ```
  Ask Kiro: "What time is it in Tokyo now?"
  ```

- [ ] **Test AWS Docs MCP** (if AWS configured)
  ```
  Ask Kiro: "Search for AWS Lambda best practices documentation"
  ```

### ✅ Functionality Verification

- [ ] **Excalidraw diagram creation**
  - Can create basic shapes (rectangles, circles, arrows)
  - Can add text
  - Can set colors and styles

- [ ] **Batch element creation**
  ```
  Ask Kiro: "Create a simple flowchart with start, process, and end steps"
  ```

- [ ] **Time conversion functionality**
  ```
  Ask Kiro: "Convert 2 PM EST to Taiwan time"
  ```

### ✅ Troubleshooting Check

If encountering issues, check the following:

- [ ] **File permissions**
  ```bash
  chmod +x node_modules/mcp-excalidraw-server/src/index.js
  ```

- [ ] **Node.js version compatibility**
  ```bash
  node --version  # Must be v16.0.0 or higher
  ```

- [ ] **NPM cache cleanup**
  ```bash
  npm cache clean --force
  ```

- [ ] **UV cache cleanup**
  ```bash
  uv cache clean
  ```

- [ ] **Fix path issues**
  ```bash
  # If seeing "Cannot find module '/node_modules/...'" error
  ./scripts/fix-excalidraw-path.sh
  ```

- [ ] **Reinstall Excalidraw MCP**
  ```bash
  rm -rf node_modules/mcp-excalidraw-server
  npm install mcp-excalidraw-server
  ```

### ✅ Automated Setup (Recommended)

- [ ] **Use automation script**
  ```bash
  ./scripts/setup-mcp-servers.sh
  ```

- [ ] **Check setup report**
  ```bash
  cat mcp-setup-report.txt
  ```

## 🎯 Success Criteria

When all checklist items are completed, you should be able to:

1. ✅ Create Excalidraw diagrams in Kiro IDE
2. ✅ Use time conversion functionality
3. ✅ Query AWS documentation (if configured)
4. ✅ Get CDK best practice recommendations (if configured)
5. ✅ Perform cost analysis (if configured)

## 📚 Related Documentation

- [MCP Integration Guide](../../infrastructure/docs/MCP_INTEGRATION_GUIDE.md)
- [Excalidraw MCP Usage Guide](excalidraw-mcp-usage-guide.md)
- Troubleshooting Guide

## 🆘 Getting Help

If you encounter issues:

1. 📖 Review the troubleshooting section
2. 🔍 Check MCP integration test reports
3. 👥 Consult with DevOps team
4. 📝 Provide detailed error messages and environment details

---

**Checklist Completion Date**: ___________  
**Checked by**: ___________  
**Version**: 1.0  
**Last Updated**: September 21, 2025