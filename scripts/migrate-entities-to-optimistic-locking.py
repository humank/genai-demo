#!/usr/bin/env python3
"""
JPA 實體樂觀鎖遷移腳本

此腳本自動將 JPA 實體遷移到繼承 BaseOptimisticLockingEntity，
包括移除重複欄位、更新建構子和生成數據庫遷移腳本。

使用方式:
    python3 scripts/migrate-entities-to-optimistic-locking.py --entity-path app/src/main/java/solid/humank/genaidemo/infrastructure/order/persistence/entity/JpaOrderEntity.java
    python3 scripts/migrate-entities-to-optimistic-locking.py --batch --priority high
"""

import os
import re
import sys
import argparse
from pathlib import Path
from typing import List, Dict, Set
import logging

# 設置日誌
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class EntityMigrator:
    def __init__(self):
        self.base_optimistic_locking_import = "solid.humank.genaidemo.infrastructure.common.persistence.BaseOptimisticLockingEntity"
        
        # 需要移除的重複欄位
        self.duplicate_fields = {
            'version', 'createdAt', 'created_at', 'updatedAt', 'updated_at'
        }
        
        # 實體優先級分類
        self.entity_priorities = {
            'high': [
                'JpaOrderEntity', 'JpaOrderItemEntity', 'JpaOrderWorkflowEntity',
                'JpaInventoryEntity', 'JpaReservationEntity', 'StockMovement',
                'JpaShoppingCartEntity', 'JpaCartItemEntity',
                'JpaPaymentEntity', 'JpaPaymentMethodEntity'
            ],
            'medium': [
                'ProductJpaEntity', 'JpaProductReviewEntity',
                'JpaPromotionEntity', 'JpaVoucherEntity',
                'JpaSellerEntity'
            ],
            'low': [
                'JpaNotificationEntity', 'JpaNotificationTemplateEntity',
                'JpaAnalyticsEventEntity', 'JpaAnalyticsSessionEntity',
                'JpaPricingRuleEntity'
            ]
        }

    def find_entities_by_priority(self, priority: str) -> List[Path]:
        """根據優先級查找實體文件"""
        entities = self.entity_priorities.get(priority, [])
        found_files = []
        
        # 搜尋整個專案目錄
        project_root = Path('app/src/main/java')
        for entity_name in entities:
            for java_file in project_root.rglob(f"{entity_name}.java"):
                found_files.append(java_file)
                
        return found_files

    def analyze_entity_file(self, file_path: Path) -> Dict:
        """分析實體文件，提取相關信息"""
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        analysis = {
            'file_path': file_path,
            'class_name': self._extract_class_name(content),
            'table_name': self._extract_table_name(content),
            'has_base_class': 'extends BaseOptimisticLockingEntity' in content,
            'duplicate_fields': self._find_duplicate_fields(content),
            'imports': self._extract_imports(content),
            'needs_migration': False
        }
        
        # 判斷是否需要遷移
        if not analysis['has_base_class'] and '@Entity' in content:
            analysis['needs_migration'] = True
            
        return analysis

    def _extract_class_name(self, content: str) -> str:
        """提取類別名稱"""
        match = re.search(r'public class (\w+)', content)
        return match.group(1) if match else 'Unknown'

    def _extract_table_name(self, content: str) -> str:
        """提取表格名稱"""
        match = re.search(r'@Table\(name = "([^"]+)"\)', content)
        return match.group(1) if match else 'unknown_table'

    def _find_duplicate_fields(self, content: str) -> List[str]:
        """查找重複欄位"""
        found_fields = []
        
        # 查找欄位聲明
        field_patterns = [
            r'private\s+Long\s+version\s*;',
            r'private\s+LocalDateTime\s+createdAt\s*;',
            r'private\s+LocalDateTime\s+updatedAt\s*;',
            r'@Column\([^)]*name\s*=\s*"created_at"[^)]*\)',
            r'@Column\([^)]*name\s*=\s*"updated_at"[^)]*\)',
            r'@Column\([^)]*name\s*=\s*"version"[^)]*\)'
        ]
        
        for pattern in field_patterns:
            if re.search(pattern, content, re.IGNORECASE):
                found_fields.append(pattern)
                
        return found_fields

    def _extract_imports(self, content: str) -> List[str]:
        """提取 import 語句"""
        imports = re.findall(r'import\s+([^;]+);', content)
        return imports

    def migrate_entity_file(self, file_path: Path, dry_run: bool = False) -> bool:
        """遷移單個實體文件"""
        logger.info(f"開始遷移實體: {file_path}")
        
        analysis = self.analyze_entity_file(file_path)
        
        if not analysis['needs_migration']:
            logger.info(f"實體 {analysis['class_name']} 已經遷移或不需要遷移")
            return False
            
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # 執行遷移轉換
        migrated_content = self._perform_migration(content, analysis)
        
        if dry_run:
            logger.info(f"DRY RUN: 將會修改 {file_path}")
            self._show_diff(content, migrated_content)
            return True
        
        # 備份原始文件
        backup_path = file_path.with_suffix('.java.backup')
        with open(backup_path, 'w', encoding='utf-8') as f:
            f.write(content)
        logger.info(f"原始文件已備份到: {backup_path}")
        
        # 寫入遷移後的內容
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(migrated_content)
        
        logger.info(f"實體 {analysis['class_name']} 遷移完成")
        return True

    def _perform_migration(self, content: str, analysis: Dict) -> str:
        """執行實際的遷移轉換"""
        migrated = content
        
        # 1. 添加 BaseOptimisticLockingEntity import
        if self.base_optimistic_locking_import not in migrated:
            import_section = re.search(r'(import\s+[^;]+;\s*)+', migrated)
            if import_section:
                last_import = import_section.end()
                new_import = f"import {self.base_optimistic_locking_import};\n"
                migrated = migrated[:last_import] + new_import + migrated[last_import:]
        
        # 2. 修改類別聲明以繼承 BaseOptimisticLockingEntity
        class_pattern = r'(public class \w+)(\s*{)'
        migrated = re.sub(class_pattern, r'\1 extends BaseOptimisticLockingEntity\2', migrated)
        
        # 3. 移除重複欄位
        migrated = self._remove_duplicate_fields(migrated)
        
        # 4. 移除重複的 getter/setter 方法
        migrated = self._remove_duplicate_methods(migrated)
        
        # 5. 添加遷移註釋
        migration_comment = f"""
/**
 * 更新日期: 2025年9月24日 下午2:34 (台北時間)
 * 更新內容: 繼承 BaseOptimisticLockingEntity 以支援 Aurora 樂觀鎖機制
 * 需求: 1.1 - 並發控制機制全面重構
 */"""
        
        # 在類別聲明前添加註釋
        class_declaration = re.search(r'(@Entity[^{]*public class \w+[^{]*{)', migrated, re.DOTALL)
        if class_declaration:
            migrated = migrated.replace(class_declaration.group(1), 
                                      migration_comment + '\n' + class_declaration.group(1))
        
        return migrated

    def _remove_duplicate_fields(self, content: str) -> str:
        """移除重複欄位"""
        # 移除版本號欄位
        content = re.sub(r'\s*@Version[^;]*;?\s*', '', content, flags=re.DOTALL)
        content = re.sub(r'\s*private\s+Long\s+version\s*;[^\n]*\n?', '', content)
        
        # 移除時間戳記欄位
        content = re.sub(r'\s*@Column\([^)]*name\s*=\s*"created_at"[^)]*\)[^;]*;?\s*', '', content, flags=re.DOTALL)
        content = re.sub(r'\s*@Column\([^)]*name\s*=\s*"updated_at"[^)]*\)[^;]*;?\s*', '', content, flags=re.DOTALL)
        content = re.sub(r'\s*private\s+LocalDateTime\s+createdAt\s*;[^\n]*\n?', '', content)
        content = re.sub(r'\s*private\s+LocalDateTime\s+updatedAt\s*;[^\n]*\n?', '', content)
        
        # 移除 @PrePersist 和 @PreUpdate 方法（如果存在）
        content = re.sub(r'\s*@PrePersist[^}]*}\s*', '', content, flags=re.DOTALL)
        content = re.sub(r'\s*@PreUpdate[^}]*}\s*', '', content, flags=re.DOTALL)
        
        return content

    def _remove_duplicate_methods(self, content: str) -> str:
        """移除重複的 getter/setter 方法"""
        # 移除版本號相關方法
        content = re.sub(r'\s*public\s+Long\s+getVersion\(\)[^}]*}\s*', '', content, flags=re.DOTALL)
        content = re.sub(r'\s*public\s+void\s+setVersion\([^}]*}\s*', '', content, flags=re.DOTALL)
        
        # 移除時間戳記相關方法
        content = re.sub(r'\s*public\s+LocalDateTime\s+getCreatedAt\(\)[^}]*}\s*', '', content, flags=re.DOTALL)
        content = re.sub(r'\s*public\s+void\s+setCreatedAt\([^}]*}\s*', '', content, flags=re.DOTALL)
        content = re.sub(r'\s*public\s+LocalDateTime\s+getUpdatedAt\(\)[^}]*}\s*', '', content, flags=re.DOTALL)
        content = re.sub(r'\s*public\s+void\s+setUpdatedAt\([^}]*}\s*', '', content, flags=re.DOTALL)
        
        return content

    def _show_diff(self, original: str, migrated: str):
        """顯示變更差異"""
        print("\n" + "="*50)
        print("變更預覽:")
        print("="*50)
        
        original_lines = original.split('\n')
        migrated_lines = migrated.split('\n')
        
        # 簡單的差異顯示
        for i, (orig, new) in enumerate(zip(original_lines, migrated_lines)):
            if orig != new:
                print(f"Line {i+1}:")
                print(f"  - {orig}")
                print(f"  + {new}")
        
        print("="*50 + "\n")

    def generate_database_migration_script(self, entities: List[Dict], output_path: str = None):
        """生成數據庫遷移腳本"""
        if not output_path:
            output_path = "scripts/database-migration-optimistic-locking.sql"
        
        script_content = """-- Aurora 樂觀鎖數據庫遷移腳本
-- 建立日期: 2025年9月24日 下午2:34 (台北時間)
-- 需求: 1.1 - 並發控制機制全面重構

-- 為所有實體表添加樂觀鎖支援

"""
        
        for entity in entities:
            if entity['needs_migration']:
                table_name = entity['table_name']
                script_content += f"""
-- 遷移表: {table_name} (實體: {entity['class_name']})
ALTER TABLE {table_name} 
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- 初始化現有記錄
UPDATE {table_name} SET version = 0 WHERE version IS NULL;
UPDATE {table_name} SET 
    created_at = COALESCE(created_at, CURRENT_TIMESTAMP),
    updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP)
WHERE created_at IS NULL OR updated_at IS NULL;

-- 創建更新觸發器
CREATE TRIGGER IF NOT EXISTS update_{table_name}_updated_at 
    BEFORE UPDATE ON {table_name} 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

"""
        
        script_content += """
-- 創建通用的 updated_at 更新函數（如果不存在）
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 為版本號欄位添加索引（提升樂觀鎖性能）
"""
        
        for entity in entities:
            if entity['needs_migration']:
                table_name = entity['table_name']
                script_content += f"CREATE INDEX IF NOT EXISTS idx_{table_name}_version ON {table_name}(version);\n"
        
        # 寫入腳本文件
        os.makedirs(os.path.dirname(output_path), exist_ok=True)
        with open(output_path, 'w', encoding='utf-8') as f:
            f.write(script_content)
        
        logger.info(f"數據庫遷移腳本已生成: {output_path}")

def main():
    parser = argparse.ArgumentParser(description='JPA 實體樂觀鎖遷移工具')
    parser.add_argument('--entity-path', help='單個實體文件路徑')
    parser.add_argument('--batch', action='store_true', help='批量遷移模式')
    parser.add_argument('--priority', choices=['high', 'medium', 'low'], help='批量遷移的優先級')
    parser.add_argument('--dry-run', action='store_true', help='預覽模式，不實際修改文件')
    parser.add_argument('--generate-sql', action='store_true', help='生成數據庫遷移腳本')
    
    args = parser.parse_args()
    
    migrator = EntityMigrator()
    
    if args.entity_path:
        # 單個文件遷移
        entity_path = Path(args.entity_path)
        if not entity_path.exists():
            logger.error(f"文件不存在: {entity_path}")
            sys.exit(1)
        
        migrator.migrate_entity_file(entity_path, dry_run=args.dry_run)
        
    elif args.batch and args.priority:
        # 批量遷移
        entity_files = migrator.find_entities_by_priority(args.priority)
        
        if not entity_files:
            logger.warning(f"未找到優先級為 {args.priority} 的實體文件")
            return
        
        logger.info(f"找到 {len(entity_files)} 個 {args.priority} 優先級的實體文件")
        
        migrated_entities = []
        for entity_file in entity_files:
            analysis = migrator.analyze_entity_file(entity_file)
            migrated_entities.append(analysis)
            
            if analysis['needs_migration']:
                migrator.migrate_entity_file(entity_file, dry_run=args.dry_run)
        
        # 生成數據庫遷移腳本
        if args.generate_sql:
            migrator.generate_database_migration_script(migrated_entities)
            
    else:
        parser.print_help()
        sys.exit(1)

if __name__ == '__main__':
    main()