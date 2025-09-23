#!/bin/bash

# Fix Excalidraw MCP Server Path Script
# This script fixes the common path resolution issue with mcp-excalidraw-server

set -e

echo "🔧 Excalidraw MCP Path Fix Script"
echo "================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
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

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check if we're in the right directory
check_directory() {
    if [ ! -f "package.json" ] || [ ! -d ".kiro" ]; then
        print_error "This script must be run from the genai-demo project root directory"
        print_status "Please navigate to the project root and try again"
        exit 1
    fi
    
    if [ ! -f ".kiro/settings/mcp.json" ]; then
        print_error "MCP configuration file not found at .kiro/settings/mcp.json"
        exit 1
    fi
    
    if [ ! -f "node_modules/mcp-excalidraw-server/src/index.js" ]; then
        print_error "mcp-excalidraw-server not found. Please install it first:"
        print_status "npm install mcp-excalidraw-server"
        exit 1
    fi
}

# Fix the path in MCP configuration
fix_excalidraw_path() {
    print_status "Checking Excalidraw MCP configuration..."
    
    # Get current absolute path
    CURRENT_PATH=$(pwd)
    ABSOLUTE_PATH="$CURRENT_PATH/node_modules/mcp-excalidraw-server/src/index.js"
    
    # Create backup
    cp .kiro/settings/mcp.json .kiro/settings/mcp.json.backup.$(date +%Y%m%d_%H%M%S)
    print_status "Created backup of MCP configuration"
    
    if command_exists jq; then
        # Use jq for precise JSON manipulation
        print_status "Using jq to update configuration..."
        
        # Check current path
        CURRENT_CONFIG_PATH=$(cat .kiro/settings/mcp.json | jq -r '.mcpServers.excalidraw.args[0]' 2>/dev/null || echo "")
        
        if [[ "$CURRENT_CONFIG_PATH" == "$ABSOLUTE_PATH" ]]; then
            print_success "Configuration already uses correct absolute path"
            return 0
        fi
        
        # Update with absolute path
        cat .kiro/settings/mcp.json | jq --arg path "$ABSOLUTE_PATH" '.mcpServers.excalidraw.args[0] = $path' > .kiro/settings/mcp.json.tmp
        mv .kiro/settings/mcp.json.tmp .kiro/settings/mcp.json
        
        print_success "Updated excalidraw configuration with absolute path"
        
    else
        # Fallback: use sed for systems without jq
        print_warning "jq not found, using sed for text replacement..."
        
        # Replace relative path with absolute path
        if grep -q "node_modules/mcp-excalidraw-server/src/index.js" .kiro/settings/mcp.json; then
            sed -i.bak "s|node_modules/mcp-excalidraw-server/src/index.js|$ABSOLUTE_PATH|g" .kiro/settings/mcp.json
            print_success "Updated excalidraw configuration with absolute path"
        else
            print_warning "No relative path found to replace. Configuration might already be correct."
        fi
    fi
    
    print_status "New path: $ABSOLUTE_PATH"
}

# Test the configuration
test_configuration() {
    print_status "Testing Excalidraw MCP server..."
    
    ABSOLUTE_PATH="$(pwd)/node_modules/mcp-excalidraw-server/src/index.js"
    
    if echo '{"jsonrpc": "2.0", "id": 1, "method": "tools/list", "params": {}}' | node "$ABSOLUTE_PATH" >/dev/null 2>&1; then
        print_success "Excalidraw MCP server is working correctly"
        return 0
    else
        print_error "Excalidraw MCP server test failed"
        print_status "Please check the error messages above"
        return 1
    fi
}

# Show current configuration
show_configuration() {
    print_status "Current MCP configuration for Excalidraw:"
    
    if command_exists jq; then
        cat .kiro/settings/mcp.json | jq '.mcpServers.excalidraw'
    else
        grep -A 20 '"excalidraw"' .kiro/settings/mcp.json | head -25
    fi
}

# Main execution
main() {
    echo
    print_status "Starting Excalidraw MCP path fix..."
    echo
    
    check_directory
    echo
    
    fix_excalidraw_path
    echo
    
    show_configuration
    echo
    
    if test_configuration; then
        echo
        print_success "✅ Excalidraw MCP server path fix completed successfully!"
        print_status "Please restart Kiro IDE to apply the changes."
        echo
        print_status "Test the fix by asking Kiro:"
        print_status "  'Create a simple rectangle with text \"Path Fixed\"'"
    else
        echo
        print_error "❌ Path fix completed but server test failed."
        print_status "Please check the configuration manually or contact support."
    fi
    
    echo
    print_status "Backup files created with timestamp suffix for safety."
}

# Run main function
main "$@"