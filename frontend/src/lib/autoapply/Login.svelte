<script lang="ts">
	import Icon from './Icon.svelte';
	import Btn from './Btn.svelte';
	import { login, register } from '$lib/api/session.svelte';

	let mode = $state<'login' | 'register'>('login');
	let email = $state('');
	let password = $state('');
	let error = $state<string | null>(null);
	let busy = $state(false);

	async function submit(e: SubmitEvent) {
		e.preventDefault();
		error = null;
		busy = true;
		try {
			if (mode === 'login') await login(email, password);
			else await register(email, password);
		} catch (err) {
			error = err instanceof Error ? err.message : 'Unknown error';
		} finally {
			busy = false;
		}
	}
</script>

<div class="aa-login">
	<div class="aa-login-card">
		<div class="aa-brand" style="justify-content:center;margin-bottom:6px;">
			<span class="aa-brandmark"><Icon name="zap" size={14} strokeWidth={2.2} /></span>
			<span class="aa-brandname" style="font-size:1.1rem;">auto<span class="gradient-text">apply</span></span>
		</div>
		<div class="eyebrow" style="text-align:center;">application autopilot</div>
		<h1 class="aa-login-title">{mode === 'login' ? 'Sign in' : 'Create account'}</h1>

		<form onsubmit={submit}>
			<div class="aa-field">
				<label for="aa-login-email">Email</label>
				<input id="aa-login-email" class="aa-input" type="email" bind:value={email} required autocomplete="email" />
			</div>
			<div class="aa-field">
				<label for="aa-login-pw">Password</label>
				<input id="aa-login-pw" class="aa-input" type="password" bind:value={password} required minlength={8} autocomplete="current-password" />
			</div>
			{#if error}<p class="aa-login-error"><Icon name="x" size={13} /> {error}</p>{/if}
			<Btn variant="primary" icon={mode === 'login' ? 'enter' : 'plus'} style="width:100%;justify-content:center;margin-top:4px;">
				{busy ? '…' : mode === 'login' ? 'Sign in' : 'Create account'}
			</Btn>
		</form>

		<p class="aa-login-switch">
			{#if mode === 'login'}
				No account?
				<button onclick={() => (mode = 'register')}>Register</button>
			{:else}
				Already registered?
				<button onclick={() => (mode = 'login')}>Sign in</button>
			{/if}
		</p>
	</div>
</div>

<style>
	.aa-login {
		min-height: 100vh;
		display: flex;
		align-items: center;
		justify-content: center;
		padding: var(--space-lg);
	}
	.aa-login-card {
		width: min(400px, 100%);
		background: var(--bg-glass);
		backdrop-filter: blur(12px);
		border: 1px solid var(--border-subtle);
		border-radius: var(--radius-2xl);
		padding: var(--space-2xl) var(--space-xl);
		box-shadow: 0 0 0 1px rgba(124, 58, 237, 0.08), 0 30px 60px -25px rgba(0, 0, 0, 0.7);
	}
	.aa-login-title {
		font-size: 1.5rem;
		text-align: center;
		margin: 6px 0 var(--space-lg);
	}
	form {
		display: flex;
		flex-direction: column;
		gap: var(--space-md);
	}
	.aa-login-error {
		color: var(--accent-error);
		font-size: 0.8rem;
		display: flex;
		align-items: center;
		gap: 5px;
		margin: 0;
	}
	.aa-login-switch {
		text-align: center;
		margin-top: var(--space-lg);
		font-size: 0.84rem;
		color: var(--text-muted);
	}
	.aa-login-switch button {
		background: none;
		border: none;
		color: var(--accent-primary-light);
		cursor: pointer;
		font: inherit;
	}
</style>
