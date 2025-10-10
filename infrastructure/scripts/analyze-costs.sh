#!/bin/bash

# ============================================
# AWS Cost Analysis Script
# ============================================
# Purpose: Automated cost analysis using AWS CLI and MCP Tools
# Architecture: AWS Native (No Lambda)
# Integration: GitHub Actions + MCP AWS Pricing
# ============================================

set -e

# Configuration
REPORT_DATE=$(date +%Y-%m-%d)
REPORT_DIR="reports/cost-analysis"
TAIWAN_REGION="ap-northeast-1"
JAPAN_REGION="ap-northeast-1"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ============================================
# Helper Functions
# ============================================

log_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
}

# ============================================
# Setup
# ============================================

log_info "Starting Cost Analysis for $REPORT_DATE..."
mkdir -p "$REPORT_DIR"

# ============================================
# 1. Fetch Cost Data from AWS Cost Explorer
# ============================================

log_info "📈 Fetching cost data from AWS Cost Explorer..."

# Get 30-day cost data
aws ce get-cost-and-usage \
  --time-period Start=$(date -d '30 days ago' +%Y-%m-%d),End=$(date +%Y-%m-%d) \
  --granularity DAILY \
  --metrics UnblendedCost \
  --group-by Type=DIMENSION,Key=SERVICE \
  --group-by Type=DIMENSION,Key=REGION \
  --output json > "$REPORT_DIR/cost-data-30days.json"

log_success "30-day cost data retrieved"

# ============================================
# 2. Get Cost Forecast
# ============================================

log_info "🔮 Fetching cost forecast..."

aws ce get-cost-forecast \
  --time-period Start=$(date +%Y-%m-%d),End=$(date -d '30 days' +%Y-%m-%d) \
  --metric UNBLENDED_COST \
  --granularity MONTHLY \
  --output json > "$REPORT_DIR/cost-forecast.json"

log_success "Cost forecast retrieved"

# ============================================
# 3. Taiwan Region Cost Analysis
# ============================================

log_info "🇹🇼 Analyzing Taiwan region costs..."

aws ce get-cost-and-usage \
  --time-period Start=$(date -d '7 days ago' +%Y-%m-%d),End=$(date +%Y-%m-%d) \
  --granularity DAILY \
  --metrics UnblendedCost \
  --filter "{\"Dimensions\":{\"Key\":\"REGION\",\"Values\":[\"$TAIWAN_REGION\"]}}" \
  --group-by Type=DIMENSION,Key=SERVICE \
  --output json > "$REPORT_DIR/taiwan-region-costs.json"

log_success "Taiwan region cost data retrieved"

# ============================================
# 4. Japan Region Cost Analysis
# ============================================

log_info "🇯🇵 Analyzing Japan region costs..."

aws ce get-cost-and-usage \
  --time-period Start=$(date -d '7 days ago' +%Y-%m-%d),End=$(date +%Y-%m-%d) \
  --granularity DAILY \
  --metrics UnblendedCost \
  --filter "{\"Dimensions\":{\"Key\":\"REGION\",\"Values\":[\"$JAPAN_REGION\"]}}" \
  --group-by Type=DIMENSION,Key=SERVICE \
  --output json > "$REPORT_DIR/japan-region-costs.json"

log_success "Japan region cost data retrieved"

# ============================================
# 5. Service-Level Cost Breakdown
# ============================================

log_info "📊 Analyzing service-level costs..."

# EKS
aws ce get-cost-and-usage \
  --time-period Start=$(date -d '7 days ago' +%Y-%m-%d),End=$(date +%Y-%m-%d) \
  --granularity DAILY \
  --metrics UnblendedCost \
  --filter "{\"Dimensions\":{\"Key\":\"SERVICE\",\"Values\":[\"Amazon Elastic Kubernetes Service\"]}}" \
  --output json > "$REPORT_DIR/eks-costs.json"

# RDS
aws ce get-cost-and-usage \
  --time-period Start=$(date -d '7 days ago' +%Y-%m-%d),End=$(date +%Y-%m-%d) \
  --granularity DAILY \
  --metrics UnblendedCost \
  --filter "{\"Dimensions\":{\"Key\":\"SERVICE\",\"Values\":[\"Amazon Relational Database Service\"]}}" \
  --output json > "$REPORT_DIR/rds-costs.json"

# ElastiCache
aws ce get-cost-and-usage \
  --time-period Start=$(date -d '7 days ago' +%Y-%m-%d),End=$(date +%Y-%m-%d) \
  --granularity DAILY \
  --metrics UnblendedCost \
  --filter "{\"Dimensions\":{\"Key\":\"SERVICE\",\"Values\":[\"Amazon ElastiCache\"]}}" \
  --output json > "$REPORT_DIR/elasticache-costs.json"

# MSK
aws ce get-cost-and-usage \
  --time-period Start=$(date -d '7 days ago' +%Y-%m-%d),End=$(date +%Y-%m-%d) \
  --granularity DAILY \
  --metrics UnblendedCost \
  --filter "{\"Dimensions\":{\"Key\":\"SERVICE\",\"Values\":[\"Amazon Managed Streaming for Apache Kafka\"]}}" \
  --output json > "$REPORT_DIR/msk-costs.json"

log_success "Service-level cost data retrieved"

# ============================================
# 6. Parse and Calculate Totals
# ============================================

log_info "💰 Calculating cost totals..."

# Calculate total cost from 30-day data
TOTAL_COST=$(jq '[.ResultsByTime[].Total.UnblendedCost.Amount | tonumber] | add' "$REPORT_DIR/cost-data-30days.json")
TOTAL_COST_FORMATTED=$(printf "%.2f" "$TOTAL_COST")

# Calculate Taiwan region cost
TAIWAN_COST=$(jq '[.ResultsByTime[].Total.UnblendedCost.Amount | tonumber] | add' "$REPORT_DIR/taiwan-region-costs.json")
TAIWAN_COST_FORMATTED=$(printf "%.2f" "$TAIWAN_COST")

# Calculate Japan region cost
JAPAN_COST=$(jq '[.ResultsByTime[].Total.UnblendedCost.Amount | tonumber] | add' "$REPORT_DIR/japan-region-costs.json")
JAPAN_COST_FORMATTED=$(printf "%.2f" "$JAPAN_COST")

# Calculate service costs
EKS_COST=$(jq '[.ResultsByTime[].Total.UnblendedCost.Amount | tonumber] | add' "$REPORT_DIR/eks-costs.json")
EKS_COST_FORMATTED=$(printf "%.2f" "$EKS_COST")

RDS_COST=$(jq '[.ResultsByTime[].Total.UnblendedCost.Amount | tonumber] | add' "$REPORT_DIR/rds-costs.json")
RDS_COST_FORMATTED=$(printf "%.2f" "$RDS_COST")

ELASTICACHE_COST=$(jq '[.ResultsByTime[].Total.UnblendedCost.Amount | tonumber] | add' "$REPORT_DIR/elasticache-costs.json")
ELASTICACHE_COST_FORMATTED=$(printf "%.2f" "$ELASTICACHE_COST")

MSK_COST=$(jq '[.ResultsByTime[].Total.UnblendedCost.Amount | tonumber] | add' "$REPORT_DIR/msk-costs.json")
MSK_COST_FORMATTED=$(printf "%.2f" "$MSK_COST")

# Get forecast
FORECAST_COST=$(jq '.Total.Amount | tonumber' "$REPORT_DIR/cost-forecast.json")
FORECAST_COST_FORMATTED=$(printf "%.2f" "$FORECAST_COST")

log_success "Cost calculations complete"

# ============================================
# 7. Generate Markdown Report
# ============================================

log_info "📝 Generating cost analysis report..."

cat > "$REPORT_DIR/cost-analysis-report-$REPORT_DATE.md" <<REPORT
# AWS Cost Analysis Report

**Report Date**: $REPORT_DATE  
**Analysis Period**: Last 30 days  
**Generated By**: AWS Native Cost Analysis (No Lambda)

---

## 📊 Executive Summary

| Metric | Amount (USD) |
|--------|--------------|
| **Total Cost (30 days)** | \$$TOTAL_COST_FORMATTED |
| **Taiwan Region Cost (7 days)** | \$$TAIWAN_COST_FORMATTED |
| **Japan DR Region Cost (7 days)** | \$$JAPAN_COST_FORMATTED |
| **Forecasted Cost (Next 30 days)** | \$$FORECAST_COST_FORMATTED |

---

## 🌏 Regional Cost Breakdown

### Taiwan Primary Region (ap-northeast-1)
- **7-Day Cost**: \$$TAIWAN_COST_FORMATTED
- **Monthly Projection**: \$$( echo "scale=2; $TAIWAN_COST * 4.3" | bc )
- **Budget Limit**: \$5,000/month
- **Budget Utilization**: $( echo "scale=1; ($TAIWAN_COST * 4.3 / 5000) * 100" | bc )%

### Japan DR Region (ap-northeast-1)
- **7-Day Cost**: \$$JAPAN_COST_FORMATTED
- **Monthly Projection**: \$$( echo "scale=2; $JAPAN_COST * 4.3" | bc )
- **Budget Limit**: \$3,000/month
- **Budget Utilization**: $( echo "scale=1; ($JAPAN_COST * 4.3 / 3000) * 100" | bc )%

---

## 🔧 Service-Level Cost Breakdown

| Service | 7-Day Cost | Monthly Projection | Budget Limit | Utilization |
|---------|------------|-------------------|--------------|-------------|
| **EKS** | \$$EKS_COST_FORMATTED | \$$( echo "scale=2; $EKS_COST * 4.3" | bc ) | \$2,000 | $( echo "scale=1; ($EKS_COST * 4.3 / 2000) * 100" | bc )% |
| **RDS (Aurora)** | \$$RDS_COST_FORMATTED | \$$( echo "scale=2; $RDS_COST * 4.3" | bc ) | \$1,500 | $( echo "scale=1; ($RDS_COST * 4.3 / 1500) * 100" | bc )% |
| **ElastiCache** | \$$ELASTICACHE_COST_FORMATTED | \$$( echo "scale=2; $ELASTICACHE_COST * 4.3" | bc ) | \$800 | $( echo "scale=1; ($ELASTICACHE_COST * 4.3 / 800) * 100" | bc )% |
| **MSK** | \$$MSK_COST_FORMATTED | \$$( echo "scale=2; $MSK_COST * 4.3" | bc ) | \$500 | $( echo "scale=1; ($MSK_COST * 4.3 / 500) * 100" | bc )% |

---

## 💡 Cost Optimization Recommendations

### High Priority
1. **EKS Optimization**
   - Consider using Spot Instances for non-critical workloads
   - Potential savings: ~30% (\$$( echo "scale=2; $EKS_COST * 4.3 * 0.3" | bc )/month)
   - Action: Review node group configuration and implement mixed instance policy

2. **RDS Reserved Instances**
   - Evaluate 1-year Reserved Instances for Aurora
   - Potential savings: ~40% (\$$( echo "scale=2; $RDS_COST * 4.3 * 0.4" | bc )/month)
   - Action: Analyze usage patterns and purchase RIs

### Medium Priority
3. **ElastiCache Right-Sizing**
   - Review cache node types and consider downsizing
   - Potential savings: ~25% (\$$( echo "scale=2; $ELASTICACHE_COST * 4.3 * 0.25" | bc )/month)
   - Action: Monitor cache utilization and adjust node types

4. **MSK Capacity Optimization**
   - Review broker instance types and storage
   - Potential savings: ~20% (\$$( echo "scale=2; $MSK_COST * 4.3 * 0.2" | bc )/month)
   - Action: Analyze throughput requirements and optimize configuration

---

## 🚨 Budget Alerts Status

### Active Alerts
- Taiwan Region: $( if (( \$(echo "$TAIWAN_COST * 4.3 > 4000" | bc -l) )); then echo "⚠️ Approaching 80% threshold"; else echo "✅ Within budget"; fi )
- Japan Region: $( if (( \$(echo "$JAPAN_COST * 4.3 > 2400" | bc -l) )); then echo "⚠️ Approaching 80% threshold"; else echo "✅ Within budget"; fi )
- EKS Service: $( if (( \$(echo "$EKS_COST * 4.3 > 1800" | bc -l) )); then echo "⚠️ Approaching 90% threshold"; else echo "✅ Within budget"; fi )
- RDS Service: $( if (( \$(echo "$RDS_COST * 4.3 > 1350" | bc -l) )); then echo "⚠️ Approaching 90% threshold"; else echo "✅ Within budget"; fi )

---

## 📈 Trend Analysis

### Cost Trend (Last 30 Days)
- **Average Daily Cost**: \$$( echo "scale=2; $TOTAL_COST / 30" | bc )
- **Forecasted Monthly Cost**: \$$FORECAST_COST_FORMATTED
- **Trend**: $( if (( \$(echo "$FORECAST_COST > $TOTAL_COST" | bc -l) )); then echo "📈 Increasing"; else echo "📉 Decreasing"; fi )

---

## 🔗 Resources

- [AWS Cost Explorer](https://console.aws.amazon.com/cost-management/home#/cost-explorer)
- [AWS Budgets](https://console.aws.amazon.com/billing/home#/budgets)
- [Cost Anomaly Detection](https://console.aws.amazon.com/cost-management/home#/anomaly-detection)
- [CloudWatch Cost Dashboard](https://console.aws.amazon.com/cloudwatch/home#dashboards:name=GenAIDemo-Cost-Management)

---

## 📅 Next Steps

1. Review high-priority optimization recommendations
2. Monitor budget utilization trends
3. Implement cost allocation tags for better tracking
4. Schedule monthly cost review meeting
5. Update cost forecasts based on upcoming changes

---

**Next Review**: $(date -d '7 days' +%Y-%m-%d)  
**Report Generated**: $(date '+%Y-%m-%d %H:%M:%S %Z')  
**Tool**: AWS CLI + MCP AWS Pricing Tools
REPORT

log_success "Cost analysis report generated: $REPORT_DIR/cost-analysis-report-$REPORT_DATE.md"

# ============================================
# 8. Summary Output
# ============================================

echo ""
echo "=========================================="
echo "Cost Analysis Complete!"
echo "=========================================="
echo "Total Cost (30 days): \$$TOTAL_COST_FORMATTED"
echo "Taiwan Region (7 days): \$$TAIWAN_COST_FORMATTED"
echo "Japan Region (7 days): \$$JAPAN_COST_FORMATTED"
echo "Forecast (30 days): \$$FORECAST_COST_FORMATTED"
echo "=========================================="
echo "Report saved to: $REPORT_DIR/cost-analysis-report-$REPORT_DATE.md"
echo "=========================================="
