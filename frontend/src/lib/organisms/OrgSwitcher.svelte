<script lang="ts">
	import Icon from '$lib/atoms/Icon.svelte'
	import { getSession, switchOrg, refreshOrgs } from '$lib/api/session.svelte'
	import { backend } from '$lib/api/backend'
	import { toast } from '$lib/utils/toasts.svelte'

	const session = getSession()
	let open = $state(false)
	let creating = $state(false)
	let createName = $state('')
	let createSlug = $state('')

	const activeOrg = $derived(session.orgs.find((o) => o.id === session.activeOrgId) ?? null)
	const label = $derived(activeOrg ? activeOrg.name : 'Personal')

	function toggle() {
		open = !open
		if (open) {
			creating = false
			createName = ''
			createSlug = ''
		}
	}

	async function doSwitch(id: string | null) {
		open = false
		try {
			await switchOrg(id)
			toast(
				id
					? `Switched to ${session.orgs.find((o) => o.id === id)?.name ?? 'org'}`
					: 'Personal workspace'
			)
		} catch (e) {
			toast(e instanceof Error ? e.message : 'Failed to switch', 'x', 'var(--accent-error)')
		}
	}

	async function doCreate() {
		if (!createName.trim() || !createSlug.trim()) return
		try {
			await backend.createOrg(createName.trim(), createSlug.trim().toLowerCase())
			await refreshOrgs()
			creating = false
			createName = ''
			createSlug = ''
			toast('Organization created')
		} catch (e) {
			toast(e instanceof Error ? e.message : 'Failed to create', 'x', 'var(--accent-error)')
		}
	}

	function closeOnClickOutside(e: MouseEvent) {
		const el = e.target as HTMLElement
		if (!el.closest('.org-switcher')) open = false
	}
</script>

<svelte:window onclick={closeOnClickOutside} />

<div class="org-switcher">
	<button class="org-btn" onclick={toggle}>
		<Icon name="building" size={13} />
		<span>{label}</span>
		<Icon name="chevronDown" size={10} style="color:var(--text-muted);" />
	</button>

	{#if open}
		<div class="org-drop">
			<div class="org-drop-header">Workspaces</div>
			<button
				class="org-item {!session.activeOrgId ? 'active' : ''}"
				onclick={() => doSwitch(null)}
			>
				<Icon name="user" size={13} />
				<span>Personal</span>
			</button>
			{#each session.orgs as org (org.id)}
				<button
					class="org-item {session.activeOrgId === org.id ? 'active' : ''}"
					onclick={() => doSwitch(org.id)}
				>
					<Icon name="users" size={13} />
					<span>{org.name}</span>
					<span class="org-plan">{org.plan}</span>
				</button>
			{/each}

			<div class="org-drop-divider"></div>

			{#if creating}
				<div class="org-create-form">
					<input placeholder="Name" bind:value={createName} />
					<input placeholder="Slug" bind:value={createSlug} />
					<button
						class="org-create-btn"
						onclick={doCreate}
						disabled={!createName.trim() || !createSlug.trim()}>Create</button
					>
					<button class="org-create-cancel" onclick={() => (creating = false)}>Cancel</button>
				</div>
			{:else}
				<button class="org-item mute" onclick={() => (creating = true)}>
					<Icon name="plus" size={13} />
					<span>Create organization…</span>
				</button>
			{/if}
		</div>
	{/if}
</div>

<style>
	.org-switcher {
		position: relative;
	}
	.org-btn {
		display: flex;
		align-items: center;
		gap: 6px;
		padding: 5px 10px;
		border: 1px solid var(--border-color, #e2e8f0);
		border-radius: 6px;
		background: var(--surface-elevated, #f8fafc);
		font-size: 0.82rem;
		font-weight: 500;
		color: var(--text-primary, #1e293b);
		cursor: pointer;
		max-width: 160px;
		overflow: hidden;
		white-space: nowrap;
	}
	.org-btn:hover {
		border-color: var(--accent-primary, #6366f1);
	}
	.org-btn span {
		overflow: hidden;
		text-overflow: ellipsis;
	}
	.org-drop {
		position: absolute;
		top: calc(100% + 6px);
		right: 0;
		min-width: 220px;
		background: var(--surface, #fff);
		border: 1px solid var(--border-color, #e2e8f0);
		border-radius: 8px;
		box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
		z-index: 200;
		padding: 4px;
	}
	.org-drop-header {
		font-size: 0.68rem;
		text-transform: uppercase;
		letter-spacing: 0.06em;
		color: var(--text-muted, #94a3b8);
		padding: 8px 10px 4px;
		font-weight: 600;
	}
	.org-item {
		display: flex;
		align-items: center;
		gap: 8px;
		width: 100%;
		padding: 7px 10px;
		border: none;
		border-radius: 5px;
		background: transparent;
		font-size: 0.82rem;
		color: var(--text-primary, #1e293b);
		cursor: pointer;
		text-align: left;
	}
	.org-item:hover {
		background: var(--surface-hover, #f1f5f9);
	}
	.org-item.active {
		background: var(--accent-soft, #eef2ff);
	}
	.org-item.mute {
		color: var(--text-muted, #94a3b8);
	}
	.org-plan {
		margin-left: auto;
		font-size: 0.68rem;
		color: var(--text-muted, #94a3b8);
		border: 1px solid var(--border-color, #e2e8f0);
		border-radius: 3px;
		padding: 1px 6px;
	}
	.org-drop-divider {
		height: 1px;
		background: var(--border-color, #e2e8f0);
		margin: 4px 0;
	}
	.org-create-form {
		display: flex;
		flex-direction: column;
		gap: 4px;
		padding: 6px 10px;
	}
	.org-create-form input {
		padding: 5px 8px;
		border: 1px solid var(--border-color, #e2e8f0);
		border-radius: 4px;
		font-size: 0.82rem;
	}
	.org-create-btn {
		padding: 5px 12px;
		border: none;
		border-radius: 4px;
		background: var(--accent-primary, #6366f1);
		color: #fff;
		font-size: 0.78rem;
		cursor: pointer;
		margin-top: 2px;
	}
	.org-create-btn:disabled {
		opacity: 0.5;
		cursor: default;
	}
	.org-create-cancel {
		padding: 3px 0;
		border: none;
		background: transparent;
		color: var(--text-muted, #94a3b8);
		font-size: 0.75rem;
		cursor: pointer;
	}
</style>
