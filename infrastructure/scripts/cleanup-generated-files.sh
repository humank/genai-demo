#!/bin/bash
# Infrastructure 目錄清理腳本

echo "🧹 清理 Infrastructure 目錄中的生成檔案..."

# 進入 infrastructure 目錄
cd "$(dirname "$0")/.."

# 刪除編譯輸出
echo "📁 刪除編譯輸出目錄..."
rm -rf dist/
rm -rf coverage/
rm -rf test-results/

# 刪除 CDK 輸出 (如果存在)
echo "☁️ 清理 CDK 輸出..."
rm -rf cdk.out/
rm -rf .cdk.staging/

# 刪除快取檔案
echo "🗂️ 清理快取檔案..."
rm -rf .jest-cache/
rm -f .eslintcache
rm -f tsconfig.tsbuildinfo

# 清理日誌檔案
echo "📋 清理日誌檔案..."
find . -name "*.log" -type f -delete
find . -name "npm-debug.log*" -type f -delete

# 清理臨時檔案
echo "🗑️ 清理臨時檔案..."
find . -name "*.tmp" -type f -delete
find . -name "*.temp" -type f -delete
find . -name ".DS_Store" -type f -delete

echo "✅ 清理完成！"
echo ""
echo "📊 清理後的目錄大小:"
du -sh . 2>/dev/null || echo "無法計算目錄大小"
echo ""
echo "🔍 剩餘的大型目錄:"
du -sh node_modules/ 2>/dev/null || echo "node_modules/ 不存在"