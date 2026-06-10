<script lang="ts">
	import Icon from './Icon.svelte';
	import Btn from './Btn.svelte';
	import Avatar from './Avatar.svelte';
	import { USERS } from './data';

	let {
		currentUserId,
		onSwitch
	}: { currentUserId: string; onSwitch: (id: string) => void } = $props();
</script>

<div class="aa-view">
	<header class="aa-pagehead">
		<div>
			<div class="eyebrow">workspace · {USERS.length} members</div>
			<h1 class="aa-h1">Users</h1>
		</div>
		<Btn variant="primary" icon="plus">Invite member</Btn>
	</header>

	<div class="aa-tablecard">
		<table class="aa-table">
			<thead>
				<tr><th>Member</th><th>Role</th><th>Status</th><th>Jobs tracked</th><th>Sent</th><th class="aa-col-actions"></th></tr>
			</thead>
			<tbody>
				{#each USERS as u (u.id)}
					<tr class="aa-row">
						<td>
							<div class="aa-jobcell">
								<Avatar initials={u.initials} hue={u.hue} size={30} />
								<div class="aa-jobcell-text">
									<span class="aa-jobtitle">{u.name} {#if u.id === currentUserId}<span class="aa-youtag">you</span>{/if}</span>
									<span class="aa-jobmeta">{u.email}</span>
								</div>
							</div>
						</td>
						<td>
							<span class={`aa-rolechip ${u.role === 'Admin' ? 'is-admin' : ''}`}>
								{#if u.role === 'Admin'}<Icon name="shield" size={11} />{/if}{u.role}
							</span>
						</td>
						<td>
							<span class="aa-stagecount">
								<span class="aa-stage-dot" style={`background:${u.active ? 'var(--accent-success)' : 'var(--text-muted)'};`}></span>
								{u.active ? 'Active' : 'Invited'}
							</span>
						</td>
						<td class="aa-jobmeta">{u.jobsTracked}</td>
						<td class="aa-jobmeta">{u.sent}</td>
						<td class="aa-col-actions">
							<div class="aa-rowactions">
								{#if u.id !== currentUserId}
									<Btn variant="ghost" size="sm" onclick={() => onSwitch(u.id)}>Switch to</Btn>
								{/if}
								<button class="aa-iconbtn" title="Member settings"><Icon name="more" size={14} /></button>
							</div>
						</td>
					</tr>
				{/each}
			</tbody>
		</table>
	</div>
	<p class="aa-cardnote" style="margin-top:12px;">Each member has their own profile, filters, documents and application history. Admins can invite, remove and reset members.</p>
</div>
