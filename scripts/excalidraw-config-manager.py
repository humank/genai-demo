#!/usr/bin/env python3
"""
Excalidraw Configuration Manager

This script manages Excalidraw configurations for the project,
ensuring consistent font usage (Excalifont) and styling across all diagrams.
"""

import json
import os
from pathlib import Path
from typing import Dict, Any, Optional

class ExcalidrawConfigManager:
    """Manages Excalidraw configuration settings for the project."""
    
    def __init__(self, project_root: Optional[str] = None):
        """Initialize the configuration manager.
        
        Args:
            project_root: Path to project root. If None, auto-detect from script location.
        """
        if project_root is None:
            # Auto-detect project root (assuming script is in scripts/ directory)
            self.project_root = Path(__file__).parent.parent
        else:
            self.project_root = Path(project_root)
        
        self.config_path = self.project_root / ".kiro" / "settings" / "excalidraw.json"
        self.config = self._load_config()
    
    def _load_config(self) -> Dict[str, Any]:
        """Load configuration from file."""
        if self.config_path.exists():
            with open(self.config_path, 'r', encoding='utf-8') as f:
                return json.load(f)
        else:
            return self._get_default_config()
    
    def _get_default_config(self) -> Dict[str, Any]:
        """Get default configuration with Excalifont settings."""
        return {
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
            },
            "colorSchemes": {
                "business": {"primary": "#1976d2", "secondary": "#e3f2fd", "accent": "#0d47a1"},
                "technical": {"primary": "#388e3c", "secondary": "#e8f5e8", "accent": "#1b5e20"},
                "user": {"primary": "#f57c00", "secondary": "#fff3e0", "accent": "#e65100"},
                "process": {"primary": "#7b1fa2", "secondary": "#f3e5f5", "accent": "#4a148c"}
            },
            "templates": {
                "boundedContext": {
                    "type": "rectangle", "width": 150, "height": 80,
                    "strokeWidth": 2, "fontFamily": "Excalifont", "fontSize": 14
                },
                "stakeholder": {
                    "type": "ellipse", "width": 120, "height": 80,
                    "strokeWidth": 2, "fontFamily": "Excalifont", "fontSize": 14
                },
                "process": {
                    "type": "rectangle", "width": 120, "height": 60,
                    "strokeWidth": 2, "fontFamily": "Excalifont", "fontSize": 14
                }
            }
        }
    
    def get_default_element_config(self, element_type: str = "text") -> Dict[str, Any]:
        """Get default configuration for an element type.
        
        Args:
            element_type: Type of element (text, label, title, etc.)
            
        Returns:
            Dictionary with default configuration for the element type.
        """
        defaults = self.config.get("elementDefaults", {})
        element_config = defaults.get(element_type, defaults.get("text", {}))
        
        # Ensure Excalifont is always used
        element_config["fontFamily"] = "Excalifont"
        
        return element_config
    
    def get_color_scheme(self, scheme_name: str = "business") -> Dict[str, str]:
        """Get color scheme by name.
        
        Args:
            scheme_name: Name of the color scheme
            
        Returns:
            Dictionary with color values
        """
        schemes = self.config.get("colorSchemes", {})
        return schemes.get(scheme_name, schemes.get("business", {}))
    
    def get_template_config(self, template_name: str) -> Dict[str, Any]:
        """Get template configuration by name.
        
        Args:
            template_name: Name of the template
            
        Returns:
            Dictionary with template configuration
        """
        templates = self.config.get("templates", {})
        template_config = templates.get(template_name, {})
        
        # Ensure Excalifont is always used
        if "fontFamily" in template_config or template_name in ["boundedContext", "stakeholder", "process"]:
            template_config["fontFamily"] = "Excalifont"
        
        return template_config
    
    def get_template_config(self, template_name: str) -> Dict[str, Any]:
        """Get template configuration by name.
        
        Args:
            template_name: Name of the template
            
        Returns:
            Dictionary with template configuration
        """
        templates = self.config.get("templates", {})
        template_config = templates.get(template_name, {}).copy()
        
        # Ensure Excalifont is always used
        if "fontFamily" in template_config or template_name in ["boundedContext", "stakeholder", "process"]:
            template_config["fontFamily"] = "Excalifont"
        
        return template_config
    
    def create_element_with_config(self, element_type: str, **kwargs) -> Dict[str, Any]:
        """Create element configuration with project defaults.
        
        Args:
            element_type: Type of element to create
            **kwargs: Additional element properties
            
        Returns:
            Dictionary with complete element configuration
        """
        # Start with default configuration
        config = self.get_default_element_config(element_type)
        
        # Apply any template-specific settings
        if "template" in kwargs:
            template_config = self.get_template_config(kwargs.pop("template"))
            config.update(template_config)
        
        # Apply color scheme if specified
        if "colorScheme" in kwargs:
            colors = self.get_color_scheme(kwargs.pop("colorScheme"))
            if "primary" in colors:
                config.setdefault("strokeColor", colors["primary"])
            if "secondary" in colors:
                config.setdefault("backgroundColor", colors["secondary"])
        
        # Apply user-provided overrides
        config.update(kwargs)
        
        # Ensure Excalifont is always used
        config["fontFamily"] = "Excalifont"
        
        return config
    
    def save_config(self):
        """Save current configuration to file."""
        self.config_path.parent.mkdir(parents=True, exist_ok=True)
        with open(self.config_path, 'w', encoding='utf-8') as f:
            json.dump(self.config, f, indent=2, ensure_ascii=False)
    
    def update_all_fonts_to_excalifont(self):
        """Update all font references in config to use Excalifont."""
        # Update default settings
        if "defaultSettings" in self.config:
            self.config["defaultSettings"]["fontFamily"] = "Excalifont"
        
        # Update element defaults
        if "elementDefaults" in self.config:
            for element_type in self.config["elementDefaults"]:
                self.config["elementDefaults"][element_type]["fontFamily"] = "Excalifont"
        
        # Update templates
        if "templates" in self.config:
            for template_name in self.config["templates"]:
                if "fontFamily" in self.config["templates"][template_name]:
                    self.config["templates"][template_name]["fontFamily"] = "Excalifont"
        
        self.save_config()
        print("✅ Updated all font references to Excalifont")

def main():
    """Main function for command-line usage."""
    import argparse
    
    parser = argparse.ArgumentParser(description="Manage Excalidraw configuration")
    parser.add_argument("--update-fonts", action="store_true", 
                       help="Update all fonts to Excalifont")
    parser.add_argument("--show-config", action="store_true",
                       help="Show current configuration")
    
    args = parser.parse_args()
    
    config_manager = ExcalidrawConfigManager()
    
    if args.update_fonts:
        config_manager.update_all_fonts_to_excalifont()
    
    if args.show_config:
        print("Current Excalidraw Configuration:")
        print(json.dumps(config_manager.config, indent=2, ensure_ascii=False))
    
    if not any(vars(args).values()):
        print("Excalidraw Configuration Manager")
        print("Available commands:")
        print("  --update-fonts    Update all fonts to Excalifont")
        print("  --show-config     Show current configuration")

if __name__ == "__main__":
    main()