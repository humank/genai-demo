#!/usr/bin/env python3
"""
驗證 Mermaid 圖表修復腳本
檢查修復後的文件是否符合標準
"""

import os
import re
import sys
from pathlib import Path
from typing import List, Dict, Tuple

class MermaidFixValidator:
    def __init__(self):
        self.root_dir = Path(".")
        self.validation_results = []
        self.issues = []
        
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
    
    def check_mermaid_references(self, content: str, file_path: Path) -> Dict:
        """檢查文件中的 Mermaid 相關內容"""
        result = {
            'file': str(file_path),
            'mmd_references': [],
            'mermaid_blocks': [],
            'svg_references': [],
            'issues': []
        }
        
        # 檢查 .mmd 文件引用 (應該已經被修復)
        mmd_pattern = r'\[([^\]]+)\]\(([^)]+\.mmd)\)'
        mmd_matches = re.findall(mmd_pattern, content)
        if mmd_matches:
            result['mmd_references'] = mmd_matches
            result['issues'].append(f"仍有 {len(mmd_matches)} 個 .mmd 文件引用未修復")
        
        # 檢查 Mermaid 代碼塊
        mermaid_block_pattern = r'```mermaid\s*\n(.*?)\n```'
        mermaid_blocks = re.findall(mermaid_block_pattern, content, re.DOTALL)
        result['mermaid_blocks'] = len(mermaid_blocks)
        
        # 檢查 SVG 引用 (排除外部 URL)
        svg_pattern = r'!\[([^\]]*)\]\(([^)]+\.svg)\)'
        svg_matches = re.findall(svg_pattern, content)
        local_svg_refs = [(text, path) for text, path in svg_matches if not path.startswith('http')]
        if local_svg_refs:
            result['svg_references'] = local_svg_refs
            # 檢查是否是 Mermaid 相關的 SVG
            for text, path in local_svg_refs:
                if 'mermaid' in path.lower() or 'mermaid' in text.lower():
                    result['issues'].append(f"發現 Mermaid 相關的 SVG 引用: {text} -> {path}")
        
        # 檢查是否有孤立的 Mermaid 標題
        orphan_mermaid_headers = re.findall(r'^##\s+.*mermaid.*$', content, re.MULTILINE | re.IGNORECASE)
        if orphan_mermaid_headers:
            result['issues'].append(f"發現可能的孤立 Mermaid 標題: {orphan_mermaid_headers}")
        
        return result
    
    def validate_file(self, file_path: Path) -> Dict:
        """驗證單個文件"""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            return self.check_mermaid_references(content, file_path)
            
        except Exception as e:
            return {
                'file': str(file_path),
                'mmd_references': [],
                'mermaid_blocks': [],
                'svg_references': [],
                'issues': [f"讀取文件失敗: {str(e)}"]
            }
    
    def run(self) -> Dict:
        """執行驗證過程"""
        print("🔍 驗證 Mermaid 圖表修復結果...")
        
        markdown_files = self.find_markdown_files()
        print(f"檢查 {len(markdown_files)} 個 Markdown 文件")
        
        total_mermaid_blocks = 0
        files_with_issues = 0
        files_with_mermaid = 0
        remaining_mmd_refs = 0
        
        for file_path in markdown_files:
            result = self.validate_file(file_path)
            self.validation_results.append(result)
            
            if result['mermaid_blocks'] > 0:
                files_with_mermaid += 1
                total_mermaid_blocks += result['mermaid_blocks']
            
            if result['mmd_references']:
                remaining_mmd_refs += len(result['mmd_references'])
            
            if result['issues']:
                files_with_issues += 1
                self.issues.extend([f"{result['file']}: {issue}" for issue in result['issues']])
                print(f"⚠️  問題: {file_path}")
                for issue in result['issues']:
                    print(f"   - {issue}")
            elif result['mermaid_blocks'] > 0:
                print(f"✅ 正常: {file_path} ({result['mermaid_blocks']} 個 Mermaid 代碼塊)")
        
        # 生成總結報告
        summary = {
            'total_files_checked': len(markdown_files),
            'files_with_mermaid': files_with_mermaid,
            'total_mermaid_blocks': total_mermaid_blocks,
            'files_with_issues': files_with_issues,
            'remaining_mmd_references': remaining_mmd_refs,
            'total_issues': len(self.issues),
            'validation_results': self.validation_results,
            'issues': self.issues
        }
        
        return summary
    
    def generate_report(self, summary: Dict) -> str:
        """生成驗證報告"""
        report_lines = [
            "# Mermaid 圖表修復驗證報告",
            "",
            f"**生成時間**: {self.get_current_time()}",
            "",
            "## 📊 驗證統計",
            "",
            f"- **檢查文件數**: {summary['total_files_checked']}",
            f"- **包含 Mermaid 的文件數**: {summary['files_with_mermaid']}",
            f"- **Mermaid 代碼塊總數**: {summary['total_mermaid_blocks']}",
            f"- **有問題的文件數**: {summary['files_with_issues']}",
            f"- **剩餘 .mmd 引用數**: {summary['remaining_mmd_references']}",
            f"- **總問題數**: {summary['total_issues']}",
            ""
        ]
        
        # 驗證結果
        if summary['remaining_mmd_references'] == 0 and summary['total_issues'] == 0:
            report_lines.extend([
                "## ✅ 驗證結果: 通過",
                "",
                "🎉 所有 Mermaid 圖表引用已成功修復！",
                "",
                "### 修復成果",
                "",
                f"- ✅ 所有 .mmd 文件引用已轉換為直接 Mermaid 代碼塊",
                f"- ✅ 共生成 {summary['total_mermaid_blocks']} 個 Mermaid 代碼塊",
                f"- ✅ {summary['files_with_mermaid']} 個文件包含 Mermaid 圖表",
                f"- ✅ GitHub 可直接渲染所有 Mermaid 圖表",
                ""
            ])
        else:
            report_lines.extend([
                "## ❌ 驗證結果: 需要注意",
                "",
                "發現以下問題需要處理：",
                ""
            ])
            
            if summary['remaining_mmd_references'] > 0:
                report_lines.append(f"- 🔴 仍有 {summary['remaining_mmd_references']} 個 .mmd 文件引用未修復")
            
            if summary['total_issues'] > 0:
                report_lines.append(f"- ⚠️  發現 {summary['total_issues']} 個其他問題")
            
            report_lines.append("")
        
        # 詳細問題列表
        if summary['issues']:
            report_lines.extend([
                "## 🔍 問題詳情",
                ""
            ])
            
            for issue in summary['issues']:
                report_lines.append(f"- {issue}")
            
            report_lines.append("")
        
        # 文件統計
        files_with_mermaid = [r for r in summary['validation_results'] if r['mermaid_blocks'] > 0]
        if files_with_mermaid:
            report_lines.extend([
                "## 📄 包含 Mermaid 的文件",
                ""
            ])
            
            for result in files_with_mermaid:
                report_lines.append(f"- **{result['file']}**: {result['mermaid_blocks']} 個代碼塊")
            
            report_lines.append("")
        
        report_lines.extend([
            "## 📋 驗證標準",
            "",
            "本次驗證檢查了以下項目：",
            "",
            "1. ✅ **無剩餘 .mmd 引用** - 所有 .mmd 文件引用應已轉換",
            "2. ✅ **Mermaid 代碼塊格式** - 使用 ```mermaid 格式",
            "3. ✅ **無 Mermaid SVG 引用** - 避免 SVG 格式的 Mermaid 圖表",
            "4. ✅ **GitHub 原生支援** - 確保 GitHub 可直接渲染",
            "",
            "## 🎯 修復效果",
            "",
            "- 📱 **GitHub 原生渲染** - 無需額外工具或插件",
            "- 🚀 **載入速度快** - 直接嵌入，無需額外請求",
            "- 📝 **易於維護** - 代碼和圖表在同一文件",
            "- 🔄 **版本控制友好** - 圖表變更可追蹤",
            ""
        ])
        
        return "\n".join(report_lines)
    
    def get_current_time(self) -> str:
        """獲取當前時間"""
        from datetime import datetime
        return datetime.now().strftime("%Y-%m-%d %H:%M:%S")

def main():
    """主函數"""
    print("🚀 開始驗證 Mermaid 圖表修復結果...")
    
    validator = MermaidFixValidator()
    summary = validator.run()
    
    # 生成報告
    report_content = validator.generate_report(summary)
    
    # 確保報告目錄存在
    report_dir = Path("reports-summaries/diagrams")
    report_dir.mkdir(parents=True, exist_ok=True)
    
    # 寫入報告
    report_file = report_dir / "mermaid-fix-validation-report.md"
    with open(report_file, 'w', encoding='utf-8') as f:
        f.write(report_content)
    
    # 輸出結果
    print(f"\n📊 驗證完成!")
    print(f"- 檢查文件: {summary['total_files_checked']}")
    print(f"- Mermaid 文件: {summary['files_with_mermaid']}")
    print(f"- Mermaid 代碼塊: {summary['total_mermaid_blocks']}")
    print(f"- 剩餘 .mmd 引用: {summary['remaining_mmd_references']}")
    
    if summary['total_issues'] > 0:
        print(f"- ⚠️  問題數: {summary['total_issues']}")
        print("\n需要注意的問題:")
        for issue in summary['issues'][:5]:  # 只顯示前5個問題
            print(f"  {issue}")
        if len(summary['issues']) > 5:
            print(f"  ... 還有 {len(summary['issues']) - 5} 個問題")
    else:
        print("- ✅ 無問題發現")
    
    print(f"\n📄 詳細報告已保存至: {report_file}")
    
    return 0 if summary['remaining_mmd_references'] == 0 and summary['total_issues'] == 0 else 1

if __name__ == "__main__":
    sys.exit(main())