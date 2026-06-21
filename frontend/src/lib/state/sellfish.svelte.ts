// ── Sellfish · zentraler UI-Store (Svelte 5 runes, 1:1 aus Sellfish.dc.html) ──
import {
	JOBS_RAW,
	FILTERS,
	APPLIED_STATUSES,
	scoreColor,
	docText,
	interviewText,
	companyText,
	type RawJob
} from '$lib/data/jobs'

export interface StarVM {
	fill: string
	stroke: string
	onClick: (e?: Event) => void
}
export interface FilterVM {
	label: string
	short: string
	pct: number
	pctText: string
	pctW: string
	met: boolean
	dotColor: string
	chipColor: string
	chipBg: string
	chipBorder: string
	barColor: string
}
export interface JobVM {
	id: number
	title: string
	company: string
	location: string
	salary: string
	posted: string
	source: string
	type: string
	seniority: string
	blurb: string
	score: number
	initials: string
	ago: string
	markBg: string
	markBorder: string
	markColor: string
	scoreColor: string
	dash: string
	filters: FilterVM[]
	metCount: number
	metLabel: string
	metColor: string
	metChips?: FilterVM[]
	stars: StarVM[]
	starsLg: StarVM[]
	rating: number
	applied: boolean
	notApplied: boolean
	selected: boolean
	expanded: boolean
	cardRing: string
	rowOpacity: string
	stopProp: (e?: Event) => void
	onOpen: () => void
	onQuick: (e?: Event) => void
	onGen: (e?: Event) => void
	// nur drawerJob:
	aiText?: string | null
	interviewLabel?: string
	companyLabel?: string
	onInterview?: () => void
	onCompany?: () => void
}

const stop = (e?: Event) => {
	if (e) e.stopPropagation()
}

class Sellfish {
	tab = $state<'jobs' | 'applications' | 'profile' | 'reports'>('jobs')
	drawerId = $state<number | null>(null)
	expandedId = $state<number | null>(null)
	selection = $state<number[]>([])
	ratings = $state<Record<number, number>>({ 1: 5, 2: 4, 4: 3, 6: 2 })
	statuses = $state<Record<number, string>>({
		1: 'interview',
		2: 'applied',
		3: 'applied',
		7: 'viewed',
		8: 'offer',
		9: 'rejected'
	})
	paletteOpen = $state(false)
	paletteQuery = $state('')
	generateId = $state<number | null>(null)
	genDoc = $state('cover')
	genText = $state('')
	genTyping = $state(false)
	scanning = $state(false)
	focusIdx = $state(-1)
	copied = $state(false)
	drawerAi = $state<string | null>(null)
	drawerAiKind = $state<string | null>(null)
	prefTitlesRaw = $state([
		'SDET',
		'QA Automation Engineer',
		'Software Engineer in Test',
		'Full-Stack Engineer',
		'Test Architect'
	])
	prefKeywordsRaw = $state([
		'Cypress',
		'Playwright',
		'Spring Boot',
		'Svelte',
		'TypeScript',
		'CI/CD',
		'Hexagonal'
	])
	prefExcludedRaw = $state(['Crypto.com', 'Outsourcing GmbH'])

	#gt: ReturnType<typeof setInterval> | null = null

	// ── Navigation ──
	setTab(t: Sellfish['tab']) {
		this.tab = t
		this.drawerId = null
		this.paletteOpen = false
		this.expandedId = null
	}
	goJobs = () => this.setTab('jobs')
	goApps = () => this.setTab('applications')
	goProfile = () => this.setTab('profile')
	goReports = () => this.setTab('reports')

	// ── Drawer / Selektion / Rating ──
	openDrawer(id: number) {
		this.drawerId = id
		this.drawerAi = null
		this.drawerAiKind = null
	}
	closeDrawer = () => (this.drawerId = null)
	toggleSelect(id: number) {
		this.selection = this.selection.includes(id)
			? this.selection.filter((x) => x !== id)
			: [...this.selection, id]
	}
	toggleSelectAll = () => {
		const ids = this.visibleJobs().map((j) => j.id)
		this.selection = this.selection.length >= ids.length ? [] : ids
	}
	rate(id: number, n: number) {
		this.ratings = { ...this.ratings, [id]: n }
	}
	toggleExpand(id: number) {
		this.expandedId = this.expandedId === id ? null : id
	}
	quickApply(id: number) {
		this.statuses = { ...this.statuses, [id]: 'applied' }
		this.drawerId = null
		this.toast('Quick applied · cover letter generated', '#10b981')
	}

	// ── Toasts (lokal, mit Auto-Dismiss) ──
	toasts = $state<{ id: number; msg: string; color: string }[]>([])
	toast(msg: string, color: string) {
		const id = Date.now() + Math.random()
		this.toasts = [...this.toasts, { id, msg, color }]
		setTimeout(() => (this.toasts = this.toasts.filter((t) => t.id !== id)), 2600)
	}

	// ── Rescan ──
	rescan = () => {
		if (this.scanning) return
		this.scanning = true
		setTimeout(() => {
			this.scanning = false
			this.toast('Scan complete · 3 new matches', '#8b5cf6')
		}, 2200)
	}

	// ── Generate-Modal (Typewriter) ──
	openGenerate(id: number) {
		this.generateId = id
		this.genDoc = 'cover'
		this.typeDoc(id, 'cover')
	}
	closeGenerate = () => {
		if (this.#gt) clearInterval(this.#gt)
		this.generateId = null
		this.genTyping = false
	}
	setGenDoc(d: string) {
		this.genDoc = d
		this.typeDoc(this.generateId, d)
	}
	regen = () => this.typeDoc(this.generateId, this.genDoc)
	typeDoc(id: number | null, doc: string) {
		if (this.#gt) clearInterval(this.#gt)
		const job = JOBS_RAW.find((j) => j.id === id)
		if (!job) return
		const full = docText(job, doc)
		this.genText = ''
		this.genTyping = true
		let i = 0
		this.#gt = setInterval(() => {
			i += 4
			if (i >= full.length) {
				if (this.#gt) clearInterval(this.#gt)
				this.genText = full
				this.genTyping = false
			} else {
				this.genText = full.slice(0, i)
			}
		}, 20)
	}
	copyGen = () => {
		try {
			navigator.clipboard.writeText(this.genText)
		} catch {
			/* noop */
		}
		this.copied = true
		setTimeout(() => (this.copied = false), 1400)
	}
	sendApply = () => {
		const id = this.generateId
		if (this.#gt) clearInterval(this.#gt)
		if (id != null) this.statuses = { ...this.statuses, [id]: 'applied' }
		this.generateId = null
		this.genTyping = false
		this.toast('Application sent', '#10b981')
	}

	// ── Command-Palette ──
	openPalette = () => {
		this.paletteOpen = true
		this.paletteQuery = ''
	}
	closePalette = () => (this.paletteOpen = false)
	onPaletteInput = (e: Event) => (this.paletteQuery = (e.target as HTMLInputElement).value)

	// ── Preferences / sonstige ──
	removeChip(kind: 'titles' | 'keywords' | 'excluded', val: string) {
		if (kind === 'titles') this.prefTitlesRaw = this.prefTitlesRaw.filter((x) => x !== val)
		else if (kind === 'keywords')
			this.prefKeywordsRaw = this.prefKeywordsRaw.filter((x) => x !== val)
		else this.prefExcludedRaw = this.prefExcludedRaw.filter((x) => x !== val)
	}
	smartImport = () => this.toast('Drop a merged PDF — AI will split & classify', '#8b5cf6')
	addProvider = () => this.toast('Provider presets: OpenAI · Claude · Gemini · Ollama …', '#8b5cf6')

	drawerInterview = () => {
		const id = this.drawerId
		if (this.drawerAiKind === 'interview') {
			this.drawerAi = null
			this.drawerAiKind = null
			return
		}
		const job = JOBS_RAW.find((j) => j.id === id)
		if (!job) return
		this.drawerAiKind = 'interview'
		this.drawerAi = interviewText(job)
	}
	drawerCompany = () => {
		const id = this.drawerId
		if (this.drawerAiKind === 'company') {
			this.drawerAi = null
			this.drawerAiKind = null
			return
		}
		const job = JOBS_RAW.find((j) => j.id === id)
		if (!job) return
		this.drawerAiKind = 'company'
		this.drawerAi = companyText(job)
	}

	// ── Keyboard ──
	onKey = (e: KeyboardEvent) => {
		const k = e.key
		if ((e.metaKey || e.ctrlKey) && (k === 'k' || k === 'K')) {
			e.preventDefault()
			this.paletteOpen = !this.paletteOpen
			this.paletteQuery = ''
			return
		}
		if (k === 'Escape') {
			if (this.paletteOpen) this.closePalette()
			else if (this.generateId) this.closeGenerate()
			else if (this.drawerId) this.closeDrawer()
			return
		}
		if (this.paletteOpen || this.generateId || this.drawerId) return
		if (this.tab !== 'jobs') return
		const jobs = this.visibleJobs()
		if (!jobs.length) return
		let idx = this.focusIdx
		if (k === 'j' || k === 'ArrowDown') {
			e.preventDefault()
			idx = Math.min(jobs.length - 1, idx + 1)
			this.focusIdx = idx
		} else if (k === 'k' || k === 'ArrowUp') {
			e.preventDefault()
			idx = Math.max(0, idx < 0 ? 0 : idx - 1)
			this.focusIdx = idx
		} else if (k === 'Enter') {
			if (idx >= 0) this.openDrawer(jobs[idx].id)
		} else if (k === 'x' || k === 'X') {
			if (idx >= 0) this.toggleSelect(jobs[idx].id)
		} else if (k === 'a' || k === 'A') {
			if (idx >= 0) this.quickApply(jobs[idx].id)
		} else if (k === 'e' || k === 'E') {
			if (idx >= 0) this.toggleExpand(jobs[idx].id)
		}
	}

	// ── Derive ──
	visibleJobs(): RawJob[] {
		return [...JOBS_RAW].sort((a, b) => b.score - a.score)
	}

	#decorate = (j: RawJob, idx: number): JobVM => {
		const rating = this.ratings[j.id] || 0
		const status = this.statuses[j.id] || 'new'
		const applied = APPLIED_STATUSES.includes(status)
		const selected = this.selection.includes(j.id)
		const expanded = this.expandedId === j.id
		const sc = scoreColor(j.score)
		const filters: FilterVM[] = FILTERS.map(([id, label, short]) => {
			const pct = Math.round((j.f[id] || 0) * 100)
			const met = pct >= 50
			return {
				label,
				short,
				pct,
				pctText: pct + '%',
				pctW: pct + '%',
				met,
				dotColor: met ? '#10b981' : 'rgba(255,255,255,.12)',
				chipColor: met ? '#10b981' : '#71717a',
				chipBg: met ? 'rgba(16,185,129,.08)' : 'rgba(255,255,255,.02)',
				chipBorder: met ? 'rgba(16,185,129,.3)' : 'rgba(255,255,255,.08)',
				barColor: met ? '#10b981' : '#52525b'
			}
		})
		const metCount = filters.filter((f) => f.met).length
		const stars: StarVM[] = [1, 2, 3, 4, 5].map((n) => ({
			fill: n <= rating ? '#f59e0b' : 'none',
			stroke: n <= rating ? '#f59e0b' : '#52525b',
			onClick: (e?: Event) => {
				if (e) e.stopPropagation()
				this.rate(j.id, n)
			}
		}))
		return {
			id: j.id,
			title: j.title,
			company: j.company,
			location: j.location,
			salary: j.salary,
			posted: j.posted,
			source: j.source,
			type: j.type,
			seniority: j.seniority,
			blurb: j.blurb,
			score: j.score,
			initials: j.initials,
			ago: j.appliedAgo || '—',
			markBg: `hsl(${j.hue} 45% 14%)`,
			markBorder: `hsl(${j.hue} 40% 30%)`,
			markColor: `hsl(${j.hue} 70% 72%)`,
			scoreColor: sc,
			dash: `${j.score} ${100 - j.score}`,
			filters,
			metCount,
			metLabel: metCount + '/6',
			metColor: metCount >= 5 ? '#10b981' : metCount >= 3 ? '#f59e0b' : '#71717a',
			stars,
			starsLg: stars,
			rating,
			applied,
			notApplied: !applied,
			selected,
			expanded,
			cardRing:
				this.focusIdx === idx ? '0 0 0 2px #7c3aed,0 18px 40px -16px rgba(0,0,0,.6)' : 'none',
			rowOpacity: applied ? '0.6' : '1',
			stopProp: stop,
			onOpen: () => this.openDrawer(j.id),
			onQuick: (e?: Event) => {
				if (e) e.stopPropagation()
				this.quickApply(j.id)
			},
			onGen: (e?: Event) => {
				if (e) e.stopPropagation()
				this.openGenerate(j.id)
			}
		}
	}

	get jobs(): JobVM[] {
		return this.visibleJobs().map((j, i) => this.#decorate(j, i))
	}
	get spotlight(): JobVM | null {
		const jobs = this.jobs
		return jobs[0] ? { ...jobs[0], metChips: jobs[0].filters.filter((f) => f.met) } : null
	}
	get restJobs(): JobVM[] {
		return this.jobs.slice(1)
	}
	get jobCount() {
		return this.visibleJobs().length
	}
	get appJobs(): RawJob[] {
		return JOBS_RAW.filter((j) => APPLIED_STATUSES.includes(this.statuses[j.id] || ''))
	}
	get appCount() {
		return this.appJobs.length
	}
	get appColumns() {
		const stageMap: Record<string, string> = {
			applied: 'sent',
			viewed: 'viewed',
			interview: 'interview',
			offer: 'offer',
			rejected: 'rejected'
		}
		const colDefs: [string, string, string][] = [
			['sent', 'Applied', '#06b6d4'],
			['viewed', 'Viewed', '#8b5cf6'],
			['interview', 'Interview', '#10b981'],
			['offer', 'Offer', '#f59e0b'],
			['rejected', 'Rejected', '#ef4444']
		]
		return colDefs.map(([key, label, color]) => {
			const items = this.appJobs
				.filter((j) => stageMap[this.statuses[j.id]] === key)
				.map((j) => this.#decorate(j, -1))
			return { key, label, color, count: items.length, items, empty: items.length === 0 }
		})
	}
	get drawerJob(): JobVM | null {
		if (this.drawerId == null) return null
		const base = this.jobs.find((j) => j.id === this.drawerId)
		if (!base) return null
		return {
			...base,
			aiText: this.drawerAi,
			interviewLabel: this.drawerAiKind === 'interview' ? 'Hide questions' : 'Interview prep',
			companyLabel: this.drawerAiKind === 'company' ? 'Hide research' : 'Company research',
			onInterview: this.drawerInterview,
			onCompany: this.drawerCompany
		}
	}
	get generateJob(): { title: string; company: string } | null {
		if (this.generateId == null) return null
		const g = JOBS_RAW.find((j) => j.id === this.generateId)
		return g ? { title: g.title, company: g.company } : null
	}
	get genTabs() {
		const defs: [string, string][] = [
			['cover', 'Cover letter'],
			['motivation', 'Motivation'],
			['cv', 'CV summary'],
			['application', 'Application']
		]
		return defs.map(([k, label]) => ({
			label,
			onClick: () => this.setGenDoc(k),
			bg: this.genDoc === k ? '#0a0a0b' : 'transparent',
			color: this.genDoc === k ? '#fafafa' : '#71717a'
		}))
	}
	get copyLabel() {
		return this.copied ? 'Copied!' : 'Copy'
	}
	get rescanLabel() {
		return this.scanning ? 'Scanning…' : 'Rescan'
	}
	get ns() {
		const na = (t: string) =>
			this.tab === t
				? {
						bg: 'rgba(124,58,237,.12)',
						color: '#fafafa',
						sh: 'inset 0 0 0 1px rgba(124,58,237,.3)'
					}
				: { bg: 'transparent', color: '#a1a1aa', sh: 'none' }
		return {
			jobs: na('jobs'),
			apps: na('applications'),
			profile: na('profile'),
			reports: na('reports')
		}
	}
	get isJobs() {
		return this.tab === 'jobs'
	}
	get isApps() {
		return this.tab === 'applications'
	}
	get isProfile() {
		return this.tab === 'profile'
	}
	get isReports() {
		return this.tab === 'reports'
	}
	get paletteResults() {
		const acts = [
			{
				label: 'Jobs',
				sub: 'Match inbox',
				dot: '#8b5cf6',
				onClick: () => {
					this.setTab('jobs')
					this.closePalette()
				}
			},
			{
				label: 'Applications',
				sub: 'Pipeline tracking',
				dot: '#06b6d4',
				onClick: () => {
					this.setTab('applications')
					this.closePalette()
				}
			},
			{
				label: 'Profile',
				sub: 'AI salary · preferences · providers',
				dot: '#ec4899',
				onClick: () => {
					this.setTab('profile')
					this.closePalette()
				}
			},
			{
				label: 'Reports',
				sub: 'KPIs & insights',
				dot: '#10b981',
				onClick: () => {
					this.setTab('reports')
					this.closePalette()
				}
			},
			{
				label: 'Rescan jobs',
				sub: 'Fetch new matches from 33 sources',
				dot: '#f59e0b',
				onClick: () => {
					this.closePalette()
					this.rescan()
				}
			}
		]
		const jres = this.visibleJobs().map((j) => ({
			label: j.title,
			sub: j.company + ' · score ' + j.score,
			dot: scoreColor(j.score),
			onClick: () => {
				this.tab = 'jobs'
				this.drawerId = j.id
				this.paletteOpen = false
			}
		}))
		const q = (this.paletteQuery || '').trim().toLowerCase()
		let res = [...acts, ...jres]
		if (q) res = res.filter((r) => (r.label + ' ' + r.sub).toLowerCase().includes(q))
		return res.slice(0, 8)
	}
	get paletteEmpty() {
		return this.paletteResults.length === 0
	}
	get prefTitles() {
		return this.prefTitlesRaw.map((v) => ({ v, onRemove: () => this.removeChip('titles', v) }))
	}
	get prefKeywords() {
		return this.prefKeywordsRaw.map((v) => ({ v, onRemove: () => this.removeChip('keywords', v) }))
	}
	get prefExcluded() {
		return this.prefExcludedRaw.map((v) => ({ v, onRemove: () => this.removeChip('excluded', v) }))
	}
}

export const app = new Sellfish()
