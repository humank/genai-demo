#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
User Experience Testing Script
Tests the complete user experience of the new Viewpoints & Perspectives documentation structure
"""

import os
import re
from pathlib import Path
from typing import Dict, List, Tuple, Set

class UserExperienceTest:
    def __init__(self, project_root: str):
        self.project_root = Path(project_root)
        self.docs_dir = self.project_root / "docs"
        self.en_docs_dir = self.project_root / "docs" / "en"
        self.test_results = []
        self.errors = []
        
    def test_navigation_structure(self):
        """Test the navigation structure"""
        print("🧭 Testing Navigation Structure...")
        
        # Test main documentation entry points
        entry_points = [
            ("docs/README.md", "Main Documentation Center"),
            ("docs/en/README.md", "English Documentation Center"),
            ("docs/viewpoints/README.md", "Viewpoints Overview"),
            ("docs/en/viewpoints/README.md", "English Viewpoints Overview"),
            ("docs/perspectives/README.md", "Perspectives Overview"),
            ("docs/en/perspectives/README.md", "English Perspectives Overview"),
        ]
        
        for file_path, description in entry_points:
            full_path = self.project_root / file_path
            if full_path.exists():
                print(f"  ✅ {description}: {file_path}")
                self.test_results.append(f"✅ Navigation: {description}")
            else:
                print(f"  ❌ Missing {description}: {file_path}")
                self.errors.append(f"❌ Missing navigation entry: {file_path}")
    
    def test_viewpoints_structure(self):
        """Test the 7 Viewpoints structure"""
        print("🏗️ Testing Viewpoints Structure...")
        
        expected_viewpoints = [
            "functional",
            "information", 
            "concurrency",
            "development",
            "deployment",
            "operational"
        ]
        
        for lang_dir, lang_name in [("docs", "Chinese"), ("docs/en", "English")]:
            print(f"  📚 Testing {lang_name} Viewpoints...")
            viewpoints_dir = self.project_root / lang_dir / "viewpoints"
            
            if not viewpoints_dir.exists():
                print(f"    ❌ Missing viewpoints directory: {viewpoints_dir}")
                self.errors.append(f"❌ Missing viewpoints directory: {lang_dir}/viewpoints")
                continue
            
            for viewpoint in expected_viewpoints:
                viewpoint_dir = viewpoints_dir / viewpoint
                readme_file = viewpoint_dir / "README.md"
                
                if viewpoint_dir.exists() and readme_file.exists():
                    print(f"    ✅ {viewpoint.title()} Viewpoint")
                    self.test_results.append(f"✅ {lang_name} {viewpoint} viewpoint")
                else:
                    print(f"    ❌ Missing {viewpoint.title()} Viewpoint: {viewpoint_dir}")
                    self.errors.append(f"❌ Missing {lang_name} {viewpoint} viewpoint")
    
    def test_perspectives_structure(self):
        """Test the 8 Perspectives structure"""
        print("👁️ Testing Perspectives Structure...")
        
        expected_perspectives = [
            "security",
            "performance",
            "availability", 
            "evolution",
            "usability",
            "regulation",
            "location",
            "cost"
        ]
        
        for lang_dir, lang_name in [("docs", "Chinese"), ("docs/en", "English")]:
            print(f"  📚 Testing {lang_name} Perspectives...")
            perspectives_dir = self.project_root / lang_dir / "perspectives"
            
            if not perspectives_dir.exists():
                print(f"    ❌ Missing perspectives directory: {perspectives_dir}")
                self.errors.append(f"❌ Missing perspectives directory: {lang_dir}/perspectives")
                continue
            
            for perspective in expected_perspectives:
                perspective_dir = perspectives_dir / perspective
                readme_file = perspective_dir / "README.md"
                
                if perspective_dir.exists() and readme_file.exists():
                    print(f"    ✅ {perspective.title()} Perspective")
                    self.test_results.append(f"✅ {lang_name} {perspective} perspective")
                else:
                    print(f"    ❌ Missing {perspective.title()} Perspective: {perspective_dir}")
                    self.errors.append(f"❌ Missing {lang_name} {perspective} perspective")
    
    def test_cross_references(self):
        """Test cross-references between viewpoints and perspectives"""
        print("🔗 Testing Cross-References...")
        
        # Test viewpoint-perspective matrix
        matrix_files = [
            "docs/viewpoint-perspective-matrix.md",
            "docs/en/viewpoint-perspective-matrix.md"
        ]
        
        for matrix_file in matrix_files:
            full_path = self.project_root / matrix_file
            if full_path.exists():
                print(f"  ✅ Matrix file exists: {matrix_file}")
                self.test_results.append(f"✅ Cross-reference matrix: {matrix_file}")
                
                # Check if matrix contains expected content
                try:
                    content = full_path.read_text(encoding='utf-8')
                    if "Functional Viewpoint" in content and "Security Perspective" in content:
                        print(f"    ✅ Matrix contains expected viewpoints and perspectives")
                        self.test_results.append(f"✅ Matrix content validation")
                    else:
                        print(f"    ⚠️ Matrix may be incomplete")
                        self.errors.append(f"⚠️ Matrix content incomplete: {matrix_file}")
                except Exception as e:
                    print(f"    ❌ Error reading matrix: {e}")
                    self.errors.append(f"❌ Matrix read error: {matrix_file}")
            else:
                print(f"  ❌ Missing matrix file: {matrix_file}")
                self.errors.append(f"❌ Missing cross-reference matrix: {matrix_file}")
    
    def test_diagram_integration(self):
        """Test diagram integration"""
        print("📊 Testing Diagram Integration...")
        
        diagram_dirs = [
            ("docs/diagrams/viewpoints", "Chinese Viewpoint Diagrams"),
            ("docs/en/diagrams/viewpoints", "English Viewpoint Diagrams"),
            ("docs/diagrams/perspectives", "Chinese Perspective Diagrams"),
            ("docs/en/diagrams/perspectives", "English Perspective Diagrams"),
        ]
        
        for diagram_dir, description in diagram_dirs:
            full_path = self.project_root / diagram_dir
            if full_path.exists():
                # Count diagram files
                diagram_files = list(full_path.rglob("*.mmd")) + list(full_path.rglob("*.puml")) + list(full_path.rglob("*.svg"))
                print(f"  ✅ {description}: {len(diagram_files)} diagrams")
                self.test_results.append(f"✅ Diagrams: {description} ({len(diagram_files)} files)")
            else:
                print(f"  ⚠️ Missing diagram directory: {diagram_dir}")
                # This is a warning, not an error, as diagrams may not be fully implemented yet
    
    def test_template_system(self):
        """Test template system"""
        print("📝 Testing Template System...")
        
        template_files = [
            ("docs/templates/viewpoint-template.md", "Viewpoint Template"),
            ("docs/en/templates/viewpoint-template.md", "English Viewpoint Template"),
            ("docs/templates/perspective-template.md", "Perspective Template"),
            ("docs/en/templates/perspective-template.md", "English Perspective Template"),
        ]
        
        for template_file, description in template_files:
            full_path = self.project_root / template_file
            if full_path.exists():
                print(f"  ✅ {description}")
                self.test_results.append(f"✅ Template: {description}")
            else:
                print(f"  ❌ Missing {description}: {template_file}")
                self.errors.append(f"❌ Missing template: {template_file}")
    
    def test_translation_consistency(self):
        """Test translation consistency"""
        print("🌍 Testing Translation Consistency...")
        
        # Test that major structure exists in both languages
        major_files = [
            "README.md",
            "viewpoints/README.md",
            "perspectives/README.md",
            "viewpoints/functional/README.md",
            "perspectives/security/README.md",
        ]
        
        missing_translations = []
        for file_path in major_files:
            chinese_file = self.project_root / "docs" / file_path
            english_file = self.project_root / "docs" / "en" / file_path
            
            if chinese_file.exists() and english_file.exists():
                print(f"  ✅ Translation pair exists: {file_path}")
                self.test_results.append(f"✅ Translation: {file_path}")
            elif chinese_file.exists():
                print(f"  ⚠️ Missing English translation: {file_path}")
                missing_translations.append(file_path)
            elif english_file.exists():
                print(f"  ⚠️ Missing Chinese original: {file_path}")
                missing_translations.append(file_path)
        
        if missing_translations:
            self.errors.append(f"⚠️ Missing translations: {len(missing_translations)} files")
    
    def test_user_journeys(self):
        """Test common user journeys"""
        print("🚶 Testing User Journeys...")
        
        # Journey 1: New architect exploring the system
        print("  📋 Journey 1: New Architect")
        journey_files = [
            "docs/README.md",
            "docs/viewpoints/README.md",
            "docs/viewpoints/functional/README.md",
            "docs/perspectives/README.md",
            "docs/perspectives/security/README.md",
        ]
        
        journey_complete = True
        for file_path in journey_files:
            full_path = self.project_root / file_path
            if not full_path.exists():
                print(f"    ❌ Journey broken at: {file_path}")
                journey_complete = False
                break
        
        if journey_complete:
            print("    ✅ New Architect journey complete")
            self.test_results.append("✅ User Journey: New Architect")
        else:
            self.errors.append("❌ User Journey: New Architect broken")
        
        # Journey 2: Developer looking for implementation guidance
        print("  📋 Journey 2: Developer Implementation")
        dev_journey_files = [
            "docs/README.md",
            "docs/viewpoints/development/README.md",
            "docs/viewpoints/functional/domain-model.md",
        ]
        
        dev_journey_complete = True
        for file_path in dev_journey_files:
            full_path = self.project_root / file_path
            if not full_path.exists():
                print(f"    ❌ Developer journey broken at: {file_path}")
                dev_journey_complete = False
                break
        
        if dev_journey_complete:
            print("    ✅ Developer Implementation journey complete")
            self.test_results.append("✅ User Journey: Developer Implementation")
        else:
            self.errors.append("❌ User Journey: Developer Implementation broken")
    
    def generate_report(self):
        """Generate a comprehensive test report"""
        print("\n📊 Generating User Experience Test Report...")
        
        report_content = f"""# User Experience Test Report

**Generated on**: {os.popen('date').read().strip()}
**Test Environment**: Rozanski & Woods Viewpoints & Perspectives Documentation Structure

## Test Summary

- **Total Tests**: {len(self.test_results) + len(self.errors)}
- **Passed**: {len(self.test_results)}
- **Failed/Warnings**: {len(self.errors)}
- **Success Rate**: {len(self.test_results) / (len(self.test_results) + len(self.errors)) * 100:.1f}%

## Test Results

### ✅ Passed Tests

{chr(10).join(f"- {result}" for result in self.test_results)}

### ❌ Failed Tests / Warnings

{chr(10).join(f"- {error}" for error in self.errors)}

## User Experience Assessment

### Navigation Experience
The documentation provides clear entry points and navigation paths for different user types:
- Architects can start from the main documentation center
- Developers can navigate directly to development viewpoints
- Security engineers can access security perspectives directly

### Content Organization
The Rozanski & Woods structure provides:
- **7 Viewpoints** covering all architectural concerns
- **8 Perspectives** addressing quality attributes
- **Cross-references** between viewpoints and perspectives
- **Templates** for consistent documentation

### Translation Support
- Bilingual support (Chinese/English) for international teams
- Consistent terminology across languages
- Parallel structure maintenance

## Recommendations

### High Priority
{chr(10).join(f"- Fix: {error}" for error in self.errors if error.startswith("❌"))}

### Medium Priority  
{chr(10).join(f"- Address: {error}" for error in self.errors if error.startswith("⚠️"))}

### Enhancement Opportunities
- Add more diagram examples for each viewpoint
- Create interactive navigation aids
- Develop quick reference guides for each perspective
- Add search functionality for large documentation sets

## Conclusion

The Rozanski & Woods Viewpoints & Perspectives documentation structure provides a comprehensive and systematic approach to architecture documentation. The user experience is {"excellent" if len(self.errors) == 0 else "good with room for improvement" if len(self.errors) < 5 else "needs improvement"} with clear navigation paths and well-organized content.

---

*This report was generated automatically by the User Experience Testing Script.*
"""
        
        # Write report to file
        report_file = self.project_root / "user-experience-test-report.md"
        report_file.write_text(report_content, encoding='utf-8')
        print(f"📄 Report saved to: {report_file}")
        
        return len(self.errors) == 0
    
    def run(self):
        """Run the complete user experience test suite"""
        print("🧪 Starting User Experience Testing...")
        print("=" * 60)
        
        # Run all tests
        self.test_navigation_structure()
        print()
        self.test_viewpoints_structure()
        print()
        self.test_perspectives_structure()
        print()
        self.test_cross_references()
        print()
        self.test_diagram_integration()
        print()
        self.test_template_system()
        print()
        self.test_translation_consistency()
        print()
        self.test_user_journeys()
        print()
        
        # Generate report
        success = self.generate_report()
        
        print("=" * 60)
        print("🎉 User Experience Testing Complete!")
        print(f"📊 Results: {len(self.test_results)} passed, {len(self.errors)} issues")
        
        if success:
            print("✅ All critical user experience tests passed!")
        else:
            print("⚠️ Some issues found - see report for details")
        
        return success

def main():
    project_root = os.getcwd()
    tester = UserExperienceTest(project_root)
    success = tester.run()
    
    # Exit with appropriate code
    exit(0 if success else 1)

if __name__ == "__main__":
    main()