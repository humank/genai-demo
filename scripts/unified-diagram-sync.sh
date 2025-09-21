#!/bin/bash

# 統一圖表同步腳本
# 整合 Hook 自動化和任務 9 的圖表生成工具

set -e

# 顏色定義
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

print_message() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

print_message $BLUE "🔄 啟動統一圖表同步系統"

# 1. 執行 Hook 的文檔-圖表同步
print_message $YELLOW "📊 執行文檔-圖表引用同步..."
if [ -f "scripts/sync-diagram-references.py" ]; then
    python3 scripts/sync-diagram-references.py --comprehensive --validate --report
    echo "✅ 文檔同步完成"
else
    echo "⚠️  sync-diagram-references.py 不存在，跳過文檔同步"
fi

# 2. 執行任務 9 的圖表生成
print_message $YELLOW "🎨 執行圖表生成..."
if [ -f "scripts/generate-all-diagrams.sh" ]; then
    ./scripts/generate-all-diagrams.sh "$@"
    echo "✅ 圖表生成完成"
else
    echo "⚠️  generate-all-diagrams.sh 不存在，跳過圖表生成"
fi

# 3. 最終驗證
print_message $YELLOW "🔍 執行最終驗證..."
if [ -f "scripts/sync-diagram-references.py" ]; then
    python3 scripts/sync-diagram-references.py --validate --report > diagram-sync-final-report.md
    echo "✅ 最終驗證完成，報告已生成：diagram-sync-final-report.md"
fi

print_message $GREEN "🎉 統一圖表同步完成！"

# 4. 顯示統計信息
print_message $BLUE "📈 統計信息："
echo "  - Mermaid 圖表: $(find docs/diagrams -name "*.mmd" | wc -l)"
echo "  - PlantUML 圖表: $(find docs/diagrams -name "*.puml" | wc -l)"  
echo "  - Excalidraw 圖表: $(find docs/diagrams -name "*.excalidraw" | wc -l)"
echo "  - SVG 圖片: $(find docs/diagrams -name "*.svg" | wc -l)"
echo "  - SVG 圖片: $(find docs/diagrams -name "*.svg" | wc -l)"