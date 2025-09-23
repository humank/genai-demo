#!/usr/bin/env python3
"""
修復 Mermaid 圖表引用腳本
將 .mmd 文件引用替換為直接的 Mermaid 代碼塊
"""

import os
import re
import sys
from pathlib import Path
from typing import List, Tuple, Dict

class MermaidReferenceFixer:
    def __init__(self):
        self.root_dir = Path(".")
        self.fixes_applied = []
        self.errors = []
        
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
    
    def read_mermaid_file(self, mmd_path: Path) -> str:
        """讀取 .mmd 文件內容"""
        try:
            if mmd_path.exists():
                with open(mmd_path, 'r', encoding='utf-8') as f:
                    return f.read().strip()
            else:
                self.errors.append(f"Mermaid 文件不存在: {mmd_path}")
                return ""
        except Exception as e:
            self.errors.append(f"讀取 Mermaid 文件失敗 {mmd_path}: {str(e)}")
            return ""
    
    def calculate_relative_path(self, from_file: Path, to_file: Path) -> Path:
        """計算相對路徑"""
        try:
            return Path(os.path.relpath(to_file, from_file.parent))
        except ValueError:
            return to_file
    
    def find_mermaid_references(self, content: str, file_path: Path) -> List[Tuple[str, str, str]]:
        """找到所有 .mmd 文件引用"""
        references = []
        
        # 匹配 Markdown 鏈接格式: [text](path.mmd)
        link_pattern = r'\[([^\]]+)\]\(([^)]+\.mmd)\)'
        
        for match in re.finditer(link_pattern, content):
            link_text = match.group(1)
            mmd_path = match.group(2)
            full_match = match.group(0)
            
            # 解析相對路徑
            if not mmd_path.startswith('http'):
                # 計算 .mmd 文件的絕對路徑
                if mmd_path.startswith('/'):
                    # 絕對路徑
                    abs_mmd_path = self.root_dir / mmd_path.lstrip('/')
                else:
                    # 相對路徑
                    abs_mmd_path = file_path.parent / mmd_path
                
                abs_mmd_path = abs_mmd_path.resolve()
                
                references.append((full_match, link_text, str(abs_mmd_path)))
        
        return references
    
    def create_mermaid_code_block(self, link_text: str, mermaid_content: str) -> str:
        """創建 Mermaid 代碼塊"""
        if not mermaid_content:
            return f"<!-- {link_text}: Mermaid 內容無法載入 -->"
        
        return f"""## {link_text}

```mermaid
{mermaid_content}
```"""
    
    def fix_file(self, file_path: Path) -> bool:
        """修復單個文件中的 Mermaid 引用"""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            original_content = content
            references = self.find_mermaid_references(content, file_path)
            
            if not references:
                return False
            
            fixes_in_file = []
            
            for full_match, link_text, mmd_path in references:
                # 讀取 Mermaid 文件內容
                mermaid_content = self.read_mermaid_file(Path(mmd_path))
                
                if mermaid_content:
                    # 創建 Mermaid 代碼塊
                    mermaid_block = self.create_mermaid_code_block(link_text, mermaid_content)
                    
                    # 替換引用
                    content = content.replace(full_match, mermaid_block)
                    
                    fixes_in_file.append({
                        'original': full_match,
                        'replacement': f"Mermaid 代碼塊: {link_text}",
                        'mmd_file': mmd_path
                    })
                else:
                    # 如果無法讀取 Mermaid 文件，保留原始引用但添加註釋
                    comment = f"<!-- 無法載入 Mermaid 文件: {mmd_path} -->\n{full_match}"
                    content = content.replace(full_match, comment)
                    
                    fixes_in_file.append({
                        'original': full_match,
                        'replacement': f"添加錯誤註釋: {mmd_path}",
                        'mmd_file': mmd_path
                    })
            
            # 如果有修改，寫回文件
            if content != original_content:
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(content)
                
                self.fixes_applied.append({
                    'file': str(file_path),
                    'fixes': fixes_in_file
                })
                
                return True
            
            return False
            
        except Exception as e:
            self.errors.append(f"處理文件失敗 {file_path}: {str(e)}")
            return False
    
    def run(self) -> Dict:
        """執行修復過程"""
        print("🔍 搜尋 Markdown 文件中的 Mermaid 引用...")
        
        markdown_files = self.find_markdown_files()
        print(f"找到 {len(markdown_files)} 個 Markdown 文件")
        
        fixed_files = 0
        
        for file_path in markdown_files:
            if self.fix_file(file_path):
                fixed_files += 1
                print(f"✅ 修復: {file_path}")
        
        # 生成報告
        report = {
            'total_files_scanned': len(markdown_files),
            'files_fixed': fixed_files,
            'total_fixes': sum(len(fix['fixes']) for fix in self.fixes_applied),
            'fixes_applied': self.fixes_applied,
            'errors': self.errors
        }
        
        return report
    
    def generate_report(self, report: Dict) -> str:
        """生成修復報告"""
        report_lines = [
            "# Mermaid 圖表引用修復報告",
            "",
            f"**生成時間**: {self.get_current_time()}",
            "",
            "## 📊 修復統計",
            "",
            f"- **掃描文件數**: {report['total_files_scanned']}",
            f"- **修復文件數**: {report['files_fixed']}",
            f"- **總修復數**: {report['total_fixes']}",
            f"- **錯誤數**: {len(report['errors'])}",
            ""
        ]
        
        if report['fixes_applied']:
            report_lines.extend([
                "## 🔧 修復詳情",
                ""
            ])
            
            for fix in report['fixes_applied']:
                report_lines.extend([
                    f"### 📄 {fix['file']}",
                    ""
                ])
                
                for detail in fix['fixes']:
                    report_lines.extend([
                        f"**原始引用**: `{detail['original']}`",
                        f"**修復為**: {detail['replacement']}",
                        f"**Mermaid 文件**: `{detail['mmd_file']}`",
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
            "## 📋 修復說明",
            "",
            "本次修復將以下類型的引用進行了處理：",
            "",
            "1. **Mermaid 文件引用** (`[text](path.mmd)`) → **直接 Mermaid 代碼塊**",
            "2. **保持 GitHub 原生支援** - 使用 ```mermaid 代碼塊格式",
            "3. **錯誤處理** - 無法讀取的文件添加註釋說明",
            "",
            "## 🎯 修復效果",
            "",
            "- ✅ GitHub 可直接渲染 Mermaid 圖表",
            "- ✅ 無需額外的文件依賴",
            "- ✅ 更好的文檔可讀性",
            "- ✅ 符合圖表生成標準",
            ""
        ])
        
        return "\n".join(report_lines)
    
    def get_current_time(self) -> str:
        """獲取當前時間"""
        from datetime import datetime
        return datetime.now().strftime("%Y-%m-%d %H:%M:%S")

def main():
    """主函數"""
    print("🚀 開始修復 Mermaid 圖表引用...")
    
    fixer = MermaidReferenceFixer()
    report = fixer.run()
    
    # 生成報告
    report_content = fixer.generate_report(report)
    
    # 確保報告目錄存在
    report_dir = Path("reports-summaries/diagrams")
    report_dir.mkdir(parents=True, exist_ok=True)
    
    # 寫入報告
    report_file = report_dir / "mermaid-references-fix-report.md"
    with open(report_file, 'w', encoding='utf-8') as f:
        f.write(report_content)
    
    # 輸出結果
    print(f"\n📊 修復完成!")
    print(f"- 掃描文件: {report['total_files_scanned']}")
    print(f"- 修復文件: {report['files_fixed']}")
    print(f"- 總修復數: {report['total_fixes']}")
    
    if report['errors']:
        print(f"- 錯誤數: {len(report['errors'])}")
        print("\n❌ 錯誤詳情:")
        for error in report['errors']:
            print(f"  {error}")
    
    print(f"\n📄 詳細報告已保存至: {report_file}")
    
    return 0 if not report['errors'] else 1

if __name__ == "__main__":
    sys.exit(main())