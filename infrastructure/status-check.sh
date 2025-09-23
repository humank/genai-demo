#!/bin/bash

# CDK Infrastructure Status Check Script
# This script provides a quick overview of the infrastructure status

echo "🏗️  GenAI Demo Infrastructure Status Check"
echo "=========================================="
echo

# Check Node.js version
echo "📋 Environment Check:"
echo "Node.js version: $(node --version)"
echo "NPM version: $(npm --version)"
echo "CDK version: $(npx cdk --version 2>/dev/null || echo 'CDK not found')"
echo

# Check if dependencies are installed
if [ -d "node_modules" ]; then
    echo "✅ Dependencies installed"
else
    echo "❌ Dependencies not installed - run 'npm install'"
fi
echo

# Run quick tests
echo "🧪 Running Quick Tests..."
npm run test:quick --silent
TEST_EXIT_CODE=$?

if [ $TEST_EXIT_CODE -eq 0 ]; then
    echo "✅ Quick tests passed"
else
    echo "❌ Quick tests failed"
fi
echo

# Check CDK synthesis
echo "🔧 Checking CDK Synthesis..."
npx cdk synth --context enableCdkNag=false > /dev/null 2>&1
SYNTH_EXIT_CODE=$?

if [ $SYNTH_EXIT_CODE -eq 0 ]; then
    echo "✅ CDK synthesis successful"
    echo "📦 Available stacks:"
    npx cdk list --context enableCdkNag=false 2>/dev/null | sed 's/^/   - /'
else
    echo "❌ CDK synthesis failed"
fi
echo

# Summary
echo "📊 Summary:"
if [ $TEST_EXIT_CODE -eq 0 ] && [ $SYNTH_EXIT_CODE -eq 0 ]; then
    echo "✅ Infrastructure is ready for deployment"
    echo
    echo "🚀 Next steps:"
    echo "   - Run full tests: npm test"
    echo "   - Deploy to dev: ./deploy-consolidated.sh"
    echo "   - View documentation: cat README.md"
else
    echo "❌ Infrastructure needs attention"
    echo
    echo "🔧 Troubleshooting:"
    if [ $TEST_EXIT_CODE -ne 0 ]; then
        echo "   - Fix failing tests: npm test"
    fi
    if [ $SYNTH_EXIT_CODE -ne 0 ]; then
        echo "   - Check CDK code: npm run build"
    fi
fi
echo