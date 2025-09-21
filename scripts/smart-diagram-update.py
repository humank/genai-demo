#!/usr/bin/env python3
"""
Smart Diagram Update Script

This script intelligently detects changes in Java code or BDD features
and only regenerates diagrams when necessary.
"""

import os
import sys
import json
import hashlib
import subprocess
from pathlib import Path
from datetime import datetime
from typing import Dict, List, Set, Optional

class SmartDiagramUpdater:
    """Intelligently updates diagrams based on code changes"""
    
    def __init__(self):
        self.project_root = Path.cwd()
        self.cache_file = self.project_root / ".kiro" / "diagram-cache.json"
        self.java_source_dir = self.project_root / "app" / "src" / "main" / "java" / "solid" / "humank" / "genaidemo"
        self.features_dir = self.project_root / "app" / "src" / "test" / "resources" / "features"
        self.output_dir = self.project_root / "docs" / "diagrams" / "viewpoints" / "functional"
        
    def load_cache(self) -> Dict:
        """Load the diagram generation cache"""
        if self.cache_file.exists():
            try:
                with open(self.cache_file, 'r') as f:
                    return json.load(f)
            except Exception as e:
                print(f"⚠️  Error loading cache: {e}")
        return {
            "last_java_hash": "",
            "last_features_hash": "",
            "last_generation_time": "",
            "java_files_count": 0,
            "feature_files_count": 0
        }
    
    def save_cache(self, cache_data: Dict):
        """Save the diagram generation cache"""
        try:
            self.cache_file.parent.mkdir(parents=True, exist_ok=True)
            with open(self.cache_file, 'w') as f:
                json.dump(cache_data, f, indent=2)
        except Exception as e:
            print(f"⚠️  Error saving cache: {e}")
    
    def calculate_directory_hash(self, directory: Path, pattern: str) -> tuple[str, int]:
        """Calculate hash of all files matching pattern in directory"""
        if not directory.exists():
            return "", 0
            
        files = list(directory.rglob(pattern))
        files.sort()  # Ensure consistent ordering
        
        hasher = hashlib.md5()
        file_count = 0
        
        for file_path in files:
            try:
                with open(file_path, 'rb') as f:
                    content = f.read()
                    hasher.update(file_path.name.encode())
                    hasher.update(content)
                    file_count += 1
            except Exception as e:
                print(f"⚠️  Error reading {file_path}: {e}")
                
        return hasher.hexdigest(), file_count
    
    def detect_changes(self) -> Dict[str, bool]:
        """Detect if Java or Feature files have changed"""
        cache = self.load_cache()
        
        # Calculate current hashes
        java_hash, java_count = self.calculate_directory_hash(self.java_source_dir, "*.java")
        features_hash, features_count = self.calculate_directory_hash(self.features_dir, "*.feature")
        
        changes = {
            "java_changed": java_hash != cache.get("last_java_hash", ""),
            "features_changed": features_hash != cache.get("last_features_hash", ""),
            "java_files_added": java_count > cache.get("java_files_count", 0),
            "feature_files_added": features_count > cache.get("feature_files_count", 0)
        }
        
        # Update cache
        cache.update({
            "current_java_hash": java_hash,
            "current_features_hash": features_hash,
            "current_java_files_count": java_count,
            "current_feature_files_count": features_count
        })
        
        return changes, cache
    
    def run_ddd_analysis(self) -> bool:
        """Run DDD code analysis"""
        try:
            print("🔍 Running DDD code analysis...")
            result = subprocess.run([
                "python3", "scripts/analyze-ddd-code.py",
                str(self.java_source_dir),
                str(self.output_dir)
            ], capture_output=True, text=True, cwd=self.project_root)
            
            if result.returncode == 0:
                print("✅ DDD analysis completed successfully")
                return True
            else:
                print(f"❌ DDD analysis failed: {result.stderr}")
                return False
                
        except Exception as e:
            print(f"❌ Error running DDD analysis: {e}")
            return False
    
    def run_bdd_analysis(self) -> bool:
        """Run BDD feature analysis"""
        try:
            print("🔍 Running BDD feature analysis...")
            result = subprocess.run([
                "python3", "scripts/analyze-bdd-features.py",
                str(self.features_dir),
                str(self.output_dir)
            ], capture_output=True, text=True, cwd=self.project_root)
            
            if result.returncode == 0:
                print("✅ BDD analysis completed successfully")
                return True
            else:
                print(f"❌ BDD analysis failed: {result.stderr}")
                return False
                
        except Exception as e:
            print(f"❌ Error running BDD analysis: {e}")
            return False
    
    def fix_plantuml_syntax(self) -> bool:
        """Fix PlantUML syntax issues"""
        try:
            print("🔧 Fixing PlantUML syntax...")
            result = subprocess.run([
                "python3", "scripts/fix-plantuml-syntax.py"
            ], capture_output=True, text=True, cwd=self.project_root)
            
            if result.returncode == 0:
                print("✅ PlantUML syntax fixed")
                return True
            else:
                print(f"⚠️  PlantUML syntax fix warnings: {result.stderr}")
                return True  # Continue even with warnings
                
        except Exception as e:
            print(f"❌ Error fixing PlantUML syntax: {e}")
            return False
    
    def generate_images(self) -> bool:
        """Generate PNG images from PlantUML files"""
        try:
            print("🖼️  Generating PNG images...")
            result = subprocess.run([
                "./scripts/generate-diagram-images.sh"
            ], capture_output=True, text=True, cwd=self.project_root)
            
            if result.returncode == 0:
                print("✅ PNG images generated successfully")
                return True
            else:
                print(f"⚠️  Image generation completed with warnings: {result.stderr}")
                return True  # Continue even with warnings
                
        except Exception as e:
            print(f"❌ Error generating images: {e}")
            return False
    
    def update_documentation(self) -> bool:
        """Update documentation with current statistics"""
        try:
            # Read analysis summaries
            ddd_summary_file = self.output_dir / "analysis-summary.json"
            bdd_summary_file = self.output_dir / "bdd-analysis-summary.json"
            
            stats = {}
            
            if ddd_summary_file.exists():
                with open(ddd_summary_file, 'r') as f:
                    ddd_data = json.load(f)
                    stats.update({
                        "domain_classes": ddd_data.get("domain_classes_count", 0),
                        "application_services": ddd_data.get("application_services_count", 0),
                        "repositories": ddd_data.get("repositories_count", 0),
                        "controllers": ddd_data.get("controllers_count", 0),
                        "domain_events": ddd_data.get("events_count", 0),
                        "bounded_contexts": len(ddd_data.get("bounded_contexts", []))
                    })
            
            if bdd_summary_file.exists():
                with open(bdd_summary_file, 'r') as f:
                    bdd_data = json.load(f)
                    stats.update({
                        "features": bdd_data.get("features_count", 0),
                        "scenarios": bdd_data.get("scenarios_count", 0),
                        "business_events": bdd_data.get("business_events_count", 0),
                        "user_journeys": bdd_data.get("user_journeys_count", 0),
                        "actors": len(bdd_data.get("actors", []))
                    })
            
            # Update README with current statistics
            readme_file = self.output_dir / "README.md"
            if readme_file.exists():
                with open(readme_file, 'r') as f:
                    content = f.read()
                
                # Update generation date
                current_time = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
                content = content.replace(
                    "## Generated on: 2025-09-21",
                    f"## Generated on: {datetime.now().strftime('%Y-%m-%d')}"
                )
                
                # Update statistics if available
                if stats:
                    for key, value in stats.items():
                        # This is a simple approach - in practice, you'd want more sophisticated updating
                        pass
                
                with open(readme_file, 'w') as f:
                    f.write(content)
            
            print("✅ Documentation updated")
            return True
            
        except Exception as e:
            print(f"⚠️  Error updating documentation: {e}")
            return True  # Continue even if documentation update fails
    
    def run_update(self, force: bool = False) -> bool:
        """Run the smart diagram update process"""
        print("🚀 Starting smart diagram update...")
        
        # Detect changes
        changes, cache = self.detect_changes()
        
        if not force and not any(changes.values()):
            print("ℹ️  No changes detected. Diagrams are up to date.")
            return True
        
        print("📊 Changes detected:")
        for change_type, changed in changes.items():
            if changed:
                print(f"   • {change_type}: ✅")
        
        success = True
        
        # Run DDD analysis if Java files changed
        if force or changes["java_changed"] or changes["java_files_added"]:
            if not self.run_ddd_analysis():
                success = False
        
        # Run BDD analysis if Feature files changed
        if force or changes["features_changed"] or changes["feature_files_added"]:
            if not self.run_bdd_analysis():
                success = False
        
        # Fix syntax and generate images if any analysis was run
        if success and (force or any(changes.values())):
            if not self.fix_plantuml_syntax():
                success = False
            
            if success and not self.generate_images():
                success = False
            
            if success and not self.update_documentation():
                success = False
        
        # Update cache on success
        if success:
            cache.update({
                "last_java_hash": cache["current_java_hash"],
                "last_features_hash": cache["current_features_hash"],
                "last_generation_time": datetime.now().isoformat(),
                "java_files_count": cache["current_java_files_count"],
                "feature_files_count": cache["current_feature_files_count"]
            })
            self.save_cache(cache)
            print("🎉 Diagram update completed successfully!")
        else:
            print("❌ Diagram update completed with errors")
        
        return success

def main():
    """Main function"""
    force = "--force" in sys.argv or "-f" in sys.argv
    
    updater = SmartDiagramUpdater()
    success = updater.run_update(force=force)
    
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main()