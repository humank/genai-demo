#!/bin/bash

# GenAI Demo - Mermaid 圖表生成腳本 (增量版本)
# 此腳本用於生成 Mermaid 圖表為 SVG 格式
# 支持增量生成，只處理變更的文件

set -e

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 配置
DIAGRAMS_DIR="docs/diagrams"
OUTPUT_DIR="docs/diagrams/generated"
CHANGED_ONLY=false

# 函數：打印帶顏色的消息
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 函數：檢查 Mermaid CLI
check_mermaid() {
    if ! command -v mmdc >/dev/null 2>&1; then
        print_error "需要安裝 Mermaid CLI: npm install -g @mermaid-js/mermaid-cli"
        exit 1
    fi
}

# 函數：檢查文件是否需要重新生成
needs_regeneration() {
    local mmd_file="$1"
    local output_file="$2"
    
    # 如果不是增量模式，總是生成
    if [ "$CHANGED_ONLY" = false ]; then
        return 0
    fi
    
    # 如果輸出文件不存在，需要生成
    if [ ! -f "$output_file" ]; then
        print_info "需要生成 $(basename "$mmd_file") - 輸出文件不存在"
        return 0
    fi
    
    # 如果源文件比輸出文件新，需要重新生成
    if [ "$mmd_file" -nt "$output_file" ]; then
        print_info "需要生成 $(basename "$mmd_file") - 源文件較新"
        return 0
    fi
    
    # 不需要重新生成
    print_info "跳過 $(basename "$mmd_file") - 無變更"
    return 1
}

# 函數：生成單個 Mermaid 圖表
generate_mermaid_diagram() {
    local mmd_file="$1"
    local filename=$(basename "$mmd_file" .mmd)
    local output_file="$OUTPUT_DIR/$(basename "$(dirname "$mmd_file")")/$filename.svg"
    
    # 檢查是否需要重新生成
    if ! needs_regeneration "$mmd_file" "$output_file"; then
        return 0
    fi
    
    print_info "生成 Mermaid 圖表: $filename"
    
    # 創建輸出目錄
    mkdir -p "$(dirname "$output_file")"
    
    # 生成 SVG
    if mmdc -i "$mmd_file" -o "$output_file" -t neutral -b white; then
        print_success "SVG 生成成功: $output_file"
        return 0
    else
        print_error "SVG 生成失敗: $mmd_file"
        return 1
    fi
}

# 函數：生成所有 Mermaid 圖表
generate_all_mermaid() {
    print_info "開始生成所有 Mermaid 圖表..."
    
    local total_files=0
    local success_files=0
    
    # 查找所有 .mmd 文件
    while IFS= read -r -d '' mmd_file; do
        if [ -f "$mmd_file" ]; then
            total_files=$((total_files + 1))
            if generate_mermaid_diagram "$mmd_file"; then
                success_files=$((success_files + 1))
            fi
        fi
    done < <(find "$DIAGRAMS_DIR" -name "*.mmd" -print0 2>/dev/null)
    
    print_info "Mermaid 圖表生成完成: $success_files/$total_files 成功"
    
    if [ $success_files -eq $total_files ]; then
        print_success "所有 Mermaid 圖表生成成功！"
        return 0
    else
        print_error "部分 Mermaid 圖表生成失敗"
        return 1
    fi
}

# 函數：顯示幫助信息
show_help() {
    echo "GenAI Demo - Mermaid 圖表生成腳本"
    echo ""
    echo "用法:"
    echo "  $0 [選項]"
    echo ""
    echo "選項:"
    echo "  -h, --help         顯示此幫助信息"
    echo "  --changed-only     只生成變更的圖表 (增量生成)"
    echo ""
}

# 主函數
main() {
    print_info "GenAI Demo - Mermaid 圖表生成腳本"
    print_info "===================================="
    
    # 解析命令行參數
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_help
                exit 0
                ;;
            --changed-only)
                CHANGED_ONLY=true
                shift
                ;;
            *)
                print_error "未知參數: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    # 檢查依賴
    check_mermaid
    
    # 創建輸出目錄
    mkdir -p "$OUTPUT_DIR"
    
    # 生成圖表
    generate_all_mermaid
}

# 執行主函數
main "$@"