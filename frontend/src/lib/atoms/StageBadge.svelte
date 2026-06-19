<script lang="ts">
	import { STAGES, type Stage } from '$lib/utils/data'

	let { stage }: { stage: Stage } = $props()

	const s = $derived(STAGES[stage])
</script>

<span
	class="aa-stage"
	style={`color:${s.color};border-color:color-mix(in srgb, ${s.color} 35%, transparent);background:color-mix(in srgb, ${s.color} 10%, transparent);`}
>
	<span class="aa-stage-dot" style={`background:${s.color};`}></span>{s.label}
</span>
