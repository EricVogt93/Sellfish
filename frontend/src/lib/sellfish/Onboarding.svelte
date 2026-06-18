<script lang="ts">
	import { onMount } from 'svelte';
	import Icon from './Icon.svelte';
	import { backend, type ProfileResponse, type PreferencesResponse, type ProviderConfig } from '$lib/api/backend';

	let {
		hasMatches,
		hasDocuments,
		onNavigate,
		onRescan,
		searching = false
	}: {
		hasMatches: boolean;
		hasDocuments: boolean;
		onNavigate: (view: string) => void;
		onRescan: () => void;
		searching?: boolean;
	} = $props();

	let profile = $state<ProfileResponse | null>(null);
	let prefs = $state<PreferencesResponse | null>(null);
	let providers = $state<ProviderConfig[]>([]);
	let loaded = $state(false);

	onMount(async () => {
		try {
			const [p, pr, pv] = await Promise.all([
				backend.getProfile(),
				backend.getPreferences(),
				backend.listProviders()
			]);
			profile = p;
			prefs = pr;
			providers = pv;
		} catch {
			/* ignore — steps stay incomplete */
		} finally {
			loaded = true;
		}
	});

	const hasProfile = $derived(!!profile && !!(profile.headline || profile.summary));
	const hasPrefs = $derived(!!prefs && prefs.desiredTitles.length > 0);
	const hasCV = $derived(hasDocuments);
	const hasProvider = $derived(providers.some((p) => p.purpose === 'CHAT' && p.hasKey));

	const steps = $derived([
		{ id: 'profile', done: hasProfile, label: 'Complete your profile', cta: 'profile', icon: 'user' },
		{ id: 'prefs', done: hasPrefs, label: 'Set job preferences & filters', cta: 'profile', icon: 'filter' },
		{ id: 'cv', done: hasCV, label: 'Upload your CV (parsed by AI)', cta: 'profile', icon: 'file' },
		{ id: 'provider', done: hasProvider, label: 'Connect an AI provider', cta: 'profile', icon: 'cpu' },
		{ id: 'search', done: hasMatches, label: 'Run your first job search', cta: 'search', icon: 'search' }
	]);

	const doneCount = $derived(steps.filter((s) => s.done).length);
	const allDone = $derived(doneCount === steps.length);
	let dismissed = $state(false);

	const show = $derived(loaded && !allDone && !dismissed);
</script>

{#if show}
	<div class="aa-onboard">
		<div class="aa-onboard-head">
			<div>
				<div class="eyebrow">getting started · {doneCount}/{steps.length}</div>
				<h2>Set up Sellfish in {steps.length - doneCount} quick step{steps.length - doneCount === 1 ? '' : 's'}</h2>
			</div>
			<button class="aa-onboard-x" onclick={() => (dismissed = true)} aria-label="Dismiss">
				<Icon name="x" size={14} />
			</button>
		</div>

		<div class="aa-onboard-bar">
			<div class="aa-onboard-fill" style="width:{(doneCount / steps.length) * 100}%"></div>
		</div>

		<ol class="aa-onboard-list">
			{#each steps as s (s.id)}
				<li class:done={s.done}>
					<span class="aa-onboard-check">
						{#if s.done}<Icon name="check" size={13} />{:else}<span class="aa-onboard-dot"></span>{/if}
					</span>
					<span class="aa-onboard-label">{s.label}</span>
					{#if !s.done}
						<button
							class="aa-onboard-cta"
							onclick={() => (s.id === 'search' ? onRescan() : onNavigate(s.cta))}
							disabled={s.id === 'search' && searching}
						>
							{s.id === 'search' ? (searching ? 'Scanning…' : 'Run') : 'Open'}
						</button>
					{/if}
				</li>
			{/each}
		</ol>
	</div>
{/if}

<style>
	.aa-onboard {
		border: 1px solid var(--border, rgba(255, 255, 255, 0.08));
		border-radius: 14px;
		padding: 1.1rem 1.3rem;
		margin-bottom: 1.2rem;
		background: var(--surface-raised, rgba(255, 255, 255, 0.03));
	}
	.aa-onboard-head {
		display: flex;
		justify-content: space-between;
		align-items: flex-start;
		gap: 1rem;
	}
	.aa-onboard-head h2 {
		font-size: 1.05rem;
		margin: 2px 0 0;
	}
	.aa-onboard-x {
		background: transparent;
		border: none;
		color: var(--text-muted);
		cursor: pointer;
		padding: 4px;
		border-radius: 6px;
		display: flex;
	}
	.aa-onboard-x:hover {
		background: var(--surface, rgba(255, 255, 255, 0.05));
	}
	.aa-onboard-bar {
		height: 5px;
		border-radius: 999px;
		background: var(--surface, rgba(255, 255, 255, 0.08));
		margin: 0.9rem 0 0.6rem;
		overflow: hidden;
	}
	.aa-onboard-fill {
		height: 100%;
		border-radius: 999px;
		background: linear-gradient(90deg, var(--accent-primary, #6366f1), var(--accent-secondary, #06b6d4));
		transition: width 0.4s ease;
	}
	.aa-onboard-list {
		list-style: none;
		padding: 0;
		margin: 0.4rem 0 0;
		display: grid;
		gap: 0.15rem;
	}
	.aa-onboard-list li {
		display: flex;
		align-items: center;
		gap: 0.7rem;
		padding: 0.4rem 0.2rem;
		font-size: 0.92rem;
	}
	.aa-onboard-list li.done .aa-onboard-label {
		color: var(--text-muted);
		text-decoration: line-through;
		text-decoration-color: var(--text-muted);
	}
	.aa-onboard-check {
		width: 20px;
		height: 20px;
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
	}
	.aa-onboard-list li.done .aa-onboard-check {
		background: var(--accent-success, #22c55e);
		color: #04130a;
	}
	.aa-onboard-dot {
		width: 8px;
		height: 8px;
		border-radius: 50%;
		border: 2px solid var(--text-muted);
	}
	.aa-onboard-label {
		flex: 1;
	}
	.aa-onboard-cta {
		font-size: 0.8rem;
		padding: 0.28rem 0.7rem;
		border-radius: 7px;
		border: 1px solid var(--accent-primary, #6366f1);
		background: transparent;
		color: var(--accent-primary, #6366f1);
		cursor: pointer;
		font-weight: 600;
	}
	.aa-onboard-cta:hover:not(:disabled) {
		background: var(--accent-primary, #6366f1);
		color: #fff;
	}
	.aa-onboard-cta:disabled {
		opacity: 0.5;
		cursor: default;
	}
</style>
