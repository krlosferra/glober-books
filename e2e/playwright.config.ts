import { execFileSync } from 'node:child_process';
import { defineConfig, devices } from '@playwright/test';

// Playwright probes the readiness URL before it starts the launcher and waits on a listener that
// never answers, so a busy port is checked here first, in the runner process only (SC-33).
if (process.env.TEST_WORKER_INDEX === undefined) {
  try {
    execFileSync(process.execPath, ['support/start-server.mjs', '--check-port'], { cwd: __dirname, stdio: 'pipe' });
  } catch {
    throw new Error('port 18080 on 127.0.0.1 is already in use; stop that process first');
  }
}

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
  // The hostile-environment test runs only on request: `npx playwright test --grep @isolation`.
  grepInvert: process.argv.includes('@isolation') ? undefined : /@isolation/,
  projects: [
    { name: 'desktop', use: { ...devices['Desktop Chrome'] } },
    {
      name: 'mobile',
      use: { ...devices['Desktop Chrome'], viewport: { width: 360, height: 740 } },
      grep: /@mobile/,
    },
  ],
});
