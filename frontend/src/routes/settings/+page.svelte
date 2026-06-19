<script lang="ts">
  import { onMount } from 'svelte';
  import { api, getAccessToken } from '$lib/api';
  import { backend, type LicenseStatus } from '$lib/api/backend';
  import type { AuditEvent, AuditPage } from '$lib/api/backend';
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

  // Curated model presets for the quick switcher (key reused server-side per
  // provider/baseUrl — set it once and the dropdown is enough afterwards).
  const LOCAL_BASE = 'http://localhost:11434/v1';   // local Ollama (free, private)
  const OR_BASE = 'https://openrouter.ai/api/v1';   // OpenRouter (public aggregator, one key)
  interface Preset { label: string; group: string; provider: string; baseUrl: string; model: string; }
  const MODEL_PRESETS: Preset[] = [
    { group: 'Local (Ollama — free & private)', label: 'Llama 3.3 (70B) — local', provider: 'OPENAI_COMPATIBLE', baseUrl: LOCAL_BASE, model: 'llama3.3' },
    { group: 'Local (Ollama — free & private)', label: 'Qwen 2.5 (32B) — local', provider: 'OPENAI_COMPATIBLE', baseUrl: LOCAL_BASE, model: 'qwen2.5:32b' },
    { group: 'OpenRouter (cloud — one key, many models)', label: 'GPT-4o mini — cheap & fast', provider: 'OPENAI_COMPATIBLE', baseUrl: OR_BASE, model: 'openai/gpt-4o-mini' },
    { group: 'OpenRouter (cloud — one key, many models)', label: 'Claude 3.5 Sonnet', provider: 'OPENAI_COMPATIBLE', baseUrl: OR_BASE, model: 'anthropic/claude-3.5-sonnet' },
    { group: 'OpenRouter (cloud — one key, many models)', label: 'Gemini 2.0 Flash', provider: 'OPENAI_COMPATIBLE', baseUrl: OR_BASE, model: 'google/gemini-2.0-flash-001' },
    { group: 'OpenRouter (cloud — one key, many models)', label: 'DeepSeek V3', provider: 'OPENAI_COMPATIBLE', baseUrl: OR_BASE, model: 'deepseek/deepseek-chat' }
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
      switchMsg = `✓ Generation model active: ${p.model}`;
    } catch (e) {
      switchMsg = '✗ ' + (e instanceof Error ? e.message : 'Error');
    } finally {
      switching = false;
    }
  }

  onMount(async () => {
    if (!getAccessToken()) {
      await goto('/');
      return;
    }
    await load();
    await loadGlobal();
    if (isAdmin) { await loadLicense(); await loadAudit(); }
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
      message = e instanceof Error ? e.message : 'Error';
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

  // ── License ──

  let licenseKey = $state('');
  let licenseLoading = $state(false);
  let licenseStatus = $state<LicenseStatus | null>(null);
  let licenseMsg = $state<string | null>(null);

  async function loadLicense() {
    try {
      licenseStatus = await backend.getLicenseStatus();
    } catch {
      licenseStatus = null;
    }
  }

  async function uploadLicense() {
    if (!licenseKey.trim()) return;
    licenseLoading = true;
    licenseMsg = null;
    try {
      const s = await backend.uploadLicense(licenseKey.trim());
      licenseStatus = s;
      licenseKey = '';
      licenseMsg = '✓ License activated';
    } catch (e) {
      licenseMsg = '✗ ' + (e instanceof Error ? e.message : 'Error');
    } finally {
      licenseLoading = false;
    }
  }

  // ── Audit-Log ──

  let auditEvents = $state<AuditEvent[]>([]);
  let auditPage = $state(0);
  let auditTotal = $state(0);
  let auditTotalPages = $state(1);
  let auditLoading = $state(false);

  async function loadAudit(p = 0) {
    auditLoading = true;
    try {
      const res = await backend.getAudit({ page: p, size: 50 });
      auditEvents = res.content;
      auditPage = res.number;
      auditTotal = res.totalElements;
      auditTotalPages = res.totalPages;
    } catch {
      auditEvents = [];
    } finally {
      auditLoading = false;
    }
  }
</script>

{#if isAdmin}
<section class="switcher">
  <h1>AI Model (Generation)</h1>
  <p class="cur">
    Active:
    <strong>{activeChat ? activeChat.model : '— none set'}</strong>
    {#if activeChat}<span class="tag">{activeChat.baseUrl && activeChat.baseUrl.includes('openrouter') ? 'Cloud' : 'local'}</span>{/if}
  </p>
  <div class="switchrow">
    <select bind:value={selectedModel} aria-label="Select model">
      {#each GROUPS as g}
        <optgroup label={g}>
          {#each MODEL_PRESETS.filter((m) => m.group === g) as m}
            <option value={m.model}>{m.label}</option>
          {/each}
        </optgroup>
      {/each}
    </select>
    <input
      placeholder="API key (once per cloud provider)"
      type="password"
      bind:value={switchKey}
    />
    <button onclick={switchModel} disabled={switching}>
      {switching ? 'Switching…' : 'Activate'}
    </button>
  </div>
  {#if switchMsg}<p class="msg">{switchMsg}</p>{/if}
  <p class="hint">
    Local models run free &amp; private on your own machine (Ollama). Cloud models (OpenRouter)
    need an API key once — afterwards the dropdown is enough.
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
      <p class="cur">Valid until: <strong>{new Date(licenseStatus.expires).toLocaleDateString()}</strong></p>
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
      <button onclick={uploadLicense} disabled={licenseLoading}>
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
  <h1>LLM-Provider</h1>
  <form onsubmit={create}>
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
        <button onclick={() => loadAudit(auditPage - 1)} disabled={auditPage <= 0 || auditLoading}>←</button>
        <span>Page {auditPage + 1} / {auditTotalPages}</span>
        <button onclick={() => loadAudit(auditPage + 1)} disabled={auditPage >= auditTotalPages - 1 || auditLoading}>→</button>
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
