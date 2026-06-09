import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';

export default defineConfig({
  plugins: [sveltekit()],
  server: {
    port: 5173,
    proxy: {
      // API-Aufrufe im Dev an das Spring-Backend weiterleiten.
      '/api': 'http://localhost:8080'
    }
  }
});
