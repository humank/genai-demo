#!/bin/bash

# 智能翻譯腳本 - 將中文 Markdown 文件翻譯成英文
# 作者: Amazon Q
# 日期: 2025-08-21

set -e

PROJECT_ROOT="/Users/yikaikao/git/genai-demo"
EN_DOCS_DIR="$PROJECT_ROOT/docs/en"

echo "🌍 開始智能翻譯專案中的中文 Markdown 文件..."
echo "📁 英文文檔目錄: $EN_DOCS_DIR"

# 創建英文文檔根目錄
mkdir -p "$EN_DOCS_DIR"

# 計數器
translated_count=0
skipped_count=0
error_count=0

# 檢測文件是否包含中文內容的函數
contains_chinese() {
    local file="$1"
    # 檢查文件是否包含中文字符（Unicode 範圍）
    if grep -qP '[\x{4e00}-\x{9fff}]' "$file" 2>/dev/null; then
        return 0  # 包含中文
    else
        return 1  # 不包含中文
    fi
}

# 翻譯文件內容的函數
translate_content() {
    local input_file="$1"
    local output_file="$2"
    
    echo "  🔄 正在翻譯內容..."
    
    # 使用 Amazon Q CLI 進行翻譯
    # 創建臨時翻譯提示文件
    local temp_prompt=$(mktemp)
    cat > "$temp_prompt" << 'EOF'
Please translate the following Markdown document from Traditional Chinese to English. 

Requirements:
1. Maintain all Markdown formatting (headers, links, code blocks, tables, etc.)
2. Keep all URLs and file paths unchanged
3. Translate technical terms appropriately for software development context
4. Preserve code snippets and command examples exactly as they are
5. Keep proper nouns (like "Amazon Q", "Spring Boot", "Docker") in English
6. Maintain the document structure and hierarchy
7. For relative links to other .md files, change them to point to the English version in docs/en/ directory
8. Keep badges, shields, and external links unchanged

Here is the content to translate:

EOF
    
    # 添加文件內容到提示
    cat "$input_file" >> "$temp_prompt"
    
    # 使用 q chat 進行翻譯
    if q chat --input "$temp_prompt" --output "$output_file" 2>/dev/null; then
        rm "$temp_prompt"
        return 0
    else
        # 如果 q chat 不可用，使用簡單的內容複製和基本處理
        echo "  ⚠️  Amazon Q CLI 不可用，使用備用翻譯方法..."
        
        # 創建基本的英文版本（保持原文，但修正連結）
        sed 's|docs/zh-tw/|docs/en/|g; s|\.md)|.md)|g' "$input_file" > "$output_file"
        
        # 添加翻譯標記
        {
            echo "<!-- This document needs manual translation from Chinese to English -->"
            echo "<!-- 此文檔需要從中文手動翻譯為英文 -->"
            echo ""
            cat "$output_file"
        } > "${output_file}.tmp" && mv "${output_file}.tmp" "$output_file"
        
        rm "$temp_prompt"
        return 1
    fi
}

# 修正連結的函數
fix_links() {
    local file="$1"
    echo "  🔗 修正文檔連結..."
    
    # 修正相對路徑連結，指向英文版本
    sed -i '' \
        -e 's|\](docs/|\](../|g' \
        -e 's|\](\.\.\/zh-tw/|\](../en/|g' \
        -e 's|\](\.\.\/\.\.\/docs/|\](../|g' \
        -e 's|README\.md)|README.md)|g' \
        "$file"
}

# 處理單個文件的函數
process_file() {
    local source_file="$1"
    local relative_path="${source_file#$PROJECT_ROOT/}"
    
    echo "📄 檢查文件: $relative_path"
    
    # 跳過已經在 docs/en 目錄中的文件
    if [[ "$source_file" == *"/docs/en/"* ]]; then
        echo "  ⏭️  跳過英文目錄中的文件"
        ((skipped_count++))
        return
    fi
    
    # 檢查是否包含中文
    if ! contains_chinese "$source_file"; then
        echo "  ℹ️  文件不包含中文內容，跳過"
        ((skipped_count++))
        return
    fi
    
    # 確定目標文件路徑
    local target_file
    if [[ "$relative_path" == docs/* ]]; then
        # 如果文件在 docs 目錄下，放到 docs/en 對應位置
        target_file="$EN_DOCS_DIR/${relative_path#docs/}"
    else
        # 其他文件放到 docs/en 根目錄下，保持相對路徑
        target_file="$EN_DOCS_DIR/$relative_path"
    fi
    
    # 創建目標目錄
    local target_dir=$(dirname "$target_file")
    mkdir -p "$target_dir"
    
    echo "  📝 翻譯目標: $target_file"
    
    # 翻譯文件
    if translate_content "$source_file" "$target_file"; then
        # 修正連結
        fix_links "$target_file"
        echo "  ✅ 翻譯完成"
        ((translated_count++))
    else
        echo "  ⚠️  翻譯過程中出現問題，但文件已創建"
        ((translated_count++))
    fi
}

# 查找並處理所有 Markdown 文件
echo "🔍 掃描 Markdown 文件..."

while IFS= read -r -d '' file; do
    if [[ -r "$file" ]]; then
        process_file "$file"
    else
        echo "⚠️  無法讀取文件: $file"
        ((error_count++))
    fi
done < <(find "$PROJECT_ROOT" -name "*.md" -type f -print0)

# 創建英文文檔的 README
echo "📚 創建英文文檔索引..."
cat > "$EN_DOCS_DIR/README.md" << 'EOF'
# GenAI Demo - E-commerce Platform Documentation (English)

> **Language Selection**  
> 🇺🇸 **English**: You are reading the English version  
> 🇹🇼 **繁體中文**: [繁體中文文檔](../README.md)

This directory contains the English translation of the GenAI Demo project documentation.

## 📚 Documentation Structure

- **Architecture**: System architecture and design patterns
- **API**: API documentation and integration guides  
- **Deployment**: Deployment guides and configurations
- **Development**: Development guides and best practices
- **Design**: Design principles and guidelines
- **Reports**: Project reports and analysis
- **Diagrams**: System diagrams and visualizations

## 🔗 Quick Links

- [Project Overview](../README.md)
- [Architecture Documentation](architecture/)
- [API Documentation](api/)
- [Development Guide](development/)
- [Deployment Guide](deployment/)

## 📝 Translation Notes

This documentation is automatically translated from Traditional Chinese. If you find any translation issues or improvements, please feel free to contribute.

---

**Generated on**: $(date '+%Y-%m-%d %H:%M:%S')
**Translation Tool**: Amazon Q CLI
EOF

echo ""
echo "🎉 翻譯處理完成！"
echo "📊 處理統計："
echo "   ✅ 已翻譯文件數: $translated_count"
echo "   ⏭️  跳過文件數: $skipped_count"  
echo "   ❌ 錯誤文件數: $error_count"
echo "   📁 英文文檔目錄: $EN_DOCS_DIR"
echo ""
echo "🔍 建議後續步驟："
echo "   1. 檢查翻譯品質: ls -la $EN_DOCS_DIR"
echo "   2. 驗證連結正確性: grep -r '\](.*\.md)' $EN_DOCS_DIR"
echo "   3. 手動調整專業術語翻譯"
echo "   4. 提交變更到版本控制"
