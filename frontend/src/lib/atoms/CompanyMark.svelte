<script lang="ts">
	import type { Job } from '$lib/utils/data'

	let { job, size = 0 }: { job: Job; size?: number } = $props()

	// size 0 = density-controlled default size (--mark-size from CSS)
	const sized = $derived(
		size > 0
			? `width:${size}px;height:${size}px;font-size:${size * 0.35}px;`
			: `font-size: calc(var(--mark-size) * 0.35);`
	)
</script>

<span
	class="aa-companymark"
	style={`${sized}background:hsl(${job.hue} 60% 50% / 0.14);border:1px solid hsl(${job.hue} 60% 60% / 0.3);color:hsl(${job.hue} 75% 72%);`}
	>{job.initials}</span
>
