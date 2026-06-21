<script lang="ts">
	// Kreisförmiger Match-Score (1:1 aus Sellfish.dc.html)
	let {
		score,
		color,
		dash,
		size = 42,
		stroke = 3.2,
		fontSize = '.72rem'
	}: {
		score: number
		color: string
		dash: string
		size?: number
		stroke?: number
		fontSize?: string
	} = $props()
</script>

<div
	style="position:relative;width:{size}px;height:{size}px;flex:none;display:inline-flex;align-items:center;justify-content:center"
>
	<svg width={size} height={size} viewBox="0 0 36 36" style="transform:rotate(-90deg)">
		<circle
			cx="18"
			cy="18"
			r="15.9"
			fill="none"
			stroke="rgba(255,255,255,.07)"
			stroke-width={stroke}
		/>
		<circle
			cx="18"
			cy="18"
			r="15.9"
			fill="none"
			stroke={color}
			stroke-width={stroke}
			stroke-linecap="round"
			pathLength="100"
			stroke-dasharray={dash}
		/>
	</svg>
	<span
		style="position:absolute;font-family:'JetBrains Mono',monospace;font-weight:{size >= 100
			? 700
			: 600};font-size:{fontSize};color:{color}{size >= 100 ? ';line-height:1' : ''}">{score}</span
	>
</div>
