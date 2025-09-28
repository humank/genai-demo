#!/usr/bin/env python3
"""
分析 reports-summaries 目錄中可以刪除的過時報告
"""

import os
import re
from pathlib import Path
from datetime import datetime, timedelta
from collections import defaultdict

class ReportsCleanupAnalyzer:
    def __init__(self):
        self.reports_dir = Path("reports-summaries")
        self.backup_dir = Path("reports-summaries/.cleanup-backup")
        
        # 可以刪除的報告類型
        self.deletable_patterns = {
            # 重複的報告 (保留最新版本)
            "duplicates": [
                r"(.+)_1\.md$",  # 帶 _1 後綴的重複文件
                r"(.+)-\d{8}_\d{6}\.md$",  # 帶時間戳的重複文件
            ],
            
            # 過時的臨時報告
            "temporary": [
                r".*-temp.*\.md$",
                r".*-draft.*\.md$", 
                r".*-wip.*\.md$",
                r".*-test.*\.md$",
            ],
            
            # 完成的遷移和修復報告 (保留最終報告)
            "completed_migrations": [
                r".*migration.*report.*\.md$",
                r".*fix.*report.*\.md$",
                r".*cleanup.*report.*\.md$",
                r".*completion.*report.*\.md$",
            ],
            
            # 過時的分析報告 (超過30天)
            "outdated_analysis": [
                r".*analysis.*\d{8}.*\.md$",
                r".*quality.*\d{8}.*\.md$",
                r".*validation.*\d{8}.*\.md$",
            ],
        }
        
        # 必須保留的重要報告
        self.keep_patterns = [
            r"README\.md$",
            r".*FINAL.*REPORT\.md$",
            r".*SUMMARY\.md$",
            r".*COMPLETION.*REPORT\.md$",
            r"SCRIPTS_CLEANUP_REPORT\.md$",
        ]
        
        # 按類別分析結果
        self.analysis_results = defaultdict(list)
        
    def is_important_report(self, file_path):
        """檢查是否為重要報告"""
        filename = file_path.name
        for pattern in self.keep_patterns:
            if re.match(pattern, filename, re.IGNORECASE):
                return True
        return False
    
    def get_file_age_days(self, file_path):
        """獲取文件年齡（天數）"""
        try:
            mtime = file_path.stat().st_mtime
            file_date = datetime.fromtimestamp(mtime)
            return (datetime.now() - file_date).days
        except:
            return 0
    
    def find_duplicates(self):
        """找出重複的報告文件"""
        duplicates = []
        
        for root, dirs, files in os.walk(self.reports_dir):
            root_path = Path(root)
            
            # 按基本名稱分組文件
            base_names = defaultdict(list)
            
            for file in files:
                if not file.endswith('.md'):
                    continue
                    
                file_path = root_path / file
                
                # 提取基本名稱 (去除版本號和時間戳)
                base_name = file
                base_name = re.sub(r'_\d+\.md$', '.md', base_name)  # 移除 _1, _2 等
                base_name = re.sub(r'-\d{8}_\d{6}\.md$', '.md', base_name)  # 移除時間戳
                base_name = re.sub(r'-\d{8}\.md$', '.md', base_name)  # 移除日期
                
                base_names[base_name].append(file_path)
            
            # 找出有多個版本的文件
            for base_name, file_list in base_names.items():
                if len(file_list) > 1:
                    # 按修改時間排序，保留最新的
                    file_list.sort(key=lambda x: x.stat().st_mtime, reverse=True)
                    
                    # 除了最新的，其他都是重複的
                    for duplicate in file_list[1:]:
                        if not self.is_important_report(duplicate):
                            duplicates.append({
                                'file': duplicate,
                                'reason': f'Duplicate of {file_list[0].name}',
                                'category': 'duplicates',
                                'age_days': self.get_file_age_days(duplicate)
                            })
        
        return duplicates
    
    def find_outdated_reports(self):
        """找出過時的報告"""
        outdated = []
        cutoff_date = datetime.now() - timedelta(days=30)
        
        for root, dirs, files in os.walk(self.reports_dir):
            root_path = Path(root)
            
            for file in files:
                if not file.endswith('.md'):
                    continue
                    
                file_path = root_path / file
                
                if self.is_important_report(file_path):
                    continue
                
                # 檢查是否為過時的分析報告
                age_days = self.get_file_age_days(file_path)
                
                # 特定類型的過時報告
                if any(re.search(pattern, file, re.IGNORECASE) for pattern in [
                    r'quality.*\d{8}',
                    r'analysis.*\d{8}', 
                    r'validation.*\d{8}',
                    r'content-duplication.*\d{8}',
                    r'outdated-content.*\d{8}',
                ]):
                    if age_days > 7:  # 超過7天的分析報告
                        outdated.append({
                            'file': file_path,
                            'reason': f'Outdated analysis report ({age_days} days old)',
                            'category': 'outdated_analysis',
                            'age_days': age_days
                        })
        
        return outdated
    
    def find_completed_tasks(self):
        """找出已完成任務的報告"""
        completed = []
        
        # 已完成的遷移和修復任務
        completed_keywords = [
            'mermaid.*complete',
            'migration.*complete', 
            'fix.*complete',
            'cleanup.*complete',
            'implementation.*complete',
            '.*final.*report',
        ]
        
        for root, dirs, files in os.walk(self.reports_dir):
            root_path = Path(root)
            
            for file in files:
                if not file.endswith('.md'):
                    continue
                    
                file_path = root_path / file
                
                if self.is_important_report(file_path):
                    continue
                
                # 檢查是否為已完成任務的中間報告
                for keyword in completed_keywords:
                    if re.search(keyword, file, re.IGNORECASE):
                        # 如果有多個相關報告，保留最終的
                        if not re.search(r'(final|summary|completion)', file, re.IGNORECASE):
                            completed.append({
                                'file': file_path,
                                'reason': f'Intermediate report for completed task',
                                'category': 'completed_tasks',
                                'age_days': self.get_file_age_days(file_path)
                            })
                        break
        
        return completed
    
    def analyze_quality_ux_reports(self):
        """特別分析 quality-ux 目錄中的大量重複報告"""
        quality_ux_dir = self.reports_dir / "quality-ux"
        if not quality_ux_dir.exists():
            return []
        
        deletable = []
        
        # 按類型分組
        report_groups = defaultdict(list)
        
        for file_path in quality_ux_dir.glob("*.md"):
            filename = file_path.name
            
            # 提取報告類型和日期
            if match := re.match(r'(.+)-(\d{8}_\d{6})\.(md|json)$', filename):
                report_type = match.group(1)
                timestamp = match.group(2)
                report_groups[report_type].append((file_path, timestamp))
        
        # 對每個類型，只保留最新的2個報告
        for report_type, files in report_groups.items():
            if len(files) > 2:
                # 按時間戳排序
                files.sort(key=lambda x: x[1], reverse=True)
                
                # 保留最新的2個，刪除其他的
                for file_path, timestamp in files[2:]:
                    deletable.append({
                        'file': file_path,
                        'reason': f'Old {report_type} report (keeping latest 2)',
                        'category': 'quality_ux_cleanup',
                        'age_days': self.get_file_age_days(file_path)
                    })
        
        return deletable
    
    def analyze_all_reports(self):
        """分析所有報告"""
        print("🔍 分析 reports-summaries 目錄中的報告...")
        print("=" * 60)
        
        # 收集所有可刪除的報告
        all_deletable = []
        
        # 1. 找出重複文件
        duplicates = self.find_duplicates()
        all_deletable.extend(duplicates)
        self.analysis_results['duplicates'] = duplicates
        
        # 2. 找出過時報告
        outdated = self.find_outdated_reports()
        all_deletable.extend(outdated)
        self.analysis_results['outdated'] = outdated
        
        # 3. 找出已完成任務報告
        completed = self.find_completed_tasks()
        all_deletable.extend(completed)
        self.analysis_results['completed'] = completed
        
        # 4. 特別處理 quality-ux 目錄
        quality_ux = self.analyze_quality_ux_reports()
        all_deletable.extend(quality_ux)
        self.analysis_results['quality_ux'] = quality_ux
        
        return all_deletable
    
    def print_analysis_results(self):
        """輸出分析結果"""
        total_files = sum(len(files) for files in self.analysis_results.values())
        
        print(f"📊 分析結果總覽:")
        print(f"   可刪除報告總數: {total_files}")
        print()
        
        for category, files in self.analysis_results.items():
            if files:
                print(f"📁 {category.replace('_', ' ').title()} ({len(files)} 個文件):")
                
                # 按目錄分組顯示
                by_dir = defaultdict(list)
                for item in files:
                    dir_name = item['file'].parent.name
                    by_dir[dir_name].append(item)
                
                for dir_name, dir_files in sorted(by_dir.items()):
                    print(f"   📂 {dir_name}/:")
                    for item in sorted(dir_files, key=lambda x: x['file'].name)[:5]:  # 只顯示前5個
                        age_info = f" ({item['age_days']}天前)" if item['age_days'] > 0 else ""
                        print(f"     🗑️  {item['file'].name}{age_info}")
                    if len(dir_files) > 5:
                        print(f"     ... 還有 {len(dir_files) - 5} 個文件")
                print()
    
    def create_backup_and_delete(self, deletable_files, dry_run=True):
        """創建備份並刪除文件"""
        if not deletable_files:
            print("✅ 沒有需要刪除的文件")
            return
        
        print(f"{'🔍 模擬刪除' if dry_run else '🧹 執行刪除'}...")
        print("=" * 60)
        
        if not dry_run:
            # 創建備份目錄
            self.backup_dir.mkdir(parents=True, exist_ok=True)
            print(f"✅ 創建備份目錄: {self.backup_dir}")
        
        deleted_count = 0
        
        for item in deletable_files:
            file_path = item['file']
            
            if dry_run:
                print(f"🔍 將刪除: {file_path.relative_to(self.reports_dir)} - {item['reason']}")
            else:
                try:
                    # 創建相對應的備份目錄結構
                    relative_path = file_path.relative_to(self.reports_dir)
                    backup_path = self.backup_dir / relative_path
                    backup_path.parent.mkdir(parents=True, exist_ok=True)
                    
                    # 備份文件
                    import shutil
                    shutil.copy2(file_path, backup_path)
                    
                    # 刪除原文件
                    file_path.unlink()
                    
                    print(f"🗑️  刪除: {relative_path} (已備份)")
                    deleted_count += 1
                    
                except Exception as e:
                    print(f"❌ 刪除失敗: {relative_path} - {e}")
        
        if not dry_run:
            print(f"\n✅ 清理完成！刪除了 {deleted_count} 個報告")
            print(f"📁 備份位置: {self.backup_dir}")
        else:
            potential_deletes = len(deletable_files)
            print(f"\n🔍 模擬完成！將刪除 {potential_deletes} 個報告")

def main():
    """主函數"""
    import argparse
    
    parser = argparse.ArgumentParser(description="分析和清理 reports-summaries 目錄中的過時報告")
    parser.add_argument("--execute", action="store_true", help="執行實際清理 (預設為模擬模式)")
    parser.add_argument("--analyze-only", action="store_true", help="只分析不清理")
    
    args = parser.parse_args()
    
    analyzer = ReportsCleanupAnalyzer()
    
    # 分析報告
    deletable_files = analyzer.analyze_all_reports()
    
    # 顯示分析結果
    analyzer.print_analysis_results()
    
    if not args.analyze_only:
        # 執行清理
        analyzer.create_backup_and_delete(deletable_files, dry_run=not args.execute)
        
        if not args.execute:
            print("\n💡 提示: 使用 --execute 參數來執行實際清理")

if __name__ == "__main__":
    main()