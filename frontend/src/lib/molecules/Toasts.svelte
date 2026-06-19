<script lang="ts">
	import Icon from '$lib/atoms/Icon.svelte'
	import { getToasts } from '$lib/utils/toasts.svelte'

	const toasts = $derived(getToasts())
</script>

<div class="aa-toasts">
	{#each toasts as t (t.id)}
		<div class="aa-toast">
			<Icon
				name={t.icon || 'check'}
				size={14}
				style={`color:${t.color || 'var(--accent-success)'};`}
			/>
			<span>{t.msg}</span>
		</div>
	{/each}
</div>
