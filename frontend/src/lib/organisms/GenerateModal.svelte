<script>
	import { app } from '$lib/state/sellfish.svelte'
	import Icon from '$lib/atoms/Icon.svelte'
</script>

{#if app.generateJob}
	{@const g = app.generateJob}
	<div
		onclick={app.closeGenerate}
		style="position:fixed;inset:0;z-index:60;background:rgba(5,5,7,.7);backdrop-filter:blur(4px);display:flex;align-items:center;justify-content:center;padding:24px;animation:sfScrim .2s ease"
	>
		<div
			onclick={(e) => e.stopPropagation()}
			style="width:min(680px,100%);max-height:88vh;display:flex;flex-direction:column;background:#111113;border:1px solid rgba(255,255,255,.1);border-radius:18px;box-shadow:0 0 0 1px rgba(124,58,237,.1),0 40px 80px -20px rgba(0,0,0,.7);animation:sfPop .24s ease both;overflow:hidden"
		>
			<header
				style="display:flex;align-items:center;gap:12px;padding:18px 20px;border-bottom:1px solid rgba(255,255,255,.06)"
			>
				<span
					style="width:36px;height:36px;border-radius:10px;display:inline-flex;align-items:center;justify-content:center;background:linear-gradient(135deg,#7c3aed,#06b6d4);box-shadow:0 0 16px rgba(124,58,237,.4)"
				>
					<Icon name="sparkles" size={18} strokeWidth={1.7} style="color:#fff" />
				</span>
				<div style="flex:1;min-width:0">
					<div style="font-weight:600;font-size:1rem">Generate &amp; review</div>
					<div
						style="font-size:.74rem;color:#71717a;white-space:nowrap;overflow:hidden;text-overflow:ellipsis"
					>
						{g.title} · {g.company}
					</div>
				</div>
				<button
					onclick={app.closeGenerate}
					class="sf-close"
					style="width:32px;height:32px;border-radius:8px;background:none;border:1px solid transparent;color:#a1a1aa;cursor:pointer;display:inline-flex;align-items:center;justify-content:center"
				>
					<Icon name="x" size={16} strokeWidth={1.8} />
				</button>
			</header>
			<div style="display:flex;gap:4px;padding:12px 20px 0">
				{#each app.genTabs as gt, i (i)}
					<button
						onclick={gt.onClick}
						style="font-family:'JetBrains Mono',monospace;font-size:.74rem;padding:8px 14px;border-radius:9px 9px 0 0;border:none;cursor:pointer;background:{gt.bg};color:{gt.color};transition:all .15s"
						>{gt.label}</button
					>
				{/each}
			</div>
			<div style="flex:1;overflow-y:auto;padding:16px 20px">
				<div
					style="background:#0a0a0b;border:1px solid rgba(255,255,255,.08);border-radius:12px;padding:18px 20px;font-size:.86rem;line-height:1.7;color:#cbcbd2;white-space:pre-wrap;min-height:240px"
				>
					{app.genText}{#if app.genTyping}<span
							style="display:inline-block;width:8px;height:1.05em;background:#7c3aed;vertical-align:-2px;animation:sfBlink 1s step-end infinite"
						></span>{/if}
				</div>
			</div>
			<footer
				style="display:flex;align-items:center;gap:10px;padding:15px 20px;border-top:1px solid rgba(255,255,255,.06)"
			>
				<button
					onclick={app.copyGen}
					class="sf-copy"
					style="display:inline-flex;align-items:center;gap:7px;font-family:'JetBrains Mono',monospace;font-size:.76rem;cursor:pointer;border-radius:10px;padding:9px 14px;background:rgba(255,255,255,.02);border:1px solid rgba(255,255,255,.1);color:#fafafa;transition:all .15s"
				>
					<Icon name="copy" size={14} strokeWidth={1.7} />{app.copyLabel}
				</button>
				<button
					onclick={app.regen}
					class="sf-regen"
					style="display:inline-flex;align-items:center;gap:7px;font-family:'JetBrains Mono',monospace;font-size:.76rem;cursor:pointer;border-radius:10px;padding:9px 14px;background:rgba(255,255,255,.02);border:1px solid rgba(255,255,255,.1);color:#a1a1aa;transition:all .15s"
				>
					<Icon name="refresh-cw" size={14} strokeWidth={1.7} />Regenerate
				</button>
				<button
					onclick={app.sendApply}
					class="sf-send"
					style="margin-left:auto;display:inline-flex;align-items:center;gap:7px;font-family:'JetBrains Mono',monospace;font-weight:500;font-size:.8rem;cursor:pointer;border-radius:11px;padding:10px 18px;background:#7c3aed;border:none;color:#fff;transition:all .2s"
				>
					<Icon name="send" size={15} strokeWidth={1.7} />Send application
				</button>
			</footer>
		</div>
	</div>
{/if}

<style>
	.sf-close:hover {
		background: rgba(255, 255, 255, 0.05);
		color: #fafafa;
	}
	.sf-copy:hover {
		border-color: rgba(124, 58, 237, 0.3);
	}
	.sf-regen:hover {
		color: #fafafa;
		border-color: rgba(124, 58, 237, 0.3);
	}
	.sf-send:hover {
		background: #8b5cf6;
		transform: translateY(-2px);
		box-shadow:
			0 0 20px rgba(124, 58, 237, 0.4),
			0 0 40px rgba(124, 58, 237, 0.4);
	}
</style>
