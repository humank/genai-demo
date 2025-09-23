#!/usr/bin/env python3
"""
圖表文件名標準化腳本
解決空格、大小寫和特殊字符問題
"""

import os
import re
import shutil
from pathlib import Path
from urllib.parse import quote, unquote

class DiagramFilenameFixer:
    def __init__(self):
        self.project_root = Path.cwd()
        self.renames_applied = []
        
    def normalize_filename(self, filename):
        """標準化文件名"""
        # 移除擴展名
        name_without_ext = Path(filename).stem
        extension = Path(filename).suffix
        
        # 標準化規則
        normalized = name_without_ext
        
        # 1. 轉換為小寫
        normalized = normalized.lower()
        
        # 2. 將空格和特殊字符轉換為連字符
        normalized = re.sub(r'[^a-z0-9]+', '-', normalized)
        
        # 3. 移除開頭和結尾的連字符
        normalized = normalized.strip('-')
        
        # 4. 將多個連續的連字符合併為一個
        normalized = re.sub(r'-+', '-', normalized)
        
        return normalized + extension
    
    def rename_diagram_files(self):
        """重命名圖表文件"""
        generated_dir = self.project_root / 'docs/diagrams/generated'
        
        if not generated_dir.exists():
            print("❌ 圖表生成目錄不存在")
            return
        
        print("🔄 開始標準化圖表文件名...")
        
        # 收集所有需要重命名的文件
        rename_map = {}
        
        for png_file in generated_dir.rglob('*.png'):
            original_name = png_file.name
            normalized_name = self.normalize_filename(original_name)
            
            if original_name != normalized_name:
                new_path = png_file.parent / normalized_name
                rename_map[str(png_file)] = str(new_path)
        
        # 執行重命名
        for old_path, new_path in rename_map.items():
            try:
                shutil.move(old_path, new_path)
                self.renames_applied.append({
                    'old': old_path,
                    'new': new_path
                })
                print(f"✅ 重命名: {Path(old_path).name} → {Path(new_path).name}")
            except Exception as e:
                print(f"❌ 重命名失敗 {old_path}: {e}")
        
        return rename_map
    
    def update_diagram_links(self, rename_map):
        """更新文檔中的圖表連結"""
        print("\\n📝 更新文檔中的圖表連結...")
        
        # 建立文件名映射
        filename_map = {}
        for old_path, new_path in rename_map.items():
            old_name = Path(old_path).name
            new_name = Path(new_path).name
            filename_map[old_name] = new_name
        
        # 掃描所有 Markdown 文件
        markdown_files = list(self.project_root.rglob('*.md'))
        
        for md_file in markdown_files:
            try:
                with open(md_file, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                original_content = content
                
                # 更新圖表連結
                for old_name, new_name in filename_map.items():
                    # 處理各種可能的連結格式
                    patterns = [
                        old_name,
                        quote(old_name),
                        old_name.replace(' ', '%20'),
                        old_name.replace(' ', '-'),
                        old_name.replace('_', '-')
                    ]
                    
                    for pattern in patterns:
                        if pattern in content:
                            content = content.replace(pattern, new_name)
                
                # 如果內容有變化，寫回文件
                if content != original_content:
                    with open(md_file, 'w', encoding='utf-8') as f:
                        f.write(content)
                    print(f"✅ 更新連結: {md_file.relative_to(self.project_root)}")
                    
            except Exception as e:
                print(f"❌ 更新文件 {md_file} 時出錯: {e}")
    
    def run_fix(self):
        """執行修復"""
        print("🎯 圖表文件名標準化工具")
        print("=" * 50)
        
        # 1. 重命名圖表文件
        rename_map = self.rename_diagram_files()
        
        if not rename_map:
            print("✅ 所有圖表文件名已經標準化")
            return
        
        # 2. 更新文檔連結
        self.update_diagram_links(rename_map)
        
        # 3. 生成報告
        self.generate_report()
        
        print("\\n🎉 圖表文件名標準化完成！")
    
    def generate_report(self):
        """生成報告"""
        if not self.renames_applied:
            return
        
        report_content = f"""# 圖表文件名標準化報告

## 📊 重命名統計

總共重命名了 {len(self.renames_applied)} 個圖表文件

## 🔄 重命名詳情

| 原文件名 | 新文件名 |
|---------|---------|
"""
        
        for rename in self.renames_applied:
            old_name = Path(rename['old']).name
            new_name = Path(rename['new']).name
            report_content += f"| {old_name} | {new_name} |\\n"
        
        report_content += f"""

## 📋 標準化規則

1. **小寫轉換**: 所有字母轉為小寫
2. **特殊字符處理**: 空格和特殊字符轉為連字符 (-)
3. **連字符優化**: 移除多餘的連字符
4. **一致性**: 確保所有圖表文件名格式一致

## ✅ 後續效果

- 圖表連結更加穩定
- 避免 URL 編碼問題
- 提升文檔可維護性
- 減少連結錯誤

---

**生成時間**: {self._get_current_time()}  
**工具版本**: fix-diagram-filenames.py v1.0  
**狀態**: 標準化完成 ✅
"""
        
        # 保存報告
        report_path = self.project_root / 'reports-summaries/task-execution/diagram-filename-fix-report.md'
        report_path.parent.mkdir(parents=True, exist_ok=True)
        
        with open(report_path, 'w', encoding='utf-8') as f:
            f.write(report_content)
        
        print(f"📋 標準化報告已生成: {report_path}")
    
    def _get_current_time(self):
        """獲取當前時間"""
        from datetime import datetime
        return datetime.now().strftime("%Y-%m-%d %H:%M:%S")

def main():
    fixer = DiagramFilenameFixer()
    fixer.run_fix()

if __name__ == "__main__":
    main()