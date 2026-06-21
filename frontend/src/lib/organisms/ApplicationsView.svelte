<script>
	import { app } from '$lib/state/sellfish.svelte'
</script>

<section data-screen-label="Applications" style="animation:sfUp .3s ease both">
	<div style="margin-bottom:18px">
		<div
			style="font-family:'JetBrains Mono',monospace;font-size:11px;letter-spacing:.08em;text-transform:uppercase;color:#71717a"
		>
			pipeline · {app.appCount} active applications
		</div>
		<h1 style="font-size:1.85rem;font-weight:600;letter-spacing:-.02em;margin-top:4px">
			Applications
		</h1>
	</div>
	<div
		style="display:grid;grid-template-columns:repeat(5,minmax(180px,1fr));gap:14px;align-items:start;overflow-x:auto"
	>
		{#each app.appColumns as col (col.key)}
			<div
				style="background:rgba(255,255,255,.02);border:1px solid rgba(255,255,255,.06);border-radius:14px;padding:12px"
			>
				<div style="display:flex;align-items:center;gap:8px;margin-bottom:12px">
					<span
						style="width:8px;height:8px;border-radius:50%;background:{col.color};box-shadow:0 0 8px {col.color}"
					></span><span
						style="font-family:'JetBrains Mono',monospace;font-size:.66rem;letter-spacing:.06em;text-transform:uppercase;color:#a1a1aa"
						>{col.label}</span
					><span
						style="margin-left:auto;font-family:'JetBrains Mono',monospace;font-size:.66rem;color:#71717a"
						>{col.count}</span
					>
				</div>
				<div style="display:flex;flex-direction:column;gap:10px">
					{#each col.items as ap (ap.id)}
						<div
							onclick={ap.onOpen}
							class="sf-appcard"
							style="background:#16161a;border:1px solid rgba(255,255,255,.06);border-radius:11px;padding:12px;cursor:pointer;transition:all .15s"
						>
							<div style="display:flex;align-items:center;gap:9px;margin-bottom:10px">
								<span
									style="flex:none;width:30px;height:30px;border-radius:8px;display:inline-flex;align-items:center;justify-content:center;font-family:'JetBrains Mono',monospace;font-weight:600;font-size:.66rem;background:{ap.markBg};border:1px solid {ap.markBorder};color:{ap.markColor}"
									>{ap.initials}</span
								>
								<div style="min-width:0">
									<div
										style="font-size:.81rem;font-weight:600;white-space:nowrap;overflow:hidden;text-overflow:ellipsis"
									>
										{ap.title}
									</div>
									<div
										style="font-size:.69rem;color:#71717a;white-space:nowrap;overflow:hidden;text-overflow:ellipsis"
									>
										{ap.company}
									</div>
								</div>
							</div>
							<div style="display:flex;align-items:center;justify-content:space-between">
								<span style="font-family:'JetBrains Mono',monospace;font-size:.7rem;color:#06b6d4"
									>{ap.salary}</span
								><span style="font-family:'JetBrains Mono',monospace;font-size:.64rem;color:#71717a"
									>{ap.ago} ago</span
								>
							</div>
						</div>
					{/each}
					{#if col.empty}<div
							style="font-family:'JetBrains Mono',monospace;font-size:.66rem;color:#52525b;text-align:center;padding:10px 0"
						>
							—
						</div>{/if}
				</div>
			</div>
		{/each}
	</div>
</section>

<style>
	.sf-appcard:hover {
		border-color: rgba(124, 58, 237, 0.3);
		transform: translateY(-2px);
	}
</style>
