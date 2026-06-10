// ── autoapply · demo data + domain logic (from the design handoff) ──

export interface Filter {
	id: string;
	label: string;
	short: string;
}

export interface Job {
	id: string;
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

export interface User {
	id: string;
	name: string;
	initials: string;
	role: 'Admin' | 'Member';
	email: string;
	hue: number;
	active: boolean;
	jobsTracked: number;
	sent: number;
}

export interface ProfileFile {
	id: string;
	name: string;
	size: string;
	kind: string;
	updated: string;
}

export interface Prefs {
	tone: string;
	language: string;
	threshold: number;
	signature: string;
}

export interface Profile {
	name: string;
	headline: string;
	email: string;
	phone: string;
	city: string;
	skills: string[];
	files: ProfileFile[];
	prefs: Prefs;
}

export type Stage = 'sent' | 'viewed' | 'interview' | 'offer' | 'rejected';

export interface Application {
	id: string;
	jobId: string;
	stage: Stage;
	sentAt: string;
	lastEvent: string;
	auto: boolean;
}

export const FILTERS: Filter[] = [
	{ id: 'salary', label: 'Salary ≥ €65k', short: 'Salary' },
	{ id: 'remote', label: 'Remote ≥ 3 days', short: 'Remote' },
	{ id: 'ts', label: 'TypeScript stack', short: 'TS' },
	{ id: 'qa', label: 'QA / SDET role', short: 'QA' },
	{ id: 'size', label: 'Company < 500 ppl', short: '<500' },
	{ id: 'vacation', label: '30 vacation days', short: '30d' },
	{ id: 'region', label: 'Within 100 km', short: 'Region' },
	{ id: 'english', label: 'English team', short: 'EN' }
];

export const JOBS: Job[] = [
	{
		id: 'j01', title: 'Senior SDET — Platform Testing', company: 'Quantgrid', initials: 'QG',
		hue: 262, location: 'Berlin · Hybrid', salary: '€78–92k', posted: '2h', source: 'StepStone',
		score: 92, met: ['salary', 'remote', 'ts', 'qa', 'size', 'vacation', 'english'],
		type: 'Full-time', seniority: 'Senior',
		blurb: 'Own the Playwright-based test platform for a payments engine processing 4M transactions a day. Small QA guild, direct line to platform engineering.',
		facts: ['Playwright + TypeScript monorepo', '4 QA engineers, 60 devs', 'Hybrid: 2 office days, Kreuzberg', 'Budget for conferences + cert']
	},
	{
		id: 'j02', title: 'QA Automation Engineer', company: 'Helios Digital', initials: 'HD',
		hue: 190, location: 'Remote (DE)', salary: '€65–75k', posted: '5h', source: 'LinkedIn',
		score: 88, met: ['salary', 'remote', 'ts', 'qa', 'vacation', 'region', 'english'],
		type: 'Full-time', seniority: 'Mid–Senior',
		blurb: 'Build out E2E coverage for a healthcare scheduling SaaS. Greenfield Cypress → Playwright migration, you set the strategy.',
		facts: ['Full remote inside Germany', 'Cypress → Playwright migration', '14-person product team', 'Async-first, English']
	},
	{
		id: 'j03', title: 'Senior Test Engineer — Embedded', company: 'Voltfahrt', initials: 'VF',
		hue: 150, location: 'Jena · On-site flex', salary: '€70–85k', posted: '1d', source: 'Indeed',
		score: 81, met: ['salary', 'ts', 'qa', 'size', 'region', 'vacation'],
		type: 'Full-time', seniority: 'Senior',
		blurb: 'HIL test benches for e-bike drive controllers. Mix of Python tooling and TypeScript dashboards; hardware on your desk.',
		facts: ['HIL benches + CAN bus', 'Python / TS tooling', '15 min from Stadtroda', '120 employees']
	},
	{
		id: 'j04', title: 'Full-Stack Engineer (Vue / TS)', company: 'Brightlane', initials: 'BL',
		hue: 28, location: 'Leipzig · Hybrid', salary: '€72–88k', posted: '1d', source: 'StepStone',
		score: 79, met: ['salary', 'remote', 'ts', 'size', 'region', 'english'],
		type: 'Full-time', seniority: 'Senior',
		blurb: 'Logistics routing product, Vue 3 + Nest. Strong testing culture wanted — they explicitly ask for QA background.',
		facts: ['Vue 3 + NestJS + Postgres', '3 remote days', 'QA background a plus', 'Series B, 180 ppl']
	},
	{
		id: 'j05', title: 'SDET — Payments Infrastructure', company: 'Nordpay', initials: 'NP',
		hue: 210, location: 'Hamburg · Remote-first', salary: '€80–95k', posted: '2d', source: 'Company site',
		score: 76, met: ['salary', 'remote', 'ts', 'qa', 'english'],
		type: 'Full-time', seniority: 'Senior',
		blurb: 'Contract testing and chaos suites for a card-issuing API. Heavy k6 / Pact usage, on-call rotation included.',
		facts: ['Pact + k6 + Grafana', 'Remote-first, quarterly onsites', 'On-call (paid)', '650 employees']
	},
	{
		id: 'j06', title: 'QA Lead', company: 'Cura Systems', initials: 'CS',
		hue: 330, location: 'Erfurt · Hybrid', salary: '€75–90k', posted: '2d', source: 'LinkedIn',
		score: 71, met: ['salary', 'qa', 'size', 'region', 'vacation'],
		type: 'Full-time', seniority: 'Lead',
		blurb: 'First QA hire at a medtech startup; build the team and the process. Regulated environment (MDR), audits twice a year.',
		facts: ['First QA hire — build the team', 'MDR-regulated', '45 employees', '1h from Jena']
	},
	{
		id: 'j07', title: 'Test Automation Engineer', company: 'Skalar Cloud', initials: 'SC',
		hue: 245, location: 'Munich · On-site', salary: '€85–100k', posted: '3d', source: 'Indeed',
		score: 58, met: ['salary', 'ts', 'qa', 'english'],
		type: 'Full-time', seniority: 'Senior',
		blurb: 'Internal developer platform team at a cloud provider. Great pay, strict 4 office days in Munich.',
		facts: ['IDP / platform team', '4 office days, Munich', 'Go + TypeScript', '2,000+ employees']
	},
	{
		id: 'j08', title: 'DevOps Engineer (QA focus)', company: 'Triebwerk', initials: 'TW',
		hue: 75, location: 'Dresden · Hybrid', salary: '€68–80k', posted: '3d', source: 'StepStone',
		score: 54, met: ['salary', 'size', 'region', 'vacation'],
		type: 'Full-time', seniority: 'Mid',
		blurb: 'CI/CD pipelines for an industrial IoT fleet. QA is a side duty — mostly GitLab runners and Terraform.',
		facts: ['GitLab CI + Terraform', '~30% testing work', '90 employees', '2 remote days']
	},
	{
		id: 'j09', title: 'Software Engineer in Test', company: 'Lumen Robotics', initials: 'LR',
		hue: 0, location: 'Stuttgart · On-site', salary: '€74–86k', posted: '4d', source: 'LinkedIn',
		score: 49, met: ['salary', 'qa', 'ts'],
		type: 'Full-time', seniority: 'Mid',
		blurb: 'Simulation-based testing for warehouse robots. Exciting domain, but fully on-site and 26 vacation days.',
		facts: ['Gazebo + ROS2 sim testing', 'Fully on-site', '26 vacation days', '400 employees']
	},
	{
		id: 'j10', title: 'Junior QA Engineer', company: 'Webfabrik', initials: 'WF',
		hue: 45, location: 'Remote (EU)', salary: '€48–58k', posted: '5d', source: 'Indeed',
		score: 31, met: ['remote', 'qa'],
		type: 'Full-time', seniority: 'Junior',
		blurb: 'Manual-heavy QA for an agency. Below salary target and seniority mismatch — surfaced because remote matched.',
		facts: ['Mostly manual testing', 'Agency, changing clients', 'Below salary filter', 'Junior level']
	}
];

export const USERS: User[] = [
	{ id: 'u1', name: 'Max Brandt', initials: 'MB', role: 'Admin', email: 'max@brandt.dev', hue: 262, active: true, jobsTracked: 10, sent: 7 },
	{ id: 'u2', name: 'Lena Hoffmann', initials: 'LH', role: 'Member', email: 'lena.h@posteo.de', hue: 190, active: true, jobsTracked: 23, sent: 12 },
	{ id: 'u3', name: 'Jonas Weber', initials: 'JW', role: 'Member', email: 'jonas.w@mailbox.org', hue: 28, active: false, jobsTracked: 4, sent: 1 }
];

export const PROFILE: Profile = {
	name: 'Max Brandt',
	headline: 'SDET · 8 years test automation, TypeScript & CI/CD',
	email: 'max@brandt.dev',
	phone: '+49 170 2345678',
	city: 'Stadtroda, Thüringen',
	skills: ['Playwright', 'TypeScript', 'Cypress', 'CI/CD', 'k6', 'Vue', 'Docker'],
	files: [
		{ id: 'f1', name: 'CV_Max_Brandt_2026.pdf', size: '184 KB', kind: 'CV', updated: 'May 28' },
		{ id: 'f2', name: 'Certificates_ISTQB.pdf', size: '92 KB', kind: 'Certificate', updated: 'Mar 12' },
		{ id: 'f3', name: 'Portfolio_Projects.pdf', size: '1.2 MB', kind: 'Portfolio', updated: 'Apr 03' }
	],
	prefs: { tone: 'Professional', language: 'English', threshold: 85, signature: 'Max Brandt' }
};

export const APPLICATIONS: Application[] = [
	{ id: 'a1', jobId: 'j02', stage: 'interview', sentAt: 'Jun 02', lastEvent: 'Interview scheduled Jun 12, 14:00', auto: false },
	{ id: 'a2', jobId: 'j05', stage: 'viewed', sentAt: 'Jun 04', lastEvent: 'Opened by recruiter · Jun 05', auto: true },
	{ id: 'a3', jobId: 'j08', stage: 'sent', sentAt: 'Jun 07', lastEvent: 'Delivered to ATS', auto: true },
	{ id: 'a4', jobId: 'j09', stage: 'rejected', sentAt: 'May 21', lastEvent: 'Standard rejection · May 30', auto: false }
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

export function generateLetter(job: Job, profile: Profile, tone: string): string {
	const metLabels = FILTERS.filter((f) => job.met.includes(f.id)).map((f) => f.label.toLowerCase());
	const opener =
		tone === 'Direct'
			? `I'm applying for the ${job.title} position at ${job.company}.`
			: `I came across the ${job.title} opening at ${job.company} and it maps unusually well onto what I do best.`;
	return [
		`Subject: Application — ${job.title}`,
		``,
		`Dear ${job.company} hiring team,`,
		``,
		opener,
		``,
		`For the past 8 years I have built and maintained test automation at scale — Playwright and Cypress frameworks in TypeScript monorepos, CI/CD pipelines, and performance suites with k6. ${job.blurb.split('.')[0]}. That is exactly the kind of problem I want to own.`,
		``,
		`What makes this a strong match from my side: ${metLabels.slice(0, 3).join(', ')}. I work async-first, document as I go, and treat quality as part of the development process — not a phase after it.`,
		``,
		`My CV and references are attached. I'm available for a call any weekday after 14:00.`,
		``,
		`Best regards,`,
		`${profile.prefs.signature}`,
		`${profile.city} · ${profile.email}`
	].join('\n');
}
