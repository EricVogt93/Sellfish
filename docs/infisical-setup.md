# Infisical Setup

Central LLM API keys are loaded at runtime from your Infisical instance, not stored in Git or the DB.

## Create a Machine Identity
1. In Infisical: **Access Control → Machine Identities → Create Identity**.
2. Auth method **Universal Auth** → generate a `Client ID` and `Client Secret`.
3. Grant the identity read access to the project/environment where the keys are stored.

## Store Secrets
Place the provider keys in the project, e.g.:
```
/llm/openai-key
/llm/anthropic-key
/llm/nim-key
```
These paths are referenced in `llm_provider_config.key_ref`.

## Configure the Backend
Via environment variables (see `infra/.env.example`):
```
INFISICAL_ENABLED=true
INFISICAL_URL=https://app.infisical.com   # or your self-hosted URL
INFISICAL_CLIENT_ID=...
INFISICAL_CLIENT_SECRET=...
INFISICAL_PROJECT_ID=...
INFISICAL_ENV=prod
```

The backend uses the official Infisical Java SDK. When `INFISICAL_ENABLED=false`, only
per-user keys (encrypted in the DB) are used — practical for local development.
