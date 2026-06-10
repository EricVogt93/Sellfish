<script lang="ts">
	import { onMount } from 'svelte';
	import Icon from './Icon.svelte';
	import Btn from './Btn.svelte';
	import { toast } from './toasts.svelte';
	import { apiDownload } from '$lib/api';
	import {
		backend,
		type ProfileResponse,
		type PreferencesResponse,
		type DocumentResponse,
		type ProviderConfig
	} from '$lib/api/backend';

	let profile = $state<ProfileResponse | null>(null);
	let prefs = $state<PreferencesResponse | null>(null);
	let documents = $state<DocumentResponse[]>([]);
	let providers = $state<ProviderConfig[]>([]);

	// Komma-Listen-Felder
	let titlesText = $state('');
	let keywordsText = $state('');
	let excludedText = $state('');

	const PROVIDERS = ['OLLAMA', 'OPENAI', 'NIM', 'OPENAI_COMPATIBLE', 'ANTHROPIC', 'GOOGLE'];
	const DOC_TYPES = ['CV', 'PROJECT_LIST', 'CERTIFICATE', 'REFERENCE', 'COVER_LETTER', 'OTHER'];

	let docType = $state('CV');
	let form = $state({ provider: 'OLLAMA', model: '', baseUrl: '', apiKey: '', keyRef: '', purpose: 'CHAT', isDefault: true });

	const toList = (s: string) => s.split(',').map((x) => x.trim()).filter(Boolean);

	async function loadAll() {
		profile = await backend.getProfile();
		prefs = await backend.getPreferences();
		titlesText = (prefs.desiredTitles ?? []).join(', ');
		keywordsText = (prefs.keywords ?? []).join(', ');
		excludedText = (prefs.excludedCompanies ?? []).join(', ');
		documents = await backend.listDocuments();
		providers = await backend.listProviders();
	}

	onMount(loadAll);

	async function saveIdentity() {
		if (!profile) return;
		try {
			profile = await backend.updateProfile({
				headline: profile.headline,
				summary: profile.summary,
				location: profile.location,
				remotePref: profile.remotePref,
				salaryMin: profile.salaryMin
			});
			toast('Profile saved', 'check');
		} catch (e) {
			toast(e instanceof Error ? e.message : 'Save failed', 'x', 'var(--accent-error)');
		}
	}

	async function savePrefs() {
		try {
			prefs = await backend.updatePreferences({
				desiredTitles: toList(titlesText),
				keywords: toList(keywordsText),
				excludedCompanies: toList(excludedText)
			});
			toast('Preferences saved — affects the next rescan', 'check');
		} catch (e) {
			toast(e instanceof Error ? e.message : 'Save failed', 'x', 'var(--accent-error)');
		}
	}

	async function upload(e: Event) {
		const input = e.target as HTMLInputElement;
		const file = input.files?.[0];
		if (!file) return;
		try {
			await backend.uploadDocument(docType, file);
			documents = await backend.listDocuments();
			toast(`${file.name} uploaded`, 'check');
		} catch (err) {
			toast(err instanceof Error ? err.message : 'Upload failed', 'x', 'var(--accent-error)');
		}
		input.value = '';
	}

	async function removeDoc(id: string) {
		await backend.deleteDocument(id);
		documents = documents.filter((d) => d.id !== id);
	}

	async function addProvider(e: SubmitEvent) {
		e.preventDefault();
		try {
			await backend.createProvider({
				provider: form.provider,
				model: form.model,
				baseUrl: form.baseUrl || undefined,
				apiKey: form.apiKey || undefined,
				keyRef: form.keyRef || undefined,
				purpose: form.purpose,
				isDefault: form.isDefault
			});
			form.apiKey = '';
			providers = await backend.listProviders();
			toast('Provider saved', 'check');
		} catch (err) {
			toast(err instanceof Error ? err.message : 'Failed', 'x', 'var(--accent-error)');
		}
	}

	async function testProvider(id: string) {
		const r = await backend.testProvider(id);
		toast((r.ok ? '✓ ' : '✗ ') + r.message, r.ok ? 'check' : 'x', r.ok ? 'var(--accent-success)' : 'var(--accent-error)');
	}

	async function removeProvider(id: string) {
		await backend.deleteProvider(id);
		providers = providers.filter((p) => p.id !== id);
	}
</script>

<div class="aa-view">
	<header class="aa-pagehead">
		<div>
			<div class="eyebrow">profile · drives matching & generation</div>
			<h1 class="aa-h1">Your profile</h1>
		</div>
	</header>

	{#if profile && prefs}
		<div class="aa-profilegrid">
			<section class="aa-card">
				<div class="aa-card-head">
					<Icon name="user" size={15} style="color:var(--accent-primary-light);" />
					<h3>Identity</h3>
				</div>
				<div class="aa-field"><label for="aa-headline">Headline</label><input id="aa-headline" class="aa-input" bind:value={profile.headline} /></div>
				<div class="aa-field"><label for="aa-summary">Summary</label><input id="aa-summary" class="aa-input" bind:value={profile.summary} /></div>
				<div class="aa-field-row">
					<div class="aa-field"><label for="aa-loc">Location</label><input id="aa-loc" class="aa-input" bind:value={profile.location} /></div>
					<div class="aa-field"><label for="aa-sal">Min. salary (€)</label><input id="aa-sal" class="aa-input" type="number" bind:value={profile.salaryMin} /></div>
				</div>
				<div class="aa-field">
					<label for="aa-remote">Remote preference</label>
					<select id="aa-remote" class="aa-input" bind:value={profile.remotePref}>
						<option value="ANY">Any</option><option value="REMOTE">Remote</option>
						<option value="HYBRID">Hybrid</option><option value="ONSITE">On-site</option>
					</select>
				</div>
				<Btn variant="primary" icon="check" onclick={saveIdentity} style="margin-top:4px;">Save identity</Btn>
			</section>

			<section class="aa-card">
				<div class="aa-card-head">
					<Icon name="filter" size={15} style="color:var(--accent-secondary);" />
					<h3>Preferences</h3>
					<span class="aa-card-headnote">scored on every job</span>
				</div>
				<div class="aa-field"><label for="aa-titles">Desired job titles (comma-separated)</label><input id="aa-titles" class="aa-input" bind:value={titlesText} /></div>
				<div class="aa-field"><label for="aa-kw">Keywords (comma-separated)</label><input id="aa-kw" class="aa-input" bind:value={keywordsText} /></div>
				<div class="aa-field"><label for="aa-excl">Excluded companies (comma-separated)</label><input id="aa-excl" class="aa-input" bind:value={excludedText} /></div>
				<Btn variant="primary" icon="check" onclick={savePrefs} style="margin-top:4px;">Save preferences</Btn>
				<p class="aa-cardnote">Titles, keywords and your location drive the match score on the next rescan.</p>
			</section>

			<section class="aa-card">
				<div class="aa-card-head">
					<Icon name="file" size={15} style="color:var(--accent-warning);" />
					<h3>Documents</h3>
				</div>
				{#each documents as f (f.id)}
					<div class="aa-filerow">
						<Icon name="file" size={15} style="color:var(--text-muted);" />
						<div class="aa-filerow-text">
							<span class="aa-filerow-name">{f.filename}</span>
							<span class="aa-jobmeta">{f.type}{f.primary ? ' · primary' : ''}{f.hasStruct ? ' · parsed' : ''}</span>
						</div>
						<button class="aa-iconbtn" title="Download" onclick={() => apiDownload(`/api/documents/${f.id}/download`, f.filename)}><Icon name="download" size={13} /></button>
						<button class="aa-iconbtn" title="Delete" onclick={() => removeDoc(f.id)}><Icon name="trash" size={13} /></button>
					</div>
				{/each}
				<div class="aa-field-row" style="margin-top:12px;align-items:end;">
					<div class="aa-field" style="margin:0;">
						<label for="aa-doctype">Type</label>
						<select id="aa-doctype" class="aa-input" bind:value={docType}>
							{#each DOC_TYPES as t (t)}<option value={t}>{t}</option>{/each}
						</select>
					</div>
					<label class="aa-dropzone" style="margin:0;">
						<Icon name="upload" size={16} /><span>Upload</span>
						<input type="file" style="display:none;" onchange={upload} />
					</label>
				</div>
			</section>

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
							<span class="aa-filerow-name">{p.provider} · {p.model}</span>
							<span class="aa-jobmeta">{p.purpose}{p.isDefault ? ' · default' : ''}{p.hasKey ? ' · key set' : ''}</span>
						</div>
						<button class="aa-iconbtn" title="Test" onclick={() => testProvider(p.id)}><Icon name="refresh" size={13} /></button>
						<button class="aa-iconbtn" title="Delete" onclick={() => removeProvider(p.id)}><Icon name="trash" size={13} /></button>
					</div>
				{/each}
				<form onsubmit={addProvider} style="margin-top:12px;">
					<div class="aa-field-row">
						<div class="aa-field" style="margin-bottom:10px;">
							<label for="aa-prov">Provider</label>
							<select id="aa-prov" class="aa-input" bind:value={form.provider}>
								{#each PROVIDERS as p (p)}<option value={p}>{p}</option>{/each}
							</select>
						</div>
						<div class="aa-field" style="margin-bottom:10px;">
							<label for="aa-purpose">Purpose</label>
							<select id="aa-purpose" class="aa-input" bind:value={form.purpose}>
								<option value="CHAT">Chat</option><option value="EMBEDDING">Embedding</option>
							</select>
						</div>
					</div>
					<div class="aa-field" style="margin-bottom:10px;"><label for="aa-model">Model</label><input id="aa-model" class="aa-input" bind:value={form.model} placeholder="e.g. llama3.1 / gpt-4o" required /></div>
					<div class="aa-field" style="margin-bottom:10px;"><label for="aa-base">Base URL (optional)</label><input id="aa-base" class="aa-input" bind:value={form.baseUrl} placeholder="http://ollama:11434" /></div>
					<div class="aa-field" style="margin-bottom:10px;"><label for="aa-key">API key (optional)</label><input id="aa-key" class="aa-input" type="password" bind:value={form.apiKey} /></div>
					<Btn variant="primary" icon="plus">Add provider</Btn>
				</form>
				<p class="aa-cardnote">Add a <strong>CHAT</strong> provider for generation and an <strong>EMBEDDING</strong> provider for semantic matching.</p>
			</section>
		</div>
	{:else}
		<div class="aa-empty"><p>Loading…</p></div>
	{/if}
</div>
