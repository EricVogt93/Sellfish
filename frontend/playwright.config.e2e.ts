import { defineConfig, devices } from '@playwright/test'

// Real E2E against the running dev stack (frontend :5173 proxies /api -> backend :8080).
// The frontend dev server + backend are started externally; this config only drives the browser.
export default defineConfig({
	testDir: 'e2e',
	timeout: 60_000,
	fullyParallel: false,
	retries: 0,
	reporter: [['list'], ['html', { outputFolder: 'e2e-report' }]],
	use: {
		baseURL: 'http://localhost:5173',
		trace: 'retain-on-failure',
		screenshot: 'only-on-failure',
		video: 'retain-on-failure'
	},
	projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }]
})
