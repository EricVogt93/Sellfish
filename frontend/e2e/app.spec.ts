import { test, expect, type Page } from '@playwright/test'

const TOKENS = {
	accessToken: 'test-access',
	refreshToken: 'test-refresh',
	tokenType: 'Bearer',
	expiresInSeconds: 1800
}
const ME = { id: 'u-1', email: 'tester@example.com', role: 'USER', locale: 'de-DE' }

function sampleMatch(over: Record<string, unknown> = {}) {
	return {
		matchId: 'm-1',
		jobId: 'j-1',
		title: 'Senior Backend Engineer',
		company: 'Acme GmbH',
		location: 'Berlin',
		url: 'https://example.com/job',
		salaryRaw: '€80k',
		postedAt: new Date().toISOString(),
		score: 0.92,
		rank: 1,
		status: 'NEW',
		scoreBreakdown: JSON.stringify({
			features: { semantic: 0.9, title: 1, keyword: 0.6, location: 1, recency: 0.8, remote: 0.5 },
			weights: {},
			total: 0.92
		}),
		description: 'Own the platform.',
		source: 'ARBEITNOW',
		remote: 'REMOTE',
		...over
	}
}

async function mockBackend(page: Page, matches: unknown[]) {
	await page.route('**/api/auth/login', (r) =>
		r.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(TOKENS) })
	)
	await page.route('**/api/me', (r) =>
		r.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(ME) })
	)
	await page.route('**/api/matches**', (r) =>
		r.fulfill({
			status: 200,
			contentType: 'application/json',
			body: JSON.stringify({
				content: matches,
				totalElements: matches.length,
				totalPages: 1,
				number: 0
			})
		})
	)
	await page.route('**/api/documents', (r) =>
		r.fulfill({ status: 200, contentType: 'application/json', body: '[]' })
	)
}

test('shows the login screen when unauthenticated', async ({ page }) => {
	await page.goto('/')
	await expect(page.getByRole('heading', { name: 'Sign in' })).toBeVisible()
	await expect(page.getByLabel('Email')).toBeVisible()
	await expect(page.getByLabel('Password')).toBeVisible()
})

test('logs in and shows the empty jobs state', async ({ page }) => {
	await mockBackend(page, [])
	await page.goto('/')

	await page.getByLabel('Email').fill('tester@example.com')
	await page.getByLabel('Password').fill('supersecret1')
	await page.getByRole('button', { name: 'Sign in' }).click()

	// Topbar-Navigation erscheint nach Login
	await expect(page.getByRole('button', { name: /Jobs/ })).toBeVisible()
	await expect(page.getByRole('heading', { name: 'Jobs' })).toBeVisible()
	await expect(page.getByText('No matches yet.', { exact: false })).toBeVisible()
})

test('renders a real match row in the jobs table', async ({ page }) => {
	await mockBackend(page, [sampleMatch()])
	await page.goto('/')

	await page.getByLabel('Email').fill('tester@example.com')
	await page.getByLabel('Password').fill('supersecret1')
	await page.getByRole('button', { name: 'Sign in' }).click()

	await expect(page.getByText('Senior Backend Engineer')).toBeVisible()
	await expect(page.getByText('Acme GmbH')).toBeVisible()
	// Score 0.92 → als 92 im Match-Ring
	await expect(page.getByText('92', { exact: true }).first()).toBeVisible()
})
