<script lang="ts">
	import { browser } from '$app/environment';
	import Icon from '$lib/autoapply/Icon.svelte';
	import Kbd from '$lib/autoapply/Kbd.svelte';
	import Avatar from '$lib/autoapply/Avatar.svelte';
	import CursorGlow from '$lib/autoapply/CursorGlow.svelte';
	import Toasts from '$lib/autoapply/Toasts.svelte';
	import Login from '$lib/autoapply/Login.svelte';
	import JobsView from '$lib/autoapply/JobsView.svelte';
	import JobDrawer from '$lib/autoapply/JobDrawer.svelte';
	import ApplyModal from '$lib/autoapply/ApplyModal.svelte';
	import ApplicationsView from '$lib/autoapply/ApplicationsView.svelte';
	import ProfileView from '$lib/autoapply/ProfileView.svelte';
	import UsersView from '$lib/autoapply/UsersView.svelte';
	import CommandPalette from '$lib/autoapply/CommandPalette.svelte';
	import { toast } from '$lib/autoapply/toasts.svelte';
	import { mapMatch, initialsOf, hueOf } from '$lib/autoapply/map';
	import type { Job } from '$lib/autoapply/data';
	import { getSession, initSession, logout } from '$lib/api/session.svelte';
	import { backend, type MatchResponse, type DocumentResponse, type MatchStatus } from '$lib/api/backend';

	const NAV = [
		{ id: 'jobs', label: 'Jobs', icon: 'inbox', key: '1' },
		{ id: 'applications', label: 'Applications', icon: 'send', key: '2' },
		{ id: 'profile', label: 'Profile', icon: 'user', key: '3' },
		{ id: 'users', label: 'Users', icon: 'users', key: '4' }
	];

	const APPLIED = new Set<MatchStatus>(['APPLIED', 'INTERVIEW', 'OFFER', 'REJECTED']);

	const session = getSession();

	let view = $state('jobs');
	let matches = $state<MatchResponse[]>([]);
	let documents = $state<DocumentResponse[]>([]);
	let ratings = $state<Record<string, number>>(loadLS('aa-ratings', {}));
	let selection = $state<Set<string>>(new Set());
	let focusIdx = $state(0);
	let expanded = $state<string | null>(null);
	let drawerId = $state<string | null>(null);
	let applyModal = $state<{ jobs: Job[]; mode: 'quick' | 'review' } | null>(null);
	let paletteOpen = $state(false);
	let userMenu = $state(false);
	let searching = $state(false);

	const jobs = $derived([...matches].map(mapMatch).sort((a, b) => b.score - a.score));
	const statuses = $derived(
		Object.fromEntries(jobs.map((j) => [j.id, j.status && APPLIED.has(j.status as MatchStatus) ? 'applied' : '']))
	);
	const applications = $derived(jobs.filter((j) => j.status && APPLIED.has(j.status as MatchStatus)));
	const openCount = $derived(jobs.filter((j) => !(j.status && APPLIED.has(j.status as MatchStatus))).length);
	const drawerJob = $derived(jobs.find((j) => j.id === drawerId) ?? null);
	const me = $derived(session.me);

	function loadLS<T>(key: string, fallback: T): T {
		if (!browser) return fallback;
		try {
			const v = JSON.parse(localStorage.getItem(key) ?? 'null');
			return v == null ? fallback : v;
		} catch {
			return fallback;
		}
	}

	$effect(() => {
		if (browser) localStorage.setItem('aa-ratings', JSON.stringify(ratings));
	});

	$effect(() => {
		void initSession();
	});

	// Nach Login: Matches + Dokumente laden
	$effect(() => {
		if (session.authed) void reload();
	});

	async function reload() {
		try {
			const [page, docs] = await Promise.all([backend.listMatches(100), backend.listDocuments()]);
			matches = page.content;
			documents = docs;
		} catch (e) {
			toast(e instanceof Error ? e.message : 'Failed to load', 'x', 'var(--accent-error)');
		}
	}

	function patchMatch(matchId: string, status: MatchStatus) {
		matches = matches.map((m) => (m.matchId === matchId ? { ...m, status } : m));
	}

	async function rescan() {
		searching = true;
		toast('Scanning sources…', 'refresh', 'var(--accent-secondary)');
		try {
			await backend.search();
			await reload();
			toast(`Done — ${jobs.length} matches`, 'check');
		} catch (e) {
			toast(e instanceof Error ? e.message : 'Search failed', 'x', 'var(--accent-error)');
		} finally {
			searching = false;
		}
	}

	// Sterne-Rating ⇒ echtes Feedback fürs Self-Learning
	async function rate(jobId: string, v: number) {
		ratings = { ...ratings, [jobId]: v };
		const job = jobs.find((j) => j.id === jobId);
		if (!job?.matchId || v === 0) return;
		const status: MatchStatus = v >= 4 ? 'SAVED' : v <= 2 ? 'DISMISSED' : 'SEEN';
		try {
			await backend.setStatus(job.matchId, status);
			patchMatch(job.matchId, status);
			toast('Rating saved — trains your match score', 'star', 'var(--accent-warning)');
		} catch {
			/* still keep local rating */
		}
	}

	function quickApply(jobId: string) {
		const job = jobs.find((j) => j.id === jobId);
		if (job) applyModal = { jobs: [job], mode: 'quick' };
	}

	function review(ids: string[]) {
		const sel = ids.map((id) => jobs.find((j) => j.id === id)).filter(Boolean) as Job[];
		if (sel.length) applyModal = { jobs: sel, mode: 'review' };
	}

	function onSent(ids: string[]) {
		ids.forEach((id) => {
			const job = jobs.find((j) => j.id === id);
			if (job?.matchId) patchMatch(job.matchId, 'APPLIED');
		});
		const sel = new Set(selection);
		ids.forEach((id) => sel.delete(id));
		selection = sel;
		toast(`${ids.length > 1 ? ids.length + ' applications' : 'Application'} sent`, 'send', 'var(--accent-primary-light)');
	}

	function toggleSelect(id: string) {
		const n = new Set(selection);
		if (n.has(id)) n.delete(id);
		else n.add(id);
		selection = n;
	}

	function onKey(e: KeyboardEvent) {
		const target = e.target as HTMLElement;
		const tag = (target.tagName || '').toLowerCase();
		const typing = tag === 'input' || tag === 'textarea' || tag === 'select' || target.isContentEditable;
		if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
			e.preventDefault();
			paletteOpen = !paletteOpen;
			return;
		}
		if (e.key === 'Escape') {
			paletteOpen = false;
			drawerId = null;
			userMenu = false;
			return;
		}
		if (typing || paletteOpen || applyModal || !session.authed) return;
		if (['1', '2', '3', '4'].includes(e.key)) {
			view = NAV[+e.key - 1].id;
			return;
		}
		if (view !== 'jobs' || jobs.length === 0) return;
		const max = jobs.length - 1;
		if (e.key === 'j' || e.key === 'ArrowDown') {
			e.preventDefault();
			focusIdx = Math.min(focusIdx + 1, max);
		} else if (e.key === 'k' || e.key === 'ArrowUp') {
			e.preventDefault();
			focusIdx = Math.max(focusIdx - 1, 0);
		} else if (e.key === 'x') {
			toggleSelect(jobs[focusIdx].id);
		} else if (e.key === 'Enter') {
			drawerId = jobs[focusIdx].id;
		} else if (e.key === 'a') {
			const job = jobs[focusIdx];
			if (!(job.status && APPLIED.has(job.status as MatchStatus))) review([job.id]);
		} else if (e.key === 'e') {
			expanded = expanded === jobs[focusIdx].id ? null : jobs[focusIdx].id;
		}
	}

	$effect(() => {
		const el = document.getElementById(`aa-row-${focusIdx}`);
		if (el) {
			const r = el.getBoundingClientRect();
			if (r.top < 70) window.scrollBy({ top: r.top - 120 });
			else if (r.bottom > innerHeight - 80) window.scrollBy({ top: r.bottom - innerHeight + 120 });
		}
	});
</script>

<svelte:head>
	<title>autoapply — application autopilot</title>
</svelte:head>

<svelte:window onkeydown={onKey} />

{#if !session.ready}
	<div class="aa-bootsplash"><span class="aa-brandmark"><Icon name="zap" size={16} strokeWidth={2.2} /></span></div>
{:else if !session.authed}
	<Login />
{:else}
	<CursorGlow />

	<header class="aa-topbar">
		<div class="aa-topbar-inner">
			<div class="aa-brand">
				<span class="aa-brandmark"><Icon name="zap" size={14} strokeWidth={2.2} /></span>
				<span class="aa-brandname">auto<span class="gradient-text">apply</span></span>
			</div>
			<nav class="aa-nav">
				{#each NAV as n (n.id)}
					<button class={`aa-navlink ${view === n.id ? 'is-active' : ''}`} onclick={() => (view = n.id)}>
						<Icon name={n.icon} size={14} />{n.label}
						{#if n.id === 'jobs'}<span class="aa-navcount">{openCount}</span>{/if}
					</button>
				{/each}
			</nav>
			<div class="aa-topbar-right">
				<button class="aa-searchbtn" onclick={() => (paletteOpen = true)}>
					<Icon name="search" size={13} /><span>Search</span><Kbd>Ctrl K</Kbd>
				</button>
				<div class="aa-usermenu-wrap">
					<button class="aa-userbtn" onclick={() => (userMenu = !userMenu)}>
						<Avatar initials={initialsOf(me?.email)} hue={hueOf(me?.email)} size={28} />
						<Icon name="chevronDown" size={12} style="color:var(--text-muted);" />
					</button>
					{#if userMenu}
						<div class="aa-usermenu">
							<div class="eyebrow" style="padding:6px 10px 4px;">{me?.email}</div>
							<button class="aa-usermenu-item" onclick={() => { view = 'profile'; userMenu = false; }}>
								<Icon name="user" size={14} style="margin:0 5px;" /><span>Profile & providers</span>
							</button>
							<button class="aa-usermenu-item" onclick={() => { view = 'users'; userMenu = false; }}>
								<Icon name="users" size={14} style="margin:0 5px;" /><span>Workspace</span>
							</button>
							<div class="aa-usermenu-divider"></div>
							<button class="aa-usermenu-item" onclick={() => { logout(); userMenu = false; }}>
								<Icon name="logout" size={14} style="margin:0 5px;" /><span>Sign out</span>
							</button>
						</div>
					{/if}
				</div>
			</div>
		</div>
	</header>

	<main class="aa-main">
		{#if view === 'jobs'}
			<JobsView
				{jobs}
				{ratings}
				{statuses}
				{selection}
				{focusIdx}
				{expanded}
				{searching}
				onRescan={rescan}
				onToggleSelect={toggleSelect}
				onToggleSelectAll={() => (selection = selection.size === jobs.length ? new Set() : new Set(jobs.map((j) => j.id)))}
				onRate={rate}
				onExpand={(id) => (expanded = expanded === id ? null : id)}
				onOpen={(id) => (drawerId = id)}
				onQuickApply={quickApply}
				onApply={review}
				onBulkApply={() => review([...selection].filter((id) => !(statuses[id] === 'applied')))}
				onClearSelection={() => (selection = new Set())}
			/>
		{:else if view === 'applications'}
			<ApplicationsView {applications} />
		{:else if view === 'profile'}
			<ProfileView />
		{:else if view === 'users'}
			<UsersView {me} />
		{/if}
	</main>

	{#if drawerJob}
		<JobDrawer
			job={drawerJob}
			rating={ratings[drawerJob.id] || 0}
			applied={statuses[drawerJob.id] === 'applied'}
			onClose={() => (drawerId = null)}
			onRate={rate}
			onQuickApply={(id) => { drawerId = null; quickApply(id); }}
			onApply={(ids) => { drawerId = null; review(ids); }}
		/>
	{/if}

	{#if applyModal && applyModal.jobs.length > 0}
		<ApplyModal
			jobs={applyModal.jobs}
			mode={applyModal.mode}
			{documents}
			onClose={() => (applyModal = null)}
			onSent={onSent}
		/>
	{/if}

	{#if paletteOpen}
		<CommandPalette
			{jobs}
			nav={NAV}
			onClose={() => (paletteOpen = false)}
			onGo={(v) => { view = v; paletteOpen = false; }}
			onJob={(id) => { view = 'jobs'; drawerId = id; paletteOpen = false; }}
		/>
	{/if}

	<Toasts />
{/if}

<style>
	.aa-bootsplash {
		min-height: 100vh;
		display: flex;
		align-items: center;
		justify-content: center;
	}
	.aa-bootsplash .aa-brandmark {
		width: 44px;
		height: 44px;
		animation: aa-glowpulse 1.4s ease-in-out infinite;
	}
</style>
