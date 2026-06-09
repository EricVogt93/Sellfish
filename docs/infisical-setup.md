# Infisical anbinden (M2)

Zentrale LLM-API-Keys werden zur Laufzeit aus deiner Infisical-Instanz geladen, nicht in Git/DB.

## Machine Identity anlegen
1. In Infisical: **Access Control → Machine Identities → Create Identity**.
2. Auth-Methode **Universal Auth** → `Client ID` und `Client Secret` erzeugen.
3. Der Identity Lese-Zugriff auf das Projekt/Environment geben, in dem die Keys liegen.

## Secrets ablegen
Lege die Provider-Keys im Projekt ab, z. B.:
```
/llm/openai-key
/llm/anthropic-key
/llm/nim-key
```
Diese Pfade werden in `llm_provider_config.key_ref` referenziert.

## Backend konfigurieren
Per ENV (siehe `infra/.env.example`):
```
INFISICAL_ENABLED=true
INFISICAL_URL=https://app.infisical.com   # oder deine self-hosted URL
INFISICAL_CLIENT_ID=...
INFISICAL_CLIENT_SECRET=...
INFISICAL_PROJECT_ID=...
INFISICAL_ENV=prod
```

Das Backend nutzt das offizielle Infisical-Java-SDK; bei `INFISICAL_ENABLED=false` werden nur
Per-User-Keys (verschlüsselt in der DB) verwendet – praktisch für lokale Entwicklung.
