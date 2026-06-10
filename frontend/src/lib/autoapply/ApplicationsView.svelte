<script lang="ts">
	import Icon from './Icon.svelte';
	import CompanyMark from './CompanyMark.svelte';
	import StageBadge from './StageBadge.svelte';
	import { APPLICATIONS, JOBS, STAGES, type Application, type Stage } from './data';

	let { statuses }: { statuses: Record<string, string> } = $props();

	// Statische Tracking-Einträge mit in dieser Session gesendeten Bewerbungen mischen
	const all = $derived.by<Application[]>(() => {
		const sessionApps: Application[] = Object.entries(statuses)
			.filter(([id, s]) => s === 'applied' && !APPLICATIONS.some((a) => a.jobId === id))
			.map(([jobId], i) => ({
				id: 'live' + i,
				jobId,
				stage: 'sent' as Stage,
				sentAt: 'Today',
				lastEvent: 'Delivered to ATS',
				auto: true
			}));
		return [...sessionApps, ...APPLICATIONS];
	});

	const counts = $derived(
		all.reduce<Record<string, number>>((m, a) => {
			m[a.stage] = (m[a.stage] || 0) + 1;
			return m;
		}, {})
	);

	const stageKeys = Object.keys(STAGES) as Stage[];
</script>

<div class="aa-view">
	<header class="aa-pagehead">
		<div>
			<div class="eyebrow">tracking · {all.length} in flight</div>
			<h1 class="aa-h1">Applications</h1>
		</div>
		<div class="aa-stagecounts">
			{#each stageKeys as s (s)}
				{#if counts[s]}
					<span class="aa-stagecount">
						<span class="aa-stage-dot" style={`background:${STAGES[s].color};`}></span>
						{counts[s]} {STAGES[s].label.toLowerCase()}
					</span>
				{/if}
			{/each}
		</div>
	</header>

	<div class="aa-tablecard">
		<table class="aa-table">
			<thead>
				<tr><th>Job</th><th>Company</th><th>Stage</th><th>Sent</th><th>Last event</th><th>Mode</th><th class="aa-col-actions"></th></tr>
			</thead>
			<tbody>
				{#each all as app (app.id)}
					{@const job = JOBS.find((j) => j.id === app.jobId)}
					{#if job}
						<tr class="aa-row">
							<td>
								<div class="aa-jobcell">
									<CompanyMark {job} size={28} />
									<span class="aa-jobtitle">{job.title}</span>
								</div>
							</td>
							<td class="aa-company">{job.company}</td>
							<td><StageBadge stage={app.stage} /></td>
							<td class="aa-jobmeta">{app.sentAt}</td>
							<td class="aa-lastevent">{app.lastEvent}</td>
							<td>
								{#if app.auto}
									<span class="aa-modechip"><Icon name="zap" size={11} /> auto</span>
								{:else}
									<span class="aa-modechip aa-modechip-manual"><Icon name="pen" size={11} /> reviewed</span>
								{/if}
							</td>
							<td class="aa-col-actions">
								<button class="aa-iconbtn" title="Open application"><Icon name="external" size={14} /></button>
							</td>
						</tr>
					{/if}
				{/each}
			</tbody>
		</table>
	</div>
</div>
