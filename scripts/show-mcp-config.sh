#!/bin/bash

# 顯示 MCP 配置文件腳本

echo "🔍 查找 MCP 配置文件..."
echo "=========================="

# 常見的 MCP 配置文件位置
MCP_LOCATIONS=(
    "$HOME/.amazonq/mcp-config.json"
    "$HOME/.kiro/settings/mcp.json"
    "$HOME/.config/mcp/config.json"
    "$HOME/Library/Application Support/Amazon Q/mcp-config.json"
    "$HOME/Library/Application Support/Kiro/mcp.json"
)

echo "📍 檢查常見位置..."
for location in "${MCP_LOCATIONS[@]}"; do
    if [ -f "$location" ]; then
        echo "✅ 找到配置文件: $location"
        echo "📄 文件大小: $(du -h "$location" | cut -f1)"
        echo "🕐 修改時間: $(stat -f "%Sm" "$location")"
        echo ""
    else
        echo "❌ 未找到: $location"
    fi
done

echo ""
echo "🔍 搜索所有 MCP 相關文件..."
find "$HOME" -name "*mcp*config*" -o -name "mcp.json" -o -name ".mcp*" 2>/dev/null | head -20

echo ""
echo "📋 顯示主要配置文件內容..."
echo "================================"

# 顯示主要配置文件
MAIN_CONFIG="$HOME/.amazonq/mcp-config.json"
if [ -f "$MAIN_CONFIG" ]; then
    echo "📄 主配置文件: $MAIN_CONFIG"
    echo "內容:"
    cat "$MAIN_CONFIG" | jq . 2>/dev/null || cat "$MAIN_CONFIG"
else
    echo "⚠️  未找到主配置文件"
    
    # 嘗試其他位置
    for location in "${MCP_LOCATIONS[@]}"; do
        if [ -f "$location" ]; then
            echo "📄 使用配置文件: $location"
            echo "內容:"
            cat "$location" | jq . 2>/dev/null || cat "$location"
            break
        fi
    done
fi

echo ""
echo "🔧 MCP 服務器統計..."
echo "==================="

if [ -f "$MAIN_CONFIG" ]; then
    SERVER_COUNT=$(cat "$MAIN_CONFIG" | jq '.mcpServers | length' 2>/dev/null || echo "無法解析")
    echo "配置的服務器數量: $SERVER_COUNT"
    
    echo ""
    echo "服務器列表:"
    cat "$MAIN_CONFIG" | jq -r '.mcpServers | keys[]' 2>/dev/null || echo "無法解析服務器列表"
fi