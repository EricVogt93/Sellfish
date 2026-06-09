<script lang="ts">
  import { getAccessToken, clearTokens } from '$lib/api';
  import { goto } from '$app/navigation';

  let { children } = $props();

  function logout() {
    clearTokens();
    goto('/login');
  }
</script>

<div class="app">
  <header>
    <a class="brand" href="/">Bewerbungsatze</a>
    <nav>
      <a href="/dashboard">Dashboard</a>
      <a href="/matches">Matches</a>
      <a href="/documents">Dokumente</a>
      <a href="/profile">Profil</a>
      <a href="/settings">Provider</a>
      {#if getAccessToken()}
        <button onclick={logout}>Logout</button>
      {:else}
        <a href="/login">Login</a>
      {/if}
    </nav>
  </header>
  <main>
    {@render children()}
  </main>
</div>

<style>
  :global(body) {
    margin: 0;
    font-family: system-ui, sans-serif;
    background: #f6f7f9;
    color: #1a1a1a;
  }
  header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0.75rem 1.5rem;
    background: #1f2937;
    color: #fff;
  }
  .brand {
    font-weight: 700;
    font-size: 1.15rem;
    color: #fff;
    text-decoration: none;
  }
  nav {
    display: flex;
    gap: 1rem;
    align-items: center;
  }
  nav a,
  nav button {
    color: #e5e7eb;
    text-decoration: none;
    background: none;
    border: none;
    cursor: pointer;
    font-size: 1rem;
  }
  main {
    max-width: 920px;
    margin: 2rem auto;
    padding: 0 1.5rem;
  }
</style>
