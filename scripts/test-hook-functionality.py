#!/usr/bin/env python3
"""
Hook Functionality Tester

This script tests the hook functionality by simulating file changes
and verifying that the automation system responds correctly.
"""

import os
import sys
import tempfile
import subprocess
from pathlib import Path
from datetime import datetime

class HookFunctionalityTester:
    """Tests hook functionality"""
    
    def __init__(self):
        self.project_root = Path.cwd()
        self.test_results = []
        
    def test_diagram_automation_manager(self) -> bool:
        """Test the diagram automation manager"""
        print("🧪 Testing diagram automation manager...")
        
        try:
            # Test status command
            result = subprocess.run([
                "python3", "scripts/diagram-automation-manager.py", "status"
            ], capture_output=True, text=True, cwd=self.project_root)
            
            if result.returncode == 0:
                print("✅ Status command works")
                self.test_results.append(("Status Command", True, "Working correctly"))
            else:
                print(f"❌ Status command failed: {result.stderr}")
                self.test_results.append(("Status Command", False, result.stderr))
                return False
            
            # Test update command (should be safe as it uses smart detection)
            result = subprocess.run([
                "python3", "scripts/diagram-automation-manager.py", "update"
            ], capture_output=True, text=True, cwd=self.project_root)
            
            if result.returncode == 0:
                print("✅ Update command works")
                self.test_results.append(("Update Command", True, "Working correctly"))
            else:
                print(f"❌ Update command failed: {result.stderr}")
                self.test_results.append(("Update Command", False, result.stderr))
                return False
                
            return True
            
        except Exception as e:
            print(f"❌ Error testing automation manager: {e}")
            self.test_results.append(("Automation Manager", False, str(e)))
            return False
    
    def test_smart_diagram_update(self) -> bool:
        """Test the smart diagram update script"""
        print("🧪 Testing smart diagram update...")
        
        try:
            result = subprocess.run([
                "python3", "scripts/smart-diagram-update.py"
            ], capture_output=True, text=True, cwd=self.project_root)
            
            if result.returncode == 0:
                print("✅ Smart diagram update works")
                self.test_results.append(("Smart Update", True, "Working correctly"))
                return True
            else:
                print(f"❌ Smart diagram update failed: {result.stderr}")
                self.test_results.append(("Smart Update", False, result.stderr))
                return False
                
        except Exception as e:
            print(f"❌ Error testing smart update: {e}")
            self.test_results.append(("Smart Update", False, str(e)))
            return False
    
    def test_individual_analyzers(self) -> bool:
        """Test individual analyzer scripts"""
        print("🧪 Testing individual analyzers...")
        
        success = True
        
        # Test DDD analyzer
        try:
            result = subprocess.run([
                "python3", "scripts/analyze-ddd-code.py",
                "app/src/main/java/solid/humank/genaidemo",
                "docs/diagrams/viewpoints/functional"
            ], capture_output=True, text=True, cwd=self.project_root)
            
            if result.returncode == 0:
                print("✅ DDD analyzer works")
                self.test_results.append(("DDD Analyzer", True, "Working correctly"))
            else:
                print(f"❌ DDD analyzer failed: {result.stderr}")
                self.test_results.append(("DDD Analyzer", False, result.stderr))
                success = False
                
        except Exception as e:
            print(f"❌ Error testing DDD analyzer: {e}")
            self.test_results.append(("DDD Analyzer", False, str(e)))
            success = False
        
        # Test BDD analyzer
        try:
            result = subprocess.run([
                "python3", "scripts/analyze-bdd-features.py",
                "app/src/test/resources/features",
                "docs/diagrams/viewpoints/functional"
            ], capture_output=True, text=True, cwd=self.project_root)
            
            if result.returncode == 0:
                print("✅ BDD analyzer works")
                self.test_results.append(("BDD Analyzer", True, "Working correctly"))
            else:
                print(f"❌ BDD analyzer failed: {result.stderr}")
                self.test_results.append(("BDD Analyzer", False, result.stderr))
                success = False
                
        except Exception as e:
            print(f"❌ Error testing BDD analyzer: {e}")
            self.test_results.append(("BDD Analyzer", False, str(e)))
            success = False
        
        return success
    
    def test_syntax_fixer(self) -> bool:
        """Test the PlantUML syntax fixer"""
        print("🧪 Testing PlantUML syntax fixer...")
        
        try:
            result = subprocess.run([
                "python3", "scripts/fix-plantuml-syntax.py"
            ], capture_output=True, text=True, cwd=self.project_root)
            
            if result.returncode == 0:
                print("✅ Syntax fixer works")
                self.test_results.append(("Syntax Fixer", True, "Working correctly"))
                return True
            else:
                print(f"❌ Syntax fixer failed: {result.stderr}")
                self.test_results.append(("Syntax Fixer", False, result.stderr))
                return False
                
        except Exception as e:
            print(f"❌ Error testing syntax fixer: {e}")
            self.test_results.append(("Syntax Fixer", False, str(e)))
            return False
    
    def test_file_structure(self) -> bool:
        """Test that all required files and directories exist"""
        print("🧪 Testing file structure...")
        
        required_files = [
            "scripts/diagram-automation-manager.py",
            "scripts/smart-diagram-update.py",
            "scripts/analyze-ddd-code.py",
            "scripts/analyze-bdd-features.py",
            "scripts/fix-plantuml-syntax.py",
            "scripts/generate-diagram-images.sh",
            ".kiro/hooks/diagram-auto-generation.kiro.hook"
        ]
        
        required_dirs = [
            "docs/diagrams/viewpoints/functional",
            "app/src/main/java/solid/humank/genaidemo",
            "app/src/test/resources/features"
        ]
        
        success = True
        
        for file_path in required_files:
            if (self.project_root / file_path).exists():
                print(f"✅ {file_path} exists")
            else:
                print(f"❌ {file_path} missing")
                self.test_results.append((f"File: {file_path}", False, "Missing"))
                success = False
        
        for dir_path in required_dirs:
            if (self.project_root / dir_path).exists():
                print(f"✅ {dir_path} exists")
            else:
                print(f"❌ {dir_path} missing")
                self.test_results.append((f"Directory: {dir_path}", False, "Missing"))
                success = False
        
        if success:
            self.test_results.append(("File Structure", True, "All required files and directories exist"))
        
        return success
    
    def simulate_hook_trigger(self) -> bool:
        """Simulate a hook trigger by running the automation manager"""
        print("🧪 Simulating hook trigger...")
        
        try:
            # This simulates what would happen when a hook is triggered
            result = subprocess.run([
                "python3", "scripts/diagram-automation-manager.py", "maintenance"
            ], capture_output=True, text=True, cwd=self.project_root)
            
            if result.returncode == 0:
                print("✅ Hook simulation successful")
                self.test_results.append(("Hook Simulation", True, "Maintenance check completed"))
                return True
            else:
                print(f"❌ Hook simulation failed: {result.stderr}")
                self.test_results.append(("Hook Simulation", False, result.stderr))
                return False
                
        except Exception as e:
            print(f"❌ Error simulating hook: {e}")
            self.test_results.append(("Hook Simulation", False, str(e)))
            return False
    
    def run_all_tests(self) -> bool:
        """Run all tests and return overall success"""
        print("🚀 Starting Hook Functionality Tests")
        print("=" * 50)
        
        tests = [
            ("File Structure Check", self.test_file_structure),
            ("Diagram Automation Manager", self.test_diagram_automation_manager),
            ("Smart Diagram Update", self.test_smart_diagram_update),
            ("Individual Analyzers", self.test_individual_analyzers),
            ("Syntax Fixer", self.test_syntax_fixer),
            ("Hook Simulation", self.simulate_hook_trigger)
        ]
        
        overall_success = True
        
        for test_name, test_func in tests:
            print(f"\n🧪 Running: {test_name}")
            print("-" * 30)
            
            try:
                success = test_func()
                if not success:
                    overall_success = False
            except Exception as e:
                print(f"❌ Test failed with exception: {e}")
                self.test_results.append((test_name, False, str(e)))
                overall_success = False
        
        # Print summary
        print(f"\n📊 Test Summary")
        print("=" * 50)
        
        passed = sum(1 for _, success, _ in self.test_results if success)
        total = len(self.test_results)
        
        print(f"Tests Passed: {passed}/{total}")
        print(f"Overall Status: {'✅ PASS' if overall_success else '❌ FAIL'}")
        
        if not overall_success:
            print(f"\n❌ Failed Tests:")
            for test_name, success, message in self.test_results:
                if not success:
                    print(f"   • {test_name}: {message}")
        
        print("\n" + "=" * 50)
        
        return overall_success

def main():
    """Main function"""
    tester = HookFunctionalityTester()
    success = tester.run_all_tests()
    
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main()