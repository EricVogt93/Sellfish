<script lang="ts">
	import Icon from '$lib/atoms/Icon.svelte'
	import { backend, type ProviderConfig } from '$lib/api/backend'

	let {
		providers = []
	}: {
		providers?: ProviderConfig[]
	} = $props()

	const PROVIDERS = ['OLLAMA', 'OPENAI', 'NIM', 'OPENAI_COMPATIBLE', 'ANTHROPIC', 'GOOGLE']
	let form = $state({
		provider: 'OLLAMA',
		model: '',
		baseUrl: '',
		apiKey: '',
		keyRef: '',
		purpose: 'CHAT',
		isDefault: true
	})
	let message = $state<string | null>(null)

	async function create(event: SubmitEvent) {
		event.preventDefault()
		message = null
		try {
			await backend.createProvider({
				provider: form.provider,
				model: form.model,
				baseUrl: form.baseUrl || undefined,
				apiKey: form.apiKey || undefined,
				keyRef: form.keyRef || undefined,
				purpose: form.purpose,
				isDefault: form.isDefault
			})
			form.apiKey = ''
			providers = await backend.listProviders()
			message = 'Provider saved.'
		} catch (e) {
			message = e instanceof Error ? e.message : 'Error'
		}
	}

	async function test(id: string) {
		const res = await backend.testProvider(id)
		message = (res.ok ? '✓ ' : '✗ ') + res.message
	}

	async function remove(id: string) {
		try {
			await backend.deleteProvider(id)
			providers = await backend.listProviders()
			message = 'Provider removed.'
		} catch (e) {
			message = e instanceof Error ? e.message : 'Failed to remove provider'
		}
	}
</script>

<section class="aa-card">
	<div class="aa-card-head">
		<Icon name="sliders" size={15} style="color:var(--accent-tertiary);" />
		<h3>AI provider</h3>
		<span class="aa-card-headnote">needed for search & generation</span>
	</div>
	{#each providers as p (p.id)}
		<div class="aa-filerow">
			<Icon name="sparkles" size={15} style="color:var(--accent-primary-light);" />
			<div class="aa-filerow-text">
				<span class="aa-filerow-name">{p.model}</span>
				<span class="aa-jobmeta"
					>{p.provider}{p.isDefault ? ' · default' : ''}{p.hasKey ? ' · key set' : ''}</span
				>
			</div>
			<button class="aa-iconbtn" title="Test" onclick={() => test(p.id)}
				><Icon name="zap" size={13} /></button
			>
			<button class="aa-iconbtn" title="Delete" onclick={() => remove(p.id)}
				><Icon name="trash" size={13} /></button
			>
		</div>
	{/each}
	<form onsubmit={create}>
		<select bind:value={form.provider}>
			{#each PROVIDERS as p}<option value={p}>{p}</option>{/each}
		</select>
		<input placeholder="Model (e.g. llama3.1)" bind:value={form.model} required />
		<input placeholder="Base URL (optional)" bind:value={form.baseUrl} />
		<input placeholder="API key (optional)" type="password" bind:value={form.apiKey} />
		<select bind:value={form.purpose}>
			<option value="CHAT">Chat</option>
			<option value="EMBEDDING">Embedding</option>
		</select>
		<label class="cb"><input type="checkbox" bind:checked={form.isDefault} /> Default</label>
		<button type="submit">Add</button>
	</form>
	{#if message}<p class="msg">{message}</p>{/if}
</section>

<style>
	form {
		display: flex;
		flex-wrap: wrap;
		gap: 0.6rem;
		align-items: center;
		margin-top: 0.8rem;
	}
	input,
	select,
	button {
		padding: 0.5rem;
		border-radius: 6px;
		border: 1px solid var(--border-default);
		background: var(--bg-glass);
		color: var(--text);
	}
	button {
		background: var(--accent-primary);
		color: #fff;
		border: none;
		cursor: pointer;
		font-weight: 600;
	}
	.cb {
		display: flex;
		gap: 0.3rem;
		align-items: center;
		font-weight: 600;
		font-size: 0.82rem;
	}
	.msg {
		color: var(--accent-success, #22c55e);
		font-size: 0.82rem;
		margin-top: 0.4rem;
	}
</style>
