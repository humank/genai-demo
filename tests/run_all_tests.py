#!/usr/bin/env python3
"""
Test runner for the automated documentation translation system.

This script runs all unit tests and integration tests, providing
comprehensive test coverage and reporting.
"""

import unittest
import sys
import os
import time
from pathlib import Path
from io import StringIO

# Add scripts directory to path for imports
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'scripts'))

def discover_and_run_tests(test_directory=None, pattern='test_*.py', verbosity=2):
    """
    Discover and run all tests in the specified directory.
    
    Args:
        test_directory: Directory to search for tests (default: current directory)
        pattern: Pattern to match test files (default: 'test_*.py')
        verbosity: Test output verbosity level (default: 2)
    
    Returns:
        TestResult object with test results
    """
    if test_directory is None:
        test_directory = os.path.dirname(__file__)
    
    # Discover tests
    loader = unittest.TestLoader()
    suite = loader.discover(test_directory, pattern=pattern)
    
    # Create test runner with custom result class for better reporting
    runner = unittest.TextTestRunner(
        verbosity=verbosity,
        stream=sys.stdout,
        buffer=True,
        resultclass=DetailedTestResult
    )
    
    # Run tests
    print(f"Running tests from {test_directory} with pattern '{pattern}'")
    print("=" * 70)
    
    start_time = time.time()
    result = runner.run(suite)
    end_time = time.time()
    
    # Print summary
    print("\n" + "=" * 70)
    print(f"Test run completed in {end_time - start_time:.2f} seconds")
    print(f"Tests run: {result.testsRun}")
    print(f"Failures: {len(result.failures)}")
    print(f"Errors: {len(result.errors)}")
    print(f"Skipped: {len(result.skipped)}")
    
    if result.failures:
        print(f"\nFAILURES ({len(result.failures)}):")
        for test, traceback in result.failures:
            print(f"  - {test}: {traceback.split('AssertionError:')[-1].strip()}")
    
    if result.errors:
        print(f"\nERRORS ({len(result.errors)}):")
        for test, traceback in result.errors:
            print(f"  - {test}: {traceback.split('Exception:')[-1].strip()}")
    
    success_rate = ((result.testsRun - len(result.failures) - len(result.errors)) / 
                   max(result.testsRun, 1)) * 100
    print(f"\nSuccess rate: {success_rate:.1f}%")
    
    return result

class DetailedTestResult(unittest.TextTestResult):
    """Enhanced test result class with detailed reporting."""
    
    def __init__(self, stream, descriptions, verbosity):
        super().__init__(stream, descriptions, verbosity)
        self.test_start_time = None
        self.slow_tests = []
        self.test_times = {}
    
    def startTest(self, test):
        super().startTest(test)
        self.test_start_time = time.time()
    
    def stopTest(self, test):
        super().stopTest(test)
        if self.test_start_time:
            test_time = time.time() - self.test_start_time
            self.test_times[str(test)] = test_time
            
            # Track slow tests (> 5 seconds)
            if test_time > 5.0:
                self.slow_tests.append((str(test), test_time))
    
    def printErrors(self):
        super().printErrors()
        
        # Print slow test warnings
        if self.slow_tests:
            self.stream.writeln("\nSLOW TESTS (>5s):")
            for test_name, test_time in sorted(self.slow_tests, key=lambda x: x[1], reverse=True):
                self.stream.writeln(f"  {test_name}: {test_time:.2f}s")

def run_specific_test_module(module_name, verbosity=2):
    """
    Run tests from a specific module.
    
    Args:
        module_name: Name of the test module (e.g., 'test_kiro_translator')
        verbosity: Test output verbosity level
    
    Returns:
        TestResult object
    """
    try:
        # Import the test module
        test_module = __import__(module_name)
        
        # Create test suite from module
        loader = unittest.TestLoader()
        suite = loader.loadTestsFromModule(test_module)
        
        # Run tests
        runner = unittest.TextTestRunner(verbosity=verbosity, resultclass=DetailedTestResult)
        return runner.run(suite)
    
    except ImportError as e:
        print(f"Error importing test module '{module_name}': {e}")
        return None

def run_test_categories():
    """Run tests by category (unit tests, integration tests, etc.)."""
    categories = {
        'Unit Tests': 'test_*.py',
        'Integration Tests': 'test_integration.py',
        'Batch Processing Tests': 'test_batch_processor.py'
    }
    
    results = {}
    total_tests = 0
    total_failures = 0
    total_errors = 0
    
    for category, pattern in categories.items():
        print(f"\n{'='*20} {category} {'='*20}")
        
        if pattern == 'test_integration.py':
            # Run integration tests separately
            result = run_specific_test_module('test_integration')
        else:
            result = discover_and_run_tests(pattern=pattern, verbosity=1)
        
        if result:
            results[category] = result
            total_tests += result.testsRun
            total_failures += len(result.failures)
            total_errors += len(result.errors)
    
    # Print overall summary
    print(f"\n{'='*60}")
    print("OVERALL TEST SUMMARY")
    print(f"{'='*60}")
    print(f"Total tests run: {total_tests}")
    print(f"Total failures: {total_failures}")
    print(f"Total errors: {total_errors}")
    
    overall_success_rate = ((total_tests - total_failures - total_errors) / 
                           max(total_tests, 1)) * 100
    print(f"Overall success rate: {overall_success_rate:.1f}%")
    
    # Print category breakdown
    print(f"\nCategory Breakdown:")
    for category, result in results.items():
        if result:
            success_rate = ((result.testsRun - len(result.failures) - len(result.errors)) / 
                           max(result.testsRun, 1)) * 100
            print(f"  {category}: {result.testsRun} tests, {success_rate:.1f}% success")
    
    return overall_success_rate >= 90.0  # Return True if 90%+ success rate

def generate_test_report(output_file='test_report.html'):
    """Generate an HTML test report."""
    # Run all tests and capture results
    original_stdout = sys.stdout
    test_output = StringIO()
    sys.stdout = test_output
    
    try:
        result = discover_and_run_tests(verbosity=2)
        test_output_str = test_output.getvalue()
    finally:
        sys.stdout = original_stdout
    
    # Generate HTML report
    html_content = f"""
<!DOCTYPE html>
<html>
<head>
    <title>Translation System Test Report</title>
    <style>
        body {{ font-family: Arial, sans-serif; margin: 20px; }}
        .header {{ background-color: #f0f0f0; padding: 20px; border-radius: 5px; }}
        .summary {{ margin: 20px 0; }}
        .success {{ color: green; }}
        .failure {{ color: red; }}
        .error {{ color: orange; }}
        .test-output {{ background-color: #f8f8f8; padding: 15px; border-radius: 5px; 
                       font-family: monospace; white-space: pre-wrap; }}
        table {{ border-collapse: collapse; width: 100%; }}
        th, td {{ border: 1px solid #ddd; padding: 8px; text-align: left; }}
        th {{ background-color: #f2f2f2; }}
    </style>
</head>
<body>
    <div class="header">
        <h1>Automated Documentation Translation System - Test Report</h1>
        <p>Generated on: {time.strftime('%Y-%m-%d %H:%M:%S')}</p>
    </div>
    
    <div class="summary">
        <h2>Test Summary</h2>
        <table>
            <tr><th>Metric</th><th>Value</th></tr>
            <tr><td>Tests Run</td><td>{result.testsRun}</td></tr>
            <tr><td class="success">Passed</td><td>{result.testsRun - len(result.failures) - len(result.errors)}</td></tr>
            <tr><td class="failure">Failures</td><td>{len(result.failures)}</td></tr>
            <tr><td class="error">Errors</td><td>{len(result.errors)}</td></tr>
            <tr><td>Success Rate</td><td>{((result.testsRun - len(result.failures) - len(result.errors)) / max(result.testsRun, 1)) * 100:.1f}%</td></tr>
        </table>
    </div>
    
    <div class="test-output">
        <h2>Test Output</h2>
        {test_output_str}
    </div>
</body>
</html>
"""
    
    # Write HTML report
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(html_content)
    
    print(f"Test report generated: {output_file}")
    return output_file

def main():
    """Main function for running tests."""
    import argparse
    
    parser = argparse.ArgumentParser(description='Run translation system tests')
    parser.add_argument('--category', choices=['unit', 'integration', 'all'], 
                       default='all', help='Test category to run')
    parser.add_argument('--module', help='Specific test module to run')
    parser.add_argument('--pattern', default='test_*.py', 
                       help='Pattern to match test files')
    parser.add_argument('--verbosity', type=int, default=2, 
                       help='Test output verbosity (0-2)')
    parser.add_argument('--report', help='Generate HTML report to specified file')
    parser.add_argument('--fail-fast', action='store_true', 
                       help='Stop on first failure')
    
    args = parser.parse_args()
    
    # Set up test environment
    os.environ['TESTING'] = '1'
    
    try:
        if args.report:
            # Generate HTML report
            generate_test_report(args.report)
            return
        
        if args.module:
            # Run specific module
            result = run_specific_test_module(args.module, args.verbosity)
            success = result and result.wasSuccessful()
        elif args.category == 'unit':
            # Run unit tests only
            result = discover_and_run_tests(pattern='test_*.py', verbosity=args.verbosity)
            success = result.wasSuccessful()
        elif args.category == 'integration':
            # Run integration tests only
            result = run_specific_test_module('test_integration', args.verbosity)
            success = result and result.wasSuccessful()
        elif args.category == 'all':
            # Run all tests by category
            success = run_test_categories()
        else:
            # Run with custom pattern
            result = discover_and_run_tests(pattern=args.pattern, verbosity=args.verbosity)
            success = result.wasSuccessful()
        
        # Exit with appropriate code
        sys.exit(0 if success else 1)
    
    except KeyboardInterrupt:
        print("\nTest run interrupted by user")
        sys.exit(1)
    except Exception as e:
        print(f"Error running tests: {e}")
        sys.exit(1)

if __name__ == '__main__':
    main()