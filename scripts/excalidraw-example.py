#!/usr/bin/env python3
"""
Excalidraw Example Script

This script demonstrates how to use the Excalidraw helper functions
with the project's Excalifont configuration.
"""

import sys
from pathlib import Path

# Add the scripts directory to Python path
sys.path.append(str(Path(__file__).parent))

from excalidraw_helpers import (
    create_text, create_rectangle, create_ellipse, create_arrow,
    create_bounded_context, create_stakeholder, create_process_step,
    create_title, excalidraw_helper
)

def create_sample_bounded_context_diagram():
    """Create a sample bounded context diagram using Excalifont."""
    print("Creating Bounded Context Diagram with Excalifont...")
    
    elements = []
    
    # Add title
    title = create_title("Bounded Contexts with Excalifont", 400, 50)
    elements.append(title)
    
    # Create bounded contexts
    contexts = [
        ("Customer", 100, 120, "business"),
        ("Order", 300, 120, "process"),
        ("Product", 500, 120, "technical"),
        ("Payment", 700, 120, "user")
    ]
    
    for name, x, y, color_scheme in contexts:
        context_elements = create_bounded_context(name, x, y, color_scheme)
        elements.extend(context_elements)
    
    # Add arrows between contexts
    arrows = [
        (250, 160, 50, 0),  # Customer -> Order
        (450, 160, 50, 0),  # Order -> Product
        (650, 160, 50, 0),  # Product -> Payment
    ]
    
    for x, y, width, height in arrows:
        arrow = create_arrow(x, y, width, height, strokeColor="#666666")
        elements.append(arrow)
    
    # Add description
    description = create_text(
        "All text elements use Excalifont for consistent typography",
        50, 250, element_type="caption"
    )
    elements.append(description)
    
    return elements

def create_sample_stakeholder_diagram():
    """Create a sample stakeholder diagram using Excalifont."""
    print("Creating Stakeholder Diagram with Excalifont...")
    
    elements = []
    
    # Add title
    title = create_title("Stakeholder Mapping with Excalifont", 400, 50)
    elements.append(title)
    
    # Create stakeholders
    stakeholders = [
        ("End Users", 100, 120, "user"),
        ("Business", 300, 120, "business"),
        ("Developers", 500, 120, "technical"),
        ("Operations", 700, 120, "process")
    ]
    
    for name, x, y, color_scheme in stakeholders:
        stakeholder_elements = create_stakeholder(name, x, y, color_scheme)
        elements.extend(stakeholder_elements)
    
    # Add central system
    system = create_rectangle(400, 200, 120, 60, 
                            backgroundColor="#f5f5f5", 
                            strokeColor="#333333")
    elements.append(system)
    
    system_label = create_text("System", 460, 220, element_type="label")
    elements.append(system_label)
    
    return elements

def create_sample_process_diagram():
    """Create a sample process diagram using Excalifont."""
    print("Creating Process Flow Diagram with Excalifont...")
    
    elements = []
    
    # Add title
    title = create_title("Process Flow with Excalifont", 400, 50)
    elements.append(title)
    
    # Create process steps
    steps = [
        ("Start", 100, 120, "business"),
        ("Process", 300, 120, "technical"),
        ("Validate", 500, 120, "process"),
        ("Complete", 700, 120, "user")
    ]
    
    for name, x, y, color_scheme in steps:
        step_elements = create_process_step(name, x, y, color_scheme)
        elements.extend(step_elements)
    
    # Add flow arrows
    arrows = [
        (220, 150, 80, 0),  # Start -> Process
        (420, 150, 80, 0),  # Process -> Validate
        (620, 150, 80, 0),  # Validate -> Complete
    ]
    
    for x, y, width, height in arrows:
        arrow = create_arrow(x, y, width, height, strokeColor="#666666")
        elements.append(arrow)
    
    return elements

def demonstrate_font_consistency():
    """Demonstrate font consistency across different element types."""
    print("\nFont Consistency Demonstration:")
    print("=" * 50)
    
    # Test different text types
    text_types = ["text", "label", "title", "subtitle", "caption"]
    
    for text_type in text_types:
        config = excalidraw_helper.get_default_element_config(text_type)
        print(f"{text_type.capitalize():>10}: {config['fontFamily']} ({config['fontSize']}px)")
    
    print("\nColor Schemes:")
    print("-" * 20)
    
    # Test color schemes
    schemes = ["business", "technical", "user", "process"]
    
    for scheme in schemes:
        colors = excalidraw_helper.config_manager.get_color_scheme(scheme)
        print(f"{scheme.capitalize():>10}: Primary={colors.get('primary', 'N/A')}")
    
    print("\nElement Templates:")
    print("-" * 20)
    
    # Test templates
    templates = ["boundedContext", "stakeholder", "process"]
    
    for template in templates:
        config = excalidraw_helper.config_manager.get_template_config(template)
        print(f"{template:>15}: {config.get('fontFamily', 'N/A')} "
              f"({config.get('width', 'N/A')}x{config.get('height', 'N/A')})")

def main():
    """Main function to run examples."""
    print("Excalidraw Configuration Example")
    print("=" * 40)
    print("This script demonstrates the use of Excalifont")
    print("in all Excalidraw diagrams for this project.")
    print()
    
    # Demonstrate font consistency
    demonstrate_font_consistency()
    
    print("\n" + "=" * 50)
    print("Creating Sample Diagrams...")
    print("=" * 50)
    
    # Create sample diagrams
    bounded_context_elements = create_sample_bounded_context_diagram()
    print(f"✅ Created bounded context diagram with {len(bounded_context_elements)} elements")
    
    stakeholder_elements = create_sample_stakeholder_diagram()
    print(f"✅ Created stakeholder diagram with {len(stakeholder_elements)} elements")
    
    process_elements = create_sample_process_diagram()
    print(f"✅ Created process flow diagram with {len(process_elements)} elements")
    
    print("\n" + "=" * 50)
    print("Configuration Summary:")
    print("=" * 50)
    print("✅ All text elements use Excalifont")
    print("✅ Consistent color schemes applied")
    print("✅ Standardized element templates used")
    print("✅ Project-wide typography consistency maintained")
    
    print("\nTo use these configurations in your own scripts:")
    print("1. Import the helper functions from excalidraw_helpers.py")
    print("2. Use create_* functions instead of direct MCP calls")
    print("3. All fonts will automatically be set to Excalifont")

if __name__ == "__main__":
    main()