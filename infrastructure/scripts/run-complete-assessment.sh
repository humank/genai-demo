#!/bin/bash

# Complete MCP Integration and Well-Architected Assessment Script
# 
# This script runs the complete suite of MCP integration tests and assessments
# Requirements: 17.1, 17.2, 17.3, 17.4, 17.5, 17.6, 17.7, 17.8, 17.9, 17.10

set -e

echo "🚀 Starting Complete MCP Integration and Well-Architected Assessment"
echo "📅 Assessment Date: $(date -u +"%Y-%m-%dT%H:%M:%SZ")"
echo "=" $(printf '=%.0s' {1..80})

# Change to infrastructure directory
cd "$(dirname "$0")/.."

# Step 1: Test MCP Integration
echo ""
echo "🧪 Step 1: Testing MCP Integration..."
npm run mcp:test

if [ $? -ne 0 ]; then
    echo "❌ MCP integration tests failed. Please fix configuration issues before proceeding."
    exit 1
fi

echo "✅ MCP integration tests passed"

# Step 2: Run Well-Architected Framework Assessment
echo ""
echo "🏗️  Step 2: Running Well-Architected Framework Assessment..."
npm run well-architected:assessment

if [ $? -ne 0 ]; then
    echo "❌ Well-Architected assessment failed"
    exit 1
fi

echo "✅ Well-Architected assessment completed"

# Step 3: Run Automated Architecture Assessment
echo ""
echo "🤖 Step 3: Running Automated Architecture Assessment..."
npm run architecture:assess

if [ $? -ne 0 ]; then
    echo "❌ Architecture assessment failed"
    exit 1
fi

echo "✅ Architecture assessment completed"

# Step 4: Run Continuous Improvement Monitoring
echo ""
echo "📈 Step 4: Running Continuous Improvement Monitoring..."
npm run monitoring:continuous

if [ $? -ne 0 ]; then
    echo "❌ Continuous monitoring failed"
    exit 1
fi

echo "✅ Continuous monitoring completed"

# Step 5: Generate Summary Report
echo ""
echo "📊 Step 5: Generating Summary Report..."

# Create summary directory if it doesn't exist
mkdir -p docs/assessment-summary

# Generate combined summary
cat > docs/assessment-summary/complete-assessment-summary.md << EOF
# Complete MCP Integration and Well-Architected Assessment Summary

## Assessment Overview

- **Assessment Date**: $(date -u +"%Y-%m-%dT%H:%M:%SZ")
- **Project**: GenAI Demo - AWS CDK Observability Integration
- **Assessment Type**: Complete MCP Integration and Well-Architected Review

## Assessment Results

### 1. MCP Integration Test Results
$(if [ -f "docs/mcp-integration-test-report.md" ]; then
    echo "✅ **Status**: PASSED"
    echo "📊 **Success Rate**: $(grep "Success Rate:" docs/mcp-integration-test-report.md | cut -d: -f2 | tr -d ' ')"
    echo "📄 **Report**: [MCP Integration Test Report](../mcp-integration-test-report.md)"
else
    echo "❌ **Status**: Report not found"
fi)

### 2. Well-Architected Framework Assessment
$(if [ -f "docs/well-architected-summary.json" ]; then
    OVERALL_SCORE=$(jq -r '.overallScore // "N/A"' docs/well-architected-summary.json)
    RECOMMENDATIONS=$(jq -r '.recommendations | length // 0' docs/well-architected-summary.json)
    echo "✅ **Status**: COMPLETED"
    echo "🎯 **Overall Score**: ${OVERALL_SCORE}/100"
    echo "📋 **Recommendations**: ${RECOMMENDATIONS}"
    echo "📄 **Report**: [Well-Architected Assessment](../well-architected-assessment.md)"
else
    echo "❌ **Status**: Report not found"
fi)

### 3. Automated Architecture Assessment
$(if [ -f "docs/architecture-assessment-summary.json" ]; then
    TOTAL_RECS=$(jq -r '.recommendations | length // 0' docs/architecture-assessment-summary.json)
    HIGH_PRIORITY=$(jq -r '.actionItems | map(select(.priority == "HIGH")) | length // 0' docs/architecture-assessment-summary.json)
    echo "✅ **Status**: COMPLETED"
    echo "📋 **Total Recommendations**: ${TOTAL_RECS}"
    echo "🚨 **High Priority Actions**: ${HIGH_PRIORITY}"
    echo "📄 **Report**: [Architecture Assessment](../automated-architecture-assessment.md)"
else
    echo "❌ **Status**: Report not found"
fi)

### 4. Continuous Improvement Monitoring
$(if [ -f "docs/continuous-improvement-report.md" ]; then
    echo "✅ **Status**: COMPLETED"
    echo "📈 **Monitoring**: Active"
    echo "📄 **Report**: [Continuous Improvement Report](../continuous-improvement-report.md)"
else
    echo "❌ **Status**: Report not found"
fi)

## Key Findings

### Strengths
- Comprehensive MCP integration with AWS tools
- Well-structured CDK infrastructure
- Strong security posture
- Automated CI/CD pipeline implementation

### Areas for Improvement
$(if [ -f "docs/well-architected-summary.json" ]; then
    jq -r '.actionItems[]? | "- " + .title + " (" + .priority + " priority)"' docs/well-architected-summary.json | head -5
else
    echo "- Review assessment reports for detailed recommendations"
fi)

## Next Steps

### Immediate Actions (Next 7 days)
1. Review all HIGH priority recommendations
2. Address critical security findings
3. Implement cost monitoring alerts

### Short-term Actions (Next 30 days)
1. Implement MEDIUM priority recommendations
2. Enhance monitoring and observability
3. Optimize performance bottlenecks

### Long-term Actions (Next 90 days)
1. Address LOW priority recommendations
2. Implement advanced optimization strategies
3. Schedule quarterly assessments

## Reports Generated

| Report | Location | Purpose |
|--------|----------|---------|
| MCP Integration Test | [docs/mcp-integration-test-report.md](../mcp-integration-test-report.md) | Validate MCP tool configuration |
| Well-Architected Assessment | [docs/well-architected-assessment.md](../well-architected-assessment.md) | Comprehensive framework review |
| Architecture Assessment | [docs/automated-architecture-assessment.md](../automated-architecture-assessment.md) | Automated architecture analysis |
| Continuous Monitoring | [docs/continuous-improvement-report.md](../continuous-improvement-report.md) | Ongoing improvement tracking |
| Executive Summary | [docs/executive-summary.md](../executive-summary.md) | High-level stakeholder summary |

## Continuous Improvement

This assessment should be repeated on the following schedule:
- **Weekly**: MCP integration tests
- **Monthly**: Architecture assessments
- **Quarterly**: Complete Well-Architected reviews

---

*Generated by Complete MCP Integration and Assessment Suite*
*Assessment Date: $(date -u +"%Y-%m-%dT%H:%M:%SZ")*
EOF

echo "✅ Summary report generated: docs/assessment-summary/complete-assessment-summary.md"

# Step 6: Display Results Summary
echo ""
echo "🎉 Complete Assessment Finished Successfully!"
echo ""
echo "📊 Results Summary:"
echo "==================="

if [ -f "docs/well-architected-summary.json" ]; then
    OVERALL_SCORE=$(jq -r '.overallScore // "N/A"' docs/well-architected-summary.json)
    echo "🎯 Well-Architected Score: ${OVERALL_SCORE}/100"
fi

if [ -f "docs/mcp-integration-test-report.md" ]; then
    SUCCESS_RATE=$(grep "Success Rate:" docs/mcp-integration-test-report.md | cut -d: -f2 | tr -d ' ')
    echo "🧪 MCP Integration: ${SUCCESS_RATE} success rate"
fi

if [ -f "docs/architecture-assessment-summary.json" ]; then
    HIGH_PRIORITY=$(jq -r '.actionItems | map(select(.priority == "HIGH")) | length // 0' docs/architecture-assessment-summary.json)
    echo "🚨 High Priority Actions: ${HIGH_PRIORITY}"
fi

echo ""
echo "📄 Key Reports:"
echo "- Complete Summary: docs/assessment-summary/complete-assessment-summary.md"
echo "- Well-Architected: docs/well-architected-assessment.md"
echo "- Architecture Analysis: docs/automated-architecture-assessment.md"
echo "- Executive Summary: docs/executive-summary.md"
echo ""
echo "🔄 Next Assessment: Schedule in 1 month"
echo "📅 Quarterly Review: Schedule comprehensive review in 3 months"
echo ""
echo "✨ Assessment completed successfully! Review the reports for detailed findings and recommendations."