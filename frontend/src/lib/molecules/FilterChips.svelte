<script lang="ts">
	import Icon from '$lib/atoms/Icon.svelte'
	import { FILTERS, type Job } from '$lib/utils/data'

	let { job }: { job: Job } = $props()
</script>

<div class="aa-chips">
	{#each FILTERS as f (f.id)}
		{@const ok = job.met.includes(f.id)}
		<span class={`aa-chip ${ok ? 'aa-chip-met' : 'aa-chip-unmet'}`}>
			<Icon name={ok ? 'check' : 'x'} size={11} strokeWidth={2.5} />
			{f.label}
		</span>
	{/each}
</div>
