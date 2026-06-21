<script lang="ts">
	// ── Sellfish · Header / Nav (1:1 aus Sellfish.dc.html) ──
	import { app } from '$lib/state/sellfish.svelte'
	import Icon from '$lib/atoms/Icon.svelte'

	const navBtn =
		'display:inline-flex;align-items:center;gap:8px;padding:8px 14px;border-radius:11px;border:none;cursor:pointer;font-size:.86rem;font-weight:500;transition:all .15s'
	const pill = (color: string, bg: string) =>
		`font-family:'JetBrains Mono',monospace;font-size:.62rem;color:${color};background:${bg};border-radius:999px;padding:1px 7px`
</script>

<header
	style="position:sticky;top:0;z-index:40;background:rgba(10,10,11,.82);backdrop-filter:blur(16px);-webkit-backdrop-filter:blur(16px);border-bottom:1px solid rgba(255,255,255,.06)"
>
	<div
		style="max-width:1340px;margin:0 auto;padding:0 22px;height:60px;display:flex;align-items:center;gap:22px"
	>
		<div style="display:flex;align-items:center;gap:10px;flex:none">
			<span
				style="width:34px;height:34px;border-radius:9px;display:inline-flex;align-items:center;justify-content:center;background:radial-gradient(circle at 30% 25%,rgba(43,108,219,.25),rgba(6,182,212,.12));box-shadow:0 0 18px rgba(43,108,219,.35),inset 0 0 0 1px rgba(255,255,255,.08);animation:sfFloat 5s ease-in-out infinite"
			>
				<img
					src="/assets/sellfish-mark.png"
					alt="Sellfish"
					style="width:26px;height:26px;object-fit:contain;display:block"
				/>
			</span>
			<span
				style="font-family:'JetBrains Mono',monospace;font-weight:600;font-size:1.02rem;letter-spacing:-.01em"
				>sellfish</span
			>
		</div>

		<nav style="display:flex;gap:3px;flex:1">
			<button
				class="sf-nav"
				onclick={app.goJobs}
				style="{navBtn};background:{app.ns.jobs.bg};color:{app.ns.jobs.color};box-shadow:{app.ns
					.jobs.sh}"
			>
				<Icon name="inbox" size={15} strokeWidth={1.7} />Jobs<span
					style={pill('#8b5cf6', 'rgba(124,58,237,.16)')}>{app.jobCount}</span
				>
			</button>
			<button
				class="sf-nav"
				onclick={app.goApps}
				style="{navBtn};background:{app.ns.apps.bg};color:{app.ns.apps.color};box-shadow:{app.ns
					.apps.sh}"
			>
				<Icon name="send" size={15} strokeWidth={1.7} />Applications<span
					style={pill('#06b6d4', 'rgba(6,182,212,.16)')}>{app.appCount}</span
				>
			</button>
			<button
				class="sf-nav"
				onclick={app.goProfile}
				style="{navBtn};background:{app.ns.profile.bg};color:{app.ns.profile.color};box-shadow:{app
					.ns.profile.sh}"
			>
				<Icon name="user" size={15} strokeWidth={1.7} />Profile
			</button>
			<button
				class="sf-nav"
				onclick={app.goReports}
				style="{navBtn};background:{app.ns.reports.bg};color:{app.ns.reports.color};box-shadow:{app
					.ns.reports.sh}"
			>
				<Icon name="barChart" size={15} strokeWidth={1.7} />Reports
			</button>
		</nav>

		<div style="display:flex;align-items:center;gap:10px;flex:none">
			<button
				class="sf-search"
				onclick={app.openPalette}
				style="display:inline-flex;align-items:center;gap:9px;background:rgba(255,255,255,.02);border:1px solid rgba(255,255,255,.06);border-radius:9px;color:#71717a;font-size:.8rem;padding:7px 11px;cursor:pointer;transition:all .15s"
			>
				<Icon name="search" size={14} strokeWidth={1.8} /><span>Search…</span><span
					style="font-family:'JetBrains Mono',monospace;font-size:.62rem;color:#71717a;background:#18181b;border:1px solid rgba(255,255,255,.06);border-radius:5px;padding:2px 6px"
					>⌘K</span
				>
			</button>
			<span
				style="width:32px;height:32px;border-radius:50%;display:inline-flex;align-items:center;justify-content:center;font-family:'JetBrains Mono',monospace;font-weight:600;font-size:.78rem;color:#fff;background:linear-gradient(135deg,#7c3aed,#06b6d4);border:1px solid rgba(255,255,255,.1)"
				>EV</span
			>
		</div>
	</div>
</header>

<style>
	.sf-nav:hover {
		color: #fafafa !important;
		background: rgba(255, 255, 255, 0.05) !important;
	}
	.sf-search:hover {
		border-color: rgba(124, 58, 237, 0.3) !important;
		color: #a1a1aa !important;
	}
</style>
