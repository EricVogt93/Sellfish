<script lang="ts">
  import { auth, setTokens } from '$lib/api';
  import { goto } from '$app/navigation';

  let email = $state('');
  let password = $state('');
  let mode = $state<'login' | 'register'>('login');
  let error = $state<string | null>(null);
  let busy = $state(false);

  async function submit(event: SubmitEvent) {
    event.preventDefault();
    error = null;
    busy = true;
    try {
      const tokens =
        mode === 'login'
          ? await auth.login(email, password)
          : await auth.register(email, password);
      setTokens(tokens);
      await goto('/dashboard');
    } catch (e) {
      error = e instanceof Error ? e.message : 'Unbekannter Fehler';
    } finally {
      busy = false;
    }
  }
</script>

<section>
  <h1>{mode === 'login' ? 'Anmelden' : 'Registrieren'}</h1>
  <form onsubmit={submit}>
    <label>
      E-Mail
      <input type="email" bind:value={email} required />
    </label>
    <label>
      Passwort
      <input type="password" bind:value={password} required minlength="8" />
    </label>
    {#if error}<p class="error">{error}</p>{/if}
    <button type="submit" disabled={busy}>
      {busy ? '…' : mode === 'login' ? 'Login' : 'Konto erstellen'}
    </button>
  </form>
  <p>
    {#if mode === 'login'}
      Noch kein Konto?
      <button class="link" onclick={() => (mode = 'register')}>Registrieren</button>
    {:else}
      Schon registriert?
      <button class="link" onclick={() => (mode = 'login')}>Anmelden</button>
    {/if}
  </p>
</section>

<style>
  form {
    display: flex;
    flex-direction: column;
    gap: 1rem;
    max-width: 360px;
  }
  label {
    display: flex;
    flex-direction: column;
    gap: 0.3rem;
    font-weight: 600;
  }
  input {
    padding: 0.5rem;
    border: 1px solid #cbd5e1;
    border-radius: 6px;
    font-size: 1rem;
  }
  button[type='submit'] {
    padding: 0.6rem;
    background: #2563eb;
    color: #fff;
    border: none;
    border-radius: 6px;
    cursor: pointer;
  }
  .link {
    background: none;
    border: none;
    color: #2563eb;
    cursor: pointer;
    padding: 0;
  }
  .error {
    color: #b91c1c;
  }
</style>
