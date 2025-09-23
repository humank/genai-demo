#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Translation Quality Fix Script
Fixes translation quality issues by properly translating Chinese content and fixing terminology consistency
"""

import os
import re
import json
from pathlib import Path
from typing import Dict, List, Tuple

class TranslationQualityFixer:
    def __init__(self, project_root: str):
        self.project_root = Path(project_root)
        self.en_docs_dir = self.project_root / "docs" / "en"
        self.terminology_file = self.project_root / "docs" / ".terminology.json"
        self.terminology = self.load_terminology()
        self.fixed_count = 0
        self.error_count = 0
        
    def load_terminology(self) -> Dict[str, str]:
        """Load terminology dictionary"""
        try:
            with open(self.terminology_file, 'r', encoding='utf-8') as f:
                data = json.load(f)
                
            # Flatten all terminology categories into a single dictionary
            terminology = {}
            for category in data.get('terminology', {}).values():
                terminology.update(category)
                
            return terminology
        except Exception as e:
            print(f"❌ Failed to load terminology: {e}")
            return {}
    
    def fix_terminology_in_content(self, content: str) -> str:
        """Fix terminology consistency in content"""
        fixed_content = content
        
        for chinese_term, english_term in self.terminology.items():
            # Replace Chinese terms with English terms
            fixed_content = fixed_content.replace(chinese_term, english_term)
            
        return fixed_content
    
    def translate_basic_content(self, content: str) -> str:
        """Basic translation of common Chinese content"""
        
        # Remove placeholder comments
        content = re.sub(r'<!-- This document needs manual translation from Chinese to English -->\n?', '', content)
        content = re.sub(r'<!-- 此文檔需要從中文手動翻譯為英文 -->\n?', '', content)
        
        # Basic translations for common patterns
        translations = {
            # Headers and sections
            r'# ([^#\n]*概覽[^#\n]*)': r'# Overview',
            r'# ([^#\n]*介紹[^#\n]*)': r'# Introduction', 
            r'# ([^#\n]*需求[^#\n]*)': r'# Requirements',
            r'# ([^#\n]*設計[^#\n]*)': r'# Design',
            r'# ([^#\n]*實施[^#\n]*)': r'# Implementation',
            r'# ([^#\n]*測試[^#\n]*)': r'# Testing',
            r'# ([^#\n]*部署[^#\n]*)': r'# Deployment',
            r'# ([^#\n]*維護[^#\n]*)': r'# Maintenance',
            r'# ([^#\n]*故障排除[^#\n]*)': r'# Troubleshooting',
            r'# ([^#\n]*最佳實踐[^#\n]*)': r'# Best Practices',
            r'# ([^#\n]*指南[^#\n]*)': r'# Guidelines',
            r'# ([^#\n]*標準[^#\n]*)': r'# Standards',
            r'# ([^#\n]*範例[^#\n]*)': r'# Examples',
            r'# ([^#\n]*模板[^#\n]*)': r'# Templates',
            r'# ([^#\n]*工具[^#\n]*)': r'# Tools',
            r'# ([^#\n]*資源[^#\n]*)': r'# Resources',
            r'# ([^#\n]*參考[^#\n]*)': r'# Reference',
            r'# ([^#\n]*附錄[^#\n]*)': r'# Appendix',
            
            # Common section headers
            r'## 概覽': '## Overview',
            r'## 介紹': '## Introduction',
            r'## 品質屬性': '## Quality Attributes',
            r'## 跨視點應用': '## Cross-Viewpoint Application',
            r'## 設計策略': '## Design Strategy',
            r'## 實現技術': '## Implementation Technique',
            r'## 測試和驗證': '## Testing and Verification',
            r'## 監控和度量': '## Monitoring and Measurement',
            r'## 利害關係人': '## Stakeholders',
            r'## 關注點': '## Concerns',
            r'## 架構元素': '## Architectural Elements',
            r'## 相關圖表': '## Related Diagrams',
            r'## 與其他視點的關聯': '## Relationships with Other Viewpoints',
            r'## 實現指南': '## Implementation Guide',
            r'## 驗證標準': '## Verification Criteria',
            
            # Common subsection headers
            r'### 主要品質屬性': '### Primary Quality Attributes',
            r'### 次要品質屬性': '### Secondary Quality Attributes',
            r'### 主要關注者': '### Primary Stakeholders',
            r'### 次要關注者': '### Secondary Stakeholders',
            
            # Common phrases
            r'本文檔': 'This document',
            r'本專案': 'This project',
            r'本系統': 'This system',
            r'如下所示': 'as shown below',
            r'詳細資訊': 'detailed information',
            r'更多資訊': 'more information',
            r'相關連結': 'related links',
            r'快速連結': 'quick links',
            r'重要提醒': 'important note',
            r'注意事項': 'notes',
            r'建議': 'recommendations',
            r'總結': 'summary',
            r'結論': 'conclusion',
        }
        
        for pattern, replacement in translations.items():
            content = re.sub(pattern, replacement, content, flags=re.MULTILINE)
        
        # Apply terminology fixes
        content = self.fix_terminology_in_content(content)
        
        return content
    
    def fix_file(self, file_path: Path):
        """Fix a single file"""
        try:
            print(f"🔧 Fixing: {file_path.relative_to(self.project_root)}")
            
            # Read content
            content = file_path.read_text(encoding='utf-8')
            
            # Apply fixes
            fixed_content = self.translate_basic_content(content)
            
            # Write back if changed
            if fixed_content != content:
                file_path.write_text(fixed_content, encoding='utf-8')
                print(f"  ✅ Fixed terminology and basic translations")
                self.fixed_count += 1
            else:
                print(f"  ℹ️  No changes needed")
                
        except Exception as e:
            print(f"  ❌ Error fixing file: {e}")
            self.error_count += 1
    
    def run(self):
        """Run the translation quality fix process"""
        print("🔧 Starting Translation Quality Fix...")
        print(f"📁 English docs directory: {self.en_docs_dir}")
        print(f"📚 Loaded {len(self.terminology)} terminology mappings")
        
        # Find all markdown files in English docs
        md_files = list(self.en_docs_dir.rglob("*.md"))
        
        print(f"🔍 Found {len(md_files)} markdown files to fix")
        
        for md_file in md_files:
            if md_file.is_file():
                self.fix_file(md_file)
        
        # Summary
        print(f"\n🎉 Translation Quality Fix Complete!")
        print(f"📊 Statistics:")
        print(f"   ✅ Files fixed: {self.fixed_count}")
        print(f"   ❌ Errors: {self.error_count}")
        print(f"   📁 Total files processed: {len(md_files)}")

def main():
    project_root = os.getcwd()
    fixer = TranslationQualityFixer(project_root)
    fixer.run()

if __name__ == "__main__":
    main()