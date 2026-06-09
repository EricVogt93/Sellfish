<script lang="ts">
  import { onMount } from 'svelte';
  import { api, getAccessToken } from '$lib/api';
  import { goto } from '$app/navigation';

  interface Config {
    id: string;
    provider: string;
    model: string;
    baseUrl?: string;
    purpose: string;
    isDefault: boolean;
    hasKey: boolean;
  }

  const PROVIDERS = ['OLLAMA', 'OPENAI', 'NIM', 'OPENAI_COMPATIBLE', 'ANTHROPIC', 'GOOGLE'];

  let configs = $state<Config[]>([]);
  let form = $state({
    provider: 'OLLAMA',
    model: '',
    baseUrl: '',
    apiKey: '',
    keyRef: '',
    purpose: 'CHAT',
    isDefault: true
  });
  let message = $state<string | null>(null);

  async function load() {
    configs = await api<Config[]>('/api/llm/configs');
  }

  onMount(async () => {
    if (!getAccessToken()) {
      await goto('/login');
      return;
    }
    await load();
  });

  async function create(event: SubmitEvent) {
    event.preventDefault();
    message = null;
    try {
      await api('/api/llm/configs', { method: 'POST', body: JSON.stringify(form) });
      form.apiKey = '';
      await load();
      message = 'Provider gespeichert.';
    } catch (e) {
      message = e instanceof Error ? e.message : 'Fehler';
    }
  }

  async function test(id: string) {
    const res = await api<{ ok: boolean; message: string }>(`/api/llm/configs/${id}/test`, {
      method: 'POST'
    });
    message = (res.ok ? '✓ ' : '✗ ') + res.message;
  }

  async function remove(id: string) {
    await api(`/api/llm/configs/${id}`, { method: 'DELETE' });
    await load();
  }
</script>

<section>
  <h1>LLM-Provider</h1>
  <form onsubmit={create}>
    <select bind:value={form.provider}>
      {#each PROVIDERS as p}<option value={p}>{p}</option>{/each}
    </select>
    <input placeholder="Modell (z. B. llama3.1)" bind:value={form.model} required />
    <input placeholder="Base-URL (optional)" bind:value={form.baseUrl} />
    <input placeholder="API-Key (optional)" type="password" bind:value={form.apiKey} />
    <input placeholder="Infisical-Pfad (optional)" bind:value={form.keyRef} />
    <select bind:value={form.purpose}>
      <option value="CHAT">Chat</option>
      <option value="EMBEDDING">Embedding</option>
    </select>
    <label class="cb"><input type="checkbox" bind:checked={form.isDefault} /> Standard</label>
    <button type="submit">Hinzufügen</button>
  </form>
  {#if message}<p class="msg">{message}</p>{/if}

  <table>
    <thead>
      <tr><th>Provider</th><th>Modell</th><th>Zweck</th><th>Key</th><th></th></tr>
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
            <button class="del" onclick={() => remove(c.id)}>Löschen</button>
          </td>
        </tr>
      {/each}
    </tbody>
  </table>
</section>

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
</style>
