<script lang="ts">
	// ── Sellfish · Toasts (1:1 aus Sellfish.dc.html) ──
	import { app } from '$lib/state/sellfish.svelte'
</script>

<div
	style="position:fixed;bottom:22px;right:22px;z-index:80;display:flex;flex-direction:column;gap:10px"
>
	{#each app.toasts as t (t.id)}
		<div
			style="display:flex;align-items:center;gap:10px;background:#18181b;border:1px solid rgba(255,255,255,.1);border-left:3px solid {t.color};border-radius:11px;padding:11px 15px;box-shadow:0 12px 30px rgba(0,0,0,.4);animation:sfUp .25s ease both;min-width:220px"
		>
			<span
				style="width:7px;height:7px;border-radius:50%;background:{t.color};box-shadow:0 0 8px {t.color}"
			></span>
			<span style="font-size:.82rem;color:#fafafa">{t.msg}</span>
		</div>
	{/each}
</div>
