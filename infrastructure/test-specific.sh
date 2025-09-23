#!/bin/bash

# Test specific test files
echo "Running consolidated stack tests..."
npm test -- --testPathPatterns="consolidated-stack.test.ts"

echo "Running security stack tests..."
npm test -- --testPathPatterns="security-stack.test.ts"

echo "Running network stack tests..."
npm test -- --testPathPatterns="network-stack.test.ts"

echo "Running unit tests..."
npm test -- --testPathPatterns="unit/"

echo "Running CDK synthesis test..."
npm run build && npm run synth