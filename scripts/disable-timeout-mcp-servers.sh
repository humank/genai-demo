#!/bin/bash

# Disable Timeout MCP Servers Script
# Disables AWS MCP servers that are experiencing connection timeouts

set -e

echo "🔧 Disabling Timeout AWS MCP Servers"
echo "====================================="
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Backup
BACKUP_FILE="$HOME/.kiro/settings/mcp.json.backup.$(date +%Y%m%d_%H%M%S)"
cp ~/.kiro/settings/mcp.json "$BACKUP_FILE"
echo -e "${GREEN}✅ Backup created: $BACKUP_FILE${NC}"
echo ""

# Disable problematic servers
echo "🔄 Disabling problematic servers..."
jq '.mcpServers["awslabs.lambda-mcp-server"].disabled = true |
    .mcpServers["awslabs.iam-mcp-server"].disabled = true |
    .mcpServers["awslabs.aws-pricing-mcp-server"].disabled = true' \
    ~/.kiro/settings/mcp.json > ~/.kiro/settings/mcp.json.tmp

mv ~/.kiro/settings/mcp.json.tmp ~/.kiro/settings/mcp.json

echo -e "${GREEN}✅ Disabled:${NC}"
echo "   - awslabs.lambda-mcp-server"
echo "   - awslabs.iam-mcp-server"
echo "   - awslabs.aws-pricing-mcp-server"
echo ""

# Show remaining active servers
echo "📋 Remaining Active Servers:"
jq -r '.mcpServers | to_entries[] | select(.value.disabled == false or .value.disabled == null) | "   ✅ \(.key)"' ~/.kiro/settings/mcp.json
echo ""

echo -e "${YELLOW}⚠️  Next Steps:${NC}"
echo "1. Restart Kiro to apply changes"
echo "2. Verify other servers work correctly"
echo "3. (Optional) Pre-install packages later:"
echo "   uvx awslabs.lambda-mcp-server@latest --help"
echo "   uvx awslabs.iam-mcp-server@latest --help"
echo "   uvx awslabs.aws-pricing-mcp-server@latest --help"
echo ""
echo "Done! 🎉"
