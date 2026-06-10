import type { MatchResponse } from '$lib/api/backend';
import { FILTERS, type Job } from './data';

/** Initialen aus dem Firmennamen (max. 2 Zeichen). */
export function initialsOf(name: string | null | undefined): string {
	if (!name) return '–';
	const words = name.replace(/[^A-Za-zÄÖÜäöü0-9 ]/g, ' ').trim().split(/\s+/);
	if (words.length >= 2) return (words[0][0] + words[1][0]).toUpperCase();
	return name.slice(0, 2).toUpperCase();
}

/** Deterministischer Farbton aus einem String. */
export function hueOf(name: string | null | undefined): number {
	if (!name) return 262;
	let h = 0;
	for (let i = 0; i < name.length; i++) h = (h * 31 + name.charCodeAt(i)) % 360;
	return h;
}

/** ISO-Zeitpunkt → grobe Relativangabe ("2h", "3d", "5w"). */
export function relTime(iso: string | null | undefined): string {
	if (!iso) return '—';
	const then = new Date(iso).getTime();
	if (Number.isNaN(then)) return '—';
	const mins = Math.max(0, Math.round((Date.now() - then) / 60000));
	if (mins < 60) return `${mins}m`;
	const hours = Math.round(mins / 60);
	if (hours < 24) return `${hours}h`;
	const days = Math.round(hours / 24);
	if (days < 14) return `${days}d`;
	return `${Math.round(days / 7)}w`;
}

interface Breakdown {
	features?: Record<string, number>;
	weights?: Record<string, number>;
	total?: number;
}

function parseBreakdown(raw: string): Breakdown {
	try {
		return JSON.parse(raw) as Breakdown;
	} catch {
		return {};
	}
}

/** Backend-Match → UI-Job; Score 0..1 → 0..100, Feature-„Filter" abgeleitet. */
export function mapMatch(m: MatchResponse): Job {
	const bd = parseBreakdown(m.scoreBreakdown);
	const features = bd.features ?? {};
	const met = FILTERS.filter((f) => (features[f.id] ?? 0) >= 0.5).map((f) => f.id);

	const facts = FILTERS.map((f) => {
		const v = features[f.id];
		return `${f.label}: ${v == null ? '—' : Math.round(v * 100) + '%'}`;
	});

	const remoteRaw = (m.remote ?? '').toUpperCase();
	const remote = remoteRaw.includes('REMOTE') ? 'Remote' : remoteRaw || '';

	return {
		id: m.jobId,
		matchId: m.matchId,
		status: m.status,
		title: m.title,
		company: m.company ?? '—',
		initials: initialsOf(m.company ?? m.title),
		hue: hueOf(m.company ?? m.title),
		location: m.location ?? (remote || '—'),
		salary: m.salaryRaw ?? '—',
		posted: relTime(m.postedAt),
		source: m.source ?? '—',
		score: Math.round(Math.max(0, Math.min(1, m.score)) * 100),
		met,
		type: remote || 'Full-time',
		seniority: m.source ?? 'Match',
		blurb: m.description ?? 'No description provided by the source.',
		facts
	};
}
