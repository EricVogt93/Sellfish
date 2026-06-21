<script>
	// Jobs-Screen — 1:1 Transkription des Design-Mocks (sellfish_full.html, Zeilen 62-127).
	import { app } from '$lib/state/sellfish.svelte'
	import Icon from '$lib/atoms/Icon.svelte'
	import ScoreRing from '$lib/atoms/ScoreRing.svelte'
	import StarRow from '$lib/atoms/StarRow.svelte'
</script>

<section data-screen-label="Jobs" style="animation:sfUp .3s ease both">
	<div
		style="display:flex;align-items:flex-end;justify-content:space-between;gap:16px;margin-bottom:18px;flex-wrap:wrap"
	>
		<div>
			<div
				style="font-family:'JetBrains Mono',monospace;font-size:11px;letter-spacing:.08em;text-transform:uppercase;color:#71717a"
			>
				inbox · {app.jobCount} matches · ranked by score
			</div>
			<h1 style="font-size:1.85rem;font-weight:600;letter-spacing:-.02em;margin-top:4px">Jobs</h1>
		</div>
		<div style="display:flex;align-items:center;gap:14px">
			<span
				style="font-family:'JetBrains Mono',monospace;font-size:.66rem;color:#71717a;display:inline-flex;gap:6px;align-items:center"
				><kbd
					style="font-family:'JetBrains Mono',monospace;font-size:.6rem;background:#18181b;border:1px solid rgba(255,255,255,.06);border-radius:4px;padding:1.5px 5px"
					>J</kbd
				><kbd
					style="font-family:'JetBrains Mono',monospace;font-size:.6rem;background:#18181b;border:1px solid rgba(255,255,255,.06);border-radius:4px;padding:1.5px 5px"
					>K</kbd
				>
				nav ·
				<kbd
					style="font-family:'JetBrains Mono',monospace;font-size:.6rem;background:#18181b;border:1px solid rgba(255,255,255,.06);border-radius:4px;padding:1.5px 5px"
					>X</kbd
				>
				select ·
				<kbd
					style="font-family:'JetBrains Mono',monospace;font-size:.6rem;background:#18181b;border:1px solid rgba(255,255,255,.06);border-radius:4px;padding:1.5px 5px"
					>↵</kbd
				> open</span
			>
			<button
				class="sf-rescan"
				onclick={app.rescan}
				style="display:inline-flex;align-items:center;gap:8px;font-family:'JetBrains Mono',monospace;font-weight:500;font-size:.78rem;cursor:pointer;border-radius:11px;padding:9px 15px;background:rgba(255,255,255,.02);border:1px solid rgba(255,255,255,.1);color:#fafafa;transition:all .2s"
				><span class={app.scanning ? 'sf-spin' : ''} style="display:inline-flex"
					><Icon name="refresh" size={14} strokeWidth={1.8} /></span
				>{app.rescanLabel}</button
			>
		</div>
	</div>
	{#if app.scanning}
		<div
			style="position:relative;height:3px;border-radius:2px;overflow:hidden;background:rgba(255,255,255,.06);margin-bottom:14px"
		>
			<div
				style="position:absolute;top:0;width:30%;height:100%;border-radius:2px;background:linear-gradient(90deg,transparent,#7c3aed,#06b6d4,transparent);animation:sfScan 1.1s ease-in-out infinite"
			></div>
		</div>
	{/if}
	{#if app.spotlight}
		{@const sp = app.spotlight}
		<div
			class="sf-spotlight"
			onclick={sp.onOpen}
			style="position:relative;overflow:hidden;border:1px solid rgba(124,58,237,.22);border-radius:22px;background:linear-gradient(135deg,rgba(124,58,237,.13),rgba(6,182,212,.06));padding:26px 28px;margin-bottom:22px;cursor:pointer;transition:all .2s"
		>
			<div
				style="position:absolute;top:-70px;right:-40px;width:300px;height:300px;border-radius:50%;background:radial-gradient(circle,rgba(124,58,237,.2),transparent 70%);filter:blur(22px);pointer-events:none"
			></div>
			<div
				style="position:relative;display:grid;grid-template-columns:1fr auto;gap:30px;align-items:center"
			>
				<div style="min-width:0">
					<div style="display:flex;align-items:center;gap:10px;margin-bottom:15px;flex-wrap:wrap">
						<span
							style="font-family:'JetBrains Mono',monospace;font-size:.6rem;letter-spacing:.12em;text-transform:uppercase;color:#fff;background:linear-gradient(135deg,#7c3aed,#06b6d4);border-radius:999px;padding:4px 11px;box-shadow:0 0 16px rgba(124,58,237,.5)"
							>★ Top match</span
						>
						<span style="font-family:'JetBrains Mono',monospace;font-size:.66rem;color:#a1a1aa"
							>{sp.metLabel} signals matched · via {sp.source} · {sp.posted}</span
						>
					</div>
					<div style="display:flex;align-items:center;gap:14px;margin-bottom:13px">
						<span
							style="flex:none;width:50px;height:50px;border-radius:13px;display:inline-flex;align-items:center;justify-content:center;font-family:'JetBrains Mono',monospace;font-weight:600;font-size:1rem;background:{sp.markBg};border:1px solid {sp.markBorder};color:{sp.markColor}"
							>{sp.initials}</span
						>
						<div style="min-width:0">
							<h2
								style="font-size:1.5rem;font-weight:600;letter-spacing:-.02em;line-height:1.15;white-space:nowrap;overflow:hidden;text-overflow:ellipsis"
							>
								{sp.title}
							</h2>
							<div style="font-size:.84rem;color:#a1a1aa">
								{sp.company} · {sp.location} · {sp.salary}
							</div>
						</div>
					</div>
					<div
						style="font-size:.88rem;color:#cbcbd2;line-height:1.6;max-width:60ch;margin-bottom:16px"
					>
						{sp.blurb}
					</div>
					<div style="display:flex;flex-wrap:wrap;gap:6px;margin-bottom:18px">
						{#each sp.metChips ?? [] as flt}<span
								style="display:inline-flex;align-items:center;gap:6px;font-family:'JetBrains Mono',monospace;font-size:.68rem;padding:4px 11px;border-radius:999px;color:#10b981;background:rgba(16,185,129,.08);border:1px solid rgba(16,185,129,.3)"
								><span style="width:5px;height:5px;border-radius:50%;background:#10b981"
								></span>{flt.label}</span
							>{/each}
					</div>
					<div
						style="display:flex;align-items:center;gap:12px;flex-wrap:wrap"
						onclick={sp.stopProp}
					>
						<button
							class="sf-applybtn"
							onclick={(e) => sp.onQuick(e)}
							style="display:inline-flex;align-items:center;gap:8px;font-family:'JetBrains Mono',monospace;font-weight:500;font-size:.82rem;cursor:pointer;border-radius:12px;padding:11px 18px;background:#7c3aed;border:none;color:#fff;transition:all .2s"
							><Icon name="zap" size={15} strokeWidth={1.7} />Quick apply</button
						>
						<button
							class="sf-genbtn"
							onclick={(e) => sp.onGen(e)}
							style="display:inline-flex;align-items:center;gap:8px;font-family:'JetBrains Mono',monospace;font-weight:500;font-size:.82rem;cursor:pointer;border-radius:12px;padding:11px 18px;background:rgba(255,255,255,.03);border:1px solid rgba(255,255,255,.12);color:#fafafa;transition:all .2s"
							><Icon name="pen" size={15} strokeWidth={1.7} />Generate</button
						>
						<div style="display:inline-flex;gap:2px;margin-left:2px">
							<StarRow stars={sp.stars} size={17} gap="2px" hoverScale="1.2" />
						</div>
					</div>
				</div>
				<div
					style="position:relative;width:150px;height:150px;display:inline-flex;align-items:center;justify-content:center;flex:none"
				>
					<svg width="150" height="150" viewBox="0 0 36 36" style="transform:rotate(-90deg)"
						><circle
							cx="18"
							cy="18"
							r="15.9"
							fill="none"
							stroke="rgba(255,255,255,.07)"
							stroke-width="2.4"
						/><circle
							cx="18"
							cy="18"
							r="15.9"
							fill="none"
							stroke={sp.scoreColor}
							stroke-width="2.4"
							stroke-linecap="round"
							pathLength="100"
							stroke-dasharray={sp.dash}
						/></svg
					>
					<div style="position:absolute;text-align:center">
						<div
							style="font-family:'JetBrains Mono',monospace;font-weight:700;font-size:2.5rem;letter-spacing:-.02em;color:{sp.scoreColor};line-height:1"
						>
							{sp.score}
						</div>
						<div
							style="font-family:'JetBrains Mono',monospace;font-size:.6rem;letter-spacing:.1em;text-transform:uppercase;color:#71717a"
						>
							match
						</div>
					</div>
				</div>
			</div>
		</div>
	{/if}
	<div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(330px,1fr));gap:14px">
		{#each app.restJobs as job (job.id)}
			<div
				class="sf-jobcard"
				onclick={job.onOpen}
				style="position:relative;background:rgba(255,255,255,.02);backdrop-filter:blur(12px);-webkit-backdrop-filter:blur(12px);border:1px solid rgba(255,255,255,.06);border-radius:18px;padding:17px;cursor:pointer;opacity:{job.rowOpacity};box-shadow:{job.cardRing};transition:all .2s"
			>
				<div style="display:flex;align-items:flex-start;gap:12px;margin-bottom:14px">
					<span
						style="flex:none;width:40px;height:40px;border-radius:11px;display:inline-flex;align-items:center;justify-content:center;font-family:'JetBrains Mono',monospace;font-weight:600;font-size:.8rem;background:{job.markBg};border:1px solid {job.markBorder};color:{job.markColor}"
						>{job.initials}</span
					>
					<div style="flex:1;min-width:0">
						<div
							style="font-weight:600;font-size:.96rem;color:#fafafa;line-height:1.25;overflow:hidden;text-overflow:ellipsis;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical"
						>
							{job.title}
						</div>
						<div
							style="font-size:.72rem;color:#71717a;margin-top:3px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis"
						>
							{job.company} · {job.source}
						</div>
					</div>
					<div
						style="position:relative;width:42px;height:42px;flex:none;display:inline-flex;align-items:center;justify-content:center"
					>
						<ScoreRing score={job.score} color={job.scoreColor} dash={job.dash} />
					</div>
				</div>
				<div
					style="display:flex;align-items:center;justify-content:space-between;gap:8px;margin-bottom:13px"
				>
					<span
						style="display:inline-flex;align-items:center;gap:5px;font-size:.74rem;color:#a1a1aa;min-width:0"
						><Icon name="mapPin" size={12} strokeWidth={1.6} style="flex:none" /><span
							style="white-space:nowrap;overflow:hidden;text-overflow:ellipsis">{job.location}</span
						></span
					>
					<span
						style="font-family:'JetBrains Mono',monospace;font-size:.74rem;color:#06b6d4;flex:none"
						>{job.salary}</span
					>
				</div>
				<div style="display:flex;align-items:center;gap:8px;margin-bottom:14px">
					<div style="flex:1;display:flex;gap:3px">
						{#each job.filters as flt}<span
								style="flex:1;height:5px;border-radius:3px;background:{flt.dotColor}"
								title={flt.label}
							></span>{/each}
					</div>
					<span
						style="font-family:'JetBrains Mono',monospace;font-size:.66rem;color:{job.metColor};flex:none"
						>{job.metLabel}</span
					>
				</div>
				<div
					style="display:flex;align-items:center;justify-content:space-between;padding-top:13px;border-top:1px solid rgba(255,255,255,.05)"
				>
					<div style="display:inline-flex;gap:1px" onclick={job.stopProp}>
						<StarRow stars={job.stars} size={14} gap="1px" hoverScale="1.25" />
					</div>
					<div style="display:inline-flex;gap:4px" onclick={job.stopProp}>
						<button
							class="sf-applybtn-sm"
							onclick={(e) => job.onQuick(e)}
							title="Quick apply"
							style="display:inline-flex;align-items:center;gap:6px;font-family:'JetBrains Mono',monospace;font-size:.7rem;border-radius:9px;padding:6px 11px;background:rgba(124,58,237,.12);border:1px solid rgba(124,58,237,.25);color:#8b5cf6;cursor:pointer;transition:all .15s"
							><Icon name="zap" size={13} strokeWidth={1.7} />Apply</button
						>
						<button
							class="sf-genbtn-sm"
							onclick={(e) => job.onGen(e)}
							title="Generate & review"
							style="width:30px;height:30px;border-radius:9px;background:none;border:1px solid rgba(255,255,255,.08);color:#71717a;cursor:pointer;display:inline-flex;align-items:center;justify-content:center;transition:all .15s"
							><Icon name="pen" size={13} strokeWidth={1.7} /></button
						>
					</div>
				</div>
			</div>
		{/each}
	</div>
</section>

<style>
	.sf-spin {
		animation: sfSpin 1s linear infinite;
	}
	.sf-rescan:hover {
		border-color: rgba(124, 58, 237, 0.3);
		background: rgba(255, 255, 255, 0.04);
	}
	.sf-spotlight:hover {
		border-color: rgba(124, 58, 237, 0.4);
	}
	.sf-applybtn:hover {
		background: #8b5cf6;
		transform: translateY(-2px);
		box-shadow:
			0 0 20px rgba(124, 58, 237, 0.4),
			0 0 40px rgba(124, 58, 237, 0.4);
	}
	.sf-genbtn:hover {
		border-color: rgba(124, 58, 237, 0.4);
	}
	.sf-jobcard:hover {
		border-color: rgba(124, 58, 237, 0.3);
		transform: translateY(-4px);
		box-shadow:
			0 0 0 1px rgba(124, 58, 237, 0.15),
			0 20px 40px -16px rgba(0, 0, 0, 0.6);
	}
	.sf-applybtn-sm:hover {
		background: rgba(124, 58, 237, 0.2);
		box-shadow: 0 0 12px rgba(124, 58, 237, 0.35);
	}
	.sf-genbtn-sm:hover {
		color: #fafafa;
		border-color: rgba(255, 255, 255, 0.16);
	}
</style>
