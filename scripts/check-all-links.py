#!/usr/bin/env python3
"""
檢查專案中所有 Markdown 文件的連結完整性
"""

import os
import re
import sys
from pathlib import Path
from urllib.parse import urlparse

class LinkChecker:
    def __init__(self, project_root):
        self.project_root = Path(project_root)
        self.broken_links = []
        self.valid_links = []
        self.external_links = []
        
    def is_external_link(self, link):
        """檢查是否為外部連結"""
        return link.startswith(('http://', 'https://', 'mailto:', 'ftp://'))
    
    def is_anchor_link(self, link):
        """檢查是否為錨點連結"""
        return link.startswith('#')
    
    def resolve_relative_path(self, base_file, link):
        """解析相對路徑"""
        base_dir = Path(base_file).parent
        
        # 移除錨點部分
        if '#' in link:
            link = link.split('#')[0]
        
        if not link:  # 純錨點連結
            return base_file
        
        # 解析相對路徑
        resolved = (base_dir / link).resolve()
        
        # 確保路徑在專案根目錄內
        try:
            resolved.relative_to(self.project_root)
            return resolved
        except ValueError:
            return None
    
    def check_file_links(self, file_path):
        """檢查單個文件中的所有連結"""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
        except Exception as e:
            print(f"❌ 無法讀取文件 {file_path}: {e}")
            return
        
        # 使用正則表達式找出所有 Markdown 連結
        link_pattern = r'\[([^\]]*)\]\(([^)]+)\)'
        matches = re.findall(link_pattern, content)
        
        for link_text, link_url in matches:
            # 跳過外部連結
            if self.is_external_link(link_url):
                self.external_links.append({
                    'file': str(file_path),
                    'text': link_text,
                    'url': link_url
                })
                continue
            
            # 跳過純錨點連結（需要更複雜的檢查）
            if self.is_anchor_link(link_url):
                continue
            
            # 檢查內部連結
            resolved_path = self.resolve_relative_path(file_path, link_url)
            
            if resolved_path is None:
                self.broken_links.append({
                    'file': str(file_path),
                    'text': link_text,
                    'url': link_url,
                    'reason': '路徑超出專案範圍'
                })
                continue
            
            if not resolved_path.exists():
                self.broken_links.append({
                    'file': str(file_path),
                    'text': link_text,
                    'url': link_url,
                    'resolved': str(resolved_path),
                    'reason': '文件不存在'
                })
            else:
                self.valid_links.append({
                    'file': str(file_path),
                    'text': link_text,
                    'url': link_url,
                    'resolved': str(resolved_path)
                })
    
    def find_markdown_files(self):
        """找出所有 Markdown 文件"""
        markdown_files = []
        
        for root, dirs, files in os.walk(self.project_root):
            # 跳過一些不需要檢查的目錄
            dirs[:] = [d for d in dirs if not d.startswith('.') and d not in ['node_modules', 'build', 'target']]
            
            for file in files:
                if file.endswith('.md'):
                    markdown_files.append(Path(root) / file)
        
        return markdown_files
    
    def run_check(self):
        """執行連結檢查"""
        print("🔍 開始檢查所有 Markdown 文件的連結...")
        print("=" * 60)
        
        markdown_files = self.find_markdown_files()
        print(f"📄 找到 {len(markdown_files)} 個 Markdown 文件")
        print()
        
        for file_path in markdown_files:
            print(f"檢查: {file_path.relative_to(self.project_root)}")
            self.check_file_links(file_path)
        
        print()
        print("=" * 60)
        print("📊 檢查結果總結:")
        print(f"✅ 有效連結: {len(self.valid_links)}")
        print(f"🌐 外部連結: {len(self.external_links)}")
        print(f"❌ 損壞連結: {len(self.broken_links)}")
        
        if self.broken_links:
            print("\n🔴 損壞的連結:")
            for link in self.broken_links:
                print(f"\n📄 文件: {link['file']}")
                print(f"🔗 連結文字: {link['text']}")
                print(f"🎯 連結 URL: {link['url']}")
                if 'resolved' in link:
                    print(f"📍 解析路徑: {link['resolved']}")
                print(f"❌ 原因: {link['reason']}")
        
        if len(self.broken_links) == 0:
            print("\n🎉 所有內部連結都正常！")
        
        return len(self.broken_links) == 0

def main():
    """主函數"""
    script_dir = Path(__file__).parent
    project_root = script_dir.parent
    
    print(f"📂 專案根目錄: {project_root}")
    
    checker = LinkChecker(project_root)
    success = checker.run_check()
    
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main()