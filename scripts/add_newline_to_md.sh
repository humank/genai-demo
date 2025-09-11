#!/bin/bash

# 批次為所有 Markdown 文件添加頁尾換行符號
# 作者: Amazon Q
# 日期: 2025-08-21

echo "開始處理專案中的所有 Markdown 文件..."

# 計數器
processed_count=0
skipped_count=0

# 查找所有 .md 文件並處理
while IFS= read -r -d '' file; do
    echo "處理文件: $file"
    
    # 檢查文件是否存在且可讀
    if [[ ! -r "$file" ]]; then
        echo "  ⚠️  無法讀取文件，跳過"
        ((skipped_count++))
        continue
    fi
    
    # 檢查文件是否以換行符結尾
    if [[ -s "$file" ]] && [[ $(tail -c1 "$file" | wc -l) -eq 0 ]]; then
        # 文件不以換行符結尾，添加一個換行符
        echo "" >> "$file"
        echo "  ✅ 已添加換行符"
        ((processed_count++))
    else
        echo "  ℹ️  文件已以換行符結尾，跳過"
        ((skipped_count++))
    fi
    
done < <(find /Users/yikaikao/git/genai-demo -name "*.md" -type f -print0)

echo ""
echo "處理完成！"
echo "已處理文件數: $processed_count"
echo "跳過文件數: $skipped_count"
echo "總文件數: $((processed_count + skipped_count))"
