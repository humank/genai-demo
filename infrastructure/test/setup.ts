// Test setup file
// Add any global test configuration here

// Suppress CDK deprecation warnings in tests
process.env.JSII_DEPRECATED = 'quiet';

// Set default AWS region for tests
process.env.CDK_DEFAULT_REGION = 'us-east-1';
process.env.CDK_DEFAULT_ACCOUNT = '123456789012';

// Increase Jest timeout for CDK synthesis
jest.setTimeout(60000);

// Mock console.warn to reduce noise in tests
const originalWarn = console.warn;
console.warn = (...args: any[]) => {
    // Suppress specific CDK warnings that are expected in tests
    const message = args[0];
    if (typeof message === 'string') {
        if (message.includes('deprecated') ||
            message.includes('CDK_DEFAULT_') ||
            message.includes('context provider')) {
            return;
        }
    }
    originalWarn.apply(console, args);
};