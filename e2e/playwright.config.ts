import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  globalSetup: './global-setup.ts',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: 'http://127.0.0.1:18080',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'off',
  },
  webServer: {
    command: 'node support/start-server.mjs',
    url: 'http://127.0.0.1:18080/api/v1/books',
    reuseExistingServer: false,
    timeout: 90_000,
    gracefulShutdown: { signal: 'SIGTERM', timeout: 10_000 },
  },
  grepInvert: /@isolation/,
  projects: [
    { name: 'desktop', use: { ...devices['Desktop Chrome'] } },
    {
      name: 'mobile',
      use: { ...devices['Desktop Chrome'], viewport: { width: 360, height: 740 } },
      grep: /@mobile/,
    },
  ],
});
