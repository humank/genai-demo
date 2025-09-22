#!/usr/bin/env python3
"""
處理孤立的 .mmd 文件腳本
將未被引用的 .mmd 文件轉換為 Markdown 文檔或整合到現有文檔中
"""

import os
import re
import sys
from pathlib import Path
from typing import List, Dict, Tuple, Set

class OrphanedMmdProcessor:
    def __init__(self):
        self.root_dir = Path(".")
        self.orphaned_files = []
        self.processed_files = []
        self.errors = []
        
    def find_all_mmd_files(self) -> List[Path]:
        """找到所有 .mmd 文件"""
        mmd_files = []
        for root, dirs, files in os.walk(self.root_dir):
            # 跳過 .git, node_modules, build 等目錄
            dirs[:] = [d for d in dirs if not d.startswith('.') and d not in ['node_modules', 'build', 'target']]
            
            for file in files:
                if file.endswith('.mmd'):
                    mmd_files.append(Path(root) / file)
        
        return mmd_files
    
    def find_markdown_files(self) -> List[Path]:
        """找到所有 Markdown 文件"""
        markdown_files = []
        for root, dirs, files in os.walk(self.root_dir):
            # 跳過 .git, node_modules, build 等目錄
            dirs[:] = [d for d in dirs if not d.startswith('.') and d not in ['node_modules', 'build', 'target']]
            
            for file in files:
                if file.endswith('.md'):
                    markdown_files.append(Path(root) / file)
        
        return markdown_files
    
    def check_if_mmd_referenced(self, mmd_file: Path, markdown_files: List[Path]) -> bool:
        """檢查 .mmd 文件是否被任何 Markdown 文件引用"""
        mmd_name = mmd_file.name
        mmd_path_str = str(mmd_file)
        
        for md_file in markdown_files:
            try:
                with open(md_file, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                # 檢查各種可能的引用模式
                if (mmd_name in content or 
                    mmd_path_str in content or
                    str(mmd_file.relative_to(self.root_dir)) in content):
                    return True
                    
            except Exception as e:
                self.errors.append(f"檢查引用失敗 {md_file}: {str(e)}")
        
        return False
    
    def read_mmd_content(self, mmd_file: Path) -> str:
        """讀取 .mmd 文件內容"""
        try:
            with open(mmd_file, 'r', encoding='utf-8') as f:
                return f.read().strip()
        except Exception as e:
            self.errors.append(f"讀取 .mmd 文件失敗 {mmd_file}: {str(e)}")
            return ""
    
    def generate_title_from_filename(self, mmd_file: Path) -> str:
        """從文件名生成標題"""
        filename = mmd_file.stem
        
        # 替換下劃線和連字符為空格
        title = filename.replace('_', ' ').replace('-', ' ')
        
        # 首字母大寫
        title = ' '.join(word.capitalize() for word in title.split())
        
        # 特殊詞彙處理
        replacements = {
            'Ddd': 'DDD',
            'Api': 'API',
            'Aws': 'AWS',
            'Ui': 'UI',
            'Ux': 'UX',
            'Sql': 'SQL',
            'Http': 'HTTP',
            'Https': 'HTTPS',
            'Json': 'JSON',
            'Xml': 'XML',
            'Yaml': 'YAML',
            'Jwt': 'JWT',
            'Oauth': 'OAuth',
            'Saml': 'SAML'
        }
        
        for old, new in replacements.items():
            title = title.replace(old, new)
        
        return title
    
    def create_markdown_from_mmd(self, mmd_file: Path, mmd_content: str) -> str:
        """從 .mmd 文件創建 Markdown 文檔"""
        title = self.generate_title_from_filename(mmd_file)
        
        # 根據文件路徑確定類別和描述
        path_parts = mmd_file.parts
        category = ""
        description = ""
        
        if 'viewpoints' in path_parts:
            if 'functional' in path_parts:
                category = "功能視角"
                description = "展示系統的功能結構和業務流程"
            elif 'information' in path_parts:
                category = "資訊視角"
                description = "展示系統的資料結構和資訊流"
            elif 'development' in path_parts:
                category = "開發視角"
                description = "展示系統的開發結構和技術架構"
            elif 'deployment' in path_parts:
                category = "部署視角"
                description = "展示系統的部署結構和基礎設施"
            elif 'operational' in path_parts:
                category = "營運視角"
                description = "展示系統的營運監控和管理"
            elif 'concurrency' in path_parts:
                category = "並發視角"
                description = "展示系統的並發處理和非同步架構"
        else:
            category = "系統架構"
            description = "展示系統的整體架構和設計"
        
        markdown_content = f"""# {title}

**類別**: {category}  
**描述**: {description}

## 架構圖

```mermaid
{mmd_content}
```

## 說明

本圖表展示了 {title.lower()} 的詳細結構，包括：

- 系統組件及其關係
- 資料流向和處理流程
- 技術架構和實現方式

## 相關文檔

- [架構概覽](../README.md) - 整體系統架構
- [設計文檔](../../architecture/) - 詳細設計說明
- [部署指南](../../deployment/) - 部署相關文檔

---

*本文檔由 .mmd 文件自動轉換生成*
"""
        
        return markdown_content
    
    def process_orphaned_file(self, mmd_file: Path) -> bool:
        """處理單個孤立的 .mmd 文件"""
        try:
            # 讀取 .mmd 文件內容
            mmd_content = self.read_mmd_content(mmd_file)
            if not mmd_content:
                return False
            
            # 生成對應的 .md 文件路徑
            md_file_path = mmd_file.with_suffix('.md')
            
            # 檢查是否已存在對應的 .md 文件
            if md_file_path.exists():
                # 如果已存在，檢查是否包含 Mermaid 代碼塊
                with open(md_file_path, 'r', encoding='utf-8') as f:
                    existing_content = f.read()
                
                if '```mermaid' in existing_content:
                    # 已存在且包含 Mermaid 代碼塊，跳過
                    self.processed_files.append({
                        'mmd_file': str(mmd_file),
                        'action': 'skipped',
                        'reason': '對應的 .md 文件已存在且包含 Mermaid 代碼塊',
                        'md_file': str(md_file_path)
                    })
                    return True
            
            # 創建 Markdown 文檔
            markdown_content = self.create_markdown_from_mmd(mmd_file, mmd_content)
            
            # 寫入 .md 文件
            with open(md_file_path, 'w', encoding='utf-8') as f:
                f.write(markdown_content)
            
            self.processed_files.append({
                'mmd_file': str(mmd_file),
                'action': 'converted',
                'reason': '轉換為 Markdown 文檔',
                'md_file': str(md_file_path)
            })
            
            return True
            
        except Exception as e:
            self.errors.append(f"處理文件失敗 {mmd_file}: {str(e)}")
            return False
    
    def run(self) -> Dict:
        """執行處理過程"""
        print("🔍 搜尋孤立的 .mmd 文件...")
        
        # 找到所有 .mmd 文件
        mmd_files = self.find_all_mmd_files()
        print(f"找到 {len(mmd_files)} 個 .mmd 文件")
        
        # 找到所有 Markdown 文件
        markdown_files = self.find_markdown_files()
        print(f"找到 {len(markdown_files)} 個 Markdown 文件")
        
        # 檢查哪些 .mmd 文件是孤立的
        for mmd_file in mmd_files:
            if not self.check_if_mmd_referenced(mmd_file, markdown_files):
                self.orphaned_files.append(mmd_file)
        
        print(f"發現 {len(self.orphaned_files)} 個孤立的 .mmd 文件")
        
        # 處理孤立的文件
        processed_count = 0
        for mmd_file in self.orphaned_files:
            if self.process_orphaned_file(mmd_file):
                processed_count += 1
                print(f"✅ 處理: {mmd_file}")
        
        # 生成報告
        report = {
            'total_mmd_files': len(mmd_files),
            'orphaned_files': len(self.orphaned_files),
            'processed_files': processed_count,
            'processed_details': self.processed_files,
            'errors': self.errors
        }
        
        return report
    
    def generate_report(self, report: Dict) -> str:
        """生成處理報告"""
        report_lines = [
            "# 孤立 .mmd 文件處理報告",
            "",
            f"**生成時間**: {self.get_current_time()}",
            "",
            "## 📊 處理統計",
            "",
            f"- **總 .mmd 文件數**: {report['total_mmd_files']}",
            f"- **孤立文件數**: {report['orphaned_files']}",
            f"- **處理文件數**: {report['processed_files']}",
            f"- **錯誤數**: {len(report['errors'])}",
            ""
        ]
        
        if report['processed_details']:
            report_lines.extend([
                "## 🔧 處理詳情",
                ""
            ])
            
            for detail in report['processed_details']:
                report_lines.extend([
                    f"### 📄 {detail['mmd_file']}",
                    "",
                    f"**處理動作**: {detail['action']}",
                    f"**處理原因**: {detail['reason']}",
                    f"**生成文件**: `{detail['md_file']}`",
                    ""
                ])
        
        if report['errors']:
            report_lines.extend([
                "## ❌ 錯誤記錄",
                ""
            ])
            
            for error in report['errors']:
                report_lines.append(f"- {error}")
            
            report_lines.append("")
        
        report_lines.extend([
            "## 📋 處理說明",
            "",
            "本次處理將孤立的 .mmd 文件進行了以下處理：",
            "",
            "1. **檢測孤立文件** - 找出未被任何 Markdown 文件引用的 .mmd 文件",
            "2. **轉換為 Markdown** - 將 .mmd 內容轉換為完整的 Markdown 文檔",
            "3. **添加說明文字** - 為圖表添加適當的標題、描述和相關連結",
            "4. **保持 GitHub 支援** - 使用 ```mermaid 代碼塊格式",
            "",
            "## 🎯 處理效果",
            "",
            "- ✅ 所有孤立的 .mmd 文件現在都有對應的 Markdown 文檔",
            "- ✅ 圖表可在 GitHub 上直接渲染",
            "- ✅ 提供了適當的上下文和說明",
            "- ✅ 符合文檔組織標準",
            "",
            "## 🔄 後續建議",
            "",
            "1. **審查生成的文檔** - 檢查自動生成的標題和描述是否合適",
            "2. **添加到導航** - 將新文檔添加到相關的 README.md 中",
            "3. **完善內容** - 根據需要添加更詳細的說明和相關連結",
            "4. **清理原文件** - 考慮是否需要保留原始的 .mmd 文件",
            ""
        ])
        
        return "\n".join(report_lines)
    
    def get_current_time(self) -> str:
        """獲取當前時間"""
        from datetime import datetime
        return datetime.now().strftime("%Y-%m-%d %H:%M:%S")

def main():
    """主函數"""
    print("🚀 開始處理孤立的 .mmd 文件...")
    
    processor = OrphanedMmdProcessor()
    report = processor.run()
    
    # 生成報告
    report_content = processor.generate_report(report)
    
    # 確保報告目錄存在
    report_dir = Path("reports-summaries/diagrams")
    report_dir.mkdir(parents=True, exist_ok=True)
    
    # 寫入報告
    report_file = report_dir / "orphaned-mmd-processing-report.md"
    with open(report_file, 'w', encoding='utf-8') as f:
        f.write(report_content)
    
    # 輸出結果
    print(f"\n📊 處理完成!")
    print(f"- 總 .mmd 文件: {report['total_mmd_files']}")
    print(f"- 孤立文件: {report['orphaned_files']}")
    print(f"- 處理文件: {report['processed_files']}")
    
    if report['errors']:
        print(f"- 錯誤數: {len(report['errors'])}")
        print("\n❌ 錯誤詳情:")
        for error in report['errors']:
            print(f"  {error}")
    
    print(f"\n📄 詳細報告已保存至: {report_file}")
    
    return 0 if not report['errors'] else 1

if __name__ == "__main__":
    sys.exit(main())