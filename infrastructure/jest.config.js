module.exports = {
  preset: 'ts-jest',
  testEnvironment: 'node',
  roots: ['<rootDir>/test', '<rootDir>/src'],
  testMatch: ['**/*.test.ts'],
  transform: {
    '^.+\\.tsx?$': ['ts-jest', {
      tsconfig: 'tsconfig.json',
    }],
  },
  collectCoverageFrom: [
    'src/**/*.{ts,tsx}',
    '!src/**/*.d.ts',
  ],
  coverageDirectory: 'coverage',
  coverageReporters: [
    'text',
    'lcov',
    'html',
  ],
  moduleFileExtensions: [
    'ts',
    'tsx',
    'js',
    'jsx',
    'json',
    'node',
  ],
  testTimeout: 60000,
  setupFilesAfterEnv: ['<rootDir>/test/setup.ts'],
  maxWorkers: 1,
  moduleNameMapper: {
    '^../lib/(.*)$': '<rootDir>/src/$1',
    '^../../lib/(.*)$': '<rootDir>/src/$1',
    '^../src/(.*)$': '<rootDir>/src/$1',
    '^../../src/(.*)$': '<rootDir>/src/$1',
  },
};