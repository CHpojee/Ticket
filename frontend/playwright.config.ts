import { defineConfig, devices } from '@playwright/test';

/**
 * E2E config. Boots the Spring Boot backend (H2, freshly seeded) and the Next.js dev
 * server, then runs headless Chromium against the critical user flows.
 */
export default defineConfig({
  testDir: './tests/e2e',
  fullyParallel: false,
  workers: 1,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: 'http://localhost:3000',
    headless: true,
    trace: 'on-first-retry',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],
  webServer: [
    {
      command: './mvnw -q spring-boot:run',
      cwd: '../backend',
      url: 'http://localhost:8080/api/health',
      timeout: 180_000,
      reuseExistingServer: !process.env.CI,
      stdout: 'ignore',
      stderr: 'pipe',
    },
    {
      command: 'npm run dev',
      url: 'http://localhost:3000',
      timeout: 120_000,
      reuseExistingServer: !process.env.CI,
    },
  ],
});
