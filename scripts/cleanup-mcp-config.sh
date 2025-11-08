#!/bin/bash

# MCP Configuration Cleanup Script
# This script helps clean up and optimize MCP server configurations

set -e

echo "🔍 MCP Configuration Cleanup Tool"
echo "=================================="
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Paths
GLOBAL_CONFIG="$HOME/.kiro/settings/mcp.json"
PROJECT_CONFIG=".kiro/settings/mcp.json"

# Check if configs exist
if [ ! -f "$GLOBAL_CONFIG" ]; then
    echo -e "${RED}❌ Global config not found: $GLOBAL_CONFIG${NC}"
    exit 1
fi

if [ ! -f "$PROJECT_CONFIG" ]; then
    echo -e "${RED}❌ Project config not found: $PROJECT_CONFIG${NC}"
    exit 1
fi

echo "📋 Current Configuration Status:"
echo ""

# Function to count servers
count_servers() {
    local config_file=$1
    local status=$2
    
    if [ "$status" == "active" ]; then
        jq '[.mcpServers | to_entries[] | select(.value.disabled == false or .value.disabled == null)] | length' "$config_file"
    else
        jq '[.mcpServers | to_entries[] | select(.value.disabled == true)] | length' "$config_file"
    fi
}

# Count servers
GLOBAL_ACTIVE=$(count_servers "$GLOBAL_CONFIG" "active")
GLOBAL_DISABLED=$(count_servers "$GLOBAL_CONFIG" "disabled")
PROJECT_ACTIVE=$(count_servers "$PROJECT_CONFIG" "active")
PROJECT_DISABLED=$(count_servers "$PROJECT_CONFIG" "disabled")

echo "Global Config:"
echo "  ✅ Active: $GLOBAL_ACTIVE"
echo "  ❌ Disabled: $GLOBAL_DISABLED"
echo ""
echo "Project Config:"
echo "  ✅ Active: $PROJECT_ACTIVE"
echo "  ❌ Disabled: $PROJECT_DISABLED"
echo ""

# Find duplicates
echo "🔍 Checking for duplicates..."
DUPLICATES=$(jq -r --slurpfile global "$GLOBAL_CONFIG" --slurpfile project "$PROJECT_CONFIG" '
  $global[0].mcpServers | keys as $global_keys |
  $project[0].mcpServers | keys as $project_keys |
  ($global_keys - ($global_keys - $project_keys))[]
' <<< '{}')

if [ -n "$DUPLICATES" ]; then
    echo -e "${YELLOW}⚠️  Found duplicate servers:${NC}"
    echo "$DUPLICATES" | while read -r server; do
        echo "  - $server"
    done
    echo ""
else
    echo -e "${GREEN}✅ No duplicates found${NC}"
    echo ""
fi

# Backup function
backup_config() {
    local config_file=$1
    local backup_file="${config_file}.backup.$(date +%Y%m%d_%H%M%S)"
    cp "$config_file" "$backup_file"
    echo -e "${GREEN}✅ Backup created: $backup_file${NC}"
}

# Menu
echo "🛠️  Available Actions:"
echo ""
echo "1. Backup both configurations"
echo "2. Remove duplicates from global config"
echo "3. Remove all disabled servers from global config"
echo "4. Show detailed server list"
echo "5. Test server connectivity"
echo "6. Exit"
echo ""

read -p "Select action (1-6): " action

case $action in
    1)
        echo ""
        echo "📦 Creating backups..."
        backup_config "$GLOBAL_CONFIG"
        backup_config "$PROJECT_CONFIG"
        echo ""
        echo -e "${GREEN}✅ Backups created successfully${NC}"
        ;;
    
    2)
        echo ""
        if [ -z "$DUPLICATES" ]; then
            echo -e "${GREEN}✅ No duplicates to remove${NC}"
        else
            echo "🗑️  Removing duplicates from global config..."
            backup_config "$GLOBAL_CONFIG"
            
            # Remove duplicates
            for server in $DUPLICATES; do
                echo "  Removing: $server"
                jq "del(.mcpServers[\"$server\"])" "$GLOBAL_CONFIG" > "${GLOBAL_CONFIG}.tmp"
                mv "${GLOBAL_CONFIG}.tmp" "$GLOBAL_CONFIG"
            done
            
            echo ""
            echo -e "${GREEN}✅ Duplicates removed from global config${NC}"
            echo -e "${YELLOW}⚠️  Restart Kiro to apply changes${NC}"
        fi
        ;;
    
    3)
        echo ""
        echo "🗑️  Removing disabled servers from global config..."
        backup_config "$GLOBAL_CONFIG"
        
        # Get list of disabled servers
        DISABLED_SERVERS=$(jq -r '.mcpServers | to_entries[] | select(.value.disabled == true) | .key' "$GLOBAL_CONFIG")
        
        if [ -z "$DISABLED_SERVERS" ]; then
            echo -e "${GREEN}✅ No disabled servers to remove${NC}"
        else
            echo "Disabled servers to remove:"
            echo "$DISABLED_SERVERS" | while read -r server; do
                echo "  - $server"
            done
            echo ""
            
            read -p "Confirm removal? (y/n): " confirm
            if [ "$confirm" == "y" ]; then
                for server in $DISABLED_SERVERS; do
                    jq "del(.mcpServers[\"$server\"])" "$GLOBAL_CONFIG" > "${GLOBAL_CONFIG}.tmp"
                    mv "${GLOBAL_CONFIG}.tmp" "$GLOBAL_CONFIG"
                done
                echo ""
                echo -e "${GREEN}✅ Disabled servers removed${NC}"
                echo -e "${YELLOW}⚠️  Restart Kiro to apply changes${NC}"
            else
                echo "Cancelled"
            fi
        fi
        ;;
    
    4)
        echo ""
        echo "📋 Global Config Servers:"
        jq -r '.mcpServers | to_entries[] | "\(.key): \(if .value.disabled then "❌ Disabled" else "✅ Active" end)"' "$GLOBAL_CONFIG"
        echo ""
        echo "📋 Project Config Servers:"
        jq -r '.mcpServers | to_entries[] | "\(.key): \(if .value.disabled then "❌ Disabled" else "✅ Active" end)"' "$PROJECT_CONFIG"
        ;;
    
    5)
        echo ""
        echo "🧪 Testing server connectivity..."
        echo ""
        
        # Test uvx
        if command -v uvx &> /dev/null; then
            echo -e "${GREEN}✅ uvx is installed${NC}"
        else
            echo -e "${RED}❌ uvx is not installed${NC}"
            echo "   Install with: curl -LsSf https://astral.sh/uv/install.sh | sh"
        fi
        
        # Test node
        if command -v node &> /dev/null; then
            echo -e "${GREEN}✅ node is installed ($(node --version))${NC}"
        else
            echo -e "${RED}❌ node is not installed${NC}"
        fi
        
        # Test excalidraw
        if [ -f "node_modules/mcp-excalidraw-server/src/index.js" ]; then
            echo -e "${GREEN}✅ excalidraw server is installed${NC}"
        else
            echo -e "${YELLOW}⚠️  excalidraw server not found${NC}"
            echo "   Install with: npm install mcp-excalidraw-server"
        fi
        
        echo ""
        echo "To test individual servers, use:"
        echo "  uvx mcp-server-time --help"
        echo "  uvx awslabs.aws-documentation-mcp-server@latest --help"
        ;;
    
    6)
        echo "Exiting..."
        exit 0
        ;;
    
    *)
        echo -e "${RED}Invalid option${NC}"
        exit 1
        ;;
esac

echo ""
echo "Done! 🎉"
