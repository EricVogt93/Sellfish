<script lang="ts">
  import { onMount } from 'svelte';
  import { api, getAccessToken } from '$lib/api';
  import { goto } from '$app/navigation';

  interface Profile {
    headline?: string;
    summary?: string;
    location?: string;
    willingnessToRelocate?: boolean;
    salaryMin?: number | null;
    remotePref?: string;
    availability?: string;
    meta?: string;
  }
  interface Preferences {
    desiredTitles?: string[];
    industries?: string[];
    contractTypes?: string[];
    excludedCompanies?: string[];
    keywords?: string[];
  }

  let profile = $state<Profile>({ remotePref: 'ANY' });
  let prefs = $state<Preferences>({});
  let titlesText = $state('');
  let keywordsText = $state('');
  let excludedText = $state('');
  let message = $state<string | null>(null);

  function toList(s: string): string[] {
    return s.split(',').map((x) => x.trim()).filter(Boolean);
  }

  onMount(async () => {
    if (!getAccessToken()) {
      await goto('/login');
      return;
    }
    profile = await api<Profile>('/api/profile');
    prefs = await api<Preferences>('/api/profile/preferences');
    titlesText = (prefs.desiredTitles ?? []).join(', ');
    keywordsText = (prefs.keywords ?? []).join(', ');
    excludedText = (prefs.excludedCompanies ?? []).join(', ');
  });

  async function save() {
    message = null;
    try {
      profile = await api<Profile>('/api/profile', {
        method: 'PUT',
        body: JSON.stringify(profile)
      });
      prefs = await api<Preferences>('/api/profile/preferences', {
        method: 'PUT',
        body: JSON.stringify({
          desiredTitles: toList(titlesText),
          keywords: toList(keywordsText),
          excludedCompanies: toList(excludedText)
        })
      });
      message = 'Gespeichert.';
    } catch (e) {
      message = e instanceof Error ? e.message : 'Fehler';
    }
  }
</script>

<section>
  <h1>Profil &amp; Wünsche</h1>
  <div class="grid">
    <label>Kurzprofil<input bind:value={profile.headline} /></label>
    <label>Standort<input bind:value={profile.location} /></label>
    <label class="full">Zusammenfassung<textarea rows="3" bind:value={profile.summary}></textarea></label>
    <label>Remote-Präferenz
      <select bind:value={profile.remotePref}>
        <option value="ANY">Egal</option>
        <option value="REMOTE">Remote</option>
        <option value="HYBRID">Hybrid</option>
        <option value="ONSITE">Vor Ort</option>
      </select>
    </label>
    <label>Mindestgehalt (€)<input type="number" bind:value={profile.salaryMin} /></label>
    <label class="full">Wunsch-Jobtitel (Komma-getrennt)<input bind:value={titlesText} /></label>
    <label class="full">Keywords (Komma-getrennt)<input bind:value={keywordsText} /></label>
    <label class="full">Ausgeschlossene Firmen (Komma-getrennt)<input bind:value={excludedText} /></label>
  </div>
  <button onclick={save}>Speichern</button>
  {#if message}<p class="msg">{message}</p>{/if}
</section>

<style>
  .grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 1rem;
    margin-bottom: 1rem;
  }
  label {
    display: flex;
    flex-direction: column;
    gap: 0.3rem;
    font-weight: 600;
  }
  .full {
    grid-column: 1 / -1;
  }
  input,
  select,
  textarea {
    padding: 0.5rem;
    border: 1px solid #cbd5e1;
    border-radius: 6px;
    font: inherit;
  }
  button {
    padding: 0.6rem 1.2rem;
    background: #2563eb;
    color: #fff;
    border: none;
    border-radius: 6px;
    cursor: pointer;
  }
  .msg {
    color: #166534;
  }
</style>
