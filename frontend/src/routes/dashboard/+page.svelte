<script lang="ts">
  import { onMount } from 'svelte';
  import { api, getAccessToken } from '$lib/api';
  import { goto } from '$app/navigation';

  interface Me {
    id: string;
    email: string;
    role: string;
    locale: string;
  }

  let me = $state<Me | null>(null);
  let error = $state<string | null>(null);

  onMount(async () => {
    if (!getAccessToken()) {
      await goto('/login');
      return;
    }
    try {
      me = await api<Me>('/api/me');
    } catch (e) {
      error = e instanceof Error ? e.message : 'Fehler';
    }
  });
</script>

<section>
  <h1>Dashboard</h1>
  {#if error}
    <p class="error">{error}</p>
  {:else if me}
    <p>Angemeldet als <strong>{me.email}</strong> ({me.role}).</p>
    <p class="hint">
      Job-Matches erscheinen hier, sobald die Such- und Matching-Module (M3/M4) aktiv sind.
    </p>
  {:else}
    <p>Lade…</p>
  {/if}
</section>

<style>
  .error {
    color: #b91c1c;
  }
  .hint {
    color: #6b7280;
  }
</style>
