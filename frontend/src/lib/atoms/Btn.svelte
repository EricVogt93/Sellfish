<script lang="ts">
	import Icon from '$lib/atoms/Icon.svelte'
	import type { Snippet } from 'svelte'

	let {
		variant = 'secondary',
		icon = '',
		size = 'md',
		disabled = false,
		title = '',
		style = '',
		onclick,
		children
	}: {
		variant?: 'primary' | 'secondary' | 'ghost'
		icon?: string
		size?: 'md' | 'sm'
		disabled?: boolean
		title?: string
		style?: string
		onclick?: (e: MouseEvent) => void
		children?: Snippet
	} = $props()
</script>

<button {title} {disabled} {onclick} {style} class={`aa-btn aa-btn-${variant} aa-btn-${size}`}>
	{#if icon}<Icon name={icon} size={size === 'sm' ? 13 : 15} />{/if}
	{@render children?.()}
</button>
