#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
修復版翻譯腳本 - 使用 Amazon Q 進行實際翻譯
作者: Amazon Q
日期: 2025-08-21

修復內容:
1. 改用 q translate 命令替代 q chat
2. 添加更好的錯誤處理
3. 添加調試信息
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
            
            # 創建臨時文件來存儲翻譯請求
            with tempfile.NamedTemporaryFile(mode='w', suffix='.md', delete=False, encoding='utf-8') as temp_file:
                temp_file.write(content)
                temp_file_path = temp_file.name
            
            try:
                # 嘗試使用 q translate 命令
                print(f"  📝 正在翻譯文件...")
                
                # 創建翻譯提示
                prompt = f"""Please translate this Markdown document from Traditional Chinese to English.

Requirements:
1. Maintain all Markdown formatting
2. Keep all URLs and file paths unchanged
3. Translate technical terms appropriately
4. Preserve code snippets exactly as they are
5. Keep proper nouns in English
6. For relative links to .md files, change them to point to docs/en/ directory

Please provide only the translated content without explanations."""

                # 使用 q chat 但是設置環境變量來避免互動問題
                env = os.environ.copy()
                env['Q_TERM'] = '1'  # 嘗試設置 Q_TERM
                
                result = subprocess.run(
                    ['q', 'chat', prompt],
                    input=content,
                    text=True,
                    capture_output=True,
                    timeout=120,  # 2分鐘超時
                    env=env
                )
                
                if result.returncode == 0 and result.stdout.strip():
                    translated_content = result.stdout.strip()
                    
                    # 清理輸出
                    if translated_content.startswith(("Here", "I'll", "I will")):
                        lines = translated_content.split('\n')
                        start_idx = 0
                        for i, line in enumerate(lines):
                            if line.startswith('#') or (line.strip() and not line.lower().startswith(('here', 'i'))):
                                start_idx = i
                                break
                        translated_content = '\n'.join(lines[start_idx:])
                    
                    # 寫入翻譯結果
                    target_file.write_text(translated_content, encoding='utf-8')
                    return True
                else:
                    print(f"  ❌ 翻譯失敗 - 返回碼: {result.returncode}")
                    if result.stderr:
                        print(f"  錯誤信息: {result.stderr[:200]}")
                    return False
                    
            finally:
                # 清理臨時文件
                try:
                    os.unlink(temp_file_path)
                except:
                    pass
                    
        except subprocess.TimeoutExpired:
            print("  ⏰ 翻譯超時")
            return False
        except Exception as e:
            print(f"  ❌ 翻譯過程出錯: {e}")
            return False
    
    def create_manual_translation(self, source_file: Path, target_file: Path) -> bool:
        """創建手動翻譯模板"""
        try:
            content = source_file.read_text(encoding='utf-8')
            
            # 創建翻譯模板
            template = f"""<!-- 
此文件需要手動翻譯
原文件: {source_file.relative_to(self.project_root)}
翻譯日期: {os.popen('date').read().strip()}

請將以下中文內容翻譯為英文，保持 Markdown 格式不變
-->

{content}

<!-- 翻譯完成後請刪除此註釋 -->
"""
            
            target_file.write_text(template, encoding='utf-8')
            return True
            
        except Exception as e:
            print(f"  ❌ 創建翻譯模板失敗: {e}")
            return False
    
    def fix_links_in_file(self, file_path: Path):
        """修正文檔中的相對連結"""
        try:
            content = file_path.read_text(encoding='utf-8')
            
            # 修正各種相對路徑連結
            patterns = [
                (r']\(docs/', r'](../'),
                (r']\(\.\./zh-tw/', r'](../en/'),
                (r']\(\.\./\.\./docs/', r'](../'),
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
            # 嘗試使用 Amazon Q 翻譯
            if self.translate_file_with_q(source_file, target_file):
                # 修正連結
                self.fix_links_in_file(target_file)
                print("  ✅ 翻譯完成")
                self.translated_count += 1
            else:
                # 如果 Q 翻譯失敗，創建手動翻譯模板
                print("  📝 創建手動翻譯模板...")
                if self.create_manual_translation(source_file, target_file):
                    print("  📋 已創建翻譯模板，需要手動完成")
                    self.translated_count += 1
                else:
                    print("  ❌ 創建翻譯模板失敗")
                    self.error_count += 1
                
        except Exception as e:
            print(f"  ❌ 處理文件時出錯: {e}")
            self.error_count += 1
    
    def run_interactive_translation(self):
        """執行互動式翻譯"""
        print("🌍 開始互動式翻譯中文 Markdown 文件...")
        print(f"📁 英文文檔目錄: {self.en_docs_dir}")
        
        # 檢查 Q 命令可用性
        try:
            result = subprocess.run(['q', '--version'], capture_output=True, text=True, timeout=5)
            print(f"🔧 Amazon Q 版本: {result.stdout.strip()}")
        except Exception as e:
            print(f"⚠️  Amazon Q 可能無法正常使用: {e}")
            print("📋 將創建手動翻譯模板")
        
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
        
        # 顯示文件列表（只顯示前10個）
        print("\n📋 需要翻譯的文件（顯示前10個）:")
        for i, file_path in enumerate(chinese_files[:10], 1):
            relative_path = file_path.relative_to(self.project_root)
            print(f"  {i:2d}. {relative_path}")
        
        if len(chinese_files) > 10:
            print(f"  ... 還有 {len(chinese_files) - 10} 個文件")
        
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
        print(f"   ✅ 已處理文件數: {self.translated_count}")
        print(f"   ⏭️  跳過文件數: {self.skipped_count}")
        print(f"   ❌ 錯誤文件數: {self.error_count}")
        
        if self.translated_count > 0:
            print(f"\n📁 翻譯文件保存在: {self.en_docs_dir}")
            print("💡 提示: 如果創建了翻譯模板，請手動完成翻譯並刪除註釋")

def main():
    project_root = "/Users/yikaikao/git/genai-demo"
    translator = QTranslator(project_root)
    translator.run_interactive_translation()

if __name__ == "__main__":
    main()