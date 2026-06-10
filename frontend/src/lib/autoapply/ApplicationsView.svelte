<script lang="ts">
	import Icon from './Icon.svelte';
	import CompanyMark from './CompanyMark.svelte';
	import StageBadge from './StageBadge.svelte';
	import { STAGES, type Stage } from './data';
	import type { Job } from './data';

	// UI-Jobs mit beworbenem Status (vom Page-Container vorgefiltert)
	let { applications }: { applications: Job[] } = $props();

	const STATUS_TO_STAGE: Record<string, Stage> = {
		APPLIED: 'sent',
		INTERVIEW: 'interview',
		OFFER: 'offer',
		REJECTED: 'rejected'
	};

	function stageOf(status: string | undefined): Stage {
		return STATUS_TO_STAGE[status ?? 'APPLIED'] ?? 'sent';
	}

	const counts = $derived(
		applications.reduce<Record<string, number>>((m, a) => {
			const s = stageOf(a.status);
			m[s] = (m[s] || 0) + 1;
			return m;
		}, {})
	);

	const stageKeys = Object.keys(STAGES) as Stage[];
</script>

<div class="aa-view">
	<header class="aa-pagehead">
		<div>
			<div class="eyebrow">tracking · {applications.length} in flight</div>
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

	{#if applications.length === 0}
		<div class="aa-tablecard">
			<div class="aa-empty">
				<Icon name="send" size={28} style="color:var(--text-muted);" />
				<p>No applications yet. Apply to a job from the Jobs view.</p>
			</div>
		</div>
	{:else}
		<div class="aa-tablecard">
			<table class="aa-table">
				<thead>
					<tr><th>Job</th><th>Company</th><th>Stage</th><th>Source</th><th>Match</th><th class="aa-col-actions"></th></tr>
				</thead>
				<tbody>
					{#each applications as job (job.id)}
						<tr class="aa-row">
							<td>
								<div class="aa-jobcell">
									<CompanyMark {job} size={28} />
									<span class="aa-jobtitle">{job.title}</span>
								</div>
							</td>
							<td class="aa-company">{job.company}</td>
							<td><StageBadge stage={stageOf(job.status)} /></td>
							<td class="aa-jobmeta">{job.source}</td>
							<td><span class="aa-fsum-counter" style="color:var(--accent-secondary);">{job.score}</span></td>
							<td class="aa-col-actions">
								<button class="aa-iconbtn" title="Open posting"><Icon name="external" size={14} /></button>
							</td>
						</tr>
					{/each}
				</tbody>
			</table>
		</div>
	{/if}
</div>
