<script lang="ts">
	import type { StarVM } from '$lib/state/sellfish.svelte'
	let {
		stars,
		size = 14,
		gap = '1px',
		hoverScale = '1.25'
	}: { stars: StarVM[]; size?: number; gap?: string; hoverScale?: string } = $props()
</script>

<div style="display:inline-flex;gap:{gap}">
	{#each stars as st, i (i)}
		<button
			onclick={st.onClick}
			class="sf-star"
			style="--hs:{hoverScale};background:none;border:none;cursor:pointer;padding:1px;display:inline-flex;transition:transform .15s"
		>
			<svg
				width={size}
				height={size}
				viewBox="0 0 24 24"
				fill={st.fill}
				stroke={st.stroke}
				stroke-width="1.4"
				stroke-linejoin="round"
			>
				<path d="m12 2 3.1 6.3 6.9 1-5 4.9 1.2 6.8L12 18l-6.2 3.3 1.2-6.8-5-4.9 6.9-1Z" />
			</svg>
		</button>
	{/each}
</div>

<style>
	.sf-star:hover {
		transform: scale(var(--hs));
	}
</style>
