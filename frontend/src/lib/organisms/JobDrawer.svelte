<script lang="ts">
	import { app } from '$lib/state/sellfish.svelte'
	import ScoreRing from '$lib/atoms/ScoreRing.svelte'
	import StarRow from '$lib/atoms/StarRow.svelte'
</script>

{#if app.drawerJob}
	{@const job = app.drawerJob}
	<div
		onclick={app.closeDrawer}
		style="position:fixed;inset:0;z-index:50;background:rgba(5,5,7,.65);backdrop-filter:blur(3px);animation:sfScrim .2s ease"
	></div>
	<aside
		style="position:fixed;top:0;right:0;bottom:0;width:min(560px,94vw);z-index:51;background:#111113;border-left:1px solid rgba(255,255,255,.08);box-shadow:-30px 0 80px rgba(0,0,0,.5);display:flex;flex-direction:column;animation:sfDrawer .28s cubic-bezier(.22,1,.36,1)"
	>
		<header
			style="display:flex;align-items:center;gap:13px;padding:18px 20px;border-bottom:1px solid rgba(255,255,255,.06)"
		>
			<span
				style="flex:none;width:44px;height:44px;border-radius:11px;display:inline-flex;align-items:center;justify-content:center;font-family:'JetBrains Mono',monospace;font-weight:600;font-size:.9rem;background:{job.markBg};border:1px solid {job.markBorder};color:{job.markColor}"
				>{job.initials}</span
			>
			<div style="flex:1;min-width:0">
				<h2
					style="font-size:1.12rem;font-weight:600;letter-spacing:-.01em;white-space:nowrap;overflow:hidden;text-overflow:ellipsis"
				>
					{job.title}
				</h2>
				<div style="font-size:.76rem;color:#71717a">
					{job.company} · {job.location} · via {job.source}
				</div>
			</div>
			<button
				onclick={app.closeDrawer}
				class="sf-close"
				style="width:32px;height:32px;border-radius:8px;background:none;border:1px solid transparent;color:#a1a1aa;cursor:pointer;display:inline-flex;align-items:center;justify-content:center"
				><svg
					width="16"
					height="16"
					viewBox="0 0 24 24"
					fill="none"
					stroke="currentColor"
					stroke-width="1.8"
					stroke-linecap="round"><path d="M18 6 6 18M6 6l12 12" /></svg
				></button
			>
		</header>
		<div style="flex:1;overflow-y:auto;padding:20px">
			<div style="display:flex;gap:14px;margin-bottom:22px">
				<div
					style="flex:1;display:flex;align-items:center;gap:12px;background:rgba(255,255,255,.02);border:1px solid rgba(255,255,255,.06);border-radius:12px;padding:13px 15px"
				>
					<ScoreRing
						score={job.score}
						color={job.scoreColor}
						dash={job.dash}
						size={50}
						stroke={3.2}
						fontSize=".84rem"
					/>
					<div>
						<div
							style="font-family:'JetBrains Mono',monospace;font-size:.7rem;letter-spacing:.05em;text-transform:uppercase;color:#a1a1aa"
						>
							match score
						</div>
						<div style="font-size:.72rem;color:#71717a">from your signals &amp; history</div>
					</div>
				</div>
				<div
					style="flex:1;display:flex;flex-direction:column;justify-content:center;gap:7px;background:rgba(255,255,255,.02);border:1px solid rgba(255,255,255,.06);border-radius:12px;padding:13px 15px"
				>
					<div style="display:inline-flex;gap:2px" onclick={job.stopProp}>
						<StarRow stars={job.starsLg} size={18} gap="2px" hoverScale="1.2" />
					</div>
					<div>
						<div
							style="font-family:'JetBrains Mono',monospace;font-size:.7rem;letter-spacing:.05em;text-transform:uppercase;color:#a1a1aa"
						>
							your rating
						</div>
						<div style="font-size:.72rem;color:#71717a">trains the score over time</div>
					</div>
				</div>
			</div>
			<div
				style="font-family:'JetBrains Mono',monospace;font-size:11px;letter-spacing:.08em;text-transform:uppercase;color:#71717a;margin-bottom:8px"
			>
				summary
			</div>
			<div style="font-size:.86rem;color:#a1a1aa;line-height:1.65;margin-bottom:16px">
				{job.blurb}
			</div>
			<div style="display:flex;flex-wrap:wrap;gap:7px;margin-bottom:22px">
				<span
					style="display:inline-flex;align-items:center;gap:6px;font-family:'JetBrains Mono',monospace;font-size:.7rem;color:#a1a1aa;background:rgba(255,255,255,.02);border:1px solid rgba(255,255,255,.08);border-radius:8px;padding:5px 10px"
					><svg
						width="12"
						height="12"
						viewBox="0 0 24 24"
						fill="none"
						stroke="#71717a"
						stroke-width="1.7"
						><rect x="2" y="7" width="20" height="14" rx="2" /><path
							d="M16 7V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v2"
						/></svg
					>{job.seniority} · {job.type}</span
				><span
					style="display:inline-flex;align-items:center;gap:6px;font-family:'JetBrains Mono',monospace;font-size:.7rem;color:#06b6d4;background:rgba(6,182,212,.06);border:1px solid rgba(6,182,212,.25);border-radius:8px;padding:5px 10px"
					>{job.salary}</span
				><span
					style="display:inline-flex;align-items:center;gap:6px;font-family:'JetBrains Mono',monospace;font-size:.7rem;color:#a1a1aa;background:rgba(255,255,255,.02);border:1px solid rgba(255,255,255,.08);border-radius:8px;padding:5px 10px"
					><svg
						width="12"
						height="12"
						viewBox="0 0 24 24"
						fill="none"
						stroke="#71717a"
						stroke-width="1.7"><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></svg
					>posted {job.posted} ago</span
				>
			</div>
			<div
				style="font-family:'JetBrains Mono',monospace;font-size:11px;letter-spacing:.08em;text-transform:uppercase;color:#71717a;margin-bottom:10px"
			>
				signal matching · {job.metLabel}
			</div>
			<div style="display:flex;flex-wrap:wrap;gap:6px;margin-bottom:22px">
				{#each job.filters as flt, i (i)}<span
						style="display:inline-flex;align-items:center;gap:6px;font-family:'JetBrains Mono',monospace;font-size:.7rem;padding:4px 11px;border-radius:999px;color:{flt.chipColor};background:{flt.chipBg};border:1px solid {flt.chipBorder}"
						><span style="width:6px;height:6px;border-radius:50%;background:{flt.chipColor}"
						></span>{flt.label}</span
					>{/each}
			</div>
			<div
				style="font-family:'JetBrains Mono',monospace;font-size:11px;letter-spacing:.08em;text-transform:uppercase;color:#71717a;margin-bottom:10px"
			>
				ai tools
			</div>
			<div style="display:flex;gap:8px;margin-bottom:10px">
				<button
					onclick={job.onInterview}
					class="sf-aitool"
					style="display:flex;align-items:center;gap:7px;padding:8px 13px;border:1px solid rgba(255,255,255,.1);border-radius:9px;background:#18181b;color:#a1a1aa;font-size:.78rem;cursor:pointer;transition:all .15s"
					><svg
						width="13"
						height="13"
						viewBox="0 0 24 24"
						fill="none"
						stroke="#8b5cf6"
						stroke-width="1.7"
						stroke-linecap="round"
						stroke-linejoin="round"
						><path d="m12 3 1.9 4.6L19 9.5l-4.6 1.9L12 16l-2.4-4.6L5 9.5l5.1-1.9Z" /></svg
					>{job.interviewLabel}</button
				><button
					onclick={job.onCompany}
					class="sf-aitool"
					style="display:flex;align-items:center;gap:7px;padding:8px 13px;border:1px solid rgba(255,255,255,.1);border-radius:9px;background:#18181b;color:#a1a1aa;font-size:.78rem;cursor:pointer;transition:all .15s"
					><svg
						width="13"
						height="13"
						viewBox="0 0 24 24"
						fill="none"
						stroke="#06b6d4"
						stroke-width="1.7"
						stroke-linecap="round"><circle cx="11" cy="11" r="7" /><path d="m21 21-4.3-4.3" /></svg
					>{job.companyLabel}</button
				>
			</div>
			{#if job.aiText}<div
					style="padding:13px 15px;background:rgba(124,58,237,.05);border:1px solid rgba(124,58,237,.2);border-radius:11px;font-size:.82rem;line-height:1.65;color:#cbcbd2;white-space:pre-wrap;margin-bottom:6px"
				>
					{job.aiText}
				</div>{/if}
		</div>
		<footer
			style="display:flex;align-items:center;gap:10px;padding:15px 20px;border-top:1px solid rgba(255,255,255,.06)"
		>
			{#if job.applied}<span
					style="display:inline-flex;align-items:center;gap:6px;font-family:'JetBrains Mono',monospace;font-size:.72rem;color:#10b981;background:rgba(16,185,129,.1);border:1px solid rgba(16,185,129,.3);border-radius:999px;padding:6px 13px"
					><svg
						width="13"
						height="13"
						viewBox="0 0 24 24"
						fill="none"
						stroke="currentColor"
						stroke-width="2.4"
						stroke-linecap="round"
						stroke-linejoin="round"><path d="m20 6-11 11-5-5" /></svg
					>Application sent</span
				>{/if}
			{#if job.notApplied}<button
					onclick={job.onQuick}
					class="sf-quick"
					style="display:inline-flex;align-items:center;gap:7px;font-family:'JetBrains Mono',monospace;font-weight:500;font-size:.8rem;cursor:pointer;border-radius:11px;padding:10px 16px;background:#7c3aed;border:none;color:#fff;transition:all .2s"
					><svg
						width="14"
						height="14"
						viewBox="0 0 24 24"
						fill="none"
						stroke="currentColor"
						stroke-width="1.7"
						stroke-linecap="round"
						stroke-linejoin="round"><path d="M13 2 3 14h9l-1 8 10-12h-9l1-8Z" /></svg
					>Quick apply</button
				><button
					onclick={job.onGen}
					class="sf-gen"
					style="display:inline-flex;align-items:center;gap:7px;font-family:'JetBrains Mono',monospace;font-weight:500;font-size:.8rem;cursor:pointer;border-radius:11px;padding:10px 16px;background:rgba(255,255,255,.02);border:1px solid rgba(255,255,255,.1);color:#fafafa;transition:all .2s"
					><svg
						width="14"
						height="14"
						viewBox="0 0 24 24"
						fill="none"
						stroke="currentColor"
						stroke-width="1.7"
						stroke-linecap="round"
						stroke-linejoin="round"
						><path d="M12 20h9" /><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z" /></svg
					>Generate &amp; review</button
				>{/if}
			<span
				style="margin-left:auto;font-family:'JetBrains Mono',monospace;font-size:.66rem;color:#71717a"
				><kbd
					style="font-family:'JetBrains Mono',monospace;font-size:.6rem;background:#18181b;border:1px solid rgba(255,255,255,.06);border-radius:4px;padding:1.5px 5px"
					>Esc</kbd
				> close</span
			>
		</footer>
	</aside>
{/if}

<style>
	.sf-close:hover {
		background: rgba(255, 255, 255, 0.05);
		color: #fafafa;
	}
	.sf-aitool:hover {
		border-color: #7c3aed;
		color: #fafafa;
	}
	.sf-quick:hover {
		background: #8b5cf6;
		transform: translateY(-2px);
		box-shadow:
			0 0 20px rgba(124, 58, 237, 0.4),
			0 0 40px rgba(124, 58, 237, 0.4);
	}
	.sf-gen:hover {
		border-color: rgba(124, 58, 237, 0.3);
		background: rgba(255, 255, 255, 0.04);
	}
</style>
