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

  // Kuratierte Modell-Presets fuer den Schnell-Umschalter (ohne Keys – der Key wird
  // serverseitig pro Provider/baseUrl wiederverwendet, einmal hinterlegt reicht).
  const LOCAL_BASE = 'http://llamacpp.ai.svc.cluster.local:8000/v1';
  const GO_BASE = 'https://opencode.ai/zen/go/v1';
  interface Preset { label: string; group: string; provider: string; baseUrl: string; model: string; }
  const MODEL_PRESETS: Preset[] = [
    { group: 'Lokal (Homelab, privat & frei)', label: 'gpt-oss-20b — lokal, schnell', provider: 'OPENAI_COMPATIBLE', baseUrl: LOCAL_BASE, model: 'gpt-oss-20b' },
    { group: 'opencode-go (Cloud)', label: 'DeepSeek V4 Pro', provider: 'OPENAI_COMPATIBLE', baseUrl: GO_BASE, model: 'deepseek-v4-pro' },
    { group: 'opencode-go (Cloud)', label: 'DeepSeek V4 Flash', provider: 'OPENAI_COMPATIBLE', baseUrl: GO_BASE, model: 'deepseek-v4-flash' },
    { group: 'opencode-go (Cloud)', label: 'GLM 5.1', provider: 'OPENAI_COMPATIBLE', baseUrl: GO_BASE, model: 'glm-5.1' },
    { group: 'opencode-go (Cloud)', label: 'Kimi K2.6', provider: 'OPENAI_COMPATIBLE', baseUrl: GO_BASE, model: 'kimi-k2.6' },
    { group: 'opencode-go (Cloud)', label: 'Qwen3.7 Max', provider: 'OPENAI_COMPATIBLE', baseUrl: GO_BASE, model: 'qwen3.7-max' },
    { group: 'opencode-go (Cloud)', label: 'MiniMax M3', provider: 'OPENAI_COMPATIBLE', baseUrl: GO_BASE, model: 'minimax-m3' }
  ];
  const GROUPS = [...new Set(MODEL_PRESETS.map((p) => p.group))];

  let globalConfigs = $state<Config[]>([]);
  let selectedModel = $state(MODEL_PRESETS[0].model);
  let switchKey = $state('');
  let switching = $state(false);
  let switchMsg = $state<string | null>(null);
  let isAdmin = $state(true);
  const activeChat = $derived(globalConfigs.find((c) => c.purpose === 'CHAT' && c.isDefault) || null);

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

  async function loadGlobal() {
    try {
      globalConfigs = await api<Config[]>('/api/admin/llm-configs');
      isAdmin = true;
      if (activeChat) selectedModel = activeChat.model;
    } catch {
      isAdmin = false; // kein Admin -> Schnell-Umschalter ausblenden
    }
  }

  async function switchModel() {
    const p = MODEL_PRESETS.find((m) => m.model === selectedModel);
    if (!p) return;
    switching = true;
    switchMsg = null;
    try {
      await api('/api/admin/llm-configs', {
        method: 'POST',
        body: JSON.stringify({
          provider: p.provider,
          model: p.model,
          purpose: 'CHAT',
          baseUrl: p.baseUrl,
          apiKey: switchKey || null, // leer -> serverseitig vorhandenen Key wiederverwenden
          isDefault: true
        })
      });
      switchKey = '';
      await loadGlobal();
      switchMsg = `✓ Generierungs-Modell aktiv: ${p.model}`;
    } catch (e) {
      switchMsg = '✗ ' + (e instanceof Error ? e.message : 'Fehler');
    } finally {
      switching = false;
    }
  }

  onMount(async () => {
    if (!getAccessToken()) {
      await goto('/login');
      return;
    }
    await load();
    await loadGlobal();
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

{#if isAdmin}
<section class="switcher">
  <h1>KI-Modell (Generierung)</h1>
  <p class="cur">
    Aktiv:
    <strong>{activeChat ? activeChat.model : '— keins gesetzt'}</strong>
    {#if activeChat}<span class="tag">{activeChat.baseUrl && activeChat.baseUrl.includes('opencode') ? 'Cloud' : 'lokal'}</span>{/if}
  </p>
  <div class="switchrow">
    <select bind:value={selectedModel} aria-label="Modell wählen">
      {#each GROUPS as g}
        <optgroup label={g}>
          {#each MODEL_PRESETS.filter((m) => m.group === g) as m}
            <option value={m.model}>{m.label}</option>
          {/each}
        </optgroup>
      {/each}
    </select>
    <input
      placeholder="API-Key (nur 1× pro Cloud-Provider)"
      type="password"
      bind:value={switchKey}
    />
    <button onclick={switchModel} disabled={switching}>
      {switching ? 'Wechsle…' : 'Aktivieren'}
    </button>
  </div>
  {#if switchMsg}<p class="msg">{switchMsg}</p>{/if}
  <p class="hint">
    Lokale Modelle laufen frei &amp; privat im Homelab. Cloud-Modelle (opencode-go) brauchen den
    Key nur beim ersten Mal — danach reicht das Dropdown.
  </p>
</section>
{/if}

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
</style>
