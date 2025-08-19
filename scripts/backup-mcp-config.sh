#!/bin/bash

# MCP 配置備份和分享腳本

echo "📋 MCP 配置備份和分享工具"
echo "========================="

# 創建備份目錄
BACKUP_DIR="./mcp-configs-backup"
mkdir -p "$BACKUP_DIR"

echo "🔍 搜索 MCP 配置文件..."

# 主要配置文件位置
MAIN_CONFIGS=(
    "$HOME/.amazonq/mcp-config.json"
    "$HOME/.kiro/settings/mcp.json"
    "$HOME/.config/mcp/config.json"
)

# 備份主要配置文件
for config in "${MAIN_CONFIGS[@]}"; do
    if [ -f "$config" ]; then
        filename=$(basename "$config")
        dirname=$(basename "$(dirname "$config")")
        backup_name="${dirname}_${filename}"
        
        echo "📄 備份: $config -> $BACKUP_DIR/$backup_name"
        cp "$config" "$BACKUP_DIR/$backup_name"
        
        # 顯示配置摘要
        echo "   📊 服務器數量: $(cat "$config" | jq '.mcpServers | length' 2>/dev/null || echo "無法解析")"
        echo "   🕐 修改時間: $(stat -f "%Sm" "$config" 2>/dev/null || stat -c "%y" "$config" 2>/dev/null || echo "未知")"
        echo ""
    fi
done

# 搜索其他 MCP 相關文件
echo "🔍 搜索其他 MCP 相關文件..."
OTHER_CONFIGS=$(find "$HOME" -name "*mcp*config*" -o -name "mcp.json" 2>/dev/null | grep -v "$BACKUP_DIR" | head -10)

if [ ! -z "$OTHER_CONFIGS" ]; then
    echo "找到其他配置文件:"
    echo "$OTHER_CONFIGS"
    
    # 詢問是否備份其他文件
    echo ""
    echo "是否要備份這些文件？(y/N)"
    read -r response
    if [[ "$response" =~ ^[Yy]$ ]]; then
        echo "$OTHER_CONFIGS" | while read -r config; do
            if [ -f "$config" ]; then
                relative_path=$(echo "$config" | sed "s|$HOME/||" | tr '/' '_')
                backup_name="other_${relative_path}"
                echo "📄 備份: $config -> $BACKUP_DIR/$backup_name"
                cp "$config" "$BACKUP_DIR/$backup_name"
            fi
        done
    fi
fi

# 創建配置摘要
echo "📝 創建配置摘要..."
SUMMARY_FILE="$BACKUP_DIR/mcp-config-summary.md"

cat > "$SUMMARY_FILE" << EOF
# MCP 配置摘要

生成時間: $(date)

## 配置文件位置

EOF

for config in "${MAIN_CONFIGS[@]}"; do
    if [ -f "$config" ]; then
        echo "### $config" >> "$SUMMARY_FILE"
        echo "" >> "$SUMMARY_FILE"
        echo "- 文件大小: $(du -h "$config" | cut -f1)" >> "$SUMMARY_FILE"
        echo "- 修改時間: $(stat -f "%Sm" "$config" 2>/dev/null || stat -c "%y" "$config" 2>/dev/null)" >> "$SUMMARY_FILE"
        
        if command -v jq &> /dev/null; then
            SERVER_COUNT=$(cat "$config" | jq '.mcpServers | length' 2>/dev/null || echo "0")
            echo "- 服務器數量: $SERVER_COUNT" >> "$SUMMARY_FILE"
            
            echo "- 服務器列表:" >> "$SUMMARY_FILE"
            cat "$config" | jq -r '.mcpServers | keys[]' 2>/dev/null | sed 's/^/  - /' >> "$SUMMARY_FILE" || echo "  - 無法解析" >> "$SUMMARY_FILE"
        fi
        echo "" >> "$SUMMARY_FILE"
    fi
done

echo "✅ 備份完成！"
echo ""
echo "📁 備份目錄: $BACKUP_DIR"
echo "📄 配置摘要: $SUMMARY_FILE"
echo ""
echo "📋 備份的文件:"
ls -la "$BACKUP_DIR"

echo ""
echo "🔗 分享配置文件:"
echo "您可以將 $BACKUP_DIR 目錄中的文件分享給其他人"
echo "或者直接查看主配置文件:"
echo ""

# 顯示主配置文件內容
MAIN_CONFIG="$HOME/.amazonq/mcp-config.json"
if [ -f "$MAIN_CONFIG" ]; then
    echo "📄 Amazon Q MCP 配置內容:"
    echo "=========================="
    cat "$MAIN_CONFIG"
fi