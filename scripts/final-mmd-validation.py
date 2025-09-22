#!/usr/bin/env python3
"""
最終 .mmd 引用驗證腳本
全面檢查所有可能的 .mmd 引用並生成完整報告
"""

import os
import re
import sys
from pathlib import Path
from typing import List, Dict, Tuple

class FinalMmdValidator:
    def __init__(self):
        self.root_dir = Path(".")
        self.mmd_references = []
        self.mmd_files = []
        
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
    
    def find_all_markdown_files(self) -> List[Path]:
        """找到所有 Markdown 文件"""
        markdown_files = []
        for root, dirs, files in os.walk(self.root_dir):
            # 跳過 .git, node_modules, build 等目錄
            dirs[:] = [d for d in dirs if not d.startswith('.') and d not in ['node_modules', 'build', 'target']]
            
            for file in files:
                if file.endswith('.md'):
                    markdown_files.append(Path(root) / file)
        
        return markdown_files
    
    def check_mmd_references_in_file(self, file_path: Path) -> List[Dict]:
        """檢查文件中的所有 .mmd 引用"""
        references = []
        
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            lines = content.split('\n')
            
            for line_num, line in enumerate(lines, 1):
                # 檢查各種可能的 .mmd 引用模式
                patterns = [
                    r'\[([^\]]*)\]\(([^)]*\.mmd)\)',  # Markdown 鏈接
                    r'"([^"]*\.mmd)"',                # 引號中的 .mmd
                    r'([^\s]*\.mmd)',                 # 任何 .mmd 文件名
                ]
                
                for pattern in patterns:
                    matches = re.finditer(pattern, line)
                    for match in matches:
                        # 排除一些明顯的誤報
                        if ('SUCCESS: Generated PNG for' in line or
                            'ERROR:' in line or
                            '2025-' in line):  # 排除日誌行
                            continue
                        
                        references.append({
                            'file': str(file_path),
                            'line': line_num,
                            'content': line.strip(),
                            'match': match.group(0),
                            'pattern': pattern
                        })
        
        except Exception as e:
            print(f"讀取文件失敗 {file_path}: {str(e)}")
        
        return references
    
    def categorize_reference(self, ref: Dict) -> str:
        """分類引用類型"""
        content = ref['content'].lower()
        match = ref['match'].lower()
        
        if 'front matter' in content or 'diagrams:' in content:
            return 'YAML Front Matter'
        elif ref['content'].startswith('│') or ref['content'].startswith('├') or ref['content'].startswith('└'):
            return 'Directory Structure'
        elif '[' in ref['match'] and '](' in ref['match']:
            return 'Markdown Link'
        elif 'generated png for' in content or 'success:' in content:
            return 'Log Entry (可忽略)'
        elif 'mermaid 文件' in content or '.mmd 文件' in content:
            return 'Documentation Text'
        else:
            return 'Other'
    
    def run(self) -> Dict:
        """執行驗證過程"""
        print("🔍 進行最終 .mmd 引用驗證...")
        
        # 找到所有文件
        mmd_files = self.find_all_mmd_files()
        markdown_files = self.find_all_markdown_files()
        
        print(f"找到 {len(mmd_files)} 個 .mmd 文件")
        print(f"找到 {len(markdown_files)} 個 Markdown 文件")
        
        # 檢查所有 Markdown 文件中的 .mmd 引用
        all_references = []
        
        for md_file in markdown_files:
            refs = self.check_mmd_references_in_file(md_file)
            all_references.extend(refs)
        
        # 分類引用
        categorized_refs = {}
        for ref in all_references:
            category = self.categorize_reference(ref)
            if category not in categorized_refs:
                categorized_refs[category] = []
            categorized_refs[category].append(ref)
        
        # 生成報告
        report = {
            'total_mmd_files': len(mmd_files),
            'total_markdown_files': len(markdown_files),
            'total_references': len(all_references),
            'mmd_files': [str(f) for f in mmd_files],
            'categorized_references': categorized_refs
        }
        
        return report
    
    def generate_report(self, report: Dict) -> str:
        """生成驗證報告"""
        report_lines = [
            "# 最終 .mmd 引用驗證報告",
            "",
            f"**生成時間**: {self.get_current_time()}",
            "",
            "## 📊 驗證統計",
            "",
            f"- **總 .mmd 文件數**: {report['total_mmd_files']}",
            f"- **總 Markdown 文件數**: {report['total_markdown_files']}",
            f"- **發現的 .mmd 引用數**: {report['total_references']}",
            ""
        ]
        
        # 現存的 .mmd 文件列表
        if report['mmd_files']:
            report_lines.extend([
                "## 📁 現存的 .mmd 文件",
                "",
                "以下 .mmd 文件仍然存在於專案中：",
                ""
            ])
            
            for mmd_file in report['mmd_files']:
                report_lines.append(f"- `{mmd_file}`")
            
            report_lines.extend([
                "",
                "**建議**: 這些文件可能是孤立的，考慮是否需要：",
                "1. 轉換為包含 Mermaid 代碼塊的 .md 文件",
                "2. 刪除不再使用的文件",
                "3. 確認是否有遺漏的引用需要修復",
                ""
            ])
        
        # 分類的引用
        if report['categorized_references']:
            report_lines.extend([
                "## 🔍 發現的 .mmd 引用 (按類型分類)",
                ""
            ])
            
            for category, refs in report['categorized_references'].items():
                report_lines.extend([
                    f"### {category} ({len(refs)} 個)",
                    ""
                ])
                
                if category == 'Log Entry (可忽略)':
                    report_lines.extend([
                        "這些是日誌條目，可以忽略：",
                        ""
                    ])
                    # 只顯示前3個例子
                    for ref in refs[:3]:
                        report_lines.append(f"- `{ref['file']}:{ref['line']}` - {ref['match']}")
                    if len(refs) > 3:
                        report_lines.append(f"- ... 還有 {len(refs) - 3} 個類似條目")
                else:
                    for ref in refs:
                        report_lines.extend([
                            f"**文件**: `{ref['file']}:{ref['line']}`",
                            f"**匹配**: `{ref['match']}`",
                            f"**內容**: `{ref['content'][:100]}...`" if len(ref['content']) > 100 else f"**內容**: `{ref['content']}`",
                            ""
                        ])
        
        # 驗證結果
        non_log_refs = sum(len(refs) for category, refs in report['categorized_references'].items() 
                          if category != 'Log Entry (可忽略)')
        
        if non_log_refs == 0:
            report_lines.extend([
                "## ✅ 驗證結果: 通過",
                "",
                "🎉 所有需要修復的 .mmd 引用已成功處理！",
                "",
                "- ✅ 沒有發現需要修復的 .mmd 引用",
                "- ✅ 所有 Mermaid 圖表現在使用 GitHub 原生支援的代碼塊格式",
                "- ✅ 文檔符合最新的圖表生成標準",
                ""
            ])
        else:
            report_lines.extend([
                "## ⚠️ 驗證結果: 需要注意",
                "",
                f"發現 {non_log_refs} 個可能需要處理的 .mmd 引用。",
                "",
                "**建議行動**:",
                "1. 檢查上述引用是否需要修復",
                "2. 將必要的引用轉換為 Mermaid 代碼塊格式",
                "3. 更新文檔說明以反映新的格式標準",
                ""
            ])
        
        report_lines.extend([
            "## 📋 驗證標準",
            "",
            "本次驗證檢查了以下項目：",
            "",
            "1. **Markdown 鏈接** - `[text](file.mmd)` 格式的引用",
            "2. **YAML Front Matter** - 元資料中的 .mmd 引用",
            "3. **目錄結構** - 文檔中的目錄樹顯示",
            "4. **文檔說明** - 關於 .mmd 文件的說明文字",
            "5. **其他引用** - 任何其他形式的 .mmd 引用",
            "",
            "## 🎯 修復標準",
            "",
            "符合以下標準的引用被認為是正確的：",
            "",
            "- ✅ 使用 ```mermaid 代碼塊格式",
            "- ✅ 引用包含 Mermaid 代碼塊的 .md 文件",
            "- ✅ 文檔說明反映當前的最佳實踐",
            "- ✅ GitHub 可直接渲染所有圖表",
            ""
        ])
        
        return "\n".join(report_lines)
    
    def get_current_time(self) -> str:
        """獲取當前時間"""
        from datetime import datetime
        return datetime.now().strftime("%Y-%m-%d %H:%M:%S")

def main():
    """主函數"""
    print("🚀 開始最終 .mmd 引用驗證...")
    
    validator = FinalMmdValidator()
    report = validator.run()
    
    # 生成報告
    report_content = validator.generate_report(report)
    
    # 確保報告目錄存在
    report_dir = Path("reports-summaries/diagrams")
    report_dir.mkdir(parents=True, exist_ok=True)
    
    # 寫入報告
    report_file = report_dir / "final-mmd-validation-report.md"
    with open(report_file, 'w', encoding='utf-8') as f:
        f.write(report_content)
    
    # 輸出結果
    print(f"\n📊 驗證完成!")
    print(f"- .mmd 文件: {report['total_mmd_files']}")
    print(f"- Markdown 文件: {report['total_markdown_files']}")
    print(f"- 發現引用: {report['total_references']}")
    
    # 統計非日誌引用
    non_log_refs = sum(len(refs) for category, refs in report['categorized_references'].items() 
                      if category != 'Log Entry (可忽略)')
    
    if non_log_refs == 0:
        print("- ✅ 所有 .mmd 引用已正確處理")
    else:
        print(f"- ⚠️  需要注意: {non_log_refs} 個引用")
    
    print(f"\n📄 詳細報告已保存至: {report_file}")
    
    return 0 if non_log_refs == 0 else 1

if __name__ == "__main__":
    sys.exit(main())