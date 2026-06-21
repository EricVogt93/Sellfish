<script lang="ts">
	// ── Sellfish · App-Shell (1:1 aus Sellfish.dc.html) ──
	import { app } from '$lib/state/sellfish.svelte'
	import CursorGlow from '$lib/atoms/CursorGlow.svelte'
	import Header from '$lib/organisms/Header.svelte'
	import Toasts from '$lib/molecules/Toasts.svelte'
	import JobsView from '$lib/organisms/JobsView.svelte'
	import ApplicationsView from '$lib/organisms/ApplicationsView.svelte'
	import ProfileView from '$lib/organisms/ProfileView.svelte'
	import ReportsView from '$lib/organisms/ReportsView.svelte'
	import JobDrawer from '$lib/organisms/JobDrawer.svelte'
	import GenerateModal from '$lib/organisms/GenerateModal.svelte'
	import CommandPalette from '$lib/organisms/CommandPalette.svelte'
</script>

<svelte:head>
	<title>Sellfish</title>
</svelte:head>

<svelte:window onkeydown={app.onKey} />

<div style="position:relative;min-height:100vh;overflow-x:hidden">
	<CursorGlow />
	<Header />

	<main style="max-width:1340px;margin:0 auto;padding:30px 22px 130px;position:relative;z-index:1">
		{#if app.isJobs}
			<JobsView />
		{:else if app.isApps}
			<ApplicationsView />
		{:else if app.isProfile}
			<ProfileView />
		{:else if app.isReports}
			<ReportsView />
		{/if}
	</main>

	{#if app.drawerJob}
		<JobDrawer />
	{/if}
	{#if app.generateJob}
		<GenerateModal />
	{/if}
	{#if app.paletteOpen}
		<CommandPalette />
	{/if}

	<Toasts />
</div>
