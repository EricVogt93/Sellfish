<script lang="ts">
	import { app } from '$lib/state/sellfish.svelte'

	let inputEl = $state<HTMLInputElement>()

	$effect(() => {
		inputEl?.focus()
	})
</script>

<div
	onclick={app.closePalette}
	style="position:fixed;inset:0;z-index:70;background:rgba(5,5,7,.6);backdrop-filter:blur(4px);display:flex;align-items:flex-start;justify-content:center;padding:14vh 24px 24px;animation:sfScrim .15s ease"
>
	<div
		onclick={(e) => e.stopPropagation()}
		style="width:min(580px,100%);background:#16161a;border:1px solid rgba(255,255,255,.12);border-radius:16px;box-shadow:0 0 0 1px rgba(124,58,237,.12),0 40px 90px -20px rgba(0,0,0,.8);overflow:hidden;animation:sfPop .18s ease both"
	>
		<div
			style="display:flex;align-items:center;gap:11px;padding:15px 18px;border-bottom:1px solid rgba(255,255,255,.06)"
		>
			<svg
				width="17"
				height="17"
				viewBox="0 0 24 24"
				fill="none"
				stroke="#71717a"
				stroke-width="1.8"
				stroke-linecap="round"><circle cx="11" cy="11" r="7" /><path d="m21 21-4.3-4.3" /></svg
			>
			<input
				bind:this={inputEl}
				value={app.paletteQuery}
				oninput={app.onPaletteInput}
				placeholder="Search jobs, jump to a tab, run a command…"
				style="flex:1;background:none;border:none;color:#fafafa;font-size:.92rem"
			/>
			<span
				style="font-family:'JetBrains Mono',monospace;font-size:.62rem;color:#71717a;background:#0a0a0b;border:1px solid rgba(255,255,255,.06);border-radius:5px;padding:2px 7px"
				>Esc</span
			>
		</div>
		<div style="max-height:46vh;overflow-y:auto;padding:8px">
			{#each app.paletteResults as r, i (i)}
				<button
					class="sf-cmdrow"
					onclick={r.onClick}
					style="display:flex;align-items:center;gap:12px;width:100%;background:none;border:none;cursor:pointer;border-radius:10px;padding:11px 12px;text-align:left;transition:background .12s"
				>
					<span
						style="width:8px;height:8px;border-radius:50%;background:{r.dot};flex:none;box-shadow:0 0 8px {r.dot}"
					></span>
					<span style="flex:1;min-width:0">
						<span
							style="display:block;font-size:.86rem;color:#fafafa;white-space:nowrap;overflow:hidden;text-overflow:ellipsis"
							>{r.label}</span
						>
						<span
							style="display:block;font-family:'JetBrains Mono',monospace;font-size:.66rem;color:#71717a;white-space:nowrap;overflow:hidden;text-overflow:ellipsis"
							>{r.sub}</span
						>
					</span>
					<svg
						width="14"
						height="14"
						viewBox="0 0 24 24"
						fill="none"
						stroke="#52525b"
						stroke-width="1.7"
						stroke-linecap="round"
						stroke-linejoin="round"><path d="m9 18 6-6-6-6" /></svg
					>
				</button>
			{/each}
			{#if app.paletteEmpty}
				<div style="text-align:center;padding:22px;font-size:.82rem;color:#71717a">No results</div>
			{/if}
		</div>
	</div>
</div>

<style>
	.sf-cmdrow:hover {
		background: rgba(124, 58, 237, 0.12);
	}
</style>
