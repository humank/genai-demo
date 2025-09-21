#!/usr/bin/env python3
"""
Test Diagram Automation System

This script tests the complete diagram automation system to ensure it works correctly
with maximum directory coverage and all standardized diagrams.
"""

import os
import sys
import json
import subprocess
from pathlib import Path
from datetime import datetime

class DiagramAutomationTester:
    """Tests the diagram automation system comprehensively"""
    
    def __init__(self):
        self.project_root = Path.cwd()
        self.scripts_dir = self.project_root / "scripts"
        self.diagrams_root = self.project_root / "docs" / "diagrams"
        self.hooks_dir = self.project_root / ".kiro" / "hooks"
        
        # Test results
        self.test_results = {
            "timestamp": datetime.now().isoformat(),
            "tests_passed": 0,
            "tests_failed": 0,
            "test_details": []
        }
    
    def log_test(self, test_name: str, passed: bool, details: str = ""):
        """Log test result"""
        status = "✅ PASS" if passed else "❌ FAIL"
        print(f"{status} {test_name}")
        if details:
            print(f"    {details}")
        
        self.test_results["test_details"].append({
            "name": test_name,
            "passed": passed,
            "details": details
        })
        
        if passed:
            self.test_results["tests_passed"] += 1
        else:
            self.test_results["tests_failed"] += 1
    
    def test_prerequisites(self) -> bool:
        """Test if all prerequisites are available"""
        print("\n🔍 Testing Prerequisites...")
        
        # Test Python 3
        try:
            result = subprocess.run(["python3", "--version"], capture_output=True, text=True)
            self.log_test("Python 3 availability", result.returncode == 0, 
                         f"Version: {result.stdout.strip()}" if result.returncode == 0 else "Not found")
        except FileNotFoundError:
            self.log_test("Python 3 availability", False, "Python 3 not found")
            return False
        
        # Test Java
        try:
            result = subprocess.run(["java", "-version"], capture_output=True, text=True)
            self.log_test("Java availability", result.returncode == 0,
                         "Java runtime available" if result.returncode == 0 else "Not found")
        except FileNotFoundError:
            self.log_test("Java availability", False, "Java not found")
            return False
        
        # Test PlantUML JAR
        plantuml_jar = self.project_root / "tools" / "plantuml.jar"
        self.log_test("PlantUML JAR availability", plantuml_jar.exists(),
                     f"Found at {plantuml_jar}" if plantuml_jar.exists() else "Not found")
        
        return True
    
    def test_hook_configuration(self) -> bool:
        """Test hook configuration"""
        print("\n🪝 Testing Hook Configuration...")
        
        hook_file = self.hooks_dir / "diagram-auto-generation.kiro.hook"
        hook_exists = hook_file.exists()
        self.log_test("Hook file exists", hook_exists)
        
        if hook_exists:
            try:
                with open(hook_file, 'r') as f:
                    hook_config = json.load(f)
                
                enabled = hook_config.get("enabled", False)
                self.log_test("Hook is enabled", enabled)
                
                patterns = hook_config.get("when", {}).get("patterns", [])
                has_puml_pattern = any("*.puml" in pattern for pattern in patterns)
                self.log_test("Hook monitors .puml files", has_puml_pattern,
                             f"Patterns: {patterns}")
                
                return enabled and has_puml_pattern
            except Exception as e:
                self.log_test("Hook configuration valid", False, f"Error: {e}")
                return False
        
        return False
    
    def test_script_availability(self) -> bool:
        """Test if all required scripts are available"""
        print("\n📜 Testing Script Availability...")
        
        required_scripts = [
            "diagram-automation-manager.py",
            "generate-diagram-images.sh",
            "fix-plantuml-syntax.py"
        ]
        
        all_available = True
        for script in required_scripts:
            script_path = self.scripts_dir / script
            exists = script_path.exists()
            self.log_test(f"Script {script}", exists)
            if not exists:
                all_available = False
        
        return all_available
    
    def test_directory_structure(self) -> bool:
        """Test diagram directory structure"""
        print("\n📁 Testing Directory Structure...")
        
        expected_dirs = [
            "docs/diagrams/plantuml",
            "docs/diagrams/plantuml/event-storming",
            "docs/diagrams/plantuml/structural",
            "docs/diagrams/viewpoints/functional",
            "docs/diagrams/perspectives/security"
        ]
        
        all_exist = True
        for dir_path in expected_dirs:
            full_path = self.project_root / dir_path
            exists = full_path.exists()
            self.log_test(f"Directory {dir_path}", exists)
            if not exists:
                all_exist = False
        
        return all_exist
    
    def test_standardized_diagrams(self) -> bool:
        """Test standardized diagram files"""
        print("\n🎨 Testing Standardized Diagrams...")
        
        # Test Event Storming standardized files
        event_storming_dir = self.diagrams_root / "plantuml" / "event-storming"
        standardized_files = [
            "big-picture-standardized.puml",
            "process-level-standardized.puml",
            "design-level-standardized.puml",
            "event-storming-colors.puml"
        ]
        
        all_exist = True
        for file_name in standardized_files:
            file_path = event_storming_dir / file_name
            exists = file_path.exists()
            self.log_test(f"Event Storming: {file_name}", exists)
            if not exists:
                all_exist = False
        
        # Test UML standardized files
        plantuml_dir = self.diagrams_root / "plantuml"
        uml_files = [
            "uml-2.5-colors.puml",
            "class-diagram.puml",
            "domain-model-diagram.puml",
            "sequence-diagram.puml"
        ]
        
        for file_name in uml_files:
            file_path = plantuml_dir / file_name
            exists = file_path.exists()
            self.log_test(f"UML 2.5: {file_name}", exists)
            if not exists:
                all_exist = False
        
        return all_exist
    
    def test_image_generation(self) -> bool:
        """Test image generation functionality"""
        print("\n🖼️  Testing Image Generation...")
        
        # Run the image generation script
        try:
            result = subprocess.run([
                "bash", str(self.scripts_dir / "generate-diagram-images.sh")
            ], capture_output=True, text=True, cwd=self.project_root)
            
            generation_success = result.returncode == 0
            self.log_test("Image generation script execution", generation_success,
                         f"Exit code: {result.returncode}")
            
            if not generation_success:
                print(f"    STDOUT: {result.stdout}")
                print(f"    STDERR: {result.stderr}")
                return False
            
            # Check if images were actually generated
            png_count = 0
            svg_count = 0
            
            for puml_file in self.diagrams_root.rglob("*.puml"):
                svg_file = puml_file.with_suffix(".svg")
                svg_file = puml_file.with_suffix(".svg")
                
                if png_file.exists():
                    png_count += 1
                if svg_file.exists():
                    svg_count += 1
            
            self.log_test("PNG images generated", png_count > 0, f"Count: {png_count}")
            self.log_test("SVG images generated", svg_count > 0, f"Count: {svg_count}")
            
            return png_count > 0 and svg_count > 0
            
        except Exception as e:
            self.log_test("Image generation script execution", False, f"Error: {e}")
            return False
    
    def test_automation_manager(self) -> bool:
        """Test the automation manager script"""
        print("\n🤖 Testing Automation Manager...")
        
        try:
            # Test status command
            result = subprocess.run([
                "python3", str(self.scripts_dir / "diagram-automation-manager.py"), "status"
            ], capture_output=True, text=True, cwd=self.project_root)
            
            status_success = result.returncode == 0
            self.log_test("Automation manager status command", status_success)
            
            if not status_success:
                print(f"    STDERR: {result.stderr}")
            
            return status_success
            
        except Exception as e:
            self.log_test("Automation manager execution", False, f"Error: {e}")
            return False
    
    def run_all_tests(self) -> bool:
        """Run all tests"""
        print("🧪 Starting Comprehensive Diagram Automation Tests")
        print("=" * 60)
        
        test_methods = [
            self.test_prerequisites,
            self.test_hook_configuration,
            self.test_script_availability,
            self.test_directory_structure,
            self.test_standardized_diagrams,
            self.test_image_generation,
            self.test_automation_manager
        ]
        
        all_passed = True
        for test_method in test_methods:
            try:
                if not test_method():
                    all_passed = False
            except Exception as e:
                print(f"❌ Test {test_method.__name__} failed with exception: {e}")
                all_passed = False
        
        return all_passed
    
    def print_summary(self):
        """Print test summary"""
        print("\n📊 Test Summary")
        print("=" * 40)
        print(f"Tests passed: {self.test_results['tests_passed']}")
        print(f"Tests failed: {self.test_results['tests_failed']}")
        print(f"Total tests: {self.test_results['tests_passed'] + self.test_results['tests_failed']}")
        
        if self.test_results['tests_failed'] == 0:
            print("\n🎉 All tests passed! Diagram automation system is fully functional.")
            print("✅ Maximum directory coverage is working correctly")
            print("✅ Event Storming standardization is active")
            print("✅ UML 2.5 standardization is active")
            print("✅ Automatic image generation is working")
        else:
            print(f"\n⚠️  {self.test_results['tests_failed']} test(s) failed. Please check the issues above.")
            print("🔧 Run the following to fix common issues:")
            print("   python3 scripts/diagram-automation-manager.py setup")

def main():
    """Main function"""
    tester = DiagramAutomationTester()
    
    success = tester.run_all_tests()
    tester.print_summary()
    
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main()