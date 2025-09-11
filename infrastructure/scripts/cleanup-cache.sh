#!/bin/bash

# Cache cleanup script for CDK infrastructure project
# This script helps resolve ts-node cache issues and other build problems

set -e

echo "🧹 Starting cache cleanup..."

# Function to print colored output
print_status() {
    echo -e "\033[1;34m$1\033[0m"
}

print_success() {
    echo -e "\033[1;32m✅ $1\033[0m"
}

print_warning() {
    echo -e "\033[1;33m⚠️  $1\033[0m"
}

print_error() {
    echo -e "\033[1;31m❌ $1\033[0m"
}

# Clean TypeScript build cache
print_status "Cleaning TypeScript build cache..."
if [ -f "tsconfig.tsbuildinfo" ]; then
    rm -f tsconfig.tsbuildinfo
    print_success "Removed tsconfig.tsbuildinfo"
else
    print_warning "tsconfig.tsbuildinfo not found"
fi

# Clean ts-node cache
print_status "Cleaning ts-node cache..."
if [ -d "node_modules/.cache/ts-node" ]; then
    rm -rf node_modules/.cache/ts-node/
    print_success "Removed ts-node cache"
else
    print_warning "ts-node cache not found"
fi

# Clean Jest cache
print_status "Cleaning Jest cache..."
if [ -d ".jest-cache" ]; then
    rm -rf .jest-cache/
    print_success "Removed Jest cache"
else
    print_warning "Jest cache not found"
fi

# Clean CDK output
print_status "Cleaning CDK output..."
if [ -d "cdk.out" ]; then
    rm -rf cdk.out/
    print_success "Removed cdk.out directory"
else
    print_warning "cdk.out directory not found"
fi

# Clean compiled JavaScript files
print_status "Cleaning compiled JavaScript files..."
find . -name "*.js" -not -path "./node_modules/*" -not -path "./scripts/*" -delete 2>/dev/null || true
find . -name "*.d.ts" -not -path "./node_modules/*" -delete 2>/dev/null || true
print_success "Removed compiled JavaScript and declaration files"

# Clean npm cache (optional)
if [ "$1" = "--deep" ]; then
    print_status "Performing deep clean (npm cache)..."
    npm cache clean --force
    print_success "Cleaned npm cache"
fi

# Reinstall dependencies if requested
if [ "$1" = "--reinstall" ] || [ "$2" = "--reinstall" ]; then
    print_status "Reinstalling dependencies..."
    rm -rf node_modules/
    npm install
    print_success "Reinstalled dependencies"
fi

print_success "Cache cleanup completed!"

# Provide next steps
echo ""
print_status "Next steps:"
echo "1. Run 'npm run build' to recompile TypeScript"
echo "2. Run 'npm run synth' to test CDK synthesis"
echo "3. If issues persist, try 'npm run troubleshoot'"
echo ""
echo "Available cleanup options:"
echo "  ./scripts/cleanup-cache.sh --deep      # Also clean npm cache"
echo "  ./scripts/cleanup-cache.sh --reinstall # Also reinstall node_modules"