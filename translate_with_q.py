#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
改進版翻譯腳本 - 使用 Amazon Q 進行實際翻譯
作者: Amazon Q
日期: 2025-08-21

功能:
1. 逐個翻譯包含中文的 Markdown 文件
2. 使用 Amazon Q 進行高品質翻譯
3. 保持 Markdown 格式和連結完整性
4. 修正相對路徑連結
"""

import os
import re
import subprocess
import tempfile
from pathlib import Path
from typing import List

class QTranslator:
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
                return bool(re.search(r'[\u4e00-\u9fff]', content))
        except Exception:
            return False
    
    def translate_file_with_q(self, source_file: Path, target_file: Path) -> bool:
        """使用 Amazon Q 翻譯單個文件"""
        try:
            print(f"  🤖 使用 Amazon Q 翻譯...")
            
            # 讀取原文件內容
            content = source_file.read_text(encoding='utf-8')
            
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
9. Translate Chinese comments in code blocks to English
10. Keep the same tone and style as the original document

Please provide only the translated content without any additional explanations.

---

{content}"""

            # 使用 q chat 進行翻譯
            result = subprocess.run(
                ['q', 'chat'],
                input=prompt,
                text=True,
                capture_output=True,
                timeout=300  # 5分鐘超時
            )
            
            if result.returncode == 0 and result.stdout.strip():
                # 清理輸出（移除可能的前綴說明）
                translated_content = result.stdout.strip()
                
                # 如果輸出包含解釋性文字，嘗試提取純翻譯內容
                if translated_content.startswith("Here") or translated_content.startswith("I'll"):
                    lines = translated_content.split('\n')
                    # 找到第一個 Markdown 標題或內容行
                    start_idx = 0
                    for i, line in enumerate(lines):
                        if line.startswith('#') or (line.strip() and not line.startswith('Here') and not line.startswith('I')):
                            start_idx = i
                            break
                    translated_content = '\n'.join(lines[start_idx:])
                
                # 寫入翻譯結果
                target_file.write_text(translated_content, encoding='utf-8')
                return True
            else:
                print(f"  ❌ 翻譯失敗: {result.stderr}")
                return False
                
        except subprocess.TimeoutExpired:
            print("  ⏰ 翻譯超時")
            return False
        except Exception as e:
            print(f"  ❌ 翻譯過程出錯: {e}")
            return False
    
    def fix_links_in_file(self, file_path: Path):
        """修正文檔中的相對連結"""
        try:
            content = file_path.read_text(encoding='utf-8')
            
            # 修正各種相對路徑連結
            patterns = [
                (r'\]\(docs/', r'](../'),
                (r'\]\(\.\./zh-tw/', r'](../en/'),
                (r'\]\(\.\./\.\./docs/', r'](../'),
            ]
            
            for pattern, replacement in patterns:
                content = re.sub(pattern, replacement, content)
            
            file_path.write_text(content, encoding='utf-8')
            
        except Exception as e:
            print(f"  ⚠️  修正連結時出錯: {e}")
    
    def get_target_path(self, source_path: Path) -> Path:
        """確定目標文件路徑"""
        relative_path = source_path.relative_to(self.project_root)
        
        if str(relative_path).startswith('docs/'):
            target_path = self.en_docs_dir / str(relative_path)[5:]
        else:
            target_path = self.en_docs_dir / relative_path
            
        return target_path
    
    def translate_single_file(self, source_file: Path):
        """翻譯單個文件"""
        relative_path = source_file.relative_to(self.project_root)
        print(f"\n📄 翻譯文件: {relative_path}")
        
        # 跳過英文目錄中的文件
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
        target_file.parent.mkdir(parents=True, exist_ok=True)
        
        print(f"  📝 翻譯目標: {target_file.relative_to(self.project_root)}")
        
        try:
            # 使用 Amazon Q 翻譯
            if self.translate_file_with_q(source_file, target_file):
                # 修正連結
                self.fix_links_in_file(target_file)
                print("  ✅ 翻譯完成")
                self.translated_count += 1
            else:
                print("  ❌ 翻譯失敗")
                self.error_count += 1
                
        except Exception as e:
            print(f"  ❌ 處理文件時出錯: {e}")
            self.error_count += 1
    
    def run_interactive_translation(self):
        """執行互動式翻譯"""
        print("🌍 開始互動式翻譯中文 Markdown 文件...")
        print(f"📁 英文文檔目錄: {self.en_docs_dir}")
        
        # 查找包含中文的文件
        chinese_files = []
        for md_file in self.project_root.rglob("*.md"):
            if (md_file.is_file() and 
                '/docs/en/' not in str(md_file) and 
                self.contains_chinese(md_file)):
                chinese_files.append(md_file)
        
        print(f"🔍 找到 {len(chinese_files)} 個包含中文的文件")
        
        if not chinese_files:
            print("✅ 沒有需要翻譯的文件")
            return
        
        # 顯示文件列表
        print("\n📋 需要翻譯的文件:")
        for i, file_path in enumerate(chinese_files, 1):
            relative_path = file_path.relative_to(self.project_root)
            print(f"  {i:2d}. {relative_path}")
        
        print("\n🎯 翻譯選項:")
        print("  a) 翻譯所有文件")
        print("  s) 選擇特定文件翻譯")
        print("  q) 退出")
        
        choice = input("\n請選擇 (a/s/q): ").lower().strip()
        
        if choice == 'q':
            print("👋 退出翻譯")
            return
        elif choice == 'a':
            # 翻譯所有文件
            for file_path in chinese_files:
                self.translate_single_file(file_path)
        elif choice == 's':
            # 選擇特定文件
            while True:
                try:
                    selection = input(f"\n請輸入文件編號 (1-{len(chinese_files)}) 或 'done' 完成: ").strip()
                    if selection.lower() == 'done':
                        break
                    
                    file_idx = int(selection) - 1
                    if 0 <= file_idx < len(chinese_files):
                        self.translate_single_file(chinese_files[file_idx])
                    else:
                        print(f"❌ 無效的編號，請輸入 1-{len(chinese_files)}")
                        
                except ValueError:
                    print("❌ 請輸入有效的數字或 'done'")
                except KeyboardInterrupt:
                    print("\n👋 翻譯中斷")
                    break
        else:
            print("❌ 無效的選擇")
            return
        
        # 輸出統計結果
        print(f"\n🎉 翻譯處理完成！")
        print(f"📊 處理統計：")
        print(f"   ✅ 已翻譯文件數: {self.translated_count}")
        print(f"   ⏭️  跳過文件數: {self.skipped_count}")
        print(f"   ❌ 錯誤文件數: {self.error_count}")

def main():
    project_root = "/Users/yikaikao/git/genai-demo"
    translator = QTranslator(project_root)
    translator.run_interactive_translation()

if __name__ == "__main__":
    main()
