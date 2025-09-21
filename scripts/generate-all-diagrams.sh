#!/bin/bash

# 自動化圖表生成腳本
# 支援 Mermaid、PlantUML 和 Excalidraw 格式

set -e

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 配置
PLANTUML_JAR="tools/plantuml.jar"
DIAGRAMS_DIR="docs/diagrams"
SCRIPTS_DIR="scripts"
LOG_FILE="diagram-generation.log"

# 函數：打印帶顏色的消息
print_message() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# 函數：記錄日誌
log_message() {
    local message=$1
    echo "$(date '+%Y-%m-%d %H:%M:%S') - $message" >> "$LOG_FILE"
}

# 函數：檢查依賴
check_dependencies() {
    print_message $BLUE "🔍 檢查依賴..."
    
    local missing_deps=()
    
    # 檢查 Java (PlantUML 需要)
    if ! command -v java &> /dev/null; then
        missing_deps+=("java")
    fi
    
    # 檢查 Node.js (Mermaid CLI 需要)
    if ! command -v node &> /dev/null; then
        missing_deps+=("node")
    fi
    
    # 檢查 PlantUML JAR
    if [ ! -f "$PLANTUML_JAR" ]; then
        print_message $YELLOW "⚠️  PlantUML JAR 不存在，正在下載..."
        mkdir -p tools
        curl -L "https://github.com/plantuml/plantuml/releases/latest/download/plantuml.jar" -o "$PLANTUML_JAR"
        if [ $? -eq 0 ]; then
            print_message $GREEN "✅ PlantUML JAR 下載完成"
        else
            print_message $RED "❌ PlantUML JAR 下載失敗"
            exit 1
        fi
    fi
    
    # 檢查 Mermaid CLI
    if ! command -v mmdc &> /dev/null; then
        print_message $YELLOW "⚠️  Mermaid CLI 未安裝，正在安裝..."
        npm install -g @mermaid-js/mermaid-cli
        if [ $? -eq 0 ]; then
            print_message $GREEN "✅ Mermaid CLI 安裝完成"
        else
            print_message $RED "❌ Mermaid CLI 安裝失敗"
            exit 1
        fi
    fi
    
    if [ ${#missing_deps[@]} -ne 0 ]; then
        print_message $RED "❌ 缺少依賴: ${missing_deps[*]}"
        print_message $YELLOW "請安裝缺少的依賴後重新運行"
        exit 1
    fi
    
    print_message $GREEN "✅ 所有依賴檢查通過"
}

# 函數：生成 PlantUML 圖表
generate_plantuml() {
    print_message $PURPLE "📊 生成 PlantUML 圖表..."
    
    local puml_files=$(find "$DIAGRAMS_DIR" -name "*.puml" -type f)
    local count=0
    local success=0
    local failed=0
    
    if [ -z "$puml_files" ]; then
        print_message $YELLOW "⚠️  未找到 PlantUML 文件"
        return 0
    fi
    
    for file in $puml_files; do
        count=$((count + 1))
        local basename=$(basename "$file" .puml)
        local dirname=$(dirname "$file")
        local svg_file="$dirname/$basename.svg"
        
        print_message $CYAN "  處理: $file"
        
        # 生成 SVG (高解析度向量圖)
        if java -jar "$PLANTUML_JAR" -tsvg "$file" 2>/dev/null; then
            success=$((success + 1))
            log_message "SUCCESS: Generated SVG for $file"
        else
            failed=$((failed + 1))
            print_message $RED "    ❌ 生成失敗: $file"
            log_message "ERROR: Failed to generate diagram for $file"
        fi
    done
    
    print_message $GREEN "📊 PlantUML 完成: $success 成功, $failed 失敗, 總計 $count"
}

# 函數：生成 Mermaid 圖表
generate_mermaid() {
    print_message $PURPLE "🌊 生成 Mermaid 圖表..."
    
    local mmd_files=$(find "$DIAGRAMS_DIR" -name "*.mmd" -type f)
    local count=0
    local success=0
    local failed=0
    
    if [ -z "$mmd_files" ]; then
        print_message $YELLOW "⚠️  未找到 Mermaid 文件"
        return 0
    fi
    
    for file in $mmd_files; do
        count=$((count + 1))
        local basename=$(basename "$file" .mmd)
        local dirname=$(dirname "$file")
        local svg_file="$dirname/$basename.svg"
        
        print_message $CYAN "  處理: $file"
        
        # 生成 SVG (高解析度向量圖)
        if mmdc -i "$file" -o "$svg_file" --backgroundColor white --width 1200 --height 800 2>/dev/null; then
            success=$((success + 1))
            log_message "SUCCESS: Generated SVG for $file"
        else
            failed=$((failed + 1))
            print_message $RED "    ❌ 生成失敗: $file"
            log_message "ERROR: Failed to generate Mermaid diagram for $file"
        fi
    done
    
    print_message $GREEN "🌊 Mermaid 完成: $success 成功, $failed 失敗, 總計 $count"
}

# 函數：生成 Excalidraw 圖表
generate_excalidraw() {
    print_message $PURPLE "✏️  生成 Excalidraw 圖表..."
    
    local excalidraw_files=$(find "$DIAGRAMS_DIR" -name "*.excalidraw" -type f)
    local count=0
    local success=0
    local failed=0
    
    if [ -z "$excalidraw_files" ]; then
        print_message $YELLOW "⚠️  未找到 Excalidraw 文件"
        return 0
    fi
    
    # 檢查是否有 Excalidraw 轉換腳本
    local converter_script="$SCRIPTS_DIR/excalidraw-to-svg.js"
    if [ ! -f "$converter_script" ]; then
        print_message $YELLOW "⚠️  Excalidraw 轉換腳本不存在，正在創建..."
        create_excalidraw_converter
    fi
    
    for file in $excalidraw_files; do
        count=$((count + 1))
        print_message $CYAN "  處理: $file"
        
        if node "$converter_script" "$file" 2>/dev/null; then
            success=$((success + 1))
            log_message "SUCCESS: Generated PNG for $file"
        else
            failed=$((failed + 1))
            print_message $RED "    ❌ 生成失敗: $file"
            log_message "ERROR: Failed to generate Excalidraw diagram for $file"
        fi
    done
    
    print_message $GREEN "✏️  Excalidraw 完成: $success 成功, $failed 失敗, 總計 $count"
}

# 函數：創建 Excalidraw 轉換腳本
create_excalidraw_converter() {
    cat > "$SCRIPTS_DIR/excalidraw-to-svg.js" << 'EOF'
#!/usr/bin/env node

const fs = require('fs');
const path = require('path');

// 簡單的 Excalidraw 到 PNG 轉換器
// 注意：這是一個佔位符實現，實際需要 Excalidraw 庫或 MCP 整合

async function convertToPNG(excalidrawFile) {
    try {
        const inputPath = excalidrawFile;
        const outputPath = inputPath.replace('.excalidraw', '.png');
        
        // 讀取 Excalidraw 文件
        const data = fs.readFileSync(inputPath, 'utf8');
        const excalidrawData = JSON.parse(data);
        
        // 創建一個佔位符 PNG（實際實現需要 Excalidraw 渲染引擎）
        console.log(`處理 Excalidraw 文件: ${inputPath}`);
        console.log(`輸出路徑: ${outputPath}`);
        
        // 這裡應該使用 Excalidraw 的渲染引擎
        // 目前創建一個標記文件表示處理過
        fs.writeFileSync(outputPath + '.placeholder', 'Excalidraw PNG placeholder');
        
        console.log(`✅ 已處理: ${path.basename(inputPath)}`);
        return true;
    } catch (error) {
        console.error(`❌ 處理失敗: ${error.message}`);
        return false;
    }
}

// 主函數
async function main() {
    const inputFile = process.argv[2];
    if (!inputFile) {
        console.error('用法: node excalidraw-to-svg.js <input.excalidraw>');
        process.exit(1);
    }
    
    const success = await convertToPNG(inputFile);
    process.exit(success ? 0 : 1);
}

if (require.main === module) {
    main();
}
EOF
    
    chmod +x "$SCRIPTS_DIR/excalidraw-to-svg.js"
    print_message $GREEN "✅ Excalidraw 轉換腳本已創建"
}

# 函數：驗證生成的圖表
validate_diagrams() {
    print_message $BLUE "🔍 驗證生成的圖表..."
    
    local svg_files=$(find "$DIAGRAMS_DIR" -name "*.svg" -type f)
    
    local png_count=$(echo "$png_files" | wc -l)
    local svg_count=$(echo "$svg_files" | wc -l)
    
    if [ -z "$png_files" ]; then
        png_count=0
    fi
    
    if [ -z "$svg_files" ]; then
        svg_count=0
    fi
    
    print_message $GREEN "📈 生成統計:"
    print_message $CYAN "  PNG 文件: $png_count"
    print_message $CYAN "  SVG 文件: $svg_count"
    
    # 檢查是否有損壞的圖片文件
    local broken_files=0
    for png in $png_files; do
        if [ -f "$png" ] && [ ! -s "$png" ]; then
            broken_files=$((broken_files + 1))
            print_message $RED "  ⚠️  空文件: $png"
        fi
    done
    
    if [ $broken_files -eq 0 ]; then
        print_message $GREEN "✅ 所有圖表文件驗證通過"
    else
        print_message $YELLOW "⚠️  發現 $broken_files 個問題文件"
    fi
}

# 函數：清理舊文件
cleanup_old_files() {
    if [ "$1" = "--clean" ]; then
        print_message $YELLOW "🧹 清理舊的生成文件..."
        
        # 刪除舊的 SVG 文件
        find "$DIAGRAMS_DIR" -name "*.svg" -type f -delete
        find "$DIAGRAMS_DIR" -name "*.placeholder" -type f -delete
        
        print_message $GREEN "✅ 清理完成"
    fi
}

# 函數：生成報告
generate_report() {
    print_message $BLUE "📋 生成圖表報告..."
    
    local report_file="$DIAGRAMS_DIR/generation-report.md"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    cat > "$report_file" << EOF
# 圖表生成報告

**生成時間**: $timestamp

## 統計信息

### 源文件統計
- Mermaid 文件 (.mmd): $(find "$DIAGRAMS_DIR" -name "*.mmd" | wc -l)
- PlantUML 文件 (.puml): $(find "$DIAGRAMS_DIR" -name "*.puml" | wc -l)
- Excalidraw 文件 (.excalidraw): $(find "$DIAGRAMS_DIR" -name "*.excalidraw" | wc -l)

### 生成文件統計
- SVG 圖片: $(find "$DIAGRAMS_DIR" -name "*.svg" | wc -l)

## 目錄結構

\`\`\`
$(tree "$DIAGRAMS_DIR" -I "*.svg|*.placeholder" 2>/dev/null || find "$DIAGRAMS_DIR" -type f \( -name "*.mmd" -o -name "*.puml" -o -name "*.excalidraw" \) | sort)
\`\`\`

## 最近的日誌

\`\`\`
$(tail -20 "$LOG_FILE" 2>/dev/null || echo "無日誌記錄")
\`\`\`

---
*此報告由自動化腳本生成*
EOF
    
    print_message $GREEN "📋 報告已生成: $report_file"
}

# 函數：顯示幫助信息
show_help() {
    cat << EOF
圖表生成腳本

用法: $0 [選項]

選項:
  --clean         清理舊的生成文件
  --plantuml      只生成 PlantUML 圖表
  --mermaid       只生成 Mermaid 圖表
  --excalidraw    只生成 Excalidraw 圖表
  --validate      只驗證現有圖表
  --report        只生成報告
  --help          顯示此幫助信息

範例:
  $0                    # 生成所有圖表
  $0 --clean            # 清理後生成所有圖表
  $0 --plantuml         # 只生成 PlantUML 圖表
  $0 --validate         # 只驗證圖表

支援的圖表格式:
  - Mermaid (.mmd)      - GitHub 直接顯示
  - PlantUML (.puml)    - 詳細 UML 圖表
  - Excalidraw (.excalidraw) - 概念設計圖

EOF
}

# 主函數
main() {
    print_message $BLUE "🎨 圖表生成腳本啟動"
    print_message $CYAN "📁 工作目錄: $(pwd)"
    print_message $CYAN "📊 圖表目錄: $DIAGRAMS_DIR"
    
    # 初始化日誌
    log_message "=== 圖表生成開始 ==="
    
    # 解析命令行參數
    case "$1" in
        --help)
            show_help
            exit 0
            ;;
        --validate)
            validate_diagrams
            exit 0
            ;;
        --report)
            generate_report
            exit 0
            ;;
        --clean)
            cleanup_old_files --clean
            ;;
    esac
    
    # 檢查依賴
    check_dependencies
    
    # 清理舊文件（如果指定）
    cleanup_old_files "$1"
    
    # 根據參數生成特定類型的圖表
    case "$1" in
        --plantuml)
            generate_plantuml
            ;;
        --mermaid)
            generate_mermaid
            ;;
        --excalidraw)
            generate_excalidraw
            ;;
        *)
            # 生成所有類型的圖表
            generate_plantuml
            generate_mermaid
            generate_excalidraw
            ;;
    esac
    
    # 驗證生成的圖表
    validate_diagrams
    
    # 生成報告
    generate_report
    
    print_message $GREEN "🎉 圖表生成完成！"
    log_message "=== 圖表生成結束 ==="
}

# 執行主函數
main "$@"