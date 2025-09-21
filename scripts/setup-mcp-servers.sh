#!/bin/bash

# MCP Servers Setup Script
# This script helps new team members quickly set up all required MCP servers

set -e

echo "🚀 MCP Servers Setup Script"
echo "=========================="

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

# Check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites..."
    
    # Check Node.js
    if command_exists node; then
        NODE_VERSION=$(node --version)
        print_success "Node.js found: $NODE_VERSION"
        
        # Check if Node.js version is >= 16
        NODE_MAJOR_VERSION=$(echo $NODE_VERSION | cut -d'.' -f1 | sed 's/v//')
        if [ "$NODE_MAJOR_VERSION" -lt 16 ]; then
            print_error "Node.js version 16 or higher is required. Current version: $NODE_VERSION"
            print_status "Please install Node.js from https://nodejs.org/"
            exit 1
        fi
    else
        print_error "Node.js not found. Please install Node.js from https://nodejs.org/"
        exit 1
    fi
    
    # Check NPM
    if command_exists npm; then
        NPM_VERSION=$(npm --version)
        print_success "NPM found: $NPM_VERSION"
    else
        print_error "NPM not found. Please install NPM."
        exit 1
    fi
    
    # Check UV
    if command_exists uv; then
        UV_VERSION=$(uv --version)
        print_success "UV found: $UV_VERSION"
    else
        print_warning "UV not found. Installing UV..."
        if command_exists brew; then
            brew install uv
        elif command_exists pip; then
            pip install uv
        else
            print_error "Cannot install UV. Please install it manually:"
            print_status "  macOS: brew install uv"
            print_status "  Other: pip install uv"
            exit 1
        fi
    fi
    
    # Check AWS CLI (optional)
    if command_exists aws; then
        AWS_VERSION=$(aws --version 2>&1 | cut -d' ' -f1)
        print_success "AWS CLI found: $AWS_VERSION"
    else
        print_warning "AWS CLI not found. Some MCP servers may not work without AWS credentials."
        print_status "Install AWS CLI from: https://aws.amazon.com/cli/"
    fi
}

# Install Excalidraw MCP Server
install_excalidraw_mcp() {
    print_status "Installing Excalidraw MCP Server..."
    
    if [ -f "package.json" ]; then
        npm install mcp-excalidraw-server
        
        # Verify installation
        if [ -f "node_modules/mcp-excalidraw-server/src/index.js" ]; then
            print_success "Excalidraw MCP Server installed successfully"
        else
            print_error "Excalidraw MCP Server installation failed"
            exit 1
        fi
    else
        print_error "package.json not found. Please run this script from the project root directory."
        exit 1
    fi
}

# Test MCP servers
test_mcp_servers() {
    print_status "Testing MCP servers..."
    
    # Test Excalidraw MCP Server
    print_status "Testing Excalidraw MCP Server..."
    if echo '{"jsonrpc": "2.0", "id": 1, "method": "tools/list", "params": {}}' | node node_modules/mcp-excalidraw-server/src/index.js >/dev/null 2>&1; then
        print_success "Excalidraw MCP Server is working"
    else
        print_error "Excalidraw MCP Server test failed"
    fi
    
    # Test Time MCP Server
    print_status "Testing Time MCP Server..."
    if uvx mcp-server-time --help >/dev/null 2>&1; then
        print_success "Time MCP Server is working"
    else
        print_warning "Time MCP Server test failed (this is normal if UV cache is empty)"
    fi
    
    # Test AWS MCP Servers (if AWS CLI is available)
    if command_exists aws; then
        print_status "Testing AWS MCP Servers..."
        
        # Test AWS Docs MCP
        if uvx awslabs.aws-documentation-mcp-server@latest --help >/dev/null 2>&1; then
            print_success "AWS Documentation MCP Server is working"
        else
            print_warning "AWS Documentation MCP Server test failed"
        fi
        
        # Test AWS CDK MCP
        if uvx awslabs.cdk-mcp-server@latest --help >/dev/null 2>&1; then
            print_success "AWS CDK MCP Server is working"
        else
            print_warning "AWS CDK MCP Server test failed"
        fi
        
        # Test AWS Pricing MCP
        if uvx awslabs.aws-pricing-mcp-server@latest --help >/dev/null 2>&1; then
            print_success "AWS Pricing MCP Server is working"
        else
            print_warning "AWS Pricing MCP Server test failed"
        fi
    else
        print_warning "Skipping AWS MCP Server tests (AWS CLI not found)"
    fi
}

# Verify and fix MCP configuration
verify_mcp_config() {
    print_status "Verifying MCP configuration..."
    
    if [ -f ".kiro/settings/mcp.json" ]; then
        print_success "MCP configuration file found"
        
        # Get current absolute path
        CURRENT_PATH=$(pwd)
        
        # Check if excalidraw server uses relative path and fix it
        if command_exists jq; then
            EXCALIDRAW_PATH=$(cat .kiro/settings/mcp.json | jq -r '.mcpServers.excalidraw.args[0]' 2>/dev/null || echo "")
            
            if [[ "$EXCALIDRAW_PATH" == "node_modules/mcp-excalidraw-server/src/index.js" ]]; then
                print_warning "Found relative path in excalidraw configuration. Fixing..."
                
                # Create backup
                cp .kiro/settings/mcp.json .kiro/settings/mcp.json.backup
                
                # Update with absolute path
                ABSOLUTE_PATH="$CURRENT_PATH/node_modules/mcp-excalidraw-server/src/index.js"
                cat .kiro/settings/mcp.json | jq --arg path "$ABSOLUTE_PATH" '.mcpServers.excalidraw.args[0] = $path' > .kiro/settings/mcp.json.tmp
                mv .kiro/settings/mcp.json.tmp .kiro/settings/mcp.json
                
                print_success "Updated excalidraw configuration with absolute path: $ABSOLUTE_PATH"
            fi
            
            SERVERS=$(cat .kiro/settings/mcp.json | jq -r '.mcpServers | keys[]' 2>/dev/null || echo "")
            if [ -n "$SERVERS" ]; then
                print_success "Configured MCP servers:"
                echo "$SERVERS" | while read server; do
                    echo "  - $server"
                done
            else
                print_warning "No MCP servers found in configuration"
            fi
        else
            print_warning "jq not found. Cannot parse MCP configuration."
            print_status "Install jq to get detailed configuration info: brew install jq"
            
            # Manual fix for systems without jq
            if grep -q "node_modules/mcp-excalidraw-server/src/index.js" .kiro/settings/mcp.json; then
                print_warning "Detected relative path in configuration. Manual fix required."
                print_status "Please update .kiro/settings/mcp.json:"
                print_status "Replace 'node_modules/mcp-excalidraw-server/src/index.js'"
                print_status "With '$CURRENT_PATH/node_modules/mcp-excalidraw-server/src/index.js'"
            fi
        fi
    else
        print_error "MCP configuration file not found at .kiro/settings/mcp.json"
        print_status "Please ensure you're running this script from the project root directory"
        exit 1
    fi
}

# Setup AWS credentials (optional)
setup_aws_credentials() {
    if command_exists aws; then
        print_status "Checking AWS credentials..."
        
        if aws sts get-caller-identity >/dev/null 2>&1; then
            print_success "AWS credentials are configured"
        else
            print_warning "AWS credentials not configured"
            read -p "Do you want to configure AWS credentials now? (y/N): " -n 1 -r
            echo
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                print_status "Configuring AWS credentials..."
                aws configure --profile kim-sso
                
                print_status "Setting environment variables..."
                echo "export AWS_PROFILE=kim-sso" >> ~/.bashrc
                echo "export AWS_REGION=ap-northeast-1" >> ~/.bashrc
                echo "export AWS_PROFILE=kim-sso" >> ~/.zshrc
                echo "export AWS_REGION=ap-northeast-1" >> ~/.zshrc
                
                print_success "AWS credentials configured"
                print_status "Please restart your terminal or run:"
                print_status "  export AWS_PROFILE=kim-sso"
                print_status "  export AWS_REGION=ap-northeast-1"
            else
                print_warning "Skipping AWS credentials setup"
            fi
        fi
    fi
}

# Generate setup report
generate_report() {
    print_status "Generating setup report..."
    
    REPORT_FILE="mcp-setup-report.txt"
    
    cat > "$REPORT_FILE" << EOF
MCP Servers Setup Report
========================
Date: $(date)
User: $(whoami)
Directory: $(pwd)

Prerequisites:
- Node.js: $(node --version 2>/dev/null || echo "Not found")
- NPM: $(npm --version 2>/dev/null || echo "Not found")
- UV: $(uv --version 2>/dev/null || echo "Not found")
- AWS CLI: $(aws --version 2>&1 | head -1 || echo "Not found")

Installed Packages:
- mcp-excalidraw-server: $([ -f "node_modules/mcp-excalidraw-server/package.json" ] && echo "Installed" || echo "Not found")

MCP Configuration:
- Configuration file: $([ -f ".kiro/settings/mcp.json" ] && echo "Found" || echo "Not found")

AWS Configuration:
- AWS Profile: ${AWS_PROFILE:-"Not set"}
- AWS Region: ${AWS_REGION:-"Not set"}
- AWS Credentials: $(aws sts get-caller-identity >/dev/null 2>&1 && echo "Configured" || echo "Not configured")

Next Steps:
1. Restart Kiro IDE to load MCP servers
2. Test MCP integration in Kiro IDE:
   - "Create a simple rectangle with text 'Hello MCP'"
   - "What time is it in Tokyo?"
   - "Search AWS documentation for Lambda best practices"

For troubleshooting, see:
- docs/mcp/excalidraw-mcp-usage-guide.md
- infrastructure/docs/MCP_INTEGRATION_GUIDE.md
EOF

    print_success "Setup report generated: $REPORT_FILE"
}

# Main execution
main() {
    echo
    print_status "Starting MCP servers setup..."
    echo
    
    check_prerequisites
    echo
    
    install_excalidraw_mcp
    echo
    
    verify_mcp_config
    echo
    
    test_mcp_servers
    echo
    
    setup_aws_credentials
    echo
    
    generate_report
    echo
    
    print_success "MCP servers setup completed!"
    print_status "Please restart Kiro IDE to load the MCP servers."
    echo
    print_status "Test the setup by asking Kiro:"
    print_status "  'Create a simple rectangle with text \"Hello MCP\"'"
    print_status "  'What time is it in Tokyo?'"
    echo
    print_status "For detailed usage instructions, see:"
    print_status "  docs/mcp/excalidraw-mcp-usage-guide.md"
    print_status "  infrastructure/docs/MCP_INTEGRATION_GUIDE.md"
}

# Run main function
main "$@"