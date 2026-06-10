<script lang="ts">
	import Icon from './Icon.svelte';
	import { FILTERS, type Profile } from './data';

	let {
		profile,
		activeFilters,
		onToggleFilter,
		onPref
	}: {
		profile: Profile;
		activeFilters: Set<string>;
		onToggleFilter: (id: string) => void;
		onPref: (key: string, value: string | number) => void;
	} = $props();
</script>

<div class="aa-view">
	<header class="aa-pagehead">
		<div>
			<div class="eyebrow">profile · drives matching & generation</div>
			<h1 class="aa-h1">Your profile</h1>
		</div>
	</header>

	<div class="aa-profilegrid">
		<section class="aa-card">
			<div class="aa-card-head">
				<Icon name="user" size={15} style="color:var(--accent-primary-light);" />
				<h3>Identity</h3>
			</div>
			<div class="aa-field"><label for="aa-pf-name">Name</label><input id="aa-pf-name" class="aa-input" value={profile.name} /></div>
			<div class="aa-field"><label for="aa-pf-headline">Headline</label><input id="aa-pf-headline" class="aa-input" value={profile.headline} /></div>
			<div class="aa-field-row">
				<div class="aa-field"><label for="aa-pf-email">Email</label><input id="aa-pf-email" class="aa-input" value={profile.email} /></div>
				<div class="aa-field"><label for="aa-pf-city">City</label><input id="aa-pf-city" class="aa-input" value={profile.city} /></div>
			</div>
			<div class="aa-field">
				<label for="aa-pf-skills">Skills (used in every cover letter)</label>
				<div class="aa-skillchips" id="aa-pf-skills">
					{#each profile.skills as s (s)}
						<span class="aa-minichip aa-minichip-met">{s}</span>
					{/each}
					<button class="aa-minichip aa-minichip-add"><Icon name="plus" size={10} /> add</button>
				</div>
			</div>
		</section>

		<section class="aa-card">
			<div class="aa-card-head">
				<Icon name="filter" size={15} style="color:var(--accent-secondary);" />
				<h3>Filters</h3>
				<span class="aa-card-headnote">scored on every job</span>
			</div>
			{#each FILTERS as f (f.id)}
				<div class="aa-filterrow">
					<span class="aa-filterlabel">{f.label}</span>
					<button
						role="switch"
						aria-checked={activeFilters.has(f.id)}
						aria-label={f.label}
						class={`aa-switch ${activeFilters.has(f.id) ? 'is-on' : ''}`}
						onclick={() => onToggleFilter(f.id)}
					>
						<span class="aa-switch-knob"></span>
					</button>
				</div>
			{/each}
			<p class="aa-cardnote">Disabled filters stop counting toward the match score on the next rescan.</p>
		</section>

		<section class="aa-card">
			<div class="aa-card-head">
				<Icon name="sliders" size={15} style="color:var(--accent-tertiary);" />
				<h3>Auto-apply preferences</h3>
			</div>
			<div class="aa-field">
				<label for="aa-pf-threshold">Quick-apply threshold · score ≥ <strong style="color:var(--accent-primary-light);">{profile.prefs.threshold}</strong></label>
				<input
					id="aa-pf-threshold"
					type="range"
					class="aa-range"
					min="50"
					max="100"
					value={profile.prefs.threshold}
					oninput={(e) => onPref('threshold', +e.currentTarget.value)}
				/>
				<p class="aa-cardnote">Jobs at or above this score show the one-click <Icon name="zap" size={11} /> quick-apply without review.</p>
			</div>
			<div class="aa-field-row">
				<div class="aa-field">
					<label for="aa-pf-tone">Letter tone</label>
					<select id="aa-pf-tone" class="aa-input" value={profile.prefs.tone} onchange={(e) => onPref('tone', e.currentTarget.value)}>
						<option>Professional</option><option>Direct</option>
					</select>
				</div>
				<div class="aa-field">
					<label for="aa-pf-lang">Language</label>
					<select id="aa-pf-lang" class="aa-input" value={profile.prefs.language} onchange={(e) => onPref('language', e.currentTarget.value)}>
						<option>English</option><option>German</option>
					</select>
				</div>
			</div>
			<div class="aa-field"><label for="aa-pf-sig">Signature</label><input id="aa-pf-sig" class="aa-input" value={profile.prefs.signature} /></div>
		</section>

		<section class="aa-card">
			<div class="aa-card-head">
				<Icon name="file" size={15} style="color:var(--accent-warning);" />
				<h3>Documents</h3>
			</div>
			{#each profile.files as f (f.id)}
				<div class="aa-filerow">
					<Icon name="file" size={15} style="color:var(--text-muted);" />
					<div class="aa-filerow-text">
						<span class="aa-filerow-name">{f.name}</span>
						<span class="aa-jobmeta">{f.kind} · {f.size} · updated {f.updated}</span>
					</div>
					<button class="aa-iconbtn" title="Download"><Icon name="download" size={13} /></button>
					<button class="aa-iconbtn" title="Delete"><Icon name="trash" size={13} /></button>
				</div>
			{/each}
			<div class="aa-dropzone">
				<Icon name="upload" size={16} />
				<span>Drop files or <a href="#0">browse</a> — PDF, max 10 MB</span>
			</div>
		</section>
	</div>
</div>
