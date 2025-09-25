#!/usr/bin/env python3
"""
Comprehensive Migration Workflow

This script provides a complete workflow for migrating from Chinese-first to English-first
documentation system with proper validation, rollback capabilities, and progress tracking.
"""

import os
import sys
import json
import logging
import argparse
from pathlib import Path
from typing import Dict, List, Optional
from datetime import datetime
import shutil

# Import our migration modules
from migrate_chinese_docs import ChineseContentDetector, ChineseDocumentationMigrator
from file_manager import FileManager

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class MigrationWorkflow:
    """
    Comprehensive migration workflow manager.
    
    This class orchestrates the complete migration process from Chinese-first to
    English-first documentation system with proper validation and rollback support.
    """
    
    def __init__(self, config_file: Optional[str] = None):
        """
        Initialize the migration workflow.
        
        Args:
            config_file: Optional configuration file path
        """
        self.config = self._load_config(config_file)
        self.detector = ChineseContentDetector()
        self.migrator = ChineseDocumentationMigrator(self.config)
        self.file_manager = FileManager()
        
        self.workflow_log = []
        self.rollback_info = []
        
        # Workflow phases
        self.phases = [
            'analysis',
            'validation',
            'backup',
            'migration',
            'verification',
            'cleanup'
        ]
        self.current_phase = None
        
        # Statistics
        self.stats = {
            'total_files_found': 0,
            'chinese_files_identified': 0,
            'files_migrated': 0,
            'migration_failures': 0,
            'rollbacks_performed': 0
        }
    
    def _load_config(self, config_file: Optional[str] = None) -> Dict:
        """Load workflow configuration."""
        default_config = {
            'target_directory': '.',
            'backup_directory': '.migration-backup',
            'create_full_backup': True,
            'validate_before_migration': True,
            'auto_rollback_on_failure': True,
            'rollback_threshold': 0.1,  # Rollback if >10% of migrations fail
            'exclude_patterns': [
                '**/*.zh-TW.md',
                'node_modules/**',
                '.git/**',
                '.kiro/**',
                'build/**',
                'target/**'
            ],
            'dry_run': False,
            'verbose_logging': False
        }
        
        if config_file and os.path.exists(config_file):
            try:
                with open(config_file, 'r', encoding='utf-8') as f:
                    user_config = json.load(f)
                default_config.update(user_config)
                logger.info(f"Loaded configuration from {config_file}")
            except Exception as e:
                logger.warning(f"Failed to load config file {config_file}: {e}")
        
        return default_config
    
    def run_workflow(self) -> Dict:
        """
        Run the complete migration workflow.
        
        Returns:
            Workflow results dictionary
        """
        try:
            logger.info("Starting comprehensive migration workflow")
            
            # Phase 1: Analysis
            self._set_phase('analysis')
            analysis_results = self._run_analysis_phase()
            
            if not analysis_results['chinese_files']:
                logger.info("No Chinese files found. Migration not needed.")
                return self._get_workflow_results()
            
            # Phase 2: Validation
            self._set_phase('validation')
            validation_results = self._run_validation_phase(analysis_results)
            
            if not validation_results['can_proceed']:
                logger.error("Validation failed. Cannot proceed with migration.")
                return self._get_workflow_results()
            
            # Phase 3: Backup
            self._set_phase('backup')
            backup_results = self._run_backup_phase()
            
            if not backup_results['success']:
                logger.error("Backup failed. Cannot proceed with migration.")
                return self._get_workflow_results()
            
            # Phase 4: Migration
            self._set_phase('migration')
            migration_results = self._run_migration_phase(analysis_results['chinese_files'])
            
            # Check if rollback is needed
            failure_rate = (migration_results['failed'] / migration_results['total']) if migration_results['total'] > 0 else 0
            if failure_rate > self.config['rollback_threshold'] and self.config['auto_rollback_on_failure']:
                logger.warning(f"Failure rate {failure_rate:.1%} exceeds threshold. Initiating rollback.")
                self._run_rollback()
                return self._get_workflow_results()
            
            # Phase 5: Verification
            self._set_phase('verification')
            verification_results = self._run_verification_phase()
            
            # Phase 6: Cleanup
            self._set_phase('cleanup')
            cleanup_results = self._run_cleanup_phase()
            
            logger.info("Migration workflow completed successfully")
            return self._get_workflow_results()
            
        except Exception as e:
            logger.error(f"Workflow failed: {e}")
            if self.config['auto_rollback_on_failure']:
                self._run_rollback()
            raise
    
    def _set_phase(self, phase: str):
        """Set the current workflow phase."""
        self.current_phase = phase
        logger.info(f"Starting phase: {phase}")
        
        self.workflow_log.append({
            'phase': phase,
            'start_time': datetime.now().isoformat(),
            'status': 'started'
        })
    
    def _run_analysis_phase(self) -> Dict:
        """Run the analysis phase."""
        logger.info("Analyzing directory for Chinese content...")
        
        chinese_files = self.detector.scan_directory(
            self.config['target_directory'],
            self.config['exclude_patterns']
        )
        
        # Generate detailed report
        report = self.detector.generate_report(chinese_files)
        
        # Update statistics
        self.stats['total_files_found'] = self.detector.stats['files_scanned']
        self.stats['chinese_files_identified'] = len(chinese_files)
        
        # Save analysis report
        report_file = 'migration-analysis-report.json'
        with open(report_file, 'w', encoding='utf-8') as f:
            json.dump(report, f, indent=2, ensure_ascii=False)
        
        logger.info(f"Analysis complete. Found {len(chinese_files)} Chinese files.")
        logger.info(f"Analysis report saved to: {report_file}")
        
        return {
            'chinese_files': chinese_files,
            'report': report,
            'report_file': report_file
        }
    
    def _run_validation_phase(self, analysis_results: Dict) -> Dict:
        """Run the validation phase."""
        logger.info("Validating migration prerequisites...")
        
        validation_results = {
            'can_proceed': True,
            'issues': [],
            'warnings': []
        }
        
        # Check disk space
        if not self._check_disk_space():
            validation_results['can_proceed'] = False
            validation_results['issues'].append("Insufficient disk space for migration")
        
        # Check write permissions
        if not self._check_write_permissions():
            validation_results['can_proceed'] = False
            validation_results['issues'].append("Insufficient write permissions")
        
        # Check for conflicting files
        conflicts = self._check_file_conflicts(analysis_results['chinese_files'])
        if conflicts:
            validation_results['warnings'].extend(conflicts)
        
        # Validate file integrity
        integrity_issues = self._validate_file_integrity(analysis_results['chinese_files'])
        if integrity_issues:
            validation_results['issues'].extend(integrity_issues)
            validation_results['can_proceed'] = False
        
        if validation_results['issues']:
            logger.error("Validation issues found:")
            for issue in validation_results['issues']:
                logger.error(f"  - {issue}")
        
        if validation_results['warnings']:
            logger.warning("Validation warnings:")
            for warning in validation_results['warnings']:
                logger.warning(f"  - {warning}")
        
        return validation_results
    
    def _run_backup_phase(self) -> Dict:
        """Run the backup phase."""
        logger.info("Creating backup before migration...")
        
        try:
            backup_dir = Path(self.config['backup_directory'])
            backup_dir.mkdir(exist_ok=True)
            
            if self.config['create_full_backup']:
                # Create full directory backup
                timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
                full_backup_path = backup_dir / f"full_backup_{timestamp}"
                
                if not self.config['dry_run']:
                    shutil.copytree(
                        self.config['target_directory'],
                        full_backup_path,
                        ignore=shutil.ignore_patterns('*.zh-TW.md', '.git', 'node_modules')
                    )
                
                self.rollback_info.append({
                    'type': 'full_backup',
                    'path': str(full_backup_path),
                    'timestamp': datetime.now().isoformat()
                })
                
                logger.info(f"Full backup created: {full_backup_path}")
            
            return {'success': True, 'backup_path': str(backup_dir)}
            
        except Exception as e:
            logger.error(f"Backup failed: {e}")
            return {'success': False, 'error': str(e)}
    
    def _run_migration_phase(self, chinese_files: List[Dict]) -> Dict:
        """Run the migration phase."""
        logger.info(f"Starting migration of {len(chinese_files)} files...")
        
        migration_results = {
            'total': len(chinese_files),
            'successful': 0,
            'failed': 0,
            'skipped': 0
        }
        
        for i, file_info in enumerate(chinese_files, 1):
            file_path = file_info['file_path']
            
            logger.info(f"Migrating file {i}/{len(chinese_files)}: {file_path}")
            
            try:
                success = self.migrator.migrate_file(file_path, self.config['dry_run'])
                
                if success:
                    migration_results['successful'] += 1
                    self.stats['files_migrated'] += 1
                else:
                    migration_results['failed'] += 1
                    self.stats['migration_failures'] += 1
                    
            except Exception as e:
                logger.error(f"Migration failed for {file_path}: {e}")
                migration_results['failed'] += 1
                self.stats['migration_failures'] += 1
        
        logger.info(f"Migration phase complete: {migration_results['successful']} successful, {migration_results['failed']} failed")
        
        return migration_results
    
    def _run_verification_phase(self) -> Dict:
        """Run the verification phase."""
        logger.info("Verifying migration results...")
        
        verification_results = {
            'files_verified': 0,
            'verification_passed': 0,
            'verification_failed': 0,
            'issues': []
        }
        
        # Verify that English files exist and are valid
        for record in self.migrator.migration_log:
            if record['status'] == 'success':
                english_file = record['english_file']
                chinese_file = record['chinese_file']
                
                verification_results['files_verified'] += 1
                
                # Check if both files exist
                if os.path.exists(english_file) and os.path.exists(chinese_file):
                    verification_results['verification_passed'] += 1
                else:
                    verification_results['verification_failed'] += 1
                    verification_results['issues'].append(f"Missing files for {english_file}")
        
        logger.info(f"Verification complete: {verification_results['verification_passed']} passed, {verification_results['verification_failed']} failed")
        
        return verification_results
    
    def _run_cleanup_phase(self) -> Dict:
        """Run the cleanup phase."""
        logger.info("Running cleanup...")
        
        cleanup_results = {
            'old_backups_removed': 0,
            'temp_files_removed': 0
        }
        
        try:
            # Clean up old backups
            if hasattr(self.file_manager, 'cleanup_backups'):
                removed = self.file_manager.cleanup_backups(self.config['target_directory'])
                cleanup_results['old_backups_removed'] = removed
            
            # Remove temporary files
            temp_files = [
                'migration-analysis-report.json',
                'migration.log',
                'translation.log'
            ]
            
            for temp_file in temp_files:
                if os.path.exists(temp_file):
                    try:
                        # Move to backup directory instead of deleting
                        backup_path = Path(self.config['backup_directory']) / temp_file
                        shutil.move(temp_file, backup_path)
                        cleanup_results['temp_files_removed'] += 1
                    except Exception as e:
                        logger.warning(f"Failed to move temp file {temp_file}: {e}")
            
            logger.info("Cleanup complete")
            
        except Exception as e:
            logger.warning(f"Cleanup encountered issues: {e}")
        
        return cleanup_results
    
    def _run_rollback(self):
        """Run rollback procedure."""
        logger.info("Initiating rollback procedure...")
        
        try:
            for rollback_item in reversed(self.rollback_info):
                if rollback_item['type'] == 'full_backup':
                    backup_path = rollback_item['path']
                    if os.path.exists(backup_path):
                        # Restore from full backup
                        logger.info(f"Restoring from backup: {backup_path}")
                        
                        # Remove current directory contents (except .git)
                        for item in Path(self.config['target_directory']).iterdir():
                            if item.name != '.git':
                                if item.is_dir():
                                    shutil.rmtree(item)
                                else:
                                    item.unlink()
                        
                        # Restore from backup
                        for item in Path(backup_path).iterdir():
                            if item.is_dir():
                                shutil.copytree(item, Path(self.config['target_directory']) / item.name)
                            else:
                                shutil.copy2(item, self.config['target_directory'])
                        
                        self.stats['rollbacks_performed'] += 1
                        logger.info("Rollback completed successfully")
                        break
            
        except Exception as e:
            logger.error(f"Rollback failed: {e}")
            raise
    
    def _check_disk_space(self) -> bool:
        """Check if there's sufficient disk space."""
        try:
            import shutil
            total, used, free = shutil.disk_usage(self.config['target_directory'])
            
            # Require at least 1GB free space
            required_space = 1024 * 1024 * 1024  # 1GB
            
            if free < required_space:
                logger.error(f"Insufficient disk space. Required: {required_space}, Available: {free}")
                return False
            
            return True
            
        except Exception as e:
            logger.warning(f"Could not check disk space: {e}")
            return True  # Assume OK if we can't check
    
    def _check_write_permissions(self) -> bool:
        """Check write permissions for target directory."""
        try:
            test_file = Path(self.config['target_directory']) / '.migration_test'
            test_file.write_text('test')
            test_file.unlink()
            return True
        except Exception:
            return False
    
    def _check_file_conflicts(self, chinese_files: List[Dict]) -> List[str]:
        """Check for potential file conflicts."""
        conflicts = []
        
        for file_info in chinese_files:
            file_path = file_info['file_path']
            chinese_path = self.file_manager.get_chinese_file_path(file_path)
            
            if os.path.exists(chinese_path):
                conflicts.append(f"Chinese file already exists: {chinese_path}")
        
        return conflicts
    
    def _validate_file_integrity(self, chinese_files: List[Dict]) -> List[str]:
        """Validate file integrity before migration."""
        issues = []
        
        for file_info in chinese_files:
            file_path = file_info['file_path']
            
            try:
                # Check if file is readable
                with open(file_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                # Basic validation
                if not content.strip():
                    issues.append(f"Empty file: {file_path}")
                
            except Exception as e:
                issues.append(f"Cannot read file {file_path}: {e}")
        
        return issues
    
    def _get_workflow_results(self) -> Dict:
        """Get comprehensive workflow results."""
        return {
            'workflow_status': 'completed' if self.current_phase == 'cleanup' else 'incomplete',
            'current_phase': self.current_phase,
            'statistics': self.stats.copy(),
            'workflow_log': self.workflow_log.copy(),
            'rollback_info': self.rollback_info.copy(),
            'timestamp': datetime.now().isoformat()
        }
    
    def print_summary(self):
        """Print workflow summary."""
        print("\n" + "="*70)
        print("MIGRATION WORKFLOW SUMMARY")
        print("="*70)
        print(f"Workflow Status: {self.current_phase or 'Not started'}")
        print(f"Total Files Found: {self.stats['total_files_found']}")
        print(f"Chinese Files Identified: {self.stats['chinese_files_identified']}")
        print(f"Files Migrated: {self.stats['files_migrated']}")
        print(f"Migration Failures: {self.stats['migration_failures']}")
        print(f"Rollbacks Performed: {self.stats['rollbacks_performed']}")
        
        if self.workflow_log:
            print(f"\nWorkflow Phases:")
            for log_entry in self.workflow_log:
                print(f"  - {log_entry['phase']}: {log_entry.get('status', 'unknown')}")
        
        print("="*70)


def main():
    """Main function for command-line usage."""
    parser = argparse.ArgumentParser(
        description='Run comprehensive migration workflow',
        formatter_class=argparse.RawDescriptionHelpFormatter
    )
    
    parser.add_argument('--config', metavar='CONFIG',
                       help='Configuration file path')
    parser.add_argument('--directory', metavar='DIR', default='.',
                       help='Target directory (default: current directory)')
    parser.add_argument('--dry-run', action='store_true',
                       help='Simulate migration without making changes')
    parser.add_argument('--verbose', '-v', action='store_true',
                       help='Verbose output')
    parser.add_argument('--quiet', '-q', action='store_true',
                       help='Quiet output (errors only)')
    
    args = parser.parse_args()
    
    # Configure logging level
    if args.quiet:
        logging.getLogger().setLevel(logging.ERROR)
    elif args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)
    
    try:
        # Create workflow
        workflow = MigrationWorkflow(args.config)
        
        # Override configuration with command line arguments
        if args.directory:
            workflow.config['target_directory'] = args.directory
        if args.dry_run:
            workflow.config['dry_run'] = True
            print("DRY RUN MODE - No files will be modified")
        
        # Run workflow
        results = workflow.run_workflow()
        
        # Print summary
        workflow.print_summary()
        
        # Save results
        results_file = 'migration-workflow-results.json'
        with open(results_file, 'w', encoding='utf-8') as f:
            json.dump(results, f, indent=2, ensure_ascii=False)
        
        print(f"\nWorkflow results saved to: {results_file}")
        
        # Return appropriate exit code
        return 0 if workflow.stats['migration_failures'] == 0 else 1
        
    except KeyboardInterrupt:
        print("\nWorkflow interrupted by user")
        return 1
    except Exception as e:
        print(f"Workflow failed: {e}")
        if args.verbose:
            import traceback
            traceback.print_exc()
        return 1


if __name__ == '__main__':
    exit(main())