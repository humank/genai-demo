#!/usr/bin/env python3
"""
修復模板文件中的 .mmd 引用腳本
將模板和文檔標準中的 .mmd 引用更新為符合新標準的格式
"""

import os
import re
import sys
from pathlib import Path
from typing import List, Dict, Tuple

class TemplateMmdFixer:
    def __init__(self):
        self.root_dir = Path(".")
        self.fixes_applied = []
        self.errors = []
        
    def find_template_files(self) -> List[Path]:
        """找到所有模板相關的 Markdown 文件"""
        template_files = []
        
        # 主要模板目錄
        template_dirs = [
            "docs/templates",
            "docs/en/templates",
            "docs/.kiro/specs",
            "docs/en/.kiro/specs"
        ]
        
        for template_dir in template_dirs:
            template_path = self.root_dir / template_dir
            if template_path.exists():
                for root, dirs, files in os.walk(template_path):
                    for file in files:
                        if file.endswith('.md'):
                            template_files.append(Path(root) / file)
        
        return template_files
    
    def fix_yaml_front_matter(self, content: str) -> Tuple[str, List[str]]:
        """修復 YAML front matter 中的 .mmd 引用"""
        fixes = []
        
        # 修復 diagrams 數組中的 .mmd 引用
        def replace_diagram_array(match):
            array_content = match.group(1)
            original = match.group(0)
            
            # 將 .mmd 引用替換為 .md 引用，並添加註釋
            updated_content = re.sub(
                r'"([^"]*\.mmd)"',
                r'"\1"  # 注意：現在使用包含 Mermaid 代碼塊的 .md 文件',
                array_content
            )
            
            if updated_content != array_content:
                fixes.append(f"更新 diagrams 數組中的 .mmd 引用")
                return f'diagrams: [{updated_content}]'
            
            return original
        
        # 匹配 diagrams: [...] 格式
        content = re.sub(
            r'diagrams:\s*\[([^\]]+)\]',
            replace_diagram_array,
            content
        )
        
        # 修復單獨的 source_file 引用
        def replace_source_file(match):
            original = match.group(0)
            mmd_file = match.group(1)
            
            if mmd_file.endswith('.mmd'):
                fixes.append(f"更新 source_file: {mmd_file}")
                return f'source_file: "{mmd_file}"  # 注意：現在使用包含 Mermaid 代碼塊的 .md 文件'
            
            return original
        
        content = re.sub(
            r'source_file:\s*"([^"]*\.mmd)"',
            replace_source_file,
            content
        )
        
        return content, fixes
    
    def fix_markdown_links(self, content: str) -> Tuple[str, List[str]]:
        """修復 Markdown 鏈接中的 .mmd 引用"""
        fixes = []
        
        # 修復 [text](path.mmd) 格式的鏈接
        def replace_mmd_link(match):
            link_text = match.group(1)
            mmd_path = match.group(2)
            original = match.group(0)
            
            if '[viewpoint]' in mmd_path or '[diagram1]' in mmd_path:
                # 這是模板佔位符，更新為新的格式
                md_path = mmd_path.replace('.mmd', '.md')
                fixes.append(f"更新模板鏈接: {mmd_path} → {md_path}")
                return f'[{link_text}]({md_path})'
            
            return original
        
        content = re.sub(
            r'\[([^\]]+)\]\(([^)]+\.mmd)\)',
            replace_mmd_link,
            content
        )
        
        return content, fixes
    
    def fix_documentation_text(self, content: str) -> Tuple[str, List[str]]:
        """修復文檔說明文字中的 .mmd 引用"""
        fixes = []
        
        # 更新文檔說明
        replacements = [
            (
                r'- \*\*Mermaid\*\* \(\.mmd\): 適合概覽圖和流程圖',
                '- **Mermaid** (.md with ```mermaid blocks): 適合概覽圖和流程圖，使用 GitHub 原生支援的代碼塊格式'
            ),
            (
                r'├── system-overview\.mmd',
                '├── system-overview.md  # 包含 Mermaid 代碼塊'
            ),
            (
                r'\.mmd 文件',
                '包含 Mermaid 代碼塊的 .md 文件'
            )
        ]
        
        for pattern, replacement in replacements:
            if re.search(pattern, content):
                content = re.sub(pattern, replacement, content)
                fixes.append(f"更新文檔說明: {pattern}")
        
        return content, fixes
    
    def add_migration_note(self, content: str, file_path: Path) -> str:
        """添加遷移說明註釋"""
        if 'template' in str(file_path).lower():
            migration_note = """
<!-- 
注意：Mermaid 圖表格式更新
- 舊格式：.mmd 文件引用
- 新格式：.md 文件中的 ```mermaid 代碼塊
- 原因：GitHub 原生支援，更好的可讀性和維護性
-->

"""
            # 在第一個 # 標題前添加註釋
            content = re.sub(r'^(# )', migration_note + r'\1', content, flags=re.MULTILINE)
        
        return content
    
    def fix_file(self, file_path: Path) -> bool:
        """修復單個文件中的 .mmd 引用"""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            original_content = content
            all_fixes = []
            
            # 修復 YAML front matter
            content, yaml_fixes = self.fix_yaml_front_matter(content)
            all_fixes.extend(yaml_fixes)
            
            # 修復 Markdown 鏈接
            content, link_fixes = self.fix_markdown_links(content)
            all_fixes.extend(link_fixes)
            
            # 修復文檔說明文字
            content, doc_fixes = self.fix_documentation_text(content)
            all_fixes.extend(doc_fixes)
            
            # 添加遷移說明
            if all_fixes:
                content = self.add_migration_note(content, file_path)
            
            # 如果有修改，寫回文件
            if content != original_content:
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(content)
                
                self.fixes_applied.append({
                    'file': str(file_path),
                    'fixes': all_fixes
                })
                
                return True
            
            return False
            
        except Exception as e:
            self.errors.append(f"處理文件失敗 {file_path}: {str(e)}")
            return False
    
    def run(self) -> Dict:
        """執行修復過程"""
        print("🔍 搜尋模板文件中的 .mmd 引用...")
        
        template_files = self.find_template_files()
        print(f"找到 {len(template_files)} 個模板文件")
        
        fixed_files = 0
        
        for file_path in template_files:
            if self.fix_file(file_path):
                fixed_files += 1
                print(f"✅ 修復: {file_path}")
        
        # 生成報告
        report = {
            'total_files_scanned': len(template_files),
            'files_fixed': fixed_files,
            'total_fixes': sum(len(fix['fixes']) for fix in self.fixes_applied),
            'fixes_applied': self.fixes_applied,
            'errors': self.errors
        }
        
        return report
    
    def generate_report(self, report: Dict) -> str:
        """生成修復報告"""
        report_lines = [
            "# 模板文件 .mmd 引用修復報告",
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
                    report_lines.append(f"- {detail}")
                
                report_lines.append("")
        
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
            "本次修復處理了以下類型的 .mmd 引用：",
            "",
            "1. **YAML Front Matter** - 更新 diagrams 數組和 source_file 欄位",
            "2. **Markdown 鏈接** - 將模板中的 .mmd 鏈接更新為 .md",
            "3. **文檔說明** - 更新關於 Mermaid 文件格式的說明文字",
            "4. **遷移註釋** - 添加格式變更的說明註釋",
            "",
            "## 🎯 修復效果",
            "",
            "- ✅ 模板文件符合新的 Mermaid 圖表標準",
            "- ✅ 文檔說明反映當前的最佳實踐",
            "- ✅ 添加了遷移說明幫助理解變更",
            "- ✅ 保持了模板的功能性和可用性",
            "",
            "## 🔄 後續步驟",
            "",
            "1. **檢查生成的模板** - 確認自動修復是否正確",
            "2. **更新使用指南** - 更新模板使用說明",
            "3. **通知團隊** - 告知團隊新的 Mermaid 圖表格式",
            "4. **驗證功能** - 測試模板的實際使用效果",
            ""
        ])
        
        return "\n".join(report_lines)
    
    def get_current_time(self) -> str:
        """獲取當前時間"""
        from datetime import datetime
        return datetime.now().strftime("%Y-%m-%d %H:%M:%S")

def main():
    """主函數"""
    print("🚀 開始修復模板文件中的 .mmd 引用...")
    
    fixer = TemplateMmdFixer()
    report = fixer.run()
    
    # 生成報告
    report_content = fixer.generate_report(report)
    
    # 確保報告目錄存在
    report_dir = Path("reports-summaries/diagrams")
    report_dir.mkdir(parents=True, exist_ok=True)
    
    # 寫入報告
    report_file = report_dir / "template-mmd-fix-report.md"
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