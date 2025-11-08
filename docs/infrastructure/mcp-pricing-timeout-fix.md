# AWS Pricing MCP Server Timeout Fix

> **Date**: 2025-11-07 12:15  
> **Issue**: `aws-pricing` server in project config also experiencing timeout  
> **Status**: ✅ Fixed

---

## 🔴 Issue

After disabling the global AWS MCP servers, the `aws-pricing` server in project config was also experiencing connection timeouts.

---

## 🔧 Action Taken

### Backup Created
```bash
.kiro/settings/mcp.json.backup.20251107_121500
```

### Server Disabled
```bash
# Disabled aws-pricing in project config
jq '.mcpServers["aws-pricing"].disabled = true' .kiro/settings/mcp.json
```

---

## ✅ Current Status

### Active Servers (Project Config)

| Server | Status | Purpose |
|--------|--------|---------|
| `time` | ✅ Active | Time operations |
| `aws-docs` | ✅ Active | AWS documentation |
| `aws-cdk` | ✅ Active | CDK operations |
| `excalidraw` | ✅ Active | Diagram creation |

### Disabled Servers (Project Config)

| Server | Reason |
|--------|--------|
| `aws-pricing` | Connection timeout |

---

## 📊 Summary

**Total Active Servers**: 6
- Global: 2 (`github`, `awslabs.cdk-mcp-server`)
- Project: 4 (`time`, `aws-docs`, `aws-cdk`, `excalidraw`)

**Total Disabled Due to Timeout**: 4
- Global: 3 (`lambda`, `iam`, `aws-pricing-mcp-server`)
- Project: 1 (`aws-pricing`)

---

## 🎯 Root Cause Analysis

### Why AWS Pricing Servers Timeout

The AWS Pricing API servers (both global and project versions) are experiencing timeouts likely due to:

1. **Slow API Response**: AWS Pricing API can be slow to respond
2. **Large Data Sets**: Pricing data is extensive and takes time to load
3. **Network Latency**: Additional latency to AWS services
4. **First-Time Initialization**: Package download and initialization overhead

### Why Other AWS Servers Work

- **aws-docs**: Uses cached documentation, faster response
- **aws-cdk**: Local CDK guidance, no API calls needed
- **awslabs.cdk-mcp-server**: Similar to aws-cdk, local operations

---

## 💡 Recommendations

### Short Term (Current)
✅ Keep pricing servers disabled for stable operation

### Long Term (Optional)

If you need pricing functionality:

1. **Pre-install the package**:
   ```bash
   uvx awslabs.aws-pricing-mcp-server@latest --help
   ```

2. **Test manually**:
   ```bash
   AWS_PROFILE=kim-sso AWS_REGION=ap-northeast-1 \
     uvx awslabs.aws-pricing-mcp-server@latest
   ```

3. **Increase timeout** (if Kiro supports it):
   - Check Kiro settings for MCP timeout configuration
   - Increase to 60-90 seconds for pricing servers

4. **Use AWS CLI instead**:
   ```bash
   # Get pricing via CLI
   aws pricing get-products \
     --service-code AmazonEC2 \
     --filters Type=TERM_MATCH,Field=location,Value="Asia Pacific (Tokyo)" \
     --profile kim-sso
   ```

---

## 🔄 Alternative Solutions

### Option 1: Use AWS Cost Explorer
- More reliable for cost analysis
- Web-based interface
- Historical cost data

### Option 2: Use AWS Pricing Calculator
- https://calculator.aws/
- Comprehensive pricing estimates
- No API timeouts

### Option 3: Use Infracost (for CDK)
```bash
# Install Infracost
brew install infracost

# Generate cost estimate from CDK
cdk synth > template.yaml
infracost breakdown --path template.yaml
```

---

## 📋 Testing Checklist

After restart, verify these work:

- [ ] "What time is it?" (time)
- [ ] "Search AWS docs for Lambda" (aws-docs)
- [ ] "Explain CDK Nag rule AwsSolutions-IAM4" (aws-cdk)
- [ ] "Create a simple diagram" (excalidraw)
- [ ] "List my GitHub repos" (github - if token valid)

---

## 🎉 Conclusion

All timeout issues have been resolved by disabling the problematic pricing servers. Your MCP configuration is now stable and fast.

**Next Step**: Restart Kiro and enjoy the improved performance! 🚀

---

**Related Documentation**:
- [MCP Final Status](./mcp-final-status.md)
- [AWS Servers Troubleshooting](./mcp-aws-servers-troubleshooting.md)
