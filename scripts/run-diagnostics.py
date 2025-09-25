#!/usr/bin/env python3
"""
System diagnostics script for the Automated Documentation Translation System.

This script performs comprehensive system checks, performance tests,
and generates diagnostic reports for troubleshooting.
"""

import os
import sys
import json
import time
import psutil
import platform
import subprocess
import tempfile
from pathlib import Path
from typing import Dict, List, Any, Optional
import logging

# Add scripts directory to path for imports
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

try:
    from kiro_translator import KiroTranslator
    from batch_processor import BatchProcessor
    from quality_assurance import QualityAssurance
    from config.translation_config import TranslationConfig
except ImportError as e:
    print(f"Warning: Could not import translation modules: {e}")

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(levelname)s: %(message)s')
logger = logging.getLogger(__name__)

class SystemDiagnostics:
    """Comprehensive system diagnostics for the translation system."""
    
    def __init__(self, workspace_root: str = None):
        """
        Initialize diagnostics.
        
        Args:
            workspace_root: Root directory of the workspace
        """
        self.workspace_root = Path(workspace_root or os.getcwd())
        self.results = {
            'timestamp': time.strftime('%Y-%m-%d %H:%M:%S'),
            'system_info': {},
            'environment_check': {},
            'configuration_check': {},
            'dependency_check': {},
            'performance_test': {},
            'functionality_test': {},
            'file_system_check': {},
            'recommendations': []
        }
    
    def collect_system_info(self) -> Dict[str, Any]:
        """Collect basic system information."""
        logger.info("Collecting system information...")
        
        try:
            info = {
                'platform': platform.platform(),
                'system': platform.system(),
                'release': platform.release(),
                'version': platform.version(),
                'machine': platform.machine(),
                'processor': platform.processor(),
                'python_version': sys.version,
                'python_executable': sys.executable,
                'cpu_count': psutil.cpu_count(),
                'memory_total': psutil.virtual_memory().total,
                'memory_available': psutil.virtual_memory().available,
                'disk_usage': {}
            }
            
            # Disk usage for workspace
            try:
                disk_usage = psutil.disk_usage(self.workspace_root)
                info['disk_usage'] = {
                    'total': disk_usage.total,
                    'used': disk_usage.used,
                    'free': disk_usage.free,
                    'percent': (disk_usage.used / disk_usage.total) * 100
                }
            except Exception as e:
                info['disk_usage'] = {'error': str(e)}
            
            self.results['system_info'] = info
            logger.info("✓ System information collected")
            return info
            
        except Exception as e:
            logger.error(f"Failed to collect system info: {e}")
            return {'error': str(e)}
    
    def check_environment(self) -> Dict[str, Any]:
        """Check environment variables and configuration."""
        logger.info("Checking environment...")
        
        env_check = {
            'environment_variables': {},
            'path_check': {},
            'permissions': {}
        }
        
        # Check important environment variables
        important_vars = [
            'OPENAI_API_KEY',
            'GOOGLE_APPLICATION_CREDENTIALS',
            'AZURE_TRANSLATOR_KEY',
            'TRANSLATION_SERVICE',
            'TARGET_LANGUAGE',
            'LOG_LEVEL'
        ]
        
        for var in important_vars:
            value = os.getenv(var)
            if value:
                # Mask sensitive values
                if 'key' in var.lower() or 'secret' in var.lower():
                    env_check['environment_variables'][var] = f"{'*' * (len(value) - 4)}{value[-4:]}" if len(value) > 4 else "***"
                else:
                    env_check['environment_variables'][var] = value
            else:
                env_check['environment_variables'][var] = None
        
        # Check Python path
        env_check['path_check'] = {
            'python_path': sys.path[:5],  # First 5 entries
            'workspace_in_path': str(self.workspace_root) in sys.path
        }
        
        # Check file permissions
        try:
            test_dirs = ['scripts', 'config', 'logs', 'backups']
            for test_dir in test_dirs:
                dir_path = self.workspace_root / test_dir
                env_check['permissions'][test_dir] = {
                    'exists': dir_path.exists(),
                    'readable': os.access(dir_path, os.R_OK) if dir_path.exists() else False,
                    'writable': os.access(dir_path, os.W_OK) if dir_path.exists() else False
                }
        except Exception as e:
            env_check['permissions']['error'] = str(e)
        
        self.results['environment_check'] = env_check
        logger.info("✓ Environment check completed")
        return env_check
    
    def check_configuration(self) -> Dict[str, Any]:
        """Check configuration files and settings."""
        logger.info("Checking configuration...")
        
        config_check = {
            'config_files': {},
            'config_validation': {},
            'terminology_check': {}
        }
        
        # Check main configuration file
        config_file = self.workspace_root / 'config' / 'translation-config.json'
        if config_file.exists():
            try:
                with open(config_file, 'r', encoding='utf-8') as f:
                    config_data = json.load(f)
                
                config_check['config_files']['main_config'] = {
                    'exists': True,
                    'valid_json': True,
                    'size': config_file.stat().st_size,
                    'sections': list(config_data.keys())
                }
                
                # Validate configuration structure
                required_sections = ['translation_service', 'processing', 'quality_assurance', 'file_management']
                missing_sections = [s for s in required_sections if s not in config_data]
                config_check['config_validation']['missing_sections'] = missing_sections
                config_check['config_validation']['valid_structure'] = len(missing_sections) == 0
                
            except json.JSONDecodeError as e:
                config_check['config_files']['main_config'] = {
                    'exists': True,
                    'valid_json': False,
                    'error': str(e)
                }
            except Exception as e:
                config_check['config_files']['main_config'] = {
                    'exists': True,
                    'error': str(e)
                }
        else:
            config_check['config_files']['main_config'] = {'exists': False}
        
        # Check terminology file
        terminology_file = self.workspace_root / 'config' / 'terminology.json'
        if terminology_file.exists():
            try:
                with open(terminology_file, 'r', encoding='utf-8') as f:
                    terminology_data = json.load(f)
                
                config_check['terminology_check'] = {
                    'exists': True,
                    'valid_json': True,
                    'preserve_terms_count': len(terminology_data.get('preserve_terms', [])),
                    'translation_overrides_count': len(terminology_data.get('translation_overrides', {}))
                }
            except Exception as e:
                config_check['terminology_check'] = {
                    'exists': True,
                    'error': str(e)
                }
        else:
            config_check['terminology_check'] = {'exists': False}
        
        # Check environment file
        env_file = self.workspace_root / '.env'
        config_check['config_files']['env_file'] = {
            'exists': env_file.exists(),
            'template_exists': (self.workspace_root / '.env.template').exists()
        }
        
        self.results['configuration_check'] = config_check
        logger.info("✓ Configuration check completed")
        return config_check
    
    def check_dependencies(self) -> Dict[str, Any]:
        """Check Python dependencies and imports."""
        logger.info("Checking dependencies...")
        
        dependency_check = {
            'required_modules': {},
            'optional_modules': {},
            'version_info': {}
        }
        
        # Required modules
        required_modules = [
            'json', 'os', 'sys', 'pathlib', 'logging', 'time',
            'subprocess', 'argparse', 'tempfile', 'shutil'
        ]
        
        # Optional modules (for translation services)
        optional_modules = [
            'openai', 'google.cloud.translate', 'requests',
            'watchdog', 'yaml', 'markdown', 'beautifulsoup4',
            'jinja2', 'click', 'tqdm', 'python-dotenv'
        ]
        
        # Check required modules
        for module in required_modules:
            try:
                __import__(module)
                dependency_check['required_modules'][module] = {'available': True}
            except ImportError as e:
                dependency_check['required_modules'][module] = {
                    'available': False,
                    'error': str(e)
                }
        
        # Check optional modules
        for module in optional_modules:
            try:
                imported_module = __import__(module)
                version = getattr(imported_module, '__version__', 'unknown')
                dependency_check['optional_modules'][module] = {
                    'available': True,
                    'version': version
                }
            except ImportError as e:
                dependency_check['optional_modules'][module] = {
                    'available': False,
                    'error': str(e)
                }
        
        # Check translation system modules
        translation_modules = [
            'kiro_translator', 'batch_processor', 'quality_assurance',
            'translate_docs', 'migration_workflow', 'file_manager'
        ]
        
        dependency_check['translation_modules'] = {}
        for module in translation_modules:
            try:
                sys.path.insert(0, str(self.workspace_root / 'scripts'))
                __import__(module)
                dependency_check['translation_modules'][module] = {'available': True}
            except ImportError as e:
                dependency_check['translation_modules'][module] = {
                    'available': False,
                    'error': str(e)
                }
        
        self.results['dependency_check'] = dependency_check
        logger.info("✓ Dependency check completed")
        return dependency_check
    
    def run_performance_test(self) -> Dict[str, Any]:
        """Run basic performance tests."""
        logger.info("Running performance tests...")
        
        performance_test = {
            'system_performance': {},
            'translation_performance': {},
            'file_io_performance': {}
        }
        
        # System performance
        try:
            # CPU test
            start_time = time.time()
            result = sum(i * i for i in range(100000))
            cpu_time = time.time() - start_time
            
            performance_test['system_performance'] = {
                'cpu_test_time': cpu_time,
                'cpu_usage_percent': psutil.cpu_percent(interval=1),
                'memory_usage_percent': psutil.virtual_memory().percent,
                'load_average': os.getloadavg() if hasattr(os, 'getloadavg') else None
            }
        except Exception as e:
            performance_test['system_performance'] = {'error': str(e)}
        
        # File I/O performance test
        try:
            test_data = "# Test Document\n\nThis is a test document for performance testing.\n" * 100
            
            with tempfile.NamedTemporaryFile(mode='w', delete=False, suffix='.md') as f:
                temp_file = f.name
                
                # Write test
                start_time = time.time()
                f.write(test_data)
                f.flush()
                write_time = time.time() - start_time
                
            # Read test
            start_time = time.time()
            with open(temp_file, 'r', encoding='utf-8') as f:
                content = f.read()
            read_time = time.time() - start_time
            
            # Cleanup
            os.unlink(temp_file)
            
            performance_test['file_io_performance'] = {
                'write_time': write_time,
                'read_time': read_time,
                'data_size': len(test_data),
                'write_speed_mb_per_sec': (len(test_data) / 1024 / 1024) / write_time if write_time > 0 else 0,
                'read_speed_mb_per_sec': (len(test_data) / 1024 / 1024) / read_time if read_time > 0 else 0
            }
        except Exception as e:
            performance_test['file_io_performance'] = {'error': str(e)}
        
        # Translation performance test (if modules available)
        try:
            from config.translation_config import TranslationConfig
            config = TranslationConfig()
            
            # Test configuration loading time
            start_time = time.time()
            for _ in range(10):
                test_config = TranslationConfig()
            config_load_time = (time.time() - start_time) / 10
            
            performance_test['translation_performance'] = {
                'config_load_time_avg': config_load_time,
                'config_available': True
            }
        except Exception as e:
            performance_test['translation_performance'] = {
                'config_available': False,
                'error': str(e)
            }
        
        self.results['performance_test'] = performance_test
        logger.info("✓ Performance tests completed")
        return performance_test
    
    def test_functionality(self) -> Dict[str, Any]:
        """Test basic functionality of translation system."""
        logger.info("Testing functionality...")
        
        functionality_test = {
            'cli_test': {},
            'config_test': {},
            'import_test': {}
        }
        
        # Test CLI availability
        try:
            cli_script = self.workspace_root / 'scripts' / 'translation-cli.py'
            if cli_script.exists():
                result = subprocess.run([
                    sys.executable, str(cli_script), '--help'
                ], capture_output=True, text=True, timeout=10)
                
                functionality_test['cli_test'] = {
                    'script_exists': True,
                    'help_works': result.returncode == 0,
                    'output_length': len(result.stdout) if result.stdout else 0
                }
            else:
                functionality_test['cli_test'] = {'script_exists': False}
        except subprocess.TimeoutExpired:
            functionality_test['cli_test'] = {
                'script_exists': True,
                'help_works': False,
                'error': 'Timeout'
            }
        except Exception as e:
            functionality_test['cli_test'] = {
                'script_exists': True,
                'error': str(e)
            }
        
        # Test configuration loading
        try:
            sys.path.insert(0, str(self.workspace_root))
            from config.translation_config import TranslationConfig
            
            config = TranslationConfig()
            functionality_test['config_test'] = {
                'config_loads': True,
                'has_translation_service': 'translation_service' in config.to_dict(),
                'has_processing': 'processing' in config.to_dict()
            }
        except Exception as e:
            functionality_test['config_test'] = {
                'config_loads': False,
                'error': str(e)
            }
        
        # Test module imports
        test_imports = [
            'kiro_translator.KiroTranslator',
            'batch_processor.BatchProcessor',
            'quality_assurance.QualityAssurance'
        ]
        
        functionality_test['import_test'] = {}
        for import_path in test_imports:
            module_name, class_name = import_path.split('.')
            try:
                sys.path.insert(0, str(self.workspace_root / 'scripts'))
                module = __import__(module_name)
                cls = getattr(module, class_name)
                functionality_test['import_test'][import_path] = {'available': True}
            except Exception as e:
                functionality_test['import_test'][import_path] = {
                    'available': False,
                    'error': str(e)
                }
        
        self.results['functionality_test'] = functionality_test
        logger.info("✓ Functionality tests completed")
        return functionality_test
    
    def check_file_system(self) -> Dict[str, Any]:
        """Check file system structure and integrity."""
        logger.info("Checking file system...")
        
        file_system_check = {
            'directory_structure': {},
            'required_files': {},
            'file_sizes': {},
            'permissions': {}
        }
        
        # Check directory structure
        required_dirs = [
            'scripts', 'config', 'logs', 'backups', 'temp',
            'tests', 'docs/en', '.kiro/hooks'
        ]
        
        for directory in required_dirs:
            dir_path = self.workspace_root / directory
            file_system_check['directory_structure'][directory] = {
                'exists': dir_path.exists(),
                'is_directory': dir_path.is_dir() if dir_path.exists() else False,
                'readable': os.access(dir_path, os.R_OK) if dir_path.exists() else False,
                'writable': os.access(dir_path, os.W_OK) if dir_path.exists() else False
            }
        
        # Check required files
        required_files = [
            'scripts/translation-cli.py',
            'scripts/kiro_translator.py',
            'scripts/batch_processor.py',
            'config/translation-config.json',
            'requirements.txt'
        ]
        
        for file_path in required_files:
            full_path = self.workspace_root / file_path
            if full_path.exists():
                stat = full_path.stat()
                file_system_check['required_files'][file_path] = {
                    'exists': True,
                    'size': stat.st_size,
                    'modified': stat.st_mtime,
                    'readable': os.access(full_path, os.R_OK),
                    'executable': os.access(full_path, os.X_OK)
                }
            else:
                file_system_check['required_files'][file_path] = {'exists': False}
        
        # Check log files
        logs_dir = self.workspace_root / 'logs'
        if logs_dir.exists():
            log_files = list(logs_dir.glob('*.log'))
            file_system_check['file_sizes']['log_files'] = {
                'count': len(log_files),
                'total_size': sum(f.stat().st_size for f in log_files),
                'largest_file': max((f.stat().st_size, f.name) for f in log_files) if log_files else None
            }
        
        self.results['file_system_check'] = file_system_check
        logger.info("✓ File system check completed")
        return file_system_check
    
    def generate_recommendations(self) -> List[str]:
        """Generate recommendations based on diagnostic results."""
        logger.info("Generating recommendations...")
        
        recommendations = []
        
        # Check system resources
        if 'system_info' in self.results:
            memory_gb = self.results['system_info'].get('memory_total', 0) / (1024**3)
            if memory_gb < 4:
                recommendations.append("Consider upgrading to at least 4GB RAM for better performance")
            
            disk_usage = self.results['system_info'].get('disk_usage', {})
            if disk_usage.get('percent', 0) > 90:
                recommendations.append("Disk space is critically low - clean up unnecessary files")
            elif disk_usage.get('percent', 0) > 80:
                recommendations.append("Disk space is running low - consider cleanup")
        
        # Check environment variables
        if 'environment_check' in self.results:
            env_vars = self.results['environment_check'].get('environment_variables', {})
            if not env_vars.get('OPENAI_API_KEY'):
                recommendations.append("Set OPENAI_API_KEY environment variable for translation services")
            if not env_vars.get('TRANSLATION_SERVICE'):
                recommendations.append("Set TRANSLATION_SERVICE environment variable to specify translation provider")
        
        # Check configuration
        if 'configuration_check' in self.results:
            config_files = self.results['configuration_check'].get('config_files', {})
            if not config_files.get('main_config', {}).get('exists'):
                recommendations.append("Create main configuration file: config/translation-config.json")
            if not config_files.get('env_file', {}).get('exists'):
                recommendations.append("Create .env file from .env.template for environment variables")
        
        # Check dependencies
        if 'dependency_check' in self.results:
            optional_modules = self.results['dependency_check'].get('optional_modules', {})
            missing_modules = [name for name, info in optional_modules.items() 
                             if not info.get('available', False)]
            if missing_modules:
                recommendations.append(f"Install missing optional modules: {', '.join(missing_modules)}")
        
        # Check performance
        if 'performance_test' in self.results:
            system_perf = self.results['performance_test'].get('system_performance', {})
            if system_perf.get('memory_usage_percent', 0) > 80:
                recommendations.append("High memory usage detected - consider reducing max_workers in configuration")
            if system_perf.get('cpu_usage_percent', 0) > 90:
                recommendations.append("High CPU usage detected - system may be under heavy load")
        
        # Check file system
        if 'file_system_check' in self.results:
            required_files = self.results['file_system_check'].get('required_files', {})
            missing_files = [name for name, info in required_files.items() 
                           if not info.get('exists', False)]
            if missing_files:
                recommendations.append(f"Missing required files: {', '.join(missing_files)}")
        
        # General recommendations
        if not recommendations:
            recommendations.append("System appears to be properly configured")
        
        self.results['recommendations'] = recommendations
        logger.info("✓ Recommendations generated")
        return recommendations
    
    def run_full_diagnostics(self) -> Dict[str, Any]:
        """Run complete diagnostic suite."""
        logger.info("Starting full system diagnostics...")
        
        diagnostic_steps = [
            ("System Information", self.collect_system_info),
            ("Environment Check", self.check_environment),
            ("Configuration Check", self.check_configuration),
            ("Dependency Check", self.check_dependencies),
            ("Performance Test", self.run_performance_test),
            ("Functionality Test", self.test_functionality),
            ("File System Check", self.check_file_system),
            ("Recommendations", self.generate_recommendations)
        ]
        
        for step_name, step_func in diagnostic_steps:
            try:
                logger.info(f"Running: {step_name}")
                step_func()
            except Exception as e:
                logger.error(f"Failed {step_name}: {e}")
                self.results[step_name.lower().replace(' ', '_')] = {'error': str(e)}
        
        logger.info("✅ Full diagnostics completed")
        return self.results
    
    def save_report(self, output_file: str = None) -> str:
        """Save diagnostic report to file."""
        if output_file is None:
            timestamp = time.strftime('%Y%m%d_%H%M%S')
            output_file = f"diagnostic_report_{timestamp}.json"
        
        output_path = self.workspace_root / output_file
        
        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(self.results, f, indent=2, default=str)
        
        logger.info(f"Diagnostic report saved to: {output_path}")
        return str(output_path)
    
    def print_summary(self):
        """Print diagnostic summary to console."""
        print("\n" + "="*60)
        print("🔍 SYSTEM DIAGNOSTICS SUMMARY")
        print("="*60)
        
        # System info
        if 'system_info' in self.results:
            info = self.results['system_info']
            print(f"\n💻 System: {info.get('platform', 'Unknown')}")
            print(f"🐍 Python: {info.get('python_version', 'Unknown').split()[0]}")
            print(f"💾 Memory: {info.get('memory_total', 0) / (1024**3):.1f}GB total, "
                  f"{info.get('memory_available', 0) / (1024**3):.1f}GB available")
            
            disk_usage = info.get('disk_usage', {})
            if 'percent' in disk_usage:
                print(f"💽 Disk: {disk_usage['percent']:.1f}% used "
                      f"({disk_usage.get('free', 0) / (1024**3):.1f}GB free)")
        
        # Configuration status
        if 'configuration_check' in self.results:
            config = self.results['configuration_check']
            main_config = config.get('config_files', {}).get('main_config', {})
            terminology = config.get('terminology_check', {})
            
            print(f"\n⚙️  Configuration:")
            print(f"   Main config: {'✓' if main_config.get('exists') else '✗'}")
            print(f"   Terminology: {'✓' if terminology.get('exists') else '✗'}")
            print(f"   Environment: {'✓' if config.get('config_files', {}).get('env_file', {}).get('exists') else '✗'}")
        
        # Dependencies status
        if 'dependency_check' in self.results:
            deps = self.results['dependency_check']
            optional_modules = deps.get('optional_modules', {})
            available_count = sum(1 for info in optional_modules.values() if info.get('available'))
            total_count = len(optional_modules)
            
            print(f"\n📦 Dependencies: {available_count}/{total_count} optional modules available")
        
        # Performance summary
        if 'performance_test' in self.results:
            perf = self.results['performance_test']
            system_perf = perf.get('system_performance', {})
            
            print(f"\n⚡ Performance:")
            if 'cpu_usage_percent' in system_perf:
                print(f"   CPU usage: {system_perf['cpu_usage_percent']:.1f}%")
            if 'memory_usage_percent' in system_perf:
                print(f"   Memory usage: {system_perf['memory_usage_percent']:.1f}%")
        
        # Recommendations
        if 'recommendations' in self.results:
            recommendations = self.results['recommendations']
            print(f"\n💡 Recommendations ({len(recommendations)}):")
            for i, rec in enumerate(recommendations[:5], 1):  # Show first 5
                print(f"   {i}. {rec}")
            if len(recommendations) > 5:
                print(f"   ... and {len(recommendations) - 5} more")
        
        print("\n" + "="*60)

def main():
    """Main function for diagnostics script."""
    import argparse
    
    parser = argparse.ArgumentParser(
        description="Run system diagnostics for the translation system"
    )
    parser.add_argument(
        '--workspace', '-w',
        help='Workspace root directory (default: current directory)'
    )
    parser.add_argument(
        '--output', '-o',
        help='Output file for diagnostic report'
    )
    parser.add_argument(
        '--verbose', '-v',
        action='store_true',
        help='Enable verbose logging'
    )
    parser.add_argument(
        '--quick', '-q',
        action='store_true',
        help='Run quick diagnostics (skip performance tests)'
    )
    
    args = parser.parse_args()
    
    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)
    
    # Initialize diagnostics
    diagnostics = SystemDiagnostics(args.workspace)
    
    try:
        if args.quick:
            # Quick diagnostics
            diagnostics.collect_system_info()
            diagnostics.check_environment()
            diagnostics.check_configuration()
            diagnostics.check_dependencies()
            diagnostics.generate_recommendations()
        else:
            # Full diagnostics
            diagnostics.run_full_diagnostics()
        
        # Print summary
        diagnostics.print_summary()
        
        # Save report
        if args.output:
            report_path = diagnostics.save_report(args.output)
            print(f"\n📄 Full report saved to: {report_path}")
        
        return 0
        
    except KeyboardInterrupt:
        logger.info("Diagnostics interrupted by user")
        return 1
    except Exception as e:
        logger.error(f"Diagnostics failed: {e}")
        return 1

if __name__ == '__main__':
    sys.exit(main())