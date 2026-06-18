<script lang="ts">
	import { onMount } from 'svelte';
	import { page } from '$app/stores';
	import { goto } from '$app/navigation';
	import { api, setTokens } from '$lib/api';
	import { initSession } from '$lib/api/session.svelte';
	import type { TokenResponse } from '$lib/api';

	let error = $state<string | null>(null);

	onMount(async () => {
		const params = new URLSearchParams($page.url.search);
		const code = params.get('code');
		const provider = params.get('provider');

		if (!code || !provider) {
			error = 'Missing code or provider parameter';
			return;
		}

		try {
			const tokens = await api<TokenResponse>('/api/auth/sso/callback', {
				method: 'POST',
				body: JSON.stringify({ code, provider })
			});
			setTokens(tokens);
			await initSession();
			await goto('/');
		} catch (e) {
			error = e instanceof Error ? e.message : 'SSO authentication failed';
		}
	});
</script>

<svelte:head>
	<title>SSO Callback — autoapply</title>
</svelte:head>

<div class="aa-callback">
	<div class="aa-callback-card">
		{#if error}
			<h1>Authentication failed</h1>
			<p class="aa-callback-error">{error}</p>
			<a href="/" class="aa-callback-link">Back to login</a>
		{:else}
			<h1>Signing in…</h1>
			<p class="aa-callback-loading">Completing authentication</p>
		{/if}
	</div>
</div>

<style>
	.aa-callback {
		min-height: 100vh;
		display: flex;
		align-items: center;
		justify-content: center;
	}
	.aa-callback-card {
		text-align: center;
	}
	.aa-callback-error {
		color: var(--accent-error);
	}
	.aa-callback-link {
		color: var(--accent-primary-light);
	}
	.aa-callback-loading {
		color: var(--text-muted);
	}
</style>
