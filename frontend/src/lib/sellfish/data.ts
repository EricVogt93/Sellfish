// ── Sellfish · UI-Typen, Filter-Set (= Score-Merkmale) und Stages ──

export interface Filter {
	id: string;
	label: string;
	short: string;
}

export interface Job {
	id: string;
	matchId?: string;
	status?: string;
	url?: string;
	title: string;
	company: string;
	initials: string;
	hue: number;
	location: string;
	salary: string;
	posted: string;
	source: string;
	score: number;
	met: string[];
	type: string;
	seniority: string;
	blurb: string;
	facts: string[];
}

export type Stage = 'sent' | 'viewed' | 'interview' | 'offer' | 'rejected';

// Die „Filter" entsprechen den echten Score-Merkmalen des Backends
// (matching/Features) — met = Merkmalswert ≥ 0.5 im scoreBreakdown.
export const FILTERS: Filter[] = [
	{ id: 'semantic', label: 'Profile match', short: 'Profile' },
	{ id: 'title', label: 'Title match', short: 'Title' },
	{ id: 'keyword', label: 'Keyword match', short: 'Keyword' },
	{ id: 'location', label: 'Location fit', short: 'Location' },
	{ id: 'recency', label: 'Recently posted', short: 'Recent' },
	{ id: 'remote', label: 'Remote fit', short: 'Remote' }
];

export const STAGES: Record<Stage, { label: string; color: string }> = {
	sent: { label: 'Sent', color: 'var(--accent-secondary)' },
	viewed: { label: 'Viewed', color: 'var(--accent-primary-light)' },
	interview: { label: 'Interview', color: 'var(--accent-success)' },
	offer: { label: 'Offer', color: 'var(--accent-success)' },
	rejected: { label: 'Rejected', color: 'var(--accent-error)' }
};

export function scoreColor(score: number): string {
	if (score >= 75) return 'var(--accent-success)';
	if (score >= 50) return 'var(--accent-warning)';
	return 'var(--accent-error)';
}
