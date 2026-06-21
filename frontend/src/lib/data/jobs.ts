// ── Sellfish · Daten + Score-/Dekorations-Logik (1:1 aus Sellfish.dc.html) ──
// Self-contained Mock-Daten und reine Helfer. Keine Backend-Abhängigkeit.

export interface RawJob {
	id: number
	title: string
	company: string
	initials: string
	location: string
	salary: string
	posted: string
	source: string
	hue: number
	score: number
	type: string
	seniority: string
	appliedAgo?: string
	blurb: string
	f: Record<string, number>
}

export type FilterDef = [id: string, label: string, short: string]

export const FILTERS: FilterDef[] = [
	['semantic', 'Profile match', 'Profile'],
	['title', 'Title match', 'Title'],
	['keyword', 'Keyword match', 'Keyword'],
	['location', 'Location fit', 'Location'],
	['recency', 'Recently posted', 'Recent'],
	['remote', 'Remote fit', 'Remote']
]

export const JOBS_RAW: RawJob[] = [
	{
		id: 1,
		title: 'Senior SDET / Test Automation',
		company: 'Qonto',
		initials: 'QO',
		location: 'Remote · EU',
		salary: '€80–95k',
		posted: '2d',
		source: 'Greenhouse',
		hue: 262,
		score: 92,
		type: 'Remote',
		seniority: 'Senior',
		appliedAgo: '2d',
		blurb:
			'Own the end-to-end automation strategy across web and API. Cypress + Playwright, TypeScript and a strong CI/CD culture — a near-perfect overlap with your profile and history.',
		f: { semantic: 0.96, title: 0.9, keyword: 0.88, location: 0.82, recency: 0.7, remote: 1 }
	},
	{
		id: 2,
		title: 'Full-Stack Engineer (Java/Svelte)',
		company: 'Pitch',
		initials: 'PI',
		location: 'Berlin · Hybrid',
		salary: '€75–88k',
		posted: '3d',
		source: 'Lever',
		hue: 210,
		score: 88,
		type: 'Hybrid',
		seniority: 'Mid–Senior',
		appliedAgo: '5d',
		blurb:
			'Build product features across a Spring Boot back-end and a Svelte front-end. Matches your full-stack range and your interest in modern web tooling.',
		f: { semantic: 0.9, title: 0.85, keyword: 0.8, location: 0.6, recency: 0.55, remote: 1 }
	},
	{
		id: 3,
		title: 'QA Automation Engineer (Cypress)',
		company: 'GitLab',
		initials: 'GL',
		location: 'Remote · Worldwide',
		salary: '€70–85k',
		posted: '1w',
		source: 'Ashby',
		hue: 24,
		score: 84,
		type: 'Remote',
		seniority: 'Mid',
		appliedAgo: '1w',
		blurb:
			'All-remote, async culture. Build and maintain Cypress/Playwright suites for a large open-source product. Strong DevEx and pipeline focus.',
		f: { semantic: 0.88, title: 0.7, keyword: 0.82, location: 0.9, recency: 0.4, remote: 1 }
	},
	{
		id: 4,
		title: 'Software Engineer in Test',
		company: 'Personio',
		initials: 'PE',
		location: 'Remote · DACH',
		salary: '€72–84k',
		posted: '4d',
		source: 'SmartRecruiters',
		hue: 160,
		score: 79,
		type: 'Remote',
		seniority: 'Senior',
		blurb:
			'Embed with product squads to raise quality through automation and tooling. DACH-focused, remote-friendly HR platform.',
		f: { semantic: 0.8, title: 0.75, keyword: 0.65, location: 0.5, recency: 0.6, remote: 0.9 }
	},
	{
		id: 5,
		title: 'Backend Engineer (Spring Boot)',
		company: 'Celonis',
		initials: 'CE',
		location: 'Munich · Remote',
		salary: '€78–92k',
		posted: '6d',
		source: 'Greenhouse',
		hue: 288,
		score: 74,
		type: 'Remote',
		seniority: 'Senior',
		blurb:
			'Service-oriented back-end work on a process-mining platform. Heavier on back-end than testing, but a strong Spring Boot overlap.',
		f: { semantic: 0.72, title: 0.55, keyword: 0.6, location: 0.45, recency: 0.3, remote: 0.8 }
	},
	{
		id: 6,
		title: 'Platform / DevOps Engineer',
		company: 'Hetzner',
		initials: 'HE',
		location: 'Remote · DE',
		salary: '€68–80k',
		posted: '1w',
		source: 'Workable',
		hue: 6,
		score: 68,
		type: 'Remote',
		seniority: 'Mid–Senior',
		blurb:
			'CI/CD, infrastructure and developer experience. Adjacent to your strengths, lighter on hands-on test automation.',
		f: { semantic: 0.66, title: 0.4, keyword: 0.5, location: 0.7, recency: 0.5, remote: 0.9 }
	},
	{
		id: 7,
		title: 'Test Engineer (Mobile)',
		company: 'N26',
		initials: 'N2',
		location: 'Remote · EU',
		salary: '€60–72k',
		posted: '3d',
		source: 'Lever',
		hue: 190,
		score: 63,
		type: 'Remote',
		seniority: 'Mid',
		appliedAgo: '3d',
		blurb:
			'Mobile QA automation for a fintech app. Some domain shift toward mobile, but a solid remote + automation fit.',
		f: { semantic: 0.6, title: 0.45, keyword: 0.4, location: 0.55, recency: 0.6, remote: 1 }
	},
	{
		id: 8,
		title: 'Lead QA Engineer',
		company: 'Trivago',
		initials: 'TR',
		location: 'Remote · DE',
		salary: '€85–98k',
		posted: '2w',
		source: 'Recruitee',
		hue: 48,
		score: 58,
		type: 'Remote',
		seniority: 'Lead',
		appliedAgo: '6d',
		blurb:
			'Lead a small QA team and set automation direction. More management than your current IC focus, but strong comp.',
		f: { semantic: 0.58, title: 0.5, keyword: 0.35, location: 0.4, recency: 0.2, remote: 0.9 }
	},
	{
		id: 9,
		title: 'Junior Automation Engineer',
		company: 'Sennder',
		initials: 'SE',
		location: 'Remote · EU',
		salary: '€48–58k',
		posted: '3w',
		source: 'Ashby',
		hue: 330,
		score: 41,
		type: 'Remote',
		seniority: 'Junior',
		appliedAgo: '2w',
		blurb:
			'Entry-level automation role. Below your seniority and salary floor — surfaced for completeness.',
		f: { semantic: 0.42, title: 0.3, keyword: 0.45, location: 0.6, recency: 0.7, remote: 0.8 }
	}
]

export const MATCHES_DAILY = [
	4, 6, 3, 8, 5, 7, 9, 6, 11, 8, 5, 7, 10, 12, 9, 6, 8, 14, 11, 7, 9, 13, 10, 8, 12, 15, 11, 9, 14,
	17
]
export const GENS_DAILY = [
	1, 2, 0, 3, 2, 1, 4, 2, 3, 5, 2, 1, 3, 4, 2, 3, 1, 5, 4, 2, 3, 6, 4, 2, 5, 7, 3, 4, 6, 8
]

export const APPLIED_STATUSES = ['applied', 'viewed', 'interview', 'offer', 'rejected']

export function scoreColor(s: number): string {
	return s >= 75 ? '#10b981' : s >= 50 ? '#f59e0b' : '#ef4444'
}

// ── AI-Textgeneratoren (Cover/Motivation/CV/Application/Interview/Company) ──

export function docText(job: RawJob, doc: string): string {
	const c = job.company,
		t = job.title
	if (doc === 'cover')
		return `Dear Hiring Team at ${c},\n\nI'm writing to apply for the ${t} role. Over the past decade I've built and scaled test automation frameworks with Cypress and Playwright, shipped full-stack features across Spring Boot and Svelte, and worked to close the gap between development and quality.\n\nWhat draws me to ${c} is your treatment of engineering quality as a first-class concern rather than an afterthought. I'd bring hands-on experience designing maintainable, developer-friendly automation, a strong CI/CD background, and a pragmatic, ownership-driven approach.\n\nI'd welcome the chance to discuss how I can help your team move faster with confidence.\n\nBest regards,\nEric Vogt`
	if (doc === 'motivation')
		return `Why ${t} at ${c}\n\nI care about software that is correct, observable, and a pleasure to work on. ${c} stood out because the role sits exactly at the intersection of automation, developer experience, and full-stack delivery — the work I'm strongest at and most energised by.\n\nI'm looking for a remote-first team where quality is shared, not siloed, and where I can keep growing toward test architecture and platform work.`
	if (doc === 'cv')
		return `Eric Vogt — SDET & Software Engineer\nStadtroda, DE · Remote\n\nSummary\nSDET with 10+ years in test automation and full-stack development. Cypress, Playwright, Spring Boot, Svelte, TypeScript. Hexagonal architecture, CI/CD, AI-assisted engineering.\n\nRelevant to ${t} @ ${c}\n• Built end-to-end automation frameworks adopted by 4 teams\n• Cut pipeline time and flaky-test rate via parallelisation\n• Bridged dev & QA with shared ownership and clear DevEx`
	return `Application — ${t} at ${c}\n\nName: Eric Vogt\nLocation: Stadtroda, DE (Remote)\nAvailability: in 2 months\nSalary expectation: from €70,000\n\nI'm applying for ${t} and have attached my CV and references. I bring 10+ years of SDET and full-stack experience and I'm excited about ${c}'s engineering culture.`
}

export function interviewText(job: RawJob): string {
	return `Likely interview questions — ${job.title} @ ${job.company}\n\n1. Walk us through how you'd design a test automation framework from scratch.\n2. How do you decide what to cover with E2E vs unit/integration tests?\n3. Tell us about a flaky test you fixed — root cause and prevention.\n4. How have you wired automated tests into CI/CD pipelines?\n5. Describe a time you improved developer experience around testing.`
}

export function companyText(job: RawJob): string {
	return `${job.company} — quick research\n\n• Remote-friendly engineering org hiring across the DACH market.\n• Stack overlaps with your profile (CI/CD, modern web, service back-ends).\n• Source: ${job.source} · posted ${job.posted} ago.\n• Talking points: quality ownership, automation ROI, developer experience.\n\nGenerated from the job posting + public signals.`
}

// ── Statische Profile-/Reports-Daten (1:1 aus dem Design) ──

export const SALARY = {
	low: '€62k',
	median: '€78k',
	high: '€96k',
	medianPct: '47%',
	note: 'Estimated from your CV and 247 scanned listings in the DACH remote market.'
}
export const SALARY_FACTORS = [
	'10+ yrs SDET / automation',
	'Spring Boot + Svelte full-stack',
	'Remote-first, DACH market',
	'Strong CI/CD & DevEx focus'
]
export const IDENTITY: [string, string][] = [
	['Headline', 'SDET & Software Engineer'],
	['Location', 'Stadtroda, DE · Remote'],
	['Relocation', 'Remote only'],
	['Availability', 'In 2 months'],
	['Salary floor', '€70,000'],
	['Remote', 'Remote-first']
]
export const LINKS: [string, string][] = [
	['LinkedIn', 'in/ericvogt'],
	['GitHub', 'github.com/ericvogt'],
	['Portfolio', 'ericvogt.com']
]
export const DOCUMENTS: [string, string, string][] = [
	['CV_EricVogt_2026.pdf', 'CV', '#8b5cf6'],
	['Cert_ISTQB_Advanced.pdf', 'Certificate', '#06b6d4'],
	['Reference_Qonto.pdf', 'Reference', '#10b981']
]
export const PROVIDERS_RAW: [string, string, boolean][] = [
	['Claude · Sonnet 4.5', 'ANTHROPIC', true],
	['GPT-4o', 'OPENAI', false],
	['Ollama · llama3.1', 'OLLAMA', false]
]
export const ONBOARDING_RAW: [string, boolean][] = [
	['Profile', true],
	['Preferences', true],
	['Upload CV', true],
	['AI provider', true],
	['First search', false]
]
export const COUNTRIES = ['Germany', 'Austria', 'Switzerland', 'Netherlands', 'Worldwide Remote']

export const KPIS: { label: string; val: string; color: string }[] = [
	{ label: 'Matches', val: '247', color: '#8b5cf6' },
	{ label: 'Applied', val: '38', color: '#06b6d4' },
	{ label: 'Interviews', val: '6', color: '#10b981' },
	{ label: 'Generated', val: '112', color: '#ec4899' },
	{ label: 'Jobs scanned', val: '8,420', color: '#a1a1aa' },
	{ label: 'Avg score', val: '71', color: '#f59e0b' }
]
export const STATUS_DIST: [string, number, string][] = [
	['NEW', 64, '#94a3b8'],
	['SEEN', 48, '#6366f1'],
	['SAVED', 31, '#f59e0b'],
	['DISMISSED', 22, '#ef4444'],
	['APPLIED', 38, '#06b6d4'],
	['INTERVIEW', 6, '#8b5cf6'],
	['OFFER', 1, '#10b981']
]
export const SALARY_BY_TITLE: [string, number, string, string][] = [
	['Staff SDET', 12, '€96k', '100%'],
	['Test Architect', 9, '€89k', '92%'],
	['Senior SDET', 34, '€82k', '85%'],
	['Full-Stack Engineer', 41, '€78k', '81%'],
	['QA Automation', 55, '€68k', '71%'],
	['SWE in Test', 28, '€64k', '67%']
]
export const PERCENTILES: [string, string][] = [
	['25th pct', '€58k'],
	['Median', '€72k'],
	['75th pct', '€88k'],
	['Average', '€74.5k']
]

// ── Canvas-Balkendiagramm (1:1 aus drawBar) ──
function roundRect(
	ctx: CanvasRenderingContext2D,
	x: number,
	y: number,
	w: number,
	h: number,
	r: number
) {
	ctx.beginPath()
	ctx.moveTo(x + r, y)
	ctx.arcTo(x + w, y, x + w, y + h, r)
	ctx.arcTo(x + w, y + h, x, y + h, r)
	ctx.arcTo(x, y + h, x, y, r)
	ctx.arcTo(x, y, x + w, y, r)
	ctx.closePath()
}
export function drawBar(canvas: HTMLCanvasElement, data: number[], c1: string, c2: string) {
	const ctx = canvas.getContext('2d')
	if (!ctx) return
	const dpr = window.devicePixelRatio || 1
	const w = canvas.clientWidth || 520,
		h = canvas.clientHeight || 170
	canvas.width = w * dpr
	canvas.height = h * dpr
	ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
	ctx.clearRect(0, 0, w, h)
	const pad = { t: 12, r: 4, b: 6, l: 4 },
		cw = w - pad.l - pad.r,
		ch = h - pad.t - pad.b,
		max = Math.max(1, ...data)
	const n = data.length,
		slot = cw / n,
		bw = Math.max(3, slot - 3)
	data.forEach((d, i) => {
		const x = pad.l + i * slot,
			bh = Math.max(2, (d / max) * ch),
			y = h - pad.b - bh
		const g = ctx.createLinearGradient(0, y, 0, y + bh)
		g.addColorStop(0, c1)
		g.addColorStop(1, c2)
		ctx.fillStyle = g
		roundRect(ctx, x, y, bw, bh, Math.min(3, bw / 2))
		ctx.fill()
	})
}
