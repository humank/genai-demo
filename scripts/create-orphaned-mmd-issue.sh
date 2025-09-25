#!/bin/bash
# 創建孤立 .mmd 文件處理的 GitHub Issue

echo "🚀 創建 GitHub Issue: 處理孤立的 .mmd 文件"

# 檢查 GitHub CLI 是否已安裝和認證
if ! command -v gh &> /dev/null; then
    echo "❌ GitHub CLI 未安裝。請先安裝 gh CLI"
    echo "   macOS: brew install gh"
    echo "   其他: https://cli.github.com/"
    exit 1
fi

# 檢查是否已認證
if ! gh auth status &> /dev/null; then
    echo "❌ GitHub CLI 未認證。請先執行:"
    echo "   gh auth login"
    exit 1
fi

# 創建 Issue
echo "📝 創建 Issue..."

# 使用倉庫中存在的標籤創建 Issue
echo "🏷️  使用可用標籤創建 Issue..."

gh issue create \
    --title "處理孤立的 .mmd 文件 - Mermaid 遷移後續清理" \
    --body-file reports-summaries/task-execution/github-issue-orphaned-mmd.md \
    --label "documentation,enhancement" \
    --assignee "@me"

if [ $? -eq 0 ]; then
    echo "✅ Issue 創建成功！"
    echo "📄 Issue 內容來源: reports-summaries/task-execution/github-issue-orphaned-mmd.md"
    echo "📋 詳細報告: reports-summaries/diagrams/orphaned-mmd-files-report.md"
else
    echo "❌ Issue 創建失敗"
    echo "💡 你可以手動創建 Issue，使用以下內容:"
    echo "   標題: 處理孤立的 .mmd 文件 - Mermaid 遷移後續清理"
    echo "   內容: 參考 reports-summaries/task-execution/github-issue-orphaned-mmd.md"
    echo "   標籤: documentation, cleanup, enhancement, low-priority"
fi