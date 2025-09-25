#!/usr/bin/env python3
"""
Performance Testing Script for Batch Translation

This script tests the performance of batch translation processing
with different configurations and provides benchmarking results.
"""

import os
import sys
import time
import tempfile
import shutil
import statistics
from pathlib import Path
from typing import List, Dict, Tuple
import argparse

# Add current directory to path for imports
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from batch_processor import BatchProcessor
from translate_docs import DocumentationTranslator


def create_test_files(count: int, size_category: str = 'medium') -> List[str]:
    """
    Create test markdown files for performance testing.
    
    Args:
        count: Number of files to create
        size_category: Size category ('small', 'medium', 'large')
        
    Returns:
        List of created file paths
    """
    # Content templates by size
    content_templates = {
        'small': """# Test Document {index}

This is a small test document for performance testing.

## Overview
Simple content with basic markdown formatting.

- Item 1
- Item 2
- Item 3

**Bold text** and *italic text*.
""",
        'medium': """# Test Document {index}

This is a medium-sized test document for performance testing.

## Overview
This document contains more comprehensive content to test translation performance
with various markdown elements and structures.

### Features
- Multiple sections
- Code blocks
- Tables
- Lists

## Code Example

```python
def hello_world():
    print("Hello, World!")
    return True
```

## Table Example

| Feature | Status | Notes |
|---------|--------|-------|
| Translation | ✅ | Working |
| Batch Processing | ✅ | Implemented |
| Performance | 🔄 | Testing |

## Lists

### Ordered List
1. First item
2. Second item
3. Third item

### Unordered List
- Alpha
- Beta
- Gamma

## Conclusion
This is a test document with {index} as the identifier.
""",
        'large': """# Comprehensive Test Document {index}

This is a large test document designed to test translation performance
with extensive content and complex markdown structures.

## Table of Contents
1. [Introduction](#introduction)
2. [Architecture](#architecture)
3. [Implementation](#implementation)
4. [Testing](#testing)
5. [Performance](#performance)
6. [Conclusion](#conclusion)

## Introduction

This document serves as a comprehensive test case for evaluating the performance
of our automated translation system. It contains various markdown elements
including headers, lists, tables, code blocks, and extensive text content.

### Purpose
The primary purpose of this document is to:
- Test translation accuracy with complex content
- Evaluate performance with larger documents
- Validate markdown formatting preservation
- Assess batch processing capabilities

## Architecture

### System Overview
The translation system consists of several key components:

```mermaid
graph LR
    A[Input Files] --> B[File Scanner]
    B --> C[Translation Engine]
    C --> D[Output Generator]
    D --> E[Validation]
```

### Components

#### File Scanner
The file scanner is responsible for:
- Identifying markdown files
- Filtering based on patterns
- Checking modification timestamps
- Queuing files for processing

#### Translation Engine
The translation engine handles:
- Content extraction
- Language detection
- Translation processing
- Format preservation

#### Output Generator
The output generator manages:
- File creation
- Naming conventions
- Directory structure
- Metadata preservation

## Implementation

### Core Classes

```python
class DocumentationTranslator:
    def __init__(self, config_file=None):
        self.translator = KiroTranslator()
        self.config = self._load_config(config_file)
    
    def translate_file(self, source_file, force=False):
        # Implementation details
        pass
    
    def translate_directory(self, directory, force=False):
        # Implementation details
        pass
```

### Configuration

The system supports various configuration options:

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| include_patterns | List | ['**/*.md'] | File patterns to include |
| exclude_patterns | List | ['**/*.zh-TW.md'] | File patterns to exclude |
| preserve_terms | List | ['API', 'DDD'] | Terms to preserve |
| backup_enabled | Boolean | true | Enable backup creation |
| dry_run | Boolean | false | Dry run mode |

## Testing

### Test Categories

#### Unit Tests
- Individual function testing
- Mock-based isolation
- Edge case validation
- Error handling verification

#### Integration Tests
- Component interaction testing
- File system operations
- Translation accuracy
- Performance benchmarks

#### End-to-End Tests
- Complete workflow testing
- Real file processing
- User scenario validation
- System integration

### Performance Metrics

We track several key performance metrics:

1. **Processing Time**: Total time to process files
2. **Throughput**: Files processed per minute
3. **Memory Usage**: Peak memory consumption
4. **Success Rate**: Percentage of successful translations
5. **Error Rate**: Percentage of failed translations

## Performance

### Benchmarking Results

Based on our testing with document {index}, we observe:

- **Small files** (< 1KB): ~0.5 seconds per file
- **Medium files** (1-10KB): ~2.0 seconds per file  
- **Large files** (> 10KB): ~5.0 seconds per file

### Optimization Strategies

1. **Parallel Processing**: Use multiple workers
2. **Caching**: Cache translation results
3. **Batching**: Process files in batches
4. **Filtering**: Skip unchanged files

## Conclusion

This test document {index} demonstrates the comprehensive testing approach
for our translation system. The performance characteristics vary based on
file size, content complexity, and system configuration.

### Key Findings
- Batch processing significantly improves throughput
- Parallel workers reduce total processing time
- File size has linear impact on processing time
- Translation accuracy remains high across all test cases

### Recommendations
1. Use batch processing for large document sets
2. Configure appropriate number of workers based on system resources
3. Enable caching for frequently translated content
4. Monitor performance metrics regularly

---

*This is test document {index} generated for performance testing purposes.*
"""
    }
    
    # Create temporary directory
    temp_dir = tempfile.mkdtemp(prefix='translation_test_')
    created_files = []
    
    template = content_templates.get(size_category, content_templates['medium'])
    
    for i in range(count):
        file_path = Path(temp_dir) / f"test_doc_{i:03d}.md"
        content = template.format(index=i)
        
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        
        created_files.append(str(file_path))
    
    return created_files


def run_performance_test(files: List[str], workers: int, test_name: str) -> Dict:
    """
    Run a performance test with specified configuration.
    
    Args:
        files: List of files to process
        workers: Number of parallel workers
        test_name: Name of the test
        
    Returns:
        Performance results dictionary
    """
    print(f"🧪 Running test: {test_name}")
    print(f"   Files: {len(files)}")
    print(f"   Workers: {workers}")
    
    # Initialize batch processor
    batch_processor = BatchProcessor(
        max_workers=workers,
        config={'dry_run': True}  # Use dry run for testing
    )
    
    # Create batch job
    job_id = batch_processor.create_translation_job(files, f"perf_test_{test_name}")
    
    # Measure processing time
    start_time = time.time()
    results = batch_processor.process_job(job_id, show_progress=False)
    end_time = time.time()
    
    # Calculate metrics
    total_time = end_time - start_time
    throughput = len(files) / total_time if total_time > 0 else 0
    avg_time_per_file = total_time / len(files) if files else 0
    
    return {
        'test_name': test_name,
        'files_count': len(files),
        'workers': workers,
        'total_time': total_time,
        'throughput': throughput,
        'avg_time_per_file': avg_time_per_file,
        'results': results
    }


def run_comprehensive_benchmark() -> List[Dict]:
    """Run comprehensive performance benchmarks."""
    print("🚀 Starting comprehensive performance benchmark")
    print("=" * 60)
    
    benchmark_results = []
    
    # Test configurations
    test_configs = [
        # File count tests
        {'files': 10, 'workers': 1, 'size': 'small', 'name': 'small_10_files_1_worker'},
        {'files': 10, 'workers': 3, 'size': 'small', 'name': 'small_10_files_3_workers'},
        {'files': 50, 'workers': 1, 'size': 'small', 'name': 'small_50_files_1_worker'},
        {'files': 50, 'workers': 3, 'size': 'small', 'name': 'small_50_files_3_workers'},
        {'files': 50, 'workers': 6, 'size': 'small', 'name': 'small_50_files_6_workers'},
        
        # File size tests
        {'files': 20, 'workers': 3, 'size': 'medium', 'name': 'medium_20_files_3_workers'},
        {'files': 10, 'workers': 3, 'size': 'large', 'name': 'large_10_files_3_workers'},
        
        # Worker scaling tests
        {'files': 30, 'workers': 1, 'size': 'medium', 'name': 'scaling_1_worker'},
        {'files': 30, 'workers': 2, 'size': 'medium', 'name': 'scaling_2_workers'},
        {'files': 30, 'workers': 4, 'size': 'medium', 'name': 'scaling_4_workers'},
        {'files': 30, 'workers': 6, 'size': 'medium', 'name': 'scaling_6_workers'},
    ]
    
    for config in test_configs:
        try:
            # Create test files
            test_files = create_test_files(config['files'], config['size'])
            
            # Run performance test
            result = run_performance_test(
                test_files, 
                config['workers'], 
                config['name']
            )
            
            benchmark_results.append(result)
            
            # Cleanup test files
            if test_files:
                temp_dir = Path(test_files[0]).parent
                shutil.rmtree(temp_dir, ignore_errors=True)
            
            print(f"   ✅ Completed in {result['total_time']:.2f}s")
            print(f"   📈 Throughput: {result['throughput']:.1f} files/sec")
            print()
            
        except Exception as e:
            print(f"   ❌ Test failed: {e}")
            print()
    
    return benchmark_results


def analyze_results(results: List[Dict]) -> Dict:
    """Analyze benchmark results and generate insights."""
    print("📊 Analyzing benchmark results")
    print("=" * 60)
    
    # Group results by category
    by_workers = {}
    by_size = {}
    by_file_count = {}
    
    for result in results:
        workers = result['workers']
        if workers not in by_workers:
            by_workers[workers] = []
        by_workers[workers].append(result)
        
        # Extract size from test name
        if 'small' in result['test_name']:
            size = 'small'
        elif 'medium' in result['test_name']:
            size = 'medium'
        elif 'large' in result['test_name']:
            size = 'large'
        else:
            size = 'unknown'
        
        if size not in by_size:
            by_size[size] = []
        by_size[size].append(result)
        
        file_count = result['files_count']
        if file_count not in by_file_count:
            by_file_count[file_count] = []
        by_file_count[file_count].append(result)
    
    # Calculate statistics
    analysis = {
        'total_tests': len(results),
        'worker_analysis': {},
        'size_analysis': {},
        'scaling_analysis': {}
    }
    
    # Worker analysis
    for workers, worker_results in by_workers.items():
        throughputs = [r['throughput'] for r in worker_results]
        analysis['worker_analysis'][workers] = {
            'avg_throughput': statistics.mean(throughputs),
            'max_throughput': max(throughputs),
            'min_throughput': min(throughputs),
            'test_count': len(worker_results)
        }
    
    # Size analysis
    for size, size_results in by_size.items():
        avg_times = [r['avg_time_per_file'] for r in size_results]
        analysis['size_analysis'][size] = {
            'avg_time_per_file': statistics.mean(avg_times),
            'max_time_per_file': max(avg_times),
            'min_time_per_file': min(avg_times),
            'test_count': len(size_results)
        }
    
    # Find scaling tests (same file count, different workers)
    scaling_tests = [r for r in results if 'scaling' in r['test_name']]
    if scaling_tests:
        scaling_tests.sort(key=lambda x: x['workers'])
        analysis['scaling_analysis'] = {
            'tests': scaling_tests,
            'efficiency': []
        }
        
        # Calculate scaling efficiency
        baseline = scaling_tests[0]
        for test in scaling_tests[1:]:
            expected_speedup = test['workers'] / baseline['workers']
            actual_speedup = baseline['total_time'] / test['total_time']
            efficiency = (actual_speedup / expected_speedup) * 100
            analysis['scaling_analysis']['efficiency'].append({
                'workers': test['workers'],
                'expected_speedup': expected_speedup,
                'actual_speedup': actual_speedup,
                'efficiency': efficiency
            })
    
    return analysis


def print_analysis_report(analysis: Dict):
    """Print detailed analysis report."""
    print("📈 Performance Analysis Report")
    print("=" * 60)
    
    print(f"Total tests conducted: {analysis['total_tests']}")
    print()
    
    # Worker analysis
    print("👥 Worker Performance Analysis:")
    for workers, stats in analysis['worker_analysis'].items():
        print(f"   {workers} workers:")
        print(f"      Average throughput: {stats['avg_throughput']:.2f} files/sec")
        print(f"      Max throughput: {stats['max_throughput']:.2f} files/sec")
        print(f"      Tests: {stats['test_count']}")
    print()
    
    # Size analysis
    print("📄 File Size Analysis:")
    for size, stats in analysis['size_analysis'].items():
        print(f"   {size.capitalize()} files:")
        print(f"      Average time per file: {stats['avg_time_per_file']:.3f}s")
        print(f"      Max time per file: {stats['max_time_per_file']:.3f}s")
        print(f"      Tests: {stats['test_count']}")
    print()
    
    # Scaling analysis
    if analysis['scaling_analysis'].get('efficiency'):
        print("⚡ Scaling Efficiency Analysis:")
        for eff in analysis['scaling_analysis']['efficiency']:
            print(f"   {eff['workers']} workers:")
            print(f"      Expected speedup: {eff['expected_speedup']:.1f}x")
            print(f"      Actual speedup: {eff['actual_speedup']:.1f}x")
            print(f"      Efficiency: {eff['efficiency']:.1f}%")
        print()
    
    # Recommendations
    print("💡 Recommendations:")
    
    # Find optimal worker count
    worker_stats = analysis['worker_analysis']
    if worker_stats:
        best_workers = max(worker_stats.keys(), 
                          key=lambda w: worker_stats[w]['avg_throughput'])
        print(f"   • Optimal worker count: {best_workers} workers")
        print(f"     (Average throughput: {worker_stats[best_workers]['avg_throughput']:.2f} files/sec)")
    
    # File size recommendations
    size_stats = analysis['size_analysis']
    if 'large' in size_stats and 'small' in size_stats:
        large_time = size_stats['large']['avg_time_per_file']
        small_time = size_stats['small']['avg_time_per_file']
        ratio = large_time / small_time if small_time > 0 else 1
        print(f"   • Large files take {ratio:.1f}x longer than small files")
        print(f"     Consider breaking large documents into smaller sections")
    
    # Scaling recommendations
    if analysis['scaling_analysis'].get('efficiency'):
        avg_efficiency = statistics.mean([e['efficiency'] for e in analysis['scaling_analysis']['efficiency']])
        if avg_efficiency < 70:
            print(f"   • Scaling efficiency is {avg_efficiency:.1f}%")
            print(f"     Consider optimizing for better parallel performance")
    
    print("=" * 60)


def main():
    """Main function for performance testing."""
    parser = argparse.ArgumentParser(
        description='Performance testing for batch translation',
        formatter_class=argparse.RawDescriptionHelpFormatter
    )
    
    parser.add_argument('--quick', action='store_true',
                       help='Run quick performance test (fewer configurations)')
    parser.add_argument('--files', type=int, default=20,
                       help='Number of test files to create (for quick test)')
    parser.add_argument('--workers', type=int, default=3,
                       help='Number of workers to test (for quick test)')
    parser.add_argument('--size', choices=['small', 'medium', 'large'], default='medium',
                       help='File size category (for quick test)')
    parser.add_argument('--output', help='Save results to JSON file')
    
    args = parser.parse_args()
    
    try:
        if args.quick:
            # Run quick test
            print("🏃 Running quick performance test")
            test_files = create_test_files(args.files, args.size)
            result = run_performance_test(test_files, args.workers, 'quick_test')
            
            print(f"\n📊 Quick Test Results:")
            print(f"Files processed: {result['files_count']}")
            print(f"Workers: {result['workers']}")
            print(f"Total time: {result['total_time']:.2f}s")
            print(f"Throughput: {result['throughput']:.2f} files/sec")
            print(f"Avg time per file: {result['avg_time_per_file']:.3f}s")
            
            # Cleanup
            if test_files:
                temp_dir = Path(test_files[0]).parent
                shutil.rmtree(temp_dir, ignore_errors=True)
            
            results = [result]
        else:
            # Run comprehensive benchmark
            results = run_comprehensive_benchmark()
        
        # Analyze results
        analysis = analyze_results(results)
        print_analysis_report(analysis)
        
        # Save results if requested
        if args.output:
            import json
            output_data = {
                'results': results,
                'analysis': analysis,
                'timestamp': time.time()
            }
            
            with open(args.output, 'w', encoding='utf-8') as f:
                json.dump(output_data, f, indent=2, ensure_ascii=False)
            
            print(f"💾 Results saved to: {args.output}")
        
        return 0
        
    except KeyboardInterrupt:
        print("\n🛑 Performance test cancelled by user")
        return 1
    except Exception as e:
        print(f"❌ Performance test failed: {e}")
        import traceback
        traceback.print_exc()
        return 1


if __name__ == '__main__':
    exit(main())