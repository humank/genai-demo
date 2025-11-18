# AWS API MCP Server Setup Report

**Date**: 2025-11-11
**Status**: ✅ Successfully configured

## 📦 Installation Summary

### What is AWS API MCP Server?

AWS API MCP Server enables AI assistants to interact with AWS services and resources through AWS CLI commands. It provides programmatic access to manage your AWS infrastructure while maintaining proper security controls.

**Key Features**:
- Execute AWS CLI commands through AI assistant
- Access all AWS services (EC2, S3, Lambda, etc.)
- Built-in safety controls with mutation consent
- Support for read-only mode
- Works with your existing AWS credentials

## ✅ Configuration Details

### Server Configuration

```json
{
  "command": "uvx",
  "args": [
    "awslabs.aws-api-mcp-server@latest"
  ],
  "env": {
    "AWS_REGION": "ap-northeast-1",
    "AWS_API_MCP_PROFILE_NAME": "kim-sso",
    "READ_OPERATIONS_ONLY": "false",
    "REQUIRE_MUTATION_CONSENT": "true",
    "FASTMCP_LOG_LEVEL": "ERROR"
  },
  "disabled": false,
  "autoApprove": [
    "call_aws"
  ]
}
```

### Configuration Breakdown

| Setting | Value | Description |
|---------|-------|-------------|
| **Region** | `ap-northeast-1` | Tokyo region (matches your AWS profile) |
| **Profile** | `kim-sso` | Uses your existing AWS SSO profile |
| **Read Only** | `false` | Can execute write operations |
| **Mutation Consent** | `true` | ⚠️ Requires confirmation before write operations |
| **Status** | `enabled` | Server is active |
| **Auto Approve** | `call_aws` | Automatically approves AWS CLI calls |

## 🔒 Security Features

### Mutation Consent (Enabled)

When `REQUIRE_MUTATION_CONSENT` is set to `true`:
- ✅ Read operations execute immediately
- ⚠️ Write operations require explicit confirmation
- 🛡️ Prevents accidental resource modifications
- 📝 Provides clear description of what will be changed

**Example Write Operations** (require consent):
- Creating EC2 instances
- Deleting S3 buckets
- Modifying security groups
- Updating Lambda functions

**Example Read Operations** (no consent needed):
- Listing EC2 instances
- Describing S3 buckets
- Getting CloudWatch metrics
- Viewing IAM policies

### IAM Permissions

The server uses your AWS profile's IAM permissions:
- Profile: `kim-sso`
- Region: `ap-northeast-1`
- Permissions: Based on your IAM role/user

**Important**: The server can only perform actions that your IAM credentials allow.

## 🚀 Usage Examples

Once Kiro is restarted, you can ask:

### Read Operations (No Consent Required)

```
"List all my EC2 instances"
"Show me S3 buckets in ap-northeast-1"
"What Lambda functions do I have?"
"Get CloudWatch metrics for my application"
"Describe my VPC configuration"
```

### Write Operations (Consent Required)

```
"Create a new S3 bucket named my-app-data"
"Start the EC2 instance i-1234567890abcdef0"
"Update Lambda function code"
"Create a new security group for web servers"
"Delete unused EBS volumes"
```

## 📊 Current MCP Server Status

### ✅ Active Servers (8)

| Server | Purpose | Status |
|--------|---------|--------|
| time | Time utilities | ✅ Active |
| aws-docs | AWS documentation | ✅ Active |
| aws-cdk | CDK guidance | ✅ Active |
| excalidraw | Diagrams | ✅ Active |
| fetch | HTTP requests | ✅ Active |
| sqlite | Database queries | ✅ Active |
| github | GitHub integration | ✅ Active |
| **aws-api** | **AWS CLI operations** | ✅ **Newly Added** |

### ❌ Disabled Servers (10)

Available to enable if needed:
- awslabs.core-mcp-server
- awslabs.aws-pricing-mcp-server
- awslabs.terraform-mcp-server
- kubernetes
- docker
- awslabs.lambda-mcp-server
- awslabs.ec2-mcp-server
- awslabs.iam-mcp-server
- ppt-automation

## 🔄 Next Steps

### 1. Restart Kiro IDE (Required)

**Important**: You must restart Kiro for the new server to load.

1. **Close Kiro completely**
2. **Reopen Kiro**
3. MCP servers will reconnect automatically

### 2. Verify AWS Credentials

Ensure your AWS SSO session is active:

```bash
# Check AWS credentials
aws sts get-caller-identity --profile kim-sso

# If expired, login again
aws sso login --profile kim-sso
```

### 3. Test the Server

After restarting Kiro, try these commands:

**Simple Read Test**:
```
"List my EC2 instances in ap-northeast-1"
```

**Bucket List Test**:
```
"Show me all S3 buckets"
```

**Region-Specific Test**:
```
"What Lambda functions do I have in Tokyo region?"
```

### 4. Verify Server Status

In Kiro:
1. Press `Cmd+Shift+P`
2. Type `MCP`
3. Select `MCP: Show Server Status`
4. Confirm `awslabs.aws-api-mcp-server` is connected

## 🛠️ Troubleshooting

### Server Not Connecting

**Check AWS credentials**:
```bash
aws sts get-caller-identity --profile kim-sso
```

**If expired, re-login**:
```bash
aws sso login --profile kim-sso
```

### Permission Denied Errors

The server uses your IAM permissions. If you get permission errors:
1. Check your IAM role/user permissions
2. Ensure the profile `kim-sso` has necessary permissions
3. Contact your AWS administrator if needed

### Server Not Appearing

1. Verify configuration:
   ```bash
   jq '.mcpServers."awslabs.aws-api-mcp-server"' ~/.kiro/settings/mcp.json
   ```

2. Check MCP logs in Kiro:
   - View → Output
   - Select "MCP" from dropdown

3. Restart Kiro completely

## 📝 Configuration Options

### Change to Read-Only Mode

If you want to prevent all write operations:

```bash
# Edit config
nano ~/.kiro/settings/mcp.json

# Change this line:
"READ_OPERATIONS_ONLY": "false"

# To:
"READ_OPERATIONS_ONLY": "true"

# Save and restart Kiro
```

### Disable Mutation Consent

If you want to allow write operations without confirmation (not recommended):

```bash
# Edit config
nano ~/.kiro/settings/mcp.json

# Change this line:
"REQUIRE_MUTATION_CONSENT": "true"

# To:
"REQUIRE_MUTATION_CONSENT": "false"

# Save and restart Kiro
```

### Change AWS Region

```bash
# Edit config
nano ~/.kiro/settings/mcp.json

# Change this line:
"AWS_REGION": "ap-northeast-1"

# To your preferred region, e.g.:
"AWS_REGION": "us-east-1"

# Save and restart Kiro
```

## 🔗 Additional Resources

- **Official Documentation**: https://awslabs.github.io/mcp/servers/aws-api-mcp-server
- **GitHub Repository**: https://github.com/awslabs/mcp-servers
- **AWS CLI Reference**: https://docs.aws.amazon.com/cli/latest/reference/

## ⚠️ Important Notes

### Security Best Practices

1. **Always use mutation consent** in production environments
2. **Review permissions** before executing write operations
3. **Use read-only mode** when exploring or learning
4. **Keep AWS credentials secure** and rotate regularly
5. **Monitor AWS CloudTrail** for API activity

### Cost Considerations

- AWS API calls may incur costs
- Be careful with resource creation commands
- Review AWS pricing before executing operations
- Use AWS Cost Explorer to monitor spending

### Backup and Safety

- Always backup important resources before modifications
- Test commands in development environment first
- Use AWS CloudFormation for infrastructure as code
- Enable AWS Config for resource tracking

---

**Configuration Status**: ✅ Ready to use
**Next Action**: 🔄 Restart Kiro IDE
**Report Generated**: 2025-11-11

