# MCP Integration and Well-Architected Framework Guide

## Overview

This guide provides comprehensive instructions for using AWS MCP (Model Context Protocol) tools to perform Well-Architected Framework reviews and continuous architecture improvements for the GenAI Demo project.

## 🚀 Quick Start Installation Guide

### For New Team Members

If you're setting up MCP servers for the first time, follow these steps:

#### 1. Install Prerequisites

```bash
# Install UV (Python package manager)
brew install uv  # macOS
# OR
pip install uv   # Other platforms

# Verify Node.js (required for Excalidraw MCP)
node --version    # Should be v16.0.0 or higher
npm --version
```

#### 2. Install Excalidraw MCP Server

```bash
# Navigate to project root
cd /path/to/genai-demo

# Install the official Excalidraw MCP server
npm install mcp-excalidraw-server

# Verify installation
ls node_modules/mcp-excalidraw-server/src/index.js
```

#### 3. Configure AWS Credentials (if using AWS MCP tools)

```bash
# Configure AWS profile
aws configure --profile kim-sso
export AWS_PROFILE=kim-sso
export AWS_REGION=ap-northeast-1

# Verify credentials
aws sts get-caller-identity
```

#### 4. Verify MCP Configuration

Check that `.kiro/settings/mcp.json` contains the correct configuration:

```bash
# View current MCP configuration
cat .kiro/settings/mcp.json | jq '.mcpServers | keys'

# Expected output: ["aws-cdk", "aws-docs", "aws-pricing", "excalidraw", "time"]
```

#### 5. Test MCP Servers

```bash
# Test Excalidraw MCP server
echo '{"jsonrpc": "2.0", "id": 1, "method": "tools-and-environment/list", "params": {}}' | \
  node node_modules/mcp-excalidraw-server/src/index.js | head -3

# Test Time MCP server
uvx mcp-server-time --help

# Test AWS MCP servers (requires AWS credentials)
uvx awslabs.aws-documentation-mcp-server@latest --help
```

#### 6. Restart Kiro IDE

After installation and configuration, restart Kiro IDE to load all MCP servers.

#### 7. Verify in Kiro IDE

Test the MCP integration by asking Kiro:

```
"Create a simple rectangle with text 'Hello MCP'"
"What time is it in Tokyo?"
"Search AWS documentation for Lambda best practices"
```

### Installation Verification Checklist

- [ ] UV package manager installed and working
- [ ] Node.js v16+ installed
- [ ] `mcp-excalidraw-server` package installed locally
- [ ] AWS credentials configured (if using AWS MCP tools)
- [ ] `.kiro/settings/mcp.json` contains all required servers
- [ ] All MCP servers respond to test commands
- [ ] Kiro IDE restarted after configuration changes
- [ ] MCP tools working in Kiro IDE

### Common Installation Issues

| Issue | Solution |
|-------|----------|
| `uv: command not found` | Install UV: `brew install uv` or `pip install uv` |
| `node: command not found` | Install Node.js from nodejs.org |
| `Cannot find module 'mcp-excalidraw-server'` | Run `npm install mcp-excalidraw-server` |
| `AWS credentials not configured` | Run `aws configure --profile kim-sso` |
| `MCP server not responding` | Restart Kiro IDE and check configuration |

---

## MCP Tools Configuration

### Configured MCP Servers

The following MCP servers are configured in `.kiro/settings/mcp.json`:

#### AWS MCP Servers

| Server | Purpose | Auto-Approved Tools |
|--------|---------|-------------------|
| `aws-docs` | AWS Documentation access | `search_documentation`, `read_documentation` |
| `aws-cdk` | CDK guidance and analysis | `CDKGeneralGuidance`, `ExplainCDKNagRule`, `CheckCDKNagSuppressions` |
| `aws-pricing` | Cost analysis and optimization | `analyze_cdk_project`, `get_pricing`, `generate_cost_report` |
| `aws-iam` | IAM policy analysis | `list_users`, `list_roles`, `get_user_policies`, `get_role_policies` |
| `aws-core` | Well-Architected reviews | `prompt_understanding`, `well_architected_review` |

#### Visualization MCP Servers

| Server | Purpose | Auto-Approved Tools |
|--------|---------|-------------------|
| `excalidraw` | Diagram creation and visualization | `create_element`, `update_element`, `delete_element`, `query_elements`, `batch_create_elements`, `group_elements`, `align_elements`, `distribute_elements` |

#### Utility MCP Servers

| Server | Purpose | Auto-Approved Tools |
|--------|---------|-------------------|
| `time` | Time zone conversion and formatting | `get_current_time`, `convert_time`, `format_time`, `calculate_time_difference` |

### Prerequisites

1. **UV Package Manager**: Install UV for Python package management

   ```bash
   # macOS
   brew install uv
   
   # Or using pip
   pip install uv
   ```

2. **Node.js and NPM**: Required for Excalidraw MCP server

   ```bash
   # Verify Node.js installation (requires v16+)
   node --version
   npm --version
   ```

3. **AWS Credentials**: Configure AWS credentials for IAM and pricing tools

   ```bash
   aws configure --profile kim-sso
   export AWS_PROFILE=kim-sso
   export AWS_REGION=ap-northeast-1
   ```

4. **Kiro IDE**: Ensure Kiro IDE is properly configured with MCP support

## MCP Server Installation Guide

### Excalidraw MCP Server Setup

The Excalidraw MCP server enables AI-powered diagram creation and visualization. Follow these steps to install and configure it:

#### 1. Install the Excalidraw MCP Server

```bash
# Navigate to project root
cd /path/to/genai-demo

# Install the official Excalidraw MCP server
npm install mcp-excalidraw-server

# Verify installation
ls node_modules/mcp-excalidraw-server/
```

#### 2. Verify MCP Configuration

The Excalidraw MCP server should already be configured in `.kiro/settings/mcp.json`:

```json
{
  "mcpServers": {
    "excalidraw": {
      "command": "node",
      "args": [
        "/ABSOLUTE/PATH/TO/PROJECT/node_modules/mcp-excalidraw-server/src/index.js"
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

**重要**: 請將 `/ABSOLUTE/PATH/TO/PROJECT` 替換為你的專案絕對路徑。例如：
- macOS: `/Users/username/git/genai-demo`
- Linux: `/home/username/git/genai-demo`
- Windows: `C:\\Users\\username\\git\\genai-demo`

#### 3. Test Excalidraw MCP Server

```bash
# Test the server directly
echo '{"jsonrpc": "2.0", "id": 1, "method": "tools-and-environment/list", "params": {}}' | \
  node node_modules/mcp-excalidraw-server/src/index.js

# Expected output: JSON response with available tools
```

#### 4. Restart Kiro IDE

After installation, restart Kiro IDE to load the new MCP server configuration.

### Alternative Installation Methods

#### Method 1: Using NPX (Not Recommended for Production)

```json
{
  "excalidraw": {
    "command": "npx",
    "args": ["mcp-excalidraw-server"],
    "env": {},
    "disabled": false
  }
}
```

**Note**: NPX method may have slower startup times and is not recommended for production use.

#### Method 2: Global Installation

```bash
# Install globally
npm install -g mcp-excalidraw-server

# Update configuration to use global installation
{
  "excalidraw": {
    "command": "mcp-excalidraw-server",
    "args": [],
    "env": {},
    "disabled": false
  }
}
```

### Troubleshooting Excalidraw MCP Installation

#### Common Issues and Solutions

1. **Module Not Found Error**

   ```bash
   Error: Cannot find module 'mcp-excalidraw-server'
   ```

   **Solution**: Ensure the package is installed locally:
   ```bash
   npm install mcp-excalidraw-server
   ls node_modules/mcp-excalidraw-server/
   ```

2. **Permission Denied**

   ```bash
   Error: EACCES: permission denied
   ```

   **Solution**: Check file permissions:
   ```bash
   chmod +x node_modules/mcp-excalidraw-server/src/index.js
   ```

3. **Node.js Version Compatibility**

   ```bash
   Error: Unsupported Node.js version
   ```

   **Solution**: Ensure Node.js v16+ is installed:
   ```bash
   node --version  # Should be v16.0.0 or higher
   ```

4. **MCP Server Not Responding**

   **Solution**: Check the server configuration and restart Kiro IDE:
   ```bash
   # Verify configuration
   cat .kiro/settings/mcp.json | jq '.mcpServers.excalidraw'
   
   # Test server manually
   node node_modules/mcp-excalidraw-server/src/index.js --help
   ```

## Well-Architected Framework Assessment

### Automated Assessment

Run the comprehensive Well-Architected Framework assessment:

```bash
cd infrastructure
npm run well-architected:assessment
```

This assessment evaluates your architecture across all six pillars:

1. **Operational Excellence** (Score: /100)
   - CDK automation implementation
   - Monitoring and observability
   - CI/CD pipeline maturity
   - Disaster recovery automation
   - Documentation completeness

2. **Security** (Score: /100)
   - IAM least privilege implementation
   - Encryption at rest and in transit
   - Network security controls
   - Secrets management
   - Security monitoring and alerting

3. **Reliability** (Score: /100)
   - Multi-AZ deployment strategy
   - Auto-scaling implementation
   - Backup and recovery procedures
   - Health checks and monitoring
   - Disaster recovery capabilities

4. **Performance Efficiency** (Score: /100)
   - ARM64 Graviton3 usage
   - Caching strategy implementation
   - Database optimization
   - Performance monitoring
   - Resource right-sizing

5. **Cost Optimization** (Score: /100)
   - Resource tagging strategy
   - Cost monitoring and alerting
   - Data lifecycle management
   - Spot instance utilization
   - Reserved capacity planning

6. **Sustainability** (Score: /100)
   - Energy-efficient compute (Graviton)
   - Auto-scaling for resource optimization
   - Serverless adoption
   - Data optimization strategies
   - Renewable energy region usage

### Assessment Reports

The assessment generates three types of reports:

1. **Detailed Report**: `infrastructure/docs/well-architected-assessment.md`
   - Comprehensive analysis of all pillars
   - Best practices implemented
   - Risks identified
   - Detailed recommendations

2. **JSON Summary**: `infrastructure/docs/well-architected-summary.json`
   - Machine-readable assessment results
   - Suitable for automation and tracking

3. **Executive Summary**: `infrastructure/docs/executive-summary.md`
   - High-level overview for stakeholders
   - Key metrics and priorities
   - Action items with timelines

## MCP Tools Usage

### Testing MCP Integration

Verify that all MCP tools are properly configured:

```bash
cd infrastructure
npm run mcp:test
```

This test validates:

- MCP configuration file structure
- Required server configurations
- Auto-approval permissions
- AWS credentials setup

### Architecture Assessment with MCP

Run automated architecture assessment using MCP tools:

```bash
cd infrastructure
npm run architecture:assess
```

This assessment includes:

- CDK project analysis
- Cost optimization analysis using AWS Pricing MCP
- Security assessment using AWS IAM MCP
- Performance analysis
- Documentation analysis using AWS Docs MCP

### Continuous Improvement Monitoring

Monitor architecture improvements over time:

```bash
cd infrastructure
npm run monitoring:continuous
```

This monitoring system:

- Tracks metrics trends over time
- Generates improvement recommendations
- Triggers alerts for threshold violations
- Maintains historical performance data

## Using MCP Tools in Kiro IDE

### AWS Documentation MCP

Access AWS documentation directly in Kiro:

```
Ask Kiro: "Search AWS documentation for EKS best practices"
```

The AWS Documentation MCP will:

- Search official AWS documentation
- Provide relevant excerpts and links
- Offer implementation guidance

### AWS CDK MCP

Get CDK-specific guidance:

```
Ask Kiro: "Explain CDK Nag rule AwsSolutions-IAM4"
Ask Kiro: "Check my CDK code for Nag suppressions"
Ask Kiro: "Provide CDK best practices for security"
```

### AWS Pricing MCP

Analyze costs and optimize spending:

```
Ask Kiro: "Analyze my CDK project costs"
Ask Kiro: "Generate a cost report for my infrastructure"
Ask Kiro: "What are the pricing options for EKS in ap-northeast-1?"
```

### AWS IAM MCP

Review IAM policies and permissions:

```
Ask Kiro: "List all IAM roles in my account"
Ask Kiro: "Analyze the permissions for my EKS service role"
Ask Kiro: "Check for overprivileged IAM policies"
```

### Excalidraw MCP

Create diagrams and visualizations directly in Kiro:

```
Ask Kiro: "Create a simple flowchart showing the order processing workflow"
Ask Kiro: "Draw a system architecture diagram with API Gateway, Lambda, and DynamoDB"
Ask Kiro: "Create a rectangle with text 'Database' and connect it to another rectangle labeled 'API'"
```

#### Excalidraw MCP Features

**Basic Element Creation**:
- Rectangles, ellipses, diamonds, arrows, text, lines
- Customizable colors, stroke width, opacity
- Text elements with font size and alignment options

**Advanced Operations**:
- Batch creation for complex diagrams
- Element grouping and ungrouping
- Alignment and distribution tools
- Element locking and unlocking
- Query and filter existing elements

**Example Usage**:

```
# Create a simple system architecture
Ask Kiro: "Create a batch of elements for a microservices architecture:
- API Gateway (rectangle, blue)
- User Service (rectangle, green) 
- Order Service (rectangle, green)
- Database (ellipse, purple)
- Connect them with arrows"

# Create a flowchart
Ask Kiro: "Create a decision flowchart:
- Start (green rectangle)
- Decision diamond (yellow)
- Process steps (blue rectangles)
- End (red rectangle)
- Connect with arrows"
```

**Generated Output**:
- Standard Excalidraw JSON format
- Can be copied and pasted into Excalidraw web app
- Supports all Excalidraw features and styling

### Time MCP

Handle time zone conversions and formatting:

```
Ask Kiro: "What time is it in Tokyo?"
Ask Kiro: "Convert 2:00 PM EST to Taiwan time"
Ask Kiro: "Calculate the time difference between New York and London"
```

## Best Practices for MCP Usage

### 1. Regular Assessments

- **Weekly**: Quick MCP integration tests
- **Monthly**: Full architecture assessment
- **Quarterly**: Comprehensive Well-Architected review

### 2. Continuous Monitoring

- Set up automated monitoring with alerts
- Track improvement trends over time
- Review recommendations regularly

### 3. Documentation Maintenance

- Keep architecture decisions updated
- Document MCP tool usage patterns
- Maintain assessment history

### 4. Security Considerations

- Use readonly mode for IAM MCP
- Regularly rotate AWS credentials
- Monitor MCP tool access logs

## Troubleshooting

### Common Issues

1. **MCP Server Not Found**

   ```bash
   # Ensure UV is installed and updated
   uv --version
   
   # Clear UV cache if needed
   uv cache clean
   ```

2. **AWS Credentials Issues**

   ```bash
   # Verify AWS credentials
   aws sts get-caller-identity --profile kim-sso
   
   # Check environment variables
   echo $AWS_PROFILE
   echo $AWS_REGION
   ```

3. **Permission Denied Errors**

   ```bash
   # Check IAM permissions for the configured profile
   aws iam get-user --profile kim-sso
   ```

4. **Excalidraw MCP Server Issues**

   ```bash
   # Check if the server is properly installed
   ls node_modules/mcp-excalidraw-server/src/index.js
   
   # Test the server manually with absolute path
   echo '{"jsonrpc": "2.0", "id": 1, "method": "tools-and-environment/list", "params": {}}' | \
     node /ABSOLUTE/PATH/TO/PROJECT/node_modules/mcp-excalidraw-server/src/index.js
   
   # Verify Node.js version (requires v16+)
   node --version
   
   # Get current directory for absolute path
   pwd
   ```

5. **Path Resolution Issues**

   If you see errors like `Cannot find module '/node_modules/mcp-excalidraw-server/src/index.js'`:

   ```bash
   # Get your project's absolute path
   cd /path/to/genai-demo
   pwd
   
   # Update .kiro/settings/mcp.json with the absolute path
   # Replace "node_modules/..." with "/full/path/to/project/node_modules/..."
   ```

5. **Time MCP Server Issues**

   ```bash
   # Test time MCP server
   uvx mcp-server-time --help
   
   # Check if UV can access the package
   uv tool list | grep mcp-server-time
   ```

### Debug Mode

Enable debug logging for MCP tools:

```json
{
  "mcpServers": {
    "aws-docs": {
      "env": {
        "FASTMCP_LOG_LEVEL": "DEBUG"
      }
    }
  }
}
```

## Integration with CI/CD

### GitHub Actions Integration

Add MCP assessments to your CI/CD pipeline:

```yaml
# .github/workflows/architecture-assessment.yml
name: Architecture Assessment

on:
  schedule:
    - cron: '0 0 * * 0'  # Weekly on Sunday
  workflow_dispatch:

jobs:
  assess:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '18'
      - name: Install dependencies
        run: |
          cd infrastructure
          npm install
      - name: Run Well-Architected Assessment
        run: |
          cd infrastructure
          npm run well-architected:assessment
      - name: Upload Assessment Reports
        uses: actions/upload-artifact@v3
        with:
          name: architecture-assessment
          path: infrastructure/docs/
```

### Automated Alerts

Set up automated alerts for assessment results:

```bash
# Example: Send Slack notification for high-priority findings
if [ $(jq '.actionItems | map(select(.priority == "HIGH")) | length' infrastructure/docs/well-architected-summary.json) -gt 0 ]; then
  curl -X POST -H 'Content-type: application/json' \
    --data '{"text":"High priority architecture issues found. Check the assessment report."}' \
    $SLACK_WEBHOOK_URL
fi
```

## Metrics and KPIs

### Key Performance Indicators

Track these KPIs using MCP assessments:

| KPI | Target | Current | Trend |
|-----|--------|---------|-------|
| Overall Well-Architected Score | ≥85% | TBD | 📈 |
| Security Score | ≥90% | TBD | 📈 |
| Cost Optimization Score | ≥80% | TBD | 📈 |
| Performance Score | ≥85% | TBD | 📈 |
| Reliability Score | ≥90% | TBD | 📈 |
| Operational Excellence Score | ≥80% | TBD | 📈 |
| Sustainability Score | ≥70% | TBD | 📈 |

### Improvement Tracking

Use the continuous monitoring system to track improvements:

```bash
# Generate monthly improvement report
npm run monitoring:continuous

# View historical trends
cat infrastructure/docs/metrics-history.json | jq '.[] | {date: .timestamp, score: .overallScore}'
```

## Advanced Usage

### Custom Assessment Criteria

Extend the assessment framework with custom criteria:

```javascript
// infrastructure/scripts/custom-assessment.js
class CustomAssessment extends WellArchitectedAssessment {
  async assessCustomCriteria() {
    // Add your custom assessment logic
    const customScore = this.evaluateCustomRequirements();
    this.assessmentResults.pillars.custom = {
      score: customScore,
      findings: ['Custom assessment completed'],
      recommendations: ['Implement custom improvements']
    };
  }
}
```

### Integration with External Tools

Integrate MCP assessments with external monitoring tools:

```bash
# Export metrics to Prometheus
curl -X POST http://prometheus-pushgateway:9091/metrics/job/architecture-assessment \
  -d "well_architected_score $(jq '.overallScore' infrastructure/docs/well-architected-summary.json)"
```

## Support and Resources

### Documentation Links

- AWS Well-Architected Framework
- AWS CDK Best Practices
- MCP Protocol Specification

### Internal Resources

- Architecture Decision Records: `docs/architecture/adr/`
- Deployment Documentation: `docs/deployment/`
- Troubleshooting Guide: `infrastructure/TROUBLESHOOTING.md`

### Getting Help

1. Check the troubleshooting section above
2. Review assessment reports for specific guidance
3. Consult AWS documentation using the MCP tools
4. Reach out to the DevOps team for assistance

---

*This guide is maintained as part of the GenAI Demo project's continuous improvement process.*
