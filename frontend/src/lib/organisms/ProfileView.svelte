<script>
	import { app } from '$lib/state/sellfish.svelte'
	import {
		SALARY,
		SALARY_FACTORS,
		IDENTITY,
		LINKS,
		DOCUMENTS,
		PROVIDERS_RAW,
		ONBOARDING_RAW,
		COUNTRIES
	} from '$lib/data/jobs'

	const providers = PROVIDERS_RAW.map(([name, type, active]) => ({
		name,
		type,
		dot: active ? '#10b981' : '#52525b',
		statusText: active ? 'Active' : 'Idle',
		statusColor: active ? '#10b981' : '#71717a',
		border: active ? 'rgba(16,185,129,.25)' : 'rgba(255,255,255,.06)'
	}))

	const onboarding = ONBOARDING_RAW.map(([label, done]) => ({
		label,
		mark: done ? '✓' : '',
		markBg: done ? '#10b981' : 'transparent',
		markBorder: done ? '#10b981' : 'rgba(255,255,255,.18)',
		color: done ? '#a1a1aa' : '#fafafa'
	}))

	const onbDone = ONBOARDING_RAW.filter(([, done]) => done).length
	const onbLabel = onbDone + '/5'
	const onbPct = (onbDone / 5) * 100 + '%'
</script>

<section data-screen-label="Profile" style="animation:sfUp .3s ease both">
	<div style="margin-bottom:18px">
		<div
			style="font-family:'JetBrains Mono',monospace;font-size:11px;letter-spacing:.08em;text-transform:uppercase;color:#71717a"
		>
			your engine · trains every match
		</div>
		<h1 style="font-size:1.85rem;font-weight:600;letter-spacing:-.02em;margin-top:4px">Profile</h1>
	</div>
	<div
		style="background:linear-gradient(135deg,rgba(124,58,237,.1),rgba(6,182,212,.06));border:1px solid rgba(124,58,237,.22);border-radius:18px;padding:22px 24px;margin-bottom:16px"
	>
		<div style="display:flex;align-items:center;gap:9px;margin-bottom:18px">
			<svg
				width="15"
				height="15"
				viewBox="0 0 24 24"
				fill="none"
				stroke="#8b5cf6"
				stroke-width="1.7"
				stroke-linecap="round"
				stroke-linejoin="round"
				><path d="m12 3 1.9 4.6L19 9.5l-4.6 1.9L12 16l-2.4-4.6L5 9.5l5.1-1.9Z" /></svg
			><span
				style="font-family:'JetBrains Mono',monospace;font-size:11px;letter-spacing:.07em;text-transform:uppercase;color:#a1a1aa"
				>AI salary estimate</span
			>
		</div>
		<div style="display:flex;align-items:flex-end;gap:30px;flex-wrap:wrap">
			<div>
				<div
					style="font-family:'JetBrains Mono',monospace;font-size:.68rem;color:#71717a;text-transform:uppercase;letter-spacing:.05em"
				>
					low
				</div>
				<div
					style="font-family:'JetBrains Mono',monospace;font-size:1.3rem;font-weight:600;color:#a1a1aa"
				>
					{SALARY.low}
				</div>
			</div>
			<div>
				<div
					style="font-family:'JetBrains Mono',monospace;font-size:.68rem;color:#8b5cf6;text-transform:uppercase;letter-spacing:.05em"
				>
					median
				</div>
				<div
					style="font-family:'JetBrains Mono',monospace;font-size:2.3rem;font-weight:700;letter-spacing:-.02em;background:linear-gradient(135deg,#8b5cf6,#06b6d4);-webkit-background-clip:text;background-clip:text;-webkit-text-fill-color:transparent"
				>
					{SALARY.median}
				</div>
			</div>
			<div>
				<div
					style="font-family:'JetBrains Mono',monospace;font-size:.68rem;color:#71717a;text-transform:uppercase;letter-spacing:.05em"
				>
					high
				</div>
				<div
					style="font-family:'JetBrains Mono',monospace;font-size:1.3rem;font-weight:600;color:#a1a1aa"
				>
					{SALARY.high}
				</div>
			</div>
		</div>
		<div
			style="position:relative;height:9px;border-radius:6px;background:linear-gradient(90deg,#7c3aed,#06b6d4,#10b981);margin:16px 0 14px;opacity:.85"
		>
			<div
				style="position:absolute;top:-5px;left:{SALARY.medianPct};width:3px;height:19px;border-radius:2px;background:#fff;box-shadow:0 0 10px rgba(255,255,255,.7)"
			></div>
		</div>
		<div style="display:flex;flex-wrap:wrap;gap:7px;margin-bottom:12px">
			{#each SALARY_FACTORS as fa, i (i)}<span
					style="font-family:'JetBrains Mono',monospace;font-size:.68rem;color:#a1a1aa;background:rgba(255,255,255,.03);border:1px solid rgba(255,255,255,.08);border-radius:999px;padding:4px 11px"
					>{fa}</span
				>{/each}
		</div>
		<div style="font-size:.76rem;color:#71717a">{SALARY.note}</div>
	</div>
	<div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(330px,1fr));gap:16px">
		<div
			style="background:rgba(255,255,255,.02);border:1px solid rgba(255,255,255,.06);border-radius:16px;padding:20px"
		>
			<div
				style="font-family:'JetBrains Mono',monospace;font-size:11px;letter-spacing:.07em;text-transform:uppercase;color:#71717a;margin-bottom:14px"
			>
				Identity
			</div>
			<div style="display:flex;flex-direction:column;gap:11px">
				{#each IDENTITY as [k, v], i (i)}<div
						style="display:flex;align-items:center;justify-content:space-between;gap:12px"
					>
						<span style="font-size:.8rem;color:#71717a">{k}</span><span
							style="font-size:.82rem;color:#fafafa;text-align:right">{v}</span
						>
					</div>{/each}
			</div>
			<div style="height:1px;background:rgba(255,255,255,.06);margin:14px 0"></div>
			<div style="display:flex;flex-direction:column;gap:9px">
				{#each LINKS as [k, v], i (i)}<div style="display:flex;align-items:center;gap:9px">
						<svg
							width="13"
							height="13"
							viewBox="0 0 24 24"
							fill="none"
							stroke="#8b5cf6"
							stroke-width="1.7"
							stroke-linecap="round"
							stroke-linejoin="round"
							><path d="M10 13a5 5 0 0 0 7 0l3-3a5 5 0 0 0-7-7l-1 1" /><path
								d="M14 11a5 5 0 0 0-7 0l-3 3a5 5 0 0 0 7 7l1-1"
							/></svg
						><span style="font-size:.74rem;color:#71717a;width:64px">{k}</span><span
							style="font-size:.78rem;color:#8b5cf6">{v}</span
						>
					</div>{/each}
			</div>
		</div>
		<div
			style="background:rgba(255,255,255,.02);border:1px solid rgba(255,255,255,.06);border-radius:16px;padding:20px"
		>
			<div
				style="font-family:'JetBrains Mono',monospace;font-size:11px;letter-spacing:.07em;text-transform:uppercase;color:#71717a;margin-bottom:14px"
			>
				Preferences
			</div>
			<div style="font-size:.7rem;color:#8b5cf6;margin-bottom:7px">Desired titles</div>
			<div style="display:flex;flex-wrap:wrap;gap:6px;margin-bottom:14px">
				{#each app.prefTitles as c, i (i)}<span
						style="display:inline-flex;align-items:center;gap:6px;font-family:'JetBrains Mono',monospace;font-size:.71rem;padding:5px 6px 5px 11px;border-radius:999px;color:#8b5cf6;background:rgba(124,58,237,.1);border:1px solid rgba(124,58,237,.25)"
						>{c.v}<button
							onclick={c.onRemove}
							class="chip-x"
							style="width:15px;height:15px;border-radius:50%;border:none;background:rgba(255,255,255,.08);color:inherit;cursor:pointer;display:inline-flex;align-items:center;justify-content:center;font-size:.72rem;line-height:1"
							>×</button
						></span
					>{/each}
			</div>
			<div style="font-size:.7rem;color:#06b6d4;margin-bottom:7px">Keywords</div>
			<div style="display:flex;flex-wrap:wrap;gap:6px;margin-bottom:14px">
				{#each app.prefKeywords as c, i (i)}<span
						style="display:inline-flex;align-items:center;gap:6px;font-family:'JetBrains Mono',monospace;font-size:.71rem;padding:5px 6px 5px 11px;border-radius:999px;color:#06b6d4;background:rgba(6,182,212,.1);border:1px solid rgba(6,182,212,.25)"
						>{c.v}<button
							onclick={c.onRemove}
							class="chip-x"
							style="width:15px;height:15px;border-radius:50%;border:none;background:rgba(255,255,255,.08);color:inherit;cursor:pointer;display:inline-flex;align-items:center;justify-content:center;font-size:.72rem;line-height:1"
							>×</button
						></span
					>{/each}
			</div>
			<div style="font-size:.7rem;color:#ef4444;margin-bottom:7px">Excluded companies</div>
			<div style="display:flex;flex-wrap:wrap;gap:6px">
				{#each app.prefExcluded as c, i (i)}<span
						style="display:inline-flex;align-items:center;gap:6px;font-family:'JetBrains Mono',monospace;font-size:.71rem;padding:5px 6px 5px 11px;border-radius:999px;color:#ef4444;background:rgba(239,68,68,.08);border:1px solid rgba(239,68,68,.25)"
						>{c.v}<button
							onclick={c.onRemove}
							class="chip-x"
							style="width:15px;height:15px;border-radius:50%;border:none;background:rgba(255,255,255,.08);color:inherit;cursor:pointer;display:inline-flex;align-items:center;justify-content:center;font-size:.72rem;line-height:1"
							>×</button
						></span
					>{/each}
			</div>
		</div>
		<div
			style="background:rgba(255,255,255,.02);border:1px solid rgba(255,255,255,.06);border-radius:16px;padding:20px"
		>
			<div
				style="font-family:'JetBrains Mono',monospace;font-size:11px;letter-spacing:.07em;text-transform:uppercase;color:#71717a;margin-bottom:14px"
			>
				AI smart import
			</div>
			<div
				onclick={app.smartImport}
				class="dropzone"
				style="border:1.5px dashed rgba(124,58,237,.35);border-radius:13px;padding:22px 16px;text-align:center;cursor:pointer;transition:all .2s;background:rgba(124,58,237,.03)"
			>
				<svg
					width="26"
					height="26"
					viewBox="0 0 24 24"
					fill="none"
					stroke="#8b5cf6"
					stroke-width="1.6"
					stroke-linecap="round"
					stroke-linejoin="round"
					style="margin-bottom:8px"
					><path d="M12 16V4" /><path d="m7 9 5-5 5 5" /><path d="M4 20h16" /></svg
				>
				<div style="font-size:.84rem;color:#fafafa;margin-bottom:3px">Drop a merged PDF</div>
				<div style="font-size:.72rem;color:#71717a;max-width:32ch;margin:0 auto">
					AI runs OCR, splits pages, and classifies each as CV · certificate · reference · cover
					letter
				</div>
			</div>
			<div style="display:flex;flex-direction:column;gap:8px;margin-top:14px">
				<div style="display:flex;align-items:center;gap:8px;font-size:.76rem;color:#a1a1aa">
					<span style="width:6px;height:6px;border-radius:50%;background:#10b981"></span>Auto-labels
					&amp; dates each document
				</div>
				<div style="display:flex;align-items:center;gap:8px;font-size:.76rem;color:#a1a1aa">
					<span style="width:6px;height:6px;border-radius:50%;background:#06b6d4"></span>Derives
					titles, keywords &amp; ex-employers
				</div>
			</div>
		</div>
		<div
			style="background:rgba(255,255,255,.02);border:1px solid rgba(255,255,255,.06);border-radius:16px;padding:20px"
		>
			<div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:14px">
				<span
					style="font-family:'JetBrains Mono',monospace;font-size:11px;letter-spacing:.07em;text-transform:uppercase;color:#71717a"
					>AI providers</span
				><button
					onclick={app.addProvider}
					class="add-btn"
					style="font-family:'JetBrains Mono',monospace;font-size:.68rem;color:#8b5cf6;background:rgba(124,58,237,.1);border:1px solid rgba(124,58,237,.25);border-radius:8px;padding:4px 10px;cursor:pointer"
					>+ Add</button
				>
			</div>
			<div style="display:flex;flex-direction:column;gap:9px">
				{#each providers as pv, i (i)}<div
						style="display:flex;align-items:center;gap:11px;padding:11px 13px;border-radius:11px;background:#16161a;border:1px solid {pv.border}"
					>
						<span
							style="width:8px;height:8px;border-radius:50%;background:{pv.dot};flex:none;box-shadow:0 0 8px {pv.dot}"
						></span>
						<div style="flex:1;min-width:0">
							<div style="font-size:.82rem;color:#fafafa">{pv.name}</div>
							<div style="font-family:'JetBrains Mono',monospace;font-size:.64rem;color:#71717a">
								{pv.type}
							</div>
						</div>
						<span
							style="font-family:'JetBrains Mono',monospace;font-size:.64rem;color:{pv.statusColor}"
							>{pv.statusText}</span
						>
					</div>{/each}
			</div>
		</div>
		<div
			style="background:rgba(255,255,255,.02);border:1px solid rgba(255,255,255,.06);border-radius:16px;padding:20px"
		>
			<div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:6px">
				<span
					style="font-family:'JetBrains Mono',monospace;font-size:11px;letter-spacing:.07em;text-transform:uppercase;color:#71717a"
					>Setup progress</span
				><span style="font-family:'JetBrains Mono',monospace;font-size:.72rem;color:#10b981"
					>{onbLabel}</span
				>
			</div>
			<div
				style="height:6px;border-radius:4px;background:rgba(255,255,255,.06);overflow:hidden;margin-bottom:15px"
			>
				<div
					style="height:100%;width:{onbPct};border-radius:4px;background:linear-gradient(90deg,#10b981,#06b6d4)"
				></div>
			</div>
			<div style="display:flex;flex-direction:column;gap:10px">
				{#each onboarding as ob, i (i)}<div style="display:flex;align-items:center;gap:10px">
						<span
							style="width:18px;height:18px;border-radius:50%;border:1.5px solid {ob.markBorder};background:{ob.markBg};display:inline-flex;align-items:center;justify-content:center;color:#0a0a0b;font-size:.66rem;font-weight:700"
							>{ob.mark}</span
						><span style="font-size:.82rem;color:{ob.color}">{ob.label}</span>
					</div>{/each}
			</div>
		</div>
		<div
			style="background:rgba(255,255,255,.02);border:1px solid rgba(255,255,255,.06);border-radius:16px;padding:20px"
		>
			<div
				style="font-family:'JetBrains Mono',monospace;font-size:11px;letter-spacing:.07em;text-transform:uppercase;color:#71717a;margin-bottom:14px"
			>
				Documents
			</div>
			<div style="display:flex;flex-direction:column;gap:9px">
				{#each DOCUMENTS as [name, type, color], i (i)}<div
						style="display:flex;align-items:center;gap:11px;padding:10px 12px;border-radius:11px;background:#16161a;border:1px solid rgba(255,255,255,.06)"
					>
						<svg
							width="16"
							height="16"
							viewBox="0 0 24 24"
							fill="none"
							stroke={color}
							stroke-width="1.6"
							stroke-linecap="round"
							stroke-linejoin="round"
							style="flex:none"
							><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8Z" /><path
								d="M14 2v6h6"
							/></svg
						><span
							style="flex:1;font-size:.79rem;color:#fafafa;white-space:nowrap;overflow:hidden;text-overflow:ellipsis"
							>{name}</span
						><span
							style="font-family:'JetBrains Mono',monospace;font-size:.62rem;color:{color};border:1px solid {color}33;border-radius:6px;padding:2px 7px"
							>{type}</span
						>
					</div>{/each}
			</div>
			<div style="height:1px;background:rgba(255,255,255,.06);margin:14px 0 12px"></div>
			<div
				style="font-family:'JetBrains Mono',monospace;font-size:11px;letter-spacing:.07em;text-transform:uppercase;color:#71717a;margin-bottom:10px"
			>
				Job countries
			</div>
			<div style="display:flex;flex-wrap:wrap;gap:6px">
				{#each COUNTRIES as co, i (i)}<span
						style="font-family:'JetBrains Mono',monospace;font-size:.7rem;color:#a1a1aa;background:rgba(255,255,255,.03);border:1px solid rgba(255,255,255,.08);border-radius:8px;padding:4px 10px"
						>{co}</span
					>{/each}<span
					style="font-family:'JetBrains Mono',monospace;font-size:.7rem;color:#71717a;background:rgba(255,255,255,.02);border:1px dashed rgba(255,255,255,.12);border-radius:8px;padding:4px 10px"
					>+13 more</span
				>
			</div>
		</div>
	</div>
</section>

<style>
	.chip-x:hover {
		background: rgba(255, 255, 255, 0.2);
	}
	.dropzone:hover {
		border-color: #7c3aed;
		background: rgba(124, 58, 237, 0.07);
	}
	.add-btn:hover {
		background: rgba(124, 58, 237, 0.18);
	}
</style>
