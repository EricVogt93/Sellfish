<script lang="ts">
	import Icon from '$lib/atoms/Icon.svelte'
	import { rmChip, chipKeydown } from '$lib/utils/chipHelpers'
	import { backend } from '$lib/api/backend'
	import { toast } from '$lib/utils/toasts.svelte'
	import type { PreferencesResponse } from '$lib/api/backend'

	let {
		prefs,
		selectedCountries = new Set<string>(),
		onPrefsChanged
	}: {
		prefs: PreferencesResponse
		selectedCountries: Set<string>
		onPrefsChanged: (p: PreferencesResponse) => void
	} = $props()

	let titles = $state<string[]>(prefs.desiredTitles ?? [])
	let keywords = $state<string[]>(prefs.keywords ?? [])
	let excluded = $state<string[]>(prefs.excludedCompanies ?? [])
	let newTitle = $state('')
	let newKeyword = $state('')
	let newExcluded = $state('')

	async function save() {
		try {
			const updated = await backend.updatePreferences({
				desiredTitles: titles,
				keywords: keywords,
				excludedCompanies: excluded,
				preferredCountries: [...selectedCountries]
			})
			onPrefsChanged(updated)
			toast('Preferences saved — affects the next rescan', 'check')
		} catch (e) {
			toast(e instanceof Error ? e.message : 'Save failed', 'x', 'var(--accent-error)')
		}
	}
</script>

<section class="aa-card">
	<div class="aa-card-head">
		<Icon name="filter" size={15} style="color:var(--accent-secondary);" />
		<h3>Preferences</h3>
		<span class="aa-card-headnote">scored on every job</span>
	</div>
	<div class="aa-field">
		<label for="aa-titles">Desired job titles</label>
		<div class="aa-chips">
			{#each titles as t, i (t)}
				<span class="aa-chip"
					>{t}<button
						class="aa-chip-x"
						onclick={() => (titles = rmChip(titles, i))}
						aria-label="Remove">×</button
					></span
				>
			{/each}
			<input
				id="aa-titles"
				class="aa-chip-input"
				placeholder="Add title…"
				bind:value={newTitle}
				onkeydown={(e) => chipKeydown(titles, newTitle, (v) => (titles = v), e)}
			/>
		</div>
	</div>
	<div class="aa-field">
		<label for="aa-kw">Keywords</label>
		<div class="aa-chips">
			{#each keywords as k, i (k)}
				<span class="aa-chip"
					>{k}<button
						class="aa-chip-x"
						onclick={() => (keywords = rmChip(keywords, i))}
						aria-label="Remove">×</button
					></span
				>
			{/each}
			<input
				id="aa-kw"
				class="aa-chip-input"
				placeholder="Add keyword…"
				bind:value={newKeyword}
				onkeydown={(e) => chipKeydown(keywords, newKeyword, (v) => (keywords = v), e)}
			/>
		</div>
	</div>
	<div class="aa-field">
		<label for="aa-excl">Excluded companies</label>
		<div class="aa-chips">
			{#each excluded as x, i (x)}
				<span class="aa-chip aa-chip-warn"
					>{x}<button
						class="aa-chip-x"
						onclick={() => (excluded = rmChip(excluded, i))}
						aria-label="Remove">×</button
					></span
				>
			{/each}
			<input
				id="aa-excl"
				class="aa-chip-input"
				placeholder="Add company…"
				bind:value={newExcluded}
				onkeydown={(e) => chipKeydown(excluded, newExcluded, (v) => (excluded = v), e)}
			/>
		</div>
	</div>
	<button class="aa-save-btn" onclick={save}>Save preferences</button>
	<p class="aa-cardnote">
		Titles, keywords and your location drive the match score on the next rescan.
	</p>
</section>

<style>
	.aa-save-btn {
		padding: 7px 16px;
		border-radius: 8px;
		border: none;
		background: var(--accent-primary, #6366f1);
		color: #fff;
		font-weight: 600;
		cursor: pointer;
		margin-top: 4px;
	}
</style>
