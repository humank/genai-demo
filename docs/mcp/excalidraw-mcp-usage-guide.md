# Excalidraw MCP Server Usage Guide

## Overview

The Excalidraw MCP Server is a fully-featured Model Context Protocol server that enables AI assistants to create and manage Excalidraw diagrams. This guide provides detailed instructions on how to install, configure, and use this powerful visualization tool.

## 🎯 Key Features

### Basic Drawing Capabilities

- ✅ **Multiple Element Types**: rectangles, ellipses, diamonds, arrows, text, lines, freedraw
- ✅ **Rich Styling Options**: background colors, stroke colors, line width, opacity, font size
- ✅ **Chinese Text Support**: Perfect support for Chinese text display

### Advanced Management Features

- ✅ **Element Management**: create, update, delete, query
- ✅ **Batch Operations**: create multiple elements at once, suitable for complex diagrams
- ✅ **Element Grouping**: group management and ungrouping
- ✅ **Alignment & Distribution**: left/center/right, top/middle/bottom alignment, horizontal/vertical distribution
- ✅ **Locking Features**: lock/unlock elements to prevent accidental modifications

### Technical Features

- ✅ **Real-time Sync**: supports real-time synchronization with canvas (optional)
- ✅ **Version Control**: version tracking for each element
- ✅ **Error Handling**: comprehensive error handling and logging
- ✅ **Resource Management**: access to scene, library, theme, and element resources

## 📦 Installation Guide

### Method 1: Local Installation (Recommended)

```bash
# 1. Navigate to project root directory
cd /path/to/genai-demo

# 2. Install Excalidraw MCP Server
npm install mcp-excalidraw-server

# 3. Verify installation
ls node_modules/mcp-excalidraw-server/src/index.js

# 4. Test server
echo '{"jsonrpc": "2.0", "id": 1, "method": "tools-and-environment/list", "params": {}}' | \
  node node_modules/mcp-excalidraw-server/src/index.js | head -3
```

### Method 2: Global Installation

```bash
# Global installation
npm install -g mcp-excalidraw-server

# Test global installation
mcp-excalidraw-server --help
```

### Method 3: Using NPX (Not recommended for production)

```bash
# Run with NPX (slower)
npx mcp-excalidraw-server --help
```

## ⚙️ Configuration Setup

### MCP Configuration File

Ensure `.kiro/settings/mcp.json` contains the correct Excalidraw configuration:

```json
{
  "mcpServers": {
    "excalidraw": {
      "command": "node",
      "args": [
        "node_modules/mcp-excalidraw-server/src/index.js"
      ],
      "env": {
        "ENABLE_CANVAS_SYNC": "false"
      },
      "disabled": false,
      "autoApprove": [
        "create_element",
        "update_element",
        "delete_element",
        "query_elements",
        "get_resource",
        "group_elements",
        "ungroup_elements",
        "align_elements",
        "distribute_elements",
        "lock_elements",
        "unlock_elements",
        "batch_create_elements"
      ]
    }
  }
}
```

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `ENABLE_CANVAS_SYNC` | `true` | Whether to enable canvas sync functionality |
| `EXPRESS_SERVER_URL` | `http://localhost:3000` | Canvas server URL |
| `DEBUG` | `false` | Whether to enable debug logging |

### Configuration Options

#### Local Installation Configuration (Recommended)

```json
{
  "excalidraw": {
    "command": "node",
    "args": ["node_modules/mcp-excalidraw-server/src/index.js"],
    "env": {
      "ENABLE_CANVAS_SYNC": "false"
    }
  }
}
```

#### Global Installation Configuration

```json
{
  "excalidraw": {
    "command": "mcp-excalidraw-server",
    "args": [],
    "env": {
      "ENABLE_CANVAS_SYNC": "false"
    }
  }
}
```

#### NPX Configuration (Not recommended)

```json
{
  "excalidraw": {
    "command": "npx",
    "args": ["mcp-excalidraw-server"],
    "env": {
      "ENABLE_CANVAS_SYNC": "false"
    }
  }
}
```

## 🎨 Usage Instructions

### Using in Kiro IDE

#### Basic Element Creation

```text
Ask Kiro: "Create a blue rectangle with text 'Start'"
Ask Kiro: "Draw a green ellipse"
Ask Kiro: "Create an arrow from (100,100) to (200,100)"
```

#### Complex Diagram Creation

```text
Ask Kiro: "Create a simple flowchart:
- Start (green rectangle)
- Process (blue rectangle)
- Decision (yellow diamond)
- End (red rectangle)
- Connect them with arrows"
```

#### System Architecture Diagram

```text
Ask Kiro: "Create a microservices architecture diagram:
- API Gateway (blue rectangle)
- User Service (green rectangle)
- Order Service (green rectangle)
- Database (purple ellipse)
- Connect them with arrows"
```

#### Batch Element Creation

```text
Ask Kiro: "Batch create the following elements:
1. Rectangle (50,50) 120x60 background #c8e6c9 text 'Start'
2. Arrow (170,80) 80x0
3. Rectangle (250,50) 120x60 background #e3f2fd text 'Process'
4. Arrow (370,80) 80x0
5. Rectangle (450,50) 120x60 background #ffcdd2 text 'End'"
```

### Available MCP Tools

#### Basic Operation Tools

| Tool Name | Function | Parameters |
|-----------|----------|------------|
| `create_element` | Create single element | type, x, y, width, height, text, colors |
| `update_element` | Update existing element | id, properties to update |
| `delete_element` | Delete element | id |
| `query_elements` | Query elements | type (optional), filter (optional) |

#### Batch Operation Tools

| Tool Name | Function | Parameters |
|-----------|----------|------------|
| `batch_create_elements` | Batch create elements | elements array |

#### Advanced Management Tools

| Tool Name | Function | Parameters |
|-----------|----------|------------|
| `group_elements` | Group elements | elementIds array |
| `ungroup_elements` | Ungroup elements | groupId |
| `align_elements` | Align elements | elementIds, alignment |
| `distribute_elements` | Distribute elements | elementIds, direction |
| `lock_elements` | Lock elements | elementIds array |
| `unlock_elements` | Unlock elements | elementIds array |

#### Resource Management Tools

| Tool Name | Function | Parameters |
|-----------|----------|------------|
| `get_resource` | Get resource | resource (scene/library/theme/elements) |

### Element Types and Properties

#### Supported Element Types

| Type | Description | Special Properties |
|------|-------------|-------------------|
| `rectangle` | Rectangle | width, height |
| `ellipse` | Ellipse | width, height |
| `diamond` | Diamond | width, height |
| `arrow` | Arrow | width, height (endpoint relative position) |
| `text` | Text | text, fontSize, fontFamily |
| `line` | Line | width, height (endpoint relative position) |
| `freedraw` | Free drawing | points array |

#### Common Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `x` | number | Required | X coordinate |
| `y` | number | Required | Y coordinate |
| `width` | number | 100 | Width |
| `height` | number | 50 | Height |
| `backgroundColor` | string | transparent | Background color (hex) |
| `strokeColor` | string | #1e1e1e | Stroke color (hex) |
| `strokeWidth` | number | 2 | Stroke width |
| `opacity` | number | 100 | Opacity (0-100) |
| `roughness` | number | 1 | Roughness (0-2) |

#### Text Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `text` | string | "" | Text content |
| `fontSize` | number | 16 | Font size |
| `fontFamily` | number | 1 | Font family (1-4) |
| `textAlign` | string | center | Text alignment (left/center/right) |

## 📋 Practical Examples

### Example 1: Simple Flowchart

```javascript
// Use batch_create_elements to create complete flowchart
{
  "elements": [
    {
      "type": "rectangle",
      "x": 50,
      "y": 50,
      "width": 120,
      "height": 60,
      "backgroundColor": "#c8e6c9",
      "strokeColor": "#4caf50",
      "text": "Start"
    },
    {
      "type": "arrow",
      "x": 170,
      "y": 80,
      "width": 80,
      "height": 0,
      "strokeColor": "#666666"
    },
    {
      "type": "rectangle",
      "x": 250,
      "y": 50,
      "width": 120,
      "height": 60,
      "backgroundColor": "#e3f2fd",
      "strokeColor": "#2196f3",
      "text": "Process"
    },
    {
      "type": "arrow",
      "x": 370,
      "y": 80,
      "width": 80,
      "height": 0,
      "strokeColor": "#666666"
    },
    {
      "type": "rectangle",
      "x": 450,
      "y": 50,
      "width": 120,
      "height": 60,
      "backgroundColor": "#ffcdd2",
      "strokeColor": "#f44336",
      "text": "End"
    }
  ]
}
```

### Example 2: System Architecture Diagram

```javascript
{
  "elements": [
    {
      "type": "rectangle",
      "x": 100,
      "y": 50,
      "width": 150,
      "height": 60,
      "backgroundColor": "#e3f2fd",
      "text": "API Gateway"
    },
    {
      "type": "rectangle",
      "x": 50,
      "y": 150,
      "width": 120,
      "height": 60,
      "backgroundColor": "#e8f5e8",
      "text": "User Service"
    },
    {
      "type": "rectangle",
      "x": 200,
      "y": 150,
      "width": 120,
      "height": 60,
      "backgroundColor": "#e8f5e8",
      "text": "Order Service"
    },
    {
      "type": "ellipse",
      "x": 125,
      "y": 250,
      "width": 120,
      "height": 60,
      "backgroundColor": "#f3e5f5",
      "text": "Database"
    }
  ]
}
```

### Example 3: Decision Flow Diagram

```javascript
{
  "elements": [
    {
      "type": "rectangle",
      "x": 150,
      "y": 50,
      "width": 100,
      "height": 50,
      "backgroundColor": "#c8e6c9",
      "text": "Start"
    },
    {
      "type": "diamond",
      "x": 125,
      "y": 150,
      "width": 150,
      "height": 80,
      "backgroundColor": "#fff3e0",
      "text": "Condition"
    },
    {
      "type": "rectangle",
      "x": 50,
      "y": 280,
      "width": 100,
      "height": 50,
      "backgroundColor": "#e3f2fd",
      "text": "Yes"
    },
    {
      "type": "rectangle",
      "x": 250,
      "y": 280,
      "width": 100,
      "height": 50,
      "backgroundColor": "#ffebee",
      "text": "No"
    }
  ]
}
```

## 🔧 Troubleshooting

### Common Issues and Solutions

#### 1. Module Not Found Error

```bash
Error: Cannot find module 'mcp-excalidraw-server'
```

**Solution**:
```bash
# Ensure package is installed
npm install mcp-excalidraw-server

# Check installation location
ls node_modules/mcp-excalidraw-server/
```

#### 2. Permission Denied

```bash
Error: EACCES: permission denied
```

**Solution**:
```bash
# Check file permissions
chmod +x node_modules/mcp-excalidraw-server/src/index.js

# Or reinstall
rm -rf node_modules/mcp-excalidraw-server
npm install mcp-excalidraw-server
```

#### 3. Node.js Version Incompatibility

```bash
Error: Unsupported Node.js version
```

**Solution**:
```bash
# Check Node.js version (requires v16+)
node --version

# If version is too old, upgrade Node.js
# Download latest version from https://nodejs.org/
```

#### 4. MCP Server Not Responding

**Solution**:
```bash
# Check configuration
cat .kiro/settings/mcp.json | jq '.mcpServers.excalidraw'

# Test server manually
node node_modules/mcp-excalidraw-server/src/index.js --help

# Restart Kiro IDE
```

#### 5. JSON Parse Error

```bash
Error: Unexpected token in JSON
```

**Solution**:
```bash
# Check MCP configuration file syntax
cat .kiro/settings/mcp.json | jq '.'

# If syntax error exists, fix JSON format
```

### Debug Mode

Enable debug logging:

```json
{
  "excalidraw": {
    "env": {
      "DEBUG": "true",
      "ENABLE_CANVAS_SYNC": "false"
    }
  }
}
```

### Test Commands

```bash
# Test tool list
echo '{"jsonrpc": "2.0", "id": 1, "method": "tools-and-environment/list", "params": {}}' | \
  node node_modules/mcp-excalidraw-server/src/index.js

# Test element creation
echo '{"jsonrpc": "2.0", "id": 1, "method": "tools/call", "params": {"name": "create_element", "arguments": {"type": "rectangle", "x": 100, "y": 100, "width": 200, "height": 100, "text": "Test"}}}' | \
  node node_modules/mcp-excalidraw-server/src/index.js
```

## 🚀 Advanced Usage

### Custom Styling

```javascript
// Create element with custom styling
{
  "type": "rectangle",
  "x": 100,
  "y": 100,
  "width": 200,
  "height": 100,
  "backgroundColor": "#e3f2fd",
  "strokeColor": "#1976d2",
  "strokeWidth": 3,
  "opacity": 80,
  "roughness": 0.5,
  "text": "Custom Style",
  "fontSize": 18,
  "fontFamily": 2
}
```

### Element Grouping and Alignment

```javascript
// 1. Create multiple elements
// 2. Use group_elements to group them
{
  "elementIds": ["element-1", "element-2", "element-3"]
}

// 3. Use align_elements to align elements
{
  "elementIds": ["element-1", "element-2", "element-3"],
  "alignment": "center"
}
```

### Complex Diagram Templates

#### UML Class Diagram Template

```javascript
{
  "elements": [
    {
      "type": "rectangle",
      "x": 100,
      "y": 100,
      "width": 200,
      "height": 120,
      "backgroundColor": "#f5f5f5",
      "strokeColor": "#333333",
      "text": "User\n---\n+id: String\n+name: String\n+email: String\n---\n+login()\n+logout()"
    }
  ]
}
```

#### Network Topology Template

```javascript
{
  "elements": [
    {
      "type": "ellipse",
      "x": 200,
      "y": 50,
      "width": 100,
      "height": 60,
      "backgroundColor": "#e3f2fd",
      "text": "Router"
    },
    {
      "type": "rectangle",
      "x": 50,
      "y": 150,
      "width": 80,
      "height": 50,
      "backgroundColor": "#e8f5e8",
      "text": "PC1"
    },
    {
      "type": "rectangle",
      "x": 270,
      "y": 150,
      "width": 80,
      "height": 50,
      "backgroundColor": "#e8f5e8",
      "text": "PC2"
    }
  ]
}
```

## 📚 Reference Resources

### Official Documentation

- Excalidraw Official Website
- MCP Protocol Specification
- Node.js Official Documentation

### Related Projects

- mcp-excalidraw-server GitHub
- Excalidraw React Components
- Model Context Protocol SDK

### Internal Documentation

- [MCP Integration Guide](../../infrastructure/docs/MCP_INTEGRATION_GUIDE.md)
- [Architecture Documentation](../architecture/)
- [Deployment Guide](../deployment/)

---

## 📞 Support and Assistance

### Getting Help

1. Check the troubleshooting section above
2. Review MCP integration test reports
3. Consult with DevOps team
4. Refer to AWS documentation (using AWS Docs MCP)

### Reporting Issues

When encountering issues, please provide the following information:

- Node.js version (`node --version`)
- NPM version (`npm --version`)
- Complete error message output
- MCP configuration file content
- Steps to reproduce the issue

---

*Documentation Version: 1.0*  
*Last Updated: September 21, 2025*  
*Maintainer: DevOps Team*