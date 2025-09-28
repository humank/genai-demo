# Excalidraw Configuration Guide

This project is configured to uniformly use the **Excalifont** font for creating all Excalidraw diagrams, ensuring visual consistency and professional appearance.

## Configuration Files

### Main Configuration File
- **Location**: `.kiro/settings/excalidraw.json`
- **Purpose**: Define project-level Excalidraw default settings

### Configuration Content

```json
{
  "defaultSettings": {
    "fontFamily": "Excalifont",
    "fontSize": 14,
    "strokeColor": "#1e1e1e",
    "backgroundColor": "transparent",
    "strokeWidth": 2,
    "roughness": 1,
    "opacity": 100
  },
  "elementDefaults": {
    "text": {"fontFamily": "Excalifont", "fontSize": 14},
    "label": {"fontFamily": "Excalifont", "fontSize": 14},
    "title": {"fontFamily": "Excalifont", "fontSize": 24},
    "subtitle": {"fontFamily": "Excalifont", "fontSize": 18},
    "caption": {"fontFamily": "Excalifont", "fontSize": 12}
  }
}
```

## Management Tools

### 1. Configuration Manager
**Script**: `scripts/excalidraw-config-manager.py`

```bash
# Update all fonts to Excalifont
python scripts/excalidraw-config-manager.py --update-fonts

# Show current configuration
python scripts/excalidraw-config-manager.py --show-config
```

### 2. Helper Functions Module
**Module**: `scripts/excalidraw_helpers.py`

Provides pre-configured functions to create Excalidraw elements, automatically applying the Excalifont font.

## Usage

### Using in Python Scripts

```python
import sys
from pathlib import Path
sys.path.append(str(Path(__file__).parent))

from excalidraw_helpers import (
    create_text, create_rectangle, create_ellipse,
    create_bounded_context, create_stakeholder, create_title
)

# Create title (automatically uses Excalifont)
title = create_title("System Architecture Overview")

# Create bounded context (automatically uses Excalifont)
customer_context = create_bounded_context("Customer", 100, 100, "business")

# Create stakeholder (automatically uses Excalifont)
end_user = create_stakeholder("End Users", 200, 200, "user")

# Create text element (automatically uses Excalifont)
description = create_text("System description text", 50, 300, element_type="caption")
```

### Best Practices When Using MCP Directly

When using Excalidraw MCP directly, ensure all text elements specify `fontFamily: "Excalifont"`:

```python
# ✅ Correct: Using Excalifont
mcp_excalidraw_create_element({
    "type": "text",
    "x": 100,
    "y": 100,
    "text": "Example text",
    "fontFamily": "Excalifont",  # Important: specify font
    "fontSize": 14
})

# ❌ Incorrect: Using default font
mcp_excalidraw_create_element({
    "type": "text",
    "x": 100,
    "y": 100,
    "text": "Example text",
    "fontSize": 14
    # Missing fontFamily setting
})
```

## Color Schemes

The project defines four standard color schemes:

### 1. Business
- **Primary**: `#1976d2` (Blue)
- **Secondary**: `#e3f2fd` (Light Blue)
- **Accent**: `#0d47a1` (Dark Blue)

### 2. Technical
- **Primary**: `#388e3c` (Green)
- **Secondary**: `#e8f5e8` (Light Green)
- **Accent**: `#1b5e20` (Dark Green)

### 3. User
- **Primary**: `#f57c00` (Orange)
- **Secondary**: `#fff3e0` (Light Orange)
- **Accent**: `#e65100` (Dark Orange)

### 4. Process
- **Primary**: `#7b1fa2` (Purple)
- **Secondary**: `#f3e5f5` (Light Purple)
- **Accent**: `#4a148c` (Dark Purple)

## Element Templates

### Bounded Context
- **Type**: Rectangle
- **Size**: 150x80
- **Font**: Excalifont, 14px

### Stakeholder
- **Type**: Ellipse
- **Size**: 120x80
- **Font**: Excalifont, 14px

### Process Step
- **Type**: Rectangle
- **Size**: 120x60
- **Font**: Excalifont, 14px

## Font Size Guidelines

- **Title**: 24px
- **Subtitle**: 18px
- **Text/Label**: 14px
- **Caption**: 12px

## Maintenance and Updates

### Updating Existing Diagrams
If you need to update existing Excalidraw diagrams to use Excalifont:

1. Run the configuration manager:
   ```bash
   python scripts/excalidraw-config-manager.py --update-fonts
   ```

2. Manually check and update font settings in existing diagrams

### Adding Color Schemes
To add new color schemes, edit `.kiro/settings/excalidraw.json`:

```json
{
  "colorSchemes": {
    "newScheme": {
      "primary": "#color1",
      "secondary": "#color2", 
      "accent": "#color3"
    }
  }
}
```

### Adding Element Templates
To add new element templates:

```json
{
  "templates": {
    "newTemplate": {
      "type": "rectangle",
      "width": 100,
      "height": 50,
      "strokeWidth": 2,
      "fontFamily": "Excalifont",
      "fontSize": 14
    }
  }
}
```

## Best Practices

1. **Consistency**: All diagrams use Excalifont font
2. **Color Standardization**: Use predefined color schemes
3. **Size Standardization**: Use predefined element templates
4. **Readability**: Ensure appropriate text size (minimum 12px)
5. **Contrast**: Ensure sufficient contrast between text and background

## Troubleshooting

### Font Not Displaying Correctly
1. Confirm Excalifont font is installed
2. Check if `fontFamily: "Excalifont"` is correctly set in element configuration
3. Use configuration manager to update settings

### Configuration Not Taking Effect
1. Check if `.kiro/settings/excalidraw.json` file exists
2. Verify JSON format is correct
3. Re-run configuration manager

---

*This configuration ensures all Excalidraw diagrams maintain consistent visual style and professional appearance throughout the project.*