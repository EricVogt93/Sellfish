<script lang="ts">
	import {
		KPIS,
		STATUS_DIST,
		SALARY_BY_TITLE,
		PERCENTILES,
		MATCHES_DAILY,
		GENS_DAILY,
		drawBar
	} from '$lib/data/jobs'

	let matchesEl = $state<HTMLCanvasElement>()
	let gensEl = $state<HTMLCanvasElement>()

	$effect(() => {
		const m = matchesEl
		const g = gensEl
		if (m) requestAnimationFrame(() => drawBar(m, MATCHES_DAILY, '#7c3aed', '#06b6d4'))
		if (g) requestAnimationFrame(() => drawBar(g, GENS_DAILY, '#ec4899', '#8b5cf6'))
	})
</script>

<section data-screen-label="Reports" style="animation:sfUp .3s ease both">
	<div style="margin-bottom:18px">
		<div
			style="font-family:'JetBrains Mono',monospace;font-size:11px;letter-spacing:.08em;text-transform:uppercase;color:#71717a"
		>
			last 30 days
		</div>
		<h1 style="font-size:1.85rem;font-weight:600;letter-spacing:-.02em;margin-top:4px">Reports</h1>
	</div>
	<div
		style="display:grid;grid-template-columns:repeat(auto-fit,minmax(150px,1fr));gap:12px;margin-bottom:16px"
	>
		{#each KPIS as k, i (i)}<div
				style="background:rgba(255,255,255,.02);border:1px solid rgba(255,255,255,.06);border-radius:14px;padding:16px 18px"
			>
				<div
					style="font-family:'JetBrains Mono',monospace;font-size:1.7rem;font-weight:700;color:{k.color};letter-spacing:-.01em"
				>
					{k.val}
				</div>
				<div
					style="font-family:'JetBrains Mono',monospace;font-size:.64rem;letter-spacing:.06em;text-transform:uppercase;color:#71717a;margin-top:3px"
				>
					{k.label}
				</div>
			</div>{/each}
	</div>
	<div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;margin-bottom:16px">
		<div
			style="background:rgba(255,255,255,.02);border:1px solid rgba(255,255,255,.06);border-radius:16px;padding:18px"
		>
			<div
				style="font-family:'JetBrains Mono',monospace;font-size:11px;letter-spacing:.07em;text-transform:uppercase;color:#71717a"
			>
				Matches per day
			</div>
			<canvas bind:this={matchesEl} style="width:100%;height:170px;display:block;margin-top:10px"
			></canvas>
		</div>
		<div
			style="background:rgba(255,255,255,.02);border:1px solid rgba(255,255,255,.06);border-radius:16px;padding:18px"
		>
			<div
				style="font-family:'JetBrains Mono',monospace;font-size:11px;letter-spacing:.07em;text-transform:uppercase;color:#71717a"
			>
				Generations per day
			</div>
			<canvas bind:this={gensEl} style="width:100%;height:170px;display:block;margin-top:10px"
			></canvas>
		</div>
	</div>
	<div
		style="background:rgba(255,255,255,.02);border:1px solid rgba(255,255,255,.06);border-radius:16px;padding:18px 20px;margin-bottom:16px"
	>
		<div
			style="font-family:'JetBrains Mono',monospace;font-size:11px;letter-spacing:.07em;text-transform:uppercase;color:#71717a;margin-bottom:12px"
		>
			Match status distribution
		</div>
		<div style="display:flex;height:26px;border-radius:8px;overflow:hidden;margin-bottom:12px">
			{#each STATUS_DIST as [label, count, color], i (i)}<div
					style="flex:{count};background:{color}"
					title={label}
				></div>{/each}
		</div>
		<div style="display:flex;flex-wrap:wrap;gap:14px">
			{#each STATUS_DIST as [label, count, color], i (i)}<span
					style="display:inline-flex;align-items:center;gap:6px;font-family:'JetBrains Mono',monospace;font-size:.7rem;color:#a1a1aa"
					><span style="width:8px;height:8px;border-radius:2px;background:{color}"></span>{label} · {count}</span
				>{/each}
		</div>
	</div>
	<div style="display:grid;grid-template-columns:1fr 1.3fr;gap:16px">
		<div
			style="background:rgba(255,255,255,.02);border:1px solid rgba(255,255,255,.06);border-radius:16px;padding:18px 20px"
		>
			<div
				style="font-family:'JetBrains Mono',monospace;font-size:11px;letter-spacing:.07em;text-transform:uppercase;color:#71717a;margin-bottom:14px"
			>
				Salary insights · 247 listings
			</div>
			<div style="display:grid;grid-template-columns:1fr 1fr;gap:11px">
				{#each PERCENTILES as [label, val], i (i)}<div
						style="background:#16161a;border:1px solid rgba(255,255,255,.06);border-radius:12px;padding:13px 15px"
					>
						<div
							style="font-family:'JetBrains Mono',monospace;font-size:1.25rem;font-weight:700;color:#06b6d4"
						>
							{val}
						</div>
						<div
							style="font-family:'JetBrains Mono',monospace;font-size:.62rem;letter-spacing:.05em;text-transform:uppercase;color:#71717a;margin-top:2px"
						>
							{label}
						</div>
					</div>{/each}
			</div>
		</div>
		<div
			style="background:rgba(255,255,255,.02);border:1px solid rgba(255,255,255,.06);border-radius:16px;padding:18px 20px"
		>
			<div
				style="font-family:'JetBrains Mono',monospace;font-size:11px;letter-spacing:.07em;text-transform:uppercase;color:#71717a;margin-bottom:14px"
			>
				Top titles by salary
			</div>
			<div style="display:flex;flex-direction:column;gap:11px">
				{#each SALARY_BY_TITLE as [title, , avg, barPct], i (i)}<div
						style="display:flex;align-items:center;gap:12px"
					>
						<span
							style="width:140px;flex:none;font-size:.8rem;color:#fafafa;white-space:nowrap;overflow:hidden;text-overflow:ellipsis"
							>{title}</span
						><span
							style="flex:1;height:8px;border-radius:5px;background:rgba(255,255,255,.06);overflow:hidden"
							><span
								style="display:block;height:100%;width:{barPct};border-radius:5px;background:linear-gradient(90deg,#7c3aed,#06b6d4)"
							></span></span
						><span
							style="width:46px;text-align:right;font-family:'JetBrains Mono',monospace;font-size:.77rem;color:#06b6d4"
							>{avg}</span
						>
					</div>{/each}
			</div>
		</div>
	</div>
</section>
