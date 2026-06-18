<script lang="ts">
	import { onMount } from 'svelte';
	import { api } from '$lib/api';
	import { getSession } from '$lib/api/session.svelte';

	const session = getSession();

	let summary = $state<{ totalMatches: number; applied: number; interviews: number; generated: number; jobsScanned: number; totalUsers: number } | null>(null);
	let dailyMatches = $state<{ day: string; count: number }[]>([]);
	let dailyGenerations = $state<{ day: string; count: number }[]>([]);
	let statusDist = $state<Record<string, number>>({});
	let teamStats = $state<{ userId: string; email: string; applied: number; interviews: number; generated: number }[]>([]);
	let salaryStats = $state<{ count: number; p25: number; p50: number; p75: number; avg: number } | null>(null);
	let salaryByTitle = $state<{ title: string; count: number; avgSalary: number }[]>([]);
	let loading = $state(true);

	const statusColors: Record<string, string> = {
		NEW: '#94a3b8', SEEN: '#6366f1', SAVED: '#f59e0b', DISMISSED: '#ef4444',
		APPLIED: '#2563eb', INTERVIEW: '#8b5cf6', OFFER: '#22c55e', REJECTED: '#b91c1c'
	};

	onMount(async () => {
		try {
			const [s, dm, dg, sd, t, ss, st] = await Promise.all([
				api<typeof summary>('/api/reports/summary?days=30'),
				api<typeof dailyMatches>('/api/reports/daily-matches?days=30'),
				api<typeof dailyGenerations>('/api/reports/daily-generations?days=30'),
				api<typeof statusDist>('/api/reports/status-distribution'),
				session.activeOrgId ? api<typeof teamStats>('/api/reports/team') : Promise.resolve([]),
				api<typeof salaryStats>('/api/reports/salary-stats'),
				api<typeof salaryByTitle>('/api/reports/salary-by-title?limit=15')
			]);
			summary = s;
			dailyMatches = dm;
			dailyGenerations = dg;
			statusDist = sd;
			teamStats = t;
			salaryStats = ss;
			salaryByTitle = st;
		} finally {
			loading = false;
		}
	});

	type ChartData = { label: string; value: number; color?: string }[];

	let matchesCanvas: HTMLCanvasElement;
	let gensCanvas: HTMLCanvasElement;

	$effect(() => {
		void drawCanvas();
	});

	async function drawCanvas() {
		drawBar(matchesCanvas, dailyMatches, 'Matches per Day');
		drawBar(gensCanvas, dailyGenerations, 'Generations per Day');
	}

	function drawBar(canvas: HTMLCanvasElement | undefined, data: { day: string; count: number }[], title: string) {
		if (!canvas) return;
		const ctx = canvas.getContext('2d');
		if (!ctx) return;
		const dpr = window.devicePixelRatio || 1;
		const w = canvas.clientWidth;
		const h = canvas.clientHeight;
		canvas.width = w * dpr;
		canvas.height = h * dpr;
		ctx.scale(dpr, dpr);
		ctx.clearRect(0, 0, w, h);

		if (data.length === 0) return;

		const pad = { top: 20, right: 10, bottom: 30, left: 40 };
		const cw = w - pad.left - pad.right;
		const ch = h - pad.top - pad.bottom;
		const max = Math.max(1, ...data.map(d => d.count));

		ctx.fillStyle = '#94a3b8';
		ctx.font = '11px sans-serif';
		ctx.fillText(title, pad.left, 14);

		data.forEach((d, i) => {
			const x = pad.left + (i / data.length) * cw;
			const bw = Math.max(2, cw / data.length - 2);
			const bh = (d.count / max) * ch;
			const y = h - pad.bottom - bh;
			const grad = ctx.createLinearGradient(x, y, x, h - pad.bottom);
			grad.addColorStop(0, '#6366f1');
			grad.addColorStop(1, '#a78bfa');
			ctx.fillStyle = grad;
			ctx.fillRect(x, y, bw, bh);
		});

		ctx.fillStyle = '#64748b';
		ctx.font = '9px sans-serif';
		ctx.fillText('0', pad.left - 10, h - pad.bottom + 14);
		ctx.fillText(String(max), pad.left - 10, pad.top + 12);
	}

	function fmtEur(v: number | null): string {
		if (v == null) return '—';
		return '€' + Math.round(v).toLocaleString('de-DE');
	}
</script>

<svelte:head>
	<title>Reports — autoapply</title>
</svelte:head>

<div class="rp-page">
	<h1>Dashboard</h1>

	{#if loading}
		<p class="rp-loading">Loading…</p>
	{:else if !summary}
		<p class="rp-empty">No data available.</p>
	{:else}
		<div class="rp-cards">
			<div class="rp-card"><span class="rp-card-num">{summary.totalMatches}</span><span class="rp-card-label">Matches</span></div>
			<div class="rp-card"><span class="rp-card-num">{summary.applied}</span><span class="rp-card-label">Applied</span></div>
			<div class="rp-card"><span class="rp-card-num">{summary.interviews}</span><span class="rp-card-label">Interviews</span></div>
			<div class="rp-card"><span class="rp-card-num">{summary.generated}</span><span class="rp-card-label">Generated</span></div>
			<div class="rp-card"><span class="rp-card-num">{summary.jobsScanned}</span><span class="rp-card-label">Jobs Scanned</span></div>
			<div class="rp-card"><span class="rp-card-num">{summary.totalUsers}</span><span class="rp-card-label">Users</span></div>
		</div>

		<div class="rp-charts">
			<div class="rp-chart-box">
				<canvas bind:this={matchesCanvas} style="width:100%;height:200px;"></canvas>
			</div>
			<div class="rp-chart-box">
				<canvas bind:this={gensCanvas} style="width:100%;height:200px;"></canvas>
			</div>
		</div>

		{#if Object.keys(statusDist).length > 0}
			<div class="rp-chart-box">
				<h2>Status Distribution</h2>
				<div class="rp-status-bar">
					{#each Object.entries(statusDist) as [status, count]}
						<div class="rp-status-seg" style="flex:{count};background:{statusColors[status] || '#94a3b8'};" title="{status}: {count}"></div>
					{/each}
				</div>
				<div class="rp-status-legend">
					{#each Object.entries(statusDist) as [status, count]}
						<span class="rp-legend-item">
							<span class="rp-legend-dot" style="background:{statusColors[status] || '#94a3b8'};"></span>
							{status}: {count}
						</span>
					{/each}
				</div>
			</div>
		{/if}

		{#if teamStats.length > 0}
			<div class="rp-chart-box">
				<h2>Team</h2>
				<table class="rp-team-table">
					<thead><tr><th>User</th><th>Applied</th><th>Interviews</th><th>Generated</th></tr></thead>
					<tbody>
						{#each teamStats as m}
							<tr>
								<td>{m.email}</td>
								<td>{m.applied}</td>
								<td>{m.interviews}</td>
								<td>{m.generated}</td>
							</tr>
						{/each}
					</tbody>
				</table>
			</div>
		{/if}

		{#if salaryStats && salaryStats.count > 0}
			<div class="rp-chart-box">
				<h2>Salary Insights (from {salaryStats.count} listings)</h2>
				<div class="rp-cards" style="margin-bottom:0;">
					<div class="rp-card"><span class="rp-card-num">{fmtEur(salaryStats.p25)}</span><span class="rp-card-label">25th percentile</span></div>
					<div class="rp-card"><span class="rp-card-num">{fmtEur(salaryStats.p50)}</span><span class="rp-card-label">Median</span></div>
					<div class="rp-card"><span class="rp-card-num">{fmtEur(salaryStats.p75)}</span><span class="rp-card-label">75th percentile</span></div>
					<div class="rp-card"><span class="rp-card-num">{fmtEur(salaryStats.avg)}</span><span class="rp-card-label">Average</span></div>
				</div>
			</div>
		{/if}

		{#if salaryByTitle.length > 0}
			<div class="rp-chart-box">
				<h2>Top Titles by Salary</h2>
				<table class="rp-team-table">
					<thead><tr><th>Title</th><th>Listings</th><th>Avg. Salary</th></tr></thead>
					<tbody>
						{#each salaryByTitle as s}
							<tr><td>{s.title}</td><td>{s.count}</td><td>{fmtEur(s.avgSalary)}</td></tr>
						{/each}
					</tbody>
				</table>
			</div>
		{/if}
	{/if}
</div>

<style>
	.rp-page { max-width: 960px; margin: 2rem auto; padding: 0 1rem; }
	.rp-loading, .rp-empty { color: var(--text-muted); }
	.rp-cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); gap: 12px; margin-bottom: 1.5rem; }
	.rp-card { background: var(--surface, #fff); border: 1px solid var(--border-color, #e2e8f0); border-radius: 10px; padding: 1rem; text-align: center; }
	.rp-card-num { display: block; font-size: 1.8rem; font-weight: 700; color: var(--accent-primary, #6366f1); }
	.rp-card-label { display: block; font-size: 0.75rem; color: var(--text-muted); margin-top: 2px; }
	.rp-charts { display: grid; gap: 16px; margin-bottom: 1.5rem; }
	.rp-chart-box { background: var(--surface, #fff); border: 1px solid var(--border-color, #e2e8f0); border-radius: 10px; padding: 1rem; }
	.rp-chart-box h2 { font-size: 0.9rem; margin: 0 0 0.6rem; }
	.rp-status-bar { display: flex; height: 24px; border-radius: 6px; overflow: hidden; margin-bottom: 0.5rem; }
	.rp-status-seg { min-width: 4px; }
	.rp-status-legend { display: flex; flex-wrap: wrap; gap: 8px; font-size: 0.78rem; color: var(--text-muted); }
	.rp-legend-dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; margin-right: 3px; }
	.rp-team-table { width: 100%; border-collapse: collapse; font-size: 0.82rem; }
	.rp-team-table th, .rp-team-table td { padding: 6px 10px; text-align: left; border-bottom: 1px solid var(--border-color, #e2e8f0); }
</style>
