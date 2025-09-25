# Kiro IDE Setup Configuration Guide

This guide helps you quickly set up Kiro IDE with all necessary configurations for terminal integration, MCP servers, and development tools.

## Prerequisites

### 1. Install Required Tools

```bash
# Install Homebrew (if not already installed)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install Python and uv (for MCP servers)
brew install python
curl -LsSf https://astral.sh/uv/install.sh | sh

# Install Node.js (if needed)
brew install node

# Install Git (if not already installed)
brew install git
```

### 2. Install Kiro IDE

Download and install Kiro IDE from the official website, then ensure the CLI is available:

```bash
# Verify Kiro CLI is installed
which kiro
# Should output: /usr/local/bin/kiro
```

## Shell Integration Setup

### 1. Configure Zsh Integration

Add the following to your `~/.zshrc`:

```bash
# Kiro IDE Terminal Integration
[[ "$TERM_PROGRAM" == "kiro" ]] && . "$(kiro --locate-shell-integration-path zsh)"
```

### 2. Configure Bash Integration (Optional)

Add the following to your `~/.bashrc`:

```bash
# Kiro IDE Terminal Integration
[[ "$TERM_PROGRAM" == "kiro" ]] && . "$(kiro --locate-shell-integration-path bash)"
```

### 3. Reload Shell Configuration

```bash
# For zsh
source ~/.zshrc

# For bash
source ~/.bashrc
```

## AWS Configuration

### 1. Install AWS CLI

```bash
brew install awscli
```

### 2. Configure AWS Profile

Add to your shell configuration (`~/.zshrc` or `~/.bashrc`):

```bash
# AWS Configuration
export AWS_PROFILE="your-sso-profile-name"
export AWS_CLI_BROWSER="open -na 'Google Chrome' --args --incognito"
export AWS_SSO_START_URL="https://your-sso-url.awsapps.com/start"
```

### 3. Configure AWS SSO

```bash
aws configure sso
```

## MCP (Model Context Protocol) Configuration

### 1. Create MCP Configuration Directory

```bash
mkdir -p ~/.kiro/settings
```

### 2. Create Global MCP Configuration

Create `~/.kiro/settings/mcp.json` with the following content:

```json
{
  "mcpServers": {
    "fetch": {
      "command": "uvx",
      "args": [
        "mcp-server-fetch"
      ],
      "env": {},
      "disabled": false,
      "autoApprove": [
        "fetch"
      ]
    },
    "aws-docs": {
      "command": "uvx",
      "args": [
        "awslabs.aws-documentation-mcp-server@latest"
      ],
      "env": {
        "FASTMCP_LOG_LEVEL": "ERROR"
      },
      "disabled": false,
      "autoApprove": [
        "search_aws_documentation",
        "get_aws_service_info",
        "search_documentation",
        "read_documentation"
      ]
    },
    "awslabs.cdk-mcp-server": {
      "command": "uvx",
      "args": [
        "awslabs.cdk-mcp-server@latest"
      ],
      "env": {
        "FASTMCP_LOG_LEVEL": "ERROR"
      },
      "disabled": false,
      "autoApprove": [
        "CDKGeneralGuidance",
        "ExplainCDKNagRule",
        "CheckCDKNagSuppressions"
      ]
    },
    "awslabs.aws-pricing-mcp-server": {
      "command": "uvx",
      "args": [
        "awslabs.aws-pricing-mcp-server@latest"
      ],
      "env": {
        "FASTMCP_LOG_LEVEL": "ERROR"
      },
      "disabled": false,
      "autoApprove": [
        "analyze_cdk_project",
        "get_pricing",
        "generate_cost_report"
      ]
    },
    "sqlite": {
      "command": "uvx",
      "args": [
        "mcp-server-sqlite"
      ],
      "env": {},
      "disabled": false,
      "autoApprove": [
        "query",
        "list_tables",
        "describe_table",
        "read_query"
      ]
    },
    "kubernetes": {
      "command": "uvx",
      "args": [
        "mcp-server-kubernetes"
      ],
      "env": {},
      "disabled": false,
      "autoApprove": [
        "get_pods",
        "get_services",
        "get_deployments",
        "describe_resource"
      ]
    },
    "docker": {
      "command": "uvx",
      "args": [
        "mcp-server-docker"
      ],
      "env": {},
      "disabled": false,
      "autoApprove": [
        "list_containers",
        "list_images",
        "container_logs"
      ]
    },
    "time": {
      "command": "uvx",
      "args": [
        "mcp-server-time"
      ],
      "env": {},
      "disabled": false,
      "autoApprove": [
        "get_current_time",
        "get_timezone",
        "convert_time",
        "format_time",
        "calculate_time_difference"
      ]
    },
    "awslabs.lambda-mcp-server": {
      "command": "uvx",
      "args": [
        "awslabs.lambda-mcp-server@latest"
      ],
      "env": {
        "FASTMCP_LOG_LEVEL": "ERROR",
        "AWS_PROFILE": "your-sso-profile-name",
        "AWS_REGION": "us-east-1"
      },
      "disabled": false,
      "autoApprove": [
        "list_functions",
        "invoke_function",
        "get_function_info",
        "update_function_code"
      ]
    },
    "awslabs.iam-mcp-server": {
      "command": "uvx",
      "args": [
        "awslabs.iam-mcp-server@latest",
        "--readonly"
      ],
      "env": {
        "FASTMCP_LOG_LEVEL": "ERROR",
        "AWS_PROFILE": "your-sso-profile-name",
        "AWS_REGION": "us-east-1"
      },
      "disabled": false,
      "autoApprove": [
        "list_users",
        "list_roles",
        "get_user_policies",
        "get_role_policies"
      ]
    }
  }
}
```

**Important**: Replace `"your-sso-profile-name"` with your actual AWS SSO profile name.

### 3. Optional: AWS Services (Enable as needed)

Add these to your MCP configuration if you need additional AWS services:

```json
"awslabs.ec2-mcp-server": {
  "command": "uvx",
  "args": [
    "awslabs.ec2-mcp-server@latest"
  ],
  "env": {
    "FASTMCP_LOG_LEVEL": "ERROR",
    "AWS_PROFILE": "your-sso-profile-name",
    "AWS_REGION": "us-east-1"
  },
  "disabled": false,
  "autoApprove": [
    "list_instances",
    "describe_instance",
    "start_instance",
    "stop_instance",
    "get_instance_logs"
  ]
},
"awslabs.terraform-mcp-server": {
  "command": "uvx",
  "args": [
    "awslabs.terraform-mcp-server@latest"
  ],
  "env": {
    "FASTMCP_LOG_LEVEL": "ERROR"
  },
  "disabled": false,
  "autoApprove": [
    "RunCheckovScan",
    "SearchAwsProviderDocs"
  ]
}
```

## Development Tools Setup

### 1. Install Docker (if using Docker MCP server)

```bash
brew install --cask docker
```

### 2. Install Kubernetes CLI (if using Kubernetes MCP server)

```bash
brew install kubectl
```

### 3. Configure Toolbox Path (Optional)

Add to your shell configuration:

```bash
export PATH=$HOME/.toolbox/bin:$PATH
```

## Project-Specific Configuration

### 1. Create Workspace MCP Configuration

For each project, create `.kiro/settings/mcp.json`:

```json
{
  "mcpServers": {}
}
```

This allows project-specific MCP server configurations to override global settings.

### 2. Create Kiro Settings Directory

```bash
mkdir -p .kiro/settings
```

## Verification Steps

### 1. Test Shell Integration

```bash
# Check if Kiro is detected
echo $TERM_PROGRAM
# Should output: kiro

# Test shell integration path
kiro --locate-shell-integration-path zsh
```

### 2. Test MCP Servers

Open Kiro IDE and check the MCP Server panel to ensure all servers are connected.

### 3. Test AWS Integration

```bash
# Test AWS CLI
aws sts get-caller-identity

# Test AWS SSO
aws sso login
```

## Troubleshooting

### MCP Server Issues

1. **Server not starting**: Check if `uv` and `uvx` are installed:

   ```bash
   which uv
   which uvx
   ```

2. **AWS MCP servers failing**: Ensure AWS credentials are configured:

   ```bash
   aws configure list
   ```

3. **Permission issues**: Check MCP server logs in Kiro IDE's output panel.

### Shell Integration Issues

1. **Integration not working**: Ensure the shell integration line is added to the correct shell configuration file.

2. **Path issues**: Verify Kiro CLI is in PATH:

   ```bash
   which kiro
   ```

### AWS Configuration Issues

1. **SSO login fails**: Check your SSO URL and profile configuration.

2. **Permission denied**: Ensure your AWS profile has the necessary permissions.

## Additional Customizations

### 1. Custom MCP Servers

To add custom MCP servers, follow this pattern:

```json
"your-custom-server": {
  "command": "uvx",
  "args": [
    "your-custom-mcp-server"
  ],
  "env": {
    "CUSTOM_ENV_VAR": "value"
  },
  "disabled": false,
  "autoApprove": [
    "function_name_1",
    "function_name_2"
  ]
}
```

### 2. Environment-Specific Configurations

Create different MCP configurations for different environments (dev, staging, prod) by using different AWS profiles and regions.

### 3. PowerPoint Automation (Optional)

If you need PowerPoint automation, add this to your MCP configuration:

```json
"ppt-automation": {
  "command": "uv",
  "args": [
    "run",
    "--directory",
    "/path/to/your/powerpoint-automation-mcp",
    "mcp-ppt-server"
  ],
  "env": {},
  "disabled": false,
  "autoApprove": [
    "initialize_powerpoint",
    "create_presentation",
    "open_presentation",
    "save_presentation"
  ]
}
```

## Quick Setup Script

Create a setup script to automate the configuration:

```bash
#!/bin/bash

# Quick Kiro Setup Script

echo "Setting up Kiro IDE configuration..."

# Create directories
mkdir -p ~/.kiro/settings

# Backup existing configurations
if [ -f ~/.zshrc ]; then
    cp ~/.zshrc ~/.zshrc.backup
fi

# Add shell integration to zshrc
if ! grep -q "kiro --locate-shell-integration-path" ~/.zshrc; then
    echo '[[ "$TERM_PROGRAM" == "kiro" ]] && . "$(kiro --locate-shell-integration-path zsh)"' >> ~/.zshrc
fi

# Add AWS configuration
if ! grep -q "AWS_PROFILE" ~/.zshrc; then
    echo 'export AWS_PROFILE="your-sso-profile-name"' >> ~/.zshrc
    echo 'export AWS_CLI_BROWSER="open -na '\''Google Chrome'\'' --args --incognito"' >> ~/.zshrc
fi

# Add toolbox path
if ! grep -q "toolbox/bin" ~/.zshrc; then
    echo 'export PATH=$HOME/.toolbox/bin:$PATH' >> ~/.zshrc
fi

echo "Configuration complete! Please:"
echo "1. Update AWS_PROFILE in ~/.zshrc with your actual profile name"
echo "2. Create ~/.kiro/settings/mcp.json with the MCP server configuration"
echo "3. Restart your terminal or run: source ~/.zshrc"
```

Save this as `setup-kiro.sh` and run:

```bash
chmod +x setup-kiro.sh
./setup-kiro.sh
```

## Notes

- Replace all instances of `"your-sso-profile-name"` with your actual AWS SSO profile name
- Adjust AWS regions as needed for your setup
- Enable/disable MCP servers based on your requirements
- Some MCP servers may require additional setup or authentication

This configuration provides a comprehensive Kiro IDE setup with terminal integration, AWS tools, and various development utilities through MCP servers.
