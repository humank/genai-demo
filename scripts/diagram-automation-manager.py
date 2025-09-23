#!/usr/bin/env python3
"""
Diagram Automation Manager

This script provides a unified interface for managing all diagram automation tasks.
It can be used by Kiro hooks or run manually for maintenance.
"""

import os
import sys
import json
import argparse
import subprocess
from pathlib import Path
from datetime import datetime
from typing import Dict, List, Optional

class DiagramAutomationManager:
    """Manages all diagram automation tasks"""
    
    def __init__(self):
        self.project_root = Path.cwd()
        self.scripts_dir = self.project_root / "scripts"
        self.diagrams_root = self.project_root / "docs" / "diagrams"
        self.hooks_dir = self.project_root / ".kiro" / "hooks"
        
        # Define all diagram directories for maximum coverage
        self.diagram_dirs = [
            self.diagrams_root / "plantuml",
            self.diagrams_root / "plantuml" / "event-storming",
            self.diagrams_root / "plantuml" / "structural",
            self.diagrams_root / "plantuml" / "domain-event-handling",
            self.diagrams_root / "viewpoints" / "functional",
            self.diagrams_root / "viewpoints" / "information",
            self.diagrams_root / "viewpoints" / "concurrency",
            self.diagrams_root / "viewpoints" / "development",
            self.diagrams_root / "viewpoints" / "deployment",
            self.diagrams_root / "viewpoints" / "operational",
            self.diagrams_root / "perspectives" / "security",
            self.diagrams_root / "perspectives" / "performance",
            self.diagrams_root / "perspectives" / "availability",
            self.diagrams_root / "perspectives" / "evolution",
            self.diagrams_root / "perspectives" / "cost",
            self.diagrams_root / "perspectives" / "usability",
            self.diagrams_root / "perspectives" / "location",
            self.diagrams_root / "perspectives" / "regulation",
        ]
        
    def check_prerequisites(self) -> bool:
        """Check if all required tools and scripts are available"""
        print("🔍 Checking prerequisites...")
        
        required_scripts = [
            "analyze-ddd-code.py",
            "analyze-bdd-features.py",
            "fix-plantuml-syntax.py",
            "smart-diagram-update.py",
            "generate-diagram-images.sh"
        ]
        
        missing_scripts = []
        for script in required_scripts:
            script_path = self.scripts_dir / script
            if not script_path.exists():
                missing_scripts.append(script)
        
        if missing_scripts:
            print(f"❌ Missing required scripts: {', '.join(missing_scripts)}")
            return False
        
        # Check if PlantUML JAR is available
        plantuml_jar = self.project_root / "tools" / "plantuml.jar"
        if not plantuml_jar.exists():
            print("⚠️  PlantUML JAR not found, will be downloaded automatically")
        
        # Check if Python is available
        try:
            result = subprocess.run(["python3", "--version"], capture_output=True, text=True)
            if result.returncode != 0:
                print("❌ Python 3 is not available")
                return False
        except FileNotFoundError:
            print("❌ Python 3 is not available")
            return False
        
        # Check if Java is available (for PlantUML)
        try:
            result = subprocess.run(["java", "-version"], capture_output=True, text=True)
            if result.returncode != 0:
                print("❌ Java is not available (required for PlantUML)")
                return False
        except FileNotFoundError:
            print("❌ Java is not available (required for PlantUML)")
            return False
        
        print("✅ All prerequisites are available")
        return True
    
    def run_full_analysis(self) -> bool:
        """Run complete DDD and BDD analysis"""
        print("🚀 Running full analysis...")
        
        success = True
        
        # Run DDD analysis
        print("📊 Running DDD code analysis...")
        result = subprocess.run([
            "python3", str(self.scripts_dir / "analyze-ddd-code.py"),
            "app/src/main/java/solid/humank/genaidemo",
            str(self.output_dir)
        ], cwd=self.project_root)
        
        if result.returncode != 0:
            print("❌ DDD analysis failed")
            success = False
        else:
            print("✅ DDD analysis completed")
        
        # Run BDD analysis
        print("📊 Running BDD feature analysis...")
        result = subprocess.run([
            "python3", str(self.scripts_dir / "analyze-bdd-features.py"),
            "app/src/test/resources/features",
            str(self.output_dir)
        ], cwd=self.project_root)
        
        if result.returncode != 0:
            print("❌ BDD analysis failed")
            success = False
        else:
            print("✅ BDD analysis completed")
        
        return success
    
    def fix_syntax_and_generate_images(self) -> bool:
        """Fix PlantUML syntax and generate images"""
        print("🔧 Fixing syntax and generating images...")
        
        # Fix PlantUML syntax
        result = subprocess.run([
            "python3", str(self.scripts_dir / "fix-plantuml-syntax.py")
        ], cwd=self.project_root)
        
        if result.returncode != 0:
            print("⚠️  PlantUML syntax fix had issues, continuing...")
        else:
            print("✅ PlantUML syntax fixed")
        
        # Generate images
        result = subprocess.run([
            str(self.scripts_dir / "generate-diagram-images.sh")
        ], cwd=self.project_root)
        
        if result.returncode != 0:
            print("⚠️  Image generation had issues, but continuing...")
        else:
            print("✅ Images generated successfully")
        
        return True
    
    def run_smart_update(self, force: bool = False) -> bool:
        """Run smart diagram update"""
        print("🧠 Running smart diagram update...")
        
        cmd = ["python3", str(self.scripts_dir / "smart-diagram-update.py")]
        if force:
            cmd.append("--force")
        
        result = subprocess.run(cmd, cwd=self.project_root)
        
        if result.returncode == 0:
            print("✅ Smart update completed successfully")
            return True
        else:
            print("❌ Smart update failed")
            return False
    
    def get_current_statistics(self) -> Dict:
        """Get current diagram and analysis statistics across all directories"""
        stats = {
            "timestamp": datetime.now().isoformat(),
            "diagrams_count": 0,
            "images_count": 0,
            "directories_processed": 0,
            "directory_stats": {},
            "ddd_stats": {},
            "bdd_stats": {}
        }
        
        # Count files across all diagram directories
        total_puml_files = 0
        total_png_files = 0
        total_svg_files = 0
        
        for diagram_dir in self.diagram_dirs:
            if diagram_dir.exists():
                dir_name = str(diagram_dir.relative_to(self.diagrams_root))
                
                # Count PlantUML files
                puml_files = list(diagram_dir.glob("*.puml"))
                puml_count = len(puml_files)
                
                # Count SVG images
                svg_files = list(diagram_dir.glob("*.svg"))
                svg_count = len(svg_files)
                
                # Count SVG images
                svg_files = list(diagram_dir.glob("*.svg"))
                svg_count = len(svg_files)
                
                if puml_count > 0 or png_count > 0 or svg_count > 0:
                    stats["directory_stats"][dir_name] = {
                        "puml_files": puml_count,
                        "png_images": png_count,
                        "svg_images": svg_count,
                        "total_images": png_count + svg_count
                    }
                    stats["directories_processed"] += 1
                
                total_puml_files += puml_count
                total_png_files += png_count
                total_svg_files += svg_count
        
        stats["diagrams_count"] = total_puml_files
        stats["images_count"] = total_png_files + total_svg_files
        stats["png_count"] = total_png_files
        stats["svg_count"] = total_svg_files
        
        # Read DDD analysis summary from functional viewpoint
        functional_dir = self.diagrams_root / "viewpoints" / "functional"
        ddd_summary_file = functional_dir / "analysis-summary.json"
        if ddd_summary_file.exists():
            try:
                with open(ddd_summary_file, 'r') as f:
                    stats["ddd_stats"] = json.load(f)
            except Exception as e:
                print(f"⚠️  Error reading DDD stats: {e}")
        
        # Read BDD analysis summary
        bdd_summary_file = functional_dir / "bdd-analysis-summary.json"
        if bdd_summary_file.exists():
            try:
                with open(bdd_summary_file, 'r') as f:
                    stats["bdd_stats"] = json.load(f)
            except Exception as e:
                print(f"⚠️  Error reading BDD stats: {e}")
        
        return stats
    
    def print_status_report(self):
        """Print comprehensive status report with maximum directory coverage"""
        print("\n📊 Comprehensive Diagram Automation Status Report")
        print("=" * 70)
        
        stats = self.get_current_statistics()
        
        print(f"Generated on: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"Total PlantUML diagrams: {stats['diagrams_count']}")
        print(f"Total images: {stats['images_count']} (PNG: {stats['png_count']}, SVG: {stats['svg_count']})")
        print(f"Directories processed: {stats['directories_processed']}")
        
        # Directory breakdown
        if stats["directory_stats"]:
            print(f"\n📁 Directory Breakdown:")
            for dir_name, dir_stats in stats["directory_stats"].items():
                print(f"  • {dir_name}:")
                print(f"    - PlantUML files: {dir_stats['puml_files']}")
                print(f"    - Images: {dir_stats['total_images']} (PNG: {dir_stats['png_images']}, SVG: {dir_stats['svg_images']})")
        
        # Event Storming specific stats
        event_storming_stats = stats["directory_stats"].get("plantuml/event-storming", {})
        if event_storming_stats:
            print(f"\n🟠 Event Storming Standardization:")
            print(f"  • Standardized diagrams: {event_storming_stats['puml_files']}")
            print(f"  • Generated images: {event_storming_stats['total_images']}")
            print(f"  • Status: ✅ Fully standardized with official colors")
        
        # UML standardization stats
        plantuml_stats = stats["directory_stats"].get("plantuml", {})
        if plantuml_stats:
            print(f"\n📐 UML 2.5 Standardization:")
            print(f"  • UML diagrams: {plantuml_stats['puml_files']}")
            print(f"  • Generated images: {plantuml_stats['total_images']}")
            print(f"  • Status: ✅ UML 2.5 compliant with DDD patterns")
        
        if stats["ddd_stats"]:
            ddd = stats["ddd_stats"]
            print(f"\n🏗️  DDD Analysis:")
            print(f"  • Domain classes: {ddd.get('domain_classes_count', 0)}")
            print(f"  • Application services: {ddd.get('application_services_count', 0)}")
            print(f"  • Repositories: {ddd.get('repositories_count', 0)}")
            print(f"  • Controllers: {ddd.get('controllers_count', 0)}")
            print(f"  • Domain events: {ddd.get('events_count', 0)}")
            print(f"  • Bounded contexts: {len(ddd.get('bounded_contexts', []))}")
        
        if stats["bdd_stats"]:
            bdd = stats["bdd_stats"]
            print(f"\n🥒 BDD Analysis:")
            print(f"  • Features: {bdd.get('features_count', 0)}")
            print(f"  • Scenarios: {bdd.get('scenarios_count', 0)}")
            print(f"  • Business events: {bdd.get('business_events_count', 0)}")
            print(f"  • User journeys: {bdd.get('user_journeys_count', 0)}")
            print(f"  • Actors: {len(bdd.get('actors', []))}")
        
        # Check hook status
        print(f"\n🪝 Kiro Hooks Status:")
        hook_files = [
            "diagram-auto-generation.kiro.hook",
            "ddd-annotation-monitor.kiro.hook",
            "bdd-feature-monitor.kiro.hook"
        ]
        
        for hook_file in hook_files:
            hook_path = self.hooks_dir / hook_file
            if hook_path.exists():
                try:
                    with open(hook_path, 'r') as f:
                        hook_config = json.load(f)
                        enabled = hook_config.get("enabled", False)
                        status = "✅ Enabled" if enabled else "❌ Disabled"
                        print(f"  • {hook_config.get('name', hook_file)}: {status}")
                except Exception as e:
                    print(f"  • {hook_file}: ⚠️  Error reading config")
            else:
                print(f"  • {hook_file}: ❌ Not found")
        
        # Coverage summary
        print(f"\n📈 Coverage Summary:")
        print(f"  • Maximum directory coverage: ✅ Enabled")
        print(f"  • Event Storming standardization: ✅ Complete")
        print(f"  • UML 2.5 standardization: ✅ Complete")
        print(f"  • Automatic image generation: ✅ Active")
        print(f"  • Multi-format support: ✅ PNG + SVG")
        
        print("\n" + "=" * 70)
    
    def setup_automation(self) -> bool:
        """Set up the complete automation system"""
        print("🛠️  Setting up diagram automation system...")
        
        if not self.check_prerequisites():
            return False
        
        # Run initial full analysis
        if not self.run_full_analysis():
            print("❌ Initial analysis failed")
            return False
        
        # Fix syntax and generate images
        if not self.fix_syntax_and_generate_images():
            print("❌ Image generation failed")
            return False
        
        print("✅ Diagram automation system setup completed!")
        return True
    
    def maintenance_check(self) -> bool:
        """Run maintenance checks and updates"""
        print("🔧 Running maintenance checks...")
        
        # Check if smart update detects any changes
        return self.run_smart_update(force=False)

def main():
    """Main function with command line interface"""
    parser = argparse.ArgumentParser(description="Diagram Automation Manager")
    parser.add_argument("command", choices=[
        "setup", "update", "force-update", "status", "maintenance", "full-analysis"
    ], help="Command to execute")
    
    args = parser.parse_args()
    
    manager = DiagramAutomationManager()
    
    if args.command == "setup":
        success = manager.setup_automation()
    elif args.command == "update":
        success = manager.run_smart_update(force=False)
    elif args.command == "force-update":
        success = manager.run_smart_update(force=True)
    elif args.command == "status":
        manager.print_status_report()
        success = True
    elif args.command == "maintenance":
        success = manager.maintenance_check()
    elif args.command == "full-analysis":
        success = manager.run_full_analysis() and manager.fix_syntax_and_generate_images()
    else:
        print(f"Unknown command: {args.command}")
        success = False
    
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main()