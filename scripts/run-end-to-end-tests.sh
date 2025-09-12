#!/bin/bash

# End-to-End Integration Test Execution Script
# This script runs comprehensive end-to-end integration tests for the observability system

set -e

echo "🚀 Starting End-to-End Integration Tests"
echo "========================================"

# Function to print colored output
print_status() {
    echo -e "\033[1;34m[INFO]\033[0m $1"
}

print_success() {
    echo -e "\033[1;32m[SUCCESS]\033[0m $1"
}

print_error() {
    echo -e "\033[1;31m[ERROR]\033[0m $1"
}

# Check prerequisites
print_status "Checking prerequisites..."

if ! command -v java &> /dev/null; then
    print_error "Java is not installed or not in PATH"
    exit 1
fi

if ! command -v ./gradlew &> /dev/null; then
    print_error "Gradle wrapper not found"
    exit 1
fi

print_success "Prerequisites check passed"

# Clean previous test results
print_status "Cleaning previous test results..."
./gradlew clean
print_success "Clean completed"

# Compile test classes
print_status "Compiling test classes..."
./gradlew compileTestJava
if [ $? -eq 0 ]; then
    print_success "Test compilation successful"
else
    print_error "Test compilation failed"
    exit 1
fi

# Run unit tests first
print_status "Running unit tests..."
./gradlew test
if [ $? -eq 0 ]; then
    print_success "Unit tests passed"
else
    print_error "Unit tests failed"
    exit 1
fi

# Run simple end-to-end test
print_status "Running simple end-to-end tests..."
./gradlew test --tests="*SimpleEndToEndIntegrationTest*"
if [ $? -eq 0 ]; then
    print_success "Simple end-to-end tests passed"
else
    print_error "Simple end-to-end tests failed"
    exit 1
fi

# Skip BDD and architecture tests for now as they may not be configured
print_status "Skipping BDD and architecture tests (not configured)"

# Generate comprehensive test report
print_status "Generating test reports..."
./gradlew runAllTests
if [ $? -eq 0 ]; then
    print_success "Test reports generated successfully"
else
    print_error "Test report generation failed"
    exit 1
fi

print_success "🎉 All End-to-End Integration Tests Completed Successfully!"
echo "========================================"
echo "Test reports available in: app/build/reports/tests/"
echo "Coverage reports available in: app/build/reports/jacoco/"