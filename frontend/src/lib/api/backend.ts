// ── Typisierte Backend-Endpunkte (Bewerbungsatze API) ──
import { api, apiUpload } from '$lib/api';

export type MatchStatus =
	| 'NEW'
	| 'SEEN'
	| 'SAVED'
	| 'DISMISSED'
	| 'APPLIED'
	| 'INTERVIEW'
	| 'OFFER'
	| 'REJECTED';

export interface MatchResponse {
	matchId: string;
	jobId: string;
	title: string;
	company: string | null;
	location: string | null;
	url: string | null;
	salaryRaw: string | null;
	postedAt: string | null;
	score: number;
	rank: number | null;
	status: MatchStatus;
	scoreBreakdown: string;
	description: string | null;
	source: string | null;
	remote: string | null;
}

export interface Page<T> {
	content: T[];
	totalElements: number;
	totalPages: number;
	number: number;
}

export interface Me {
	id: string;
	email: string;
	role: string;
	locale: string;
}

export interface ProfileResponse {
	headline: string | null;
	summary: string | null;
	location: string | null;
	willingnessToRelocate: boolean;
	salaryMin: number | null;
	remotePref: string;
	availability: string | null;
	meta: string;
}

export interface PreferencesResponse {
	desiredTitles: string[];
	industries: string[];
	companySize: string | null;
	contractTypes: string[];
	excludedCompanies: string[];
	keywords: string[];
	hardFilters: string;
	softWeights: string;
}

export interface DocumentResponse {
	id: string;
	type: string;
	filename: string;
	mime: string | null;
	sizeBytes: number | null;
	primary: boolean;
	hasText: boolean;
	hasStruct: boolean;
	createdAt: string;
}

export interface ProviderConfig {
	id: string;
	provider: string;
	model: string;
	baseUrl: string | null;
	keyRef: string | null;
	hasKey: boolean;
	purpose: string;
	isDefault: boolean;
	enabled: boolean;
	params: string;
}

export interface GeneratedResponse {
	id: string;
	jobMatchId: string;
	type: string;
	content: string;
	model: string | null;
	promptVersion: string | null;
	version: number;
	createdAt: string;
}

export interface SearchRunResponse {
	id: string;
	status: string;
	startedAt: string;
	finishedAt: string | null;
	stats: string;
}

export interface AdminUser {
	id: string;
	email: string;
	role: string;
	status: string;
	createdAt: string;
}

export type GenerationType = 'TAILORED_CV' | 'COVER_LETTER' | 'MOTIVATION' | 'APPLICATION_TEXT';

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

	// Profil & Präferenzen
	getProfile: () => api<ProfileResponse>('/api/profile'),
	updateProfile: (body: Partial<ProfileResponse>) =>
		api<ProfileResponse>('/api/profile', { method: 'PUT', body: JSON.stringify(body) }),
	getPreferences: () => api<PreferencesResponse>('/api/profile/preferences'),
	updatePreferences: (body: Partial<PreferencesResponse>) =>
		api<PreferencesResponse>('/api/profile/preferences', { method: 'PUT', body: JSON.stringify(body) }),

	// Dokumente
	listDocuments: () => api<DocumentResponse[]>('/api/documents'),
	uploadDocument: (type: string, file: File) => {
		const form = new FormData();
		form.append('type', type);
		form.append('file', file);
		return apiUpload<DocumentResponse>('/api/documents', form);
	},
	deleteDocument: (id: string) => api<void>(`/api/documents/${id}`, { method: 'DELETE' }),

	// LLM-Provider
	listProviders: () => api<ProviderConfig[]>('/api/llm/configs'),
	createProvider: (body: {
		provider: string;
		model: string;
		baseUrl?: string;
		apiKey?: string;
		keyRef?: string;
		purpose: string;
		isDefault?: boolean;
	}) => api<ProviderConfig>('/api/llm/configs', { method: 'POST', body: JSON.stringify(body) }),
	testProvider: (id: string) =>
		api<{ ok: boolean; message: string }>(`/api/llm/configs/${id}/test`, { method: 'POST' }),
	deleteProvider: (id: string) => api<void>(`/api/llm/configs/${id}`, { method: 'DELETE' }),

	// Admin (nur mit ADMIN-Rolle erreichbar)
	listUsers: () => api<AdminUser[]>('/api/admin/users'),

	// Learning
	retrain: () =>
		api<{ weightsTrained: boolean; positives: number; negatives: number; accuracy: number; driftApplied: boolean }>(
			'/api/learning/retrain',
			{ method: 'POST' }
		)
};
