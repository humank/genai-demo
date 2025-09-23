#!/usr/bin/env python3
"""
簡化版連結檢查器 - 只報告損壞的連結
"""

import os
import re
from pathlib import Path

def check_links():
    project_root = Path.cwd()
    broken_links = []
    
    # 找出所有 Markdown 文件
    markdown_files = []
    for root, dirs, files in os.walk(project_root):
        dirs[:] = [d for d in dirs if not d.startswith('.') and d not in ['node_modules', 'build', 'target']]
        for file in files:
            if file.endswith('.md'):
                markdown_files.append(Path(root) / file)
    
    print(f"🔍 檢查 {len(markdown_files)} 個 Markdown 文件...")
    
    for file_path in markdown_files:
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
        except:
            continue
        
        # 找出所有內部連結
        link_pattern = r'\[([^\]]*)\]\(([^)]+)\)'
        matches = re.findall(link_pattern, content)
        
        for link_text, link_url in matches:
            # 跳過外部連結和錨點連結
            if (link_url.startswith(('http://', 'https://', 'mailto:', '#')) or 
                'localhost:' in link_url):
                continue
            
            # 解析相對路徑
            base_dir = file_path.parent
            if '#' in link_url:
                link_url = link_url.split('#')[0]
            
            if not link_url:  # 純錨點連結
                continue
            
            resolved = (base_dir / link_url).resolve()
            
            # 檢查文件是否存在
            if not resolved.exists():
                broken_links.append({
                    'file': str(file_path.relative_to(project_root)),
                    'text': link_text,
                    'url': link_url,
                    'resolved': str(resolved.relative_to(project_root))
                })
    
    print(f"\n📊 檢查結果:")
    print(f"❌ 損壞連結: {len(broken_links)}")
    
    if broken_links:
        print("\n🔴 損壞的連結:")
        for link in broken_links:
            print(f"\n📄 {link['file']}")
            print(f"🔗 [{link['text']}]({link['url']})")
            print(f"📍 解析為: {link['resolved']}")
    else:
        print("\n🎉 所有內部連結都正常！")
    
    return len(broken_links)

if __name__ == "__main__":
    broken_count = check_links()
    exit(broken_count)