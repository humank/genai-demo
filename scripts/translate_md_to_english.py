#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
智能翻譯腳本 - 將中文 Markdown 文件翻譯成英文
作者: Amazon Q
日期: 2025-08-21

功能:
1. 自動檢測包含中文的 Markdown 文件
2. 保持目錄結構翻譯到 docs/en 目錄
3. 修正文檔內的相對連結
4. 保持 Markdown 格式完整性
"""

import os
import re
import shutil
import subprocess
import tempfile
from pathlib import Path
from typing import List, Tuple

class MarkdownTranslator:
    def __init__(self, project_root: str):
        self.project_root = Path(project_root)
        self.en_docs_dir = self.project_root / "docs" / "en"
        self.translated_count = 0
        self.skipped_count = 0
        self.error_count = 0
        
    def contains_chinese(self, file_path: Path) -> bool:
        """檢測文件是否包含中文字符"""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
                # 檢查是否包含中文字符 (Unicode 範圍 4e00-9fff)
                return bool(re.search(r'[\u4e00-\u9fff]', content))
        except Exception as e:
            print(f"  ❌ 讀取文件失敗: {e}")
            return False
    
    def get_target_path(self, source_path: Path) -> Path:
        """確定目標文件路徑"""
        relative_path = source_path.relative_to(self.project_root)
        
        if str(relative_path).startswith('docs/'):
            # 如果文件在 docs 目錄下，放到 docs/en 對應位置
            target_path = self.en_docs_dir / str(relative_path)[5:]  # 移除 'docs/' 前綴
        else:
            # 其他文件放到 docs/en 根目錄下，保持相對路徑
            target_path = self.en_docs_dir / relative_path
            
        return target_path
    
    def translate_with_q_chat(self, input_file: Path, output_file: Path) -> bool:
        """使用 Amazon Q CLI 進行翻譯"""
        try:
            # 創建翻譯提示
            prompt = f"""Please translate the following Markdown document from Traditional Chinese to English.

Requirements:
1. Maintain all Markdown formatting (headers, links, code blocks, tables, etc.)
2. Keep all URLs and file paths unchanged
3. Translate technical terms appropriately for software development context
4. Preserve code snippets and command examples exactly as they are
5. Keep proper nouns (like "Amazon Q", "Spring Boot", "Docker") in English
6. Maintain the document structure and hierarchy
7. For relative links to other .md files, change them to point to the English version in docs/en/ directory
8. Keep badges, shields, and external links unchanged

Here is the content to translate:

{input_file.read_text(encoding='utf-8')}"""

            # 嘗試使用 q chat
            with tempfile.NamedTemporaryFile(mode='w', suffix='.txt', delete=False) as temp_file:
                temp_file.write(prompt)
                temp_file_path = temp_file.name
            
            try:
                result = subprocess.run(
                    ['q', 'chat', '--input', temp_file_path],
                    capture_output=True,
                    text=True,
                    timeout=300  # 5分鐘超時
                )
                
                if result.returncode == 0 and result.stdout.strip():
                    output_file.write_text(result.stdout, encoding='utf-8')
                    return True
                else:
                    print(f"  ⚠️  Q CLI 翻譯失敗: {result.stderr}")
                    return False
                    
            finally:
                os.unlink(temp_file_path)
                
        except subprocess.TimeoutExpired:
            print("  ⏰ 翻譯超時")
            return False
        except FileNotFoundError:
            print("  ❌ 找不到 q 命令")
            return False
        except Exception as e:
            print(f"  ❌ 翻譯過程出錯: {e}")
            return False
    
    def create_placeholder_translation(self, input_file: Path, output_file: Path):
        """創建佔位符翻譯（當自動翻譯不可用時）"""
        content = input_file.read_text(encoding='utf-8')
        
        # 基本的連結修正
        content = re.sub(r'docs/zh-tw/', 'docs/en/', content)
        content = re.sub(r'\]\(docs/', '](..', content)
        
        # 添加翻譯標記
        translated_content = f"""<!-- This document needs manual translation from Chinese to English -->
<!-- 此文檔需要從中文手動翻譯為英文 -->

{content}"""
        
        output_file.write_text(translated_content, encoding='utf-8')
    
    def fix_links(self, file_path: Path):
        """修正文檔中的相對連結"""
        try:
            content = file_path.read_text(encoding='utf-8')
            
            # 修正各種相對路徑連結
            patterns = [
                (r'\]\(docs/', r'](../'),
                (r'\]\(\.\./zh-tw/', r'](../en/'),
                (r'\]\(\.\./\.\./docs/', r'](../'),
                (r'README\.md\)', r'README.md)'),
            ]
            
            for pattern, replacement in patterns:
                content = re.sub(pattern, replacement, content)
            
            file_path.write_text(content, encoding='utf-8')
            
        except Exception as e:
            print(f"  ⚠️  修正連結時出錯: {e}")
    
    def process_file(self, source_file: Path):
        """處理單個文件"""
        relative_path = source_file.relative_to(self.project_root)
        print(f"📄 檢查文件: {relative_path}")
        
        # 跳過已經在 docs/en 目錄中的文件
        if '/docs/en/' in str(source_file):
            print("  ⏭️  跳過英文目錄中的文件")
            self.skipped_count += 1
            return
        
        # 檢查是否包含中文
        if not self.contains_chinese(source_file):
            print("  ℹ️  文件不包含中文內容，跳過")
            self.skipped_count += 1
            return
        
        # 確定目標文件路徑
        target_file = self.get_target_path(source_file)
        
        # 創建目標目錄
        target_file.parent.mkdir(parents=True, exist_ok=True)
        
        print(f"  📝 翻譯目標: {target_file.relative_to(self.project_root)}")
        
        try:
            # 嘗試使用 Q CLI 翻譯
            if self.translate_with_q_chat(source_file, target_file):
                print("  ✅ 使用 Q CLI 翻譯完成")
            else:
                # 使用備用方法
                print("  🔄 使用備用翻譯方法...")
                self.create_placeholder_translation(source_file, target_file)
                print("  ⚠️  創建了需要手動翻譯的版本")
            
            # 修正連結
            self.fix_links(target_file)
            print("  🔗 連結修正完成")
            
            self.translated_count += 1
            
        except Exception as e:
            print(f"  ❌ 處理文件時出錯: {e}")
            self.error_count += 1
    
    def create_english_readme(self):
        """創建英文文檔的 README"""
        readme_content = f"""# GenAI Demo - E-commerce Platform Documentation (English)

> **Language Selection**  
> 🇺🇸 **English**: You are reading the English version  
> 🇹🇼 **繁體中文**: [繁體中文文檔](../README.md)

This directory contains the English translation of the GenAI Demo project documentation.

## 📚 Documentation Structure

- **Architecture**: System architecture and design patterns
- **API**: API documentation and integration guides  
- **Deployment**: Deployment guides and configurations
- **Development**: Development guides and best practices
- **Design**: Design principles and guidelines
- **Reports**: Project reports and analysis
- **Diagrams**: System diagrams and visualizations

## 🔗 Quick Links

- [Project Overview](../README.md)
- [Architecture Documentation](architecture/)
- [API Documentation](api/)
- [Development Guide](development/)
- [Deployment Guide](deployment/)

## 📝 Translation Notes

This documentation is automatically translated from Traditional Chinese. If you find any translation issues or improvements, please feel free to contribute.

---

**Generated on**: {subprocess.run(['date', '+%Y-%m-%d %H:%M:%S'], capture_output=True, text=True).stdout.strip()}
**Translation Tool**: Amazon Q CLI
"""
        
        readme_path = self.en_docs_dir / "README.md"
        readme_path.write_text(readme_content, encoding='utf-8')
        print(f"📚 創建英文文檔索引: {readme_path}")
    
    def run(self):
        """執行翻譯流程"""
        print("🌍 開始智能翻譯專案中的中文 Markdown 文件...")
        print(f"📁 英文文檔目錄: {self.en_docs_dir}")
        
        # 創建英文文檔目錄
        self.en_docs_dir.mkdir(parents=True, exist_ok=True)
        
        # 查找所有 Markdown 文件
        print("🔍 掃描 Markdown 文件...")
        md_files = list(self.project_root.rglob("*.md"))
        
        for md_file in md_files:
            if md_file.is_file():
                self.process_file(md_file)
        
        # 創建英文文檔索引
        self.create_english_readme()
        
        # 輸出統計結果
        print("\n🎉 翻譯處理完成！")
        print("📊 處理統計：")
        print(f"   ✅ 已翻譯文件數: {self.translated_count}")
        print(f"   ⏭️  跳過文件數: {self.skipped_count}")
        print(f"   ❌ 錯誤文件數: {self.error_count}")
        print(f"   📁 英文文檔目錄: {self.en_docs_dir}")
        print("\n🔍 建議後續步驟：")
        print(f"   1. 檢查翻譯品質: ls -la {self.en_docs_dir}")
        print(f"   2. 驗證連結正確性: grep -r '\\](.*\\.md)' {self.en_docs_dir}")
        print("   3. 手動調整專業術語翻譯")
        print("   4. 提交變更到版本控制")

def main():
    project_root = "/Users/yikaikao/git/genai-demo"
    translator = MarkdownTranslator(project_root)
    translator.run()

if __name__ == "__main__":
    main()
