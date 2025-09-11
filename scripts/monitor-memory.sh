#!/bin/bash

# 記憶體監控腳本
# 在測試或編譯期間監控記憶體使用情況

echo "📊 開始監控記憶體使用情況..."
echo "按 Ctrl+C 停止監控"

# 創建日誌文件
MEMORY_LOG="build/memory-usage.log"
mkdir -p build
echo "時間,總記憶體(MB),已用記憶體(MB),可用記憶體(MB),記憶體使用率(%)" > $MEMORY_LOG

while true; do
    # 獲取記憶體資訊 (macOS)
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        TOTAL_MEM=$(sysctl -n hw.memsize)
        TOTAL_MEM_MB=$((TOTAL_MEM / 1024 / 1024))
        
        # 獲取已用記憶體
        USED_MEM=$(vm_stat | grep "Pages active" | awk '{print $3}' | sed 's/\.//')
        USED_MEM_MB=$((USED_MEM * 4096 / 1024 / 1024))
        
        # 計算可用記憶體
        AVAILABLE_MEM_MB=$((TOTAL_MEM_MB - USED_MEM_MB))
        
        # 計算使用率
        USAGE_PERCENT=$((USED_MEM_MB * 100 / TOTAL_MEM_MB))
    else
        # Linux
        TOTAL_MEM_MB=$(free -m | awk 'NR==2{print $2}')
        USED_MEM_MB=$(free -m | awk 'NR==2{print $3}')
        AVAILABLE_MEM_MB=$(free -m | awk 'NR==2{print $7}')
        USAGE_PERCENT=$((USED_MEM_MB * 100 / TOTAL_MEM_MB))
    fi
    
    # 獲取當前時間
    TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')
    
    # 輸出到控制台
    printf "\r記憶體使用: %d/%d MB (%.1f%%) | 可用: %d MB" \
        $USED_MEM_MB $TOTAL_MEM_MB $USAGE_PERCENT $AVAILABLE_MEM_MB
    
    # 記錄到日誌文件
    echo "$TIMESTAMP,$TOTAL_MEM_MB,$USED_MEM_MB,$AVAILABLE_MEM_MB,$USAGE_PERCENT" >> $MEMORY_LOG
    
    # 如果記憶體使用率超過 90%，發出警告
    if [ $USAGE_PERCENT -gt 90 ]; then
        echo ""
        echo "⚠️  警告: 記憶體使用率超過 90%！"
    fi
    
    sleep 2
done