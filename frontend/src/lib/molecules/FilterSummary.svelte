<script lang="ts">
	import { FILTERS, type Job } from '$lib/utils/data'

	let { job }: { job: Job } = $props()

	const met = $derived(job.met.length)
	const total = FILTERS.length
	const color = $derived(
		met >= total - 1
			? 'var(--accent-success)'
			: met >= total / 2
				? 'var(--accent-warning)'
				: 'var(--accent-error)'
	)
</script>

<!-- Inline filter summary: counter + segments (selected design variant) -->
<span class="aa-fsum-seg" title={`${met} of ${total} filters met`}>
	<span class="aa-fsum-counter" style={`color:${color};`}>{met}/{total}</span>
	<span class="aa-segments">
		{#each FILTERS as f (f.id)}
			<span
				class="aa-seg"
				style={`background:${job.met.includes(f.id) ? 'var(--accent-success)' : 'rgba(255,255,255,0.10)'};`}
			></span>
		{/each}
	</span>
</span>
