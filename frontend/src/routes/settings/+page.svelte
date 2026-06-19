<script lang="ts">
	import { onMount } from 'svelte'
	import { api, getAccessToken } from '$lib/api'
	import { backend, type LicenseStatus } from '$lib/api/backend'
	import type { AuditEvent } from '$lib/api/backend'
	import { goto } from '$app/navigation'

	interface Config {
		id: string
		provider: string
		model: string
		baseUrl?: string
		purpose: string
		isDefault: boolean
		hasKey: boolean
	}

	const PROVIDERS = ['OLLAMA', 'OPENAI', 'NIM', 'OPENAI_COMPATIBLE', 'ANTHROPIC', 'GOOGLE']

	// Pre-configured provider presets — one click fills the form; user just enters their key.
	interface ProviderPreset {
		label: string
		group: string
		provider: string
		baseUrl: string
		models: { chat: string; embedding?: string }[]
	}
	const PROVIDER_PRESETS: ProviderPreset[] = [
		{
			label: 'OpenAI',
			group: 'Cloud',
			provider: 'OPENAI',
			baseUrl: '',
			models: [
				{ chat: 'gpt-4o' },
				{ chat: 'gpt-4o-mini' },
				{ chat: 'o1-mini' },
				{ chat: 'text-embedding-3-small', embedding: 'text-embedding-3-small' }
			]
		},
		{
			label: 'Anthropic (Claude)',
			group: 'Cloud',
			provider: 'ANTHROPIC',
			baseUrl: '',
			models: [{ chat: 'claude-3-5-sonnet-20241022' }, { chat: 'claude-3-5-haiku-20241022' }]
		},
		{
			label: 'Google (Gemini)',
			group: 'Cloud',
			provider: 'GOOGLE',
			baseUrl: '',
			models: [
				{ chat: 'gemini-2.0-flash' },
				{ chat: 'gemini-1.5-pro' },
				{ chat: 'text-embedding-004', embedding: 'text-embedding-004' }
			]
		},
		{
			label: 'xAI (Grok)',
			group: 'Cloud',
			provider: 'OPENAI_COMPATIBLE',
			baseUrl: 'https://api.x.ai/v1',
			models: [{ chat: 'grok-2-1212' }, { chat: 'grok-beta' }]
		},
		{
			label: 'Z.AI (GLM)',
			group: 'Cloud',
			provider: 'OPENAI_COMPATIBLE',
			baseUrl: 'https://api.z.ai/api/paas/v4',
			models: [
				{ chat: 'glm-4.7' },
				{ chat: 'glm-4-plus' },
				{ chat: 'embedding-3', embedding: 'embedding-3' }
			]
		},
		{
			label: 'Moonshot (Kimi)',
			group: 'Cloud',
			provider: 'OPENAI_COMPATIBLE',
			baseUrl: 'https://api.moonshot.cn/v1',
			models: [{ chat: 'moonshot-v1-8k' }, { chat: 'moonshot-v1-32k' }]
		},
		{
			label: 'MiniMax',
			group: 'Cloud',
			provider: 'OPENAI_COMPATIBLE',
			baseUrl: 'https://api.minimaxi.com/v1',
			models: [{ chat: 'MiniMax-Text-01' }]
		},
		{
			label: 'DeepSeek',
			group: 'Cloud',
			provider: 'OPENAI_COMPATIBLE',
			baseUrl: 'https://api.deepseek.com',
			models: [{ chat: 'deepseek-chat' }, { chat: 'deepseek-reasoner' }]
		},
		{
			label: 'OpenRouter',
			group: 'Cloud',
			provider: 'OPENAI_COMPATIBLE',
			baseUrl: 'https://openrouter.ai/api/v1',
			models: [
				{ chat: 'openai/gpt-4o-mini' },
				{ chat: 'anthropic/claude-3.5-sonnet' },
				{ chat: 'google/gemini-2.0-flash-001' }
			]
		},
		{
			label: 'NVIDIA NIM',
			group: 'Cloud',
			provider: 'NIM',
			baseUrl: 'https://integrate.api.nvidia.com/v1',
			models: [{ chat: 'meta/llama-3.1-70b-instruct' }]
		},
		{
			label: 'Ollama (local)',
			group: 'Local / Self-hosted',
			provider: 'OLLAMA',
			baseUrl: 'http://localhost:11434',
			models: [
				{ chat: 'llama3.2' },
				{ chat: 'qwen2.5' },
				{ chat: 'nomic-embed-text', embedding: 'nomic-embed-text' }
			]
		},
		{
			label: 'vLLM / llama.cpp',
			group: 'Local / Self-hosted',
			provider: 'OPENAI_COMPATIBLE',
			baseUrl: 'http://localhost:8000/v1',
			models: [{ chat: 'custom-model' }]
		}
	]
	const PRESET_GROUPS = [...new Set(PROVIDER_PRESETS.map((p) => p.group))]
	let activePreset = $state<ProviderPreset | null>(null)
	let selectedPresetModel = $state('')
	const presetModels = $derived(activePreset?.models ?? [])

	function applyPreset(preset: ProviderPreset) {
		activePreset = preset
		selectedPresetModel = preset.models[0].chat
		form.provider = preset.provider
		form.baseUrl = preset.baseUrl
		form.model = preset.models[0].chat
		form.purpose = preset.models[0].embedding ? 'EMBEDDING' : 'CHAT'
	}

	function onPresetModelChange(modelId: string) {
		const m = activePreset?.models.find((mm) => mm.chat === modelId || mm.embedding === modelId)
		form.model = m?.embedding ?? modelId
		form.purpose = m?.embedding ? 'EMBEDDING' : 'CHAT'
	}

	// Quick switcher is driven by the actually-configured CHAT providers, not a
	// hardcoded list — so it always reflects reality and "Activate" promotes the
	// selected existing config to the default generation model.
	let globalConfigs = $state<Config[]>([])
	let selectedModel = $state('')
	const chatConfigs = $derived(globalConfigs.filter((c) => c.purpose === 'CHAT'))
	let switchKey = $state('')
	let switching = $state(false)
	let switchMsg = $state<string | null>(null)
	let isAdmin = $state(true)
	const activeChat = $derived(
		globalConfigs.find((c) => c.purpose === 'CHAT' && c.isDefault) || null
	)

	let configs = $state<Config[]>([])
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

	async function load() {
		configs = await api<Config[]>('/api/llm/configs')
	}

	async function loadGlobal() {
		try {
			globalConfigs = await api<Config[]>('/api/admin/llm-configs')
			isAdmin = true
			selectedModel = activeChat?.model ?? chatConfigs[0]?.model ?? ''
		} catch {
			isAdmin = false // no admin -> hide the quick switcher
		}
	}

	async function switchModel() {
		const cfg = chatConfigs.find((c) => c.model === selectedModel)
		if (!cfg) return
		switching = true
		switchMsg = null
		try {
			await api('/api/admin/llm-configs', {
				method: 'POST',
				body: JSON.stringify({
					provider: cfg.provider,
					model: cfg.model,
					purpose: 'CHAT',
					baseUrl: cfg.baseUrl ?? null,
					apiKey: switchKey || null, // empty -> reuse the existing key server-side
					isDefault: true
				})
			})
			switchKey = ''
			await loadGlobal()
			switchMsg = `✓ Generation model active: ${cfg.model}`
		} catch (e) {
			switchMsg = '✗ ' + (e instanceof Error ? e.message : 'Error')
		} finally {
			switching = false
		}
	}

	onMount(async () => {
		if (!getAccessToken()) {
			await goto('/')
			return
		}
		await load()
		await loadGlobal()
		if (isAdmin) {
			await loadLicense()
			await loadAudit()
		}
	})

	async function create(event: SubmitEvent) {
		event.preventDefault()
		message = null
		try {
			await api('/api/llm/configs', { method: 'POST', body: JSON.stringify(form) })
			form.apiKey = ''
			await load()
			message = 'Provider saved.'
		} catch (e) {
			message = e instanceof Error ? e.message : 'Error'
		}
	}

	async function test(id: string) {
		const res = await api<{ ok: boolean; message: string }>(`/api/llm/configs/${id}/test`, {
			method: 'POST'
		})
		message = (res.ok ? '✓ ' : '✗ ') + res.message
	}

	async function remove(id: string) {
		try {
			await api(`/api/llm/configs/${id}`, { method: 'DELETE' })
			await load()
			message = 'Provider removed.'
		} catch (e) {
			message = e instanceof Error ? e.message : 'Failed to remove provider'
		}
	}

	// ── License ──

	let licenseKey = $state('')
	let licenseLoading = $state(false)
	let licenseStatus = $state<LicenseStatus | null>(null)
	let licenseMsg = $state<string | null>(null)

	async function loadLicense() {
		try {
			licenseStatus = await backend.getLicenseStatus()
		} catch {
			licenseStatus = null
		}
	}

	async function uploadLicense() {
		if (!licenseKey.trim()) return
		licenseLoading = true
		licenseMsg = null
		try {
			const s = await backend.uploadLicense(licenseKey.trim())
			licenseStatus = s
			licenseKey = ''
			licenseMsg = '✓ License activated'
		} catch (e) {
			licenseMsg = '✗ ' + (e instanceof Error ? e.message : 'Error')
		} finally {
			licenseLoading = false
		}
	}

	// ── Audit-Log ──

	let auditEvents = $state<AuditEvent[]>([])
	let auditPage = $state(0)
	let auditTotal = $state(0)
	let auditTotalPages = $state(1)
	let auditLoading = $state(false)

	async function loadAudit(p = 0) {
		auditLoading = true
		try {
			const res = await backend.getAudit({ page: p, size: 50 })
			auditEvents = res.content
			auditPage = res.number
			auditTotal = res.totalElements
			auditTotalPages = res.totalPages
		} catch {
			auditEvents = []
		} finally {
			auditLoading = false
		}
	}
</script>

{#if isAdmin}
	<section class="switcher">
		<h1>AI Model (Generation)</h1>
		<p class="cur">
			Active:
			<strong>{activeChat ? activeChat.model : '— none set'}</strong>
			{#if activeChat}<span class="tag">{activeChat.provider}</span>{/if}
		</p>
		{#if chatConfigs.length > 0}
			<div class="switchrow">
				<select bind:value={selectedModel} aria-label="Select generation model">
					{#each chatConfigs as c (c.id)}
						<option value={c.model}>{c.model} ({c.provider})</option>
					{/each}
				</select>
				<input
					placeholder="API key (only when adding a new provider)"
					type="password"
					bind:value={switchKey}
				/>
				<button onclick={switchModel} disabled={switching} aria-label="Activate AI model">
					{switching ? 'Switching…' : 'Activate'}
				</button>
			</div>
		{:else}
			<p class="hint">
				No generation model configured yet. Add a CHAT provider below to enable it.
			</p>
		{/if}
		{#if switchMsg}<p class="msg">{switchMsg}</p>{/if}
		<p class="hint">
			The dropdown lists your configured CHAT providers. Add new ones in the provider table below;
			switching here promotes the selected model to the default for generation.
		</p>
	</section>
{/if}

{#if isAdmin && licenseStatus !== null}
	<section class="switcher">
		<h1>Enterprise license</h1>
		<p class="cur">
			Status:
			<strong>{licenseStatus.valid ? 'Active' : 'No license'}</strong>
			{#if licenseStatus.valid && licenseStatus.subject}
				<span class="tag">{licenseStatus.subject}</span>
			{/if}
		</p>
		{#if licenseStatus.valid && licenseStatus.expires}
			<p class="cur">
				Valid until: <strong>{new Date(licenseStatus.expires).toLocaleDateString()}</strong>
			</p>
		{/if}
		{#if licenseStatus.features.length > 0}
			<p class="cur">
				Features:
				{#each licenseStatus.features as f}
					<span class="feat-tag">{f}</span>
				{/each}
			</p>
		{/if}
		<div class="switchrow">
			<input
				placeholder="Paste license key…"
				bind:value={licenseKey}
				style="flex:1 1 20rem; font-family: monospace; font-size: 0.8rem;"
			/>
			<button onclick={uploadLicense} disabled={licenseLoading} aria-label="Activate license">
				{licenseLoading ? 'Activating…' : 'Activate'}
			</button>
		</div>
		{#if licenseMsg}<p class="msg">{licenseMsg}</p>{/if}
		<p class="hint">
			Enterprise licenses are RSA-signed and validated offline. Paste the key here to unlock
			features like SSO, multi-tenant, audit log, and HA.
		</p>
	</section>
{/if}

<section>
	<h1>LLM provider</h1>

	<div class="preset-bar">
		<span class="eyebrow">Quick setup — pick a provider, enter your key</span>
		{#each PRESET_GROUPS as g}
			<div class="preset-group">
				<span class="preset-group-label">{g}</span>
				{#each PROVIDER_PRESETS.filter((p) => p.group === g) as p}
					<button
						type="button"
						class="preset-chip"
						class:active={activePreset === p}
						onclick={() => applyPreset(p)}>{p.label}</button
					>
				{/each}
			</div>
		{/each}
	</div>

	<form onsubmit={create}>
		{#if activePreset}
			<input value={activePreset.label} disabled />
			<select
				bind:value={selectedPresetModel}
				onchange={(e) => onPresetModelChange(e.currentTarget.value)}
			>
				{#each presetModels as m}
					<option value={m.chat}>{m.chat}{m.embedding ? ' (embedding)' : ''}</option>
				{/each}
			</select>
			<input
				placeholder="Base URL"
				bind:value={form.baseUrl}
				disabled={activePreset.baseUrl === ''}
			/>
			<input placeholder="API key" type="password" bind:value={form.apiKey} required />
			<select bind:value={form.purpose} disabled>
				<option value={form.purpose}>{form.purpose}</option>
			</select>
			<label class="cb"><input type="checkbox" bind:checked={form.isDefault} /> Default</label>
			<button type="submit">Add</button>
			<button
				type="button"
				class="del"
				onclick={() => {
					activePreset = null
					selectedPresetModel = ''
				}}>Custom</button
			>
		{:else}
			<select bind:value={form.provider}>
				{#each PROVIDERS as p}<option value={p}>{p}</option>{/each}
			</select>
			<input placeholder="Model (e.g. llama3.1)" bind:value={form.model} required />
			<input placeholder="Base URL (optional)" bind:value={form.baseUrl} />
			<input placeholder="API key (optional)" type="password" bind:value={form.apiKey} />
			<input placeholder="Infisical path (optional)" bind:value={form.keyRef} />
			<select bind:value={form.purpose}>
				<option value="CHAT">Chat</option>
				<option value="EMBEDDING">Embedding</option>
			</select>
			<label class="cb"><input type="checkbox" bind:checked={form.isDefault} /> Default</label>
			<button type="submit">Add</button>
		{/if}
	</form>
	{#if message}<p class="msg">{message}</p>{/if}

	<table>
		<thead>
			<tr><th>Provider</th><th>Model</th><th>Purpose</th><th>Key</th><th></th></tr>
		</thead>
		<tbody>
			{#each configs as c}
				<tr>
					<td>{c.provider}{c.isDefault ? ' ★' : ''}</td>
					<td>{c.model}</td>
					<td>{c.purpose}</td>
					<td>{c.hasKey ? '✓' : '–'}</td>
					<td>
						<button onclick={() => test(c.id)}>Test</button>
						<button class="del" onclick={() => remove(c.id)}>Delete</button>
					</td>
				</tr>
			{/each}
		</tbody>
	</table>
</section>

{#if isAdmin}
	<section>
		<h1>Audit log</h1>
		{#if auditLoading}
			<p class="msg">Loading…</p>
		{:else if auditEvents.length === 0}
			<p class="msg">No events.</p>
		{:else}
			<div class="audit-bar">
				<span>{auditTotal} events total</span>
				<div class="audit-pager">
					<button onclick={() => loadAudit(auditPage - 1)} disabled={auditPage <= 0 || auditLoading}
						>←</button
					>
					<span>Page {auditPage + 1} / {auditTotalPages}</span>
					<button
						onclick={() => loadAudit(auditPage + 1)}
						disabled={auditPage >= auditTotalPages - 1 || auditLoading}>→</button
					>
				</div>
			</div>
			<table>
				<thead>
					<tr><th>Time</th><th>User</th><th>Action</th><th>Target</th><th>IP</th></tr>
				</thead>
				<tbody>
					{#each auditEvents as e}
						<tr>
							<td class="audit-ts">{new Date(e.ts).toLocaleString()}</td>
							<td class="audit-uid">{e.userId?.slice(0, 8)}…</td>
							<td><span class="audit-action">{e.action}</span></td>
							<td>{e.targetType ? e.targetType + ': ' + e.targetId?.slice(0, 8) + '…' : '–'}</td>
							<td class="audit-ip">{e.ip || '–'}</td>
						</tr>
					{/each}
				</tbody>
			</table>
		{/if}
	</section>
{/if}

<style>
	form {
		display: flex;
		flex-wrap: wrap;
		gap: 0.6rem;
		align-items: center;
		margin-bottom: 1rem;
	}
	input,
	select,
	button {
		padding: 0.5rem;
		border-radius: 6px;
		border: 1px solid #cbd5e1;
	}
	button {
		background: #2563eb;
		color: #fff;
		border: none;
		cursor: pointer;
	}
	.del {
		background: #b91c1c;
	}
	.cb {
		display: flex;
		gap: 0.3rem;
		align-items: center;
		font-weight: 600;
	}
	table {
		width: 100%;
		border-collapse: collapse;
	}
	th,
	td {
		text-align: left;
		padding: 0.5rem;
		border-bottom: 1px solid #e5e7eb;
	}
	.msg {
		color: #166534;
	}
	.preset-bar {
		margin-bottom: 1.2rem;
		padding: 0.8rem;
		background: var(--surface, rgba(255, 255, 255, 0.03));
		border: 1px solid var(--border, rgba(255, 255, 255, 0.08));
		border-radius: 10px;
	}
	.preset-group {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		gap: 0.4rem;
		margin-top: 0.5rem;
	}
	.preset-group-label {
		font-size: 0.75rem;
		color: var(--text-muted);
		text-transform: uppercase;
		letter-spacing: 0.05em;
		margin-right: 0.3rem;
	}
	.preset-chip {
		padding: 0.3rem 0.7rem;
		border-radius: 8px;
		border: 1px solid var(--border, rgba(255, 255, 255, 0.12));
		background: transparent;
		color: var(--text);
		cursor: pointer;
		font-size: 0.82rem;
		font-weight: 600;
		transition: all 0.15s;
	}
	.preset-chip:hover {
		border-color: var(--accent-primary, #6366f1);
		color: var(--accent-primary, #6366f1);
	}
	.preset-chip.active {
		background: var(--accent-primary, #6366f1);
		border-color: var(--accent-primary, #6366f1);
		color: #fff;
	}
	.switcher {
		border: 1px solid #c7d2fe;
		background: #f5f7ff;
		border-radius: 10px;
		padding: 1rem 1.2rem;
		margin-bottom: 1.6rem;
	}
	.switchrow {
		display: flex;
		flex-wrap: wrap;
		gap: 0.6rem;
		align-items: center;
		margin: 0.6rem 0;
	}
	.switchrow select {
		min-width: 16rem;
		flex: 1 1 16rem;
	}
	.cur {
		font-size: 0.95rem;
	}
	.cur .tag {
		display: inline-block;
		margin-left: 0.4rem;
		padding: 0.05rem 0.5rem;
		border-radius: 999px;
		background: #6366f1;
		color: #fff;
		font-size: 0.72rem;
		font-weight: 600;
	}
	.hint {
		font-size: 0.8rem;
		color: #6b7280;
	}
	.feat-tag {
		display: inline-block;
		margin: 0 0.2rem;
		padding: 0.05rem 0.5rem;
		border-radius: 999px;
		background: #dbeafe;
		color: #1e40af;
		font-size: 0.75rem;
		font-weight: 600;
	}
	.audit-bar {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-bottom: 0.5rem;
		font-size: 0.82rem;
		color: var(--text-muted);
	}
	.audit-pager {
		display: flex;
		gap: 0.5rem;
		align-items: center;
	}
	.audit-pager button:disabled {
		opacity: 0.4;
		cursor: default;
	}
	.audit-ts {
		font-size: 0.78rem;
		white-space: nowrap;
	}
	.audit-uid,
	.audit-ip {
		font-family: monospace;
		font-size: 0.72rem;
	}
	.audit-action {
		font-size: 0.75rem;
		padding: 1px 6px;
		border-radius: 3px;
		background: #f1f5f9;
	}
</style>
