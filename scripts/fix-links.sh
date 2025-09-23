#!/bin/bash

# 連結修復快速執行腳本

set -e

echo "🔗 GenAI Demo 連結修復工具"
echo "=========================="

# 檢查 Python 是否可用
if ! command -v python3 &> /dev/null; then
    echo "❌ 錯誤: 需要 Python 3"
    exit 1
fi

# 檢查是否在專案根目錄
if [ ! -f "scripts/master-link-fixer.py" ]; then
    echo "❌ 錯誤: 請在專案根目錄執行此腳本"
    exit 1
fi

# 設置執行權限
chmod +x scripts/*.py
chmod +x scripts/*.sh

# 根據參數執行不同模式
case "${1:-interactive}" in
    "auto"|"--auto")
        echo "🤖 執行自動修復模式..."
        python3 scripts/master-link-fixer.py --auto
        ;;
    "quick"|"--quick")
        echo "⚡ 執行快速修復..."
        python3 scripts/auto-fix-all-links.py
        ;;
    "check"|"--check")
        echo "🔍 執行連結檢查..."
        python3 scripts/check-final-links.py
        ;;
    "diagrams"|"--diagrams")
        echo "🖼️ 修復圖表文件名..."
        python3 scripts/fix-diagram-filenames.py
        ;;
    "help"|"--help"|"-h")
        echo "使用方法:"
        echo "  ./scripts/fix-links.sh [選項]"
        echo ""
        echo "選項:"
        echo "  auto      執行完整自動修復 (包含備份)"
        echo "  quick     執行快速修復 (不含備份)"
        echo "  check     只檢查連結狀態"
        echo "  diagrams  只修復圖表文件名"
        echo "  help      顯示此幫助信息"
        echo ""
        echo "無參數時進入互動模式"
        ;;
    "interactive"|*)
        echo "🎛️ 進入互動模式..."
        python3 scripts/master-link-fixer.py
        ;;
esac

echo ""
echo "✅ 執行完成！"
echo "📋 請查看 reports-summaries/task-execution/ 目錄中的報告"