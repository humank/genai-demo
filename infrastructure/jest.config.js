module.exports = {
  preset: 'ts-jest',
  testEnvironment: 'node',
  roots: ['<rootDir>/test'],
  testMatch: ['**/*.test.ts'],
  transform: {
    '^.+\\.tsx?$': 'ts-jest'
  },
  collectCoverageFrom: [
    'lib/**/*.ts',
    '!lib/**/*.d.ts',
    '!lib/**/*.js'
  ],
  coverageDirectory: 'coverage',
  coverageReporters: ['text', 'lcov', 'html', 'json', 'cobertura'],
  coverageThreshold: {
    global: {
      branches: 70,
      functions: 70,
      lines: 70,
      statements: 70
    }
  },
  setupFilesAfterEnv: ['<rootDir>/test/setup.ts'],
  testTimeout: 60000, // Increased for comprehensive validation
  verbose: true,
  moduleFileExtensions: ['ts', 'tsx', 'js', 'jsx', 'json', 'node'],
  extensionsToTreatAsEsm: [],
  globals: {
    'ts-jest': {
      useESM: false
    }
  },
  // Enhanced test reporting for CI/CD
  reporters: [
    'default',
    ['jest-junit', {
      outputDirectory: 'test-results',
      outputName: 'junit.xml',
      classNameTemplate: '{classname}',
      titleTemplate: '{title}',
      ancestorSeparator: ' › ',
      usePathForSuiteName: true
    }]
  ],
  // Test result processor for enhanced reporting
  testResultsProcessor: '<rootDir>/test/utils/test-results-processor.js',
  // Snapshot serializer for better snapshot testing
  snapshotSerializers: ['<rootDir>/test/utils/snapshot-serializer.js'],
  // Test environment options
  testEnvironmentOptions: {
    NODE_ENV: 'test'
  },
  // Maximum worker processes for parallel testing
  maxWorkers: '50%',
  // Cache directory
  cacheDirectory: '<rootDir>/.jest-cache',
  // Clear mocks between tests
  clearMocks: true,
  // Collect coverage from additional files
  collectCoverageFrom: [
    'lib/**/*.ts',
    'scripts/**/*.js',
    '!lib/**/*.d.ts',
    '!lib/**/*.js',
    '!**/node_modules/**',
    '!**/coverage/**',
    '!**/test/**'
  ],
  // Coverage thresholds per directory
  coverageThreshold: {
    global: {
      branches: 70,
      functions: 70,
      lines: 70,
      statements: 70
    },
    './lib/stacks/': {
      branches: 80,
      functions: 80,
      lines: 80,
      statements: 80
    }
  }
};
