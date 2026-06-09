<script lang="ts">
  import { onMount } from 'svelte';
  import { api, apiUpload, getAccessToken } from '$lib/api';
  import { goto } from '$app/navigation';

  interface Doc {
    id: string;
    type: string;
    filename: string;
    sizeBytes: number;
    primary: boolean;
    hasText: boolean;
    hasStruct: boolean;
  }

  const TYPES = ['CV', 'PROJECT_LIST', 'CERTIFICATE', 'REFERENCE', 'COVER_LETTER', 'OTHER'];

  let docs = $state<Doc[]>([]);
  let type = $state('CV');
  let file = $state<FileList | null>(null);
  let message = $state<string | null>(null);
  let busy = $state(false);

  async function load() {
    docs = await api<Doc[]>('/api/documents');
  }

  onMount(async () => {
    if (!getAccessToken()) {
      await goto('/login');
      return;
    }
    await load();
  });

  async function upload(event: SubmitEvent) {
    event.preventDefault();
    if (!file || file.length === 0) return;
    busy = true;
    message = null;
    try {
      const form = new FormData();
      form.append('type', type);
      form.append('file', file[0]);
      await apiUpload('/api/documents', form);
      await load();
      message = 'Hochgeladen.';
    } catch (e) {
      message = e instanceof Error ? e.message : 'Fehler';
    } finally {
      busy = false;
    }
  }

  async function remove(id: string) {
    await api(`/api/documents/${id}`, { method: 'DELETE' });
    await load();
  }
</script>

<section>
  <h1>Dokumente</h1>
  <form onsubmit={upload}>
    <select bind:value={type}>
      {#each TYPES as t}<option value={t}>{t}</option>{/each}
    </select>
    <input type="file" onchange={(e) => (file = (e.target as HTMLInputElement).files)} />
    <button type="submit" disabled={busy}>{busy ? '…' : 'Hochladen'}</button>
  </form>
  {#if message}<p class="msg">{message}</p>{/if}

  <table>
    <thead>
      <tr><th>Typ</th><th>Datei</th><th>Text</th><th>Struktur</th><th></th></tr>
    </thead>
    <tbody>
      {#each docs as d}
        <tr>
          <td>{d.type}{d.primary ? ' ★' : ''}</td>
          <td><a href={`/api/documents/${d.id}/download`}>{d.filename}</a></td>
          <td>{d.hasText ? '✓' : '–'}</td>
          <td>{d.hasStruct ? '✓' : '–'}</td>
          <td><button class="del" onclick={() => remove(d.id)}>Löschen</button></td>
        </tr>
      {/each}
    </tbody>
  </table>
</section>

<style>
  form {
    display: flex;
    gap: 0.75rem;
    align-items: center;
    margin-bottom: 1rem;
  }
  select,
  button {
    padding: 0.5rem;
    border-radius: 6px;
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
