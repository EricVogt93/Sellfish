<script lang="ts">
	import { onMount } from 'svelte';
	import Icon from './Icon.svelte';
	import Avatar from './Avatar.svelte';
	import { backend, type AdminUser, type Me } from '$lib/api/backend';
	import { initialsOf, hueOf } from './map';

	let { me }: { me: Me | null } = $props();

	let members = $state<AdminUser[]>([]);
	let isAdmin = $state(false);

	onMount(async () => {
		try {
			members = await backend.listUsers();
			isAdmin = true;
		} catch {
			// Kein Admin: nur der eigene Account wird angezeigt.
			isAdmin = false;
			if (me) members = [{ id: me.id, email: me.email, role: me.role, status: 'ACTIVE', createdAt: '' }];
		}
	});
</script>

<div class="aa-view">
	<header class="aa-pagehead">
		<div>
			<div class="eyebrow">workspace · {members.length} member{members.length === 1 ? '' : 's'}</div>
			<h1 class="aa-h1">Users</h1>
		</div>
	</header>

	<div class="aa-tablecard">
		<table class="aa-table">
			<thead>
				<tr><th>Member</th><th>Role</th><th>Status</th></tr>
			</thead>
			<tbody>
				{#each members as u (u.id)}
					<tr class="aa-row">
						<td>
							<div class="aa-jobcell">
								<Avatar initials={initialsOf(u.email)} hue={hueOf(u.email)} size={30} />
								<div class="aa-jobcell-text">
									<span class="aa-jobtitle">{u.email} {#if u.id === me?.id}<span class="aa-youtag">you</span>{/if}</span>
									<span class="aa-jobmeta">{u.id}</span>
								</div>
							</div>
						</td>
						<td>
							<span class={`aa-rolechip ${u.role === 'ADMIN' ? 'is-admin' : ''}`}>
								{#if u.role === 'ADMIN'}<Icon name="shield" size={11} />{/if}{u.role}
							</span>
						</td>
						<td>
							<span class="aa-stagecount">
								<span class="aa-stage-dot" style={`background:${u.status === 'ACTIVE' ? 'var(--accent-success)' : 'var(--text-muted)'};`}></span>
								{u.status === 'ACTIVE' ? 'Active' : u.status}
							</span>
						</td>
					</tr>
				{/each}
			</tbody>
		</table>
	</div>
	{#if !isAdmin}
		<p class="aa-cardnote" style="margin-top:12px;">You see only your own account. Workspace member management requires an admin role.</p>
	{:else}
		<p class="aa-cardnote" style="margin-top:12px;">Each member has their own profile, filters, documents and application history.</p>
	{/if}
</div>
