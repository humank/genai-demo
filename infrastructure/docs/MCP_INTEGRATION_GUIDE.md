# MCP Integration and Well-Architected Framework Guide

## Overview

This guide provides comprehensive instructions for using AWS MCP (Model Context Protocol) tools to perform Well-Architected Framework reviews and continuous architecture improvements for the GenAI Demo project.

## MCP Tools Configuration

### Configured AWS MCP Servers

The following AWS MCP servers are configured in `.kiro/settings/mcp.json`:

| Server | Purpose | Auto-Approved Tools |
|--------|---------|-------------------|
| `aws-docs` | AWS Documentation access | `search_documentation`, `read_documentation` |
| `aws-cdk` | CDK guidance and analysis | `CDKGeneralGuidance`, `ExplainCDKNagRule`, `CheckCDKNagSuppressions` |
| `aws-pricing` | Cost analysis and optimization | `analyze_cdk_project`, `get_pricing`, `generate_cost_report` |
| `aws-iam` | IAM policy analysis | `list_users`, `list_roles`, `get_user_policies`, `get_role_policies` |
| `aws-core` | Well-Architected reviews | `prompt_understanding`, `well_architected_review` |

### Prerequisites

1. **UV Package Manager**: Install UV for Python package management

   ```bash
   # macOS
   brew install uv
   
   # Or using pip
   pip install uv
   ```

2. **AWS Credentials**: Configure AWS credentials for IAM and pricing tools

   ```bash
   aws configure --profile kim-sso
   export AWS_PROFILE=kim-sso
   export AWS_REGION=ap-northeast-1
   ```

3. **Kiro IDE**: Ensure Kiro IDE is properly configured with MCP support

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

- [AWS Well-Architected Framework](https://aws.amazon.com/architecture/well-architected/)
- [AWS CDK Best Practices](https://docs.aws.amazon.com/cdk/v2/guide/best-practices.html)
- [MCP Protocol Specification](https://modelcontextprotocol.io/)

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
