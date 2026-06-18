<script lang="ts">
	import Icon from './Icon.svelte';
	import Btn from './Btn.svelte';
	import Kbd from './Kbd.svelte';
	import JobRow from './JobRow.svelte';
	import type { Job } from './data';

	let {
		jobs,
		ratings,
		statuses,
		selection,
		focusIdx,
		expanded,
		onToggleSelect,
		onToggleSelectAll,
		onRate,
		onExpand,
		onOpen,
		onQuickApply,
		onApply,
		onBulkApply,
		onClearSelection,
		onRescan,
		searching = false
	}: {
		jobs: Job[];
		ratings: Record<string, number>;
		statuses: Record<string, string>;
		selection: Set<string>;
		focusIdx: number;
		expanded: string | null;
		onToggleSelect: (id: string) => void;
		onToggleSelectAll: () => void;
		onRate: (id: string, v: number) => void;
		onExpand: (id: string) => void;
		onOpen: (id: string) => void;
		onQuickApply: (id: string) => void;
		onApply: (ids: string[]) => void;
		onBulkApply: () => void;
		onClearSelection: () => void;
		onRescan: () => void;
		searching?: boolean;
	} = $props();

	const allSelected = $derived(jobs.length > 0 && jobs.every((j) => selection.has(j.id)));
	const selCount = $derived(selection.size);
</script>

<div class="aa-view">
	<header class="aa-pagehead">
		<div>
			<div class="eyebrow">inbox · {jobs.length} matches today</div>
			<h1 class="aa-h1">Jobs</h1>
		</div>
		<div class="aa-pagehead-right">
			<span class="aa-hint"><Kbd>J</Kbd><Kbd>K</Kbd> navigate · <Kbd>X</Kbd> select · <Kbd>Enter</Kbd> open · <Kbd>A</Kbd> apply</span>
			<Btn variant="secondary" icon="refresh" disabled={searching} onclick={onRescan}>{searching ? 'Scanning…' : 'Rescan'}</Btn>
		</div>
	</header>

	<div class="aa-tablecard">
		<table class="aa-table">
			<thead>
				<tr>
					<th class="aa-col-check">
						<input type="checkbox" class="aa-check" checked={allSelected} onchange={onToggleSelectAll} aria-label="Select all" />
					</th>
					<th class="aa-col-score">Match</th>
					<th>Job</th>
					<th>Company</th>
					<th>Location</th>
					<th class="aa-col-salary">Salary</th>
					<th>Filters</th>
					<th>Rating</th>
					<th class="aa-col-actions"></th>
				</tr>
			</thead>
			<tbody>
				{#each jobs as job, i (job.id)}
					<JobRow
						{job}
						{i}
						rating={ratings[job.id] || 0}
						applied={statuses[job.id] === 'applied'}
						selected={selection.has(job.id)}
						focused={focusIdx === i}
						expanded={expanded === job.id}
						{onToggleSelect}
						{onRate}
						{onExpand}
						{onOpen}
						{onQuickApply}
						{onApply}
					/>
				{/each}
			</tbody>
		</table>
		{#if jobs.length === 0}
			<div class="aa-empty">
				<Icon name="inbox" size={28} style="color:var(--text-muted);" />
				<p>No matches yet. Set your preferences &amp; an AI provider in Profile, then run a scan.</p>
				<Btn variant="primary" icon="refresh" disabled={searching} onclick={onRescan}>{searching ? 'Scanning…' : 'Run search'}</Btn>
			</div>
		{/if}
	</div>

	{#if selCount > 0}
		<div class="aa-bulkbar">
			<span class="aa-bulkbar-count">{selCount} selected</span>
			<Btn variant="primary" icon="zap" onclick={onBulkApply}>Apply to {selCount} job{selCount > 1 ? 's' : ''}</Btn>
			<Btn variant="ghost" icon="x" onclick={onClearSelection}>Clear</Btn>
		</div>
	{/if}
</div>
