#!/usr/bin/env python3
"""
Excalidraw Helper Functions

This module provides helper functions for creating Excalidraw elements
with consistent project-wide styling, especially using Excalifont.
"""

import json
import sys
from pathlib import Path
from typing import Dict, Any, List, Optional, Union

# Add the scripts directory to Python path for imports
sys.path.append(str(Path(__file__).parent))

try:
    from excalidraw_config_manager import ExcalidrawConfigManager
except ImportError:
    # Fallback if config manager is not available
    class ExcalidrawConfigManager:
        def get_default_element_config(self, element_type: str = "text") -> Dict[str, Any]:
            return {"fontFamily": "Excalifont", "fontSize": 14}
        
        def get_color_scheme(self, scheme_name: str = "business") -> Dict[str, str]:
            schemes = {
                "business": {"primary": "#1976d2", "secondary": "#e3f2fd", "accent": "#0d47a1"},
                "technical": {"primary": "#388e3c", "secondary": "#e8f5e8", "accent": "#1b5e20"},
                "user": {"primary": "#f57c00", "secondary": "#fff3e0", "accent": "#e65100"},
                "process": {"primary": "#7b1fa2", "secondary": "#f3e5f5", "accent": "#4a148c"}
            }
            return schemes.get(scheme_name, schemes["business"])
        
        def get_template_config(self, template_name: str) -> Dict[str, Any]:
            templates = {
                "boundedContext": {"type": "rectangle", "width": 150, "height": 80, "strokeWidth": 2, "fontFamily": "Excalifont", "fontSize": 14},
                "stakeholder": {"type": "ellipse", "width": 120, "height": 80, "strokeWidth": 2, "fontFamily": "Excalifont", "fontSize": 14},
                "process": {"type": "rectangle", "width": 120, "height": 60, "strokeWidth": 2, "fontFamily": "Excalifont", "fontSize": 14}
            }
            return templates.get(template_name, {"fontFamily": "Excalifont", "fontSize": 14})
        
        def create_element_with_config(self, element_type: str, **kwargs) -> Dict[str, Any]:
            config = self.get_default_element_config(element_type)
            config.update(kwargs)
            config["fontFamily"] = "Excalifont"
            return config

class ExcalidrawHelper:
    """Helper class for creating Excalidraw elements with project styling."""
    
    def __init__(self):
        """Initialize the helper with project configuration."""
        self.config_manager = ExcalidrawConfigManager()
    
    def get_default_element_config(self, element_type: str = "text") -> Dict[str, Any]:
        """Get default configuration for an element type."""
        return self.config_manager.get_default_element_config(element_type)
    
    def create_text_element(self, text: str, x: int, y: int, 
                           element_type: str = "text", **kwargs) -> Dict[str, Any]:
        """Create a text element with project defaults.
        
        Args:
            text: Text content
            x, y: Position coordinates
            element_type: Type of text element (text, label, title, etc.)
            **kwargs: Additional element properties
            
        Returns:
            Dictionary with element configuration
        """
        config = self.config_manager.create_element_with_config(element_type, **kwargs)
        
        element = {
            "type": "text",
            "x": x,
            "y": y,
            "text": text,
            "fontFamily": "Excalifont",  # Always use Excalifont
            **config
        }
        
        # Set default dimensions if not provided
        element.setdefault("width", len(text) * 8)  # Rough estimate
        element.setdefault("height", config.get("fontSize", 14) + 6)
        
        return element
    
    def create_rectangle_element(self, x: int, y: int, width: int, height: int,
                               **kwargs) -> Dict[str, Any]:
        """Create a rectangle element with project defaults.
        
        Args:
            x, y: Position coordinates
            width, height: Dimensions
            **kwargs: Additional element properties
            
        Returns:
            Dictionary with element configuration
        """
        config = self.config_manager.create_element_with_config("rectangle", **kwargs)
        
        return {
            "type": "rectangle",
            "x": x,
            "y": y,
            "width": width,
            "height": height,
            **config
        }
    
    def create_ellipse_element(self, x: int, y: int, width: int, height: int,
                              **kwargs) -> Dict[str, Any]:
        """Create an ellipse element with project defaults.
        
        Args:
            x, y: Position coordinates
            width, height: Dimensions
            **kwargs: Additional element properties
            
        Returns:
            Dictionary with element configuration
        """
        config = self.config_manager.create_element_with_config("ellipse", **kwargs)
        
        return {
            "type": "ellipse",
            "x": x,
            "y": y,
            "width": width,
            "height": height,
            **config
        }
    
    def create_arrow_element(self, x: int, y: int, width: int, height: int,
                            **kwargs) -> Dict[str, Any]:
        """Create an arrow element with project defaults.
        
        Args:
            x, y: Position coordinates
            width, height: Dimensions (direction and length)
            **kwargs: Additional element properties
            
        Returns:
            Dictionary with element configuration
        """
        config = self.config_manager.create_element_with_config("arrow", **kwargs)
        
        return {
            "type": "arrow",
            "x": x,
            "y": y,
            "width": width,
            "height": height,
            **config
        }
    
    def create_bounded_context_element(self, name: str, x: int, y: int,
                                     color_scheme: str = "business") -> List[Dict[str, Any]]:
        """Create a bounded context with rectangle and label.
        
        Args:
            name: Name of the bounded context
            x, y: Position coordinates
            color_scheme: Color scheme to use
            
        Returns:
            List of element configurations (rectangle and text)
        """
        colors = self.config_manager.get_color_scheme(color_scheme)
        template = self.config_manager.get_template_config("boundedContext")
        
        width = template.get("width", 150)
        height = template.get("height", 80)
        
        rectangle = self.create_rectangle_element(
            x, y, width, height,
            backgroundColor=colors.get("secondary", "#e3f2fd"),
            strokeColor=colors.get("primary", "#1976d2")
        )
        
        text = self.create_text_element(
            name, x + width//2 - len(name)*4, y + height//2 - 10,
            element_type="label"
        )
        
        return [rectangle, text]
    
    def create_stakeholder_element(self, name: str, x: int, y: int,
                                 color_scheme: str = "user") -> List[Dict[str, Any]]:
        """Create a stakeholder with ellipse and label.
        
        Args:
            name: Name of the stakeholder
            x, y: Position coordinates
            color_scheme: Color scheme to use
            
        Returns:
            List of element configurations (ellipse and text)
        """
        colors = self.config_manager.get_color_scheme(color_scheme)
        template = self.config_manager.get_template_config("stakeholder")
        
        width = template.get("width", 120)
        height = template.get("height", 80)
        
        ellipse = self.create_ellipse_element(
            x, y, width, height,
            backgroundColor=colors.get("secondary", "#fff3e0"),
            strokeColor=colors.get("primary", "#f57c00")
        )
        
        text = self.create_text_element(
            name, x + width//2 - len(name)*4, y + height//2 - 10,
            element_type="label"
        )
        
        return [ellipse, text]
    
    def create_process_step_element(self, name: str, x: int, y: int,
                                  color_scheme: str = "process") -> List[Dict[str, Any]]:
        """Create a process step with rectangle and label.
        
        Args:
            name: Name of the process step
            x, y: Position coordinates
            color_scheme: Color scheme to use
            
        Returns:
            List of element configurations (rectangle and text)
        """
        colors = self.config_manager.get_color_scheme(color_scheme)
        template = self.config_manager.get_template_config("process")
        
        width = template.get("width", 120)
        height = template.get("height", 60)
        
        rectangle = self.create_rectangle_element(
            x, y, width, height,
            backgroundColor=colors.get("secondary", "#f3e5f5"),
            strokeColor=colors.get("primary", "#7b1fa2")
        )
        
        text = self.create_text_element(
            name, x + width//2 - len(name)*4, y + height//2 - 10,
            element_type="label"
        )
        
        return [rectangle, text]
    
    def create_title_element(self, title: str, x: int, y: int) -> Dict[str, Any]:
        """Create a title element.
        
        Args:
            title: Title text
            x, y: Position coordinates
            
        Returns:
            Dictionary with title element configuration
        """
        return self.create_text_element(title, x, y, element_type="title")
    
    def create_diagram_with_title(self, title: str, elements: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """Create a complete diagram with title and elements.
        
        Args:
            title: Diagram title
            elements: List of diagram elements
            
        Returns:
            List of all elements including title
        """
        title_element = self.create_title_element(title, 400, 50)
        return [title_element] + elements

# Global helper instance
excalidraw_helper = ExcalidrawHelper()

# Convenience functions
def create_text(text: str, x: int, y: int, **kwargs) -> Dict[str, Any]:
    """Create text element with Excalifont."""
    return excalidraw_helper.create_text_element(text, x, y, **kwargs)

def create_rectangle(x: int, y: int, width: int, height: int, **kwargs) -> Dict[str, Any]:
    """Create rectangle element."""
    return excalidraw_helper.create_rectangle_element(x, y, width, height, **kwargs)

def create_ellipse(x: int, y: int, width: int, height: int, **kwargs) -> Dict[str, Any]:
    """Create ellipse element."""
    return excalidraw_helper.create_ellipse_element(x, y, width, height, **kwargs)

def create_arrow(x: int, y: int, width: int, height: int, **kwargs) -> Dict[str, Any]:
    """Create arrow element."""
    return excalidraw_helper.create_arrow_element(x, y, width, height, **kwargs)

def create_bounded_context(name: str, x: int, y: int, color_scheme: str = "business") -> List[Dict[str, Any]]:
    """Create bounded context with rectangle and label."""
    return excalidraw_helper.create_bounded_context_element(name, x, y, color_scheme)

def create_stakeholder(name: str, x: int, y: int, color_scheme: str = "user") -> List[Dict[str, Any]]:
    """Create stakeholder with ellipse and label."""
    return excalidraw_helper.create_stakeholder_element(name, x, y, color_scheme)

def create_process_step(name: str, x: int, y: int, color_scheme: str = "process") -> List[Dict[str, Any]]:
    """Create process step with rectangle and label."""
    return excalidraw_helper.create_process_step_element(name, x, y, color_scheme)

def create_title(title: str, x: int = 400, y: int = 50) -> Dict[str, Any]:
    """Create title element."""
    return excalidraw_helper.create_title_element(title, x, y)

if __name__ == "__main__":
    # Example usage
    print("Excalidraw Helper Functions")
    print("Example: Creating a bounded context")
    
    elements = create_bounded_context("Customer", 100, 100, "business")
    for element in elements:
        print(json.dumps(element, indent=2))