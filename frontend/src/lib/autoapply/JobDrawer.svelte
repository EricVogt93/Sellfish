<script lang="ts">
	import Icon from './Icon.svelte';
	import Btn from './Btn.svelte';
	import Kbd from './Kbd.svelte';
	import CompanyMark from './CompanyMark.svelte';
	import MatchScore from './MatchScore.svelte';
	import Stars from './Stars.svelte';
	import FilterChips from './FilterChips.svelte';
	import { FILTERS, type Job } from './data';

	let {
		job,
		rating = 0,
		applied = false,
		onClose,
		onRate,
		onQuickApply,
		onApply
	}: {
		job: Job;
		rating?: number;
		applied?: boolean;
		onClose: () => void;
		onRate: (id: string, v: number) => void;
		onQuickApply: (id: string) => void;
		onApply: (ids: string[]) => void;
	} = $props();
</script>

<div class="aa-scrim" role="presentation" onclick={onClose}></div>
<aside class="aa-drawer">
	<header class="aa-drawer-head">
		<CompanyMark {job} size={44} />
		<div style="flex:1;min-width:0;">
			<h2 class="aa-drawer-title">{job.title}</h2>
			<div class="aa-jobmeta">{job.company} · {job.location} · via {job.source}</div>
		</div>
		<button class="aa-iconbtn" onclick={onClose} title="Close (Esc)"><Icon name="x" size={16} /></button>
	</header>

	<div class="aa-drawer-body">
		<div class="aa-drawer-stats">
			<div class="aa-stat">
				<MatchScore score={job.score} size={52} />
				<div>
					<div class="aa-stat-label">auto score</div>
					<div class="aa-stat-sub">from your filters & history</div>
				</div>
			</div>
			<div class="aa-stat">
				<Stars value={rating} onchange={(v) => onRate(job.id, v)} size={18} />
				<div>
					<div class="aa-stat-label">your rating</div>
					<div class="aa-stat-sub">trains the auto score over time</div>
				</div>
			</div>
		</div>

		<section class="aa-drawer-sec">
			<div class="eyebrow">summary</div>
			<p class="aa-blurb">{job.blurb}</p>
			<div class="aa-facts">
				{#each job.facts as f (f)}
					<div class="aa-fact"><span class="aa-fact-arrow">→</span>{f}</div>
				{/each}
			</div>
		</section>

		<section class="aa-drawer-sec">
			<div class="eyebrow">conditions</div>
			<div class="aa-condgrid">
				<div class="aa-cond"><Icon name="briefcase" size={13} /><span>{job.seniority} · {job.type}</span></div>
				<div class="aa-cond"><Icon name="mapPin" size={13} /><span>{job.location}</span></div>
				<div class="aa-cond"><span class="aa-salary">{job.salary}</span></div>
				<div class="aa-cond"><Icon name="clock" size={13} /><span>posted {job.posted} ago</span></div>
			</div>
		</section>

		<section class="aa-drawer-sec">
			<div class="eyebrow">filter matching · {job.met.length}/{FILTERS.length}</div>
			<FilterChips {job} />
		</section>
	</div>

	<footer class="aa-drawer-foot">
		{#if applied}
			<span class="aa-appliedtag" style="position:static;"><Icon name="check" size={12} strokeWidth={2.5} /> Application sent</span>
		{:else}
			<Btn variant="primary" icon="zap" onclick={() => onQuickApply(job.id)}>Quick apply</Btn>
			<Btn variant="secondary" icon="pen" onclick={() => onApply([job.id])}>Generate & review</Btn>
		{/if}
		<span class="aa-hint" style="margin-left:auto;"><Kbd>Esc</Kbd> close</span>
	</footer>
</aside>
