#!/usr/bin/env python3
"""
Translation Configuration System

This module provides comprehensive configuration management for the automated
documentation translation system with environment-specific settings and validation.
"""

import os
import json
import logging
from pathlib import Path
from typing import Dict, List, Optional, Any, Union
from dataclasses import dataclass, field, asdict
from enum import Enum

logger = logging.getLogger(__name__)

class LogLevel(Enum):
    """Logging levels."""
    DEBUG = "DEBUG"
    INFO = "INFO"
    WARNING = "WARNING"
    ERROR = "ERROR"
    CRITICAL = "CRITICAL"

class TranslationMode(Enum):
    """Translation operation modes."""
    AUTOMATIC = "automatic"
    MANUAL = "manual"
    HYBRID = "hybrid"

@dataclass
class TranslationSettings:
    """Translation-specific settings."""
    target_language: str = "zh-TW"
    source_language: str = "en"
    file_extension: str = ".zh-TW.md"
    mode: TranslationMode = TranslationMode.AUTOMATIC
    preserve_formatting: bool = True
    preserve_code_blocks: bool = True
    preserve_links: bool = True
    context_aware: bool = True

@dataclass
class FilePatterns:
    """File pattern configuration."""
    include_patterns: List[str] = field(default_factory=lambda: ["**/*.md"])
    exclude_patterns: List[str] = field(default_factory=lambda: [
        "**/*.zh-TW.md",
        "node_modules/**",
        ".git/**",
        ".kiro/**",
        "build/**",
        "target/**",
        ".backup/**",
        "coverage/**",
        "dist/**",
        "tmp/**",
        "temp/**"
    ])
    watch_patterns: List[str] = field(default_factory=lambda: ["**/*.md"])
    backup_exclude_patterns: List[str] = field(default_factory=lambda: [
        ".git/**",
        "node_modules/**",
        "**/*.log",
        "**/*.tmp"
    ])

@dataclass
class TerminologySettings:
    """Terminology and translation consistency settings."""
    preserve_terms: List[str] = field(default_factory=lambda: [
        # Core technical terms
        "API", "DDD", "README", "GitHub", "Docker", "Kubernetes",
        
        # Backend technologies
        "Spring Boot", "JPA", "Hibernate", "PostgreSQL", "Redis",
        "MySQL", "MongoDB", "Elasticsearch", "RabbitMQ", "Kafka",
        
        # Cloud and infrastructure
        "AWS", "CDK", "Lambda", "S3", "RDS", "EKS", "VPC", "EC2",
        "CloudFormation", "Terraform", "Ansible",
        
        # Web technologies
        "HTTP", "HTTPS", "REST", "GraphQL", "JSON", "XML", "YAML",
        "OAuth", "JWT", "CORS", "WebSocket",
        
        # Development practices
        "CI/CD", "DevOps", "Microservices", "TDD", "BDD", "SOLID",
        "MVC", "MVP", "MVVM", "DRY", "KISS", "YAGNI",
        
        # Build tools
        "Gradle", "Maven", "npm", "yarn", "webpack", "Vite",
        
        # Frontend technologies
        "React", "Angular", "Vue", "TypeScript", "JavaScript",
        "HTML", "CSS", "SCSS", "Sass", "Bootstrap", "Tailwind",
        
        # Testing frameworks
        "JUnit", "Mockito", "Cucumber", "Selenium", "Jest", "Cypress",
        
        # Monitoring and observability
        "Prometheus", "Grafana", "Jaeger", "Zipkin", "ELK Stack"
    ])
    
    technical_domains: Dict[str, List[str]] = field(default_factory=lambda: {
        "backend": [
            "Spring", "Hibernate", "JPA", "PostgreSQL", "Redis",
            "Microservices", "REST API", "GraphQL"
        ],
        "frontend": [
            "React", "Angular", "Vue", "TypeScript", "JavaScript",
            "HTML", "CSS", "Responsive Design"
        ],
        "devops": [
            "Docker", "Kubernetes", "AWS", "CDK", "CI/CD",
            "Infrastructure as Code", "Monitoring"
        ],
        "testing": [
            "JUnit", "Mockito", "Cucumber", "TDD", "BDD",
            "Integration Testing", "E2E Testing"
        ],
        "architecture": [
            "DDD", "Hexagonal Architecture", "Clean Architecture",
            "Event-Driven Architecture", "CQRS", "Event Sourcing"
        ]
    })
    
    custom_translations: Dict[str, str] = field(default_factory=dict)
    consistency_check: bool = True

@dataclass
class FileManagementSettings:
    """File management configuration."""
    backup_enabled: bool = True
    backup_directory: str = ".backup"
    atomic_writes: bool = True
    preserve_metadata: bool = True
    cleanup_old_backups: bool = True
    backup_retention_count: int = 5
    create_directories: bool = True
    verify_integrity: bool = True

@dataclass
class WatcherSettings:
    """File watcher configuration."""
    enabled: bool = True
    debounce_delay: float = 2.0
    watch_paths: List[str] = field(default_factory=lambda: ["."])
    recursive: bool = True
    log_file: str = "translation-watcher.log"
    max_queue_size: int = 100
    processing_threads: int = 2

@dataclass
class QualitySettings:
    """Quality assurance configuration."""
    validate_markdown_structure: bool = True
    check_link_integrity: bool = True
    verify_code_block_preservation: bool = True
    terminology_consistency_check: bool = True
    translation_quality_threshold: float = 0.8
    auto_fix_common_issues: bool = True
    generate_quality_reports: bool = True

@dataclass
class LoggingSettings:
    """Logging configuration."""
    level: LogLevel = LogLevel.INFO
    file: str = "translation.log"
    format: str = "%(asctime)s - %(name)s - %(levelname)s - %(message)s"
    max_file_size: str = "10MB"
    backup_count: int = 3
    console_output: bool = True
    structured_logging: bool = False

@dataclass
class PerformanceSettings:
    """Performance and resource management settings."""
    max_concurrent_translations: int = 3
    translation_timeout: int = 300  # seconds
    retry_attempts: int = 3
    retry_delay: int = 5  # seconds
    memory_limit_mb: int = 512
    cache_enabled: bool = True
    cache_size: int = 100
    batch_size: int = 10

@dataclass
class KiroIntegrationSettings:
    """Kiro-specific integration settings."""
    translation_prompt_template: str = """
Please translate the following markdown content from English to Traditional Chinese (zh-TW).

IMPORTANT REQUIREMENTS:
1. Preserve ALL markdown formatting exactly (headers, lists, code blocks, links, tables)
2. Keep ALL technical terms unchanged: {preserve_terms}
3. Keep ALL code examples, file paths, and URLs unchanged
4. Maintain professional technical documentation tone
5. Preserve line breaks and spacing
6. Keep HTML tags and markdown syntax intact
7. Do not translate content inside code blocks (```code```) or inline code (`code`)

Content to translate:
{content}

Please provide ONLY the translated content without any additional explanation or comments.
"""
    
    context_aware_prompts: Dict[str, str] = field(default_factory=lambda: {
        "api_documentation": "This is API documentation. Pay special attention to preserving endpoint URLs, parameter names, and response formats.",
        "architecture_documentation": "This is architecture documentation. Maintain technical precision for architectural concepts and patterns.",
        "user_guide": "This is a user guide. Focus on clear, user-friendly language while preserving technical accuracy.",
        "development_guide": "This is development documentation. Preserve all code examples, commands, and technical procedures exactly.",
        "configuration_guide": "This is configuration documentation. Keep all configuration keys, values, and file paths unchanged.",
        "troubleshooting_guide": "This is troubleshooting documentation. Maintain clarity for problem-solving steps and error messages."
    })
    
    use_context_detection: bool = True
    max_prompt_length: int = 4000
    temperature: float = 0.3  # Lower temperature for more consistent translations

@dataclass
class TranslationConfig:
    """Main configuration class containing all settings."""
    translation: TranslationSettings = field(default_factory=TranslationSettings)
    file_patterns: FilePatterns = field(default_factory=FilePatterns)
    terminology: TerminologySettings = field(default_factory=TerminologySettings)
    file_management: FileManagementSettings = field(default_factory=FileManagementSettings)
    watcher: WatcherSettings = field(default_factory=WatcherSettings)
    quality: QualitySettings = field(default_factory=QualitySettings)
    logging: LoggingSettings = field(default_factory=LoggingSettings)
    performance: PerformanceSettings = field(default_factory=PerformanceSettings)
    kiro_integration: KiroIntegrationSettings = field(default_factory=KiroIntegrationSettings)
    
    # Environment-specific settings
    environment: str = "development"
    debug_mode: bool = False
    dry_run: bool = False
    
    # Metadata
    version: str = "1.0.0"
    created_at: Optional[str] = None
    updated_at: Optional[str] = None

class ConfigurationManager:
    """
    Configuration manager for the translation system.
    
    This class handles loading, validation, and management of configuration
    settings with support for environment-specific overrides.
    """
    
    def __init__(self, config_file: Optional[str] = None, environment: Optional[str] = None):
        """
        Initialize the configuration manager.
        
        Args:
            config_file: Path to configuration file
            environment: Environment name (development, staging, production)
        """
        self.config_file = config_file
        self.environment = environment or os.getenv('TRANSLATION_ENV', 'development')
        self.config = TranslationConfig()
        
        # Load configuration
        self._load_configuration()
        
        # Apply environment-specific settings
        self._apply_environment_settings()
        
        # Validate configuration
        self._validate_configuration()
    
    def _load_configuration(self):
        """Load configuration from file."""
        if self.config_file and os.path.exists(self.config_file):
            try:
                with open(self.config_file, 'r', encoding='utf-8') as f:
                    config_data = json.load(f)
                
                # Update configuration with loaded data
                self._update_config_from_dict(config_data)
                
                logger.info(f"Configuration loaded from {self.config_file}")
                
            except Exception as e:
                logger.warning(f"Failed to load configuration from {self.config_file}: {e}")
        
        # Load environment-specific configuration
        env_config_file = f"config/translation-config-{self.environment}.json"
        if os.path.exists(env_config_file):
            try:
                with open(env_config_file, 'r', encoding='utf-8') as f:
                    env_config_data = json.load(f)
                
                self._update_config_from_dict(env_config_data)
                logger.info(f"Environment configuration loaded from {env_config_file}")
                
            except Exception as e:
                logger.warning(f"Failed to load environment configuration: {e}")
    
    def _update_config_from_dict(self, config_data: Dict[str, Any]):
        """Update configuration from dictionary."""
        for section_name, section_data in config_data.items():
            if hasattr(self.config, section_name) and isinstance(section_data, dict):
                section = getattr(self.config, section_name)
                
                if hasattr(section, '__dict__'):
                    # Update dataclass fields
                    for key, value in section_data.items():
                        if hasattr(section, key):
                            setattr(section, key, value)
                else:
                    # Direct assignment for simple types
                    setattr(self.config, section_name, section_data)
    
    def _apply_environment_settings(self):
        """Apply environment-specific settings."""
        self.config.environment = self.environment
        
        if self.environment == 'development':
            self.config.debug_mode = True
            self.config.logging.level = LogLevel.DEBUG
            self.config.logging.console_output = True
            self.config.quality.generate_quality_reports = True
            
        elif self.environment == 'staging':
            self.config.debug_mode = False
            self.config.logging.level = LogLevel.INFO
            self.config.performance.max_concurrent_translations = 2
            
        elif self.environment == 'production':
            self.config.debug_mode = False
            self.config.logging.level = LogLevel.WARNING
            self.config.logging.console_output = False
            self.config.performance.max_concurrent_translations = 5
            self.config.quality.auto_fix_common_issues = False
    
    def _validate_configuration(self):
        """Validate configuration settings."""
        errors = []
        warnings = []
        
        # Validate translation settings
        if not self.config.translation.target_language:
            errors.append("Target language must be specified")
        
        if not self.config.translation.source_language:
            errors.append("Source language must be specified")
        
        # Validate file patterns
        if not self.config.file_patterns.include_patterns:
            warnings.append("No include patterns specified")
        
        # Validate performance settings
        if self.config.performance.max_concurrent_translations < 1:
            errors.append("Max concurrent translations must be at least 1")
        
        if self.config.performance.translation_timeout < 10:
            warnings.append("Translation timeout is very low (< 10 seconds)")
        
        # Validate paths
        for watch_path in self.config.watcher.watch_paths:
            if not os.path.exists(watch_path):
                warnings.append(f"Watch path does not exist: {watch_path}")
        
        # Log validation results
        if errors:
            for error in errors:
                logger.error(f"Configuration error: {error}")
            raise ValueError(f"Configuration validation failed: {errors}")
        
        if warnings:
            for warning in warnings:
                logger.warning(f"Configuration warning: {warning}")
    
    def get_config(self) -> TranslationConfig:
        """Get the current configuration."""
        return self.config
    
    def save_config(self, output_file: Optional[str] = None) -> str:
        """
        Save current configuration to file.
        
        Args:
            output_file: Output file path
            
        Returns:
            Path to saved configuration file
        """
        if output_file is None:
            output_file = self.config_file or f"config/translation-config-{self.environment}.json"
        
        # Update metadata
        self.config.updated_at = datetime.now().isoformat()
        if self.config.created_at is None:
            self.config.created_at = self.config.updated_at
        
        # Convert to dictionary
        config_dict = asdict(self.config)
        
        # Convert enums to strings
        config_dict = self._convert_enums_to_strings(config_dict)
        
        # Save to file
        os.makedirs(os.path.dirname(output_file), exist_ok=True)
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(config_dict, f, indent=2, ensure_ascii=False)
        
        logger.info(f"Configuration saved to {output_file}")
        return output_file
    
    def _convert_enums_to_strings(self, obj: Any) -> Any:
        """Convert enum values to strings recursively."""
        if isinstance(obj, dict):
            return {key: self._convert_enums_to_strings(value) for key, value in obj.items()}
        elif isinstance(obj, list):
            return [self._convert_enums_to_strings(item) for item in obj]
        elif isinstance(obj, Enum):
            return obj.value
        else:
            return obj
    
    def update_setting(self, section: str, key: str, value: Any):
        """
        Update a specific configuration setting.
        
        Args:
            section: Configuration section name
            key: Setting key
            value: New value
        """
        if hasattr(self.config, section):
            section_obj = getattr(self.config, section)
            if hasattr(section_obj, key):
                setattr(section_obj, key, value)
                logger.info(f"Updated {section}.{key} = {value}")
            else:
                raise ValueError(f"Unknown setting: {section}.{key}")
        else:
            raise ValueError(f"Unknown section: {section}")
    
    def get_setting(self, section: str, key: str) -> Any:
        """
        Get a specific configuration setting.
        
        Args:
            section: Configuration section name
            key: Setting key
            
        Returns:
            Setting value
        """
        if hasattr(self.config, section):
            section_obj = getattr(self.config, section)
            if hasattr(section_obj, key):
                return getattr(section_obj, key)
            else:
                raise ValueError(f"Unknown setting: {section}.{key}")
        else:
            raise ValueError(f"Unknown section: {section}")
    
    def print_config_summary(self):
        """Print a summary of current configuration."""
        print("\n" + "="*60)
        print("TRANSLATION SYSTEM CONFIGURATION")
        print("="*60)
        print(f"Environment: {self.config.environment}")
        print(f"Version: {self.config.version}")
        print(f"Debug Mode: {self.config.debug_mode}")
        print(f"Dry Run: {self.config.dry_run}")
        
        print(f"\nTranslation Settings:")
        print(f"  Source Language: {self.config.translation.source_language}")
        print(f"  Target Language: {self.config.translation.target_language}")
        print(f"  Mode: {self.config.translation.mode.value}")
        
        print(f"\nFile Management:")
        print(f"  Backup Enabled: {self.config.file_management.backup_enabled}")
        print(f"  Atomic Writes: {self.config.file_management.atomic_writes}")
        
        print(f"\nPerformance:")
        print(f"  Max Concurrent: {self.config.performance.max_concurrent_translations}")
        print(f"  Timeout: {self.config.performance.translation_timeout}s")
        
        print(f"\nQuality Assurance:")
        print(f"  Validate Structure: {self.config.quality.validate_markdown_structure}")
        print(f"  Check Links: {self.config.quality.check_link_integrity}")
        
        print("="*60)


# Global configuration instance
_config_manager: Optional[ConfigurationManager] = None

def get_config_manager(config_file: Optional[str] = None, environment: Optional[str] = None) -> ConfigurationManager:
    """
    Get the global configuration manager instance.
    
    Args:
        config_file: Configuration file path
        environment: Environment name
        
    Returns:
        ConfigurationManager instance
    """
    global _config_manager
    
    if _config_manager is None:
        _config_manager = ConfigurationManager(config_file, environment)
    
    return _config_manager

def get_config() -> TranslationConfig:
    """Get the current configuration."""
    return get_config_manager().get_config()


def main():
    """Main function for configuration management."""
    import argparse
    from datetime import datetime
    
    parser = argparse.ArgumentParser(description='Translation Configuration Manager')
    parser.add_argument('--generate', action='store_true', help='Generate default configuration')
    parser.add_argument('--validate', metavar='CONFIG', help='Validate configuration file')
    parser.add_argument('--environment', choices=['development', 'staging', 'production'],
                       default='development', help='Environment')
    parser.add_argument('--output', metavar='FILE', help='Output file for generated configuration')
    
    args = parser.parse_args()
    
    if args.generate:
        # Generate default configuration
        config_manager = ConfigurationManager(environment=args.environment)
        output_file = args.output or f"config/translation-config-{args.environment}.json"
        saved_file = config_manager.save_config(output_file)
        print(f"Default configuration generated: {saved_file}")
        config_manager.print_config_summary()
        
    elif args.validate:
        # Validate configuration file
        try:
            config_manager = ConfigurationManager(args.validate, args.environment)
            print(f"Configuration file {args.validate} is valid")
            config_manager.print_config_summary()
        except Exception as e:
            print(f"Configuration validation failed: {e}")
            return 1
    
    else:
        parser.print_help()
    
    return 0


if __name__ == '__main__':
    exit(main())