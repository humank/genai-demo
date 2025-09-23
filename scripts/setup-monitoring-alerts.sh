#!/bin/bash

# 監控和警報配置腳本
# 用於設置 CloudWatch 警報和監控配置

set -e

ENVIRONMENT=${1:-production}
PROJECT_NAME="genai-demo"
AWS_REGION=${AWS_REGION:-us-east-1}

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 檢查 AWS CLI 配置
check_aws_config() {
    log_info "檢查 AWS CLI 配置..."
    
    if ! aws sts get-caller-identity &>/dev/null; then
        log_error "AWS CLI 未配置或憑證無效"
        log_info "請執行 'aws configure' 設置憑證"
        exit 1
    fi
    
    local account_id
    account_id=$(aws sts get-caller-identity --query Account --output text)
    log_success "AWS 帳戶: $account_id"
}

# 創建 SNS 主題用於警報通知
create_sns_topic() {
    log_info "創建 SNS 主題用於警報通知..."
    
    local topic_name="${PROJECT_NAME}-${ENVIRONMENT}-alerts"
    local topic_arn
    
    # 檢查主題是否已存在
    topic_arn=$(aws sns list-topics --query "Topics[?contains(TopicArn, '$topic_name')].TopicArn" --output text)
    
    if [ -z "$topic_arn" ]; then
        topic_arn=$(aws sns create-topic --name "$topic_name" --query TopicArn --output text)
        log_success "SNS 主題已創建: $topic_arn"
    else
        log_info "SNS 主題已存在: $topic_arn"
    fi
    
    echo "$topic_arn"
}

# 創建 CloudWatch 警報
create_cloudwatch_alarms() {
    local sns_topic_arn=$1
    log_info "創建 CloudWatch 警報..."
    
    local namespace="GenAI/Demo/$ENVIRONMENT"
    local alarm_prefix="${PROJECT_NAME}-${ENVIRONMENT}"
    
    # 1. API 響應時間警報
    log_info "創建 API 響應時間警報..."
    aws cloudwatch put-metric-alarm \
        --alarm-name "${alarm_prefix}-high-api-response-time" \
        --alarm-description "API 響應時間過高" \
        --metric-name "http.server.requests" \
        --namespace "$namespace" \
        --statistic Average \
        --period 300 \
        --threshold 2000 \
        --comparison-operator GreaterThanThreshold \
        --evaluation-periods 2 \
        --alarm-actions "$sns_topic_arn" \
        --ok-actions "$sns_topic_arn" \
        --dimensions Name=uri,Value=/api/analytics/events \
        --treat-missing-data notBreaching
    
    # 2. 錯誤率警報
    log_info "創建錯誤率警報..."
    aws cloudwatch put-metric-alarm \
        --alarm-name "${alarm_prefix}-high-error-rate" \
        --alarm-description "API 錯誤率過高" \
        --metric-name "http.server.requests" \
        --namespace "$namespace" \
        --statistic Sum \
        --period 300 \
        --threshold 10 \
        --comparison-operator GreaterThanThreshold \
        --evaluation-periods 2 \
        --alarm-actions "$sns_topic_arn" \
        --dimensions Name=status,Value=5xx \
        --treat-missing-data notBreaching
    
    # 3. 事件處理失敗警報
    log_info "創建事件處理失敗警報..."
    aws cloudwatch put-metric-alarm \
        --alarm-name "${alarm_prefix}-event-processing-failures" \
        --alarm-description "可觀測性事件處理失敗" \
        --metric-name "observability.events.failed" \
        --namespace "$namespace" \
        --statistic Sum \
        --period 300 \
        --threshold 5 \
        --comparison-operator GreaterThanThreshold \
        --evaluation-periods 1 \
        --alarm-actions "$sns_topic_arn" \
        --treat-missing-data notBreaching
    
    # 4. JVM 記憶體使用率警報
    log_info "創建 JVM 記憶體使用率警報..."
    aws cloudwatch put-metric-alarm \
        --alarm-name "${alarm_prefix}-high-memory-usage" \
        --alarm-description "JVM 記憶體使用率過高" \
        --metric-name "jvm.memory.used" \
        --namespace "$namespace" \
        --statistic Average \
        --period 300 \
        --threshold 85 \
        --comparison-operator GreaterThanThreshold \
        --evaluation-periods 3 \
        --alarm-actions "$sns_topic_arn" \
        --dimensions Name=area,Value=heap \
        --treat-missing-data notBreaching
    
    # 5. Kafka 消費者延遲警報
    log_info "創建 Kafka 消費者延遲警報..."
    aws cloudwatch put-metric-alarm \
        --alarm-name "${alarm_prefix}-kafka-consumer-lag" \
        --alarm-description "Kafka 消費者延遲過高" \
        --metric-name "kafka.consumer.lag" \
        --namespace "$namespace" \
        --statistic Maximum \
        --period 300 \
        --threshold 1000 \
        --comparison-operator GreaterThanThreshold \
        --evaluation-periods 2 \
        --alarm-actions "$sns_topic_arn" \
        --treat-missing-data notBreaching
    
    # 6. 資料庫連接池警報
    log_info "創建資料庫連接池警報..."
    aws cloudwatch put-metric-alarm \
        --alarm-name "${alarm_prefix}-db-connection-pool-exhausted" \
        --alarm-description "資料庫連接池接近耗盡" \
        --metric-name "hikari.connections.active" \
        --namespace "$namespace" \
        --statistic Average \
        --period 300 \
        --threshold 18 \
        --comparison-operator GreaterThanThreshold \
        --evaluation-periods 2 \
        --alarm-actions "$sns_topic_arn" \
        --treat-missing-data notBreaching
    
    log_success "CloudWatch 警報創建完成"
}

# 創建 CloudWatch 儀表板
create_cloudwatch_dashboard() {
    log_info "創建 CloudWatch 儀表板..."
    
    local dashboard_name="${PROJECT_NAME}-${ENVIRONMENT}-observability"
    local namespace="GenAI/Demo/$ENVIRONMENT"
    
    # 儀表板 JSON 配置
    local dashboard_body=$(cat << EOF
{
    "widgets": [
        {
            "type": "metric",
            "x": 0,
            "y": 0,
            "width": 12,
            "height": 6,
            "properties": {
                "metrics": [
                    ["$namespace", "observability.events.received", {"stat": "Sum"}],
                    [".", "observability.events.processed", {"stat": "Sum"}],
                    [".", "observability.events.failed", {"stat": "Sum"}]
                ],
                "view": "timeSeries",
                "stacked": false,
                "region": "$AWS_REGION",
                "title": "可觀測性事件處理",
                "period": 300,
                "yAxis": {
                    "left": {
                        "min": 0
                    }
                }
            }
        },
        {
            "type": "metric",
            "x": 12,
            "y": 0,
            "width": 12,
            "height": 6,
            "properties": {
                "metrics": [
                    ["$namespace", "http.server.requests", "uri", "/api/analytics/events", {"stat": "Average"}],
                    ["...", "/api/analytics/performance", {"stat": "Average"}],
                    ["...", "/api/analytics/stats", {"stat": "Average"}]
                ],
                "view": "timeSeries",
                "stacked": false,
                "region": "$AWS_REGION",
                "title": "API 響應時間 (ms)",
                "period": 300,
                "yAxis": {
                    "left": {
                        "min": 0
                    }
                }
            }
        },
        {
            "type": "metric",
            "x": 0,
            "y": 6,
            "width": 8,
            "height": 6,
            "properties": {
                "metrics": [
                    ["$namespace", "jvm.memory.used", "area", "heap", {"stat": "Average"}],
                    [".", "jvm.memory.max", ".", ".", {"stat": "Average"}]
                ],
                "view": "timeSeries",
                "stacked": false,
                "region": "$AWS_REGION",
                "title": "JVM 記憶體使用",
                "period": 300,
                "yAxis": {
                    "left": {
                        "min": 0
                    }
                }
            }
        },
        {
            "type": "metric",
            "x": 8,
            "y": 6,
            "width": 8,
            "height": 6,
            "properties": {
                "metrics": [
                    ["$namespace", "hikari.connections.active", {"stat": "Average"}],
                    [".", "hikari.connections.idle", {"stat": "Average"}],
                    [".", "hikari.connections.pending", {"stat": "Average"}]
                ],
                "view": "timeSeries",
                "stacked": false,
                "region": "$AWS_REGION",
                "title": "資料庫連接池",
                "period": 300,
                "yAxis": {
                    "left": {
                        "min": 0
                    }
                }
            }
        },
        {
            "type": "metric",
            "x": 16,
            "y": 6,
            "width": 8,
            "height": 6,
            "properties": {
                "metrics": [
                    ["$namespace", "kafka.consumer.lag", {"stat": "Maximum"}],
                    [".", "kafka.consumer.records.consumed.rate", {"stat": "Average"}]
                ],
                "view": "timeSeries",
                "stacked": false,
                "region": "$AWS_REGION",
                "title": "Kafka 消費者指標",
                "period": 300,
                "yAxis": {
                    "left": {
                        "min": 0
                    }
                }
            }
        },
        {
            "type": "log",
            "x": 0,
            "y": 12,
            "width": 24,
            "height": 6,
            "properties": {
                "query": "SOURCE '/aws/eks/${PROJECT_NAME}-${ENVIRONMENT}/cluster' | fields @timestamp, @message\n| filter @message like /ERROR/\n| sort @timestamp desc\n| limit 100",
                "region": "$AWS_REGION",
                "title": "最近錯誤日誌",
                "view": "table"
            }
        }
    ]
}
EOF
)
    
    # 創建儀表板
    aws cloudwatch put-dashboard \
        --dashboard-name "$dashboard_name" \
        --dashboard-body "$dashboard_body"
    
    log_success "CloudWatch 儀表板已創建: $dashboard_name"
    log_info "儀表板 URL: https://$AWS_REGION.console.aws.amazon.com/cloudwatch/home?region=$AWS_REGION#dashboards:name=$dashboard_name"
}

# 設置 CloudWatch Logs 保留期
setup_log_retention() {
    log_info "設置 CloudWatch Logs 保留期..."
    
    local log_groups=(
        "/aws/eks/${PROJECT_NAME}-${ENVIRONMENT}/cluster"
        "/aws/lambda/${PROJECT_NAME}-${ENVIRONMENT}"
        "/aws/apigateway/${PROJECT_NAME}-${ENVIRONMENT}"
    )
    
    local retention_days=30
    if [ "$ENVIRONMENT" = "production" ]; then
        retention_days=90
    fi
    
    for log_group in "${log_groups[@]}"; do
        if aws logs describe-log-groups --log-group-name-prefix "$log_group" --query 'logGroups[0].logGroupName' --output text 2>/dev/null | grep -q "$log_group"; then
            aws logs put-retention-policy \
                --log-group-name "$log_group" \
                --retention-in-days "$retention_days" 2>/dev/null || true
            log_info "設置日誌保留期: $log_group ($retention_days 天)"
        fi
    done
}

# 創建自定義指標過濾器
create_metric_filters() {
    log_info "創建自定義指標過濾器..."
    
    local log_group="/aws/eks/${PROJECT_NAME}-${ENVIRONMENT}/cluster"
    local namespace="GenAI/Demo/$ENVIRONMENT"
    
    # 錯誤日誌指標過濾器
    aws logs put-metric-filter \
        --log-group-name "$log_group" \
        --filter-name "${PROJECT_NAME}-${ENVIRONMENT}-error-count" \
        --filter-pattern "[timestamp, request_id, level=\"ERROR\", ...]" \
        --metric-transformations \
            metricName=application.errors,metricNamespace="$namespace",metricValue=1,defaultValue=0 2>/dev/null || true
    
    # 可觀測性事件指標過濾器
    aws logs put-metric-filter \
        --log-group-name "$log_group" \
        --filter-name "${PROJECT_NAME}-${ENVIRONMENT}-observability-events" \
        --filter-pattern "[timestamp, request_id, level, logger, message=\"Received * analytics events\"]" \
        --metric-transformations \
            metricName=observability.events.log.received,metricNamespace="$namespace",metricValue=1,defaultValue=0 2>/dev/null || true
    
    log_success "指標過濾器創建完成"
}

# 設置 AWS X-Ray 追蹤
setup_xray_tracing() {
    log_info "設置 AWS X-Ray 追蹤..."
    
    # 檢查 X-Ray 服務是否可用
    if aws xray get-service-map --start-time "$(date -d '1 hour ago' -u +%Y-%m-%dT%H:%M:%SZ)" --end-time "$(date -u +%Y-%m-%dT%H:%M:%SZ)" &>/dev/null; then
        log_success "X-Ray 追蹤已啟用"
    else
        log_warning "X-Ray 追蹤可能未正確配置"
    fi
    
    # 創建 X-Ray 採樣規則
    local sampling_rule=$(cat << EOF
{
    "version": 2,
    "default": {
        "fixed_target": 1,
        "rate": 0.1
    },
    "rules": [
        {
            "description": "Observability API sampling",
            "service_name": "${PROJECT_NAME}-${ENVIRONMENT}",
            "http_method": "POST",
            "url_path": "/api/analytics/*",
            "fixed_target": 2,
            "rate": 0.5
        }
    ]
}
EOF
)
    
    echo "$sampling_rule" > /tmp/xray-sampling-rule.json
    
    # 更新採樣規則
    aws xray put-trace-segments --trace-segment-documents file:///tmp/xray-sampling-rule.json 2>/dev/null || true
    
    rm -f /tmp/xray-sampling-rule.json
}

# 驗證監控配置
verify_monitoring_setup() {
    log_info "驗證監控配置..."
    
    # 檢查警報
    local alarm_count
    alarm_count=$(aws cloudwatch describe-alarms --alarm-name-prefix "${PROJECT_NAME}-${ENVIRONMENT}" --query 'length(MetricAlarms)' --output text)
    
    if [ "$alarm_count" -gt "0" ]; then
        log_success "CloudWatch 警報已配置: $alarm_count 個"
    else
        log_warning "未找到 CloudWatch 警報"
    fi
    
    # 檢查儀表板
    local dashboard_count
    dashboard_count=$(aws cloudwatch list-dashboards --dashboard-name-prefix "${PROJECT_NAME}-${ENVIRONMENT}" --query 'length(DashboardEntries)' --output text)
    
    if [ "$dashboard_count" -gt "0" ]; then
        log_success "CloudWatch 儀表板已配置: $dashboard_count 個"
    else
        log_warning "未找到 CloudWatch 儀表板"
    fi
    
    # 檢查 SNS 主題
    local topic_count
    topic_count=$(aws sns list-topics --query "length(Topics[?contains(TopicArn, '${PROJECT_NAME}-${ENVIRONMENT}')])" --output text)
    
    if [ "$topic_count" -gt "0" ]; then
        log_success "SNS 通知主題已配置: $topic_count 個"
    else
        log_warning "未找到 SNS 通知主題"
    fi
}

# 生成監控配置報告
generate_monitoring_report() {
    log_info "生成監控配置報告..."
    
    local report_file="monitoring-setup-report-$ENVIRONMENT-$(date +%Y%m%d-%H%M%S).md"
    
    cat > "$report_file" << EOF
# 監控配置報告

**環境**: $ENVIRONMENT  
**配置時間**: $(date)  
**AWS 區域**: $AWS_REGION  

## 配置摘要

### CloudWatch 警報
$(aws cloudwatch describe-alarms --alarm-name-prefix "${PROJECT_NAME}-${ENVIRONMENT}" --query 'MetricAlarms[].{Name:AlarmName,State:StateValue,Reason:StateReason}' --output table)

### CloudWatch 儀表板
$(aws cloudwatch list-dashboards --dashboard-name-prefix "${PROJECT_NAME}-${ENVIRONMENT}" --query 'DashboardEntries[].{Name:DashboardName,LastModified:LastModified}' --output table)

### SNS 主題
$(aws sns list-topics --query "Topics[?contains(TopicArn, '${PROJECT_NAME}-${ENVIRONMENT}')]" --output table)

## 存取連結

- **CloudWatch 控制台**: https://$AWS_REGION.console.aws.amazon.com/cloudwatch/home?region=$AWS_REGION
- **儀表板**: https://$AWS_REGION.console.aws.amazon.com/cloudwatch/home?region=$AWS_REGION#dashboards:name=${PROJECT_NAME}-${ENVIRONMENT}-observability
- **警報**: https://$AWS_REGION.console.aws.amazon.com/cloudwatch/home?region=$AWS_REGION#alarmsV2:

## 建議

1. **測試警報**: 建議觸發測試警報以驗證通知機制
2. **調整閾值**: 根據實際使用情況調整警報閾值
3. **添加訂閱**: 為 SNS 主題添加 Email 或 Slack 訂閱
4. **定期檢查**: 定期檢查和更新監控配置

---
*此報告由監控配置腳本自動生成*
EOF

    log_success "監控配置報告已生成: $report_file"
}

# 主函數
main() {
    echo "======================================"
    echo "  監控和警報配置腳本"
    echo "======================================"
    echo "環境: $ENVIRONMENT"
    echo "專案: $PROJECT_NAME"
    echo "AWS 區域: $AWS_REGION"
    echo "======================================"
    echo
    
    check_aws_config
    
    local sns_topic_arn
    sns_topic_arn=$(create_sns_topic)
    
    create_cloudwatch_alarms "$sns_topic_arn"
    create_cloudwatch_dashboard
    setup_log_retention
    create_metric_filters
    setup_xray_tracing
    verify_monitoring_setup
    generate_monitoring_report
    
    echo
    echo "======================================"
    log_success "監控和警報配置完成！"
    echo "======================================"
    echo
    log_info "下一步："
    echo "1. 為 SNS 主題添加 Email 訂閱："
    echo "   aws sns subscribe --topic-arn $sns_topic_arn --protocol email --notification-endpoint your-email@example.com"
    echo
    echo "2. 訪問 CloudWatch 儀表板："
    echo "   https://$AWS_REGION.console.aws.amazon.com/cloudwatch/home?region=$AWS_REGION#dashboards:name=${PROJECT_NAME}-${ENVIRONMENT}-observability"
    echo
    echo "3. 測試警報配置："
    echo "   ./scripts/test-monitoring-alerts.sh $ENVIRONMENT"
}

# 顯示使用說明
show_usage() {
    echo "使用方法: $0 [environment]"
    echo
    echo "支援的環境:"
    echo "  production  - 生產環境 (預設)"
    echo "  staging     - 預發布環境"
    echo "  test        - 測試環境"
    echo
    echo "環境變數:"
    echo "  AWS_REGION  - AWS 區域 (預設: us-east-1)"
    echo
    echo "範例:"
    echo "  $0 production"
    echo "  AWS_REGION=ap-southeast-1 $0 production"
    echo
}

# 處理命令列參數
if [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
    show_usage
    exit 0
fi

# 執行主函數
main "$@"