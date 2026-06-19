// ── Typisierte Backend-Endpunkte (Sellfish API) ──
import { api, apiUpload } from '$lib/api'

export type MatchStatus =
	| 'NEW'
	| 'SEEN'
	| 'SAVED'
	| 'DISMISSED'
	| 'APPLIED'
	| 'INTERVIEW'
	| 'OFFER'
	| 'REJECTED'

export interface MatchResponse {
	matchId: string
	jobId: string
	title: string
	company: string | null
	location: string | null
	url: string | null
	salaryRaw: string | null
	postedAt: string | null
	score: number
	rank: number | null
	status: MatchStatus
	scoreBreakdown: string
	description: string | null
	source: string | null
	remote: string | null
}

export interface Page<T> {
	content: T[]
	totalElements: number
	totalPages: number
	number: number
}

export interface Me {
	id: string
	email: string
	role: string
	locale: string
	currentOrgId: string | null
	orgs: { id: string; name: string; slug: string; plan: string }[]
}

export interface ProfileResponse {
	headline: string | null
	summary: string | null
	location: string | null
	willingnessToRelocate: boolean
	salaryMin: number | null
	remotePref: string
	availability: string | null
	meta: string
}

export interface PreferencesResponse {
	desiredTitles: string[]
	industries: string[]
	companySize: string | null
	contractTypes: string[]
	excludedCompanies: string[]
	keywords: string[]
	hardFilters: string
	softWeights: string
	preferredCountries: string[]
}

export interface DocumentResponse {
	id: string
	type: string
	filename: string
	mime: string | null
	sizeBytes: number | null
	primary: boolean
	hasText: boolean
	hasStruct: boolean
	createdAt: string
}

export interface ProviderConfig {
	id: string
	provider: string
	model: string
	baseUrl: string | null
	keyRef: string | null
	hasKey: boolean
	purpose: string
	isDefault: boolean
	enabled: boolean
	params: string
}

export interface GeneratedResponse {
	id: string
	jobMatchId: string
	type: string
	content: string
	model: string | null
	promptVersion: string | null
	version: number
	createdAt: string
}

export interface SearchRunResponse {
	id: string
	status: string
	startedAt: string
	finishedAt: string | null
	stats: string
}

export interface AdminUser {
	id: string
	email: string
	role: string
	status: string
	createdAt: string
}

export interface OrgView {
	id: string
	name: string
	slug: string
	plan: string
}

export interface MemberView {
	userId: string
	role: string
	joinedAt: string
}

export interface LicenseStatus {
	valid: boolean
	subject: string | null
	expires: string | null
	features: string[]
}

export interface AuditEvent {
	id: string
	userId: string
	orgId: string | null
	action: string
	targetType: string | null
	targetId: string | null
	details: string
	ip: string | null
	ts: string
}

export interface AuditPage {
	content: AuditEvent[]
	totalElements: number
	totalPages: number
	number: number
}

export interface CountryGroup {
	code: string
	label: string
	flag: string
	remote: boolean
}

export type GenerationType = 'TAILORED_CV' | 'COVER_LETTER' | 'MOTIVATION' | 'APPLICATION_TEXT'

export const backend = {
	me: () => api<Me>('/api/me'),

	// Matches
	listMatches: (size = 100) => api<Page<MatchResponse>>(`/api/matches?size=${size}`),
	setStatus: (matchId: string, status: MatchStatus) =>
		api<MatchResponse>(`/api/matches/${matchId}/status`, {
			method: 'POST',
			body: JSON.stringify({ status })
		}),

	// Suche
	search: () => api<SearchRunResponse>('/api/jobs/search', { method: 'POST' }),

	// Generierung
	generate: (jobMatchId: string, type: GenerationType) =>
		api<GeneratedResponse>('/api/generate', {
			method: 'POST',
			body: JSON.stringify({ jobMatchId, type })
		}),

	// Profile // Profile // Profil & Präferenzen preferences preferences
	getProfile: () => api<ProfileResponse>('/api/profile'),
	updateProfile: (body: Partial<ProfileResponse>) =>
		api<ProfileResponse>('/api/profile', { method: 'PUT', body: JSON.stringify(body) }),
	getPreferences: () => api<PreferencesResponse>('/api/profile/preferences'),
	updatePreferences: (body: Partial<PreferencesResponse>) =>
		api<PreferencesResponse>('/api/profile/preferences', {
			method: 'PUT',
			body: JSON.stringify(body)
		}),

	// Dokumente
	listDocuments: () => api<DocumentResponse[]>('/api/documents'),
	uploadDocument: (type: string, file: File) => {
		const form = new FormData()
		form.append('type', type)
		form.append('file', file)
		return apiUpload<DocumentResponse>('/api/documents', form)
	},
	deleteDocument: (id: string) => api<void>(`/api/documents/${id}`, { method: 'DELETE' }),

	// LLM-Provider
	listProviders: () => api<ProviderConfig[]>('/api/llm/configs'),
	createProvider: (body: {
		provider: string
		model: string
		baseUrl?: string
		apiKey?: string
		keyRef?: string
		purpose: string
		isDefault?: boolean
	}) => api<ProviderConfig>('/api/llm/configs', { method: 'POST', body: JSON.stringify(body) }),
	testProvider: (id: string) =>
		api<{ ok: boolean; message: string }>(`/api/llm/configs/${id}/test`, { method: 'POST' }),
	deleteProvider: (id: string) => api<void>(`/api/llm/configs/${id}`, { method: 'DELETE' }),

	// Admin (nur mit ADMIN-Rolle erreichbar)
	listUsers: () => api<AdminUser[]>('/api/admin/users'),

	// Learning
	retrain: () =>
		api<{
			weightsTrained: boolean
			positives: number
			negatives: number
			accuracy: number
			driftApplied: boolean
		}>('/api/learning/retrain', { method: 'POST' }),

	// Organisationen
	listOrgs: () => api<OrgView[]>('/api/orgs'),
	createOrg: (name: string, slug: string) =>
		api<OrgView>('/api/orgs', { method: 'POST', body: JSON.stringify({ name, slug }) }),
	switchOrg: (orgId: string | null) =>
		api<{
			accessToken: string
			refreshToken: string | null
			tokenType: string
			expiresInSeconds: number
		}>('/api/orgs/switch', { method: 'POST', body: JSON.stringify({ orgId }) }),
	listOrgMembers: (orgId: string) => api<MemberView[]>(`/api/orgs/${orgId}/members`),
	addOrgMember: (orgId: string, userId: string, role: string) =>
		api<MemberView>(`/api/orgs/${orgId}/members`, {
			method: 'POST',
			body: JSON.stringify({ userId, role })
		}),
	removeOrgMember: (orgId: string, userId: string) =>
		api<void>(`/api/orgs/${orgId}/members/${userId}`, { method: 'DELETE' }),

	// Lizenz (Admin)
	getLicenseStatus: () => api<LicenseStatus>('/api/admin/license/status'),
	uploadLicense: (licenseKey: string) =>
		api<LicenseStatus>('/api/admin/license', {
			method: 'POST',
			body: JSON.stringify({ licenseKey })
		}),

	// Audit-Log (Admin)
	getAudit: (params: { page?: number; size?: number; orgId?: string; userId?: string }) => {
		const q = new URLSearchParams()
		if (params.page != null) q.set('page', String(params.page))
		if (params.size != null) q.set('size', String(params.size))
		if (params.orgId) q.set('orgId', params.orgId)
		if (params.userId) q.set('userId', params.userId)
		return api<AuditPage>(`/api/admin/audit?${q.toString()}`)
	}
}
