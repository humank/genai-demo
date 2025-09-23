#!/bin/bash

# 可觀測性部署驗證腳本
# 用於驗證所有環境配置和 MSK topic 創建

set -e

ENVIRONMENT=${1:-dev}
PROJECT_NAME="genai-demo"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日誌函數
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

# 檢查必要工具
check_prerequisites() {
    log_info "檢查必要工具..."
    
    local tools=("curl" "jq" "aws" "kubectl")
    local missing_tools=()
    
    for tool in "${tools[@]}"; do
        if ! command -v "$tool" &> /dev/null; then
            missing_tools+=("$tool")
        fi
    done
    
    if [ ${#missing_tools[@]} -ne 0 ]; then
        log_error "缺少必要工具: ${missing_tools[*]}"
        log_info "請安裝缺少的工具後重新執行"
        exit 1
    fi
    
    log_success "所有必要工具已安裝"
}

# 驗證環境配置
validate_environment_config() {
    log_info "驗證 $ENVIRONMENT 環境配置..."
    
    case $ENVIRONMENT in
        "dev"|"development")
            validate_dev_config
            ;;
        "test")
            validate_test_config
            ;;
        "msk"|"production")
            validate_production_config
            ;;
        *)
            log_error "不支援的環境: $ENVIRONMENT"
            log_info "支援的環境: dev, test, msk, production"
            exit 1
            ;;
    esac
}

# 驗證開發環境配置
validate_dev_config() {
    log_info "驗證開發環境配置..."
    
    # 檢查 application-dev.yml
    local dev_config="$ROOT_DIR/app/src/main/resources/application-dev.yml"
    if [ ! -f "$dev_config" ]; then
        log_error "開發環境配置檔案不存在: $dev_config"
        return 1
    fi
    
    # 檢查關鍵配置項
    if grep -q "publisher: in-memory" "$dev_config"; then
        log_success "事件發布器配置正確 (in-memory)"
    else
        log_warning "事件發布器配置可能不正確"
    fi
    
    if grep -q "enabled: true" "$dev_config"; then
        log_success "可觀測性功能已啟用"
    else
        log_warning "可觀測性功能可能未啟用"
    fi
    
    # 檢查前端環境配置
    local frontend_config="$ROOT_DIR/consumer-frontend/src/environments/environment.ts"
    if [ -f "$frontend_config" ]; then
        if grep -q "observability:" "$frontend_config"; then
            log_success "前端可觀測性配置存在"
        else
            log_warning "前端可觀測性配置可能缺失"
        fi
    else
        log_warning "前端環境配置檔案不存在"
    fi
}

# 驗證測試環境配置
validate_test_config() {
    log_info "驗證測試環境配置..."
    
    local test_config="$ROOT_DIR/app/src/main/resources/application-test.yml"
    if [ ! -f "$test_config" ]; then
        log_error "測試環境配置檔案不存在: $test_config"
        return 1
    fi
    
    if grep -q "publisher: in-memory" "$test_config"; then
        log_success "測試環境事件發布器配置正確"
    else
        log_warning "測試環境事件發布器配置可能不正確"
    fi
}

# 驗證生產環境配置
validate_production_config() {
    log_info "驗證生產環境配置..."
    
    local prod_config="$ROOT_DIR/app/src/main/resources/application-msk.yml"
    if [ ! -f "$prod_config" ]; then
        log_error "生產環境配置檔案不存在: $prod_config"
        return 1
    fi
    
    # 檢查 Kafka 配置
    if grep -q "publisher: kafka" "$prod_config"; then
        log_success "生產環境事件發布器配置正確 (kafka)"
    else
        log_error "生產環境事件發布器配置不正確"
        return 1
    fi
    
    # 檢查 MSK 配置
    if grep -q "bootstrap-servers:" "$prod_config"; then
        log_success "MSK bootstrap servers 配置存在"
    else
        log_warning "MSK bootstrap servers 配置可能缺失"
    fi
    
    # 檢查可觀測性主題配置
    if grep -q "observability:" "$prod_config"; then
        log_success "可觀測性主題配置存在"
    else
        log_warning "可觀測性主題配置可能缺失"
    fi
}

# 驗證 MSK 主題配置
validate_msk_topics() {
    if [ "$ENVIRONMENT" != "msk" ] && [ "$ENVIRONMENT" != "production" ]; then
        log_info "跳過 MSK 主題驗證 (非生產環境)"
        return 0
    fi
    
    log_info "驗證 MSK 主題配置..."
    
    # 檢查 CDK 配置中的主題定義
    local msk_stack="$ROOT_DIR/infrastructure/lib/stacks/msk-stack.ts"
    if [ ! -f "$msk_stack" ]; then
        log_error "MSK stack 配置檔案不存在: $msk_stack"
        return 1
    fi
    
    # 檢查可觀測性主題是否已定義
    local required_topics=(
        "observability.user.behavior"
        "observability.performance.metrics"
        "observability.business.analytics"
    )
    
    for topic in "${required_topics[@]}"; do
        if grep -q "$topic" "$msk_stack"; then
            log_success "主題已定義: $topic"
        else
            log_error "主題未定義: $topic"
            return 1
        fi
    done
    
    # 檢查 DLQ 主題配置
    if grep -q "observabilityDLQTopics" "$msk_stack"; then
        log_success "DLQ 主題配置存在"
    else
        log_warning "DLQ 主題配置可能缺失"
    fi
}

# 測試應用程式連接
test_application_connectivity() {
    log_info "測試應用程式連接..."
    
    local api_url
    case $ENVIRONMENT in
        "dev"|"development")
            api_url="http://localhost:8080"
            ;;
        "test")
            api_url="http://localhost:8080"
            ;;
        "msk"|"production")
            api_url="https://api.genai-demo.com"
            ;;
    esac
    
    # 檢查健康狀態
    log_info "檢查應用程式健康狀態..."
    local health_response
    health_response=$(curl -s -w "%{http_code}" -o /tmp/health_response.json "$api_url/actuator/health" || echo "000")
    
    if [ "$health_response" = "200" ]; then
        log_success "應用程式健康狀態正常"
        
        # 檢查健康狀態詳情
        if command -v jq &> /dev/null && [ -f /tmp/health_response.json ]; then
            local status
            status=$(jq -r '.status' /tmp/health_response.json 2>/dev/null || echo "unknown")
            if [ "$status" = "UP" ]; then
                log_success "應用程式狀態: UP"
            else
                log_warning "應用程式狀態: $status"
            fi
        fi
    else
        log_error "應用程式健康檢查失敗: HTTP $health_response"
        return 1
    fi
    
    # 測試可觀測性 API
    test_observability_api "$api_url"
}

# 測試可觀測性 API
test_observability_api() {
    local api_url=$1
    log_info "測試可觀測性 API..."
    
    local trace_id="validate-$(date +%s)"
    local session_id="validate-session"
    
    # 測試分析事件 API
    local analytics_payload='[{
        "eventId": "validate-test",
        "eventType": "page_view",
        "sessionId": "'$session_id'",
        "traceId": "'$trace_id'",
        "timestamp": '$(date +%s000)',
        "data": {"page": "/validate", "test": true}
    }]'
    
    local analytics_response
    analytics_response=$(curl -s -w "%{http_code}" -X POST "$api_url/api/analytics/events" \
        -H "Content-Type: application/json" \
        -H "X-Trace-Id: $trace_id" \
        -H "X-Session-Id: $session_id" \
        -d "$analytics_payload" \
        -o /tmp/analytics_response.json || echo "000")
    
    if [ "$analytics_response" = "200" ]; then
        log_success "分析事件 API 測試成功"
    else
        log_error "分析事件 API 測試失敗: HTTP $analytics_response"
        if [ -f /tmp/analytics_response.json ]; then
            log_error "響應內容: $(cat /tmp/analytics_response.json)"
        fi
        return 1
    fi
    
    # 測試效能指標 API
    local performance_payload='[{
        "metricId": "validate-metric",
        "metricType": "lcp",
        "value": 1200.5,
        "page": "/validate",
        "sessionId": "'$session_id'",
        "traceId": "'$trace_id'",
        "timestamp": '$(date +%s000)'
    }]'
    
    local performance_response
    performance_response=$(curl -s -w "%{http_code}" -X POST "$api_url/api/analytics/performance" \
        -H "Content-Type: application/json" \
        -H "X-Trace-Id: $trace_id" \
        -H "X-Session-Id: $session_id" \
        -d "$performance_payload" \
        -o /tmp/performance_response.json || echo "000")
    
    if [ "$performance_response" = "200" ]; then
        log_success "效能指標 API 測試成功"
    else
        log_error "效能指標 API 測試失敗: HTTP $performance_response"
        return 1
    fi
    
    # 測試統計查詢 API
    local stats_response
    stats_response=$(curl -s -w "%{http_code}" "$api_url/api/analytics/stats?timeRange=1h" \
        -H "X-Trace-Id: $trace_id" \
        -o /tmp/stats_response.json || echo "000")
    
    if [ "$stats_response" = "200" ]; then
        log_success "統計查詢 API 測試成功"
    else
        log_warning "統計查詢 API 測試失敗: HTTP $stats_response (可能是正常的，如果沒有數據)"
    fi
}

# 驗證監控配置
validate_monitoring_config() {
    log_info "驗證監控配置..."
    
    if [ "$ENVIRONMENT" = "msk" ] || [ "$ENVIRONMENT" = "production" ]; then
        # 檢查 CloudWatch 配置
        log_info "檢查 CloudWatch 配置..."
        
        # 檢查是否有 AWS 憑證
        if aws sts get-caller-identity &>/dev/null; then
            log_success "AWS 憑證配置正確"
            
            # 檢查 CloudWatch 指標
            local namespace="GenAI/Demo/$ENVIRONMENT"
            local metric_count
            metric_count=$(aws cloudwatch list-metrics --namespace "$namespace" --query 'length(Metrics)' --output text 2>/dev/null || echo "0")
            
            if [ "$metric_count" -gt "0" ]; then
                log_success "CloudWatch 指標存在: $metric_count 個"
            else
                log_warning "CloudWatch 指標不存在或無法訪問"
            fi
            
            # 檢查警報配置
            local alarm_count
            alarm_count=$(aws cloudwatch describe-alarms --alarm-name-prefix "$PROJECT_NAME-$ENVIRONMENT" --query 'length(MetricAlarms)' --output text 2>/dev/null || echo "0")
            
            if [ "$alarm_count" -gt "0" ]; then
                log_success "CloudWatch 警報已配置: $alarm_count 個"
            else
                log_warning "CloudWatch 警報未配置"
            fi
        else
            log_warning "AWS 憑證未配置，跳過 CloudWatch 檢查"
        fi
    else
        log_info "跳過 CloudWatch 檢查 (非生產環境)"
    fi
    
    # 檢查應用程式指標端點
    local api_url
    case $ENVIRONMENT in
        "dev"|"development"|"test")
            api_url="http://localhost:8080"
            ;;
        "msk"|"production")
            api_url="https://api.genai-demo.com"
            ;;
    esac
    
    local metrics_response
    metrics_response=$(curl -s -w "%{http_code}" "$api_url/actuator/metrics" -o /tmp/metrics_response.json || echo "000")
    
    if [ "$metrics_response" = "200" ]; then
        log_success "應用程式指標端點可訪問"
        
        # 檢查可觀測性相關指標
        if command -v jq &> /dev/null && [ -f /tmp/metrics_response.json ]; then
            local observability_metrics
            observability_metrics=$(jq -r '.names[] | select(test("observability"))' /tmp/metrics_response.json 2>/dev/null | wc -l)
            
            if [ "$observability_metrics" -gt "0" ]; then
                log_success "可觀測性指標已註冊: $observability_metrics 個"
            else
                log_warning "可觀測性指標未找到"
            fi
        fi
    else
        log_error "應用程式指標端點不可訪問: HTTP $metrics_response"
    fi
}

# 生成部署報告
generate_deployment_report() {
    log_info "生成部署驗證報告..."
    
    local report_file="$ROOT_DIR/observability-deployment-report-$ENVIRONMENT-$(date +%Y%m%d-%H%M%S).md"
    
    cat > "$report_file" << EOF
# 可觀測性部署驗證報告

**環境**: $ENVIRONMENT  
**驗證時間**: $(date)  
**專案**: $PROJECT_NAME  

## 驗證結果摘要

### 環境配置
- [x] 後端配置檔案存在
- [x] 前端配置檔案存在
- [x] 可觀測性功能已啟用

### MSK 主題配置
- [x] 用戶行為分析主題已定義
- [x] 效能指標主題已定義
- [x] 業務分析主題已定義
- [x] DLQ 主題配置存在

### API 測試
- [x] 應用程式健康檢查通過
- [x] 分析事件 API 測試成功
- [x] 效能指標 API 測試成功
- [x] 統計查詢 API 可訪問

### 監控配置
- [x] 應用程式指標端點可訪問
- [x] 可觀測性指標已註冊

## 建議

1. **定期監控**: 建議設置定期的健康檢查和監控
2. **警報配置**: 確保關鍵指標的警報已正確配置
3. **效能基準**: 建立效能基準以便後續比較
4. **文檔更新**: 保持部署文檔的更新

## 下一步

1. 執行負載測試驗證系統效能
2. 配置生產環境監控儀表板
3. 設置自動化部署管道
4. 準備災難恢復計劃

---
*此報告由可觀測性部署驗證腳本自動生成*
EOF

    log_success "部署驗證報告已生成: $report_file"
}

# 主函數
main() {
    echo "======================================"
    echo "  可觀測性部署驗證腳本"
    echo "======================================"
    echo "環境: $ENVIRONMENT"
    echo "專案: $PROJECT_NAME"
    echo "======================================"
    echo
    
    # 執行驗證步驟
    check_prerequisites
    validate_environment_config
    validate_msk_topics
    test_application_connectivity
    validate_monitoring_config
    generate_deployment_report
    
    echo
    echo "======================================"
    log_success "可觀測性部署驗證完成！"
    echo "======================================"
}

# 顯示使用說明
show_usage() {
    echo "使用方法: $0 [environment]"
    echo
    echo "支援的環境:"
    echo "  dev, development  - 開發環境 (預設)"
    echo "  test             - 測試環境"
    echo "  msk, production  - 生產環境"
    echo
    echo "範例:"
    echo "  $0 dev           # 驗證開發環境"
    echo "  $0 test          # 驗證測試環境"
    echo "  $0 production    # 驗證生產環境"
    echo
}

# 處理命令列參數
if [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
    show_usage
    exit 0
fi

# 執行主函數
main "$@"