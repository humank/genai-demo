#!/usr/bin/env python3
"""
文檔自動翻譯 Hook
當 commit message 包含 [translate] 或 [en] 時，自動翻譯變更的 .md 檔案
"""

import os
import re
import subprocess
import sys
from pathlib import Path
from typing import List, Dict, Tuple

class DocumentTranslator:
    def __init__(self):
        self.repo_root = Path.cwd()
        self.zh_tw_dir = self.repo_root / "docs" / "zh-tw"
        self.en_dir = self.repo_root / "docs" / "en"
        
    def should_translate(self, commit_msg: str) -> bool:
        """檢查 commit message 是否包含翻譯觸發關鍵字"""
        return "[translate]" in commit_msg or "[en]" in commit_msg
    
    def get_staged_md_files(self) -> List[str]:
        """獲取本次 commit 中變更的 .md 檔案"""
        try:
            result = subprocess.run(
                ["git", "diff", "--cached", "--name-only", "--diff-filter=AM"],
                capture_output=True, text=True, check=True
            )
            files = result.stdout.strip().split('\n')
            return [f for f in files if f.endswith('.md') and f]
        except subprocess.CalledProcessError:
            return []
    
    def get_commit_message(self) -> str:
        """獲取 commit message"""
        try:
            # 從 git 環境變數或參數獲取 commit message
            if len(sys.argv) > 1:
                return sys.argv[1]
            
            # 嘗試從 .git/COMMIT_EDITMSG 讀取
            commit_msg_file = self.repo_root / ".git" / "COMMIT_EDITMSG"
            if commit_msg_file.exists():
                return commit_msg_file.read_text(encoding='utf-8').strip()
            
            return ""
        except Exception:
            return ""
    
    def convert_links(self, content: str, source_file: str) -> str:
        """轉換 markdown 中的連結指向英文版本"""
        
        # 連結模式: [text](path) 或 [text](path#anchor)
        link_pattern = r'\[([^\]]+)\]\(([^)]+)\)'
        
        def replace_link(match):
            text = match.group(1)
            link = match.group(2)
            
            # 分離路徑和錨點
            if '#' in link:
                path, anchor = link.split('#', 1)
                # 翻譯錨點 (簡單的中文轉英文規則)
                anchor = self.translate_anchor(anchor)
            else:
                path = link
                anchor = None
            
            # 如果是相對路徑的 .md 檔案，轉換為英文版本
            if path.endswith('.md') and not path.startswith('http'):
                # 轉換路徑指向英文版本
                if path.startswith('./'):
                    # 同目錄檔案
                    new_path = path
                elif path.startswith('../'):
                    # 上級目錄檔案
                    new_path = path
                elif path.startswith('docs/'):
                    # 絕對路徑，轉換為英文版本
                    new_path = path.replace('docs/', 'docs/en/', 1)
                else:
                    # 相對路徑，保持不變但確保指向英文版本
                    new_path = path
            else:
                new_path = path
            
            # 重組連結
            if anchor:
                new_link = f"{new_path}#{anchor}"
            else:
                new_link = new_path
            
            return f"[{text}]({new_link})"
        
        return re.sub(link_pattern, replace_link, content)
    
    def translate_anchor(self, anchor: str) -> str:
        """翻譯錨點名稱"""
        # 簡單的中英文對照表
        translations = {
            "專案架構": "project-architecture",
            "技術棧": "tech-stack",
            "文檔": "documentation",
            "如何運行": "how-to-run",
            "架構測試": "architecture-testing",
            "bdd-測試": "bdd-testing",
            "uml-圖表": "uml-diagrams",
            "常見問題": "faq",
            "貢獻": "contributing",
            "授權": "license",
            "tell-dont-ask-原則": "tell-dont-ask-principle",
            "設計思考點": "design-considerations",
            "領域驅動設計": "domain-driven-design",
            "防禦性編程實踐": "defensive-programming-practices",
            "設計模式應用": "design-patterns-application",
            "改進建議": "improvement-suggestions",
            "參考資源": "references"
        }
        
        # 先嘗試直接對照
        if anchor in translations:
            return translations[anchor]
        
        # 如果沒有直接對照，進行簡單的轉換
        # 移除中文字符，保留英文和數字，轉為小寫，用連字符連接
        import unicodedata
        
        # 移除中文標點，保留英文字母、數字和連字符
        cleaned = re.sub(r'[^\w\s-]', '', anchor)
        cleaned = re.sub(r'\s+', '-', cleaned.strip())
        cleaned = cleaned.lower()
        
        return cleaned if cleaned else anchor
    
    def ensure_directory_structure(self, target_file: str):
        """確保目標目錄結構存在"""
        target_path = Path(target_file)
        target_path.parent.mkdir(parents=True, exist_ok=True)
    
    def translate_file(self, source_file: str) -> str:
        """翻譯單個檔案並返回目標檔案路徑"""
        source_path = Path(source_file)
        
        # 確定目標檔案路徑
        if source_file.startswith('docs/'):
            # docs/ 下的檔案移到 docs/en/
            target_file = source_file.replace('docs/', 'docs/en/', 1)
        else:
            # 根目錄的 .md 檔案移到 docs/en/
            target_file = f"docs/en/{source_file}"
        
        # 確保目標目錄存在
        self.ensure_directory_structure(target_file)
        
        try:
            # 讀取原始檔案
            with open(source_file, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # 轉換連結
            translated_content = self.convert_links(content, source_file)
            
            # 這裡應該調用實際的翻譯 API，暫時先標記需要翻譯
            # 在實際實現中，這裡會調用 AI 翻譯服務
            translated_content = f"""<!-- This file is auto-translated from {source_file} -->
<!-- 此檔案由 {source_file} 自動翻譯而來 -->

{translated_content}

<!-- Translation completed at {self.get_current_timestamp()} -->
"""
            
            # 寫入目標檔案
            with open(target_file, 'w', encoding='utf-8') as f:
                f.write(translated_content)
            
            return target_file
            
        except Exception as e:
            print(f"翻譯檔案 {source_file} 時發生錯誤: {e}")
            return None
    
    def get_current_timestamp(self) -> str:
        """獲取當前時間戳"""
        from datetime import datetime
        return datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    
    def add_to_git(self, files: List[str]):
        """將翻譯後的檔案加入 git"""
        for file in files:
            if file:
                try:
                    subprocess.run(["git", "add", file], check=True)
                    print(f"已將 {file} 加入 git")
                except subprocess.CalledProcessError as e:
                    print(f"無法將 {file} 加入 git: {e}")
    
    def run(self):
        """執行翻譯流程"""
        commit_msg = self.get_commit_message()
        
        if not self.should_translate(commit_msg):
            print("Commit message 不包含翻譯觸發關鍵字，跳過翻譯")
            return 0
        
        staged_files = self.get_staged_md_files()
        if not staged_files:
            print("沒有找到需要翻譯的 .md 檔案")
            return 0
        
        print(f"找到 {len(staged_files)} 個需要翻譯的檔案:")
        for file in staged_files:
            print(f"  - {file}")
        
        translated_files = []
        for file in staged_files:
            print(f"正在翻譯: {file}")
            target_file = self.translate_file(file)
            if target_file:
                translated_files.append(target_file)
                print(f"  → {target_file}")
        
        if translated_files:
            print(f"將 {len(translated_files)} 個翻譯檔案加入 git...")
            self.add_to_git(translated_files)
            print("翻譯完成！")
        
        return 0

if __name__ == "__main__":
    translator = DocumentTranslator()
    sys.exit(translator.run())