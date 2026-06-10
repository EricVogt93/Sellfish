<script lang="ts">
	import { browser } from '$app/environment';
	import Icon from '$lib/autoapply/Icon.svelte';
	import Kbd from '$lib/autoapply/Kbd.svelte';
	import Avatar from '$lib/autoapply/Avatar.svelte';
	import CursorGlow from '$lib/autoapply/CursorGlow.svelte';
	import Toasts from '$lib/autoapply/Toasts.svelte';
	import JobsView from '$lib/autoapply/JobsView.svelte';
	import JobDrawer from '$lib/autoapply/JobDrawer.svelte';
	import ApplyModal from '$lib/autoapply/ApplyModal.svelte';
	import ApplicationsView from '$lib/autoapply/ApplicationsView.svelte';
	import ProfileView from '$lib/autoapply/ProfileView.svelte';
	import UsersView from '$lib/autoapply/UsersView.svelte';
	import CommandPalette from '$lib/autoapply/CommandPalette.svelte';
	import { toast } from '$lib/autoapply/toasts.svelte';
	import { FILTERS, JOBS, PROFILE, USERS, type Prefs } from '$lib/autoapply/data';

	const NAV = [
		{ id: 'jobs', label: 'Jobs', icon: 'inbox', key: '1' },
		{ id: 'applications', label: 'Applications', icon: 'send', key: '2' },
		{ id: 'profile', label: 'Profile', icon: 'user', key: '3' },
		{ id: 'users', label: 'Users', icon: 'users', key: '4' }
	];

	function loadLS<T>(key: string, fallback: T): T {
		if (!browser) return fallback;
		try {
			const v = JSON.parse(localStorage.getItem(key) ?? 'null');
			return v == null ? fallback : v;
		} catch {
			return fallback;
		}
	}

	let view = $state('jobs');
	let ratings = $state<Record<string, number>>(loadLS('aa-ratings', {}));
	let statuses = $state<Record<string, string>>(loadLS('aa-statuses', {}));
	let selection = $state<Set<string>>(new Set());
	let focusIdx = $state(0);
	let expanded = $state<string | null>(null);
	let drawerId = $state<string | null>(null);
	let applyModal = $state<{ ids: string[]; mode: 'quick' | 'review' } | null>(null);
	let paletteOpen = $state(false);
	let currentUserId = $state('u1');
	let activeFilters = $state<Set<string>>(new Set(FILTERS.map((f) => f.id)));
	let prefs = $state<Prefs>({ ...PROFILE.prefs });
	let userMenu = $state(false);

	const profile = $derived({ ...PROFILE, prefs });
	const jobs = [...JOBS].sort((a, b) => b.score - a.score);
	const currentUser = $derived(USERS.find((u) => u.id === currentUserId)!);
	const openCount = $derived(jobs.filter((j) => statuses[j.id] !== 'applied').length);
	const drawerJob = $derived(JOBS.find((j) => j.id === drawerId) ?? null);

	$effect(() => {
		if (browser) localStorage.setItem('aa-ratings', JSON.stringify(ratings));
	});
	$effect(() => {
		if (browser) localStorage.setItem('aa-statuses', JSON.stringify(statuses));
	});

	function rate(jobId: string, v: number) {
		ratings = { ...ratings, [jobId]: v };
		if (v > 0) toast('Rating saved — auto score adapts over time', 'star', 'var(--accent-warning)');
	}

	function quickApply(jobId: string) {
		const job = JOBS.find((j) => j.id === jobId)!;
		if (job.score < prefs.threshold) {
			toast(`Score ${job.score} is below your ${prefs.threshold} threshold — review first`, 'eye', 'var(--accent-warning)');
			applyModal = { ids: [jobId], mode: 'review' };
		} else {
			applyModal = { ids: [jobId], mode: 'quick' };
		}
	}

	function onSent(ids: string[]) {
		const n = { ...statuses };
		ids.forEach((id) => {
			n[id] = 'applied';
		});
		statuses = n;
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

	function switchUser(id: string) {
		currentUserId = id;
		userMenu = false;
		const u = USERS.find((x) => x.id === id)!;
		toast(`Switched to ${u.name}`, 'user', 'var(--accent-secondary)');
	}

	// ── Keyboard-Shortcuts (Windows/Linux: Ctrl) ──────────────────────
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
		if (typing || paletteOpen || applyModal) return;
		if (['1', '2', '3', '4'].includes(e.key)) {
			view = NAV[+e.key - 1].id;
			return;
		}
		if (view !== 'jobs') return;
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
			if (statuses[jobs[focusIdx].id] !== 'applied') applyModal = { ids: [jobs[focusIdx].id], mode: 'review' };
		} else if (e.key === 'e') {
			expanded = expanded === jobs[focusIdx].id ? null : jobs[focusIdx].id;
		}
	}

	// Fokuszeile im Sichtbereich halten
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
			<button class="aa-iconbtn" title="Notifications"><Icon name="bell" size={16} /><span class="aa-belldot"></span></button>
			<div class="aa-usermenu-wrap">
				<button class="aa-userbtn" onclick={() => (userMenu = !userMenu)}>
					<Avatar initials={currentUser.initials} hue={currentUser.hue} size={28} />
					<Icon name="chevronDown" size={12} style="color:var(--text-muted);" />
				</button>
				{#if userMenu}
					<div class="aa-usermenu">
						<div class="eyebrow" style="padding:6px 10px 4px;">switch profile</div>
						{#each USERS as u (u.id)}
							<button class={`aa-usermenu-item ${u.id === currentUserId ? 'is-active' : ''}`} onclick={() => switchUser(u.id)}>
								<Avatar initials={u.initials} hue={u.hue} size={24} />
								<span style="flex:1;text-align:left;">{u.name}</span>
								{#if u.id === currentUserId}<Icon name="check" size={13} style="color:var(--accent-primary-light);" />{/if}
							</button>
						{/each}
						<div class="aa-usermenu-divider"></div>
						<button
							class="aa-usermenu-item"
							onclick={() => {
								view = 'users';
								userMenu = false;
							}}
						>
							<Icon name="users" size={14} style="margin:0 5px;" /><span>Manage users</span>
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
			onToggleSelect={toggleSelect}
			onToggleSelectAll={() => (selection = selection.size === jobs.length ? new Set() : new Set(jobs.map((j) => j.id)))}
			onRate={rate}
			onExpand={(id) => (expanded = expanded === id ? null : id)}
			onOpen={(id) => (drawerId = id)}
			onQuickApply={quickApply}
			onApply={(ids) => (applyModal = { ids, mode: 'review' })}
			onBulkApply={() => (applyModal = { ids: [...selection].filter((id) => statuses[id] !== 'applied'), mode: 'review' })}
			onClearSelection={() => (selection = new Set())}
		/>
	{:else if view === 'applications'}
		<ApplicationsView {statuses} />
	{:else if view === 'profile'}
		<ProfileView
			{profile}
			{activeFilters}
			onToggleFilter={(id) => {
				const n = new Set(activeFilters);
				if (n.has(id)) n.delete(id);
				else n.add(id);
				activeFilters = n;
			}}
			onPref={(k, v) => (prefs = { ...prefs, [k]: v })}
		/>
	{:else if view === 'users'}
		<UsersView {currentUserId} onSwitch={switchUser} />
	{/if}
</main>

{#if drawerJob}
	<JobDrawer
		job={drawerJob}
		rating={ratings[drawerJob.id] || 0}
		applied={statuses[drawerJob.id] === 'applied'}
		onClose={() => (drawerId = null)}
		onRate={rate}
		onQuickApply={(id) => {
			drawerId = null;
			quickApply(id);
		}}
		onApply={(ids) => {
			drawerId = null;
			applyModal = { ids, mode: 'review' };
		}}
	/>
{/if}

{#if applyModal && applyModal.ids.length > 0}
	<ApplyModal
		jobIds={applyModal.ids}
		mode={applyModal.mode}
		{profile}
		onClose={() => (applyModal = null)}
		onSent={onSent}
	/>
{/if}

{#if paletteOpen}
	<CommandPalette
		{jobs}
		nav={NAV}
		onClose={() => (paletteOpen = false)}
		onGo={(v) => {
			view = v;
			paletteOpen = false;
		}}
		onJob={(id) => {
			view = 'jobs';
			drawerId = id;
			paletteOpen = false;
		}}
	/>
{/if}

<Toasts />
