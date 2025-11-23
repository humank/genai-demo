#!/usr/bin/env python3
"""
統計專案中 Markdown 文件的分布情況
"""

import os
from pathlib import Path
from collections import defaultdict

class MarkdownFilesStatistics:
    def __init__(self):
        self.project_root = Path.cwd()
        self.file_stats = defaultdict(list)
        self.total_files = 0
        
    def analyze_files(self):
        """分析所有 Markdown 文件"""
        print("📊 分析專案中的 Markdown 文件分布...")
        print("=" * 60)
        
        # 排除的目錄
        exclude_dirs = {'.git', 'node_modules', 'build', 'target'}
        
        for root, dirs, files in os.walk(self.project_root):
            # 過濾目錄
            dirs[:] = [d for d in dirs if d not in exclude_dirs]
            
            for file in files:
                if file.endswith('.md'):
                    file_path = Path(root) / file
                    relative_path = file_path.relative_to(self.project_root)
                    
                    # 按頂級目錄分類
                    top_dir = str(relative_path).split('/')[0] if '/' in str(relative_path) else 'root'
                    self.file_stats[top_dir].append(str(relative_path))
                    self.total_files += 1
        
        # 排序統計結果
        for category in self.file_stats:
            self.file_stats[category].sort()
    
    def print_statistics(self):
        """輸出統計結果"""
        print(f"📄 總計 Markdown 文件: {self.total_files} 個")
        print()
        
        # 按文件數量排序
        sorted_categories = sorted(self.file_stats.items(), key=lambda x: len(x[1]), reverse=True)
        
        for category, files in sorted_categories:
            print(f"📁 {category}/")
            print(f"   文件數: {len(files)} 個")
            
            # 如果是docs目錄，進一步細分
            if category == 'docs':
                docs_subcategories = defaultdict(list)
                for file in files:
                    parts = file.split('/')
                    if len(parts) > 1:
                        subdir = parts[1]
                        docs_subcategories[subdir].append(file)
                    else:
                        docs_subcategories['root'].append(file)
                
                print("   子目錄分布:")
                for subdir, subfiles in sorted(docs_subcategories.items(), key=lambda x: len(x[1]), reverse=True):
                    print(f"     📂 docs/{subdir}/: {len(subfiles)} 個文件")
            
            # 顯示前5個文件作為例子
            print("   例子:")
            for file in files[:5]:
                print(f"     - {file}")
            if len(files) > 5:
                print(f"     ... 還有 {len(files) - 5} 個文件")
            print()
        
        # 特別統計
        docs_files = len(self.file_stats.get('docs', []))
        reports_files = len(self.file_stats.get('reports-summaries', []))
        
        print("🎯 重點統計:")
        print(f"   📚 docs/ 目錄: {docs_files} 個文件 ({docs_files/self.total_files*100:.1f}%)")
        print(f"   📋 reports-summaries/ 目錄: {reports_files} 個文件 ({reports_files/self.total_files*100:.1f}%)")
        print(f"   🔧 其他目錄: {self.total_files - docs_files - reports_files} 個文件 ({(self.total_files - docs_files - reports_files)/self.total_files*100:.1f}%)")

def main():
    """主函數"""
    stats = MarkdownFilesStatistics()
    stats.analyze_files()
    stats.print_statistics()

if __name__ == "__main__":
    main()