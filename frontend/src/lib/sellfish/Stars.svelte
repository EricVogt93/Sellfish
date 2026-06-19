<script lang="ts">
	let {
		value = 0,
		size = 15,
		onchange
	}: { value?: number; size?: number; onchange?: (v: number) => void } = $props()

	let hover = $state(0)
</script>

<span class="aa-stars" role="group" onmouseleave={() => (hover = 0)}>
	{#each [1, 2, 3, 4, 5] as n (n)}
		{@const on = n <= (hover || value)}
		<button
			class="aa-star"
			aria-label={`${n} stars`}
			onmouseenter={() => (hover = n)}
			onclick={(e) => {
				e.stopPropagation()
				onchange?.(n === value ? 0 : n)
			}}
		>
			<svg
				width={size}
				height={size}
				viewBox="0 0 24 24"
				fill={on ? 'var(--accent-warning)' : 'none'}
				stroke={on ? 'var(--accent-warning)' : 'var(--text-muted)'}
				stroke-width="1.6"
				stroke-linejoin="round"
			>
				<polygon
					points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"
				></polygon>
			</svg>
		</button>
	{/each}
</span>
