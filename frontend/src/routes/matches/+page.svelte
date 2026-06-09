<script lang="ts">
  import { onMount } from 'svelte';
  import { api, getAccessToken } from '$lib/api';
  import { goto } from '$app/navigation';

  interface Match {
    matchId: string;
    jobId: string;
    title: string;
    company?: string;
    location?: string;
    url?: string;
    score: number;
    status: string;
    scoreBreakdown: string;
  }
  interface Page<T> {
    content: T[];
  }

  const STATUSES = ['SAVED', 'DISMISSED', 'APPLIED', 'INTERVIEW', 'OFFER', 'REJECTED'];

  let matches = $state<Match[]>([]);
  let busy = $state(false);
  let message = $state<string | null>(null);
  let generated = $state<Record<string, string>>({});

  async function load() {
    const page = await api<Page<Match>>('/api/matches?size=50');
    matches = page.content;
  }

  onMount(async () => {
    if (!getAccessToken()) {
      await goto('/login');
      return;
    }
    await load();
  });

  async function runSearch() {
    busy = true;
    message = 'Suche läuft…';
    try {
      await api('/api/jobs/search', { method: 'POST' });
      await load();
      message = `Fertig – ${matches.length} Matches.`;
    } catch (e) {
      message = e instanceof Error ? e.message : 'Fehler';
    } finally {
      busy = false;
    }
  }

  async function setStatus(m: Match, status: string) {
    await api(`/api/matches/${m.matchId}/status`, {
      method: 'POST',
      body: JSON.stringify({ status })
    });
    m.status = status;
    matches = matches;
  }

  async function generate(m: Match, type: string) {
    const res = await api<{ content: string }>('/api/generate', {
      method: 'POST',
      body: JSON.stringify({ jobMatchId: m.matchId, type })
    });
    generated[m.matchId] = res.content;
    generated = generated;
  }
</script>

<section>
  <div class="head">
    <h1>Job-Matches</h1>
    <button onclick={runSearch} disabled={busy}>{busy ? '…' : 'Neue Suche'}</button>
  </div>
  {#if message}<p class="msg">{message}</p>{/if}

  {#each matches as m (m.matchId)}
    <article>
      <div class="row">
        <div>
          <strong>{m.title}</strong>
          <div class="meta">{m.company ?? ''} · {m.location ?? ''} · Score {(m.score * 100).toFixed(0)}%</div>
        </div>
        <span class="badge">{m.status}</span>
      </div>
      <div class="actions">
        {#each STATUSES as s}
          <button class="chip" onclick={() => setStatus(m, s)}>{s}</button>
        {/each}
        {#if m.url}<a class="chip link" href={m.url} target="_blank" rel="noreferrer">Anzeige</a>{/if}
      </div>
      <div class="actions">
        <button class="gen" onclick={() => generate(m, 'COVER_LETTER')}>Anschreiben</button>
        <button class="gen" onclick={() => generate(m, 'MOTIVATION')}>Motivation</button>
        <button class="gen" onclick={() => generate(m, 'TAILORED_CV')}>CV anpassen</button>
        <button class="gen" onclick={() => generate(m, 'APPLICATION_TEXT')}>Kurztext</button>
      </div>
      {#if generated[m.matchId]}
        <pre>{generated[m.matchId]}</pre>
      {/if}
    </article>
  {/each}
</section>

<style>
  .head {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .head button {
    padding: 0.6rem 1.2rem;
    background: #2563eb;
    color: #fff;
    border: none;
    border-radius: 6px;
    cursor: pointer;
  }
  article {
    background: #fff;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    padding: 1rem;
    margin-bottom: 1rem;
  }
  .row {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
  }
  .meta {
    color: #6b7280;
    font-size: 0.9rem;
  }
  .badge {
    background: #eef2ff;
    color: #3730a3;
    padding: 0.2rem 0.5rem;
    border-radius: 999px;
    font-size: 0.8rem;
  }
  .actions {
    display: flex;
    flex-wrap: wrap;
    gap: 0.4rem;
    margin-top: 0.6rem;
  }
  .chip,
  .gen {
    border: 1px solid #cbd5e1;
    background: #f8fafc;
    border-radius: 6px;
    padding: 0.3rem 0.6rem;
    cursor: pointer;
    font-size: 0.85rem;
    text-decoration: none;
    color: #1f2937;
  }
  .gen {
    background: #ecfdf5;
    border-color: #6ee7b7;
  }
  pre {
    white-space: pre-wrap;
    background: #f9fafb;
    padding: 0.75rem;
    border-radius: 6px;
    margin-top: 0.6rem;
  }
  .msg {
    color: #374151;
  }
</style>
