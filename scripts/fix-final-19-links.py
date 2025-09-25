#!/usr/bin/env python3
"""
Fix the final 19 broken links
"""
import os

def fix_path_issues():
    """Fix all remaining path issues"""
    
    path_corrections = [
        # Fix docs/perspectives/evolution/README.md
        {
            'file': 'docs/perspectives/evolution/README.md',
            'replacements': [
                ('../design/refactoring-guide.md', '../../design/refactoring-guide.md'),
            ]
        },
        
        # Fix docs/reports/README.md
        {
            'file': 'docs/reports/README.md',
            'replacements': [
                ('../reports-summaries/project-management/project-summary-2025.md', 
                 '../../reports-summaries/project-management/project-summary-2025.md'),
                ('../reports-summaries/architecture-design/ddd-record-refactoring-summary.md', 
                 '../../reports-summaries/architecture-design/ddd-record-refactoring-summary.md'),
            ]
        },
        
        # Fix docs/viewpoints/development/README.md
        {
            'file': 'docs/viewpoints/development/README.md',
            'replacements': [
                ('../design/refactoring-guide.md', '../../design/refactoring-guide.md'),
                ('../development/coding-standards.md', '../../development/coding-standards.md'),
            ]
        },
        
        # Fix docs/viewpoints/operational/configuration-guide.md
        {
            'file': 'docs/viewpoints/operational/configuration-guide.md',
            'replacements': [
                ('../troubleshooting/observability-troubleshooting.md', 
                 '../../troubleshooting/observability-troubleshooting.md'),
            ]
        },
        
        # Fix docs/viewpoints/operational/observability-overview.md
        {
            'file': 'docs/viewpoints/operational/observability-overview.md',
            'replacements': [
                ('../troubleshooting/observability-troubleshooting.md', 
                 '../../troubleshooting/observability-troubleshooting.md'),
                ('../api/observability-api.md', '../../api/observability-api.md'),
            ]
        }
    ]
    
    fixed_count = 0
    for correction in path_corrections:
        file_path = correction['file']
        if os.path.exists(file_path):
            try:
                with open(file_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                original_content = content
                for old_path, new_path in correction['replacements']:
                    content = content.replace(old_path, new_path)
                
                if content != original_content:
                    with open(file_path, 'w', encoding='utf-8') as f:
                        f.write(content)
                    fixed_count += 1
                    print(f"✅ Fixed paths in: {file_path}")
                    
            except Exception as e:
                print(f"❌ Error fixing {file_path}: {e}")
    
    return fixed_count

def fix_reports_summaries_paths():
    """Fix reports-summaries path issues"""
    
    files_to_fix = [
        'reports-summaries/general/local-changes-summary-2025-09-17.md',
        'reports-summaries/general/local-changes-summary-2025-09-17_1.md'
    ]
    
    fixed_count = 0
    for file_path in files_to_fix:
        if os.path.exists(file_path):
            try:
                with open(file_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                original_content = content
                
                # Fix README.md paths - from reports-summaries/general/ to root
                # The correct path should be ../../README.md
                content = content.replace('../../../README.md', '../../README.md')
                content = content.replace('../../../docs/testing/test-performance-monitoring.md', 
                                        '../../docs/testing/test-performance-monitoring.md')
                
                if content != original_content:
                    with open(file_path, 'w', encoding='utf-8') as f:
                        f.write(content)
                    fixed_count += 1
                    print(f"✅ Fixed duplicate paths in: {file_path}")
                    
            except Exception as e:
                print(f"❌ Error fixing {file_path}: {e}")
    
    return fixed_count

def verify_path_resolution():
    """Verify that paths resolve correctly"""
    
    test_cases = [
        ('docs/perspectives/evolution/', '../../design/refactoring-guide.md'),
        ('docs/reports/', '../../reports-summaries/project-management/project-summary-2025.md'),
        ('docs/viewpoints/development/', '../../design/refactoring-guide.md'),
        ('docs/viewpoints/development/', '../../development/coding-standards.md'),
        ('docs/viewpoints/operational/', '../../troubleshooting/observability-troubleshooting.md'),
        ('docs/viewpoints/operational/', '../../api/observability-api.md'),
        ('reports-summaries/general/', '../../README.md'),
        ('reports-summaries/general/', '../../docs/testing/test-performance-monitoring.md')
    ]
    
    valid_count = 0
    invalid_count = 0
    
    for base_dir, relative_path in test_cases:
        full_path = os.path.normpath(os.path.join(base_dir, relative_path))
        if os.path.exists(full_path):
            valid_count += 1
            print(f"✅ Path resolves: {base_dir} + {relative_path} = {full_path}")
        else:
            invalid_count += 1
            print(f"❌ Path invalid: {base_dir} + {relative_path} = {full_path}")
    
    return valid_count, invalid_count

def main():
    print("🔧 Fixing the final 19 broken links...")
    
    # Fix main path issues
    path_fixes = fix_path_issues()
    print(f"🔗 Fixed {path_fixes} main path issues")
    
    # Fix reports-summaries duplicate paths
    reports_fixes = fix_reports_summaries_paths()
    print(f"📄 Fixed {reports_fixes} reports-summaries path issues")
    
    # Verify path resolution
    print("\n📋 Verifying path resolution...")
    valid, invalid = verify_path_resolution()
    print(f"✅ {valid} paths resolve correctly")
    print(f"❌ {invalid} paths still invalid")
    
    total_fixes = path_fixes + reports_fixes
    print(f"\n📊 Total fixes applied: {total_fixes}")
    print(f"🎯 Target: 100% link success rate")

if __name__ == "__main__":
    main()