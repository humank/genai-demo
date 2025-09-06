#!/bin/bash

# GenAI Demo - PlantUML 圖表生成腳本
# 此腳本用於生成所有 PlantUML 圖表為 PNG 和 SVG 格式

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
DIAGRAMS_DIR="docs/diagrams/plantuml"
OUTPUT_DIR="docs/diagrams/generated"

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
    mkdir -p "$OUTPUT_DIR/structural"
    mkdir -p "$OUTPUT_DIR/behavioral"
    mkdir -p "$OUTPUT_DIR/interaction"
    mkdir -p "$OUTPUT_DIR/event-storming"
    print_info "輸出目錄已創建"
}

# 函數：生成單個圖表
generate_diagram() {
    local puml_file="$1"
    local output_subdir="$2"
    local filename=$(basename "$puml_file" .puml)
    local output_path="$OUTPUT_DIR/$output_subdir"
    
    print_info "生成圖表: $filename"
    
    # 生成 PNG 格式
    if java -jar "$PLANTUML_JAR" -tpng -o "../../generated/$output_subdir" "$puml_file"; then
        print_success "PNG 生成成功: $output_path/$filename.png"
    else
        print_error "PNG 生成失敗: $puml_file"
        return 1
    fi
    
    # 生成 SVG 格式
    if java -jar "$PLANTUML_JAR" -tsvg -o "../../generated/$output_subdir" "$puml_file"; then
        print_success "SVG 生成成功: $output_path/$filename.svg"
    else
        print_error "SVG 生成失敗: $puml_file"
        return 1
    fi
}

# 函數：生成所有圖表
generate_all_diagrams() {
    local total_files=0
    local success_files=0
    
    print_info "開始生成所有 PlantUML 圖表..."
    
    # 結構圖
    if [ -d "$DIAGRAMS_DIR/structural" ]; then
        print_info "生成結構圖..."
        for puml_file in "$DIAGRAMS_DIR/structural"/*.puml; do
            if [ -f "$puml_file" ]; then
                total_files=$((total_files + 1))
                if generate_diagram "$puml_file" "structural"; then
                    success_files=$((success_files + 1))
                fi
            fi
        done
    fi
    
    # 行為圖
    if [ -d "$DIAGRAMS_DIR/behavioral" ]; then
        print_info "生成行為圖..."
        for puml_file in "$DIAGRAMS_DIR/behavioral"/*.puml; do
            if [ -f "$puml_file" ]; then
                total_files=$((total_files + 1))
                if generate_diagram "$puml_file" "behavioral"; then
                    success_files=$((success_files + 1))
                fi
            fi
        done
    fi
    
    # 交互圖
    if [ -d "$DIAGRAMS_DIR/interaction" ]; then
        print_info "生成交互圖..."
        for puml_file in "$DIAGRAMS_DIR/interaction"/*.puml; do
            if [ -f "$puml_file" ]; then
                total_files=$((total_files + 1))
                if generate_diagram "$puml_file" "interaction"; then
                    success_files=$((success_files + 1))
                fi
            fi
        done
        
        # 處理子目錄
        if [ -d "$DIAGRAMS_DIR/interaction/sequence-diagrams" ]; then
            for puml_file in "$DIAGRAMS_DIR/interaction/sequence-diagrams"/*.puml; do
                if [ -f "$puml_file" ]; then
                    total_files=$((total_files + 1))
                    if generate_diagram "$puml_file" "interaction"; then
                        success_files=$((success_files + 1))
                    fi
                fi
            done
        fi
    fi
    
    # Event Storming 圖
    if [ -d "$DIAGRAMS_DIR/event-storming" ]; then
        print_info "生成 Event Storming 圖..."
        for puml_file in "$DIAGRAMS_DIR/event-storming"/*.puml; do
            if [ -f "$puml_file" ]; then
                total_files=$((total_files + 1))
                if generate_diagram "$puml_file" "event-storming"; then
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
    local found=false
    
    print_info "搜尋圖表: $diagram_name"
    
    # 在所有子目錄中搜尋
    for subdir in structural behavioral interaction event-storming; do
        local puml_file="$DIAGRAMS_DIR/$subdir/$diagram_name"
        if [ -f "$puml_file" ]; then
            found=true
            generate_diagram "$puml_file" "$subdir"
            break
        fi
        
        # 檢查是否需要添加 .puml 擴展名
        if [ -f "$puml_file.puml" ]; then
            found=true
            generate_diagram "$puml_file.puml" "$subdir"
            break
        fi
    done
    
    # 檢查 sequence-diagrams 子目錄
    local seq_file="$DIAGRAMS_DIR/interaction/sequence-diagrams/$diagram_name"
    if [ -f "$seq_file" ] || [ -f "$seq_file.puml" ]; then
        found=true
        if [ -f "$seq_file" ]; then
            generate_diagram "$seq_file" "interaction"
        else
            generate_diagram "$seq_file.puml" "interaction"
        fi
    fi
    
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
    
    for subdir in structural behavioral interaction event-storming; do
        if [ -d "$DIAGRAMS_DIR/$subdir" ]; then
            echo -e "${YELLOW}$subdir:${NC}"
            for puml_file in "$DIAGRAMS_DIR/$subdir"/*.puml; do
                if [ -f "$puml_file" ]; then
                    echo "  - $(basename "$puml_file")"
                fi
            done
        fi
    done
    
    if [ -d "$DIAGRAMS_DIR/interaction/sequence-diagrams" ]; then
        echo -e "${YELLOW}sequence-diagrams:${NC}"
        for puml_file in "$DIAGRAMS_DIR/interaction/sequence-diagrams"/*.puml; do
            if [ -f "$puml_file" ]; then
                echo "  - $(basename "$puml_file")"
            fi
        done
    fi
}

# 函數：驗證圖表語法
validate_diagrams() {
    print_info "驗證 PlantUML 圖表語法..."
    local total_files=0
    local valid_files=0
    
    for subdir in structural behavioral interaction event-storming; do
        if [ -d "$DIAGRAMS_DIR/$subdir" ]; then
            for puml_file in "$DIAGRAMS_DIR/$subdir"/*.puml; do
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
    echo "  -h, --help      顯示此幫助信息"
    echo "  -l, --list      列出所有可用的圖表"
    echo "  -v, --validate  驗證圖表語法"
    echo "  -c, --clean     清理生成的文件"
    echo "  -a, --all       生成所有圖表 (默認)"
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
            generate_all_diagrams
            ;;
        *)
            create_output_dirs
            generate_specific_diagram "$1"
            ;;
    esac
}

# 執行主函數
main "$@"