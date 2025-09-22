#!/bin/bash

# GenAI Demo - PlantUML 圖表生成腳本
# 此腳本用於生成所有 PlantUML 圖表為 PNG 和 SVG 格式
# 遵循 diagram-generation-standards.md 標準

set -e  # 遇到錯誤時退出

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 配置
PLANTUML_JAR="tools/plantuml.jar"
PLANTUML_URL="https://github.com/plantuml/plantuml/releases/latest/download/plantuml.jar"
DIAGRAMS_DIR="docs/diagrams"
OUTPUT_DIR="docs/diagrams/generated"
DEFAULT_FORMAT="png"  # PNG 推薦用於 GitHub 文檔 (更清晰易讀)

# 函數：打印帶顏色的消息
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 函數：檢查 PlantUML JAR 文件
check_plantuml() {
    if [ ! -f "$PLANTUML_JAR" ]; then
        print_warning "PlantUML JAR 文件不存在，正在下載..."
        mkdir -p tools
        if command -v curl >/dev/null 2>&1; then
            curl -L -o "$PLANTUML_JAR" "$PLANTUML_URL"
        elif command -v wget >/dev/null 2>&1; then
            wget -O "$PLANTUML_JAR" "$PLANTUML_URL"
        else
            print_error "需要 curl 或 wget 來下載 PlantUML"
            exit 1
        fi
        print_success "PlantUML JAR 文件下載完成"
    fi
}

# 函數：檢查 Java
check_java() {
    if ! command -v java >/dev/null 2>&1; then
        print_error "需要安裝 Java 來運行 PlantUML"
        exit 1
    fi
    
    java_version=$(java -version 2>&1 | head -n1 | cut -d'"' -f2)
    print_info "使用 Java 版本: $java_version"
}

# 函數：創建輸出目錄
create_output_dirs() {
    mkdir -p "$OUTPUT_DIR/functional"
    mkdir -p "$OUTPUT_DIR/information"
    mkdir -p "$OUTPUT_DIR/deployment"
    mkdir -p "$OUTPUT_DIR/development"
    mkdir -p "$OUTPUT_DIR/operational"
    mkdir -p "$OUTPUT_DIR/concurrency"
    mkdir -p "$OUTPUT_DIR/perspectives"
    mkdir -p "$OUTPUT_DIR/plantuml"
    mkdir -p "$OUTPUT_DIR/legacy"
    print_info "輸出目錄已創建"
}

# 函數：生成單個圖表
generate_diagram() {
    local puml_file="$1"
    local output_subdir="$2"
    local format="${3:-both}"  # png, svg, or both
    local filename=$(basename "$puml_file" .puml)
    local output_path="$OUTPUT_DIR/$output_subdir"
    
    print_info "生成圖表: $filename (格式: $format)"
    
    local success=true
    
    # 生成 PNG 格式 (推薦用於 GitHub 文檔 - 更清晰易讀)
    if [ "$format" = "png" ] || [ "$format" = "both" ]; then
        if java -jar "$PLANTUML_JAR" -tpng -o "../../generated/$output_subdir" "$puml_file"; then
            print_success "PNG 生成成功: $output_path/$filename.png"
        else
            print_error "PNG 生成失敗: $puml_file"
            success=false
        fi
    fi
    
    # 生成 SVG 格式 (高解析度向量圖，適合打印)
    if [ "$format" = "svg" ] || [ "$format" = "both" ]; then
        if java -jar "$PLANTUML_JAR" -tsvg -o "../../generated/$output_subdir" "$puml_file"; then
            print_success "SVG 生成成功: $output_path/$filename.svg"
        else
            print_error "SVG 生成失敗: $puml_file"
            success=false
        fi
    fi
    
    if [ "$success" = true ]; then
        return 0
    else
        return 1
    fi
}

# 函數：生成所有圖表
generate_all_diagrams() {
    local total_files=0
    local success_files=0
    local format="${1:-$DEFAULT_FORMAT}"
    
    print_info "開始生成所有 PlantUML 圖表 (格式: $format)..."
    
    # Viewpoints 圖表
    for viewpoint in functional information deployment development operational concurrency; do
        if [ -d "$DIAGRAMS_DIR/viewpoints/$viewpoint" ]; then
            print_info "生成 $viewpoint viewpoint 圖表..."
            for puml_file in "$DIAGRAMS_DIR/viewpoints/$viewpoint"/*.puml; do
                if [ -f "$puml_file" ]; then
                    total_files=$((total_files + 1))
                    if generate_diagram "$puml_file" "$viewpoint" "$format"; then
                        success_files=$((success_files + 1))
                    fi
                fi
            done
        fi
    done
    
    # Perspectives 圖表
    if [ -d "$DIAGRAMS_DIR/perspectives" ]; then
        print_info "生成 perspectives 圖表..."
        for perspective_dir in "$DIAGRAMS_DIR/perspectives"/*; do
            if [ -d "$perspective_dir" ]; then
                for puml_file in "$perspective_dir"/*.puml; do
                    if [ -f "$puml_file" ]; then
                        total_files=$((total_files + 1))
                        if generate_diagram "$puml_file" "perspectives" "$format"; then
                            success_files=$((success_files + 1))
                        fi
                    fi
                done
            fi
        done
    fi
    
    # PlantUML 目錄圖表
    if [ -d "$DIAGRAMS_DIR/plantuml" ]; then
        print_info "生成 plantuml 目錄圖表..."
        for puml_file in "$DIAGRAMS_DIR/plantuml"/*.puml; do
            if [ -f "$puml_file" ]; then
                total_files=$((total_files + 1))
                if generate_diagram "$puml_file" "plantuml" "$format"; then
                    success_files=$((success_files + 1))
                fi
            fi
        done
        
        # 處理子目錄
        for subdir in "$DIAGRAMS_DIR/plantuml"/*; do
            if [ -d "$subdir" ]; then
                for puml_file in "$subdir"/*.puml; do
                    if [ -f "$puml_file" ]; then
                        total_files=$((total_files + 1))
                        if generate_diagram "$puml_file" "plantuml" "$format"; then
                            success_files=$((success_files + 1))
                        fi
                    fi
                done
            fi
        done
    fi
    
    # Legacy 圖表
    if [ -d "$DIAGRAMS_DIR/legacy" ]; then
        print_info "生成 legacy 圖表..."
        for puml_file in $(find "$DIAGRAMS_DIR/legacy" -name "*.puml"); do
            if [ -f "$puml_file" ]; then
                total_files=$((total_files + 1))
                if generate_diagram "$puml_file" "legacy" "$format"; then
                    success_files=$((success_files + 1))
                fi
            fi
        done
    fi
    
    print_info "圖表生成完成: $success_files/$total_files 成功"
    
    if [ $success_files -eq $total_files ]; then
        print_success "所有圖表生成成功！"
        return 0
    else
        print_warning "部分圖表生成失敗"
        return 1
    fi
}

# 函數：生成特定圖表
generate_specific_diagram() {
    local diagram_name="$1"
    local format="${2:-$DEFAULT_FORMAT}"
    local found=false
    
    print_info "搜尋圖表: $diagram_name"
    
    # 搜尋所有可能的位置
    local search_paths=(
        "$DIAGRAMS_DIR/viewpoints/functional"
        "$DIAGRAMS_DIR/viewpoints/information"
        "$DIAGRAMS_DIR/viewpoints/deployment"
        "$DIAGRAMS_DIR/viewpoints/development"
        "$DIAGRAMS_DIR/viewpoints/operational"
        "$DIAGRAMS_DIR/viewpoints/concurrency"
        "$DIAGRAMS_DIR/perspectives"
        "$DIAGRAMS_DIR/plantuml"
        "$DIAGRAMS_DIR/legacy"
    )
    
    for search_path in "${search_paths[@]}"; do
        if [ -d "$search_path" ]; then
            # 直接搜尋
            local puml_file="$search_path/$diagram_name"
            if [ -f "$puml_file" ]; then
                found=true
                local category=$(basename "$(dirname "$puml_file")")
                generate_diagram "$puml_file" "$category" "$format"
                break
            fi
            
            # 添加 .puml 擴展名搜尋
            if [ -f "$puml_file.puml" ]; then
                found=true
                local category=$(basename "$(dirname "$puml_file.puml")")
                generate_diagram "$puml_file.puml" "$category" "$format"
                break
            fi
            
            # 遞歸搜尋子目錄
            local found_file=$(find "$search_path" -name "$diagram_name" -o -name "$diagram_name.puml" 2>/dev/null | head -1)
            if [ -n "$found_file" ]; then
                found=true
                local category=$(basename "$(dirname "$found_file")")
                generate_diagram "$found_file" "$category" "$format"
                break
            fi
        fi
    done
    
    if [ "$found" = false ]; then
        print_error "找不到圖表: $diagram_name"
        print_info "可用的圖表:"
        list_available_diagrams
        exit 1
    fi
}

# 函數：列出可用的圖表
list_available_diagrams() {
    print_info "可用的 PlantUML 圖表:"
    
    # Viewpoints
    echo -e "${YELLOW}Viewpoints:${NC}"
    for viewpoint in functional information deployment development operational concurrency; do
        if [ -d "$DIAGRAMS_DIR/viewpoints/$viewpoint" ]; then
            echo -e "  ${BLUE}$viewpoint:${NC}"
            for puml_file in "$DIAGRAMS_DIR/viewpoints/$viewpoint"/*.puml; do
                if [ -f "$puml_file" ]; then
                    echo "    - $(basename "$puml_file")"
                fi
            done
        fi
    done
    
    # Perspectives
    if [ -d "$DIAGRAMS_DIR/perspectives" ]; then
        echo -e "${YELLOW}Perspectives:${NC}"
        for perspective_dir in "$DIAGRAMS_DIR/perspectives"/*; do
            if [ -d "$perspective_dir" ]; then
                local perspective_name=$(basename "$perspective_dir")
                echo -e "  ${BLUE}$perspective_name:${NC}"
                for puml_file in "$perspective_dir"/*.puml; do
                    if [ -f "$puml_file" ]; then
                        echo "    - $(basename "$puml_file")"
                    fi
                done
            fi
        done
    fi
    
    # PlantUML directory
    if [ -d "$DIAGRAMS_DIR/plantuml" ]; then
        echo -e "${YELLOW}PlantUML:${NC}"
        for puml_file in "$DIAGRAMS_DIR/plantuml"/*.puml; do
            if [ -f "$puml_file" ]; then
                echo "  - $(basename "$puml_file")"
            fi
        done
        
        # 子目錄
        for subdir in "$DIAGRAMS_DIR/plantuml"/*; do
            if [ -d "$subdir" ]; then
                local subdir_name=$(basename "$subdir")
                echo -e "  ${BLUE}$subdir_name:${NC}"
                for puml_file in "$subdir"/*.puml; do
                    if [ -f "$puml_file" ]; then
                        echo "    - $(basename "$puml_file")"
                    fi
                done
            fi
        done
    fi
    
    # Legacy
    if [ -d "$DIAGRAMS_DIR/legacy" ]; then
        echo -e "${YELLOW}Legacy:${NC}"
        find "$DIAGRAMS_DIR/legacy" -name "*.puml" | while read puml_file; do
            echo "  - $(basename "$puml_file")"
        done
    fi
}

# 函數：驗證圖表語法
validate_diagrams() {
    print_info "驗證 PlantUML 圖表語法..."
    local total_files=0
    local valid_files=0
    
    # 找到所有 PlantUML 文件
    local puml_files=$(find "$DIAGRAMS_DIR" -name "*.puml" 2>/dev/null)
    
    for puml_file in $puml_files; do
        if [ -f "$puml_file" ]; then
            total_files=$((total_files + 1))
            if java -jar "$PLANTUML_JAR" -checkonly "$puml_file" >/dev/null 2>&1; then
                valid_files=$((valid_files + 1))
                print_success "語法正確: $(basename "$puml_file")"
            else
                print_error "語法錯誤: $(basename "$puml_file")"
            fi
        fi
    done
    
    print_info "語法驗證完成: $valid_files/$total_files 正確"
}

# 函數：清理生成的文件
clean_generated() {
    if [ -d "$OUTPUT_DIR" ]; then
        print_info "清理生成的圖表文件..."
        rm -rf "$OUTPUT_DIR"
        print_success "清理完成"
    else
        print_info "沒有需要清理的文件"
    fi
}

# 函數：顯示幫助信息
show_help() {
    echo "GenAI Demo - PlantUML 圖表生成腳本"
    echo ""
    echo "用法:"
    echo "  $0 [選項] [圖表名稱]"
    echo ""
    echo "選項:"
    echo "  -h, --help         顯示此幫助信息"
    echo "  -l, --list         列出所有可用的圖表"
    echo "  -v, --validate     驗證圖表語法"
    echo "  -c, --clean        清理生成的文件"
    echo "  -a, --all          生成所有圖表 (默認)"
    echo "  --format=FORMAT    指定輸出格式: png, svg, both (默認: both)"
    echo "                     PNG 推薦用於 GitHub 文檔 (更清晰易讀)"
    echo "                     SVG 適合高解析度顯示和打印"
    echo ""
    echo "範例:"
    echo "  $0                                    # 生成所有圖表"
    echo "  $0 domain-model-class-diagram.puml   # 生成特定圖表"
    echo "  $0 --validate                        # 驗證所有圖表語法"
    echo "  $0 --clean                           # 清理生成的文件"
    echo ""
}

# 主函數
main() {
    print_info "GenAI Demo - PlantUML 圖表生成腳本"
    print_info "=================================="
    
    # 檢查依賴
    check_java
    check_plantuml
    
    # 解析命令行參數
    case "${1:-}" in
        -h|--help)
            show_help
            exit 0
            ;;
        -l|--list)
            list_available_diagrams
            exit 0
            ;;
        -v|--validate)
            validate_diagrams
            exit 0
            ;;
        -c|--clean)
            clean_generated
            exit 0
            ;;
        -a|--all|"")
            create_output_dirs
            local format="$DEFAULT_FORMAT"
            if [[ "$1" == --format=* ]]; then
                format="${1#--format=}"
                shift
            fi
            generate_all_diagrams "$format"
            ;;
        --format=*)
            create_output_dirs
            local format="${1#--format=}"
            generate_all_diagrams "$format"
            ;;
        *)
            create_output_dirs
            local format="$DEFAULT_FORMAT"
            if [[ "$2" == --format=* ]]; then
                format="${2#--format=}"
            fi
            generate_specific_diagram "$1" "$format"
            ;;
    esac
}

# 執行主函數
main "$@"