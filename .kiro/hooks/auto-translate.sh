#!/bin/bash

# 文檔自動翻譯腳本
# 使用方式: ./auto-translate.sh [commit-message]

set -e

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

# 檢查是否需要翻譯
should_translate() {
    local commit_msg="$1"
    if [[ "$commit_msg" == *"[translate]"* ]] || [[ "$commit_msg" == *"[en]"* ]]; then
        return 0
    else
        return 1
    fi
}

# 獲取 staged 的 .md 檔案
get_staged_md_files() {
    git diff --cached --name-only --diff-filter=AM | grep '\.md$' || true
}

# 轉換檔案路徑為英文版本
convert_to_english_path() {
    local file="$1"
    
    if [[ "$file" == docs/* ]]; then
        # docs/ 下的檔案移到 docs/en/
        echo "${file/docs\//docs/en/}"
    else
        # 根目錄的 .md 檔案移到 docs/en/
        echo "docs/en/$file"
    fi
}

# 建立目錄結構
ensure_directory() {
    local file="$1"
    local dir=$(dirname "$file")
    mkdir -p "$dir"
}

# 翻譯單個檔案 (這裡是簡化版本，實際應該調用 AI 翻譯)
translate_file() {
    local source="$1"
    local target="$2"
    
    log_info "翻譯 $source -> $target"
    
    # 確保目標目錄存在
    ensure_directory "$target"
    
    # 讀取原始檔案並進行基本的連結轉換
    # 這裡是簡化版本，實際應該調用 Kiro AI 進行翻譯
    
    # 建立翻譯標記檔案
    cat > "$target" << EOF
<!-- This file is auto-translated from $source -->
<!-- 此檔案由 $source 自動翻譯而來 -->
<!-- Please use Kiro AI to complete the actual translation -->
<!-- 請使用 Kiro AI 完成實際翻譯 -->

$(cat "$source")

<!-- Translation placeholder - Use Kiro AI to translate this content -->
<!-- 翻譯佔位符 - 請使用 Kiro AI 翻譯此內容 -->
EOF
    
    log_success "已建立翻譯佔位符: $target"
}

# 主函數
main() {
    local commit_msg="${1:-$(cat .git/COMMIT_EDITMSG 2>/dev/null || echo '')}"
    
    log_info "檢查是否需要翻譯..."
    
    if ! should_translate "$commit_msg"; then
        log_info "Commit message 不包含翻譯觸發關鍵字 [translate] 或 [en]，跳過翻譯"
        exit 0
    fi
    
    log_info "偵測到翻譯觸發關鍵字，開始處理..."
    
    # 獲取需要翻譯的檔案
    local staged_files
    staged_files=$(get_staged_md_files)
    
    if [[ -z "$staged_files" ]]; then
        log_warning "沒有找到需要翻譯的 .md 檔案"
        exit 0
    fi
    
    log_info "找到以下需要翻譯的檔案:"
    echo "$staged_files" | while read -r file; do
        echo "  - $file"
    done
    
    # 翻譯每個檔案
    local translated_files=()
    while IFS= read -r file; do
        if [[ -n "$file" ]]; then
            local target_file
            target_file=$(convert_to_english_path "$file")
            translate_file "$file" "$target_file"
            translated_files+=("$target_file")
        fi
    done <<< "$staged_files"
    
    # 將翻譯後的檔案加入 git
    if [[ ${#translated_files[@]} -gt 0 ]]; then
        log_info "將翻譯檔案加入 git..."
        for file in "${translated_files[@]}"; do
            git add "$file"
            log_success "已加入: $file"
        done
        
        log_success "翻譯完成！共處理 ${#translated_files[@]} 個檔案"
        log_info "請使用 Kiro AI 完成實際的內容翻譯"
    fi
}

# 執行主函數
main "$@"