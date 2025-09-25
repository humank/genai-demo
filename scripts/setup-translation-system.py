#!/usr/bin/env python3
"""
Setup script for the Automated Documentation Translation System.

This script handles initial system installation, dependency management,
configuration setup, and system validation.
"""

import os
import sys
import json
import subprocess
import shutil
import logging
from pathlib import Path
from typing import List, Dict, Optional, Tuple
import argparse

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class TranslationSystemSetup:
    """Setup manager for the translation system."""
    
    def __init__(self, workspace_root: str = None):
        """
        Initialize the setup manager.
        
        Args:
            workspace_root: Root directory of the workspace (default: current directory)
        """
        self.workspace_root = Path(workspace_root or os.getcwd())
        self.python_executable = sys.executable
        self.requirements_installed = False
        self.config_created = False
        self.directories_created = False
        
    def check_prerequisites(self) -> bool:
        """
        Check system prerequisites.
        
        Returns:
            True if all prerequisites are met
        """
        logger.info("Checking system prerequisites...")
        
        # Check Python version
        if sys.version_info < (3, 8):
            logger.error(f"Python 3.8+ required, found {sys.version}")
            return False
        
        logger.info(f"✓ Python {sys.version.split()[0]} found")
        
        # Check if we're in a Kiro workspace
        kiro_dir = self.workspace_root / '.kiro'
        if not kiro_dir.exists():
            logger.warning("Not in a Kiro workspace - some features may not work")
        else:
            logger.info("✓ Kiro workspace detected")
        
        # Check write permissions
        if not os.access(self.workspace_root, os.W_OK):
            logger.error(f"No write permission in {self.workspace_root}")
            return False
        
        logger.info("✓ Write permissions verified")
        
        # Check available disk space (minimum 1GB)
        try:
            statvfs = os.statvfs(self.workspace_root)
            free_space = statvfs.f_frsize * statvfs.f_bavail
            if free_space < 1024 * 1024 * 1024:  # 1GB
                logger.warning(f"Low disk space: {free_space / 1024 / 1024 / 1024:.1f}GB available")
            else:
                logger.info(f"✓ Disk space: {free_space / 1024 / 1024 / 1024:.1f}GB available")
        except (OSError, AttributeError):
            logger.warning("Could not check disk space")
        
        return True
    
    def create_directory_structure(self) -> bool:
        """
        Create required directory structure.
        
        Returns:
            True if successful
        """
        logger.info("Creating directory structure...")
        
        directories = [
            'scripts',
            'config',
            'logs',
            'backups',
            'temp',
            'tests',
            'docs/en',
            '.kiro/hooks',
            '.kiro/config'
        ]
        
        try:
            for directory in directories:
                dir_path = self.workspace_root / directory
                dir_path.mkdir(parents=True, exist_ok=True)
                logger.debug(f"Created directory: {dir_path}")
            
            logger.info("✓ Directory structure created")
            self.directories_created = True
            return True
            
        except Exception as e:
            logger.error(f"Failed to create directories: {e}")
            return False
    
    def install_dependencies(self, force: bool = False) -> bool:
        """
        Install Python dependencies.
        
        Args:
            force: Force reinstallation of dependencies
            
        Returns:
            True if successful
        """
        logger.info("Installing Python dependencies...")
        
        # Check if requirements.txt exists
        requirements_file = self.workspace_root / 'requirements.txt'
        if not requirements_file.exists():
            logger.info("Creating requirements.txt...")
            self.create_requirements_file()
        
        try:
            # Install dependencies
            cmd = [
                self.python_executable, '-m', 'pip', 'install',
                '-r', str(requirements_file)
            ]
            
            if force:
                cmd.append('--force-reinstall')
            
            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                cwd=self.workspace_root
            )
            
            if result.returncode == 0:
                logger.info("✓ Dependencies installed successfully")
                self.requirements_installed = True
                return True
            else:
                logger.error(f"Failed to install dependencies: {result.stderr}")
                return False
                
        except Exception as e:
            logger.error(f"Error installing dependencies: {e}")
            return False
    
    def create_requirements_file(self):
        """Create requirements.txt file with necessary dependencies."""
        requirements = [
            "openai>=1.0.0",
            "google-cloud-translate>=3.0.0",
            "watchdog>=3.0.0",
            "pyyaml>=6.0",
            "requests>=2.28.0",
            "markdown>=3.4.0",
            "beautifulsoup4>=4.11.0",
            "jinja2>=3.1.0",
            "click>=8.0.0",
            "tqdm>=4.64.0",
            "python-dotenv>=1.0.0",
            "markdownify>=0.11.0",
            "memory-profiler>=0.60.0"
        ]
        
        requirements_file = self.workspace_root / 'requirements.txt'
        with open(requirements_file, 'w', encoding='utf-8') as f:
            f.write('\n'.join(requirements))
        
        logger.info(f"Created {requirements_file}")
    
    def create_default_configuration(self) -> bool:
        """
        Create default configuration files.
        
        Returns:
            True if successful
        """
        logger.info("Creating default configuration...")
        
        try:
            # Main configuration file
            config_file = self.workspace_root / 'config' / 'translation-config.json'
            if not config_file.exists():
                default_config = {
                    "translation_service": {
                        "service_type": "openai",
                        "model": "gpt-3.5-turbo",
                        "max_tokens": 4000,
                        "temperature": 0.3,
                        "rate_limit_per_minute": 60
                    },
                    "processing": {
                        "max_chunk_size": 3000,
                        "max_workers": 4,
                        "enable_caching": True,
                        "backup_before_translation": True,
                        "preserve_formatting": True,
                        "validate_after_translation": True
                    },
                    "quality_assurance": {
                        "enable_terminology_check": True,
                        "enable_format_validation": True,
                        "enable_cross_reference_check": True,
                        "min_confidence_score": 0.7,
                        "terminology_file": "config/terminology.json"
                    },
                    "file_management": {
                        "backup_directory": "backups",
                        "log_directory": "logs",
                        "temp_directory": "temp",
                        "preserve_directory_structure": True,
                        "file_patterns": ["*.md", "*.txt", "*.rst"]
                    }
                }
                
                with open(config_file, 'w', encoding='utf-8') as f:
                    json.dump(default_config, f, indent=2)
                
                logger.info(f"✓ Created {config_file}")
            
            # Terminology configuration
            terminology_file = self.workspace_root / 'config' / 'terminology.json'
            if not terminology_file.exists():
                terminology_config = {
                    "preserve_terms": [
                        "API", "REST", "JSON", "HTTP", "HTTPS",
                        "Docker", "Kubernetes", "Git", "GitHub",
                        "DDD", "TDD", "BDD", "SOLID",
                        "JavaScript", "TypeScript", "Python", "Java",
                        "React", "Angular", "Vue", "Node.js",
                        "AWS", "Azure", "GCP", "CDK", "Terraform"
                    ],
                    "translation_overrides": {
                        "API": "API",
                        "endpoint": "端點",
                        "authentication": "身份驗證",
                        "authorization": "授權",
                        "configuration": "配置",
                        "deployment": "部署",
                        "monitoring": "監控",
                        "logging": "日誌記錄"
                    }
                }
                
                with open(terminology_file, 'w', encoding='utf-8') as f:
                    json.dump(terminology_config, f, indent=2, ensure_ascii=False)
                
                logger.info(f"✓ Created {terminology_file}")
            
            # Environment template
            env_template = self.workspace_root / '.env.template'
            if not env_template.exists():
                env_content = """# Translation System Environment Variables

# OpenAI Configuration
OPENAI_API_KEY=your-openai-api-key-here

# Google Cloud Configuration (optional)
GOOGLE_APPLICATION_CREDENTIALS=path/to/service-account-key.json

# Azure Translator Configuration (optional)
AZURE_TRANSLATOR_KEY=your-azure-key-here
AZURE_TRANSLATOR_ENDPOINT=https://api.cognitive.microsofttranslator.com
AZURE_TRANSLATOR_REGION=your-region

# System Configuration
TRANSLATION_SERVICE=openai
TARGET_LANGUAGE=zh-TW
LOG_LEVEL=INFO

# Performance Settings
MAX_WORKERS=4
RATE_LIMIT_PER_MINUTE=60
"""
                
                with open(env_template, 'w', encoding='utf-8') as f:
                    f.write(env_content)
                
                logger.info(f"✓ Created {env_template}")
            
            self.config_created = True
            return True
            
        except Exception as e:
            logger.error(f"Failed to create configuration: {e}")
            return False
    
    def setup_kiro_hooks(self) -> bool:
        """
        Set up Kiro hooks for automatic translation.
        
        Returns:
            True if successful
        """
        logger.info("Setting up Kiro hooks...")
        
        try:
            hooks_dir = self.workspace_root / '.kiro' / 'hooks'
            hooks_dir.mkdir(parents=True, exist_ok=True)
            
            # Auto-translation hook
            hook_file = hooks_dir / 'auto-translation.kiro.hook'
            if not hook_file.exists():
                hook_config = {
                    "name": "Auto Translation",
                    "description": "Automatically translate markdown files when they change",
                    "when": {
                        "patterns": ["**/*.md"],
                        "exclude_patterns": [
                            "**/*.zh-TW.md",
                            "**/node_modules/**",
                            "**/.git/**",
                            "**/backups/**"
                        ]
                    },
                    "then": {
                        "command": "python scripts/translate-docs.py --file {file} --auto",
                        "async": True
                    }
                }
                
                with open(hook_file, 'w', encoding='utf-8') as f:
                    json.dump(hook_config, f, indent=2)
                
                logger.info(f"✓ Created {hook_file}")
            
            return True
            
        except Exception as e:
            logger.error(f"Failed to setup Kiro hooks: {e}")
            return False
    
    def validate_installation(self) -> Tuple[bool, List[str]]:
        """
        Validate the installation.
        
        Returns:
            Tuple of (success, list of issues)
        """
        logger.info("Validating installation...")
        
        issues = []
        
        # Check Python modules can be imported
        test_imports = [
            'openai',
            'watchdog',
            'yaml',
            'requests',
            'markdown'
        ]
        
        for module in test_imports:
            try:
                __import__(module)
                logger.debug(f"✓ {module} import successful")
            except ImportError:
                issues.append(f"Cannot import {module}")
        
        # Check configuration files
        config_file = self.workspace_root / 'config' / 'translation-config.json'
        if not config_file.exists():
            issues.append("Configuration file not found")
        else:
            try:
                with open(config_file, 'r', encoding='utf-8') as f:
                    json.load(f)
                logger.debug("✓ Configuration file valid")
            except json.JSONDecodeError:
                issues.append("Configuration file has invalid JSON")
        
        # Check script files exist
        required_scripts = [
            'scripts/translation-cli.py',
            'scripts/kiro_translator.py',
            'scripts/batch_processor.py',
            'scripts/translate_docs.py'
        ]
        
        for script in required_scripts:
            script_path = self.workspace_root / script
            if not script_path.exists():
                issues.append(f"Required script not found: {script}")
        
        # Test basic functionality
        try:
            from config.translation_config import TranslationConfig
            config = TranslationConfig()
            logger.debug("✓ Configuration loading works")
        except Exception as e:
            issues.append(f"Configuration loading failed: {e}")
        
        success = len(issues) == 0
        if success:
            logger.info("✓ Installation validation passed")
        else:
            logger.error(f"Installation validation failed with {len(issues)} issues")
            for issue in issues:
                logger.error(f"  - {issue}")
        
        return success, issues
    
    def run_health_check(self) -> bool:
        """
        Run system health check.
        
        Returns:
            True if system is healthy
        """
        logger.info("Running system health check...")
        
        try:
            # Test translation CLI
            result = subprocess.run([
                self.python_executable, 'scripts/translation-cli.py', 'config', '--show'
            ], capture_output=True, text=True, cwd=self.workspace_root)
            
            if result.returncode != 0:
                logger.error(f"CLI health check failed: {result.stderr}")
                return False
            
            logger.info("✓ CLI health check passed")
            
            # Test configuration loading
            result = subprocess.run([
                self.python_executable, '-c',
                'from config.translation_config import TranslationConfig; TranslationConfig()'
            ], capture_output=True, text=True, cwd=self.workspace_root)
            
            if result.returncode != 0:
                logger.error(f"Configuration health check failed: {result.stderr}")
                return False
            
            logger.info("✓ Configuration health check passed")
            
            return True
            
        except Exception as e:
            logger.error(f"Health check failed: {e}")
            return False
    
    def create_startup_script(self) -> bool:
        """
        Create startup script for easy system launch.
        
        Returns:
            True if successful
        """
        logger.info("Creating startup script...")
        
        try:
            startup_script = self.workspace_root / 'start-translation-system.sh'
            script_content = f"""#!/bin/bash
# Automated Documentation Translation System Startup Script

set -e

# Change to workspace directory
cd "{self.workspace_root}"

# Load environment variables
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
fi

# Check if virtual environment should be activated
if [ -d "venv" ]; then
    source venv/bin/activate
    echo "Activated virtual environment"
fi

# Function to start file watcher
start_watcher() {{
    echo "Starting file watcher..."
    python scripts/watch-docs.py --directory docs/ --recursive &
    WATCHER_PID=$!
    echo "File watcher started with PID $WATCHER_PID"
    echo $WATCHER_PID > .translation-watcher.pid
}}

# Function to stop file watcher
stop_watcher() {{
    if [ -f .translation-watcher.pid ]; then
        PID=$(cat .translation-watcher.pid)
        if kill -0 $PID 2>/dev/null; then
            kill $PID
            echo "Stopped file watcher (PID $PID)"
        fi
        rm -f .translation-watcher.pid
    fi
}}

# Function to show status
show_status() {{
    echo "Translation System Status:"
    echo "========================="
    python scripts/translation-cli.py status --dir docs/
    
    if [ -f .translation-watcher.pid ]; then
        PID=$(cat .translation-watcher.pid)
        if kill -0 $PID 2>/dev/null; then
            echo "File watcher: Running (PID $PID)"
        else
            echo "File watcher: Stopped"
            rm -f .translation-watcher.pid
        fi
    else
        echo "File watcher: Not running"
    fi
}}

# Handle command line arguments
case "$1" in
    start)
        start_watcher
        ;;
    stop)
        stop_watcher
        ;;
    restart)
        stop_watcher
        sleep 2
        start_watcher
        ;;
    status)
        show_status
        ;;
    translate)
        shift
        python scripts/translation-cli.py translate "$@"
        ;;
    batch)
        shift
        python scripts/translation-cli.py batch "$@"
        ;;
    validate)
        shift
        python scripts/translation-cli.py validate "$@"
        ;;
    *)
        echo "Usage: $0 {{start|stop|restart|status|translate|batch|validate}}"
        echo ""
        echo "Commands:"
        echo "  start     - Start the file watcher"
        echo "  stop      - Stop the file watcher"
        echo "  restart   - Restart the file watcher"
        echo "  status    - Show system status"
        echo "  translate - Translate files (pass additional arguments)"
        echo "  batch     - Batch translate (pass additional arguments)"
        echo "  validate  - Validate translations (pass additional arguments)"
        echo ""
        echo "Examples:"
        echo "  $0 start"
        echo "  $0 translate --file README.md"
        echo "  $0 batch --input-dir docs/ --output-dir docs/"
        exit 1
        ;;
esac
"""
            
            with open(startup_script, 'w', encoding='utf-8') as f:
                f.write(script_content)
            
            # Make script executable
            os.chmod(startup_script, 0o755)
            
            logger.info(f"✓ Created {startup_script}")
            return True
            
        except Exception as e:
            logger.error(f"Failed to create startup script: {e}")
            return False
    
    def setup_complete_system(self, force_deps: bool = False) -> bool:
        """
        Run complete system setup.
        
        Args:
            force_deps: Force reinstallation of dependencies
            
        Returns:
            True if setup was successful
        """
        logger.info("Starting complete system setup...")
        
        steps = [
            ("Prerequisites check", self.check_prerequisites),
            ("Directory structure", self.create_directory_structure),
            ("Dependencies installation", lambda: self.install_dependencies(force_deps)),
            ("Configuration creation", self.create_default_configuration),
            ("Kiro hooks setup", self.setup_kiro_hooks),
            ("Startup script creation", self.create_startup_script),
            ("Installation validation", lambda: self.validate_installation()[0]),
            ("Health check", self.run_health_check)
        ]
        
        for step_name, step_func in steps:
            logger.info(f"Running: {step_name}")
            try:
                if not step_func():
                    logger.error(f"Setup failed at: {step_name}")
                    return False
            except Exception as e:
                logger.error(f"Setup failed at {step_name}: {e}")
                return False
        
        logger.info("✅ System setup completed successfully!")
        self.print_setup_summary()
        return True
    
    def print_setup_summary(self):
        """Print setup summary and next steps."""
        print("\n" + "="*60)
        print("🎉 TRANSLATION SYSTEM SETUP COMPLETE!")
        print("="*60)
        print("\n📁 Files created:")
        print("  • config/translation-config.json - Main configuration")
        print("  • config/terminology.json - Technical terms configuration")
        print("  • .env.template - Environment variables template")
        print("  • .kiro/hooks/auto-translation.kiro.hook - Automatic translation hook")
        print("  • start-translation-system.sh - System startup script")
        print("  • requirements.txt - Python dependencies")
        
        print("\n🚀 Next steps:")
        print("  1. Copy .env.template to .env and configure your API keys:")
        print("     cp .env.template .env")
        print("     # Edit .env with your API keys")
        
        print("\n  2. Test the system:")
        print("     python scripts/translation-cli.py config --show")
        print("     python scripts/translation-cli.py translate --file README.md")
        
        print("\n  3. Start automatic translation:")
        print("     ./start-translation-system.sh start")
        
        print("\n  4. Check system status:")
        print("     ./start-translation-system.sh status")
        
        print("\n📚 Documentation:")
        print("  • User Guide: docs/en/translation-user-guide.md")
        print("  • API Reference: docs/en/translation-api-reference.md")
        print("  • Troubleshooting: docs/en/translation-troubleshooting.md")
        
        print("\n⚠️  Important:")
        print("  • Configure your API keys in .env before using")
        print("  • Review config/translation-config.json for your needs")
        print("  • Add technical terms to config/terminology.json")
        
        print("\n" + "="*60)

def main():
    """Main function for setup script."""
    parser = argparse.ArgumentParser(
        description="Setup the Automated Documentation Translation System"
    )
    parser.add_argument(
        '--workspace', '-w',
        help='Workspace root directory (default: current directory)'
    )
    parser.add_argument(
        '--force-deps', '-f',
        action='store_true',
        help='Force reinstallation of dependencies'
    )
    parser.add_argument(
        '--check-only', '-c',
        action='store_true',
        help='Only run validation checks'
    )
    parser.add_argument(
        '--verbose', '-v',
        action='store_true',
        help='Enable verbose logging'
    )
    
    args = parser.parse_args()
    
    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)
    
    # Initialize setup manager
    setup = TranslationSystemSetup(args.workspace)
    
    try:
        if args.check_only:
            # Run validation only
            success, issues = setup.validate_installation()
            if success:
                print("✅ System validation passed")
                setup.run_health_check()
            else:
                print("❌ System validation failed:")
                for issue in issues:
                    print(f"  - {issue}")
            return 0 if success else 1
        else:
            # Run complete setup
            success = setup.setup_complete_system(args.force_deps)
            return 0 if success else 1
    
    except KeyboardInterrupt:
        logger.info("Setup interrupted by user")
        return 1
    except Exception as e:
        logger.error(f"Setup failed with unexpected error: {e}")
        return 1

if __name__ == '__main__':
    sys.exit(main())