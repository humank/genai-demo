import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
    testDir: '.',
    testMatch: '**/*.spec.ts',
    fullyParallel: true,
    forbidOnly: !!process.env.CI,
    retries: process.env.CI ? 2 : 0,
    workers: process.env.CI ? 1 : undefined,
    reporter: 'html',
    timeout: 30_000,

    use: {
        trace: 'on-first-retry',
        screenshot: 'only-on-failure',
    },

    projects: [
        {
            name: 'consumer',
            use: {
                ...devices['Desktop Chrome'],
                baseURL: 'http://localhost:3000',
            },
            testMatch: 'consumer/**/*.spec.ts',
        },
        {
            name: 'cmc',
            use: {
                ...devices['Desktop Chrome'],
                baseURL: 'http://localhost:3002',
            },
            testMatch: 'cmc/**/*.spec.ts',
        },
    ],

    webServer: [
        {
            command: 'pnpm --filter @repo/consumer dev',
            port: 3000,
            cwd: '..',
            reuseExistingServer: !process.env.CI,
            timeout: 120_000,
        },
        {
            command: 'pnpm --filter @repo/cmc dev',
            port: 3002,
            cwd: '..',
            reuseExistingServer: !process.env.CI,
            timeout: 120_000,
        },
    ],
})
