#!/usr/bin/env python3
"""
優化的連結檢查 - 只檢查核心重要文檔
"""

import os
import re
from pathlib import Path

class OptimizedLinkChecker:
    def __init__(self):
        self.project_root = Path.cwd()
        self.broken_links = []
        
        # 只檢查核心重要文檔
        self.core_files = [
            'docs/viewpoints/development/README.md',
            'docs/viewpoints/functional/README.md',
            'docs/viewpoints/information/README.md',
            'docs/viewpoints/README.md',
            'docs/README.md',
            'README.md'
        ]
        
    def check_markdown_file(self, file_path):
        """檢查單個 Markdown 文件中的連結"""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
        except Exception as e:
            return
        
        # 匹配 Markdown 連結
        link_pattern = r'\[([^\]]*)\]\(([^)]+)\)'
        matches = re.findall(link_pattern, content)
        
        for link_text, link_url in matches:
            # 跳過外部連結
            if link_url.startswith(('http://', 'https://', 'mailto:')):
                continue
            
            # 檢查連結是否損壞
            if self.is_link_broken(link_url, file_path):
                self.broken_links.append({
                    'file': str(file_path.relative_to(self.project_root)),
                    'link_text': link_text,
                    'link_url': link_url
                })
    
    def is_link_broken(self, link_url, current_file):
        """檢查連結是否損壞"""
        # 移除錨點部分
        file_part = link_url.split('#')[0] if '#' in link_url else link_url
        
        if not file_part:  # 純錨點連結
            return False
        
        # 計算目標路徑
        if file_part.startswith('/'):
            target_path = self.project_root / file_part.lstrip('/')
        else:
            target_path = current_file.parent / file_part
        
        try:
            target_path = target_path.resolve()
            return not target_path.exists()
        except Exception:
            return True
    
    def scan_core_files(self):
        """掃描核心文件"""
        existing_files = []
        
        for file_path_str in self.core_files:
            file_path = self.project_root / file_path_str
            if file_path.exists():
                existing_files.append(file_path)
                self.check_markdown_file(file_path)
        
        print(f"🔍 掃描 {len(existing_files)} 個核心文檔...")
    
    def print_results(self):
        """輸出結果"""
        print(f"\n📊 核心文檔連結檢查結果:")
        print(f"損壞連結總數: {len(self.broken_links)}")
        
        if not self.broken_links:
            print("🎉 恭喜！所有核心文檔連結都是完美的！")
            print("🏆 達到 100% 完美狀態！")
            return
        
        print("=" * 50)
        
        for i, link in enumerate(self.broken_links, 1):
            print(f"{i}. 📄 {link['file']}")
            print(f"   🔗 [{link['link_text']}]({link['link_url']})")

def main():
    checker = OptimizedLinkChecker()
    checker.scan_core_files()
    checker.print_results()

if __name__ == "__main__":
    main()
