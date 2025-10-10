#!/usr/bin/env python3
"""
清理 scripts 目錄中不需要的腳本
基於 hooks 使用情況和 package.json 引用來判斷哪些腳本需要保留
"""

import os
import shutil
from pathlib import Path

class ScriptsCleaner:
    def __init__(self):
        self.scripts_dir = Path("scripts")
        self.backup_dir = Path("scripts/.backup")
        
        # 被 hooks 使用的腳本 (從 hook 文件分析得出)
        self.hook_used_scripts = {
            # diagram-documentation-sync.kiro.hook 使用的腳本
            "generate-diagrams.sh",
            "generate-mermaid-diagrams.sh", 
            "generate-excalidraw-diagrams.sh",
            "validate-diagram-links.py",
            "detect-outdated-content.py",
            "assess-documentation-quality.py",
            
            # bdd-feature-monitor.kiro.hook 使用的腳本
            "analyze-bdd-features.py",
            "smart-diagram-update.py",
            "sync-diagram-references.py",
            
            # ddd-annotation-monitor.kiro.hook 使用的腳本
            "analyze-ddd-code.py",
            
            # reports-organization-monitor.kiro.hook 使用的腳本
            "organize-reports-summaries.py",
            "update-report-links.py",
        }
        
        # 被 package.json 使用的腳本
        self.package_json_scripts = {
            "check-documentation-quality.sh",
            "check-links-advanced.js",
            "validate-diagrams.py",
            "validate-metadata.py",
            "check-translation-quality.sh",
        }
        
        # 重要的核心腳本 (基礎設施和常用工具)
        self.core_scripts = {
            "markdown-files-statistics.py",  # 統計腳本
            "generate-all-diagrams.sh",     # 圖表生成
            "README.md",                    # 文檔
            "DIAGRAM-AUTOMATION-README.md", # 文檔
            "cleanup-unused-scripts.py",    # 這個清理腳本本身
            "check-all-links.py",           # 連結檢查腳本
            "build-optimized.sh",           # 構建腳本
            
            # 啟動腳本
            "start-backend.sh",
            "start-cmc-frontend.sh", 
            "start-consumer-frontend.sh",
            "start-fullstack.sh",
            "stop-backend.sh",
            "stop-cmc-frontend.sh",
            "stop-consumer-frontend.sh", 
            "stop-fullstack.sh",
            
            # 測試腳本
            "test-api.sh",
            "run-end-to-end-tests.sh",
            "run-optimized-tests.sh",
            
            # 設置腳本
            "setup-mcp-servers.sh",
            "backup-mcp-config.sh",
            "show-mcp-config.sh",
        }
        
        # 需要保留的腳本 (合併所有類別)
        self.keep_scripts = self.hook_used_scripts | self.package_json_scripts | self.core_scripts
        
        # 明確要刪除的腳本 (重複的連結檢查腳本)
        self.delete_scripts = {
            # 重複的連結檢查腳本 (保留 check-links-advanced.js)
            "check-links-final.py",
            "check-links-simple.py", 
            "comprehensive-all-files-link-check.py",
            "safe-link-check.py",
            "optimized-link-check.py",
            
            # 重複的連結修復腳本 (太多類似功能)
            "fix-all-remaining-broken-links.py",
            "fix-all-remaining-links.py",
            "fix-broken-links-comprehensive.py",
            "fix-broken-links.py",
            "fix-final-19-links.py",
            "fix-remaining-links.py",
            "final-link-cleanup.py",
            "final-remaining-fixes.py",
            "fix-path-levels.py",
            
            # 過時的圖表腳本
            "fix-diagram-filenames.py",
            "fix-diagram-references.py",
            "fix-mermaid-references.py",
            "fix-template-mmd-references.py",
            "fix-plantuml-syntax.py",
            "validate-mermaid-fixes.py",
            "final-mmd-validation.py",
            "process-orphaned-mmd-files.py",
            "create-orphaned-mmd-issue.sh",
            
            # 過時的測試腳本
            "disable-problematic-tests.sh",
            "fix-all-test-errors.sh",
            "fix-missing-methods.sh",
            "fix-test-dto-usage.sh",
            "test-failed-only.sh",
            "test-minimal.sh",
            "test-simple.sh",
            "test-unit-only.sh",
            "test-parallel-max.sh",
            "test-all-max-memory.sh",
            
            # 重複的翻譯腳本
            "batch-translate.py",
            "migrate-chinese-docs.py",
            "translate_md_to_english.py",
            "translate_md_to_english.sh",
            "mock_translator.py",
            
            # 過時的工具腳本
            "create-link-redirects.py",
            "detect-content-duplication.py",
            "unified-diagram-sync.sh",
            # "sync-diagram-references.py" 被 hooks 使用，不刪除
            "update-tasks-checkboxes.py",
            "watch-docs.py",
            "web_dashboard.py",
            "dashboard.py",
            
            # 過時的設置和配置腳本
            "add_newline_to_md.sh",
            "fix-excalidraw-path.sh",
            "fix-links.sh",
            "redis-dev.sh",
            "staging-redis-tests.sh",
            "monitor-memory.sh",
            "check-system-resources.sh",
            "setup-monitoring-alerts.sh",
            "setup-translation-system.py",
            
            # 過時的驗證腳本
            "validate-observability-deployment.sh",
            "verify-swagger-ui.sh",
            "test-database-config.sh",
            "test-documentation-quality.sh",
            "check-hook-status.py",
            "test-hook-functionality.py",
            "run-diagnostics.py",
            
            # 過時的生成和處理腳本
            "generate_data.py",
            "generate-diagram-images.sh",
            "generate-standardized-diagrams.sh",
            "excalidraw-to-svg.js",
            "excalidraw-config-manager.py",
            "excalidraw-example.py",
            "excalidraw_helpers.py",
            
            # 過時的遷移腳本
            "migrate-entities-to-optimistic-locking.py",
            "migration-workflow.py",
            "database-migration-orders-optimistic-locking.sql",
            "database-migration-phase1-optimistic-locking.sql",
            
            # 過時的執行腳本
            "execute-viewpoints-perspectives-qa.sh",
            "run-performance-reliability-tests.sh",
            "run-tests-optimized.sh",
            "test-user-experience.py",
            "performance-test.py",
            
            # 其他工具腳本
            "batch_processor.py",
            "file_manager.py", 
            "monitoring.py",
            "quality_assurance.py",
            "report_generator.py",
            "diagram-automation-manager.py",
            "test-diagram-automation.py",
        }
    
    def create_backup_dir(self):
        """創建備份目錄"""
        if not self.backup_dir.exists():
            self.backup_dir.mkdir(parents=True)
            print(f"✅ 創建備份目錄: {self.backup_dir}")
    
    def backup_and_delete_script(self, script_name):
        """備份並刪除腳本"""
        script_path = self.scripts_dir / script_name
        if script_path.exists():
            # 備份到 .backup 目錄
            backup_path = self.backup_dir / script_name
            shutil.copy2(script_path, backup_path)
            
            # 刪除原文件
            script_path.unlink()
            print(f"🗑️  刪除: {script_name} (已備份到 .backup/)")
            return True
        return False
    
    def analyze_scripts(self):
        """分析腳本使用情況"""
        print("📊 分析 scripts 目錄中的腳本...")
        print("=" * 60)
        
        all_scripts = set()
        for item in self.scripts_dir.iterdir():
            if item.is_file() and not item.name.startswith('.'):
                all_scripts.add(item.name)
        
        print(f"📄 總腳本數量: {len(all_scripts)}")
        print(f"🔗 Hook 使用的腳本: {len(self.hook_used_scripts)}")
        print(f"📦 package.json 使用的腳本: {len(self.package_json_scripts)}")
        print(f"⚙️  核心腳本: {len(self.core_scripts)}")
        print(f"✅ 需要保留的腳本: {len(self.keep_scripts)}")
        print(f"🗑️  明確要刪除的腳本: {len(self.delete_scripts)}")
        
        # 檢查是否有腳本既在保留列表又在刪除列表中
        conflicts = self.keep_scripts & self.delete_scripts
        if conflicts:
            print(f"⚠️  衝突的腳本 (既要保留又要刪除): {conflicts}")
        
        # 找出未分類的腳本
        unclassified = all_scripts - self.keep_scripts - self.delete_scripts
        if unclassified:
            print(f"❓ 未分類的腳本: {unclassified}")
        
        return all_scripts
    
    def clean_scripts(self, dry_run=True):
        """清理腳本"""
        print(f"\n{'🔍 模擬清理' if dry_run else '🧹 執行清理'}...")
        print("=" * 60)
        
        self.create_backup_dir()
        
        deleted_count = 0
        
        for script_name in sorted(self.delete_scripts):
            script_path = self.scripts_dir / script_name
            if script_path.exists():
                if dry_run:
                    print(f"🔍 將刪除: {script_name}")
                else:
                    if self.backup_and_delete_script(script_name):
                        deleted_count += 1
        
        if not dry_run:
            print(f"\n✅ 清理完成！刪除了 {deleted_count} 個腳本")
            print(f"📁 備份位置: {self.backup_dir}")
        else:
            potential_deletes = len([s for s in self.delete_scripts if (self.scripts_dir / s).exists()])
            print(f"\n🔍 模擬完成！將刪除 {potential_deletes} 個腳本")
    
    def show_kept_scripts(self):
        """顯示保留的腳本"""
        print("\n📋 保留的腳本分類:")
        print("=" * 60)
        
        print("\n🔗 Hook 使用的腳本:")
        for script in sorted(self.hook_used_scripts):
            if (self.scripts_dir / script).exists():
                print(f"  ✅ {script}")
            else:
                print(f"  ❌ {script} (不存在)")
        
        print("\n📦 package.json 使用的腳本:")
        for script in sorted(self.package_json_scripts):
            if (self.scripts_dir / script).exists():
                print(f"  ✅ {script}")
            else:
                print(f"  ❌ {script} (不存在)")
        
        print("\n⚙️  核心腳本:")
        for script in sorted(self.core_scripts):
            if (self.scripts_dir / script).exists():
                print(f"  ✅ {script}")
            else:
                print(f"  ❌ {script} (不存在)")

def main():
    """主函數"""
    import argparse
    
    parser = argparse.ArgumentParser(description="清理 scripts 目錄中不需要的腳本")
    parser.add_argument("--execute", action="store_true", help="執行實際清理 (預設為模擬模式)")
    parser.add_argument("--analyze-only", action="store_true", help="只分析不清理")
    
    args = parser.parse_args()
    
    cleaner = ScriptsCleaner()
    
    # 分析腳本
    all_scripts = cleaner.analyze_scripts()
    
    if not args.analyze_only:
        # 顯示保留的腳本
        cleaner.show_kept_scripts()
        
        # 清理腳本
        cleaner.clean_scripts(dry_run=not args.execute)
        
        if not args.execute:
            print("\n💡 提示: 使用 --execute 參數來執行實際清理")

if __name__ == "__main__":
    main()