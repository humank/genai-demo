#!/usr/bin/env python3
"""
Test Migration Analysis Script

This script analyzes the existing test codebase to identify integration tests
that need to be migrated to the staging-tests directory structure.
"""

import os
import re
import json
from pathlib import Path
from typing import Dict, List, Set, Tuple
from dataclasses import dataclass, asdict
from collections import defaultdict

@dataclass
class TestFile:
    """Represents a test file with its characteristics"""
    path: str
    name: str
    package: str
    test_type: str
    annotations: List[str]
    dependencies: List[str]
    external_services: List[str]
    migration_target: str
    complexity_score: int
    notes: List[str]

class TestMigrationAnalyzer:
    """Analyzes test files for migration categorization"""
    
    def __init__(self, test_root: str = "app/src/test/java"):
        self.test_root = Path(test_root)
        self.test_files: List[TestFile] = []
        
        # Integration test indicators
        self.integration_annotations = {
            '@SpringBootTest', '@DataJpaTest', '@WebMvcTest', '@JsonTest',
            '@TestPropertySource', '@ActiveProfiles', '@Sql', '@Transactional',
            '@DirtiesContext', '@TestExecutionListeners', '@ContextConfiguration'
        }
        
        # External service indicators
        self.external_service_patterns = {
            'redis': ['redis', 'RedisTemplate', 'JedisPool', 'LettuceConnectionFactory'],
            'kafka': ['kafka', 'KafkaTemplate', 'KafkaConsumer', 'KafkaProducer', '@KafkaListener'],
            'database': ['DataSource', 'JdbcTemplate', 'EntityManager', 'Repository', 'H2', 'PostgreSQL', 'Aurora'],
            'dynamodb': ['DynamoDB', 'AmazonDynamoDB', 'DynamoDBMapper'],
            'aws': ['AWS', 'S3', 'SQS', 'SNS', 'CloudWatch', 'X-Ray'],
            'http': ['RestTemplate', 'WebClient', 'MockWebServer', 'WireMock'],
            'messaging': ['JmsTemplate', 'RabbitTemplate', 'MessageProducer']
        }
        
        # Performance test indicators
        self.performance_patterns = [
            'performance', 'load', 'stress', 'benchmark', 'concurrent',
            'throughput', 'latency', '@Benchmark', 'JMH'
        ]
        
        # Cross-region test indicators
        self.cross_region_patterns = [
            'cross.region', 'multi.region', 'disaster.recovery', 'failover',
            'replication', 'backup', 'recovery'
        ]

    def analyze_all_tests(self) -> Dict:
        """Analyze all test files and categorize them"""
        print("🔍 Scanning test files...")
        
        # Find all Java test files
        java_files = list(self.test_root.rglob("*.java"))
        print(f"Found {len(java_files)} Java files")
        
        for java_file in java_files:
            if self._is_test_file(java_file):
                test_file = self._analyze_test_file(java_file)
                if test_file:
                    self.test_files.append(test_file)
        
        print(f"Analyzed {len(self.test_files)} test files")
        
        # Generate comprehensive analysis
        analysis = self._generate_analysis()
        return analysis

    def _is_test_file(self, file_path: Path) -> bool:
        """Check if file is a test file"""
        name = file_path.name
        return (name.endswith('Test.java') or 
                name.endswith('Tests.java') or 
                'test' in name.lower())

    def _analyze_test_file(self, file_path: Path) -> TestFile:
        """Analyze a single test file"""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
        except Exception as e:
            print(f"⚠️  Error reading {file_path}: {e}")
            return None
        
        # Extract basic information
        relative_path = str(file_path.relative_to(self.test_root))
        name = file_path.stem
        package = self._extract_package(content)
        
        # Analyze annotations
        annotations = self._extract_annotations(content)
        
        # Determine test type
        test_type = self._determine_test_type(content, annotations, name)
        
        # Find dependencies and external services
        dependencies = self._extract_dependencies(content)
        external_services = self._identify_external_services(content)
        
        # Determine migration target
        migration_target = self._determine_migration_target(test_type, external_services, name, content)
        
        # Calculate complexity score
        complexity_score = self._calculate_complexity(content, annotations, external_services)
        
        # Generate notes
        notes = self._generate_notes(content, annotations, external_services, test_type)
        
        return TestFile(
            path=relative_path,
            name=name,
            package=package,
            test_type=test_type,
            annotations=annotations,
            dependencies=dependencies,
            external_services=external_services,
            migration_target=migration_target,
            complexity_score=complexity_score,
            notes=notes
        )

    def _extract_package(self, content: str) -> str:
        """Extract package declaration"""
        match = re.search(r'package\s+([\w.]+);', content)
        return match.group(1) if match else ""

    def _extract_annotations(self, content: str) -> List[str]:
        """Extract test-related annotations"""
        annotations = []
        for annotation in self.integration_annotations:
            if annotation in content:
                annotations.append(annotation)
        
        # Look for custom test annotations
        custom_annotations = re.findall(r'@(\w+Test|\w+Integration)', content)
        annotations.extend([f"@{ann}" for ann in custom_annotations])
        
        return list(set(annotations))

    def _determine_test_type(self, content: str, annotations: List[str], name: str) -> str:
        """Determine the type of test"""
        name_lower = name.lower()
        content_lower = content.lower()
        
        # Check for performance tests
        if any(pattern in content_lower or pattern in name_lower 
               for pattern in self.performance_patterns):
            return "performance"
        
        # Check for cross-region tests
        if any(pattern in content_lower or pattern in name_lower 
               for pattern in self.cross_region_patterns):
            return "cross-region"
        
        # Check for integration test annotations
        integration_annotations = ['@SpringBootTest', '@DataJpaTest', '@WebMvcTest', 
                                 '@JsonTest', '@TestPropertySource']
        if any(ann in annotations for ann in integration_annotations):
            return "integration"
        
        # Check for unit test indicators
        if ('@ExtendWith(MockitoExtension.class)' in content or 
            '@Mock' in content or 
            'unittest' in name_lower or
            'unit' in name_lower):
            return "unit"
        
        # Check for BDD tests
        if ('cucumber' in content_lower or 
            'gherkin' in content_lower or 
            'bdd' in name_lower):
            return "bdd"
        
        # Default classification based on content analysis
        if any(ann in annotations for ann in self.integration_annotations):
            return "integration"
        
        return "unit"  # Default to unit test

    def _extract_dependencies(self, content: str) -> List[str]:
        """Extract import dependencies"""
        imports = re.findall(r'import\s+([\w.]+);', content)
        
        # Filter for relevant test dependencies
        relevant_deps = []
        for imp in imports:
            if any(keyword in imp.lower() for keyword in 
                   ['test', 'mock', 'spring', 'junit', 'assertj', 'cucumber']):
                relevant_deps.append(imp.split('.')[-1])
        
        return list(set(relevant_deps))

    def _identify_external_services(self, content: str) -> List[str]:
        """Identify external service dependencies"""
        services = []
        content_lower = content.lower()
        
        for service, patterns in self.external_service_patterns.items():
            if any(pattern.lower() in content_lower for pattern in patterns):
                services.append(service)
        
        return services

    def _determine_migration_target(self, test_type: str, external_services: List[str], 
                                  name: str, content: str) -> str:
        """Determine where the test should be migrated"""
        name_lower = name.lower()
        content_lower = content.lower()
        
        # Performance tests
        if test_type == "performance":
            return "staging-tests/performance/"
        
        # Cross-region tests
        if test_type == "cross-region":
            return "staging-tests/cross-region/"
        
        # Unit tests stay in place
        if test_type == "unit":
            return "app/src/test/java/ (no migration needed)"
        
        # BDD tests stay in place but may need staging support
        if test_type == "bdd":
            return "app/src/test/java/ (may need staging support)"
        
        # Integration tests - categorize by primary service
        if test_type == "integration":
            # Database integration
            if 'database' in external_services or any(db in content_lower 
                for db in ['repository', 'jpa', 'jdbc', 'datasource']):
                return "staging-tests/integration/database/"
            
            # Cache integration
            if 'redis' in external_services:
                return "staging-tests/integration/cache/"
            
            # Messaging integration
            if any(msg in external_services for msg in ['kafka', 'messaging']):
                return "staging-tests/integration/messaging/"
            
            # Monitoring integration
            if any(monitor in content_lower for monitor in ['actuator', 'metrics', 'health']):
                return "staging-tests/integration/monitoring/"
            
            # General integration
            return "staging-tests/integration/"
        
        return "staging-tests/integration/"

    def _calculate_complexity(self, content: str, annotations: List[str], 
                            external_services: List[str]) -> int:
        """Calculate complexity score for migration priority"""
        score = 0
        
        # Base score for integration tests
        if any(ann in annotations for ann in self.integration_annotations):
            score += 3
        
        # External service dependencies
        score += len(external_services) * 2
        
        # Code complexity indicators
        if '@Transactional' in annotations:
            score += 2
        if '@DirtiesContext' in annotations:
            score += 3
        if 'TestExecutionListeners' in content:
            score += 2
        
        # File size (rough estimate)
        lines = len(content.split('\n'))
        if lines > 200:
            score += 3
        elif lines > 100:
            score += 1
        
        return score

    def _generate_notes(self, content: str, annotations: List[str], 
                       external_services: List[str], test_type: str) -> List[str]:
        """Generate migration notes"""
        notes = []
        
        if test_type == "integration":
            notes.append("Requires staging environment with external services")
        
        if '@DirtiesContext' in annotations:
            notes.append("Uses @DirtiesContext - may need careful cleanup handling")
        
        if 'TestExecutionListeners' in content:
            notes.append("Uses custom test execution listeners")
        
        if len(external_services) > 2:
            notes.append(f"Complex dependencies: {', '.join(external_services)}")
        
        if '@Transactional' in annotations:
            notes.append("Uses transactions - ensure proper rollback in staging")
        
        if 'WireMock' in content or 'MockWebServer' in content:
            notes.append("Uses HTTP mocking - may need real service integration")
        
        return notes

    def _generate_analysis(self) -> Dict:
        """Generate comprehensive analysis report"""
        # Categorize tests
        by_type = defaultdict(list)
        by_migration_target = defaultdict(list)
        by_external_service = defaultdict(list)
        
        for test in self.test_files:
            by_type[test.test_type].append(test)
            by_migration_target[test.migration_target].append(test)
            
            for service in test.external_services:
                by_external_service[service].append(test)
        
        # Calculate statistics
        total_tests = len(self.test_files)
        integration_tests = len(by_type['integration'])
        unit_tests = len(by_type['unit'])
        performance_tests = len(by_type['performance'])
        cross_region_tests = len(by_type['cross-region'])
        
        # Migration complexity analysis
        high_complexity = [t for t in self.test_files if t.complexity_score >= 8]
        medium_complexity = [t for t in self.test_files if 4 <= t.complexity_score < 8]
        low_complexity = [t for t in self.test_files if t.complexity_score < 4]
        
        return {
            'summary': {
                'total_tests': total_tests,
                'integration_tests': integration_tests,
                'unit_tests': unit_tests,
                'performance_tests': performance_tests,
                'cross_region_tests': cross_region_tests,
                'migration_needed': integration_tests + performance_tests + cross_region_tests
            },
            'by_type': {k: [asdict(t) for t in v] for k, v in by_type.items()},
            'by_migration_target': {k: [asdict(t) for t in v] for k, v in by_migration_target.items()},
            'by_external_service': {k: [asdict(t) for t in v] for k, v in by_external_service.items()},
            'complexity_analysis': {
                'high_complexity': [asdict(t) for t in high_complexity],
                'medium_complexity': [asdict(t) for t in medium_complexity],
                'low_complexity': [asdict(t) for t in low_complexity]
            },
            'migration_priority': sorted([asdict(t) for t in self.test_files], 
                                       key=lambda x: x['complexity_score'], reverse=True)
        }

def main():
    """Main execution function"""
    print("🚀 Starting Test Migration Analysis")
    print("=" * 50)
    
    analyzer = TestMigrationAnalyzer()
    analysis = analyzer.analyze_all_tests()
    
    # Save detailed analysis
    output_file = "test-migration-analysis.json"
    with open(output_file, 'w') as f:
        json.dump(analysis, f, indent=2)
    
    print(f"\n📊 Analysis Summary:")
    print(f"Total tests analyzed: {analysis['summary']['total_tests']}")
    print(f"Integration tests: {analysis['summary']['integration_tests']}")
    print(f"Unit tests: {analysis['summary']['unit_tests']}")
    print(f"Performance tests: {analysis['summary']['performance_tests']}")
    print(f"Cross-region tests: {analysis['summary']['cross_region_tests']}")
    print(f"Tests requiring migration: {analysis['summary']['migration_needed']}")
    
    print(f"\n📁 Migration Targets:")
    for target, tests in analysis['by_migration_target'].items():
        if 'staging-tests' in target:
            print(f"  {target}: {len(tests)} tests")
    
    print(f"\n🔧 External Service Dependencies:")
    for service, tests in analysis['by_external_service'].items():
        print(f"  {service}: {len(tests)} tests")
    
    print(f"\n⚡ Complexity Distribution:")
    print(f"  High complexity (≥8): {len(analysis['complexity_analysis']['high_complexity'])} tests")
    print(f"  Medium complexity (4-7): {len(analysis['complexity_analysis']['medium_complexity'])} tests")
    print(f"  Low complexity (<4): {len(analysis['complexity_analysis']['low_complexity'])} tests")
    
    print(f"\n✅ Detailed analysis saved to: {output_file}")
    print("🎯 Ready for migration mapping and strategy creation!")

if __name__ == "__main__":
    main()