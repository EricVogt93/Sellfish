<script lang="ts">
	import { scoreColor } from '$lib/utils/data'

	let { score, size = 36 }: { score: number; size?: number } = $props()

	const color = $derived(scoreColor(score))
	const r = $derived((size - 5) / 2)
	const c = $derived(2 * Math.PI * r)
</script>

<span
	class="aa-score-ring"
	title={`Match score ${score}`}
	style={`width:${size}px;height:${size}px;`}
>
	<svg width={size} height={size}>
		<circle
			cx={size / 2}
			cy={size / 2}
			{r}
			fill="none"
			stroke="rgba(255,255,255,0.08)"
			stroke-width="3"
		></circle>
		<circle
			cx={size / 2}
			cy={size / 2}
			{r}
			fill="none"
			stroke={color}
			stroke-width="3"
			stroke-linecap="round"
			stroke-dasharray={`${(score / 100) * c} ${c}`}
			transform={`rotate(-90 ${size / 2} ${size / 2})`}
		></circle>
	</svg>
	<span style={`color:${color};font-size:${size * 0.31}px;`}>{score}</span>
</span>
