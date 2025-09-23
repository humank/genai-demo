#!/usr/bin/env python3
"""
Hook Status Checker

This script checks the status and configuration of all Kiro hooks,
identifies potential conflicts, and provides recommendations.
"""

import json
import os
from pathlib import Path
from typing import Dict, List, Set

class HookStatusChecker:
    """Checks hook configurations for conflicts and issues"""
    
    def __init__(self):
        self.project_root = Path.cwd()
        self.hooks_dir = self.project_root / ".kiro" / "hooks"
        self.hooks = {}
        
    def load_hooks(self):
        """Load all hook configurations"""
        if not self.hooks_dir.exists():
            print("❌ .kiro/hooks directory not found")
            return False
            
        hook_files = list(self.hooks_dir.glob("*.kiro.hook"))
        
        for hook_file in hook_files:
            try:
                with open(hook_file, 'r', encoding='utf-8') as f:
                    hook_config = json.load(f)
                    self.hooks[hook_file.name] = {
                        'config': hook_config,
                        'file_path': hook_file
                    }
            except Exception as e:
                print(f"⚠️  Error loading {hook_file}: {e}")
                
        return len(self.hooks) > 0
    
    def analyze_conflicts(self) -> Dict:
        """Analyze potential conflicts between hooks"""
        conflicts = {
            'pattern_overlaps': [],
            'duplicate_actions': [],
            'resource_conflicts': []
        }
        
        # Check for pattern overlaps
        enabled_hooks = {name: hook for name, hook in self.hooks.items() 
                        if hook['config'].get('enabled', False)}
        
        for hook1_name, hook1 in enabled_hooks.items():
            patterns1 = set(hook1['config'].get('when', {}).get('patterns', []))
            
            for hook2_name, hook2 in enabled_hooks.items():
                if hook1_name >= hook2_name:  # Avoid duplicate comparisons
                    continue
                    
                patterns2 = set(hook2['config'].get('when', {}).get('patterns', []))
                
                # Check for overlapping patterns
                overlaps = self._find_pattern_overlaps(patterns1, patterns2)
                if overlaps:
                    conflicts['pattern_overlaps'].append({
                        'hook1': hook1_name,
                        'hook2': hook2_name,
                        'overlapping_patterns': list(overlaps)
                    })
        
        # Check for duplicate actions (same scripts being called)
        script_usage = {}
        for hook_name, hook in enabled_hooks.items():
            prompt = hook['config'].get('then', {}).get('prompt', '')
            scripts = self._extract_scripts_from_prompt(prompt)
            
            for script in scripts:
                if script not in script_usage:
                    script_usage[script] = []
                script_usage[script].append(hook_name)
        
        for script, hooks in script_usage.items():
            if len(hooks) > 1:
                conflicts['duplicate_actions'].append({
                    'script': script,
                    'hooks': hooks
                })
        
        return conflicts
    
    def _find_pattern_overlaps(self, patterns1: Set[str], patterns2: Set[str]) -> Set[str]:
        """Find overlapping file patterns between two sets"""
        overlaps = set()
        
        for p1 in patterns1:
            for p2 in patterns2:
                if self._patterns_overlap(p1, p2):
                    overlaps.add(f"{p1} ↔ {p2}")
        
        return overlaps
    
    def _patterns_overlap(self, pattern1: str, pattern2: str) -> bool:
        """Check if two glob patterns overlap"""
        # Simple overlap detection - can be enhanced
        if pattern1 == pattern2:
            return True
        
        # Check if one pattern is a subset of another
        if pattern1 in pattern2 or pattern2 in pattern1:
            return True
        
        # Check for common prefixes with wildcards
        parts1 = pattern1.split('/')
        parts2 = pattern2.split('/')
        
        min_len = min(len(parts1), len(parts2))
        for i in range(min_len):
            if parts1[i] != parts2[i] and '**' not in parts1[i] and '**' not in parts2[i]:
                return False
        
        return True
    
    def _extract_scripts_from_prompt(self, prompt: str) -> List[str]:
        """Extract script names from hook prompts"""
        scripts = []
        
        # Look for python script calls
        import re
        python_scripts = re.findall(r'python3\s+scripts/([^\s]+\.py)', prompt)
        scripts.extend(python_scripts)
        
        # Look for shell script calls
        shell_scripts = re.findall(r'scripts/([^\s]+\.sh)', prompt)
        scripts.extend(shell_scripts)
        
        return scripts
    
    def generate_recommendations(self, conflicts: Dict) -> List[str]:
        """Generate recommendations based on conflicts"""
        recommendations = []
        
        if conflicts['pattern_overlaps']:
            recommendations.append(
                "🔧 **Pattern Overlap Resolution**: Consider disabling redundant hooks or "
                "consolidating overlapping patterns into a single hook for better performance."
            )
        
        if conflicts['duplicate_actions']:
            recommendations.append(
                "⚡ **Script Execution Optimization**: Multiple hooks are calling the same scripts. "
                "Consider using a single hook with intelligent routing to avoid resource conflicts."
            )
        
        if not conflicts['pattern_overlaps'] and not conflicts['duplicate_actions']:
            recommendations.append(
                "✅ **No Conflicts Detected**: Hook configurations are well-structured with no conflicts."
            )
        
        # General recommendations
        recommendations.extend([
            "📊 **Performance**: Use smart caching mechanisms to avoid unnecessary script executions.",
            "🔍 **Monitoring**: Regularly check hook execution logs for performance issues.",
            "🛠️ **Maintenance**: Keep hook configurations updated as the project evolves."
        ])
        
        return recommendations
    
    def print_status_report(self):
        """Print comprehensive hook status report"""
        print("🎣 Kiro Hook Status Report")
        print("=" * 50)
        
        if not self.hooks:
            print("❌ No hooks found")
            return
        
        # Hook overview
        print(f"\n📋 Hook Overview ({len(self.hooks)} hooks found)")
        print("-" * 30)
        
        for hook_name, hook_data in self.hooks.items():
            config = hook_data['config']
            enabled = config.get('enabled', False)
            status = "✅ Enabled" if enabled else "❌ Disabled"
            name = config.get('name', hook_name)
            version = config.get('version', 'N/A')
            
            print(f"• **{name}** ({hook_name})")
            print(f"  Status: {status} | Version: {version}")
            
            # Show patterns
            patterns = config.get('when', {}).get('patterns', [])
            if patterns:
                print(f"  Patterns: {', '.join(patterns[:2])}{'...' if len(patterns) > 2 else ''}")
        
        # Conflict analysis
        print(f"\n🔍 Conflict Analysis")
        print("-" * 30)
        
        conflicts = self.analyze_conflicts()
        
        if conflicts['pattern_overlaps']:
            print("⚠️  **Pattern Overlaps Detected:**")
            for overlap in conflicts['pattern_overlaps']:
                print(f"   • {overlap['hook1']} ↔ {overlap['hook2']}")
                for pattern in overlap['overlapping_patterns']:
                    print(f"     - {pattern}")
        
        if conflicts['duplicate_actions']:
            print("⚠️  **Duplicate Script Executions:**")
            for dup in conflicts['duplicate_actions']:
                print(f"   • Script: {dup['script']}")
                print(f"     Called by: {', '.join(dup['hooks'])}")
        
        if not conflicts['pattern_overlaps'] and not conflicts['duplicate_actions']:
            print("✅ No conflicts detected")
        
        # Recommendations
        print(f"\n💡 Recommendations")
        print("-" * 30)
        
        recommendations = self.generate_recommendations(conflicts)
        for i, rec in enumerate(recommendations, 1):
            print(f"{i}. {rec}")
        
        # System health check
        print(f"\n🏥 System Health Check")
        print("-" * 30)
        
        # Check if required scripts exist
        required_scripts = [
            'diagram-automation-manager.py',
            'smart-diagram-update.py',
            'analyze-ddd-code.py',
            'analyze-bdd-features.py'
        ]
        
        scripts_dir = self.project_root / "scripts"
        missing_scripts = []
        
        for script in required_scripts:
            if not (scripts_dir / script).exists():
                missing_scripts.append(script)
        
        if missing_scripts:
            print(f"❌ Missing required scripts: {', '.join(missing_scripts)}")
        else:
            print("✅ All required scripts are available")
        
        # Check if output directory exists
        output_dir = self.project_root / "docs" / "diagrams" / "viewpoints" / "functional"
        if output_dir.exists():
            puml_count = len(list(output_dir.glob("*.puml")))
            svg_count = len(list(output_dir.glob("*.svg")))
            print(f"✅ Output directory exists: {puml_count} PlantUML files, {svg_count} SVG images")
        else:
            print("❌ Output directory not found")
        
        print("\n" + "=" * 50)

def main():
    """Main function"""
    checker = HookStatusChecker()
    
    if not checker.load_hooks():
        print("❌ Failed to load hook configurations")
        return
    
    checker.print_status_report()

if __name__ == "__main__":
    main()